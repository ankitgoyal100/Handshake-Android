package com.handshake.Handshake;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    public static String toGmtString(Date date){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }
}
