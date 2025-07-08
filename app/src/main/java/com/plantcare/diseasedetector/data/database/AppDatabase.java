package com.plantcare.diseasedetector.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.data.models.DiseaseInfo;

/**
 * Room Database for Plant Disease Detector App
 * Manages all database operations and provides DAOs
 */
@Database(
        entities = {ScanResult.class, DiseaseInfo.class},
        version = 2,
        exportSchema = false
)
@TypeConverters({DatabaseConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    // Database name
    private static final String DATABASE_NAME = "plant_disease_database";

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    /**
     * Get DAO for scan results
     */
    public abstract ScanResultDao scanResultDao();

    /**
     * Get DAO for disease information
     */
    public abstract DiseaseInfoDao diseaseInfoDao();

    /**
     * Get singleton database instance
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration() // Handle schema changes
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Close database (for testing purposes)
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}