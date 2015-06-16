package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientAsync;
import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.models.Account;
import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/16/15.
 */
public class RequestServerSync {
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

    private static void performSyncHelper() {
        RestClientSync.get(context, "/requests", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                System.out.println(response.toString());

                try {
                    final JSONArray requestsJSONArray = response.getJSONArray("requests");
                    UserServerSync.cacheUser(context, requestsJSONArray, new UserArraySyncCompleted() {
                        @Override
                        public void syncCompletedListener(ArrayList<User> users) {
                            ArrayList<Long> requestUserIds = new ArrayList<Long>();
                            for (int i = 0; i < requestsJSONArray.length(); i++) {
                                try {
                                    requestUserIds.add(requestsJSONArray.getJSONObject(i).getLong("id"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            Realm realm = Realm.getInstance(context);
                            RealmResults<User> requestReceivedUsers = realm.where(User.class).equalTo("requestReceived", true).findAll();
                            for (User user : requestReceivedUsers) {
                                if (!requestUserIds.contains(user.getUserId())) {
                                    realm.beginTransaction();
                                    user.setRequestReceived(false);
                                    realm.commitTransaction();
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.syncCompletedListener();
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                System.out.println(errorResponse.toString());
                if (statusCode == 401) session.logoutUser();
                else performSyncHelper();
            }
        });
    }

    public static void sendRequest(User user, UserSyncCompleted listener) {
        final Realm realm = Realm.getInstance(context);
        Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

        RequestParams params = new RequestParams();
        params.put("card_ids", account.getCards().first().getCardId());

        RestClientAsync.post(context, "/users/" + user.getUserId() + "/request", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                realm.beginTransaction();
                try {
                    user = User.updateContact(user, realm, response.getJSONObject("user"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                System.out.println(errorResponse.toString());
                realm.beginTransaction();
                user.setRequestSent(false);
                realm.commitTransaction();
                if (statusCode == 401) session.logoutUser();
            }
        });
    }

}
