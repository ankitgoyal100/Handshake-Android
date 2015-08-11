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
import android.widget.ImageView;

import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.handshake.models.Address;
import com.handshake.models.Card;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.EditTextCustomFont;

import io.realm.Realm;

public class EditAddressActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    private String city;
    private String label;
    private String state;
    private String street1;
    private String street2;
    private String zip;
    private boolean isEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);

        changeColor(getResources().getColor(R.color.orange));

        final EditTextCustomFont street1EditText = (EditTextCustomFont) findViewById(R.id.street1);
        final EditTextCustomFont street2EditText = (EditTextCustomFont) findViewById(R.id.street2);
        final EditTextCustomFont cityEditText = (EditTextCustomFont) findViewById(R.id.city);
        final EditTextCustomFont stateEditText = (EditTextCustomFont) findViewById(R.id.state);
        final EditTextCustomFont zipEditText = (EditTextCustomFont) findViewById(R.id.zip);

        if (getIntent().hasExtra("street1")) {
            isEdit = true;
            setTitle("Edit Address");

            street1 = getIntent().getStringExtra("street1");
            street2 = getIntent().getStringExtra("street2");
            city = getIntent().getStringExtra("city");
            state = getIntent().getStringExtra("state");
            zip = getIntent().getStringExtra("zip");
            label = getIntent().getStringExtra("label");

            street1EditText.setText(street1);
            street2EditText.setText(street2);
            cityEditText.setText(city);
            stateEditText.setText(state);
            zipEditText.setText(zip);
        } else {
            isEdit = false;
            setTitle("Add Address");
            label = "Home";
        }

        final View[] labels = {findViewById(R.id.home), findViewById(R.id.work), findViewById(R.id.other)};
        final ImageView[] checkBoxes = {(ImageView) findViewById(R.id.checkbox1), (ImageView) findViewById(R.id.checkbox2), (ImageView) findViewById(R.id.checkbox3)};
        checkBoxes[Utils.getIndexOfLabel(label, true)].setImageDrawable(getResources().getDrawable(R.mipmap.checkmark));
        for (int i = 0; i < labels.length; i++) {
            labels[i].setTag(i);
            labels[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < checkBoxes.length; j++) {
                        if (j == Integer.parseInt(v.getTag().toString())) {
                            label = Utils.threeLabels[j];
                            checkBoxes[j].setImageDrawable(getResources().getDrawable(R.mipmap.checkmark));
                            checkBoxes[j].setVisibility(View.VISIBLE);
                        } else {
                            checkBoxes[j].setVisibility(View.GONE);
                        }
                    }
                }
            });
        }

        ButtonCustomFont saveButton = (ButtonCustomFont) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Realm realm = Realm.getInstance(EditAddressActivity.this);
                final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
                final Card card = account.getCards().first();

                if (isEdit) {
                    for (int i = 0; i < card.getAddresses().size(); i++) {
                        if (card.getAddresses().get(i).getStreet1().equals(street1) && card.getAddresses().get(i).getStreet2().equals(street2) &&
                                card.getAddresses().get(i).getCity().equals(city) && card.getAddresses().get(i).getZip().equals(zip)) {
                            realm.beginTransaction();
                            card.getAddresses().get(i).setStreet1(street1EditText.getText().toString());
                            card.getAddresses().get(i).setStreet2(street2EditText.getText().toString());
                            card.getAddresses().get(i).setCity(cityEditText.getText().toString());
                            card.getAddresses().get(i).setState(stateEditText.getText().toString());
                            card.getAddresses().get(i).setZip(zipEditText.getText().toString());
                            card.getAddresses().get(i).setLabel(label);
                            realm.commitTransaction();
                        }
                    }

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("is_initial_setup", EditProfileActivity.isIntialSetup);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
                    realm.beginTransaction();
                    Address address = realm.createObject(Address.class);
                    address.setStreet1(street1EditText.getText().toString());
                    address.setStreet2(street2EditText.getText().toString());
                    address.setCity(cityEditText.getText().toString());
                    address.setState(stateEditText.getText().toString());
                    address.setZip(zipEditText.getText().toString());
                    address.setLabel(label);
                    card.getAddresses().add(realm.copyToRealm(address));
                    realm.commitTransaction();

                    Intent intent = new Intent(EditAddressActivity.this, EditProfileActivity.class);
                    intent.putExtra("is_initial_setup", EditProfileActivity.isIntialSetup);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    setResult(RESULT_OK, intent);
                    startActivity(intent);
                    finish();
                }
                realm.close();
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
