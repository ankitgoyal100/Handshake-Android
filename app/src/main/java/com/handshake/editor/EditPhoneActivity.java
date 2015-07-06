package com.handshake.editor;

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
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.countrypicker.CountryPicker;
import com.countrypicker.CountryPickerListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.Utils;
import com.handshake.models.Account;
import com.handshake.models.Card;
import com.handshake.models.Phone;
import com.handshake.views.ButtonCustomFont;
import com.handshake.views.EditTextCustomFont;
import com.handshake.views.TextViewCustomFont;

import java.util.Locale;

import io.realm.Realm;

public class EditPhoneActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    private Context context = this;

    String countryCode;
    String phoneNumber;
    String label;
    private boolean isEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_phone);

        changeColor(getResources().getColor(R.color.orange));

        final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        final TextViewCustomFont countryCodeTextView = (TextViewCustomFont) findViewById(R.id.country_code);
        final EditTextCustomFont phoneEditText = (EditTextCustomFont) findViewById(R.id.number);
        phoneEditText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        if (getIntent().hasExtra("number") && getIntent().hasExtra("code")) {
            isEdit = true;
            setTitle("Edit Phone");

            phoneNumber = getIntent().getStringExtra("number");
            countryCode = getIntent().getStringExtra("code");
            label = getIntent().getStringExtra("label");

            Locale loc = new Locale("", countryCode);
            countryCodeTextView.setText(loc.getDisplayCountry() + " (+" + phoneUtil.getCountryCodeForRegion(countryCode) + ")");

            try {
                Phonenumber.PhoneNumber numberObject = phoneUtil.parse(phoneNumber, countryCode);
                if (phoneUtil.isValidNumber(numberObject))
                    phoneEditText.setText(phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
            } catch (NumberParseException e) {
                phoneEditText.setText(phoneNumber);
                e.printStackTrace();
            }
        } else {
            isEdit = false;
            setTitle("Add Phone");
            countryCode = "US";
            label = "Home";
        }

        final View[] labels = {findViewById(R.id.home), findViewById(R.id.mobile), findViewById(R.id.work), findViewById(R.id.other)};
        final ImageView[] checkBoxes = {(ImageView) findViewById(R.id.checkbox1), (ImageView) findViewById(R.id.checkbox2), (ImageView) findViewById(R.id.checkbox3), (ImageView) findViewById(R.id.checkbox4)};
        checkBoxes[Utils.getIndexOfLabel(label, false)].setImageDrawable(getResources().getDrawable(R.mipmap.checkmark));
        for (int i = 0; i < labels.length; i++) {
            labels[i].setTag(i);
            labels[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < checkBoxes.length; j++) {
                        if (j == Integer.parseInt(v.getTag().toString())) {
                            label = Utils.fourLabels[j];
                            checkBoxes[j].setImageDrawable(getResources().getDrawable(R.mipmap.checkmark));
                            checkBoxes[j].setVisibility(View.VISIBLE);
                        } else {
                            checkBoxes[j].setVisibility(View.GONE);
                        }
                    }
                }
            });
        }

        countryCodeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CountryPicker picker = CountryPicker.newInstance("Select Country");
                picker.show(getSupportFragmentManager(), "COUNTRY_PICKER");
                picker.setListener(new CountryPickerListener() {

                    @Override
                    public void onSelectCountry(String name, String code) {
                        countryCode = code;
                        countryCodeTextView.setText(name + " (+" + phoneUtil.getCountryCodeForRegion(code) + ")");
                        picker.dismiss();
                    }
                });
            }
        });

        ButtonCustomFont saveButton = (ButtonCustomFont) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Realm realm = Realm.getInstance(EditPhoneActivity.this);
                final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
                final Card card = account.getCards().first();

                if (isEdit) {
                    for (int i = 0; i < card.getPhones().size(); i++) {
                        if (card.getPhones().get(i).getNumber().equals(phoneNumber)) {
                            realm.beginTransaction();
                            try {
                                Phonenumber.PhoneNumber numberObject = phoneUtil.parse(phoneEditText.getText().toString(), countryCode);
                                if (phoneUtil.isValidNumber(numberObject))
                                    card.getPhones().get(i).setNumber(phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL).replaceAll("\\s", ""));
                                else {
                                    Toast.makeText(context, "Invalid phone number.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } catch (NumberParseException e) {
                                e.printStackTrace();
                            }
                            card.getPhones().get(i).setCountryCode(countryCode);
                            card.getPhones().get(i).setLabel(label);
                            realm.commitTransaction();
                        }
                    }
                } else {
                    realm.beginTransaction();
                    Phone phone = realm.createObject(Phone.class);
                    try {
                        Phonenumber.PhoneNumber numberObject = phoneUtil.parse(phoneEditText.getText().toString(), countryCode);
                        if (phoneUtil.isValidNumber(numberObject))
                            phone.setNumber(phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL).replaceAll("\\s", ""));
                        else {
                            Toast.makeText(context, "Invalid phone number.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    }
                    phone.setCountryCode(countryCode);
                    phone.setLabel(label);
                    card.getPhones().add(realm.copyToRealm(phone));
                    realm.commitTransaction();
                }

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
