package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientAsync;
import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class ContactServerSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
    private static SyncCompleted listener;

    private static int counter = 0;

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

    private static void performSyncHelper() {
        final Realm realm = Realm.getInstance(context);
        //Get most recent contactUpdated date Mon Jun 15 21:04:57 PDT 2015
        String date = "";
        RealmResults<User> result = realm.where(User.class).equalTo("isContact", true).findAll();
        result.sort("contactUpdated", RealmResults.SORT_ORDER_DESCENDING);

        if (result.size() > 0) date = Utils.toGmtString(result.first().getContactUpdated());

        syncPage(1, date, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                Realm realm = Realm.getInstance(context);
                RealmResults<User> toDelete = realm.where(User.class).equalTo("syncStatus", Utils.UserDeleted).findAll();
                if (toDelete.size() == 0) {
                    ContactSync.performSync(context, new SyncCompleted() {
                        @Override
                        public void syncCompletedListener() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.syncCompletedListener();
                                }
                            });
                        }
                    });
                }
                for (final User user : toDelete) {
                    counter++;
                    RestClientAsync.delete(context, "/users/" + user.getUserId(), new RequestParams(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            user.setSyncStatus(Utils.UserSynced);

                            counter--;
                            if (counter == 0) {
                                ContactSync.performSync(context, new SyncCompleted() {
                                    @Override
                                    public void syncCompletedListener() {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.syncCompletedListener();
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private static void syncPage(final int page, final String contactUpdated, final SyncCompleted listener) {
        RequestParams params = new RequestParams();
        params.put("page", page);
        if (!contactUpdated.equals("")) params.put("since_date", contactUpdated);

        RestClientSync.get(context, "/contacts", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray contacts = response.getJSONArray("contacts");

                    final HashMap<Long, JSONObject> map = new HashMap<Long, JSONObject>();
                    for (int i = 0; i < contacts.length(); i++) {
                        map.put(contacts.getJSONObject(i).getLong("id"), contacts.getJSONObject(i));
                    }

                    UserServerSync.cacheUser(context, contacts, new UserArraySyncCompleted() {
                        @Override
                        public void syncCompletedListener(ArrayList<User> users) {
                            Realm realm = Realm.getInstance(context);
                            RealmResults<User> areContacts = realm.where(User.class).equalTo("isContact", true).findAll();

                            for (int i = 0; i < areContacts.size(); i++) {
                                if (!map.keySet().contains(areContacts.get(i).getUserId())) {
                                    realm.beginTransaction();
                                    try {
                                        areContacts.get(i).setContactUpdated(Utils.formatDate(
                                                map.get(areContacts.get(i).getUserId()).getString("contact_updated")));
                                    } catch (JSONException e) {

                                    }
                                    realm.commitTransaction();
                                }

                            }


                        }
                    });

                    if (contacts.length() < 200) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.syncCompletedListener();
                            }
                        });
                        return;
                    }

                    syncPage(page + 1, contactUpdated, listener);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
                if (statusCode == 401) session.logoutUser();
            }
        });
    }

    public static void deleteContact(User user) {
        if (!user.isContact()) return;

        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        user.setIsContact(false);
        user.setContactUpdated(new Date(0));
        user.setSyncStatus(Utils.UserDeleted);

        for (int i = 0; i < user.getFeedItems().size(); i++) {
            user.getFeedItems().get(i).removeFromRealm();
        }
        realm.commitTransaction();

        performSync(context, listener);
    }
}