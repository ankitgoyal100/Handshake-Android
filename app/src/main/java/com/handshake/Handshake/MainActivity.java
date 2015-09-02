package com.handshake.Handshake;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.astuetz.PagerSlidingTabStrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.handshake.helpers.AccountServerSync;
import com.handshake.helpers.CardServerSync;
import com.handshake.helpers.ContactServerSync;
import com.handshake.helpers.ContactUploader;
import com.handshake.helpers.FeedItemServerSync;
import com.handshake.helpers.GroupArraySyncCompleted;
import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.RequestServerSync;
import com.handshake.helpers.SuggestionsServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.helpers.UserSyncCompleted;
import com.handshake.listview.SearchAdapter;
import com.handshake.models.Account;
import com.handshake.models.Group;
import com.handshake.models.User;
import com.handshake.notifications.RegistrationIntentService;
import com.handshake.views.DelayAutoCompleteTextView;
import com.handshake.views.TextViewCustomFont;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.Realm;


public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    private static final int QR_CODE = 1;
    public static boolean contactSyncCompleted = false;
    private static ViewPager sPager;
    private final Handler handler = new Handler();
    public Context context = this;
    SessionManager session;
    int syncsCompleted = 0;
    private Drawable oldBackground = null;
    private PagerSlidingTabStrip tabs;
    private TabAdapter tabAdapter;
    private int TAG_CONTACTS = 0;
    private int TAG_ADD = 1;
    private ProfileFragment profileFragment;
    private FeedFragment feedFragment;
    private RequestFragment requestFragment;
    private GroupFragment groupFragment;
    private DelayAutoCompleteTextView searchView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);
        session.checkLogin();

        if (!session.isLoggedIn()) return;

        Toolbar v = (Toolbar) findViewById(R.id.actionBar);
        setSupportActionBar(v);

        final ImageButton contactButton = (ImageButton) v.findViewById(R.id.action_contacts);
        contactButton.setTag(TAG_CONTACTS);
        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactButton.getTag() == TAG_CONTACTS) {
                    Intent intent = new Intent(MainActivity.this, ContactActivity.class);
                    startActivity(intent);
                } else {
                    CharSequence[] items = {"Join Group", "Create Group", "Scan a QR Code"};
                    new MaterialDialog.Builder(context)
                            .items(items)
                            .itemsCallback(new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    if (which == 0) {
                                        Intent intent = new Intent(context, JoinGroupActivity.class);
                                        startActivity(intent);
//                                        dialog.cancel();
                                    } else if (which == 1) {
                                        Intent intent = new Intent(context, CreateEditGroupActivity.class);
                                        intent.putExtra("isEdit", false);
                                        startActivity(intent);
//                                        dialog.cancel();
                                    } else {
                                        startActivityForResult(new Intent(context, ScanActivity.class), QR_CODE);
                                    }
                                }
                            })
                            .show();
                }
            }
        });

        searchView = (DelayAutoCompleteTextView) v.findViewById(R.id.search);
        searchView.setAdapter(new SearchAdapter(this)); // 'this' is Activity instance
        searchView.setLoadingIndicator(
                (android.widget.ProgressBar) findViewById(R.id.pb_loading_indicator));
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Long userId = (Long) parent.getItemAtPosition(position);
                Realm realm = Realm.getInstance(context);
                User user = realm.where(User.class).equalTo("userId", userId).findFirst();
                Intent i;
                if (user.isContact()) {
                    i = new Intent(context, ContactUserProfileActivity.class);
                } else {
                    i = new Intent(MainActivity.this, GenericUserProfileActivity.class);
                }

                realm.close();
                i.putExtra("userId", userId);
                startActivity(i);

                searchView.setText("");
                searchView.clearFocus();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            searchView.setOnDismissListener(new AutoCompleteTextView.OnDismissListener() {
                @Override
                public void onDismiss() {
                    searchView.setText("");
                    searchView.clearFocus();
                }
            });
        }

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        sPager = (ViewPager) findViewById(R.id.pager);
        tabAdapter = new TabAdapter(getSupportFragmentManager());

        sPager.setOffscreenPageLimit(4);
        sPager.setAdapter(tabAdapter);

        tabs.setIndicatorColor(getResources().getColor(R.color.orange));
        tabs.setTabPaddingLeftRight(16);
        tabs.setShouldExpand(true);
        tabs.setViewPager(sPager);

        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                pageChanged(position, contactButton);
            }

            @Override
            public void onPageSelected(int position) {
                pageChanged(position, contactButton);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        changeColor(getResources().getColor(R.color.orange));

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (isConnected(context) && prefs.getBoolean("isFirstRun", true)) {
            prefs.edit().putBoolean("isFirstRun", false).apply();

            performSyncs(new SyncCompleted() {
                @Override
                public void syncCompletedListener() {
                    prefs.edit().putBoolean("isFirstRun", false).apply();
//                    System.out.println("All syncs completed");

                    if (profileFragment != null)
                        profileFragment.fillViews();
                    if (feedFragment != null) {
                        feedFragment.setIntroVisible();
                        feedFragment.setSuggestionText();
                    }
                    if (requestFragment != null) {
                        requestFragment.setIntroVisible();
                        requestFragment.setSuggestionText();
                    }
                    if (groupFragment != null)
                        groupFragment.setIntroVisible();

                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    String code = Utils.getCodes(context, clipboard.getPrimaryClip());
                    SessionManager sessionManager = new SessionManager(context);
                    if (!code.equals("") && !code.equals(sessionManager.getLastCopiedGroup())) {
                        checkCode(code);
                    }
                }
            });
        } else if (!isConnected(context)) {
            new AlertDialogWrapper.Builder(context)
                    .setTitle("No internet connection")
                    .setMessage("Please refresh the page after you have connected to the internet.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }

    private void pageChanged(int position, ImageButton contactButton) {
        if (position == 0 || position == 1 || position == 3) {
            contactButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_contacts_icon));
            contactButton.setTag(TAG_CONTACTS);
        } else {
            contactButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_add));
            contactButton.setTag(TAG_ADD);
        }
    }

    private void checkCode(final String code) {
        Realm realm = Realm.getInstance(context);
        Group group = realm.where(Group.class).notEqualTo("syncStatus", Utils.GroupDeleted).equalTo("code", code).findFirst();

        if (group != null) {
            return;
        }

        RestClientAsync.get(context, "/groups/find/" + code, new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                SessionManager sessionManager = new SessionManager(context);
                sessionManager.setLastCopiedGroup(code);

                LayoutInflater inflater = getLayoutInflater();
                View dialoglayout = inflater.inflate(R.layout.join_group_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(dialoglayout);
                final AlertDialog alertDialog = builder.create();
                alertDialog.show();

                try {
                    ArrayList<String> thumbUrls = new ArrayList<String>();

                    JSONArray members = response.getJSONObject("group").getJSONArray("members");
                    for (int i = 0; i < members.length(); i++) {
                        if (!members.getJSONObject(i).isNull("thumb")) {
                            thumbUrls.add(members.getJSONObject(i).getString("thumb"));
                        }
                    }

                    TextViewCustomFont groupCode = (TextViewCustomFont) dialoglayout.findViewById(R.id.group_code);
                    RelativeLayout groupPhoto1Options = (RelativeLayout) dialoglayout.findViewById(R.id.group_photo_1_options);
                    RelativeLayout groupPhoto2Options = (RelativeLayout) dialoglayout.findViewById(R.id.group_photo_2_options);
                    RelativeLayout groupPhoto3Options = (RelativeLayout) dialoglayout.findViewById(R.id.group_photo_3_options);
                    RelativeLayout groupPhoto4Options = (RelativeLayout) dialoglayout.findViewById(R.id.group_photo_4_options);

                    if (thumbUrls.size() == 0) {
                        groupCode.setVisibility(View.VISIBLE);
                        groupPhoto1Options.setVisibility(View.GONE);
                        groupPhoto2Options.setVisibility(View.GONE);
                        groupPhoto3Options.setVisibility(View.GONE);
                        groupPhoto4Options.setVisibility(View.GONE);

                        groupCode.setText((code.substring(0, 2) + "-" + code.substring(2, 4) + "-" + code.substring(4)).toUpperCase());
                    } else if (thumbUrls.size() == 1) {
                        groupCode.setVisibility(View.GONE);
                        groupPhoto1Options.setVisibility(View.VISIBLE);
                        groupPhoto2Options.setVisibility(View.GONE);
                        groupPhoto3Options.setVisibility(View.GONE);
                        groupPhoto4Options.setVisibility(View.GONE);

                        Picasso.with(context).load(thumbUrls.get(0))
                                .into((ImageView) groupPhoto1Options.findViewById(R.id.image));

                        groupPhoto1Options.findViewById(R.id.transparent_circle).setVisibility(View.GONE);
                        groupPhoto1Options.findViewById(R.id.transparent_circle_dialog).setVisibility(View.VISIBLE);
                    } else if (thumbUrls.size() == 2) {
                        groupCode.setVisibility(View.GONE);
                        groupPhoto1Options.setVisibility(View.GONE);
                        groupPhoto2Options.setVisibility(View.VISIBLE);
                        groupPhoto3Options.setVisibility(View.GONE);
                        groupPhoto4Options.setVisibility(View.GONE);

                        Picasso.with(context).load(thumbUrls.get(0))
                                .into((ImageView) groupPhoto2Options.findViewById(R.id.left_image));
                        Picasso.with(context).load(thumbUrls.get(1))
                                .into((ImageView) groupPhoto2Options.findViewById(R.id.right_image));

                        groupPhoto2Options.findViewById(R.id.transparent_circle).setVisibility(View.GONE);
                        groupPhoto2Options.findViewById(R.id.transparent_circle_dialog).setVisibility(View.VISIBLE);
                    } else if (thumbUrls.size() == 3) {
                        groupCode.setVisibility(View.GONE);
                        groupPhoto1Options.setVisibility(View.GONE);
                        groupPhoto2Options.setVisibility(View.GONE);
                        groupPhoto3Options.setVisibility(View.VISIBLE);
                        groupPhoto4Options.setVisibility(View.GONE);

                        Picasso.with(context).load(thumbUrls.get(0))
                                .into((ImageView) groupPhoto3Options.findViewById(R.id.top_left_image));
                        Picasso.with(context).load(thumbUrls.get(1))
                                .into((ImageView) groupPhoto3Options.findViewById(R.id.bottom_left_image));
                        Picasso.with(context).load(thumbUrls.get(2))
                                .into((ImageView) groupPhoto3Options.findViewById(R.id.right_image));

                        groupPhoto3Options.findViewById(R.id.transparent_circle).setVisibility(View.GONE);
                        groupPhoto3Options.findViewById(R.id.transparent_circle_dialog).setVisibility(View.VISIBLE);
                    } else {
                        groupCode.setVisibility(View.GONE);
                        groupPhoto1Options.setVisibility(View.GONE);
                        groupPhoto2Options.setVisibility(View.GONE);
                        groupPhoto3Options.setVisibility(View.GONE);
                        groupPhoto4Options.setVisibility(View.VISIBLE);

                        Picasso.with(context).load(thumbUrls.get(0))
                                .into((ImageView) groupPhoto4Options.findViewById(R.id.top_left_image));
                        Picasso.with(context).load(thumbUrls.get(1))
                                .into((ImageView) groupPhoto4Options.findViewById(R.id.bottom_left_image));
                        Picasso.with(context).load(thumbUrls.get(2))
                                .into((ImageView) groupPhoto4Options.findViewById(R.id.top_right_image));
                        Picasso.with(context).load(thumbUrls.get(3))
                                .into((ImageView) groupPhoto4Options.findViewById(R.id.bottom_right_image));

                        groupPhoto4Options.findViewById(R.id.transparent_circle).setVisibility(View.GONE);
                        groupPhoto4Options.findViewById(R.id.transparent_circle_dialog).setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                TextView text = (TextView) dialoglayout.findViewById(R.id.text);
                try {
                    text.setText("Want to join " + response.getJSONObject("group").getString("name") + "?");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                TextView exit = (TextView) dialoglayout.findViewById(R.id.no_thanks);
                exit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });

                Button join = (Button) dialoglayout.findViewById(R.id.join_group);
                join.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ProgressDialog dialog = ProgressDialog.show(context, "", "Joining group...", true);

                        Realm realm = Realm.getInstance(context);
                        SessionManager sessionManager = new SessionManager(context);
                        Account account = realm.where(Account.class).equalTo("userId", sessionManager.getID()).findFirst();

                        JSONArray cardIds = new JSONArray();
                        cardIds.put(account.getCards().first().getCardId());

                        JSONObject jsonParams = new JSONObject();
                        try {
                            jsonParams.put("code", code);
                            jsonParams.put("card_ids", cardIds);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        RestClientAsync.post(context, "/groups/join/", jsonParams, "application/json", new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    dialog.cancel();
                                    alertDialog.dismiss();
                                    JSONObject groupJSONObject = response.getJSONObject("group");
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.put(groupJSONObject);
                                    GroupServerSync.cacheGroup(jsonArray, new GroupArraySyncCompleted() {
                                        @Override
                                        public void syncCompletedListener(ArrayList<Group> groups) {
                                            final Group group = groups.get(0);
                                            GroupServerSync.loadGroupMembers(group);
                                            FeedItemServerSync.performSync(context, new SyncCompleted() {
                                                @Override
                                                public void syncCompletedListener() {
                                                    if (groupFragment != null) {
                                                        groupFragment.setIntroVisible();
                                                    }

                                                    if (feedFragment != null) {
                                                        feedFragment.setIntroVisible();
                                                    }
                                                }
                                            });
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                dialog.cancel();
                                Toast.makeText(context, "There was an error. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        });

                        realm.close();
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            }
        });

        realm.close();
    }

    private void performSyncs(final SyncCompleted listener) {
        ContactServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
                contactSyncCompleted = true;
//                System.out.println("Contact sync completed " + syncsCompleted);
            }
        });

        AccountServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
                if (profileFragment != null)
                    profileFragment.fillViews();
//                System.out.println("Account sync completed " + syncsCompleted);
            }
        });

        CardServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
                if (profileFragment != null)
                    profileFragment.fillViews();
//                System.out.println("Card sync completed " + syncsCompleted);
            }
        });

        FeedItemServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
                if (feedFragment != null)
                    feedFragment.setIntroVisible();
