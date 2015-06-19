package com.handshake.Handshake;

import android.app.Activity;
import android.content.Context;
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

import com.astuetz.PagerSlidingTabStrip;
import com.handshake.helpers.AccountServerSync;
import com.handshake.helpers.CardServerSync;
import com.handshake.helpers.ContactServerSync;
import com.handshake.helpers.FeedItemServerSync;
import com.handshake.helpers.GroupServerSync;
import com.handshake.helpers.RequestServerSync;
import com.handshake.helpers.SuggestionsServerSync;
import com.handshake.helpers.SyncCompleted;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);
        session.checkLogin();

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        sPager = (ViewPager) findViewById(R.id.pager);
        tabAdapter = new TabAdapter(this, getSupportFragmentManager());

        sPager.setOffscreenPageLimit(4);
        sPager.setAdapter(tabAdapter);

        tabs.setIndicatorColor(getResources().getColor(R.color.orange));
        tabs.setTabPaddingLeftRight(16);
        tabs.setShouldExpand(true);
        tabs.setViewPager(sPager);

        changeColor(getResources().getColor(R.color.orange));

        performSyncs();
    }

    private void performSyncs() {
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
            return HomeFragment.newInstance();
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
