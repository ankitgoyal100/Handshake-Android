package com.handshake.Handshake;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.handshake.helpers.CardServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.models.Account;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;


public class SignUpActivity extends AppCompatActivity {

    public static boolean cardSyncCompleted = false;
    private final Handler handler = new Handler();
    SessionManager session;
    private Drawable oldBackground = null;
    private Context context = this;
    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            getSupportActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            handler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            handler.removeCallbacks(what);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        session = new SessionManager(context);

        changeColor(getResources().getColor(R.color.orange));

        final EditText firstName = (EditText) findViewById(R.id.first_name);
        final EditText lastName = (EditText) findViewById(R.id.last_name);
        final EditText email = (EditText) findViewById(R.id.email);
        final EditText password = (EditText) findViewById(R.id.password);
        final Button login = (Button) findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!MainActivity.isConnected(context)) {
                    Toast.makeText(context, "No internet connection.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (firstName.getText().toString().equals("")) {
                    Toast.makeText(context, "First name can't be blank.", Toast.LENGTH_LONG).show();
                } else if (lastName.getText().toString().equals("")) {
                    Toast.makeText(context, "Last name can't be blank.", Toast.LENGTH_LONG).show();
                } else if (email.getText().toString().equals("")) {
                    Toast.makeText(context, "Email can't be blank.", Toast.LENGTH_LONG).show();
                } else if (password.getText().toString().equals("")) {
                    Toast.makeText(context, "Password can't be blank.", Toast.LENGTH_LONG).show();
                } else if (password.getText().toString().length() < 8) {
                    Toast.makeText(context, "Password is too short.", Toast.LENGTH_LONG).show();
                }

                RequestParams params = new RequestParams();
                params.add("first_name", firstName.getText().toString());
                params.add("last_name", lastName.getText().toString());
                params.add("email", email.getText().toString());
                params.add("password", password.getText().toString());

                login.setEnabled(false);
                final ProgressDialog dialog = ProgressDialog.show(context, "", "Signing up...", true);

                RestClientAsync.post(context, "/account", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        login.setEnabled(true);
                        dialog.cancel();

                        RequestParams params = new RequestParams();
                        params.add("email", email.getText().toString());
                        params.add("password", password.getText().toString());

                        RestClientAsync.post(context, "/tokens", params, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                                    prefs.edit().putBoolean("isFirstRun", true).apply();

                                    session.createLoginSession(response.getJSONObject("user").getLong("id"), response.getString("auth_token"),
                                            email.getText().toString());

                                    Realm realm = Realm.getInstance(context);
                                    realm.beginTransaction();
                                    Account account = realm.createObject(Account.class);
                                    account = Account.updateAccount(account, realm, response.getJSONObject("user"));
                                    realm.commitTransaction();
                                    realm.close();

                                    Intent intent = new Intent(SignUpActivity.this, GetStartedActivity.class);
                                    intent.putExtra("first_name", firstName.getText().toString());
                                    startActivity(intent);

                                    CardServerSync.performSync(context, new SyncCompleted() {
                                        @Override
                                        public void syncCompletedListener() {
                                            cardSyncCompleted = true;
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                login.setEnabled(true);
                                dialog.cancel();

                                try {
                                    Toast.makeText(context, errorResponse.getJSONArray("errors").getString(0), Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        login.setEnabled(true);
                        dialog.cancel();

                        if (errorResponse == null) {
                            Toast.makeText(context, "There was an error. Please try again.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        try {
                            Toast.makeText(context, errorResponse.getJSONArray("errors").getString(0), Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public void changeColor(int newColor) {
        // change ActionBar color just if an ActionBar is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            Drawable colorDrawable = new ColorDrawable(newColor);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{colorDrawable});

            if (oldBackground == null) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    ld.setCallback(drawableCallback);
                } else {
                    getSupportActionBar().setBackgroundDrawable(ld);
                }

            } else {

                TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, ld});

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    td.setCallback(drawableCallback);
                } else {
                    getSupportActionBar().setBackgroundDrawable(td);
                }

                td.startTransition(200);

            }

            oldBackground = ld;

            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(true);

        }
    }
}
