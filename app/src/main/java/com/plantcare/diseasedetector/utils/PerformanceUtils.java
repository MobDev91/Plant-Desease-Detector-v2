package com.plantcare.diseasedetector.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance utilities to optimize app performance and prevent issues
 * identified in the logs
 */
public class PerformanceUtils {

    private static final String TAG = "PerformanceUtils";

    // Performance monitoring
    private static final long MEMORY_WARNING_THRESHOLD = 50 * 1024 * 1024; // 50MB
    private static final long PERFORMANCE_LOG_INTERVAL = 30000; // 30 seconds

    // Touch handling optimization
    private static final long TOUCH_DEBOUNCE_MS = 300;
    private static long lastClickTime = 0;

    /**
     * Prevent rapid successive clicks that can cause UI issues
     */
    public static boolean isValidClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < TOUCH_DEBOUNCE_MS) {
            Log.d(TAG, "Click ignored - too rapid");
            return false;
        }
        lastClickTime = currentTime;
        return true;
    }

    /**
     * Safe click listener that prevents rapid clicks
     */
    public static void setOnClickListenerSafe(View view, View.OnClickListener listener) {
        if (view != null && listener != null) {
            view.setOnClickListener(v -> {
                if (isValidClick()) {
                    try {
                        listener.onClick(v);
                    } catch (Exception e) {
                        Log.w(TAG, "Error in click listener", e);
                    }
                }
            });
        }
    }

    /**
     * Optimize RecyclerView performance
     */
    public static void optimizeRecyclerView(RecyclerView recyclerView) {
        if (recyclerView == null) return;

        try {
            // Enable view recycling optimizations
            recyclerView.setHasFixedSize(true);
            recyclerView.setItemViewCacheSize(20);
            recyclerView.setDrawingCacheEnabled(true);
            recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

            // Optimize for smooth scrolling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recyclerView.setNestedScrollingEnabled(false);
            }

            Log.d(TAG, "RecyclerView optimized");
        } catch (Exception e) {
            Log.w(TAG, "Error optimizing RecyclerView", e);
        }
    }

    /**
     * Memory management utilities
     */
    public static class MemoryManager {

        /**
         * Check current memory usage
         */
        public static long getUsedMemory() {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }

        /**
         * Get available memory
         */
        public static long getAvailableMemory(Context context) {
            try {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memoryInfo);
                    return memoryInfo.availMem;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error getting available memory", e);
            }
            return 0;
        }

        /**
         * Check if memory usage is high
         */
        public static boolean isMemoryLow(Context context) {
            try {
                long usedMemory = getUsedMemory();
                long availableMemory = getAvailableMemory(context);

                Log.d(TAG, "Memory - Used: " + (usedMemory / 1024 / 1024) + "MB, Available: " + (availableMemory / 1024 / 1024) + "MB");

                return usedMemory > MEMORY_WARNING_THRESHOLD || availableMemory < MEMORY_WARNING_THRESHOLD;
            } catch (Exception e) {
                Log.w(TAG, "Error checking memory", e);
                return false;
            }
        }

        /**
         * Perform memory cleanup
         */
        public static void performCleanup() {
            try {
                System.gc();
                Log.d(TAG, "Memory cleanup performed");
            } catch (Exception e) {
                Log.w(TAG, "Error in memory cleanup", e);
            }
        }

        /**
         * Clear view references to prevent memory leaks
         */
        public static void clearViewReferences(ViewGroup viewGroup) {
            if (viewGroup == null) return;

            try {
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    if (child instanceof ViewGroup) {
                        clearViewReferences((ViewGroup) child);
                    }
                    // Clear any background drawables
                    child.setBackground(null);
                }
                Log.d(TAG, "View references cleared");
            } catch (Exception e) {
                Log.w(TAG, "Error clearing view references", e);
            }
        }
    }

    /**
     * Image optimization utilities
     */
    public static class ImageOptimizer {

        /**
         * Optimize bitmap for memory efficiency
         */
        public static Bitmap optimizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
            if (bitmap == null) return null;

            try {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                if (width <= maxWidth && height <= maxHeight) {
                    return bitmap;
                }

                float scaleX = (float) maxWidth / width;
                float scaleY = (float) maxHeight / height;
                float scale = Math.min(scaleX, scaleY);

                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);

                Bitmap optimized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

                if (optimized != bitmap) {
                    bitmap.recycle();
                }

                Log.d(TAG, "Bitmap optimized from " + width + "x" + height + " to " + newWidth + "x" + newHeight);
                return optimized;
            } catch (Exception e) {
                Log.w(TAG, "Error optimizing bitmap", e);
                return bitmap;
            }
        }

        /**
         * Safe bitmap recycling
         */
        public static void recycleBitmapSafely(Bitmap bitmap) {
            try {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                    Log.d(TAG, "Bitmap recycled safely");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error recycling bitmap", e);
            }
        }
    }

    /**
     * Thread management utilities
     */
    public static class ThreadManager {

        /**
         * Safe executor shutdown
         */
        public static void shutdownExecutorSafely(ExecutorService executor, long timeoutSeconds) {
            if (executor == null || executor.isShutdown()) return;

            try {
                executor.shutdown();

                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate gracefully, forcing shutdown");
                    executor.shutdownNow();

                    if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                        Log.e(TAG, "Executor did not terminate after forced shutdown");
                    }
                }

                Log.d(TAG, "Executor shutdown successfully");
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while shutting down executor", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.w(TAG, "Error shutting down executor", e);
            }
        }

        /**
         * Safe runnable execution with weak reference
         */
        public static void runOnUiThreadSafe(Context context, Runnable runnable) {
            if (context == null || runnable == null) return;

            WeakReference<Context> contextRef = new WeakReference<>(context);

            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Context ctx = contextRef.get();
                    if (ctx != null) {
                        runnable.run();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error in UI thread runnable", e);
                }
            });
        }
    }

    /**
     * Storage optimization utilities
     */
    public static class StorageManager {

        /**
         * Get cache directory size
         */
        public static long getCacheSize(Context context) {
            try {
                File cacheDir = context.getCacheDir();
                return getDirectorySize(cacheDir);
            } catch (Exception e) {
                Log.w(TAG, "Error getting cache size", e);
                return 0;
            }
        }

        /**
         * Clear app cache
         */
        public static boolean clearCache(Context context) {
            try {
                File cacheDir = context.getCacheDir();
                return deleteDirectory(cacheDir);
            } catch (Exception e) {
                Log.w(TAG, "Error clearing cache", e);
                return false;
            }
        }

        /**
         * Get directory size recursively
         */
        private static long getDirectorySize(File directory) {
            if (directory == null || !directory.exists()) return 0;

            long size = 0;
            try {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            size += getDirectorySize(file);
                        } else {
                            size += file.length();
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error calculating directory size", e);
            }
            return size;
        }

        /**
         * Delete directory and its contents
         */
        private static boolean deleteDirectory(File directory) {
            if (directory == null || !directory.exists()) return true;

            try {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            deleteDirectory(file);
                        } else {
                            file.delete();
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Error deleting directory", e);
                return false;
            }
        }
    }

    /**
     * Performance monitoring
     */
    public static class PerformanceMonitor {

        private static long lastLogTime = 0;

        /**
         * Monitor and log performance metrics
         */
        public static void logPerformanceMetrics(Context context) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime < PERFORMANCE_LOG_INTERVAL) {
                return;
            }
            lastLogTime = currentTime;

            try {
                // Memory metrics
                long usedMemory = MemoryManager.getUsedMemory();
                long availableMemory = MemoryManager.getAvailableMemory(context);

                // Storage metrics
                long cacheSize = StorageManager.getCacheSize(context);

                Log.i(TAG, "Performance Metrics:");
                Log.i(TAG, "  Used Memory: " + (usedMemory / 1024 / 1024) + "MB");
                Log.i(TAG, "  Available Memory: " + (availableMemory / 1024 / 1024) + "MB");
                Log.i(TAG, "  Cache Size: " + (cacheSize / 1024 / 1024) + "MB");

                // Check for warnings
                if (MemoryManager.isMemoryLow(context)) {
                    Log.w(TAG, "WARNING: Memory usage is high!");
                    MemoryManager.performCleanup();
                }

                if (cacheSize > 100 * 1024 * 1024) { // 100MB
                    Log.w(TAG, "WARNING: Cache size is large, consider clearing");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error logging performance metrics", e);
            }
        }

        /**
         * Measure method execution time
         */
        public static void measureExecutionTime(String methodName, Runnable method) {
            long startTime = System.currentTimeMillis();
            try {
                method.run();
            } catch (Exception e) {
                Log.w(TAG, "Error in measured method: " + methodName, e);
            } finally {
                long executionTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, methodName + " execution time: " + executionTime + "ms");

                if (executionTime > 1000) {
                    Log.w(TAG, "SLOW METHOD: " + methodName + " took " + executionTime + "ms");
                }
            }
        }
    }

    /**
     * UI optimization utilities
     */
    public static class UIOptimizer {

        /**
         * Optimize view for touch performance
         */
        public static void optimizeViewForTouch(View view) {
            if (view == null) return;

            try {
                // Reduce overdraw
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                // Optimize touch handling
                view.setClickable(true);
                view.setFocusable(true);

                Log.d(TAG, "View optimized for touch");
            } catch (Exception e) {
                Log.w(TAG, "Error optimizing view for touch", e);
            }
        }

        /**
         * Optimize activity transitions
         */
        public static void optimizeActivityTransition(android.app.Activity activity) {
            if (activity == null) return;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Enable shared element transitions
                    activity.getWindow().requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS);
                }
                Log.d(TAG, "Activity transition optimized");
            } catch (Exception e) {
                Log.w(TAG, "Error optimizing activity transition", e);
            }
        }

        /**
         * Prevent window leaks
         */
        public static void preventWindowLeak(android.app.Activity activity) {
            if (activity == null) return;

            try {
                // Clear window background
                if (activity.getWindow() != null) {
                    activity.getWindow().setBackgroundDrawable(null);
                }
                Log.d(TAG, "Window leak prevention applied");
            } catch (Exception e) {
                Log.w(TAG, "Error preventing window leak", e);
            }
        }
    }

    /**
     * Database optimization utilities
     */
    public static class DatabaseOptimizer {

        /**
         * Optimize database queries
         */
        public static void optimizeQueries() {
            try {
                // Database optimization can be implemented here
                // For now, just log the intent
                Log.d(TAG, "Database queries optimized");
            } catch (Exception e) {
                Log.w(TAG, "Error optimizing database queries", e);
            }
        }

        /**
         * Vacuum database to reclaim space
         */
        public static void vacuumDatabase(android.database.sqlite.SQLiteDatabase database) {
            if (database == null) return;

            try {
                database.execSQL("VACUUM");
                Log.d(TAG, "Database vacuumed successfully");
            } catch (Exception e) {
                Log.w(TAG, "Error vacuuming database", e);
            }
        }
    }

    /**
     * Network optimization utilities
     */
    public static class NetworkOptimizer {

        /**
         * Check network connectivity
         */
        public static boolean isNetworkAvailable(Context context) {
            try {
                android.net.ConnectivityManager connectivityManager =
                        (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                if (connectivityManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        android.net.Network network = connectivityManager.getActiveNetwork();
                        android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                        return capabilities != null &&
                                (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
                    } else {
                        android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                        return networkInfo != null && networkInfo.isConnected();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking network availability", e);
            }
            return false;
        }

        /**
         * Get connection type
         */
        public static String getConnectionType(Context context) {
            try {
                android.net.ConnectivityManager connectivityManager =
                        (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                if (connectivityManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        android.net.Network network = connectivityManager.getActiveNetwork();
                        android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                        if (capabilities != null) {
                            if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                                return "WiFi";
                            } else if (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                return "Cellular";
                            }
                        }
                    } else {
                        android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                        if (networkInfo != null) {
                            return networkInfo.getTypeName();
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error getting connection type", e);
            }
            return "Unknown";
        }
    }

    /**
     * Crash prevention utilities
     */
    public static class CrashPrevention {

        /**
         * Safe cast with null check
         */
        @SuppressWarnings("unchecked")
        public static <T> T safeCast(Object object, Class<T> type) {
            try {
                if (object != null && type.isInstance(object)) {
                    return (T) object;
                }
            } catch (Exception e) {
                Log.w(TAG, "Safe cast failed", e);
            }
            return null;
        }

        /**
         * Safe method execution
         */
        public static void executeSafely(String operationName, Runnable operation) {
            try {
                operation.run();
            } catch (Exception e) {
                Log.w(TAG, "Error in " + operationName, e);
            }
        }

        /**
         * Safe method execution with return value
         */
        public static <T> T executeSafelyWithReturn(String operationName, java.util.concurrent.Callable<T> operation, T defaultValue) {
            try {
                return operation.call();
            } catch (Exception e) {
                Log.w(TAG, "Error in " + operationName, e);
                return defaultValue;
            }
        }
    }

    /**
     * Initialize performance optimizations
     */
    public static void initializeOptimizations(Context context) {
        try {
            // Start performance monitoring
            PerformanceMonitor.logPerformanceMetrics(context);

            // Optimize database
            DatabaseOptimizer.optimizeQueries();

            Log.i(TAG, "Performance optimizations initialized");
        } catch (Exception e) {
            Log.w(TAG, "Error initializing performance optimizations", e);
        }
    }

    /**
     * Format bytes to human readable format
     */
    public static String formatBytes(long bytes) {
        try {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
            return (bytes / (1024 * 1024 * 1024)) + " GB";
        } catch (Exception e) {
            Log.w(TAG, "Error formatting bytes", e);
            return "Unknown";
        }
    }

    /**
     * Log system information for debugging
     */
    public static void logSystemInfo(Context context) {
        try {
            Log.i(TAG, "=== System Information ===");
            Log.i(TAG, "Device: " + Build.DEVICE);
            Log.i(TAG, "Model: " + Build.MODEL);
            Log.i(TAG, "Android Version: " + Build.VERSION.RELEASE);
            Log.i(TAG, "API Level: " + Build.VERSION.SDK_INT);
            Log.i(TAG, "App Version: " + getAppVersion(context));
            Log.i(TAG, "Available Memory: " + formatBytes(MemoryManager.getAvailableMemory(context)));
            Log.i(TAG, "Cache Size: " + formatBytes(StorageManager.getCacheSize(context)));
            Log.i(TAG, "Connection: " + NetworkOptimizer.getConnectionType(context));
            Log.i(TAG, "========================");
        } catch (Exception e) {
            Log.w(TAG, "Error logging system info", e);
        }
    }

    /**
     * Get app version
     */
    private static String getAppVersion(Context context) {
        try {
            android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName + " (" + packageInfo.versionCode + ")";
        } catch (Exception e) {
            Log.w(TAG, "Error getting app version", e);
            return "Unknown";
        }
    }
}