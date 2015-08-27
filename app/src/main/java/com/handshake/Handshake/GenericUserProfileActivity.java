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
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;

/**
 * Created by ankitgoyal on 6/27/15.
 */
public class GenericUserProfileActivity extends AppCompatActivity {
    private Drawable oldBackground = null;

    private Context context = this;

    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_user_profile);

//        changeColor(getResources().getColor(R.color.orange));

        CircleImageView profileImage = (CircleImageView) findViewById(R.id.profile_image);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) profileImage.getLayoutParams();
        params.setMargins(Utils.dpToPx(context, 16), Utils.getStatusBarHeight(context) + Utils.dpToPx(context, 16),
                Utils.dpToPx(context, 16), Utils.dpToPx(context, 16));

        fillViews();
    }

    private void fillViews() {
        final Realm realm = Realm.getInstance(context);
        SessionManager sessionManager = new SessionManager(context);
        final User account = realm.where(User.class).equalTo("userId", getIntent().getLongExtra("userId", sessionManager.getID())).findFirst();

        if (account.isContact()) {
            Intent i = new Intent(this, ContactUserProfileActivity.class);
            i.putExtra("userId", getIntent().getLongExtra("userId", sessionManager.getID()));
            startActivity(i);
            finish();
        }

        TextViewCustomFont name = (TextViewCustomFont) findViewById(R.id.name);
        String lastName = "";
        if (!account.getLastName().equals("null"))
            lastName = account.getLastName();
        name.setText(account.getFirstName() + " " + lastName);

        CircleImageView profileImage = (CircleImageView) findViewById(R.id.profile_image);
        ImageView backdrop = (ImageView) findViewById(R.id.backdrop);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        if (!account.getThumb().isEmpty() && !account.getThumb().equals("null")) {
            Picasso.with(context).load(account.getThumb()).transform(new CircleTransform()).into(profileImage);
            if (!account.getPicture().isEmpty() && !account.getPicture().equals("null"))
                Picasso.with(context).load(account.getPicture()).into(backdrop);
            else
                Picasso.with(context).load(account.getThumb()).into(backdrop);
        } else {
            Picasso.with(context).load(R.drawable.default_profile).transform(new CircleTransform()).into(profileImage);
            collapsingToolbar.setContentScrimColor(getResources().getColor(R.color.background_window));
        }

        TextViewCustomFont contacts = (TextViewCustomFont) findViewById(R.id.contacts);
        TextViewCustomFont mutual = (TextViewCustomFont) findViewById(R.id.mutual);

        if (account.getContacts() == 1)
            contacts.setText(account.getContacts() + " contact");
        else
            contacts.setText(account.getContacts() + " contacts");
        mutual.setText(account.getMutual() + " mutual");

        contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(GenericUserProfileActivity.this, ContactActivity.class);
                SessionManager sessionManager = new SessionManager(context);
                i.putExtra("userId", getIntent().getLongExtra("userId", sessionManager.getID()));
                i.putExtra("type", "contacts");
                startActivity(i);
            }
        });

        mutual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(GenericUserProfileActivity.this, ContactActivity.class);
                SessionManager sessionManager = new SessionManager(context);
                i.putExtra("userId", getIntent().getLongExtra("userId", sessionManager.getID()));
                i.putExtra("type", "mutual");
                startActivity(i);
            }
        });

        TextViewCustomFont text = (TextViewCustomFont) findViewById(R.id.text);

        MainActivity.setContactButtons(context, account,
                (ImageView) findViewById(R.id.button_one), (ImageView) findViewById(R.id.button_two), text);
        realm.close();
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
