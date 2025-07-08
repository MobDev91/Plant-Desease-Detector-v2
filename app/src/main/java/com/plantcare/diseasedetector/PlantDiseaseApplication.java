package com.plantcare.diseasedetector;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.plantcare.diseasedetector.utils.PerformanceUtils;

/**
 * Application class for PlantDiseaseDetector
 * Initializes app-wide optimizations and performance monitoring
 */
public class PlantDiseaseApplication extends Application {

    private static final String TAG = "PlantDiseaseApp";
    private static PlantDiseaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            instance = this;

            // Log application startup
            Log.i(TAG, "PlantDiseaseDetector Application starting...");

            // Initialize performance optimizations
            initializeOptimizations();

            // Log system information for debugging
            PerformanceUtils.logSystemInfo(this);

            Log.i(TAG, "Application initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing application", e);
        }
    }

    /**
     * Get application instance
     */
    public static PlantDiseaseApplication getInstance() {
        return instance;
    }

    /**
     * Get application context safely
     */
    public static Context getAppContext() {
        return instance != null ? instance.getApplicationContext() : null;
    }

    /**
     * Initialize performance optimizations
     */
    private void initializeOptimizations() {
        try {
            // Initialize performance monitoring
            PerformanceUtils.initializeOptimizations(this);

            // Setup uncaught exception handler for crash prevention
            setupExceptionHandler();

            // Monitor memory usage
            startMemoryMonitoring();

            Log.d(TAG, "Performance optimizations initialized");
        } catch (Exception e) {
            Log.w(TAG, "Error initializing optimizations", e);
        }
    }

    /**
     * Setup global exception handler for crash prevention
     */
    private void setupExceptionHandler() {
        try {
            Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

            Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                try {
                    Log.e(TAG, "Uncaught exception in thread " + thread.getName(), exception);

                    // Log system state before crash
                    logCrashInfo();

                    // Perform cleanup
                    performEmergencyCleanup();

                } catch (Exception e) {
                    Log.e(TAG, "Error in exception handler", e);
                } finally {
                    // Call original handler
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, exception);
                    }
                }
            });

            Log.d(TAG, "Exception handler setup complete");
        } catch (Exception e) {
            Log.w(TAG, "Error setting up exception handler", e);
        }
    }

    /**
     * Start memory monitoring
     */
    private void startMemoryMonitoring() {
        try {
            // Monitor memory usage periodically
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (instance != null) {
                            PerformanceUtils.PerformanceMonitor.logPerformanceMetrics(instance);

                            // Schedule next check
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 30000); // 30 seconds
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error in memory monitoring", e);
                    }
                }
            }, 30000); // Start after 30 seconds

            Log.d(TAG, "Memory monitoring started");
        } catch (Exception e) {
            Log.w(TAG, "Error starting memory monitoring", e);
        }
    }

    /**
     * Log crash information
     */
    private void logCrashInfo() {
        try {
            Log.e(TAG, "=== CRASH INFORMATION ===");
            Log.e(TAG, "Time: " + new java.util.Date());
            Log.e(TAG, "Used Memory: " + PerformanceUtils.formatBytes(PerformanceUtils.MemoryManager.getUsedMemory()));
            Log.e(TAG, "Available Memory: " + PerformanceUtils.formatBytes(PerformanceUtils.MemoryManager.getAvailableMemory(this)));
            Log.e(TAG, "Cache Size: " + PerformanceUtils.formatBytes(PerformanceUtils.StorageManager.getCacheSize(this)));
            Log.e(TAG, "Connection: " + PerformanceUtils.NetworkOptimizer.getConnectionType(this));
            Log.e(TAG, "========================");
        } catch (Exception e) {
            Log.e(TAG, "Error logging crash info", e);
        }
    }

    /**
     * Perform emergency cleanup before crash
     */
    private void performEmergencyCleanup() {
        try {
            // Clear caches
            PerformanceUtils.StorageManager.clearCache(this);

            // Perform memory cleanup
            PerformanceUtils.MemoryManager.performCleanup();

            Log.d(TAG, "Emergency cleanup completed");
        } catch (Exception e) {
            Log.w(TAG, "Error in emergency cleanup", e);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        try {
            Log.w(TAG, "Low memory warning received");

            // Perform aggressive cleanup
            PerformanceUtils.MemoryManager.performCleanup();
            PerformanceUtils.StorageManager.clearCache(this);

            Log.d(TAG, "Low memory cleanup completed");
        } catch (Exception e) {
            Log.w(TAG, "Error handling low memory", e);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        try {
            Log.d(TAG, "Memory trim requested - level: " + level);

            switch (level) {
                case TRIM_MEMORY_RUNNING_MODERATE:
                case TRIM_MEMORY_RUNNING_LOW:
                case TRIM_MEMORY_RUNNING_CRITICAL:
                    // App is running but memory is getting low
                    PerformanceUtils.MemoryManager.performCleanup();
                    break;

                case TRIM_MEMORY_UI_HIDDEN:
                    // App UI is hidden, safe to clear caches
                    PerformanceUtils.StorageManager.clearCache(this);
                    PerformanceUtils.MemoryManager.performCleanup();
                    break;

                case TRIM_MEMORY_BACKGROUND:
                case TRIM_MEMORY_MODERATE:
                case TRIM_MEMORY_COMPLETE:
                    // App is in background, perform aggressive cleanup
                    PerformanceUtils.StorageManager.clearCache(this);
                    PerformanceUtils.MemoryManager.performCleanup();
                    break;
            }

            Log.d(TAG, "Memory trim completed for level: " + level);
        } catch (Exception e) {
            Log.w(TAG, "Error handling memory trim", e);
        }
    }

    @Override
    public void onTerminate() {
        try {
            Log.i(TAG, "Application terminating");

            // Perform final cleanup
            performEmergencyCleanup();

            super.onTerminate();
        } catch (Exception e) {
            Log.w(TAG, "Error in onTerminate", e);
        }
    }

    /**
     * Check if app is in foreground
     */
    public boolean isAppInForeground() {
        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
                if (processes != null) {
                    for (android.app.ActivityManager.RunningAppProcessInfo process : processes) {
                        if (process.processName.equals(getPackageName())) {
                            return process.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking if app is in foreground", e);
        }
        return false;
    }

    /**
     * Get app performance info
     */
    public String getPerformanceInfo() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("Performance Information:\n");
            info.append("Used Memory: ").append(PerformanceUtils.formatBytes(PerformanceUtils.MemoryManager.getUsedMemory())).append("\n");
            info.append("Available Memory: ").append(PerformanceUtils.formatBytes(PerformanceUtils.MemoryManager.getAvailableMemory(this))).append("\n");
            info.append("Cache Size: ").append(PerformanceUtils.formatBytes(PerformanceUtils.StorageManager.getCacheSize(this))).append("\n");
            info.append("Connection: ").append(PerformanceUtils.NetworkOptimizer.getConnectionType(this)).append("\n");
            info.append("Foreground: ").append(isAppInForeground()).append("\n");
            return info.toString();
        } catch (Exception e) {
            Log.w(TAG, "Error getting performance info", e);
            return "Performance info unavailable";
        }
    }
}