//                System.out.println("FeedItem sync completed " + syncsCompleted);
            }
        });

        GroupServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
                if (groupFragment != null)
                    groupFragment.setIntroVisible();
//                System.out.println("Group sync completed " + syncsCompleted);
            }
        });

        RequestServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
                if (requestFragment != null)
                    requestFragment.setIntroVisible();
//                System.out.println("Request sync completed " + syncsCompleted);
            }
        });

        AccountServerSync.sendUserLocation(context);

        ContactUploader.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
//                System.out.println("Contact upload sync completed");
                SuggestionsServerSync.performSync(context, new SyncCompleted() {
                    @Override
                    public void syncCompletedListener() {
                        syncsCompleted++;
                        if (feedFragment != null)
                            feedFragment.setSuggestionText();
                        if (requestFragment != null)
                            requestFragment.setSuggestionText();
//                        System.out.println("Suggestion sync completed " + syncsCompleted);
                    }
                });
            }
        });

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (syncsCompleted != 7) {
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.syncCompletedListener();
                    }
                });
            }
        }).start();
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

    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == QR_CODE && resultCode == Activity.RESULT_OK) {
            String code = data.getStringExtra(ScanActivity.RESULT_EXTRA_STR);
            if (!code.equals("")) {
                checkCode(code);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public void selectSearchView() {
        if (searchView != null) {
            searchView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public class TabAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        private final int[] ICONS = {
                R.mipmap.home_tab,
                R.mipmap.inbox_tab,
                R.mipmap.groups_tab,
                R.mipmap.profile_tab};

        public TabAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return ICONS.length;
        }

        @Override
        public int getPageIconResId(int position) {
            return ICONS[position];
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                feedFragment = FeedFragment.newInstance();
                return feedFragment;
            } else if (position == 1) {
                requestFragment = RequestFragment.newInstance();
                return requestFragment;
            } else if (position == 2) {
                groupFragment = GroupFragment.newInstance();
                return groupFragment;
            } else {
                profileFragment = ProfileFragment.newInstance();
                return profileFragment;
            }
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

    public static void setContactButtons(final Context context, final Long id,
                                         final ImageView buttonOne, final ImageView buttonTwo, final TextViewCustomFont text) {
        Realm realm = Realm.getInstance(context);
        final User account = realm.where(User.class).equalTo("userId", id).findFirst();

        String lastName = "";
        if (!account.getLastName().equals("null"))
            lastName = account.getLastName();
        final String name = account.getFirstName() + " " + lastName;

        if (account.isContact()) {
            buttonOne.setVisibility(View.GONE);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonTwo.setImageDrawable(context.getResources().getDrawable(R.drawable.contacts_button));
        } else if (account.isRequestReceived()) {
            buttonOne.setVisibility(View.VISIBLE);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonOne.setImageDrawable(context.getResources().getDrawable(R.drawable.decline_button));
            buttonTwo.setImageDrawable(context.getResources().getDrawable(R.drawable.accept_button));
        } else if (account.isRequestSent()) {
            buttonOne.setVisibility(View.GONE);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonTwo.setImageDrawable(context.getResources().getDrawable(R.drawable.requested_button));
        } else {
            buttonOne.setVisibility(View.GONE);
            buttonTwo.setVisibility(View.VISIBLE);
            buttonTwo.setImageDrawable(context.getResources().getDrawable(R.drawable.add_button));
        }

        if (text != null) {
            if (account.isRequestReceived()) {
                text.setText("Know " + name + "? Accept the request!");
            } else if (account.isRequestSent()) {
                text.setText("Your request is pending.");
            } else {
                text.setText("Know " + name + "? Send a request!");
            }
        }

        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (account.isRequestReceived()) {
                    buttonOne.setVisibility(View.GONE);
                    buttonTwo.setVisibility(View.GONE);

                    Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show();

                    RequestServerSync.declineRequest(context, account, new UserSyncCompleted() {
                        @Override
                        public void syncCompletedListener(User users) {
                            setContactButtons(context, id, buttonOne, buttonTwo, text);
                        }

                        @Override
                        public void syncFailedListener() {
                            Toast.makeText(context, "Could not decline request at this time. Please try again later.", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                setContactButtons(context, id, buttonOne, buttonTwo, text);
            }
        });

        buttonTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (account.isContact()) {
                    new AlertDialogWrapper.Builder(context)
                            .setTitle("Delete contact")
                            .setMessage("Are you sure you want to delete this contact?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ContactServerSync.deleteContact(account);
                                    setContactButtons(context, id, buttonOne, buttonTwo, text);
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if (account.isRequestReceived()) {
                    buttonOne.setVisibility(View.GONE);
                    buttonTwo.setVisibility(View.VISIBLE);
                    buttonTwo.setImageDrawable(context.getResources().getDrawable(R.drawable.contacts_button));

                    Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();

                    RequestServerSync.acceptRequest(context, account, new UserSyncCompleted() {
                        @Override
                        public void syncCompletedListener(User users) {
                            if (text == null) {
                                setContactButtons(context, id, buttonOne, buttonTwo, text);
                            } else {
                                Intent i = new Intent(context, ContactUserProfileActivity.class);
                                i.putExtra("userId", account.getUserId());
                                context.startActivity(i);
                                ((Activity) context).finish();
                            }
                        }

                        @Override
                        public void syncFailedListener() {
                            Toast.makeText(context, "Could not accept request at this time. Please try again later.", Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (account.isRequestSent()) {
                    new AlertDialogWrapper.Builder(context)
                            .setTitle("Delete request")
                            .setMessage("Are you sure you want to delete this request?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    RequestServerSync.deleteRequest(context, account, new UserSyncCompleted() {
                                        @Override
                                        public void syncCompletedListener(User users) {
                                            setContactButtons(context, id, buttonOne, buttonTwo, text);
                                        }

                                        @Override
                                        public void syncFailedListener() {

                                        }
                                    });
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else {
                    RequestServerSync.sendRequest(context, account, new UserSyncCompleted() {
                        @Override
                        public void syncCompletedListener(User users) {
                            buttonOne.setVisibility(View.GONE);
                            buttonTwo.setVisibility(View.VISIBLE);
                            buttonTwo.setImageDrawable(context.getResources().getDrawable(R.drawable.requested_button));
                            setContactButtons(context, id, buttonOne, buttonTwo, text);
                        }

                        @Override
                        public void syncFailedListener() {
                            Toast.makeText(context, "Could not sent request at this time. Please try again later.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            return (mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting());
        } else return false;
    }
}
