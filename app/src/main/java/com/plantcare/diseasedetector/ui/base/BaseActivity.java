package com.plantcare.diseasedetector.ui.base;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;

/**
 * Base Activity class that provides common functionality and fixes
 * for all activities in the PlantDiseaseDetector app
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    // Common UI state
    private boolean isDestroyed = false;
    private boolean isPaused = false;

    // Touch handling optimization
    private long lastTouchTime = 0;
    private static final long TOUCH_THROTTLE_MS = 50; // Prevent rapid touches

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: " + getClass().getSimpleName());

        // Initialize common setup
        setupCommonConfiguration();
    }

    /**
     * Setup common configuration for all activities
     */
    private void setupCommonConfiguration() {
        try {
            // Prevent multiple rapid touches
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } catch (Exception e) {
            Log.w(TAG, "Error setting up common configuration", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        Log.d(TAG, "onResume: " + getClass().getSimpleName());
    }

    @Override
    protected void onPause() {
        try {
            isPaused = true;
            hideKeyboardSafely();
            super.onPause();
            Log.d(TAG, "onPause: " + getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            isDestroyed = true;

            // Cleanup any resources
            cleanupResources();

            super.onDestroy();
            Log.d(TAG, "onDestroy: " + getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        try {
            super.onWindowFocusChanged(hasFocus);
            Log.d(TAG, "onWindowFocusChanged: " + hasFocus + " - " + getClass().getSimpleName());

            if (hasFocus && !isPaused) {
                // Handle focus gained - can be overridden by subclasses
                onWindowFocusGained();
            }
        } catch (Exception e) {
            Log.w(TAG, "Window focus change error", e);
        }
    }

    /**
     * Optimized touch event handling to prevent excessive touch events
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            // Throttle touch events to improve performance
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTouchTime < TOUCH_THROTTLE_MS) {
                return true; // Consume but don't process rapid touches
            }
            lastTouchTime = currentTime;

            return super.onTouchEvent(event);
        } catch (Exception e) {
            Log.w(TAG, "Touch event handling error", e);
            return false;
        }
    }

    /**
     * Safe method to hide keyboard
     */
    protected void hideKeyboardSafely() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error hiding keyboard", e);
        }
    }

    /**
     * Safe method to show keyboard
     */
    protected void showKeyboardSafely(View view) {
        try {
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    view.requestFocus();
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error showing keyboard", e);
        }
    }

    /**
     * Safe toast display
     */
    protected void showToastSafely(String message) {
        try {
            if (!isDestroyed && !isFinishing()) {
                runOnUiThread(() -> {
                    try {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.w(TAG, "Error showing toast", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in showToastSafely", e);
        }
    }

    /**
     * Safe UI update method
     */
    protected void runOnUiThreadSafely(Runnable action) {
        try {
            if (!isDestroyed && !isFinishing()) {
                runOnUiThread(() -> {
                    try {
                        if (!isDestroyed && !isFinishing()) {
                            action.run();
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error in UI thread action", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in runOnUiThreadSafely", e);
        }
    }

    /**
     * Safe executor shutdown
     */
    protected void shutdownExecutorSafely(ExecutorService executor) {
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                Log.d(TAG, "Executor shutdown successfully");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error shutting down executor", e);
        }
    }

    /**
     * Check if activity is in safe state for operations
     */
    protected boolean isActivitySafe() {
        return !isDestroyed && !isFinishing() && !isPaused;
    }

    /**
     * Get safe activity reference - returns null if activity is not safe
     */
    protected BaseActivity getSafeActivity() {
        return isActivitySafe() ? this : null;
    }

    // Abstract and virtual methods for subclasses

    /**
     * Called when window gains focus - override for custom behavior
     */
    protected void onWindowFocusGained() {
        // Default implementation - can be overridden
    }

    /**
     * Cleanup resources - override to cleanup activity-specific resources
     */
    protected void cleanupResources() {
        // Default implementation - can be overridden
        Log.d(TAG, "Cleaning up base resources");
    }

    /**
     * Handle back press with safety checks
     */
    @Override
    public void onBackPressed() {
        try {
            hideKeyboardSafely();
            super.onBackPressed();
        } catch (Exception e) {
            Log.w(TAG, "Error in onBackPressed", e);
            // Fallback - finish activity
            finish();
        }
    }

    /**
     * Safe method to start activities
     */
    protected void startActivitySafely(android.content.Intent intent) {
        try {
            if (isActivitySafe()) {
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error starting activity", e);
            showToastSafely("Unable to open screen");
        }
    }

    /**
     * Safe method to finish activity
     */
    protected void finishSafely() {
        try {
            hideKeyboardSafely();
            if (!isFinishing()) {
                finish();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error finishing activity", e);
        }
    }

    // Utility methods for common operations

    /**
     * Log activity lifecycle for debugging
     */
    protected void logLifecycle(String method) {
        Log.d(TAG, method + ": " + getClass().getSimpleName() +
                " - isDestroyed: " + isDestroyed +
                ", isFinishing: " + isFinishing() +
                ", isPaused: " + isPaused);
    }

    /**
     * Handle common errors gracefully
     */
    protected void handleError(String operation, Exception e) {
        Log.e(TAG, "Error in " + operation + " - " + getClass().getSimpleName(), e);

        // Don't show error toasts for destroyed activities
        if (isActivitySafe()) {
            showToastSafely("An error occurred. Please try again.");
        }
    }

    /**
     * Memory cleanup helper
     */
    protected void performMemoryCleanup() {
        try {
            // Suggest garbage collection
            System.gc();
            Log.d(TAG, "Memory cleanup performed");
        } catch (Exception e) {
            Log.w(TAG, "Error in memory cleanup", e);
        }
    }
}