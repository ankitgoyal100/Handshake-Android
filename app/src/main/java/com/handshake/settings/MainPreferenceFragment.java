package com.handshake.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.text.InputType;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.handshake.Handshake.LoginActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.RestClientAsync;
import com.handshake.Handshake.SessionManager;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

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

        final MaterialEditTextPreference emailPreference = (MaterialEditTextPreference) getPreferenceManager().findPreference("email_preference");
        emailPreference.setSummary(SessionManager.getEmail());
        emailPreference.setText(SessionManager.getEmail());
        emailPreference.getEditText().setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        emailPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                RequestParams params = new RequestParams();
                params.put("email", newValue.toString());

                RestClientAsync.put(getActivity(), "/account", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        SessionManager session = new SessionManager(getActivity());
                        try {
                            session.updateEmail(response.getJSONObject("user").getString("email"));
                            emailPreference.setSummary(SessionManager.getEmail());
                            emailPreference.setText(SessionManager.getEmail());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        emailPreference.setText(SessionManager.getEmail());

                        if(statusCode == 422) {
                            try {
                                Toast.makeText(getActivity(), errorResponse.getJSONArray("errors").getString(0), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(getActivity(), "Could not update email. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                return true;
            }
        });

        Preference resetPasswordPreference = getPreferenceManager().findPreference("reset_password_preference");
        resetPasswordPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialogWrapper.Builder(getActivity())
                        .setTitle("Reset password?")
                        .setMessage("You will be sent an email with reset instructions.")
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                LoginActivity.forgotPassword(getActivity(), SessionManager.getEmail());
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
