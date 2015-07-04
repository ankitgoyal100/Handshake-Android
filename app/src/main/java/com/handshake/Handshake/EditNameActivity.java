package com.handshake.Handshake;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.handshake.models.Account;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.EditTextCustomFont;

import io.realm.Realm;

public class EditNameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_name);

        final Realm realm = Realm.getInstance(this);
        final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

        final EditTextCustomFont firstName = (EditTextCustomFont) findViewById(R.id.first_name);
        final EditTextCustomFont lastName = (EditTextCustomFont) findViewById(R.id.last_name);

        firstName.setText(account.getFirstName());
        if (!account.getLastName().equals("null"))
            lastName.setText(account.getLastName());

        ButtonCustomFont saveButton = (ButtonCustomFont) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                realm.beginTransaction();
                account.setFirstName(firstName.getText().toString());
                account.setLastName(lastName.getText().toString());
                realm.commitTransaction();

                Intent returnIntent = new Intent();
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
}
