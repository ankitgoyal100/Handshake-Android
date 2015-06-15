package com.handshake.Handshake;

import android.content.Context;
import android.os.Handler;

import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class ContactServerSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
//    private static Realm realm;
    private static SyncCompleted listener;

    private static int counter = 0;

    public static void performSync(final Context c, final SyncCompleted l) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                context = c;
                listener = l;
                session = new SessionManager(context);
//                realm = Realm.getInstance(context);
                performSyncHelper();
            }
        }).start();

    }

    private static void performSyncHelper() {
        final Realm realm = Realm.getInstance(context);
        //Get most recent contactUpdated date
        String date = "";
        RealmResults<User> result = realm.where(User.class).equalTo("isContact", true).findAll();
        result.sort("updatedAt", RealmResults.SORT_ORDER_DESCENDING);

        if (result.size() > 0) date = result.first().getContactUpdated().toString();

        System.out.println("Date: " + date);

        syncPage(1, date, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                System.out.println("Sync completed listener");
                RealmResults<User> toDelete = realm.where(User.class).equalTo("syncStatus", Utils.userDeleted).findAll();
                if(toDelete.size() == 0) listener.syncCompletedListener();
                for (final User user : toDelete) {
                    System.out.println("Deleting user: " + user);
                    counter++;
                    RestClientAsync.delete(context, "/users/" + user.getUserId(), new RequestParams(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            user.setSyncStatus(Utils.userSynced);

                            counter--;
                            if (counter == 0) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.syncCompletedListener();
                                    }
                                });
                            }
                        }
                    });
                }

                //TODO: Contact Sync
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
                System.out.println(response.toString());

                Realm realm = Realm.getInstance(context);

                final HashMap<Integer, JSONObject> map = new HashMap<Integer, JSONObject>();

                try {
                    JSONArray contacts = response.getJSONArray("contacts");
                    for (int i = 0; i < contacts.length(); i++) {
                        map.put(contacts.getJSONObject(i).getInt("id"), contacts.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                System.out.println("Map: " + map.size());

                // update/delete contact and remove from list
                Iterator it = map.keySet().iterator();
                while (it.hasNext()) {
                    final Integer key = (Integer) it.next();
                    User result = realm.where(User.class).equalTo("userId", key).findFirst();
                    if (result != null) {
                        if (result.getSyncStatus() != Utils.userDeleted) {
                            realm.beginTransaction();

                            result = User.updateContact(result, realm, map.get(key));
                            result.setSyncStatus(Utils.userSynced);
                            try {
                                result.setContactUpdated(Utils.formatDate(map.get(key).getString("contact_updated")));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            realm.commitTransaction();

                        }
                        it.remove();
                    }
                }
                System.out.println("Map after updating contacts: " + map.size());

                // all left over are new contacts unless they are deleted
                for (final int key : map.keySet()) {
                    try {
                        if (!map.get(key).getBoolean("is_contact")) {
                            continue;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    realm.beginTransaction();

                    User user = realm.createObject(User.class);
                    user = User.updateContact(user, realm, map.get(key));
                    user.setSyncStatus(Utils.userSynced);
                    try {
                        user.setContactUpdated(Utils.formatDate(map.get(key).getString("contact_updated")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    System.out.println("User added: " + user.toString());

                    realm.commitTransaction();
                }

                try {
                    if (response.getJSONArray("contacts").length() < 200) {
                        listener.syncCompletedListener();
                        return;
                    }

                    syncPage(page + 1, contactUpdated, listener);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (statusCode == 401) session.logoutUser();
            }
        });
    }
}

