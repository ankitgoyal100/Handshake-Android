package com.handshake.Handshake;

import android.app.Activity;
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

import com.handshake.models.Account;
import com.handshake.views.TextViewCustomFont;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;


public class LoginActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;
    private Context context = this;

    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(context);

        changeColor(getResources().getColor(R.color.orange));

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

                if (email.getText().toString().equals("")) {
                    Toast.makeText(context, "Email can't be blank.", Toast.LENGTH_LONG).show();
                } else if (password.getText().toString().equals("")) {
                    Toast.makeText(context, "Password can't be blank.", Toast.LENGTH_LONG).show();
                }

                RequestParams params = new RequestParams();
                params.add("email", email.getText().toString());
                params.add("password", password.getText().toString());

                final ProgressDialog dialog = ProgressDialog.show(context, "", "Logging in...", true);
                login.setEnabled(false);

                RestClientAsync.post(context, "/tokens", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        login.setEnabled(true);
                        dialog.cancel();

                        try {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            prefs.edit().putBoolean("isFirstRun", true).apply();

                            session.createLoginSession(response.getJSONObject("user").getLong("id"), response.getString("auth_token"), email.getText().toString());

                            Realm realm = Realm.getInstance(context);
                            realm.beginTransaction();
                            Account account = realm.createObject(Account.class);
                            account = Account.updateAccount(account, realm, response.getJSONObject("user"));
                            realm.commitTransaction();

                            realm.close();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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

        TextViewCustomFont forgotPassword = (TextViewCustomFont) findViewById(R.id.forgot_password);
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, ForgotPasswordActivity.class);
                startActivity(i);
            }
        });
    }

    public static void forgotPassword(final Context context, final String email) {

        JSONObject emailObj = new JSONObject();
        JSONObject user = new JSONObject();

        try {
            emailObj.put("email", email);
            user.put("user", emailObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RestClientAsync.post(context, "/password", user, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Toast.makeText(context, "Instructions sent to your email.", Toast.LENGTH_LONG).show();

                SessionManager sessionManager = new SessionManager(context);
                if (!email.equals(sessionManager.getEmail()))
                    ((Activity) context).finish();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(context, "Could not send reset instructions at this time.", Toast.LENGTH_LONG).show();
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
}
