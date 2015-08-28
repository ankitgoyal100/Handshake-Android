package com.handshake.Handshake;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by ankitgoyal on 8/28/15.
 */
public class HandshakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        System.out.println("===Setting isFirstRun===");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("isFirstRun", true).apply();
    }
}
