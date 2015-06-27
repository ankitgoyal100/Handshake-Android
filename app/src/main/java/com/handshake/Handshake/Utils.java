package com.handshake.Handshake;

import android.content.ClipData;
import android.content.Context;

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
    public static short userSynced = 0;
    public static short userDeleted = 1;

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
        if(clipboard == null) return "";
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < clipboard.getItemCount(); i++) {
            String[] s = clipboard.getItemAt(i).coerceToText(context).toString().split("\\s+");
            for (int j = 0; j < s.length; j++)
                strings.add(s[j]);
        }

        for (int i = 0; i < strings.size(); i++) {
            if (strings.get(i).length() == 8 && strings.get(i).charAt(2) == '-' && strings.get(i).charAt(5) == '-') {
                return (strings.get(i).substring(0, 2) + strings.get(i).substring(3, 5) +
                        strings.get(i).substring(6)).toLowerCase();
            }
        }

        return "";
    }
}