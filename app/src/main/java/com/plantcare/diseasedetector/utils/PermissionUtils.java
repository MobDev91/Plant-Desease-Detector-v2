package com.plantcare.diseasedetector.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * Utility class for handling app permissions
 */
public class PermissionUtils {

    /**
     * Check if camera permission is granted
     */
    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if storage permission is granted
     * Note: For Android 10+ (API 29+), scoped storage is used by default
     */
    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses scoped storage, no permission needed for app-specific directories
            return true;
        } else {
            // For older Android versions, check write external storage permission
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Check if read external storage permission is granted
     */
    public static boolean hasReadStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses granular media permissions
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 still uses READ_EXTERNAL_STORAGE for accessing other app files
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            // Older Android versions
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Check if location permission is granted (if needed for future features)
     */
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get required permissions array based on Android version
     */
    public static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            // Android 9 and below
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Check if all required permissions are granted
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        String[] permissions = getRequiredPermissions();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get permission explanation message
     */
    public static String getPermissionExplanation(String permission) {
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "Camera permission is required to take photos of plants for disease detection.";
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "Storage permission is required to save plant scan results and images.";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Storage permission is required to access images from your gallery.";
            case Manifest.permission.READ_MEDIA_IMAGES:
                return "Media permission is required to access images for plant disease scanning.";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Location permission helps track where plant diseases are detected.";
            default:
                return "This permission is required for the app to function properly.";
        }
    }

    /**
     * Check if permission is permanently denied
     */
    public static boolean isPermissionPermanentlyDenied(Context context, String permission) {
        return !(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
    }
}