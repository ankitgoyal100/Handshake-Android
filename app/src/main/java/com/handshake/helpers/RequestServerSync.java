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
            public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
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
                            for (int i = 0; i < requestReceivedUsers.size(); i++) {
                                if (!requestUserIds.contains(requestReceivedUsers.get(i).getUserId())) {
                                    realm.beginTransaction();
                                    requestReceivedUsers.get(i).setRequestReceived(false);
                                    realm.commitTransaction();
                                }
                            }
                            realm.close();
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
                if (errorResponse == null) return;
                if (statusCode == 401) session.logoutUser();
                else performSyncHelper();
            }
        });
    }

    public static void sendRequest(final Context context, final User user, final UserSyncCompleted listener) {
        if (user.isRequestSent()) return;

        Realm realm = Realm.getInstance(context);
        SessionManager sessionManager = new SessionManager(context);
        Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();

        JSONArray cardIds = new JSONArray();
        cardIds.put(account.getCards().first().getCardId());

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("card_ids", cardIds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        realm.close();

        RestClientAsync.post(context, "/users/" + user.getUserId() + "/request", jsonParams, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(context, listener, user, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setRequestSent(false);
                realm.commitTransaction();
                realm.close();

                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        realm.beginTransaction();
        user.setRequestSent(true);
        realm.commitTransaction();
    }

    public static void deleteRequest(final Context context, final User user, final UserSyncCompleted listener) {
        if (!user.isRequestSent()) return;

        RestClientAsync.delete(context, "/users/" + user.getUserId() + "/request", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(context, listener, user, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setRequestSent(true);
                realm.commitTransaction();

                realm.close();
                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        user.setRequestSent(false);
        realm.commitTransaction();
        realm.close();
    }

    public static void acceptRequest(final Context context, final User user, final UserSyncCompleted listener) {
        if (!user.isRequestReceived()) return;

        Realm realm = Realm.getInstance(context);
        SessionManager sessionManager = new SessionManager(context);
        Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();

        JSONArray cardIds = new JSONArray();
        cardIds.put(account.getCards().first().getCardId());

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("card_ids", cardIds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        realm.close();

        RestClientAsync.post(context, "/users/" + user.getUserId() + "/accept", jsonParams, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(context, listener, user, response);
                FeedItemServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
                System.out.println(errorResponse.toString());
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setIsContact(false);
                user.setRequestReceived(true);
                realm.commitTransaction();

                realm.close();
                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        realm.beginTransaction();
        user.setIsContact(true);
        user.setRequestReceived(false);
        realm.commitTransaction();
    }

    public static void declineRequest(final Context context, final User user, final UserSyncCompleted listener) {
        if (!user.isRequestReceived()) return;

        RestClientAsync.delete(context, "/users/" + user.getUserId() + "/decline", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                success(context, listener, user, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                user.setRequestReceived(true);
                realm.commitTransaction();

                realm.close();
                listener.syncFailedListener();

                if (statusCode == 401) session.logoutUser();
            }
        });

        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        user.setRequestReceived(false);
        realm.commitTransaction();
        realm.close();
    }

    private static void success(Context context, UserSyncCompleted listener, User user, JSONObject response) {
        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        try {
            User.updateContact(user, realm, response.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        realm.commitTransaction();

        realm.close();
        listener.syncCompletedListener(user);
    }
}
