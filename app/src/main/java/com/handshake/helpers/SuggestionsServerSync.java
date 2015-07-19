package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.models.Suggestion;
import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/17/15.
 */
public class SuggestionsServerSync {
    private static Handler handler = new Handler();

    private static SessionManager session;
    private static Context context;
    private static SyncCompleted listener;

    private static Executor executor = Executors.newSingleThreadExecutor();

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
        RestClientSync.get(context, "/suggestions", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    final JSONArray suggestionsArray = response.getJSONArray("suggestions");
                    UserServerSync.cacheUser(context, suggestionsArray, new UserArraySyncCompleted() {
                        @Override
                        public void syncCompletedListener(ArrayList<User> users) {
                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    ArrayList<Long> userIds = new ArrayList<Long>();
                                    for (int i = 0; i < suggestionsArray.length(); i++) {
                                        try {
                                            userIds.add(suggestionsArray.getJSONObject(i).getLong("id"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    Realm realm = Realm.getInstance(context);
                                    RealmResults<User> userRealmResults = realm.where(User.class).findAll();

                                    for (int i = 0; i < userRealmResults.size(); i++) {
                                        if (userIds.contains(userRealmResults.get(i).getUserId())) {
                                            if (userRealmResults.get(i).getSuggestion() != null)
                                                continue;

                                            realm.beginTransaction();
                                            Suggestion suggestion = realm.createObject(Suggestion.class);
                                            suggestion.setUser(userRealmResults.get(i));
                                            realm.commitTransaction();
                                        }
                                    }

                                    RealmResults<Suggestion> suggestionRealmResults = realm.where(Suggestion.class).findAll();
                                    for (int i = 0; i < suggestionRealmResults.size(); i++) {
                                        if (!userIds.contains(suggestionRealmResults.get(i).getUser().getUserId())) {
                                            realm.beginTransaction();
                                            suggestionRealmResults.get(i).removeFromRealm();
                                            realm.commitTransaction();
                                        }
                                    }

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.syncCompletedListener();
                                        }
                                    });
                                }
                            });
                        }
                    });
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
}
