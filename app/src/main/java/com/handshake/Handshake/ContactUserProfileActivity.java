package com.handshake.Handshake;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.helpers.ContactSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.models.Address;
import com.handshake.models.Card;
import com.handshake.models.Email;
import com.handshake.models.Phone;
import com.handshake.models.Social;
import com.handshake.models.User;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;

public class ContactUserProfileActivity extends AppCompatActivity {

    private Context context = this;
    private Handler handler = new Handler();

    private Drawable oldBackground = null;

    private LinearLayout infoLayout;
    private LinearLayout socialLayout;
    private static Executor executor = Executors.newSingleThreadExecutor();
    private Realm r;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_user_profile);

//        changeColor(getResources().getColor(R.color.orange));

        RelativeLayout autoSyncLayout = (RelativeLayout) findViewById(R.id.save_to_phone_layout);
        View autoSyncDivider = findViewById(R.id.save_to_phone_divider);
        Switch autoSyncToggle = (Switch) findViewById(R.id.save_to_phone);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean isAutosync = sharedPreferences.getBoolean("autosync_preference", true);

        if (!isAutosync) {
            autoSyncLayout.setVisibility(View.VISIBLE);
            autoSyncDivider.setVisibility(View.VISIBLE);

            final Realm realm = Realm.getInstance(context);
            final User account = realm.where(User.class).equalTo("userId", getIntent().getLongExtra("userId", SessionManager.getID())).findFirst();
            autoSyncToggle.setChecked(account.isSavesToPhone());

            autoSyncToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    realm.beginTransaction();
                    account.setSavesToPhone(isChecked);
                    realm.commitTransaction();
                }
            });
