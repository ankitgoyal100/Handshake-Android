package com.handshake.Handshake;

import android.content.Context;

import com.handshake.models.User;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 6/14/15.
 */
public class UserServerSync {
    private static Executor executor = Executors.newSingleThreadExecutor();

    public static void createUser(final Context context, final JSONArray contacts, final UserSyncCompleted listener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getInstance(context);

                ArrayList<Long> allIDs = new ArrayList<Long>();
                for (int i = 0; i < contacts.length(); i++) {
                    try {
                        allIDs.add(contacts.getJSONObject(i).getLong("id"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                final HashMap<Long, User> map = new HashMap<Long, User>();

                // map ids to User objects
                RealmResults<User> users = realm.allObjects(User.class);
                for (User user : users) {
                    if (allIDs.contains(user.getUserId()))
                        map.put(user.getUserId(), user);
                }

                System.out.println("Map: " + map.size());

                allIDs.clear();

                // update/create users
                for (int i = 0; i < contacts.length(); i++) {
                    try {
                        User user;
                        if (!map.containsKey(contacts.getJSONObject(i).getLong("id"))) {
                            realm.beginTransaction();
                            user = realm.createObject(User.class);
                            user.setSyncStatus(Utils.userSynced);
                            realm.commitTransaction();
                        } else {
                            user = map.get(contacts.getJSONObject(i).getLong("id"));
                        }

                        if (user.getSyncStatus() == Utils.userSynced) {
                            realm.beginTransaction();
                            user = User.updateContact(user, realm, contacts.getJSONObject(i));
                            realm.commitTransaction();
                        }

                        allIDs.add(user.getUserId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                map.clear();

                users = realm.allObjects(User.class);
                for (User user : users) {
                    if (allIDs.contains(user.getUserId()))
                        map.put(user.getUserId(), user);
                }

                ArrayList<User> orderedArray = new ArrayList<User>();
                for(long id : allIDs) {
                    orderedArray.add(map.get(id));
                }

                listener.syncCompletedListener(orderedArray);
            }
        });
    }
}
