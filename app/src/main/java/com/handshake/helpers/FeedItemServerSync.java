package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.models.FeedItem;
import com.handshake.models.Group;
import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/17/15.
 */
public class FeedItemServerSync {
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

                syncPage(1, new SyncCompleted() {
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
        }).start();

    }

    private static void syncPage(final int page, final SyncCompleted listener) {
        RequestParams params = new RequestParams();
        params.put("page", page);

        RestClientSync.get(context, "/feed", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                try {
                    final JSONArray feedObjects = response.getJSONArray("feed");

                    if (feedObjects.length() == 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.syncCompletedListener();
                            }
                        });
                        return;
                    }

                    final JSONArray users = new JSONArray();
                    final JSONArray groups = new JSONArray();
                    for (int i = 0; i < feedObjects.length(); i++) {
                        if (feedObjects.getJSONObject(i).has("user") && !feedObjects.getJSONObject(i).isNull("user")) {
                            users.put(feedObjects.getJSONObject(i).getJSONObject("user"));
                        }
                        if (feedObjects.getJSONObject(i).has("group") && !feedObjects.getJSONObject(i).isNull("group")) {
                            groups.put(feedObjects.getJSONObject(i).getJSONObject("group"));
                        }
                    }


                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            UserServerSync.cacheUser(context, users, new UserArraySyncCompleted() {
                                @Override
                                public void syncCompletedListener(ArrayList<User> usersArray) {
                                    GroupServerSync.cacheGroup(groups, new GroupArraySyncCompleted() {
                                        @Override
                                        public void syncCompletedListener(ArrayList<Group> groupsArray) {
                                            executor.execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ArrayList<Long> userIds = new ArrayList<Long>();
                                                    for (int i = 0; i < users.length(); i++) {
                                                        try {
                                                            userIds.add(users.getJSONObject(i).getLong("id"));
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                    Realm realm = Realm.getInstance(context);
                                                    RealmResults<User> usersResults = realm.where(User.class).findAll();

                                                    HashMap<Long, User> usersMap = new HashMap<Long, User>();
                                                    for (User user : usersResults) {
                                                        if (userIds.contains(user.getUserId())) {
                                                            usersMap.put(user.getUserId(), user);
                                                        }
                                                    }

                                                    ArrayList<Long> groupIds = new ArrayList<Long>();
                                                    for (int i = 0; i < groups.length(); i++) {
                                                        try {
                                                            groupIds.add(groups.getJSONObject(i).getLong("id"));
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                    RealmResults<Group> groupsResults = realm.where(Group.class).findAll();

                                                    HashMap<Long, Group> groupsMap = new HashMap<Long, Group>();
                                                    for (Group group : groupsResults) {
                                                        if (groupIds.contains(group.getGroupId())) {
                                                            groupsMap.put(group.getGroupId(), group);
                                                        }
                                                    }

                                                    ArrayList<Long> feedIds = new ArrayList<Long>();
                                                    for (int i = 0; i < feedObjects.length(); i++) {
                                                        try {
                                                            feedIds.add(feedObjects.getJSONObject(i).getLong("id"));
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                    RealmResults<FeedItem> feedResults = realm.where(FeedItem.class).findAll();

                                                    HashMap<Long, FeedItem> feedMap = new HashMap<Long, FeedItem>();
                                                    for (FeedItem feed : feedResults) {
                                                        if (feedIds.contains(feed.getFeedId())) {
                                                            feedMap.put(feed.getFeedId(), feed);
                                                        }
                                                    }

                                                    for (int i = 0; i < feedObjects.length(); i++) {
                                                        try {
                                                            FeedItem feedItem;
                                                            if (!feedMap.containsKey(feedObjects.getJSONObject(i).getLong("id"))) {
                                                                realm.beginTransaction();
                                                                feedItem = realm.createObject(FeedItem.class);
                                                                realm.commitTransaction();
                                                            } else {
                                                                feedItem = feedMap.get(feedObjects.getJSONObject(i).getLong("id"));
                                                            }

                                                            realm.beginTransaction();
                                                            feedItem = FeedItem.updateFeedItem(feedItem, realm, feedObjects.getJSONObject(i));

                                                            if (feedItem != null && feedItem.isValid() && feedObjects.getJSONObject(i).has("user") && !feedObjects.getJSONObject(i).isNull("user")) {
                                                                feedItem.setUser(usersMap.get(
                                                                        feedObjects.getJSONObject(i).getJSONObject("user").getLong("id")));

                                                                if (usersMap.containsKey(feedObjects.getJSONObject(i).getJSONObject("user").getLong("id")))
                                                                    usersMap.get(
                                                                            feedObjects.getJSONObject(i).getJSONObject("user").getLong("id")).getFeedItems().add(realm.copyToRealm(feedItem));
                                                            }
                                                            if (feedItem != null && feedItem.isValid() && feedObjects.getJSONObject(i).has("group") && !feedObjects.getJSONObject(i).isNull("group")) {
                                                                feedItem.setGroup(groupsMap.get(
                                                                        feedObjects.getJSONObject(i).getJSONObject("group").getLong("id")));

                                                                if (groupsMap.containsKey(feedObjects.getJSONObject(i).getJSONObject("group").getLong("id")))
                                                                    groupsMap.get(
                                                                            feedObjects.getJSONObject(i).getJSONObject("group").getLong("id")).getFeedItems().add(realm.copyToRealm(feedItem));
                                                            }
                                                            realm.commitTransaction();
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }

                                                    if (page == 1) {
                                                        RealmResults<FeedItem> results = realm.where(FeedItem.class).findAll();
                                                        for (int i = 0; i < results.size(); i++) {
                                                            if (!feedIds.contains(results.get(i).getFeedId())) {
                                                                realm.beginTransaction();
                                                                results.get(i).removeFromRealm();
                                                                realm.commitTransaction();
                                                            }
                                                        }
                                                    }

                                                    realm.close();

                                                    if (feedObjects.length() < 100) {
                                                        handler.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                listener.syncCompletedListener();
                                                            }
                                                        });
                                                        return;
                                                    }

                                                    syncPage(page + 1, listener);
                                                }
                                            });
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
