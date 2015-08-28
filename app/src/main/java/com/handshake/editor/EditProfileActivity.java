package com.handshake.editor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.Handshake.MainActivity;
import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.SignUpActivity;
import com.handshake.Handshake.Utils;
import com.handshake.helpers.AccountServerSync;
import com.handshake.helpers.CardServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.models.Account;
import com.handshake.models.Address;
import com.handshake.models.Card;
import com.handshake.models.Email;
import com.handshake.models.Phone;
import com.handshake.models.Social;
import com.handshake.views.CircleTransform;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import io.realm.Realm;

public class EditProfileActivity extends AppCompatActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;

    private int VIEW_REMOVE = 0;
    private int VIEW_ADD = 1;

    private Context context = this;
    private ImageView profileImage;

    private View facebookView;
    private View twitterView;
    private View instagramView;
    private View snapchatView;

    public static boolean isIntialSetup;
    private CallbackManager callbackManager;
    private Bitmap circle;
    private Bitmap photo;
    private Realm realm;
    private Realm realm2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        changeColor(getResources().getColor(R.color.orange));

        FacebookSdk.sdkInitialize(getApplicationContext());

        fillViews();
    }

    private void fillViews() {
        isIntialSetup = getIntent().hasExtra("is_initial_setup") && getIntent().getBooleanExtra("is_initial_setup", false);
        initialSetup();

        realm = Realm.getInstance(this);
        SessionManager sessionManager = new SessionManager(context);
        final Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();
        final Card card = account.getCards().first();

        setName(account);
        setImage(account);

        if (card != null) {
            setContactInformation(card);
            setSocials(card);
        }

        TextViewCustomFont addInformation = (TextViewCustomFont) findViewById(R.id.add_information);
        addInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, AddContactActivity.class);
                startActivityForResult(i, 0);
            }
        });
    }

    private void initialSetup() {
        Button saveButton = (Button) findViewById(R.id.save);
        if (isIntialSetup) {
            LinearLayout nameLayout = (LinearLayout) findViewById(R.id.name_layout);
            View nameDivider = findViewById(R.id.name_divider);
            nameLayout.setVisibility(View.GONE);
            nameDivider.setVisibility(View.GONE);
            saveButton.setText("Next");

            TextViewCustomFont intro = (TextViewCustomFont) findViewById(R.id.intro);
            View introDivider = findViewById(R.id.intro_divider);
            View pictureDivider = findViewById(R.id.edit_picture_divider);
            intro.setVisibility(View.VISIBLE);
            introDivider.setVisibility(View.VISIBLE);
            pictureDivider.setVisibility(View.VISIBLE);

            if (!SignUpActivity.cardSyncCompleted) {
                final ProgressDialog dialog = ProgressDialog.show(context, "", "Loading profile...", true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!SignUpActivity.cardSyncCompleted) {

                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                dialog.cancel();
                            }
                        });
                    }
                }).start();
            }
        } else {
            TextViewCustomFont intro = (TextViewCustomFont) findViewById(R.id.intro);
            View introDivider = findViewById(R.id.intro_divider);
            View pictureDivider = findViewById(R.id.edit_picture_divider);
            intro.setVisibility(View.GONE);
            introDivider.setVisibility(View.GONE);
            pictureDivider.setVisibility(View.GONE);

            LinearLayout nameLayout = (LinearLayout) findViewById(R.id.name_layout);
            View nameDivider = findViewById(R.id.name_divider);
            nameLayout.setVisibility(View.VISIBLE);
            nameDivider.setVisibility(View.VISIBLE);
            saveButton.setText("Save");
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Realm realm = Realm.getInstance(context);
                SessionManager sessionManager = new SessionManager(context);
                final Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();
                final Card card = account.getCards().first();

                realm.beginTransaction();
                account.setSyncStatus(Utils.AccountUpdated);
                card.setSyncStatus(Utils.CardUpdated);
                realm.commitTransaction();
                realm.close();

                AccountServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                    }
                });

                CardServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                    }
                });

                if (isIntialSetup) {
                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);
                }

                finish();
            }
        });
    }

    private void setContactInformation(Card card) {
        final LinearLayout infoLayout = (LinearLayout) findViewById(R.id.linear_layout);
        infoLayout.removeAllViews();

        for (int i = 0; i < card.getPhones().size(); i++) {
            final Phone phone = card.getPhones().get(i);
            LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mLinearView = inflater.inflate(R.layout.added_info_cell, null);
            final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
            final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
            final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
            final String phoneNumber = phone.getNumber();
            final String phoneCountryCode = phone.getCountryCode();
            final String phoneLabel = phone.getLabel();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageView1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialogWrapper.Builder(context)
                                    .setMessage("Are you sure?")
                                    .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            infoLayout.removeView(mLinearView);
                                            Realm realm = Realm.getInstance(context);
                                            realm.beginTransaction();
                                            phone.removeFromRealm();
                                            realm.commitTransaction();
                                            realm.close();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .show();
                        }
                    });

                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    try {
                        Phonenumber.PhoneNumber numberObject = phoneUtil.parse(phoneNumber, phoneCountryCode);
                        if (phoneUtil.isValidNumber(numberObject))
                            title.setText(phoneUtil.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.NATIONAL));
                    } catch (NumberParseException e) {
                        title.setText(phoneNumber);
                        e.printStackTrace();
                    }

                    description.setText(phoneLabel);
                    mLinearView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(context, EditPhoneActivity.class);
                            i.putExtra("number", phoneNumber);
                            i.putExtra("code", phoneCountryCode);
                            i.putExtra("label", phoneLabel);
                            startActivityForResult(i, 0);
                        }
                    });

                    infoLayout.addView(mLinearView);
                }
            });
        }

        for (int i = 0; i < card.getEmails().size(); i++) {
            final Email email = card.getEmails().get(i);
            LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mLinearView = inflater.inflate(R.layout.added_info_cell, null);
            final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
            final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
            final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
            final String emailAddress = email.getAddress();
            final String emailLabel = email.getLabel();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageView1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialogWrapper.Builder(context)
                                    .setMessage("Are you sure?")
                                    .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            infoLayout.removeView(mLinearView);
                                            Realm realm = Realm.getInstance(context);
                                            realm.beginTransaction();
                                            email.removeFromRealm();
                                            realm.commitTransaction();
                                            realm.close();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .show();
                        }
                    });

                    title.setText(emailAddress);
                    description.setText(emailLabel);
                    mLinearView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(context, EditEmailActivity.class);
                            i.putExtra("address", emailAddress);
                            i.putExtra("label", emailLabel);
                            startActivityForResult(i, 0);
                        }
                    });

                    infoLayout.addView(mLinearView);
                }
            });
        }

        for (int i = 0; i < card.getAddresses().size(); i++) {
            final Address address = card.getAddresses().get(i);
            LayoutInflater inflater = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mLinearView = inflater.inflate(R.layout.added_info_cell, null);
            final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
            final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
            final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
            final String addressStreet1 = address.getStreet1();
            final String addressStreet2 = address.getStreet2();
            final String addressCity = address.getCity();
            final String addressState = address.getState();
            final String addressZip = address.getZip();
            final String addressLabel = address.getLabel();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageView1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new AlertDialogWrapper.Builder(context)
                                    .setMessage("Are you sure?")
                                    .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            infoLayout.removeView(mLinearView);
                                            Realm realm = Realm.getInstance(context);
                                            realm.beginTransaction();
                                            address.removeFromRealm();
                                            realm.commitTransaction();
                                            realm.close();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .show();
                        }
                    });

                    if (addressStreet2.length() != 0)
                        title.setText(addressStreet1 + "\n" + addressStreet2 + "\n" +
                                addressCity + ", " + addressState + " " + addressZip);
                    else
                        title.setText(addressStreet1 + "\n" +
                                addressCity + ", " + addressState + " " + addressZip);

                    description.setText(addressLabel);
                    mLinearView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(context, EditAddressActivity.class);
                            i.putExtra("street1", addressStreet1);
                            i.putExtra("street2", addressStreet2);
                            i.putExtra("city", addressCity);
                            i.putExtra("state", addressState);
                            i.putExtra("zip", addressZip);
                            i.putExtra("label", addressLabel);
                            startActivityForResult(i, 0);
                        }
                    });
                    infoLayout.addView(mLinearView);
                }
            });
        }

    }

    private void setName(Account account) {
        TextViewCustomFont name = (TextViewCustomFont) findViewById(R.id.name);
        String lastName = "";
        if (!account.getLastName().equals("null"))
            lastName = account.getLastName();
        name.setText(account.getFirstName() + " " + lastName);

        LinearLayout nameLayout = (LinearLayout) findViewById(R.id.name_layout);
        nameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, EditNameActivity.class);
                startActivityForResult(i, 0);
            }
        });
    }

    private void setSocials(final Card card) {
        facebookView = findViewById(R.id.facebook);
        twitterView = findViewById(R.id.twitter);
        instagramView = findViewById(R.id.instagram);
        snapchatView = findViewById(R.id.snapchat);

        for (Social social : card.getSocials()) {
            if (social.getNetwork().equals("facebook")) {
                facebookView.setTag(VIEW_REMOVE);
                ImageView imageView = (ImageView) facebookView.findViewById(R.id.imageView1);
                imageView.setImageDrawable(getResources().getDrawable(R.mipmap.facebook_icon));
                ((TextView) facebookView.findViewById(R.id.title)).setText("Remove Facebook");
            } else if (social.getNetwork().equals("twitter")) {
                twitterView.setTag(VIEW_REMOVE);
                ImageView imageView = (ImageView) twitterView.findViewById(R.id.imageView1);
                imageView.setImageDrawable(getResources().getDrawable(R.mipmap.twitter_icon));
                ((TextView) twitterView.findViewById(R.id.title)).setText("Remove @" + social.getUsername());
            } else if (social.getNetwork().equals("instagram")) {
                instagramView.setTag(VIEW_REMOVE);
                ImageView imageView = (ImageView) instagramView.findViewById(R.id.imageView1);
                imageView.setImageDrawable(getResources().getDrawable(R.mipmap.instagram_icon));
                ((TextView) instagramView.findViewById(R.id.title)).setText("Remove @" + social.getUsername());
            } else if (social.getNetwork().equals("snapchat")) {
                snapchatView.setTag(VIEW_REMOVE);
                ImageView imageView = (ImageView) snapchatView.findViewById(R.id.imageView1);
                imageView.setImageDrawable(getResources().getDrawable(R.mipmap.snapchat_icon));
                ((TextView) snapchatView.findViewById(R.id.title)).setText("Remove " + social.getUsername());
            }
        }


        if (facebookView.getTag() != VIEW_REMOVE) {
            facebookView.setTag(VIEW_ADD);
            ImageView imageView = (ImageView) facebookView.findViewById(R.id.imageView1);
            imageView.setImageDrawable(getResources().getDrawable(R.mipmap.facebook_icon));
            ((TextView) facebookView.findViewById(R.id.title)).setText("Add Facebook");
        }

        if (twitterView.getTag() != VIEW_REMOVE) {
            twitterView.setTag(VIEW_ADD);
            ImageView imageView = (ImageView) twitterView.findViewById(R.id.imageView1);
            imageView.setImageDrawable(getResources().getDrawable(R.mipmap.twitter_icon));
            ((TextView) twitterView.findViewById(R.id.title)).setText("Add Twitter");
        }

        if (instagramView.getTag() != VIEW_REMOVE) {
            instagramView.setTag(VIEW_ADD);
            ImageView imageView = (ImageView) instagramView.findViewById(R.id.imageView1);
            imageView.setImageDrawable(getResources().getDrawable(R.mipmap.instagram_icon));
            ((TextView) instagramView.findViewById(R.id.title)).setText("Add Instagram");
        }

        if (snapchatView.getTag() != VIEW_REMOVE) {
            snapchatView.setTag(VIEW_ADD);
            ImageView imageView = (ImageView) snapchatView.findViewById(R.id.imageView1);
            imageView.setImageDrawable(getResources().getDrawable(R.mipmap.snapchat_icon));
            ((TextView) snapchatView.findViewById(R.id.title)).setText("Add Snapchat");
        }

        final Realm realm2 = Realm.getInstance(context);

        facebookView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (facebookView.getTag() == VIEW_REMOVE) {
                    new AlertDialogWrapper.Builder(context)
                            .setMessage("Are you sure?")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    facebookView.setTag(VIEW_ADD);
                                    ((TextView) facebookView.findViewById(R.id.title)).setText("Add Facebook");
                                    for (int i = 0; i < card.getSocials().size(); i++) {
                                        if (card.getSocials().get(i).getNetwork().equals("facebook")) {
                                            realm2.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm2.commitTransaction();
                                        }
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if (facebookView.getTag() == VIEW_ADD) {
//                    if(!SessionManager.getFBID().equals("Not Connected") && !SessionManager.getFBID().isEmpty()) {
//                        addFacebookToCard(SessionManager.getFBID());
//                        facebookView.setTag(VIEW_REMOVE);
//                        fillViews();
//                        return;
//                    }

                    callbackManager = CallbackManager.Factory.create();
                    LoginManager.getInstance().logInWithReadPermissions(EditProfileActivity.this,
                            Arrays.asList("email"));
                    LoginManager.getInstance().registerCallback(callbackManager,
                            new FacebookCallback<LoginResult>() {
                                @Override
                                public void onSuccess(LoginResult loginResult) {
                                    GraphRequest.newMeRequest(
                                            loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                                                @Override
                                                public void onCompleted(JSONObject json, GraphResponse response) {
                                                    if (response.getError() != null) {
                                                        Toast.makeText(context, "There was an error.", Toast.LENGTH_LONG).show();
                                                    } else {
                                                        try {
//                                                            SessionManager.setFBID(json.getString("id"));
                                                            addFacebookToCard(json.getString("id"));
                                                            facebookView.setTag(VIEW_REMOVE);
                                                            fillViews();
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }

                                            }).executeAsync();

                                }

                                @Override
                                public void onCancel() {
                                    Log.d("Cancel", "On cancel");
                                }

                                @Override
                                public void onError(FacebookException error) {
                                    Log.d("Error", error.toString());
                                }
                            });
                }
            }
        });

        twitterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (twitterView.getTag() == VIEW_REMOVE) {
                    new AlertDialogWrapper.Builder(context)
                            .setMessage("Are you sure?")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    twitterView.setTag(VIEW_ADD);
                                    ((TextView) twitterView.findViewById(R.id.title)).setText("Add Twitter");
                                    for (int i = 0; i < card.getSocials().size(); i++) {
                                        if (card.getSocials().get(i).getNetwork().equals("twitter")) {
                                            realm2.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm2.commitTransaction();
                                        }
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if (twitterView.getTag() == VIEW_ADD) {
                    Intent intent = new Intent(context, EditSocialActivity.class);
                    intent.putExtra("network", "Twitter");
                    startActivityForResult(intent, 1);
                }
            }
        });

        instagramView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (instagramView.getTag() == VIEW_REMOVE) {
                    new AlertDialogWrapper.Builder(context)
                            .setMessage("Are you sure?")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    instagramView.setTag(VIEW_ADD);
                                    ((TextView) instagramView.findViewById(R.id.title)).setText("Add Instagram");
                                    for (int i = 0; i < card.getSocials().size(); i++) {
                                        if (card.getSocials().get(i).getNetwork().equals("instagram")) {
                                            realm2.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm2.commitTransaction();
                                        }
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if (instagramView.getTag() == VIEW_ADD) {
                    Intent intent = new Intent(context, EditSocialActivity.class);
                    intent.putExtra("network", "Instagram");
                    startActivityForResult(intent, 2);
                }
            }
        });

        snapchatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (snapchatView.getTag() == VIEW_REMOVE) {
                    new AlertDialogWrapper.Builder(context)
                            .setMessage("Are you sure?")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    snapchatView.setTag(VIEW_ADD);
                                    ((TextView) snapchatView.findViewById(R.id.title)).setText("Add Snapchat");
                                    for (int i = 0; i < card.getSocials().size(); i++) {
                                        if (card.getSocials().get(i).getNetwork().equals("snapchat")) {
                                            realm2.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm2.commitTransaction();
                                        }
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if (snapchatView.getTag() == VIEW_ADD) {
                    Intent intent = new Intent(context, EditSocialActivity.class);
                    intent.putExtra("network", "Snapchat");
                    startActivityForResult(intent, 3);
                }
            }
        });
    }

    private void addFacebookToCard(String id) {
        Realm realm = Realm.getInstance(context);
        SessionManager sessionManager = new SessionManager(context);
        final Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();
        final Card card = account.getCards().first();

        realm.beginTransaction();
        Social social = realm.createObject(Social.class);
        social.setNetwork("facebook");
        social.setUsername(id);
        card.getSocials().add(realm.copyToRealm(social));
        realm.commitTransaction();
        realm.close();
    }

    private void setImage(Account account) {
        profileImage = (ImageView) findViewById(R.id.picture);
        TextViewCustomFont profileImageText = (TextViewCustomFont) findViewById(R.id.picture_text);

        if (!account.getThumb().isEmpty() && !account.getThumb().equals("null")) {
            Picasso.with(this).load(account.getThumb())
                    .resize(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40)).transform(new CircleTransform()).into(profileImage);
            profileImageText.setText("Change picture");
        } else if (!account.getPicture().isEmpty() && !account.getPicture().equals("null")) {
            Picasso.with(this).load(account.getPicture())
                    .resize(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40)).transform(new CircleTransform()).into(profileImage);
            profileImageText.setText("Change picture");
        } else if (account.getPictureData() != null && account.getPictureData().length > 0) {
            photo = BitmapFactory.decodeByteArray(account.getPictureData(), 0, account.getPictureData().length);
            CircleTransform transform = new CircleTransform();
            circle = transform.transform(photo);
            profileImage.setImageBitmap(circle);
            profileImageText.setText("Change picture");
        } else {
            Picasso.with(this).load(R.drawable.default_profile)
                    .resize(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40)).transform(new CircleTransform()).into(profileImage);
            profileImageText.setText("Add picture");
        }

        LinearLayout profileLayout = (LinearLayout) findViewById(R.id.edit_picture);
        profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence[] items = {"Image from Camera", "Image from Library"};
                new MaterialDialog.Builder(context)
                        .items(items)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                if (which == 0) {
                                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    startActivityForResult(takePicture, 4);
                                } else {
                                    Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                    startActivityForResult(pickPhoto, 5);
                                }
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialogWrapper.Builder(context)
                .setMessage("Are you sure?")
                .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditProfileActivity.this.finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (callbackManager != null)
            callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == RESULT_OK) {
            fillViews();
        } else if (requestCode == 1 && resultCode == RESULT_OK) {
            twitterView.setTag(VIEW_REMOVE);
            fillViews();
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            instagramView.setTag(VIEW_REMOVE);
            fillViews();
        } else if (requestCode == 3 && resultCode == RESULT_OK) {
            snapchatView.setTag(VIEW_REMOVE);
            fillViews();
        } else if (requestCode == 4 && resultCode == RESULT_OK) {
            fillViews();

            Bitmap photo = (Bitmap) data.getExtras().get("data");

            Realm realm = Realm.getInstance(context);
            SessionManager sessionManager = new SessionManager(context);
            final Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();
            realm.beginTransaction();
            account.setPictureData(getBytesFromBitmap(photo));
            account.setPicture("");
            account.setThumb("");
            realm.commitTransaction();
            realm.close();

            CircleTransform transform = new CircleTransform();
            Bitmap circle = transform.transform(photo);
            profileImage.setImageBitmap(circle);
        } else if (requestCode == 5 && resultCode == RESULT_OK) {
            fillViews();

            Uri selectedImage = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                Realm realm = Realm.getInstance(context);
                SessionManager sessionManager = new SessionManager(context);
                final Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();
                realm.beginTransaction();
                account.setPictureData(getBytesFromBitmap(bitmap));
                account.setPicture("");
                account.setThumb("");
                realm.commitTransaction();
                realm.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Picasso.with(this).load(selectedImage)
                    .resize(Utils.dpToPx(context, 40), Utils.dpToPx(context, 40)).transform(new CircleTransform()).into(profileImage);
        }
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
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
    protected void onDestroy() {
        super.onDestroy();

        if (circle != null) circle.recycle();
        if (photo != null) photo.recycle();
        if (realm != null) realm.close();
        if (realm2 != null) realm2.close();
    }
}
