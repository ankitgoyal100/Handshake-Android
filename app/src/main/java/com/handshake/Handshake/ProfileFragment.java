package com.handshake.Handshake;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.editor.EditProfileActivity;
import com.handshake.listview.CircleTransform;
import com.handshake.models.Account;
import com.handshake.models.Address;
import com.handshake.models.Card;
import com.handshake.models.Email;
import com.handshake.models.Phone;
import com.handshake.models.Social;
import com.handshake.views.TextViewCustomFont;
import com.squareup.picasso.Picasso;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;

/**
 * Created by ankitgoyal on 6/27/15.
 */
public class ProfileFragment extends Fragment {
    private Handler handler = new Handler();
    private LinearLayout infoLayout;
    private LinearLayout socialLayout;
    private static Executor executor = Executors.newSingleThreadExecutor();

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        infoLayout = (LinearLayout) getView().findViewById(R.id.linear_layout);
        socialLayout = (LinearLayout) getView().findViewById(R.id.linear_layout_2);
        fillViews();
    }

    private void fillViews() {
        final Realm realm = Realm.getInstance(getActivity());
        final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
        final Card card = account.getCards().first();

        System.out.println("HERE");
        for (Address a : card.getAddresses())
            System.out.println("Address3: " + a.toString());

        TextViewCustomFont name = (TextViewCustomFont) getView().findViewById(R.id.name);
        String lastName = "";
        if (!account.getLastName().equals("null"))
            lastName = account.getLastName();
        name.setText(account.getFirstName() + " " + lastName);

        CircleImageView profileImage = (CircleImageView) getView().findViewById(R.id.profile_image);
        ImageView backdrop = (ImageView) getView().findViewById(R.id.backdrop);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) getView().findViewById(R.id.collapsing_toolbar);

        if (!account.getThumb().isEmpty() && !account.getThumb().equals("null")) {
            Picasso.with(getActivity()).load(account.getThumb()).transform(new CircleTransform()).into(profileImage);
            Picasso.with(getActivity()).load(account.getThumb()).into(backdrop);
        } else {
            Picasso.with(getActivity()).load(R.drawable.default_profile).transform(new CircleTransform()).into(profileImage);
            collapsingToolbar.setContentScrimColor(getResources().getColor(R.color.background_window));
        }

        FloatingActionButton editButton = (FloatingActionButton) getView().findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), EditProfileActivity.class);
                startActivity(i);
            }
        });

//        final ProgressDialog dialog = ProgressDialog.show(getActivity(), "", "Loading profile...", true);
//
//        executor.execute(new Runnable() {
//            @Override
//            public void run() {
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
        infoLayout.removeAllViews();
        socialLayout.removeAllViews();
//                    }
//                });
//
//                while (!MainActivity.cardSyncCompleted) {
//
//                }
//
//                Realm realm = Realm.getInstance(getActivity());
//
//                final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
//                final Card card = account.getCards().first();

        for (final Phone phone : card.getPhones()) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mLinearView = inflater.inflate(R.layout.info_cell, null);
            final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
            final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
            final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
            final ImageView imageView2 = (ImageView) mLinearView.findViewById(R.id.imageView2);
            final String phoneNumber = phone.getNumber();
            final String phoneCountryCode = phone.getCountryCode();
            final String phoneLabel = phone.getLabel();


//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            dialog.cancel();

            getView().findViewById(R.id.divider1).setVisibility(View.VISIBLE);

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
//                    });
//                }

        for (final Email email : card.getEmails()) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mLinearView = inflater.inflate(R.layout.info_cell, null);
            final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
            final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
            final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
            final ImageView imageView2 = (ImageView) mLinearView.findViewById(R.id.imageView2);
            final String emailAddress = email.getAddress();
            final String emailLabel = email.getLabel();

//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
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
//                    });
//                }

        for (int i = 0; i < card.getAddresses().size(); i++)
            System.out.println("Address2: " + card.getAddresses().get(i).toString());

        for (final Address address : card.getAddresses()) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
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

//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
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
//                    });
//                }

        for (final Social social : card.getSocials()) {
            LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View mLinearView = inflater.inflate(R.layout.social_cell, null);
            final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
            final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
            final String username = social.getUsername();
            final String network = social.getNetwork();

//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
            if (card.getSocials().size() == 0) {
                getView().findViewById(R.id.divider1).setVisibility(View.GONE);
            } else {
                getView().findViewById(R.id.divider2).setVisibility(View.VISIBLE);
            }

            if (network.equals("facebook")) {
                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.facebook_icon));
                title.setText("Facebook");
                mLinearView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            getActivity().getPackageManager().getPackageInfo("com.facebook.katana", 0);
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
                        Toast.makeText(getActivity(),
                                "Unable to open Snapchat. Please manually add the user via the Snapchat application.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            socialLayout.addView(mLinearView);
        }
//                    });
//                }
//            }
//        });
    }

    @Override
    public void onResume() {
        super.onResume();

        final Realm realm = Realm.getInstance(getActivity());
        final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
        final Card card = account.getCards().first();

        System.out.println("HERE");
        for (Address a : card.getAddresses())
            System.out.println("Address1: " + a.toString());

        fillViews();
    }
}
