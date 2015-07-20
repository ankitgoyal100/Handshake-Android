package com.handshake.settings;

import android.os.Bundle;

import com.handshake.Handshake.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class NotificationsPreferenceFragment extends android.preference.PreferenceFragment {

    public NotificationsPreferenceFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_notifications);
    }

}
