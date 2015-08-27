package com.handshake.Handshake;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.handshake.helpers.ContactServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.helpers.UserArraySyncCompleted;
import com.handshake.helpers.UserServerSync;
import com.handshake.listview.ContactAdapter;
import com.handshake.models.User;
import com.handshake.views.TextViewCustomFont;
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

    private SwipeRefreshLayout swipeContainer;

    RealmResults<User> users;
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        changeColor(getResources().getColor(R.color.orange));

        final ListView list = (ListView) findViewById(R.id.list);

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(R.color.orange);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ContactServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        MainActivity.contactSyncCompleted = true;
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        });

        SessionManager sessionManager = new SessionManager(context);
        if (getIntent().hasExtra("userId") && getIntent().getLongExtra("userId", -1) != sessionManager.getID()) {
            final long userId = getIntent().getLongExtra("userId", sessionManager.getID());

            RestClientAsync.get(context, "/users/" + userId + "/" + getIntent().getStringExtra("type"), new RequestParams(), new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
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
                                                realm = Realm.getInstance(context);
                                                RealmQuery<User> query = realm.where(User.class);
                                                query.equalTo("userId", -1);

                                                for (int i = 0; i < userIds.size(); i++) {
                                                    query.or().equalTo("userId", userIds.get(i));
                                                }

                                                users = query.findAll();

                                                users.sort("firstName", true);

                                                ContactAdapter myAdapter = new ContactAdapter(context, users, true);
                                                list.setAdapter(myAdapter);

                                                View empty = getLayoutInflater().inflate(R.layout.empty_list_view, null, false);
                                                TextViewCustomFont text = (TextViewCustomFont) empty.findViewById(R.id.empty_list_item);
                                                text.setText("No contacts");
                                                addContentView(empty, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                                list.setEmptyView(empty);
                                            }
                                        });
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    if (errorResponse == null) return;
                }
            });
        } else {
            realm = Realm.getInstance(this);
            users = realm.where(User.class).equalTo("isContact", true).findAll();
            users.sort("firstName", true);

            ContactAdapter myAdapter = new ContactAdapter(this, users, true);
            list.setAdapter(myAdapter);

            View empty = getLayoutInflater().inflate(R.layout.empty_list_view, null, false);
            TextViewCustomFont text = (TextViewCustomFont) empty.findViewById(R.id.empty_list_item);
            text.setText("No contacts");
            addContentView(empty, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            list.setEmptyView(empty);
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

    @Override
    public void onResume() {
        super.onResume();
        swipeContainer.setRefreshing(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null)
            realm.close();
    }
}
