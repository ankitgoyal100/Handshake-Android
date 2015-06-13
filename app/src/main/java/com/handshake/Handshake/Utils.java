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
    public static short userCreated = 1;
    public static short userUpdated = 2;
    public static short userDeleted = 3;

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
}
