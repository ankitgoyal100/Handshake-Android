package com.handshake.helpers;

import android.content.Context;
import android.os.Handler;

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

    }

}
