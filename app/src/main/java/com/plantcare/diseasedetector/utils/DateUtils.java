package com.plantcare.diseasedetector.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for date and time operations
 */
public class DateUtils {

    // Date format patterns
    private static final String PATTERN_FULL_DATE = "MMMM dd, yyyy";
    private static final String PATTERN_SHORT_DATE = "MMM dd, yyyy";
    private static final String PATTERN_TIME = "HH:mm";
    private static final String PATTERN_DATE_TIME = "MMM dd, yyyy HH:mm";

    /**
     * Get time ago string from a given date
     * Examples: "2 minutes ago", "1 hour ago", "3 days ago"
     */
    public static String getTimeAgo(Date date) {
        if (date == null) {
            return "Unknown";
        }

        long currentTime = System.currentTimeMillis();
        long givenTime = date.getTime();
        long diffInMillis = currentTime - givenTime;

        // Convert milliseconds to different time units
        long seconds = diffInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        } else if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        } else if (days < 7) {
            return days == 1 ? "1 day ago" : days + " days ago";
        } else if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        } else if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        } else {
            return years == 1 ? "1 year ago" : years + " years ago";
        }
    }

    /**
     * Get formatted date string
     */
    public static String getFormattedDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_SHORT_DATE, Locale.getDefault());
        return formatter.format(date);
    }

    /**
     * Get formatted date and time string
     */
    public static String getFormattedDateTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_DATE_TIME, Locale.getDefault());
        return formatter.format(date);
    }

    /**
     * Get formatted time string
     */
    public static String getFormattedTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_TIME, Locale.getDefault());
        return formatter.format(date);
    }

    /**
     * Get full formatted date string
     */
    public static String getFullFormattedDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_FULL_DATE, Locale.getDefault());
        return formatter.format(date);
    }

    /**
     * Check if date is today
     */
    public static boolean isToday(Date date) {
        if (date == null) {
            return false;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        String givenDate = dateFormat.format(date);

        return today.equals(givenDate);
    }

    /**
     * Check if date is yesterday
     */
    public static boolean isYesterday(Date date) {
        if (date == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long yesterdayTime = currentTime - (24 * 60 * 60 * 1000); // 24 hours ago
        Date yesterday = new Date(yesterdayTime);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String yesterdayStr = dateFormat.format(yesterday);
        String givenDateStr = dateFormat.format(date);

        return yesterdayStr.equals(givenDateStr);
    }

    /**
     * Get current date and time
     */
    public static Date getCurrentDate() {
        return new Date();
    }

    /**
     * Get friendly date string (Today, Yesterday, or formatted date)
     */
    public static String getFriendlyDate(Date date) {
        if (date == null) {
            return "Unknown";
        }

        if (isToday(date)) {
            return "Today";
        } else if (isYesterday(date)) {
            return "Yesterday";
        } else {
            return getFormattedDate(date);
        }
    }

    public static String getShortDate(Date date) {
        if (date == null) return "";

        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd", Locale.getDefault());
        return formatter.format(date);
    }

}