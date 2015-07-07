package com.handshake.editor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.Handshake.R;
import com.handshake.Handshake.SessionManager;
import com.handshake.Handshake.SettingsActivity;
import com.handshake.Handshake.Utils;
import com.handshake.helpers.AccountServerSync;
import com.handshake.helpers.CardServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.CircleTransform;
import com.handshake.models.Account;
import com.handshake.models.Address;
import com.handshake.models.Card;
import com.handshake.models.Email;
import com.handshake.models.Phone;
import com.handshake.models.Social;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        changeColor(getResources().getColor(R.color.orange));

        fillViews();

        Button saveButton = (Button) findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Realm realm = Realm.getInstance(context);
                final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
                final Card card = account.getCards().first();

                realm.beginTransaction();
                account.setSyncStatus(Utils.AccountUpdated);
                card.setSyncStatus(Utils.CardUpdated);
                realm.commitTransaction();

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

                finish();
            }
        });
    }

    private void fillViews() {
        final Realm realm = Realm.getInstance(this);
        final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
        final Card card = account.getCards().first();

        setName(account);
        setImage(account);
        setContactInformation(card);
        setSocials(card);

        TextViewCustomFont addInformation = (TextViewCustomFont) findViewById(R.id.add_information);
        addInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, AddContactActivity.class);
                startActivityForResult(i, 0);
            }
        });
    }

    private void setContactInformation(Card card) {
        if (card.getPhones().size() == 0 && card.getEmails().size() == 0 && card.getAddresses().size() == 0)
            findViewById(R.id.divider).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.divider).setVisibility(View.GONE);

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

        final Realm realm = Realm.getInstance(context);

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
                                            realm.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm.commitTransaction();
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
                    //TODO: add fb view
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
                                            realm.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm.commitTransaction();
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
                                            realm.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm.commitTransaction();
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
                                            realm.beginTransaction();
                                            card.getSocials().get(i).removeFromRealm();
                                            realm.commitTransaction();
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

    private void setImage(Account account) {
        profileImage = (ImageView) findViewById(R.id.picture);

        if (!account.getThumb().isEmpty() && !account.getThumb().equals("null")) {
            Picasso.with(this).load(account.getThumb()).transform(new CircleTransform()).into(profileImage);
        } else {
            Picasso.with(this).load(R.drawable.default_profile).transform(new CircleTransform()).into(profileImage);
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
                                    startActivityForResult(pickPhoto, 4);
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
            updateImage(profileImage, data);
        }
    }

    private void updateImage(final ImageView profileImage, Intent data) {
        Uri selectedImage = data.getData();
        Picasso.with(this).load(selectedImage).transform(new CircleTransform()).into(profileImage, new Callback() {
            @Override
            public void onSuccess() {
                Realm realm = Realm.getInstance(context);
                final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
                realm.beginTransaction();
                account.setPictureData(getBytesFromBitmap(((BitmapDrawable) profileImage.getDrawable()).getBitmap()));
                realm.commitTransaction();
            }

            @Override
            public void onError() {

            }
        });
    }

    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_profile, menu);
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
            Intent i = new Intent(EditProfileActivity.this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
