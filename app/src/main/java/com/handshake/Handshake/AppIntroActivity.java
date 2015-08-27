package com.handshake.Handshake;

import android.content.Intent;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;

/**
 * Created by ankitgoyal on 8/3/15.
 */
public class AppIntroActivity extends AppIntro2 {

    // Please DO NOT override onCreate. Use init
    @Override
    public void init(Bundle savedInstanceState) {
        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest
        addSlide(AppIntroFragment.newInstance("Slide 1", "Description 1",
                R.drawable.logo, getResources().getColor(R.color.orange)));

        addSlide(AppIntroFragment.newInstance("Slide 2", "Description 2",
                R.drawable.logo, getResources().getColor(R.color.dark_gray)));

        addSlide(AppIntroFragment.newInstance("Slide 3", "Description 3",
                R.drawable.logo, getResources().getColor(R.color.light_gray)));
    }

    @Override
    public void onDonePressed() {
        SessionManager sessionManager = new SessionManager(getApplicationContext());
        sessionManager.setIntroScreenDisplayed(true);

        Intent i = new Intent(getApplicationContext(), IntroActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}