package com.handshake.Handshake;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;

import com.handshake.editor.EditProfileActivity;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.TextViewCustomFont;

public class GetStartedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_started);

        String firstName = getIntent().getStringExtra("first_name");
        TextViewCustomFont text = (TextViewCustomFont) findViewById(R.id.text);
        TextViewCustomFont welcome = (TextViewCustomFont) findViewById(R.id.welcome);
        ButtonCustomFont getStarted = (ButtonCustomFont) findViewById(R.id.get_started);

        welcome.setText("Welcome, " + firstName + "!");

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1200);
        text.startAnimation(fadeIn);
        welcome.startAnimation(fadeIn);

        AlphaAnimation fadeInSecond = new AlphaAnimation(0.0f, 1.0f);
        fadeInSecond.setDuration(1200);
        fadeInSecond.setStartOffset(800);
        getStarted.startAnimation(fadeInSecond);

        getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(GetStartedActivity.this, EditProfileActivity.class);
                i.putExtra("is_initial_setup", true);
                startActivity(i);
            }
        });
    }
}
