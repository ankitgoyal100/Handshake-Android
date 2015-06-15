package com.handshake;

import android.content.Context;

import com.handshake.Handshake.Utils;
import com.handshake.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import io.realm.Realm;

/**
 * Created by ankitgoyal on 6/14/15.
 */
public class UserServerSync {
    private static LinkedList<JSONArray> queue = new LinkedList<>();

    private static void createUser(Context context, JSONArray contacts) {
        Realm realm = Realm.getInstance(context);

        final HashMap<Integer, JSONObject> map = new HashMap<Integer, JSONObject>();

        try {
            for (int i = 0; i < contacts.length(); i++) {
                map.put(contacts.getJSONObject(i).getInt("id"), contacts.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("Map: " + map.size());

        // update/delete contact and remove from list
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            final Integer key = (Integer) it.next();
            User result = realm.where(User.class).equalTo("userId", key).findFirst();
            if (result != null) {
                if (result.getSyncStatus() != Utils.userDeleted) {
                    realm.beginTransaction();

                    result = User.updateContact(result, realm, map.get(key));
                    result.setSyncStatus(Utils.userSynced);
                    try {
                        result.setContactUpdated(Utils.formatDate(map.get(key).getString("contact_updated")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    realm.commitTransaction();

                }
                it.remove();
            }
        }
        System.out.println("Map after updating contacts: " + map.size());

        // all left over are new contacts unless they are deleted
        for (final int key : map.keySet()) {
            try {
                if (!map.get(key).getBoolean("is_contact")) {
                    continue;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            realm.beginTransaction();

            User user = realm.createObject(User.class);
            user = User.updateContact(user, realm, map.get(key));
            user.setSyncStatus(Utils.userSynced);
            try {
                user.setContactUpdated(Utils.formatDate(map.get(key).getString("contact_updated")));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("User added: " + user.toString());

            realm.commitTransaction();
        }
    }

    public static void addToQueue(JSONArray json) {
        queue.add(json);
    }

    public static void runQueue(Context context) {
        while(!queue.isEmpty()) {
            createUser(context, queue.remove());
        }
    }
}
