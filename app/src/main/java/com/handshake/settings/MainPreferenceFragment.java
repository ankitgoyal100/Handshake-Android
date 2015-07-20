package com.handshake.settings;

import android.os.Bundle;

import com.handshake.Handshake.R;

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
    }

}
