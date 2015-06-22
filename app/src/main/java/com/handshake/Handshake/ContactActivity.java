package com.handshake.Handshake;

import android.app.ListActivity;
import android.os.Bundle;

import com.handshake.listview.ContactAdapter;
import com.handshake.models.User;

import io.realm.Realm;
import io.realm.RealmResults;


public class ContactActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        Realm realm = Realm.getInstance(this);
        RealmResults<User> users = realm.where(User.class).equalTo("isContact", true).findAll();
        users.sort("firstName", true);
        ContactAdapter myAdapter = new ContactAdapter(this, users, true);
        setListAdapter(myAdapter);
    }
}
