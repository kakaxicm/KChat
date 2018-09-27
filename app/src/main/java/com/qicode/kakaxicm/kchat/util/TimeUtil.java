package com.qicode.kakaxicm.kchat.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chenming on 2018/9/22
 */
public class TimeUtil {
    public static String getHourStrTime(long timestamp) {
        return getStrTime(timestamp, "HH:mm");
    }

    public static String getStrTime (long timestamp, String format) {

        String timeString = null;
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        timeString = sdf.format(new Date(timestamp));
        return timeString;
    }
}
