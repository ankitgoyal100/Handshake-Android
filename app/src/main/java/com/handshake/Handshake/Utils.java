package com.handshake.Handshake;

import android.content.ClipData;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by ankitgoyal on 6/13/15.
 */
public class Utils {
    public static String[] threeLabels = {"Home", "Work", "Other"};
    public static String[] fourLabels = {"Home", "Mobile", "Work", "Other"};

    public static short UserSynced = 0;
    public static short UserDeleted = 1;

    public static short CardSynced = 0;
    public static short CardCreated = 1;
    public static short CardUpdated = 2;
    public static short CardDeleted = 3;

    public static short AccountSynced = 0;
    public static short AccountUpdated = 1;

    public static short GroupSynced = 0;
    public static short GroupCreated = 1;
    public static short GroupUpdated = 2;
    public static short GroupDeleted = 3;

    public static int getIndexOfLabel(String label, boolean isThree) {
        if (label.equals("Home")) return 0;
        else if (label.equals("Mobile") && !isThree) return 1;
        else if (label.equals("Work") && !isThree) return 2;
        else if (label.equals("Work") && isThree) return 1;
        else if (label.equals("Other") && !isThree) return 3;
        else if (label.equals("Other") && isThree) return 2;

        return 0;
    }

    public static int labelToType(String label, boolean isPhone) {
        if(label.equals("Home")) return 1;
        else if(label.equals("Mobile") && isPhone) return 2;
        else if(label.equals("Mobile") && !isPhone) return 4;
        else if(label.equals("Work") && isPhone) return 3;
        else if(label.equals("Work") && !isPhone) return 2;
        else if(label.equals("Other") && isPhone) return 7;
        else return 3;
    }

    public static Date formatDate(String str) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = null;
        try {
            date = format.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    public static String toGmtString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    public static String getTimeSince(Date createdAt) {
        Date currentDate = new Date(System.currentTimeMillis());
        int time = (int) ((currentDate.getTime() - createdAt.getTime()) / 1000.0);

        String message;

        if (time < 60) {
            if (time == 1)
                message = "1 second ago";
            else
                message = time + " seconds ago";
        } else {
            time /= 60;

            if (time < 60) {
                if (time == 1)
                    message = "1 minute ago";
                else
                    message = time + " minutes ago";
            } else {
                time /= 60;

                if (time < 24) {
                    if (time == 1)
                        message = "1 hour ago";
                    else
                        message = time + " hours ago";
                } else {
                    time /= 24;

                    if (time < 7) {
                        if (time == 1)
                            message = "1 day ago";
                        else
                            message = time + " days ago";
                    } else {
                        time /= 7;

                        if (time < 4.34812) {
                            if (time == 1)
                                message = "1 week ago";
                            else
                                message = time + " weeks ago";
                        } else {
                            time /= 4.34812;

                            if (time < 12) {
                                if (time == 1)
                                    message = "1 month ago";
                                else
                                    message = time + " months ago";
                            } else {
                                time /= 12;

                                if (time == 1)
                                    message = "1 year ago";
                                else
                                    message = time + " years ago";
                            }
                        }
                    }
                }
            }
        }

        return message;
    }

    public static String getCodes(Context context, ClipData clipboard) {
        if (clipboard == null) return "";
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < clipboard.getItemCount(); i++) {
            String[] s = clipboard.getItemAt(i).coerceToText(context).toString().replaceAll("[^-a-zA-Z0-9\\s]", "").split("\\s+");
            for (int j = 0; j < s.length; j++)
                strings.add(s[j]);
        }

        for (int i = 0; i < strings.size(); i++) {
            String[] stringSplitByDashes = strings.get(i).split("-");
            if(stringSplitByDashes.length == 3 && stringSplitByDashes[0].length() == 2
                    && stringSplitByDashes[1].length() == 2 && stringSplitByDashes[2].length() == 2)
                return strings.get(i).replaceAll("-", "").toLowerCase();
        }

        return "";
    }

    public static void setDynamicHeight(ListView mListView) {
        ListAdapter mListAdapter = mListView.getAdapter();
        if (mListAdapter == null) {
            // when adapter is null
            return;
        }
        int height = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        for (int i = 0; i < mListAdapter.getCount(); i++) {
            View listItem = mListAdapter.getView(i, null, mListView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            height += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = mListView.getLayoutParams();
        params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
        mListView.setLayoutParams(params);
        mListView.requestLayout();
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) Math.ceil(dp * density);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
