package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;

import io.realm.Realm;

/**
 * Created by ankitgoyal on 6/16/15.
 */
public class AccountServerSync {
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
        final Realm realm = Realm.getInstance(context);
        final Account[] account = {realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst()};

        if (account[0] == null) return;

        System.out.println("Account: " + account[0].toString());

        if (account[0].getSyncStatus() == Utils.AccountSynced) {
            RequestParams params = Account.accountToParams(account[0]);
            if (account[0].getPicture().isEmpty() && account[0].getPictureData() != null) {
                params.put("picture", new ByteArrayInputStream(account[0].getPictureData()), "picture.jpg", "image/jpg");

                RestClientSync.put(context, "/account", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        putOnSuccess(account[0], response);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        System.out.println(errorResponse.toString());
                        if (statusCode == 401) session.logoutUser();
                        else performSyncHelper();
                    }
                });
            } else {
                RestClientSync.put(context, "/account", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        putOnSuccess(account[0], response);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        System.out.println(errorResponse.toString());
                        if (statusCode == 401) session.logoutUser();
                        else performSyncHelper();
                    }
                });
            }
        } else {
            RestClientSync.get(context, "/account", new RequestParams(), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        Realm realm = Realm.getInstance(context);
                        realm.beginTransaction();
                        account[0] = Account.updateAccount(account[0], realm, response.getJSONObject("user"));
                        account[0].setSyncStatus(Utils.AccountSynced);
                        realm.commitTransaction();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    System.out.println(errorResponse.toString());
                    if (statusCode == 401) session.logoutUser();
                    else performSyncHelper();
                }
            });
        }


        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.syncCompletedListener();
            }
        });
    }

    private static void putOnSuccess(Account account, JSONObject response) {
        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        try {
            account = Account.updateAccount(account, realm, response.getJSONObject("user"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        account.setSyncStatus(Utils.AccountSynced);
        realm.commitTransaction();
    }
}