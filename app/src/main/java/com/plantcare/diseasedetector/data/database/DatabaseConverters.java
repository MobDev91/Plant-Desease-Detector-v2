package com.plantcare.diseasedetector.data.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Type converters for Room database
 * Handles conversion of complex types to database-compatible types
 */
public class DatabaseConverters {

    /**
     * Convert timestamp to Date object
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * Convert Date object to timestamp
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}