package com.handshake.Handshake;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.handshake.listview.CircleTransform;
import com.handshake.models.Account;
import com.handshake.models.Address;
import com.handshake.models.Card;
import com.handshake.models.Email;
import com.handshake.models.Phone;
import com.handshake.models.Social;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;

/**
 * Created by ankitgoyal on 6/27/15.
 */
public class ProfileFragment extends Fragment {
    private Handler handler = new Handler();

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

        final Realm realm = Realm.getInstance(getActivity());
        final Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

        TextViewCustomFont name = (TextViewCustomFont) getView().findViewById(R.id.name);
        name.setText(account.getFirstName() + " " + account.getLastName());

        getView().findViewById(R.id.divider1).setBackgroundColor(MainActivity.dividerColor);
        getView().findViewById(R.id.divider2).setBackgroundColor(MainActivity.dividerColor);

        if (account.getCards().first().getSocials().size() == 0)
            getView().findViewById(R.id.divider2).setVisibility(View.GONE);

        CircleImageView profileImage = (CircleImageView) getView().findViewById(R.id.profile_image);
        ImageView backdrop = (ImageView) getView().findViewById(R.id.backdrop);
        CollapsingToolbarLayout collabsingToolbar = (CollapsingToolbarLayout) getView().findViewById(R.id.collapsing_toolbar);

        if (!account.getThumb().isEmpty() && !account.getThumb().equals("null")) {
            Picasso.with(getActivity()).load(account.getThumb()).transform(new CircleTransform()).into(profileImage);
            Picasso.with(getActivity()).load(account.getThumb()).transform(new CircleTransform()).into(backdrop);
        } else {
            Picasso.with(getActivity()).load(R.drawable.default_profile).transform(new CircleTransform()).into(profileImage);
            collabsingToolbar.setContentScrimColor(getResources().getColor(R.color.background_window));
        }

        final LinearLayout infoLayout = (LinearLayout) getView().findViewById(R.id.linear_layout);
        final LinearLayout socialLayout = (LinearLayout) getView().findViewById(R.id.linear_layout_2);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!MainActivity.cardSyncCompleted) {

                }

                Realm realm = Realm.getInstance(getActivity());

                Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();
                Card card = account.getCards().first();

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
                    final View divider = mLinearView.findViewById(R.id.divider);


                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            divider.setBackgroundColor(MainActivity.dividerColor);

                            imageView1.setVisibility(View.VISIBLE);
                            imageView2.setVisibility(View.VISIBLE);
                            imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.message_button));
                            imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.call_button));

                            description.setText(phoneLabel);
                            infoLayout.addView(mLinearView);

                            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                            try {
                                Phonenumber.PhoneNumber numberObject = phoneUtil.parse(phoneNumber, phoneCountryCode);
                                System.out.println(numberObject.toString());
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
                    LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View mLinearView = inflater.inflate(R.layout.info_cell, null);
                    final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
                    final TextViewCustomFont description = (TextViewCustomFont) mLinearView.findViewById(R.id.description);
                    final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
                    final ImageView imageView2 = (ImageView) mLinearView.findViewById(R.id.imageView2);
                    final String emailAddress = email.getAddress();
                    final String emailLabel = email.getLabel();
                    final View divider = mLinearView.findViewById(R.id.divider);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            divider.setBackgroundColor(MainActivity.dividerColor);
                            imageView1.setVisibility(View.GONE);
                            imageView2.setVisibility(View.VISIBLE);
                            imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.email_button));

                            title.setText(emailAddress);
                            description.setText(emailLabel);
                            infoLayout.addView(mLinearView);
                        }
                    });
                }

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
                    final View divider = mLinearView.findViewById(R.id.divider);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            divider.setBackgroundColor(MainActivity.dividerColor);
                            imageView1.setVisibility(View.GONE);
                            imageView2.setVisibility(View.VISIBLE);
                            imageView2.setImageDrawable(getResources().getDrawable(R.mipmap.maps_button));

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
                    LayoutInflater inflater = (LayoutInflater) getActivity().getApplicationContext()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View mLinearView = inflater.inflate(R.layout.social_cell, null);
                    final TextViewCustomFont title = (TextViewCustomFont) mLinearView.findViewById(R.id.title);
                    final ImageView imageView1 = (ImageView) mLinearView.findViewById(R.id.imageView1);
                    final String username = social.getUsername();
                    final String network = social.getNetwork();
                    final View divider = mLinearView.findViewById(R.id.divider);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            divider.setBackgroundColor(MainActivity.dividerColor);

                            if (network.equals("facebook")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.facebook_icon));
                                title.setText(username);
                            } else if (network.equals("twitter")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.twitter_icon));
                                title.setText("@" + username);
                            } else if (network.equals("instagram")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.instagram_icon));
                                title.setText("@" + username);
                            } else if (network.equals("snapchat")) {
                                imageView1.setImageDrawable(getResources().getDrawable(R.mipmap.snapchat_icon));
                                title.setText(username);
                            }

                            socialLayout.addView(mLinearView);
                        }
                    });
                }
            }
        }).start();
    }
}
