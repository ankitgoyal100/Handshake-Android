package com.handshake.settings;

import android.os.Bundle;
import android.preference.Preference;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainPreferenceFragment extends android.preference.PreferenceFragment {

    public MainPreferenceFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);

        MaterialEditTextPreference emailPreference = (MaterialEditTextPreference) getPreferenceManager().findPreference("email_preference");
        emailPreference.setSummary(SessionManager.getEmail());
        emailPreference.setText(SessionManager.getEmail());

        Preference autosyncPreference = (Preference) getPreferenceManager().findPreference("autosync_preference");
    }

}
