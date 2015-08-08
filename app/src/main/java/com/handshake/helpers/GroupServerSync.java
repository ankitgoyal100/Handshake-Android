package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.handshake.Handshake.RestClientAsync;
import com.handshake.Handshake.RestClientSync;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.handshake.models.Group;
import com.handshake.models.GroupMember;
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
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/16/15.
 */
public class GroupServerSync {
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
        RestClientSync.get(context, "/groups", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    final JSONArray groups = response.getJSONArray("groups");

                    cacheGroup(groups, new GroupArraySyncCompleted() {
                        @Override
                        public void syncCompletedListener(ArrayList<Group> c) {
                            Realm realm = Realm.getInstance(context);
                            RealmResults<Group> requestedGroups = realm.where(Group.class).notEqualTo("syncStatus", Utils.GroupSynced).findAll();
                            Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

                            ArrayList<Long> allIDs = new ArrayList<Long>();
                            for (int i = 0; i < groups.length(); i++) {
                                try {
                                    allIDs.add(groups.getJSONObject(i).getLong("id"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            RealmResults<Group> syncedGroups = realm.where(Group.class).equalTo("syncStatus", Utils.GroupSynced).findAll();
                            for (int i = 0; i < syncedGroups.size(); i++) {
                                if (!allIDs.contains(syncedGroups.get(i).getGroupId())) {
                                    realm.beginTransaction();

                                    for (int j = 0; j < syncedGroups.get(i).getFeedItems().size(); j++) {
                                        syncedGroups.get(i).getFeedItems().get(j).removeFromRealm();
                                    }

                                    syncedGroups.get(i).removeFromRealm();
                                    realm.commitTransaction();
                                }
                            }

                            if (Looper.myLooper() == null) {
                                Looper.prepare();
                            }

                            for (int i = 0; i < requestedGroups.size(); i++) {
                                final Group group = requestedGroups.get(i);
                                if (group.getSyncStatus() == Utils.GroupCreated) {
                                    JSONObject params = new JSONObject();
                                    try {
                                        params.put("name", group.getName());
                                        JSONArray cardIds = new JSONArray();
                                        cardIds.put(account.getCards().first().getCardId());
                                        params.put("card_ids", cardIds);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    RestClientSync.post(context, "/groups", params, "application/json", new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            Realm realm = Realm.getInstance(context);
                                            realm.beginTransaction();
                                            try {
                                                Group.updateGroup(group, realm, response.getJSONObject("group"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            group.setSyncStatus(Utils.GroupSynced);
                                            realm.commitTransaction();

                                            realm.close();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                            System.out.println(errorResponse.toString());
                                        }
                                    });
                                } else if (group.getSyncStatus() == Utils.GroupUpdated) {
                                    RequestParams params = new RequestParams();
                                    params.put("name", group.getName());

                                    RestClientAsync.put(context, "/groups/" + group.getGroupId(), params, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            Realm realm = Realm.getInstance(context);
                                            realm.beginTransaction();
                                            try {
                                                Group.updateGroup(group, realm, response.getJSONObject("group"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            group.setSyncStatus(Utils.GroupSynced);
                                            realm.commitTransaction();

                                            realm.close();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                            System.out.println(errorResponse.toString());
                                        }
                                    });
                                } else if (group.getSyncStatus() == Utils.GroupDeleted) {
                                    RestClientAsync.delete(context, "/groups/" + group.getGroupId(), new RequestParams(), new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            Realm realm = Realm.getInstance(context);
                                            realm.beginTransaction();
                                            group.removeFromRealm();
                                            realm.commitTransaction();

                                            realm.close();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                            System.out.println(errorResponse.toString());
                                        }
                                    });
                                }
                            }

                            requestedGroups = realm.where(Group.class).findAll();
                            for (int i = 0; i < requestedGroups.size(); i++) {
                                Group group = requestedGroups.get(i);
                                loadGroupMembers(group);
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
                else performSyncHelper();
            }
        });
    }

    public static void cacheGroup(final JSONArray jsonArray, final GroupArraySyncCompleted listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getInstance(context);

                ArrayList<Long> allIDs = new ArrayList<Long>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        allIDs.add(jsonArray.getJSONObject(i).getLong("id"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                final HashMap<Long, Group> map = new HashMap<Long, Group>();

                // map ids to User objects
                RealmResults<Group> groups = realm.allObjects(Group.class);
                for (Group group : groups) {
                    if (allIDs.contains(group.getGroupId()))
                        map.put(group.getGroupId(), group);
                }

                // update/create users
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        Group group;
                        if (!map.containsKey(jsonArray.getJSONObject(i).getLong("id"))) {
                            realm.beginTransaction();
                            group = realm.createObject(Group.class);
                            group.setSyncStatus(Utils.GroupSynced);
                            realm.commitTransaction();
                        } else {
                            group = map.get(jsonArray.getJSONObject(i).getLong("id"));
                        }

                        if (group.isValid() && group.getSyncStatus() == Utils.GroupSynced) {
                            realm.beginTransaction();
                            group = Group.updateGroup(group, realm, jsonArray.getJSONObject(i));
                            realm.commitTransaction();
                        }

                        if (group != null)
                            map.put(jsonArray.getJSONObject(i).getLong("id"), group);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                map.clear();

                groups = realm.allObjects(Group.class);

                for (int i = 0; i < groups.size(); i++) {
                    Group group = groups.get(i);
                    if (allIDs.contains(group.getGroupId())) {
                        map.put(group.getGroupId(), group);
                    }
                }

                ArrayList<Group> orderedArray = new ArrayList<Group>();
                for (Long id : allIDs) {
                    orderedArray.add(map.get(id));
                }

                listener.syncCompletedListener(orderedArray);
                realm.close();
            }
        });
    }

    public static void loadGroupMembers(final Group group) {
        final long groupId = group.getGroupId();
        RestClientSync.get(context, "/groups/" + groupId + "/members", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    final JSONArray membersJSON = response.getJSONArray("members");
                    UserServerSync.cacheUser(context, membersJSON, new UserArraySyncCompleted() {
                        @Override
                        public void syncCompletedListener(ArrayList<User> users) {
                            Realm realm = Realm.getInstance(context);
                            Group group = realm.where(Group.class).equalTo("groupId", groupId).findFirst();

                            if (group == null) return;

                            realm.beginTransaction();
                            RealmResults<GroupMember> members = realm.where(GroupMember.class).equalTo("group.groupId", groupId).findAll();
                            members.clear();
                            for (int i = 0; i < group.getMembers().size(); i++)
                                group.getMembers().get(i).removeFromRealm();
                            realm.commitTransaction();

                            ArrayList<Long> allIDs = new ArrayList<Long>();
                            for (int i = 0; i < membersJSON.length(); i++) {
                                try {
                                    allIDs.add(membersJSON.getJSONObject(i).getLong("id"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            final HashMap<Long, User> map = new HashMap<Long, User>();

                            // map ids to User objects
                            RealmResults<User> allUsers = realm.allObjects(User.class);
                            for (User user : allUsers) {
                                if (allIDs.contains(user.getUserId()))
                                    map.put(user.getUserId(), user);
                            }

                            realm.beginTransaction();
                            RealmList<GroupMember> groupMembers = new RealmList<>();
                            for (int i = 0; i < membersJSON.length(); i++) {
                                try {
                                    User user = map.get(membersJSON.getJSONObject(i).getLong("id"));

                                    if (user == null) return;

                                    GroupMember member = realm.createObject(GroupMember.class);
                                    member.setUser(user);
                                    member.setName(user.getFirstName() + " " + user.getLastName());
                                    member.setGroup(group);

                                    groupMembers.add(member);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            group.setMembers(groupMembers);
                            realm.commitTransaction();
                            realm.close();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse == null) return;
            }
        });
    }

    public static void deleteGroup(Context context, Group group) {
        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        group.setSyncStatus(Utils.GroupDeleted);

        for (int i = 0; i < group.getFeedItems().size(); i++) {
            group.getFeedItems().get(i).removeFromRealm();
        }

        realm.commitTransaction();
        realm.close();
    }
}
