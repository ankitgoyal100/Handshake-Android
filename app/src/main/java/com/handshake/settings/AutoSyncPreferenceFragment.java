package com.handshake.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;

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

        final SwitchPreference autosyncPreference = (SwitchPreference) getPreferenceScreen().findPreference("autosync_preference");
        final SwitchPreference overwriteNamesPreference = (SwitchPreference) getPreferenceScreen().findPreference("overwrite_names_preference");
        final SwitchPreference overwritePicturesPreference = (SwitchPreference) getPreferenceScreen().findPreference("overwrite_pictures_preference");

        autosyncPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ((SwitchPreference) preference).setChecked((boolean) newValue);
                overwriteNamesPreference.setEnabled((boolean) newValue);
                overwritePicturesPreference.setEnabled((boolean) newValue);
                return true;
            }
        });

    }
}
