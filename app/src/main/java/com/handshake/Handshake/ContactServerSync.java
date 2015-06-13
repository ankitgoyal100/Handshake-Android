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

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class ContactServerSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
    private static Realm realm;
    private static SyncCompleted listener;

    private static int counter = 0;

    public static void performSync(final Context c, final SyncCompleted l) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                performSyncHelper(c, l);
            }
        }).start();

    }

    private static void performSyncHelper(Context c, SyncCompleted l) {
        context = c;
        listener = l;
        session = new SessionManager(c);
        //Get most recent contactUpdated date
        realm = Realm.getInstance(context);

        String date = "";
        RealmResults<User> result = realm.where(User.class).equalTo("isContact", true).findAll();
        result.sort("contact_updated", RealmResults.SORT_ORDER_DESCENDING);

        if (result.size() > 0) date = result.first().getContactUpdated().toString();
        syncPage(1, date, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                RealmResults<User> toDelete = realm.where(User.class).equalTo("syncStatus", Utils.userDeleted).findAll();
                for (final User user : toDelete) {
                    counter++;
                    RestClient.delete(context, "/users/" + user.getUserId(), new RequestParams(), new JsonHttpResponseHandler() {
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

        RestClient.get(context, "/contacts", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                HashMap<Integer, JSONObject> map = new HashMap<Integer, JSONObject>();

                try {
                    JSONArray contacts = response.getJSONArray("contacts");
                    for (int i = 0; i < contacts.length(); i++) {
                        map.put(contacts.getJSONObject(i).getInt("id"), contacts.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // update/delete contact and remove from list
                for (int key : map.keySet()) {
                    User result = realm.where(User.class).equalTo("userId", key).findFirst();
                    if (result != null) {
                        if (result.getSyncStatus() != Utils.userDeleted)
                            result.updateContact(realm, map.get(key));
                        map.remove(key);
                    }
                }
                // all left over are new contacts unless they are deleted
                for (int key : map.keySet()) {
                    try {
                        if (!map.get(key).getBoolean("is_contact")) {
                            continue;
                        }

                        User.createContact(realm, map.get(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                System.out.println(errorResponse.toString());
                if (statusCode == 401) session.logoutUser();
            }
        });
    }
}

