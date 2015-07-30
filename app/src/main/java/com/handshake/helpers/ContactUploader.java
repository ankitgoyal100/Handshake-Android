package com.handshake.helpers;

import android.content.Context;
import android.database.Cursor;
import android.os.Looper;
import android.provider.ContactsContract;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by ankitgoyal on 7/30/15.
 */
public class ContactUploader {
    private static long MILLISECONDS_BETWEEN_SYNC = 604800000;

    public static void performSync(final Context c, final SyncCompleted l) {
        Date currentDate = new Date(System.currentTimeMillis());
        String lastUpdatedAtString = SessionManager.getLastContactSynced();
        if (!lastUpdatedAtString.equals("") &&
                (currentDate.getTime() - new Date(lastUpdatedAtString).getTime()) < MILLISECONDS_BETWEEN_SYNC) {
            l.syncCompletedListener();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONArray phones = new JSONArray();
                final JSONArray emails = new JSONArray();

                final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

                Cursor systemPhones = c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                while (systemPhones.moveToNext()) {
                    String phone = systemPhones.getString(systemPhones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phone = phone.replaceAll("\\D+", "");

                    boolean phoneAdded = false;

                    try {
                        PhoneNumber phoneNumber = phoneUtil.parse(phone, c.getResources().getConfiguration().locale.getCountry());

                        if (phoneUtil.isValidNumber(phoneNumber)) {
                            phones.put(phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164));
                            phoneAdded = true;
                        }
                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    }

                    try {
                        if (!phoneAdded && phone.length() > 0 && phone.charAt(0) != '+') {
                            PhoneNumber phoneNumber = phoneUtil.parse("+" + phone, c.getResources().getConfiguration().locale.getCountry());

                            if (phoneUtil.isValidNumber(phoneNumber)) {
                                phones.put(phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164));
                                phoneAdded = true;
                            }
                        }
                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    }


                }
                systemPhones.close();

                Cursor systemEmails = c.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
                while (systemEmails.moveToNext()) {
                    String email = systemEmails.getString(systemEmails.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                    emails.put(email);
                }
                systemEmails.close();

                System.out.println("Phones: " + phones);
                System.out.println("Emails: " + emails);

                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }

                JSONObject phoneParams = new JSONObject();
                try {
                    phoneParams.put("phones", phones);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RestClientSync.post(c, "/upload/phones", phoneParams, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        System.out.println("Success 1");

                        JSONObject emailParams = new JSONObject();
                        try {
                            emailParams.put("emails", emails);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        RestClientSync.post(c, "/upload/emails", emailParams, "application/json", new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                SessionManager.setLastContactSynced(System.currentTimeMillis() + "");
                                l.syncCompletedListener();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                if (statusCode == 401) {
                                    SessionManager session = new SessionManager(c);
                                    session.logoutUser();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if (statusCode == 401) {
                            SessionManager session = new SessionManager(c);
                            session.logoutUser();
                        }
                    }
                });
            }
        }).start();
    }
}
