package com.yqrb.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    // 格式化日期
    public static String formatDate(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    // 获取当前日期
    public static Date getCurrentDate() {
        return new Date();
    }

    // 格式化当前日期为yyyyMMdd
    public static String getCurrentDateStr() {
        return formatDate(new Date(), "yyyyMMdd");
    }
}