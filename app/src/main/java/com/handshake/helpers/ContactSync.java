package com.handshake.helpers;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Card;
import com.handshake.models.User;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

import static android.content.ContentProviderOperation.Builder;
import static android.content.ContentProviderOperation.newInsert;
import static android.content.ContentProviderOperation.newUpdate;
import static android.provider.ContactsContract.CommonDataKinds.Email;
import static android.provider.ContactsContract.CommonDataKinds.Note;
import static android.provider.ContactsContract.CommonDataKinds.Phone;
import static android.provider.ContactsContract.CommonDataKinds.Photo;
import static android.provider.ContactsContract.CommonDataKinds.StructuredName;

/**
 * Created by ankitgoyal on 8/3/15.
 */
public class ContactSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
    private static SyncCompleted listener;

    public static void performSync(final Context c, final SyncCompleted l) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                context = c;
                listener = l;
                session = new SessionManager(context);
                performSyncHelper();
            }
        }).start();

    }

    public static void syncAll(final Context c, final SyncCompleted l) {
        context = c;
        listener = l;

        Realm realm = Realm.getInstance(context);
        RealmResults<User> contacts = realm.where(User.class).equalTo("isContact", true).findAll();

        realm.beginTransaction();
        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).setSaved(false);
        }
        realm.commitTransaction();

        realm.close();
        performSync(context, listener);
    }

    private static void performSyncHelper() {
        Realm realm = Realm.getInstance(context);
        RealmResults<User> users;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAutosync = sharedPreferences.getBoolean("autosync_preference", true);
        if (isAutosync) {
            users = realm.where(User.class).equalTo("isContact", true).equalTo("saved", false).findAll();
        } else {
            users = realm.where(User.class).equalTo("isContact", true).equalTo("saved", false).equalTo("savesToPhone", true).findAll();
        }

        for (int i = 0; i < users.size(); i++) {
            syncContactToAddressBook(users.get(i));
        }

        realm.close();
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.syncCompletedListener();
            }
        });
    }

    private static void syncContactToAddressBook(User user) {
        if (user.getCards().size() == 0) return;

        Card card = user.getCards().first();

        if (card.getPhones().size() + card.getEmails().size() + card.getAddresses().size() + card.getSocials().size() == 0)
            return;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        // find record to update
        String contactId = "";
        int matches = 0; // count of how many contacts have matching data

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int certainty = 0; // count of how many data points match
                boolean nameMatch = false;

                String currentContactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                // check name
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (name != null && name.contains(user.getFirstName())) {
                    nameMatch = true;
                    certainty++;
                }

                // check phones
                Cursor phoneCursor = contentResolver.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = ?", new String[]{currentContactId}, null);
                while (phoneCursor != null && phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                    phoneNumber = phoneNumber.replaceAll("\\D+", "");

                    for (int i = 0; i < card.getPhones().size(); i++) {
                        if (card.getPhones().get(i).getNumber().contains(phoneNumber)) {
                            // match
                            certainty++;
                            break;
                        }
                    }
                }
                if (phoneCursor != null)
                    phoneCursor.close();

                // check emails
                Cursor emailCursor = contentResolver.query(Email.CONTENT_URI, null, Email.CONTACT_ID + " = ?", new String[]{currentContactId}, null);
                while (emailCursor != null && emailCursor.moveToNext()) {
                    String email = emailCursor.getString(emailCursor.getColumnIndex(Email.DATA));
                    for (int i = 0; i < card.getEmails().size(); i++) {
                        if (card.getEmails().get(i).getAddress().contains(email)) {
                            // match
                            certainty++;
                            break;
                        }
                    }
                }
                if (emailCursor != null)
                    emailCursor.close();

                // require 2 certainty points for guaranteed match
                if (certainty >= 2) {
                    contactId = currentContactId;
                    matches = 1; // we are certain
                    break;
                } else if (!nameMatch && certainty == 1) { // cannot say match solely based on first name
                    contactId = currentContactId;
                    matches++;
                }
            }
        }


        if (contactId.isEmpty() || matches != 1) { // if no record found or multiple matches make a new contact
            createAddressBookContact(user, card);
        } else {
            updateAddressBookContact(user, card, contactId);
        }

        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        user.setSaved(true);
        realm.commitTransaction();
        realm.close();
    }

    private static void updateAddressBookContact(User user, Card card, String contactId) {
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean overwritePictures = sharedPreferences.getBoolean("overwrite_pictures_preference", true);
            boolean overwriteName = sharedPreferences.getBoolean("overwrite_names_preference", true);

            ContentResolver contentResolver = context.getContentResolver();
            String where = Data.RAW_CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";

            String[] photoParams = new String[]{getRawContactId(Integer.parseInt(contactId)) + "", Photo.CONTENT_ITEM_TYPE};
            String[] nameParams = new String[]{getRawContactId(Integer.parseInt(contactId)) + "", StructuredName.CONTENT_ITEM_TYPE};
            String[] socialParams = new String[]{getRawContactId(Integer.parseInt(contactId)) + "", Note.CONTENT_ITEM_TYPE};

            ArrayList<android.content.ContentProviderOperation> ops = new ArrayList<android.content.ContentProviderOperation>();

            if (!user.getPicture().isEmpty() && (!contactHasPhoto(context, contactId) || overwritePictures)) {
                if (user.getPictureData() != null && user.getPictureData().length > 0) {
                    ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection(where, photoParams)
                            .withValue(Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, user.getPictureData())
                            .build());
                } else {
                    try {
                        DefaultHttpClient client = new DefaultHttpClient();
                        HttpGet request = new HttpGet(user.getPicture());
                        HttpResponse response = client.execute(request);
                        HttpEntity entity = response.getEntity();
                        int imageLength = (int) (entity.getContentLength());
                        InputStream is = entity.getContent();

                        byte[] imageBlob = new byte[imageLength];
                        int bytesRead = 0;
                        while (bytesRead < imageLength) {
                            int n = is.read(imageBlob, bytesRead, imageLength - bytesRead);
                            if (n <= 0)
                                ; // do some error handling
                            bytesRead += n;
                        }

                        Realm realm = Realm.getInstance(context);
                        realm.beginTransaction();
                        user.setPictureData(imageBlob);
                        realm.commitTransaction();
                        realm.close();

                        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                .withSelection(where, photoParams)
                                .withValue(Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBlob)
                                .build());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (overwriteName) {
                ops.add(newUpdate(Data.CONTENT_URI)
                        .withSelection(where, nameParams)
                        .withValue(StructuredName.GIVEN_NAME, user.getFirstName())
                        .withValue(StructuredName.FAMILY_NAME, user.getLastName())
                        .build());
            }

            for (int i = 0; i < card.getPhones().size(); i++) {
                boolean skip = false;
                Cursor phoneCursor = contentResolver.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = ?", new String[]{contactId}, null);
                while (phoneCursor != null && phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.NUMBER));
                    phoneNumber = phoneNumber.replaceAll("\\D+", "");

                    if (card.getPhones().get(i).getNumber().contains(phoneNumber) ||
                            card.getPhones().get(i).getNumber().contains(phoneNumber.substring(1))) {
                        skip = true;
                        break;
                    }
                }
                if (skip) continue;

                PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                try {
                    Phonenumber.PhoneNumber numberObject = phoneUtil.parse(card.getPhones().get(i).getNumber(),
                            card.getPhones().get(i).getCountryCode());
                    if (phoneUtil.isValidNumber(numberObject)) {

                        // Adding insert operation to operations list
                        // to insert Phone Number in the table ContactsContract.Data
                        ContentValues values = new ContentValues();
                        values.put(Data.RAW_CONTACT_ID, getRawContactId(Integer.parseInt(contactId)));
                        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                        values.put(Phone.NUMBER, phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
                        values.put(Phone.TYPE, Utils.labelToType(card.getPhones().get(i).getLabel(), true));
                        context.getContentResolver().insert(Data.CONTENT_URI, values);
                    }
                } catch (NumberParseException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < card.getEmails().size(); i++) {
                boolean skip = false;
                Cursor emailCursor = contentResolver.query(Email.CONTENT_URI, null, Email.CONTACT_ID + " = ?", new String[]{contactId}, null);
                while (emailCursor != null && emailCursor.moveToNext()) {
                    String email = emailCursor.getString(emailCursor.getColumnIndex(Email.DATA));
                    if (card.getEmails().get(i).getAddress().contains(email)) {
                        skip = true;
                        break;
                    }
                }

                if (skip) continue;

                if (card.getEmails().get(i).getAddress().isEmpty()) continue;

                // Adding insert operation to operations list
                // to insert Email in the table ContactsContract.Data
                ContentValues values = new ContentValues();
                values.put(Data.RAW_CONTACT_ID, getRawContactId(Integer.parseInt(contactId)));
                values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                values.put(Email.ADDRESS, card.getEmails().get(i).getAddress());
                values.put(Email.TYPE, Utils.labelToType(card.getEmails().get(i).getLabel(), false));
                context.getContentResolver().insert(Data.CONTENT_URI, values);
            }

            for (int i = 0; i < card.getAddresses().size(); i++) {
                boolean skip = false;
                Cursor addressCursor = contentResolver.query(StructuredPostal.CONTENT_URI, null, StructuredPostal.CONTACT_ID + " = ?", new String[]{contactId}, null);
                while (addressCursor != null && addressCursor.moveToNext()) {
                    String street = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.STREET));
                    if (street.contains(card.getAddresses().get(i).getStreet1())) {
                        skip = true;
                        break;
                    }
                }

                if (skip) continue;

                ContentValues values = new ContentValues();
                values.put(Data.RAW_CONTACT_ID, getRawContactId(Integer.parseInt(contactId)));
                values.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
                values.put(Phone.TYPE, Utils.labelToType(card.getPhones().get(i).getLabel(), true));

                if (!card.getAddresses().get(i).getStreet1().isEmpty() &&
                        !card.getAddresses().get(i).getStreet2().isEmpty())
                    values.put(StructuredPostal.STREET, card.getAddresses().get(i).getStreet1() + "\n" + card.getAddresses().get(i).getStreet2());
                else if (!card.getAddresses().get(i).getStreet1().isEmpty())
                    values.put(StructuredPostal.STREET, card.getAddresses().get(i).getStreet1());
                else if (!card.getAddresses().get(i).getStreet2().isEmpty())
                    values.put(StructuredPostal.STREET, card.getAddresses().get(i).getStreet2());

                if (!card.getAddresses().get(i).getCity().isEmpty())
                    values.put(StructuredPostal.CITY, card.getAddresses().get(i).getCity());
                if (!card.getAddresses().get(i).getState().isEmpty())
                    values.put(StructuredPostal.REGION, card.getAddresses().get(i).getState());
                if (!card.getAddresses().get(i).getZip().isEmpty())
                    values.put(StructuredPostal.POSTCODE, card.getAddresses().get(i).getZip());

                context.getContentResolver().insert(Data.CONTENT_URI, values);
            }

            String social = "";
            for (int i = 0; i < card.getSocials().size(); i++) {
                if (!card.getSocials().get(i).getNetwork().equals("facebook")) {
                    String network = card.getSocials().get(i).getNetwork();
                    network = network.substring(0, 1).toUpperCase() + network.substring(1);

                    social += network + ": " + card.getSocials().get(i).getUsername() + "\n";
                }
            }

            if (!social.isEmpty())
                ops.add(newUpdate(Data.CONTENT_URI)
                        .withSelection(where, socialParams)
                        .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                        .withValue(Note.NOTE, social)
                        .build());

            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAddressBookContact(User user, Card card) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        int rawContactID = ops.size();

        // Adding insert operation to operations list
        // to insert a new raw contact in the table ContactsContract.RawContacts
        ops.add(newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build());

        if (!user.getPicture().isEmpty()) {
            if (user.getPictureData() != null && user.getPictureData().length > 0) {
                // Adding insert operation to operations list
                // to insert Photo in the table ContactsContract.Data
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                        .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                        .withValue(ContactsContract.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, user.getPictureData())
                        .build());

            } else {
                try {
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(user.getPicture());
                    HttpResponse response = client.execute(request);
                    HttpEntity entity = response.getEntity();
                    int imageLength = (int) (entity.getContentLength());
                    InputStream is = entity.getContent();

                    byte[] imageBlob = new byte[imageLength];
                    int bytesRead = 0;
                    while (bytesRead < imageLength) {
                        int n = is.read(imageBlob, bytesRead, imageLength - bytesRead);
                        if (n <= 0)
                            ; // do some error handling
                        bytesRead += n;
                    }

                    Realm realm = Realm.getInstance(context);
                    realm.beginTransaction();
                    user.setPictureData(imageBlob);
                    realm.commitTransaction();
                    realm.close();

                    // Adding insert operation to operations list
                    // to insert Photo in the table ContactsContract.Data
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                            .withValue(ContactsContract.Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBlob)
                            .build());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Adding insert operation to operations list
        // to insert display name in the table ContactsContract.Data
        ops.add(newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawContactID)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.GIVEN_NAME, user.getFirstName())
                .withValue(StructuredName.FAMILY_NAME, user.getLastName())
                .build());

        for (int i = 0; i < card.getPhones().size(); i++) {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            try {
                Phonenumber.PhoneNumber numberObject = phoneUtil.parse(card.getPhones().get(i).getNumber(),
                        card.getPhones().get(i).getCountryCode());
                if (phoneUtil.isValidNumber(numberObject)) {
                    // Adding insert operation to operations list
                    // to insert Phone Number in the table ContactsContract.Data
                    ops.add(newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))
                            .withValue(Phone.TYPE, Utils.labelToType(card.getPhones().get(i).getLabel(), true))
                            .build());
                }
            } catch (NumberParseException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < card.getEmails().size(); i++) {
            if (card.getEmails().get(i).getAddress().isEmpty()) continue;
            // Adding insert operation to operations list
            // to insert Email in the table ContactsContract.Data
            ops.add(newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactID)
                    .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.ADDRESS, card.getEmails().get(i).getAddress())
                    .withValue(Email.TYPE, Utils.labelToType(card.getEmails().get(i).getLabel(), false))
                    .build());
        }

        for (int i = 0; i < card.getAddresses().size(); i++) {
            Builder address =
                    newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactID)
                            .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(StructuredPostal.TYPE, Utils.labelToType(card.getAddresses().get(i).getLabel(), false));

            if (!card.getAddresses().get(i).getStreet1().isEmpty() &&
                    !card.getAddresses().get(i).getStreet2().isEmpty())
                address.withValue(StructuredPostal.STREET, card.getAddresses().get(i).getStreet1() + "\n" + card.getAddresses().get(i).getStreet2());
            else if (!card.getAddresses().get(i).getStreet1().isEmpty())
                address.withValue(StructuredPostal.STREET, card.getAddresses().get(i).getStreet1());
            else if (!card.getAddresses().get(i).getStreet2().isEmpty())
                address.withValue(StructuredPostal.STREET, card.getAddresses().get(i).getStreet2());

            if (!card.getAddresses().get(i).getCity().isEmpty())
                address.withValue(StructuredPostal.CITY, card.getAddresses().get(i).getCity());
            if (!card.getAddresses().get(i).getState().isEmpty())
                address.withValue(StructuredPostal.REGION, card.getAddresses().get(i).getState());
            if (!card.getAddresses().get(i).getZip().isEmpty())
                address.withValue(StructuredPostal.POSTCODE, card.getAddresses().get(i).getZip());

            ops.add(address.build());
        }

        String social = "";
        for (int i = 0; i < card.getSocials().size(); i++) {
            if (!card.getSocials().get(i).getNetwork().equals("facebook")) {
                String network = card.getSocials().get(i).getNetwork();
                network = network.substring(0, 1).toUpperCase() + network.substring(1);

                social += network + ": " + card.getSocials().get(i).getUsername() + "\n";
            }
        }

        if (!social.isEmpty())
            ops.add(newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactID)
                    .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, social)
                    .build());

        try {
            // Executing all the insert operations as a single database transaction
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private static boolean contactHasPhoto(Context context, String contactId) {
        Cursor cur = context.getContentResolver().query(
                Data.CONTENT_URI,
                null,
                Data.CONTACT_ID + "=" + contactId + " AND "
                        + Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                        + "'", null, null);
        if (cur != null) {
            if (!cur.moveToFirst()) {
                return false;
            }
        } else {
            return false; // error in cursor process
        }
        return true;
    }

    public static int getRawContactId(int contactId) {
        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(contactId)};
        Cursor c = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        int rawContactId = -1;
        if (c.moveToFirst()) {
            rawContactId = c.getInt(c.getColumnIndex(ContactsContract.RawContacts._ID));
        }
        return rawContactId;
    }
}
