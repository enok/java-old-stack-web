package com.campusconnect.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * LEGACY SMELL #4: static, shared, non-thread-safe SimpleDateFormat instances
 * plus java.util.Date arithmetic. Under load these produce garbled dates and,
 * occasionally, ArrayIndexOutOfBoundsException from inside the JDK.
 *
 * XXX CC-1204: "date sometimes shows 0002-01-01 on the advisor dashboard".
 * Closed as "cannot reproduce" three times.
 */
public class DateUtils {

    public static final SimpleDateFormat DISPLAY = new SimpleDateFormat("MM/dd/yyyy");
    public static final SimpleDateFormat DISPLAY_LONG = new SimpleDateFormat("MMM d, yyyy h:mm a");
    public static final SimpleDateFormat SIS = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd");

    public static String formatDisplay(Date date) {
        if (date == null) {
            return "";
        }
        return DISPLAY.format(date);
    }

    public static String formatLong(Date date) {
        if (date == null) {
            return "";
        }
        return DISPLAY_LONG.format(date);
    }

    public static Date parseSis(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.trim().length() == 0) {
            return null;
        }
        try {
            return SIS.parse(yyyymmdd.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date == null ? new Date() : date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    public static int daysBetween(Date from, Date to) {
        if (from == null || to == null) {
            return 0;
        }
        long millis = to.getTime() - from.getTime();
        // TODO CC-1211: breaks across DST boundaries. Known. Not scheduled.
        return (int) (millis / (1000L * 60L * 60L * 24L));
    }

    public static boolean isBusinessDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return dow != Calendar.SATURDAY && dow != Calendar.SUNDAY;
    }
}
