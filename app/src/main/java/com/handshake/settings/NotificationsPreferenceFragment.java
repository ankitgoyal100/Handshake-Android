package com.handshake.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;

import com.handshake.Handshake.R;

import java.util.ArrayList;

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

        final ArrayList<SwitchPreference> preferences = new ArrayList<>();
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("requests_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("new_contacts_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("updated_contact_information_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("new_group_members_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("contact_joined_handshake_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("suggestions_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("new_features_preference"));
        preferences.add((SwitchPreference) getPreferenceScreen().findPreference("offers_preference"));

        SwitchPreference notificationsPreference = (SwitchPreference) getPreferenceScreen().findPreference("notifications_enabled_preference");
        notificationsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                for (int i = 0; i < preferences.size(); i++) {
                    preferences.get(i).setEnabled((boolean) newValue);
                }

                return true;
            }
        });
    }

}
