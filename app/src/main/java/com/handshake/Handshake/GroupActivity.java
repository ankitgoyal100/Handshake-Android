package com.handshake.Handshake;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.handshake.helpers.GroupServerSync;
import com.handshake.models.Group;
import com.handshake.models.User;
import com.handshake.views.Icon;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import io.realm.Realm;

public class GroupActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    private Context context = this;

    private Group group;
    private TextViewCustomFont groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        changeColor(getResources().getColor(R.color.orange));

        Long id = getIntent().getLongExtra("id", -1);
        if (id == -1) finish();

        Realm realm = Realm.getInstance(context);
        group = realm.where(Group.class).equalTo("groupId", id).findFirst();

        if (group == null) finish();

        int position = 0;

        Icon iv1 = (Icon) findViewById(R.id.imageView1);
        Icon iv2 = (Icon) findViewById(R.id.imageView2);
        Icon iv3 = (Icon) findViewById(R.id.imageView3);

        Icon[] imageViews = new Icon[3];
        imageViews[0] = iv1;
        imageViews[1] = iv2;
        imageViews[2] = iv3;

        for (int i = 0; i < group.getMembers().size(); i++) {
            User user = group.getMembers().get(i).getUser();
            if (!user.getPicture().isEmpty() && !user.getPicture().equals("null") && position < imageViews.length) {
                Picasso.with(context).load(user.getPicture()).into(imageViews[position]);
                position++;
            } else if (!user.getThumb().isEmpty() && !user.getThumb().equals("null") && position < imageViews.length) {
                Picasso.with(context).load(user.getThumb()).into(imageViews[position]);
                position++;
            }
        }

        TextViewCustomFont groupCode = (TextViewCustomFont) findViewById(R.id.group_code);
        String code = group.getCode().substring(0, 2) + "-" + group.getCode().substring(2, 4) + "-" + group.getCode().substring(4);
        groupCode.setText(code.toUpperCase());

        groupName = (TextViewCustomFont) findViewById(R.id.group_name);
        groupName.setText(group.getName());

        TextViewCustomFont members = (TextViewCustomFont) findViewById(R.id.members);
        members.setText("Members (" + group.getMembers().size() + ")");
        members.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupActivity.this, GroupMemberActivity.class);
                intent.putExtra("groupId", group.getGroupId());
                startActivity(intent);
            }
        });

        TextViewCustomFont editName = (TextViewCustomFont) findViewById(R.id.edit_name);
        editName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupActivity.this, CreateEditGroupActivity.class);
                intent.putExtra("isEdit", true);
                intent.putExtra("groupId", group.getGroupId());
                startActivityForResult(intent, 1);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String newName = data.getStringExtra("groupName");
                groupName.setText(newName);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave_group) {
            new AlertDialogWrapper.Builder(this)
                    .setTitle("Are you sure?")
                    .setMessage("You won't receive any new contacts from this group.")
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            GroupServerSync.deleteGroup(GroupActivity.this, group);
                            dialog.cancel();
                            Toast.makeText(context, "Left group.", Toast.LENGTH_SHORT).show();
                            GroupActivity.this.finish();
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

        return super.onOptionsItemSelected(item);
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
