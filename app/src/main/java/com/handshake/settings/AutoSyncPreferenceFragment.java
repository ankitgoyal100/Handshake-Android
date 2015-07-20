package com.handshake.settings;

import android.os.Bundle;

import com.handshake.Handshake.R;

/**
 * Created by ankitgoyal on 7/20/15.
 */
public class AutoSyncPreferenceFragment extends android.preference.PreferenceFragment {

    public AutoSyncPreferenceFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_autosync);
    }
}
