package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientAsync;
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
        SessionManager sessionManager = new SessionManager(context);
        final Account[] account = {realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst()};

        if (account[0] == null) return;

        if (account[0].getSyncStatus() == Utils.AccountUpdated) {
            RequestParams params = Account.accountToParams(account[0]);
            if (account[0].getPicture().isEmpty() && account[0].getPictureData() != null && account[0].getPictureData().length > 0) {
                params.put("picture", new ByteArrayInputStream(account[0].getPictureData()), "picture.jpg", "image/jpg");
            }

            RestClientSync.put(context, "/account", params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Realm realm = Realm.getInstance(context);
                    realm.beginTransaction();
                    try {
                        account[0] = Account.updateAccount(account[0], realm, response.getJSONObject("user"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    account[0].setSyncStatus(Utils.AccountSynced);
                    realm.commitTransaction();
                    realm.close();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (statusCode == 401) session.logoutUser();
                    else performSyncHelper();
                }
            });
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
                        realm.close();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (errorResponse == null) return;
                    if (statusCode == 401) session.logoutUser();
                    else performSyncHelper();
                }
            });
        }

        realm.close();
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.syncCompletedListener();
            }
        });
    }

    public static void sendUserLocation(final Context context) {
        GPSTracker gpsTracker = new GPSTracker(context);

        if (!gpsTracker.canGetLocation()) return;

        RequestParams params = new RequestParams();
        params.put("lat", gpsTracker.getLatitude());
        params.put("lng", gpsTracker.getLongitude());

        RestClientAsync.put(context, "/account/location", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

            }
        });

        gpsTracker.stopUsingGPS();
    }
}