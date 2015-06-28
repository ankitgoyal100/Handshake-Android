package com.handshake.Handshake;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.handshake.helpers.AccountServerSync;
import com.handshake.helpers.CardServerSync;
import com.handshake.helpers.ContactServerSync;
import com.handshake.helpers.FeedItemServerSync;
import com.handshake.helpers.GroupArraySyncCompleted;
import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.RequestServerSync;
import com.handshake.helpers.SuggestionsServerSync;
import com.handshake.helpers.SyncCompleted;
import com.handshake.listview.CircleTransform;
import com.handshake.models.Account;
import com.handshake.models.Group;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.realm.Realm;


public class MainActivity extends ActionBarActivity {

    private final Handler handler = new Handler();
    private Drawable oldBackground = null;
    public Context context = this;

    SessionManager session;

    private PagerSlidingTabStrip tabs;
    private TabAdapter tabAdapter;
    private static ViewPager sPager;

    int syncsCompleted = 0;

    private int TAG_CONTACTS = 0;
    private int TAG_ADD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);
        session.checkLogin();

        if (!session.isLoggedIn()) return;

        this.getSupportActionBar().setDisplayShowCustomEnabled(true);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        this.getSupportActionBar().setDisplayShowHomeEnabled(false);

        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar, null);
        this.getSupportActionBar().setCustomView(v);

        final ImageButton contactButton = (ImageButton) v.findViewById(R.id.action_contacts);
        contactButton.setTag(TAG_CONTACTS);
        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (contactButton.getTag() == TAG_CONTACTS) {
                    Intent intent = new Intent(MainActivity.this, ContactActivity.class);
                    startActivity(intent);
                } else {
                    new AlertDialog.Builder(context)
                            .setMessage("Would you like to create a group or join a group?")
                            .setPositiveButton("Join Group", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(context, JoinGroupActivity.class);
                                    startActivity(intent);
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton("Create Group", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(context, CreateEditGroupActivity.class);
                                    intent.putExtra("isEdit", false);
                                    startActivity(intent);
                                    dialog.cancel();
                                }
                            })
                            .show();
                }
            }
        });

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        sPager = (ViewPager) findViewById(R.id.pager);
        tabAdapter = new TabAdapter(this, getSupportFragmentManager());

        sPager.setOffscreenPageLimit(4);
        sPager.setAdapter(tabAdapter);

        tabs.setIndicatorColor(getResources().getColor(R.color.orange));
        tabs.setTabPaddingLeftRight(16);
        tabs.setShouldExpand(true);
        tabs.setViewPager(sPager);

        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0 || position == 1 || position == 3) {
                    contactButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_contacts_icon));
                    contactButton.setTag(TAG_CONTACTS);
                } else {
                    contactButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_add));
                    contactButton.setTag(TAG_ADD);
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0 || position == 1 || position == 3) {
                    contactButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_contacts_icon));
                    contactButton.setTag(TAG_CONTACTS);
                } else {
                    contactButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_add));
                    contactButton.setTag(TAG_ADD);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        changeColor(getResources().getColor(R.color.orange));

        performSyncs(new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                String code = Utils.getCodes(context, clipboard.getPrimaryClip());
                if (code != "" && code != SessionManager.getLastCopiedGroup()) {
                    checkCode(code);
                }
            }
        });
    }

    private void checkCode(final String code) {
        Realm realm = Realm.getInstance(context);
        Group group = realm.where(Group.class).equalTo("code", code).findFirst();

        if (group != null) {
            return;
        }

        RestClientAsync.get(context, "/groups/find/" + code, new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                SessionManager.setLastCopiedGroup(code);

                LayoutInflater inflater = getLayoutInflater();
                View dialoglayout = inflater.inflate(R.layout.join_group_dialog, null);

                //TODO: fix group icon
                ImageView groupIcon = (ImageView) dialoglayout.findViewById(R.id.group_icon);
                Picasso.with(context).load(R.drawable.default_profile).transform(new CircleTransform()).into(groupIcon);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(dialoglayout);
                final AlertDialog alertDialog = builder.create();
                alertDialog.show();

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
                        Realm realm = Realm.getInstance(context);
                        Account account = realm.where(Account.class).equalTo("userId", SessionManager.getID()).findFirst();

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
                                    alertDialog.dismiss();
                                    System.out.println(response.toString());
                                    JSONObject groupJSONObject = response.getJSONObject("group");
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.put(groupJSONObject);
                                    GroupServerSync.cacheGroup(jsonArray, new GroupArraySyncCompleted() {
                                        @Override
                                        public void syncCompletedListener(ArrayList<Group> groups) {
                                            System.out.println(groups.toString());
                                            Group group = groups.get(0);
                                            GroupServerSync.loadGroupMembers(group);
                                            FeedItemServerSync.performSync(context, new SyncCompleted() {
                                                @Override
                                                public void syncCompletedListener() {

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
                                Toast.makeText(context, "There was an error. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
            }
        });
    }

    private void performSyncs(final SyncCompleted listener) {
        ContactServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("Contact sync completed " + syncsCompleted);
            }
        });

        AccountServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("Account sync completed " + syncsCompleted);
            }
        });

        CardServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("Card sync completed " + syncsCompleted);
            }
        });

        FeedItemServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("FeedItem sync completed " + syncsCompleted);
            }
        });

        GroupServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("Group sync completed " + syncsCompleted);
            }
        });

        RequestServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("Request sync completed " + syncsCompleted);
            }
        });

        SuggestionsServerSync.performSync(context, new SyncCompleted() {
            @Override
            public void syncCompletedListener() {
                syncsCompleted++;
//                System.out.println("Suggestion sync completed " + syncsCompleted);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (syncsCompleted != 7) {
                }
                System.out.println("All syncs completed!");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.syncCompletedListener();
                    }
                });
            }
        }).start();
    }

    public class TabAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        private final int[] ICONS = {
                R.mipmap.home_tab,
                R.mipmap.inbox_tab,
                R.mipmap.groups_tab,
                R.mipmap.profile_tab};
        private Activity activity;
        private FragmentManager fragmentManager;

        public TabAdapter(Activity a, FragmentManager fm) {
            super(fm);
            fragmentManager = fm;
            activity = a;
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
            if (position == 0)
                return FeedFragment.newInstance();
            else if (position == 1)
                return RequestFragment.newInstance();
            else if (position == 2)
                return GroupFragment.newInstance();
            else
                return RequestFragment.newInstance();
        }
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

    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