//            realm.close();
        } else {
            autoSyncLayout.setVisibility(View.GONE);
            autoSyncDivider.setVisibility(View.GONE);
        }

        CircleImageView profileImage = (CircleImageView) findViewById(R.id.profile_image);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) profileImage.getLayoutParams();
        params.setMargins(Utils.dpToPx(context, 16), Utils.getStatusBarHeight(context) + Utils.dpToPx(context, 16),
                Utils.dpToPx(context, 16), Utils.dpToPx(context, 16));

        infoLayout = (LinearLayout) findViewById(R.id.linear_layout);
        socialLayout = (LinearLayout) findViewById(R.id.linear_layout_2);
        fillViews();
    }

    private void fillViews() {
        final Realm realm = Realm.getInstance(context);
        final User account = realm.where(User.class).equalTo("userId", getIntent().getLongExtra("userId", SessionManager.getID())).findFirst();

        if(!account.isContact()) {
            Intent i = new Intent(this, GenericUserProfileActivity.class);
            i.putExtra("userId", getIntent().getLongExtra("userId", SessionManager.getID()));
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

        contacts.setText(account.getContacts() + " contacts");
        mutual.setText(account.getMutual() + " mutual");
        contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ContactUserProfileActivity.this, ContactActivity.class);
                i.putExtra("userId", account.getUserId());
                i.putExtra("type", "contacts");
                startActivity(i);
            }
        });

        mutual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ContactUserProfileActivity.this, ContactActivity.class);
                i.putExtra("userId", account.getUserId());
                i.putExtra("type", "mutual");
                startActivity(i);
            }
        });

        TextViewCustomFont text = (TextViewCustomFont) findViewById(R.id.text);

        MainActivity.setContactButtons(context, account,
                (ImageView) findViewById(R.id.button_one), (ImageView) findViewById(R.id.button_two), text);

        ImageView notifications = (ImageView) findViewById(R.id.notifications);
        setNotificationsButton(account, notifications);

        final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading profile...", true);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        infoLayout.removeAllViews();
                        socialLayout.removeAllViews();
                    }
                });

                while (!MainActivity.contactSyncCompleted) {

                }

                r = Realm.getInstance(context);

                final User account = r.where(User.class).equalTo("userId", getIntent().getLongExtra("userId", SessionManager.getID())).findFirst();
                final Card card = account.getCards().first();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.cancel();
                        if (card == null) {
                            Toast.makeText(context, "There was an error. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                for (final Phone phone : card.getPhones()) {
                    LayoutInflater inflater = (LayoutInflater) context.getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View mLinearView = inflater.inflate(R.layout.info_cell, null);
                    final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
                    final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
                    final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
                    final ImageView imageView2 = (ImageView) mLinearView.findViewById(R.id.imageView2);
                    final String phoneNumber = phone.getNumber();
                    final String phoneCountryCode = phone.getCountryCode();
                    final String phoneLabel = phone.getLabel();


                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.no_info).setVisibility(View.GONE);
                            findViewById(R.id.divider1).setVisibility(View.VISIBLE);

                            imageView1.setVisibility(View.VISIBLE);
                            imageView2.setVisibility(View.VISIBLE);
                            imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.message_button));
                            imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.call_button));

                            imageView1.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                                    sendIntent.setData(Uri.parse("sms:" + phoneNumber));
                                    startActivity(sendIntent);
                                }
                            });

                            imageView2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String uri = "tel:" + phoneNumber;
                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    intent.setData(Uri.parse(uri));
                                    startActivity(intent);
                                }
                            });

                            description.setText(phoneLabel);
                            infoLayout.addView(mLinearView);

                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                            try {
                                Phonenumber.PhoneNumber numberObject = phoneUtil.parse(phoneNumber, phoneCountryCode);
                                if (phoneUtil.isValidNumber(numberObject))
                                    title.setText(phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
                            } catch (NumberParseException e) {
                                title.setText(phoneNumber);
                                e.printStackTrace();
                            }
                        }
                    });
                }

                for (final Email email : card.getEmails()) {
                    LayoutInflater inflater = (LayoutInflater) context.getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View mLinearView = inflater.inflate(R.layout.info_cell, null);
                    final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
                    final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
                    final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
                    final ImageView imageView2 = (ImageView) mLinearView.findViewById(R.id.imageView2);
                    final String emailAddress = email.getAddress();
                    final String emailLabel = email.getLabel();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.no_info).setVisibility(View.GONE);
                            imageView1.setVisibility(View.GONE);
                            imageView2.setVisibility(View.VISIBLE);
                            imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.email_button));

                            imageView2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                            "mailto", emailAddress, null));
                                    startActivity(Intent.createChooser(emailIntent, "Send email"));
                                }
                            });

                            title.setText(emailAddress);
                            description.setText(emailLabel);
                            infoLayout.addView(mLinearView);
                        }
                    });
                }


                for (final Address address : card.getAddresses()) {
                    LayoutInflater inflater = (LayoutInflater) context.getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View mLinearView = inflater.inflate(R.layout.info_cell, null);
                    final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
                    final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
                    final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
                    final ImageView imageView2 = (ImageView) mLinearView.findViewById(R.id.imageView2);
                    final String addressStreet1 = address.getStreet1();
                    final String addressStreet2 = address.getStreet2();
                    final String addressCity = address.getCity();
                    final String addressState = address.getState();
                    final String addressZip = address.getZip();
                    final String addressLabel = address.getLabel();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.no_info).setVisibility(View.GONE);
                            imageView1.setVisibility(View.GONE);
                            imageView2.setVisibility(View.VISIBLE);
                            imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.maps_button));

                            imageView2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String address;
                                    if (addressStreet2.length() != 0)
                                        address = addressStreet1 + ", " + addressStreet2 + ", " +
                                                addressCity + ", " + addressState + " " + addressZip;
                                    else
                                        address = addressStreet1 + ", " +
                                                addressCity + ", " + addressState + " " + addressZip;

                                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + address);
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                    mapIntent.setPackage("com.google.android.apps.maps");
                                    startActivity(mapIntent);
                                }
                            });

                            if (addressStreet2.length() != 0)
                                title.setText(addressStreet1 + "\n" + addressStreet2 + "\n" +
                                        addressCity + ", " + addressState + " " + addressZip);
                            else
                                title.setText(addressStreet1 + "\n" +
                                        addressCity + ", " + addressState + " " + addressZip);

                            description.setText(addressLabel);
                            infoLayout.addView(mLinearView);
                        }
                    });
                }

                for (final Social social : card.getSocials()) {
                    LayoutInflater inflater = (LayoutInflater) context.getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View mLinearView = inflater.inflate(R.layout.social_cell, null);
                    final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
                    final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
                    final String username = social.getUsername();
                    final String network = social.getNetwork();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.no_info).setVisibility(View.GONE);
                            findViewById(R.id.divider2).setVisibility(View.VISIBLE);

                            if (network.equals("facebook")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.facebook_icon));
                                title.setText("Facebook");
                                mLinearView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/" + username)));
                                        } catch (Exception e) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://facebook.com/" + username)));
                                        }
                                    }
                                });
                            } else if (network.equals("twitter")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.twitter_icon));
                                title.setText("@" + username);
                                mLinearView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("twitter://user?screen_name=" + username)));
                                        } catch (Exception e) {
                                            startActivity(new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("https://twitter.com/#!/" + username)));
                                        }
                                    }
                                });
                            } else if (network.equals("instagram")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.instagram_icon));
                                title.setText("@" + username);
                                mLinearView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Uri uri = Uri.parse("http://instagram.com/_u/" + username + "/");
                                        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

                                        likeIng.setPackage("com.instagram.android");

                                        try {
                                            startActivity(likeIng);
                                        } catch (ActivityNotFoundException e) {
                                            startActivity(new Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("http://instagram.com/_u/" + username + "/")));
                                        }
                                    }
                                });
                            } else if (network.equals("snapchat")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.snapchat_icon));
                                title.setText(username);
                                mLinearView.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(context,
                                                "Unable to open Snapchat. Please manually add the user via the Snapchat application.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            socialLayout.addView(mLinearView);
                        }
                    });
                }
                r.close();
            }
        });
