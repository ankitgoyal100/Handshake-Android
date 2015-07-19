package com.handshake.Handshake;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.handshake.helpers.UserArraySyncCompleted;
import com.handshake.helpers.UserServerSync;
import com.handshake.listview.ContactAdapter;
import com.handshake.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;


public class ContactActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;
    public Context context = this;

    RealmResults<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        changeColor(getResources().getColor(R.color.orange));

        final ListView list = (ListView) findViewById(R.id.list);

        if (getIntent().hasExtra("userId") && getIntent().getLongExtra("userId", -1) != SessionManager.getID()) {
            final long userId = getIntent().getLongExtra("userId", SessionManager.getID());

            RestClientAsync.get(context, "/users/" + userId + "/" + getIntent().getStringExtra("type"), new RequestParams(), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        UserServerSync.cacheUser(context, response.getJSONArray(getIntent().getStringExtra("type")), new UserArraySyncCompleted() {
                            @Override
                            public void syncCompletedListener(final ArrayList<User> u) {
                                final ArrayList<Long> userIds = new ArrayList<Long>();
                                for (int i = 0; i < u.size(); i++)
                                    userIds.add(u.get(i).getUserId());

                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Realm realm = Realm.getInstance(context);
                                        RealmQuery<User> query = realm.where(User.class);
                                        query.equalTo("userId", -1);

                                        for (int i = 0; i < userIds.size(); i++) {
                                            query.or().equalTo("userId", userIds.get(i));
                                        }

                                        users = query.findAll();

                                        ContactAdapter myAdapter = new ContactAdapter(context, users, true);
                                        list.setAdapter(myAdapter);
                                    }
                                });
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (errorResponse == null) return;
                }
            });
        } else {
            Realm realm = Realm.getInstance(this);
            users = realm.where(User.class).equalTo("isContact", true).findAll();
            users.sort("firstName", true);

            ContactAdapter myAdapter = new ContactAdapter(this, users, true);
            list.setAdapter(myAdapter);
        }
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
