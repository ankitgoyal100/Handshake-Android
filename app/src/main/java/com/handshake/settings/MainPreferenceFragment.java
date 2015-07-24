package com.handshake.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.handshake.Handshake.LoginActivity;
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

        Preference resetPasswordPreference = getPreferenceManager().findPreference("reset_password_preference");
        resetPasswordPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LoginActivity.forgotPassword(getActivity());
                return true;
            }
        });

        Preference logoutPreference = getPreferenceManager().findPreference("logout_preference");
        logoutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialogWrapper.Builder(getActivity())
                        .setMessage("Are you sure?")
                        .setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SessionManager session = new SessionManager(getActivity());
                                session.logoutUser();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();

                return true;
            }
        });

        Preference facebookPreference = (Preference) getPreferenceManager().findPreference("facebook_preference");
        facebookPreference.setSummary(SessionManager.getFBName());
        facebookPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //TODO: fb login
                return true;
            }
        });
    }

}
