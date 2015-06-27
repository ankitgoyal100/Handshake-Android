package com.handshake.Handshake;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.models.Group;

import java.util.Date;

import io.realm.Realm;

public class CreateGroupActivity extends ActionBarActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        changeColor(getResources().getColor(R.color.orange));

//        - (void)groupEdited:(Group *)group {
//            self.nameLabel.text = group.name;
//            group.syncStatus = @(GroupUpdated);
//
//            [GroupServerSync sync];
//        }

        final EditTextCustomFont groupName = (EditTextCustomFont) findViewById(R.id.group_name);
        ButtonCustomFont create = (ButtonCustomFont) findViewById(R.id.create);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Realm realm = Realm.getInstance(context);
                realm.beginTransaction();
                Group group = realm.createObject(Group.class);
                group.setName(groupName.getText().toString());
                group.setCreatedAt(new Date(System.currentTimeMillis()));
                group.setSyncStatus(Utils.GroupCreated);
                realm.commitTransaction();

                GroupServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        System.out.println("Create group sync listener completed");
                    }
                });

                CreateGroupActivity.this.finish();
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
