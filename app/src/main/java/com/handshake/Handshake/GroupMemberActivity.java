package com.handshake.Handshake;

import android.content.Context;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.ListView;

import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.GroupMemberAdapter;
import com.handshake.models.GroupMember;
import com.handshake.models.User;
import com.handshake.views.TextViewCustomFont;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupMemberActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;
    public Context context = this;

    private SwipeRefreshLayout swipeContainer;
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
                GroupServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        swipeContainer.setRefreshing(false);
                    }
                });
            }
        });

        realm = Realm.getInstance(this);

        long groupId = getIntent().getLongExtra("groupId", -1);

        RealmResults<GroupMember> groupMembers = realm.where(GroupMember.class).equalTo("group.groupId", groupId).findAll();

        groupMembers.sort("name", true);
        GroupMemberAdapter myAdapter = new GroupMemberAdapter(this, groupMembers, true);
        list.setAdapter(myAdapter);

        View empty = getLayoutInflater().inflate(R.layout.empty_list_view, null, false);
        TextViewCustomFont text = (TextViewCustomFont) empty.findViewById(R.id.empty_list_item);
        text.setText("No members");
        addContentView(empty, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        list.setEmptyView(empty);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GroupMember groupMember = (GroupMember) list.getItemAtPosition(position);
                Long userId = groupMember.getUser().getUserId();
                Realm realm = Realm.getInstance(context);
                User user = realm.where(User.class).equalTo("userId", userId).findFirst();

                Intent i;
                if (user.isContact()) {
                    i = new Intent(context, ContactUserProfileActivity.class);
                } else {
                    i = new Intent(context, GenericUserProfileActivity.class);
                }
                realm.close();
                i.putExtra("userId", userId);
                context.startActivity(i);
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


    @Override
    public void onResume() {
        super.onResume();
        swipeContainer.setRefreshing(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }
}
