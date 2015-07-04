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
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.models.Group;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.EditTextCustomFont;

import java.util.Date;

import io.realm.Realm;

public class CreateEditGroupActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    private Context context = this;
    private Group group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        changeColor(getResources().getColor(R.color.orange));

        final boolean isEdit = getIntent().getBooleanExtra("isEdit", false);

        final EditTextCustomFont groupName = (EditTextCustomFont) findViewById(R.id.group_name);
        ButtonCustomFont create = (ButtonCustomFont) findViewById(R.id.create);

        if (isEdit) {
            long groupId = getIntent().getLongExtra("groupId", -1);

            Realm realm = Realm.getInstance(context);
            group = realm.where(Group.class).equalTo("groupId", groupId).findFirst();

            if (group == null) finish();

            groupName.setText(group.getName());

            create.setText("Save");
        } else {
            create.setText("Create");
        }

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Realm realm = Realm.getInstance(context);
                if (isEdit) {
                    realm.beginTransaction();
                    group.setName(groupName.getText().toString());
                    group.setSyncStatus(Utils.GroupUpdated);
                    realm.commitTransaction();

                    GroupServerSync.performSync(context, new SyncCompleted() {
                        @Override
                        public void syncCompletedListener() {

                        }
                    });

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("groupName", group.getName());
                    setResult(RESULT_OK, returnIntent);

                    CreateEditGroupActivity.this.finish();
                } else {
                    realm.beginTransaction();
                    Group newGroup = realm.createObject(Group.class);
                    newGroup.setName(groupName.getText().toString());
                    newGroup.setCreatedAt(new Date(System.currentTimeMillis()));
                    newGroup.setSyncStatus(Utils.GroupCreated);
                    realm.commitTransaction();

                    GroupServerSync.performSync(context, new SyncCompleted() {
                        @Override
                        public void syncCompletedListener() {
                        }
                    });

                    CreateEditGroupActivity.this.finish();
                }
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
