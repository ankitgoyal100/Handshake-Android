package com.handshake.Handshake;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

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

    public static final String KEY_LAST_COPIED_GROUP = "copiedGroup";

    private static final String KEY_CONTACT_SYNCED = "contactSync";

    private static final String KEY_INTRO_SCREEN_DISPLAYED = "introScreenDisplayed";

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
        //wTODO clear db
        Realm realm = Realm.getInstance(sContext);
        realm.beginTransaction();
//        realm.where(Account.class).findAll().clear();
//        realm.where(Address.class).findAll().clear();
//        realm.where(Card.class).findAll().clear();
//        realm.where(Email.class).findAll().clear();
//        realm.where(FeedItem.class).findAll().clear();
//        realm.where(Group.class).findAll().clear();
//        realm.where(GroupMember.class).findAll().clear();
//        realm.where(Phone.class).findAll().clear();
//        realm.where(Social.class).findAll().clear();
//        realm.where(Suggestion.class).findAll().clear();
//        realm.where(User.class).findAll().clear();
        realm.commitTransaction();

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

    public void updateEmail(String email) {
        editor.putString(KEY_EMAIL, email);
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
        boolean introScreenDisplayed = getIntroScreenDisplayed();

        Intent i;
//        if (introScreenDisplayed)
        i = new Intent(sContext, IntroActivity.class);
//        else
//            i = new Intent(sContext, AppIntroActivity.class);

        // Clearing all data from Shared Preferences
        editor.clear();
        editor.putBoolean(KEY_INTRO_SCREEN_DISPLAYED, introScreenDisplayed);
        editor.apply();

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    public static Long getLastContactSynced() {
        return pref.getLong(KEY_CONTACT_SYNCED, 0);
    }

    public static void setLastContactSynced(Long time) {
        editor.putLong(KEY_CONTACT_SYNCED, time);
        editor.apply();
    }

    public static void setIntroScreenDisplayed(boolean displayed) {
        editor.putBoolean(KEY_INTRO_SCREEN_DISPLAYED, displayed);
        editor.apply();
    }

    public static boolean getIntroScreenDisplayed() {
        return pref.getBoolean(KEY_INTRO_SCREEN_DISPLAYED, false);
    }

    /**
     * Quick check for login
     * *
     */
    // Get Login State
    public boolean isLoggedIn() {
        return pref.getBoolean(sIsLogin, false);
    }

    public static void setLastCopiedGroup(String code) {
        editor.putString(KEY_LAST_COPIED_GROUP, code);
        editor.commit();
    }

    public static String getLastCopiedGroup() {
        return pref.getString(KEY_LAST_COPIED_GROUP, "");
    }
}