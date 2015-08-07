package com.handshake.editor;

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

import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.models.Account;
import com.handshake.models.Card;
import com.handshake.models.Social;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.EditTextCustomFont;
import com.handshake.views.TextViewCustomFont;

import io.realm.Realm;

public class EditSocialActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_social);

        changeColor(getResources().getColor(R.color.orange));

        TextViewCustomFont prefix = (TextViewCustomFont) findViewById(R.id.prefix);
        final EditTextCustomFont username = (EditTextCustomFont) findViewById(R.id.username);

        if(getIntent().getStringExtra("network").equals("Twitter") ||
                getIntent().getStringExtra("network").equals("Instagram")) {
            prefix.setVisibility(View.VISIBLE);
            setTitle("Add " + getIntent().getStringExtra("network"));
        } else {
            prefix.setVisibility(View.GONE);
            setTitle("Add Snapchat");
        }

        final Realm realm = Realm.getInstance(this);
        final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
        final Card card = account.getCards().first();

        ButtonCustomFont saveButton = (ButtonCustomFont) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realm.beginTransaction();
                Social social = realm.createObject(Social.class);
                social.setNetwork(getIntent().getStringExtra("network").toLowerCase());
                social.setUsername(username.getText().toString());
                card.getSocials().add(realm.copyToRealm(social));
                realm.commitTransaction();

                Intent returnIntent = new Intent();
                returnIntent.putExtra("is_initial_setup", EditProfileActivity.isIntialSetup);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_name, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_cancel) {
            finish();
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
