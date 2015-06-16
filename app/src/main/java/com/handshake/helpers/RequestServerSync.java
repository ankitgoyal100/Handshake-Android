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

    public static void sendRequest(final User user, final UserSyncCompleted listener) {
        if(user.isRequestSent()) return;

        Realm realm = Realm.getInstance(context);
        Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

        RequestParams params = new RequestParams();
        JSONArray cardIds = new JSONArray();
        cardIds.put(account.getCards().first().getCardId())
        params.put("card_ids", cardIds);

        RestClientAsync.post(context, "/users/" + user.getUserId() + "/request", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(listener, user, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setRequestSent(false);
                realm.commitTransaction();

                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        realm.beginTransaction();
        user.setRequestSent(true);
        realm.commitTransaction();
    }

    public static void deleteRequest(final User user, final UserSyncCompleted listener) {
        if(!user.isRequestSent()) return;

        RestClientAsync.delete(context, "/users/" + user.getUserId() + "/request", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(listener, user, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setRequestSent(true);
                realm.commitTransaction();

                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        user.setRequestSent(false);
        realm.commitTransaction();
    }

    public static void acceptRequest(final User user, final UserSyncCompleted listener) {
        if(!user.isRequestReceived()) return;

        Realm realm = Realm.getInstance(context);
        Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

        RequestParams params = new RequestParams();
        JSONArray cardIds = new JSONArray();
        cardIds.put(account.getCards().first().getCardId())
        params.put("card_ids", cardIds);

        RestClientAsync.post(context, "/users/" + user.getUserId() + "/accept", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(listener, user, response);
                FeedItemServerSync.performSync(context);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setIsContact(false);
                user.setRequestReceived(true);
                realm.commitTransaction();

                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        realm.beginTransaction();
        user.setIsContact(true);
        user.setRequestReceived(false);
        realm.commitTransaction();
    }

    public static void declineRequest(final User user, final UserSyncCompleted listener) {
        if(!user.isRequestReceived()) return;

        RestClientAsync.delete(context, "/users/" + user.getUserId() + "/decline", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(listener, user, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setRequestReceived(true);
                realm.commitTransaction();

                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        user.setRequestReceived(false);
        realm.commitTransaction();
    }

    private static void success(UserSyncCompleted listener, User user, JSONObject response) {
        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        try {
            User.updateContact(user, realm, response.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        realm.commitTransaction();

        listener.syncCompletedListener(user);
    }
}
