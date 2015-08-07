package com.handshake.Handshake;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.handshake.helpers.FeedItemServerSync;
import com.handshake.helpers.GroupArraySyncCompleted;
import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.models.Account;
import com.handshake.models.Group;
import com.handshake.views.EditTextCustomFont;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.Realm;

public class JoinGroupActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    SessionManager session;

    private Context context = this;
    private EditTextCustomFont et1;
    private EditTextCustomFont et2;
    private EditTextCustomFont et3;
    private EditTextCustomFont et4;
    private EditTextCustomFont et5;
    private EditTextCustomFont et6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        session = new SessionManager(this);
        session.checkLogin();

        et1 = (EditTextCustomFont) findViewById(R.id.editText1);
        et2 = (EditTextCustomFont) findViewById(R.id.editText2);
        et3 = (EditTextCustomFont) findViewById(R.id.editText3);
        et4 = (EditTextCustomFont) findViewById(R.id.editText4);
        et5 = (EditTextCustomFont) findViewById(R.id.editText5);
        et6 = (EditTextCustomFont) findViewById(R.id.editText6);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String code = Utils.getCodes(context, clipboard.getPrimaryClip());
        if (code != "" && code != SessionManager.getLastCopiedGroup()) {
            checkCode(code);
        }

        Button join = (Button) findViewById(R.id.join);
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et1.getText().toString().length() == 0 ||
                        et2.getText().toString().length() == 0 ||
                        et3.getText().toString().length() == 0 ||
                        et4.getText().toString().length() == 0 ||
                        et5.getText().toString().length() == 0 ||
                        et6.getText().toString().length() == 0) {
                    Toast.makeText(context, "Please enter in a complete code.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String code = (et1.getText().toString().charAt(0) + "") + (et2.getText().toString().charAt(0) + "") +
                        (et3.getText().toString().charAt(0) + "") + (et4.getText().toString().charAt(0) + "") +
                        (et5.getText().toString().charAt(0) + "") + (et6.getText().toString().charAt(0) + "");

                Realm realm = Realm.getInstance(context);
                Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

                JSONArray cardIds = new JSONArray();
                cardIds.put(account.getCards().first().getCardId());

                JSONObject jsonParams = new JSONObject();
                try {
                    jsonParams.put("code", code.toLowerCase());
                    jsonParams.put("card_ids", cardIds);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RestClientAsync.post(context, "/groups/join", jsonParams, "application/json", new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            JSONObject groupJSONObject = response.getJSONObject("group");
                            JSONArray jsonArray = new JSONArray();
                            jsonArray.put(groupJSONObject);
                            GroupServerSync.cacheGroup(jsonArray, new GroupArraySyncCompleted() {
                                @Override
                                public void syncCompletedListener(ArrayList<Group> groups) {
                                    Group group = groups.get(0);
                                    GroupServerSync.loadGroupMembers(group);
                                    FeedItemServerSync.performSync(context, new SyncCompleted() {
                                        @Override
                                        public void syncCompletedListener() {
                                        }
                                    });
                                }
                            });

                            Toast.makeText(context, "Successfully joined group.", Toast.LENGTH_SHORT).show();
                            JoinGroupActivity.this.finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        if (errorResponse == null) return;
                        System.out.println(errorResponse.toString());
                        if (statusCode == 401) session.logoutUser();
                        else if (statusCode == 401)
                            Toast.makeText(context, "The code you entered does not match any existing groups.", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(context, "Could not join group at this time. Please try again later.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        et1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (et1.getText().toString().length() == 1) et2.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (et2.getText().toString().length() == 1) et3.requestFocus();
                else if (et2.getText().toString().length() == 0) et1.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et2.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // You can identify which key pressed buy checking keyCode value
                // with KeyEvent.KEYCODE_
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (et2.getText().length() == 0) {
                        et1.setText("");
                        et1.requestFocus();
                    }
                }
                return false;
            }
        });

        et3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (et3.getText().toString().length() == 1) et4.requestFocus();
                else if (et3.getText().toString().length() == 0) et2.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et3.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // You can identify which key pressed buy checking keyCode value
                // with KeyEvent.KEYCODE_
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (et3.getText().length() == 0) {
                        et2.setText("");
                        et2.requestFocus();
                    }
                }
                return false;
            }
        });

        et4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (et4.getText().toString().length() == 1) et5.requestFocus();
                else if (et4.getText().toString().length() == 0) et3.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et4.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // You can identify which key pressed buy checking keyCode value
                // with KeyEvent.KEYCODE_
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (et4.getText().length() == 0) {
                        et3.setText("");
                        et3.requestFocus();
                    }
                }
                return false;
            }
        });

        et5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (et5.getText().toString().length() == 1) et6.requestFocus();
                else if (et5.getText().toString().length() == 0) et4.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et5.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // You can identify which key pressed buy checking keyCode value
                // with KeyEvent.KEYCODE_
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (et5.getText().length() == 0) {
                        et4.setText("");
                        et4.requestFocus();
                    }
                }
                return false;
            }
        });

        et6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (et6.getText().toString().length() == 0) et5.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        et6.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // You can identify which key pressed buy checking keyCode value
                // with KeyEvent.KEYCODE_
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (et6.getText().length() == 0) {
                        et5.setText("");
                        et5.requestFocus();
                    }
                }
                return false;
            }
        });

        changeColor(getResources().getColor(R.color.orange));
    }

    private void checkCode(final String code) {
        Realm realm = Realm.getInstance(context);
        Group group = realm.where(Group.class).equalTo("code", code).findFirst();

        if (group != null) {
            return;
        }

        RestClientAsync.get(context, "/groups/find/" + code, new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                SessionManager.setLastCopiedGroup(code);
                new AlertDialogWrapper.Builder(context)
                        .setTitle("Paste code?")
                        .setMessage("Would you like to paste the copied group code?")
                        .setPositiveButton("Paste", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                et1.setText(code.toUpperCase().charAt(0) + "");
                                et2.setText(code.toUpperCase().charAt(1) + "");
                                et3.setText(code.toUpperCase().charAt(2) + "");
                                et4.setText(code.toUpperCase().charAt(3) + "");
                                et5.setText(code.toUpperCase().charAt(4) + "");
                                et6.setText(code.toUpperCase().charAt(5) + "");

                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
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
