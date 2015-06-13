package com.handshake.Handshake;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SessionManager {
    // Shared Preferences
    static SharedPreferences pref;

    // Editor for Shared preferences
    static Editor editor;

    // Context
    static Context sContext;

    // Sharedpref file name
    private static final String sPrefName = "HandshakePref";

    // All Shared Preferences Keys
    private static final String sIsLogin = "IsLoggedIn";

    // User name (make variable public to access from outside)
    public static final String KEY_ID = "id";

    // Password (make variable public to access from outside)
    public static final String KEY_TOKEN = "token";

    // Constructor
    public SessionManager(Context context) {
        this.sContext = context;
        pref = sContext.getSharedPreferences(sPrefName, 0);
        editor = pref.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String id, String token) {
        // Storing login value as TRUE
        editor.putBoolean(sIsLogin, true);

        // Storing name in pref
        editor.putString(KEY_ID, id);

        // Storing password in pref
        editor.putString(KEY_TOKEN, token);

        // commit changes
        editor.apply();
    }

    /**
     * Check login method wil check user login status If false it will redirect user to login page Else won't do anything
     */
    public void checkLogin() {
        // Check login status
        if (!this.isLoggedIn()) {
            logoutUser();
        }

    }

    /**
     * Clear session details
     */
    private void logoutUser() {
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.apply();

        // After logout redirect user to Loing Activity
        Intent i = new Intent(sContext, IntroActivity.class);
        // Closing all the Activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add new Flag to start new Activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Staring Login Activity
        ((Activity) sContext).finish();
        sContext.startActivity(i);
    }

    public static String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    /**
     * Quick check for login
     * *
     */
    // Get Login State
    public boolean isLoggedIn() {
        return pref.getBoolean(sIsLogin, false);
    }

}