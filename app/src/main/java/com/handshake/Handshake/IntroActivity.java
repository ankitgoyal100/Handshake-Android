package com.handshake.Handshake;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.handshake.views.TextViewCustomFont;
import com.handshake.views.TextureVideoView;


public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

//        ImageView background = (ImageView) findViewById(R.id.background);
//        background.setColorFilter(getResources().getColor(R.color.tint));
        TextureVideoView cropTextureView = (TextureVideoView) findViewById(R.id.background);
        cropTextureView.setScaleType(TextureVideoView.ScaleType.CENTER_CROP);
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro_video);
        cropTextureView.setDataSource(this, uri);
        cropTextureView.play();
        cropTextureView.setLooping(true);

        TextViewCustomFont login = (TextViewCustomFont) findViewById(R.id.login);
        Button signUp = (Button) findViewById(R.id.sign_up);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IntroActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_intro, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
