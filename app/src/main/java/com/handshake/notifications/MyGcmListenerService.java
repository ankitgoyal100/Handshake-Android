/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.handshake.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GcmListenerService;
import com.handshake.Handshake.ContactUserProfileActivity;
import com.handshake.Handshake.GenericUserProfileActivity;
import com.handshake.Handshake.MainActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.helpers.ContactSync;
import com.handshake.helpers.FeedItemServerSync;
import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.helpers.UserArraySyncCompleted;
import com.handshake.helpers.UserServerSync;
import com.handshake.models.Group;
import com.handshake.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.Realm;

public class MyGcmListenerService extends GcmListenerService {

    // private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, final Bundle data) {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) return;

        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */

        try {
            JSONArray users = new JSONArray();
            if (!data.containsKey("user")) return;
            final JSONObject user = new JSONObject(data.getString("user"));
            users.put(user);
            UserServerSync.cacheUser(getApplicationContext(), users, new UserArraySyncCompleted() {
                @Override
                public void syncCompletedListener(final ArrayList<User> users) {
                    if (users.size() == 0) {
                        sendNotification(data, 0, false);
                        return;
                    }

                    final long userId = users.get(0).getUserId();
                    final boolean isContact = users.get(0).isContact();

                    FeedItemServerSync.performSync(getApplicationContext(), new SyncCompleted() {
                        @Override
                        public void syncCompletedListener() {
                            ContactSync.performSync(getApplicationContext(), new SyncCompleted() {
                                @Override
                                public void syncCompletedListener() {
                                    if (data.containsKey("group_id")) {
                                        Realm realm = Realm.getInstance(getApplicationContext());
                                        Group group = realm.where(Group.class).equalTo("groupId", Long.parseLong(data.getString("group_id"))).findFirst();
                                        GroupServerSync.loadGroupMembers(group);
                                        realm.close();
                                    }

                                    sendNotification(data, userId, isContact);
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
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     */
    private void sendNotification(final Bundle data, long userId, boolean isContact) {
        Intent intent;
        if (userId == 0) {
            intent = new Intent(this, MainActivity.class);
        } else if (isContact) {
            intent = new Intent(this, ContactUserProfileActivity.class);
        } else {
            intent = new Intent(this, GenericUserProfileActivity.class);
        }
        intent.putExtra("userId", userId);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("Handshake")
                .setContentText(data.getString("message"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
