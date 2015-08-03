package com.handshake.settings;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.handshake.Handshake.R;
import com.handshake.Handshake.RestClientAsync;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

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

        final SwitchPreference notificationsPreference = (SwitchPreference) getPreferenceScreen().findPreference("enabled");
        final SwitchPreference requestsPreference = (SwitchPreference) getPreferenceScreen().findPreference("requests");
        final SwitchPreference newContactsPreference = (SwitchPreference) getPreferenceScreen().findPreference("new_contacts");
        final SwitchPreference updatedContactInfoPreference = (SwitchPreference) getPreferenceScreen().findPreference("new_contact_information");
        final SwitchPreference newGroupMembersPreference = (SwitchPreference) getPreferenceScreen().findPreference("new_group_members");
        final SwitchPreference contactJoinedHandshakePreference = (SwitchPreference) getPreferenceScreen().findPreference("contact_joined");
        final SwitchPreference suggestionsPreference = (SwitchPreference) getPreferenceScreen().findPreference("suggestions");
        final SwitchPreference newFeaturesPreference = (SwitchPreference) getPreferenceScreen().findPreference("new_features");
        final SwitchPreference offersPreference = (SwitchPreference) getPreferenceScreen().findPreference("offers");

        final ArrayList<SwitchPreference> preferences = new ArrayList<>();
        preferences.add(notificationsPreference);
        preferences.add(requestsPreference);
        preferences.add(newContactsPreference);
        preferences.add(updatedContactInfoPreference);
        preferences.add(newGroupMembersPreference);
        preferences.add(contactJoinedHandshakePreference);
        preferences.add(suggestionsPreference);
        preferences.add(newFeaturesPreference);
        preferences.add(offersPreference);

        final ProgressDialog dialog = ProgressDialog.show(getActivity(), "", "Loading preferences...", true);

        RestClientAsync.get(getActivity(), "/notifications/settings", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    dialog.cancel();
                    JSONObject settings = response.getJSONObject("settings");
                    notificationsPreference.setChecked(settings.getBoolean("enabled"));
                    requestsPreference.setChecked(settings.getBoolean("requests"));
                    newContactsPreference.setChecked(settings.getBoolean("new_contacts"));
                    updatedContactInfoPreference.setChecked(settings.getBoolean("new_contact_information"));
                    newGroupMembersPreference.setChecked(settings.getBoolean("new_group_members"));
                    contactJoinedHandshakePreference.setChecked(settings.getBoolean("contact_joined"));
                    suggestionsPreference.setChecked(settings.getBoolean("suggestions"));
                    newFeaturesPreference.setChecked(settings.getBoolean("new_features"));
                    offersPreference.setChecked(settings.getBoolean("offers"));
                    allPreferencesEnabled(preferences, notificationsPreference.isChecked());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                dialog.cancel();
                Toast.makeText(getActivity(), "An error occurred. Please try again.", Toast.LENGTH_LONG).show();
            }
        });

//        notificationsPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                ((SwitchPreference) preference).setChecked((boolean) newValue);
//                allPreferencesEnabled(preferences, (boolean) newValue);
//                updatePreference(preferences);
//                return true;
//            }
//        });

        for (int i = 0; i < preferences.size(); i++) {
            preferences.get(i).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ((SwitchPreference) preference).setChecked((boolean) newValue);
                    allPreferencesEnabled(preferences, preferences.get(0).isChecked());
                    updatePreference(preferences);
                    return true;
                }
            });
        }
    }

    private void updatePreference(final ArrayList<SwitchPreference> preferences) {
        RequestParams params = new RequestParams();
        for (int i = 0; i < preferences.size(); i++) {
            params.put(preferences.get(i).getKey(), preferences.get(i).isChecked());
        }

        RestClientAsync.put(getActivity(), "/notifications/settings", params, new JsonHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                updatePreference(preferences);
            }
        });
    }

    private void allPreferencesEnabled(ArrayList<SwitchPreference> preferences, boolean newValue) {
        for (int i = 1; i < preferences.size(); i++) {
            preferences.get(i).setEnabled(newValue);
        }
    }

}
