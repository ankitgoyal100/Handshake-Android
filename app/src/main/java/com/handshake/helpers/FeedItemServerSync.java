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
                        System.out.println("Sync completed listener");
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
        executor.execute(new Runnable() {
            @Override
            public void run() {
                RequestParams params = new RequestParams();
                params.put("page", page);

                RestClientSync.get(context, "/feed", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        System.out.println(response.toString());

                        try {

                            final JSONArray feedObjects = response.getJSONArray("feed");

                            if (feedObjects.length() == 0) return;

                            final JSONArray users = new JSONArray();
                            final JSONArray groups = new JSONArray();
                            for (int i = 0; i < feedObjects.length(); i++) {
                                if (feedObjects.getJSONObject(i).has("user"))
                                    users.put(feedObjects.getJSONObject(i).getJSONObject("user"));
                                if (feedObjects.getJSONObject(i).has("group"))
                                    groups.put(feedObjects.getJSONObject(i).getJSONObject("group"));
                            }

                            UserServerSync.cacheUser(context, users, new UserArraySyncCompleted() {
                                @Override
                                public void syncCompletedListener(ArrayList<User> usersArray) {
                                    GroupServerSync.cacheGroup(groups, new GroupArraySyncCompleted() {
                                        @Override
                                        public void syncCompletedListener(ArrayList<Group> groupsArray) {
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
                                                    if (feedObjects.getJSONObject(i).has("user"))
                                                        feedItem.setUser(usersMap.get(
                                                                feedObjects.getJSONObject(i).getJSONObject("user").getLong("id")));
                                                    if (feedObjects.getJSONObject(i).has("group"))
                                                        feedItem.setUser(usersMap.get(
                                                                feedObjects.getJSONObject(i).getJSONObject("group").getLong("id")));
                                                    realm.commitTransaction();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            if (page == 1) {
                                                RealmResults<FeedItem> results = realm.where(FeedItem.class).findAll();
                                                for (FeedItem feedItem : results) {
                                                    if (!feedIds.contains(feedItem.getFeedId())) {
                                                        realm.beginTransaction();
                                                        feedItem.removeFromRealm();
                                                        realm.commitTransaction();
                                                    }
                                                }
                                            }

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
        });
    }
}