//        realm.close();
    }

    private void setNotificationsButton(final User account, final ImageView notifications) {
        if (account.isNotifications()) {
            ViewGroup.LayoutParams params = notifications.getLayoutParams();
            params.height = dipToPixels(context, 37.5f);
            params.width = dipToPixels(context, 117.71f);
            notifications.setLayoutParams(params);
            notifications.setImageDrawable(getResources().getDrawable(R.drawable.notifications_on_button));
            notifications.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RestClientAsync.post(context, "/users/" + account.getUserId() + "/black_list", new RequestParams(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            Realm realm = Realm.getInstance(context);
                            realm.beginTransaction();
                            account.setNotifications(false);
                            realm.commitTransaction();
//                            realm.close();

                            setNotificationsButton(account, notifications);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            Toast.makeText(context, "There was an error. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            ViewGroup.LayoutParams params = notifications.getLayoutParams();
            params.height = dipToPixels(context, 37.5f);
            params.width = dipToPixels(context, 129.17f);
            notifications.setLayoutParams(params);
            notifications.setImageDrawable(getResources().getDrawable(R.drawable.notifications_off_button));
            notifications.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RestClientAsync.delete(context, "/users/" + account.getUserId() + "/black_list", new RequestParams(), new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            Realm realm = Realm.getInstance(context);
                            realm.beginTransaction();
                            account.setNotifications(true);
                            realm.commitTransaction();
//                            realm.close();

                            setNotificationsButton(account, notifications);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            Toast.makeText(context, "There was an error. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    public static int dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
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

    @Override
    public void onResume() {
        super.onResume();
        fillViews();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ContactSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (r != null)
            r.close();
    }
}
