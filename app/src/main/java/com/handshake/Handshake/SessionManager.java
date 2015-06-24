package com.handshake.Handshake;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.handshake.models.Account;
import com.handshake.models.Card;
import com.handshake.models.FeedItem;
import com.handshake.models.Suggestion;

import io.realm.Realm;

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

    public static final String KEY_EMAIL = "email";

    // Constructor
    public SessionManager(Context context) {
        this.sContext = context;
        pref = sContext.getSharedPreferences(sPrefName, 0);
        editor = pref.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(long id, String token, String email) {
        Realm realm = Realm.getInstance(sContext);
        realm.beginTransaction();
        realm.clear(Account.class);
        realm.clear(Card.class);
        realm.clear(FeedItem.class);
        realm.clear(Suggestion.class);
        realm.commitTransaction();
        realm.close();
        // Storing login value as TRUE
        editor.putBoolean(sIsLogin, true);

        // Storing name in pref
        editor.putLong(KEY_ID, id);

        // Storing password in pref
        editor.putString(KEY_TOKEN, token);

        editor.putString(KEY_EMAIL, email);

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
    public void logoutUser() {
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

    public static long getID() {
        return pref.getLong(KEY_ID, 0);
    }

    public static String getEmail() {
        return pref.getString(KEY_EMAIL, "");
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