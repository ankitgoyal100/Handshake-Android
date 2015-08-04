package com.handshake.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.handshake.Handshake.SessionManager;
import com.handshake.models.User;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by ankitgoyal on 8/3/15.
 */
public class ContactSync {
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

    public static void syncAll(final Context c, final SyncCompleted l) {
        context = c;
        listener = l;

        Realm realm = Realm.getInstance(context);
        RealmResults<User> contacts = realm.where(User.class).equalTo("isContact", true).findAll();

        realm.beginTransaction();
        for (int i = 0; i < contacts.size(); i++) {
            contacts.get(i).setSaved(false);
        }
        realm.commitTransaction();

        performSync(context, listener);
    }

    private static void performSyncHelper() {
        Realm realm = Realm.getInstance(context);
        RealmResults<User> users;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAutosync = sharedPreferences.getBoolean("autosync_preference", false);
        if (isAutosync) {
            users = realm.where(User.class).equalTo("isContact", true).equalTo("saved", false).findAll();
        } else {
            users = realm.where(User.class).equalTo("isContact", true).equalTo("saved", false).equalTo("savesToPhone", true).findAll();
        }

        for (int i = 0; i < users.size(); i++) {
            syncContactToAddressBook(users.get(i));
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.syncCompletedListener();
            }
        });
    }

    private static void syncContactToAddressBook(User user) {

    }

}
