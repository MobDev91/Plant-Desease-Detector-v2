package com.plantcare.diseasedetector.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for image processing and management
 * Handles image loading, resizing, rotation, and file operations
 */
public class ImageUtils {

    private static final String TAG = "ImageUtils";
    private static final String IMAGE_DIRECTORY = "PlantScans";
    private static final int MAX_IMAGE_SIZE = 1024; // Maximum width/height for processed images
    private static final int JPEG_QUALITY = 85; // JPEG compression quality

    /**
     * Create a new image file in the app's external directory
     */
    public static File createImageFile(Context context) {
        try {
            // Create image file name with timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "PLANT_" + timeStamp + "_";

            // Get app's external storage directory
            File storageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY);

            // Create directory if it doesn't exist
            if (!storageDir.exists() && !storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create image directory");
                return null;
            }

            // Create the image file
            File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);

            Log.d(TAG, "Created image file: " + imageFile.getAbsolutePath());
            return imageFile;

        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    /**
     * Get optimized bitmap for display purposes
     */
    public static Bitmap getDisplayBitmap(String imagePath) {
        return getDisplayBitmap(imagePath, MAX_IMAGE_SIZE);
    }

    /**
     * Get optimized bitmap with specific max size
     */
    public static Bitmap getDisplayBitmap(String imagePath, int maxSize) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w(TAG, "Image path is null or empty");
            return null;
        }

        try {
            // First, get image dimensions without loading the full image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // Calculate sample size to reduce memory usage
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            options.inJustDecodeBounds = false;

            // Load the bitmap with reduced size
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: " + imagePath);
                return null;
            }

            // Apply rotation correction if needed
            bitmap = rotateImageIfRequired(bitmap, imagePath);

            // Further resize if still too large
            bitmap = resizeBitmapIfNeeded(bitmap, maxSize);

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error loading display bitmap from: " + imagePath, e);
            return null;
        }
    }

    /**
     * Get bitmap from URI (for gallery images)
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for URI: " + uri);
                return null;
            }

            // Get image dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
            options.inJustDecodeBounds = false;

            // Load the bitmap
            inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap != null) {
                bitmap = resizeBitmapIfNeeded(bitmap, MAX_IMAGE_SIZE);
            }

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error getting bitmap from URI: " + uri, e);
            return null;
        }
    }

    /**
     * Calculate optimal sample size for image loading
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Resize bitmap if it exceeds maximum size
     */
    public static Bitmap resizeBitmapIfNeeded(Bitmap bitmap, int maxSize) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Check if resizing is needed
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        // Calculate new dimensions maintaining aspect ratio
        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        // Create resized bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        // Recycle original if it's different from resized
        if (resizedBitmap != bitmap) {
            bitmap.recycle();
        }

        Log.d(TAG, String.format("Resized bitmap from %dx%d to %dx%d", width, height, newWidth, newHeight));
        return resizedBitmap;
    }

    /**
     * Rotate image based on EXIF data to correct orientation
     */
    public static Bitmap rotateImageIfRequired(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotation = getRotationFromOrientation(orientation);

            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);

                Bitmap rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true
                );

                if (rotatedBitmap != bitmap) {
                    bitmap.recycle();
                }

                Log.d(TAG, "Rotated image by " + rotation + " degrees");
                return rotatedBitmap;
            }

        } catch (IOException e) {
            Log.w(TAG, "Could not read EXIF data from: " + imagePath, e);
        }

        return bitmap;
    }

    /**
     * Get rotation angle from EXIF orientation
     */
    private static int getRotationFromOrientation(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * Compress and save bitmap to file
     */
    public static boolean saveBitmapToFile(Bitmap bitmap, File file) {
        return saveBitmapToFile(bitmap, file, JPEG_QUALITY);
    }

    /**
     * Compress and save bitmap to file with custom quality
     */
    public static boolean saveBitmapToFile(Bitmap bitmap, File file, int quality) {
        if (bitmap == null || file == null) {
            Log.e(TAG, "Bitmap or file is null");
            return false;
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();

            if (success) {
                Log.d(TAG, "Bitmap saved successfully to: " + file.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to compress bitmap");
            }

            return success;

        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file: " + file.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Convert bitmap to byte array
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        return bitmapToByteArray(bitmap, JPEG_QUALITY);
    }

    /**
     * Convert bitmap to byte array with custom quality
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap, int quality) {
        if (bitmap == null) return null;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error converting bitmap to byte array", e);
            return null;
        }
    }

    /**
     * Convert byte array to bitmap
     */
    public static Bitmap byteArrayToBitmap(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) return null;

        try {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting byte array to bitmap", e);
            return null;
        }
    }

    /**
     * Get image file size in MB
     */
    public static float getImageFileSizeMB(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return 0;

        File file = new File(imagePath);
        if (!file.exists()) return 0;

        long sizeBytes = file.length();
        return sizeBytes / (1024.0f * 1024.0f);
    }

    /**
     * Delete image file
     */
    public static boolean deleteImageFile(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.w(TAG, "Image path is null or empty");
            return false;
        }

        File file = new File(imagePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "Deleted image file: " + imagePath);
            } else {
                Log.e(TAG, "Failed to delete image file: " + imagePath);
            }
            return deleted;
        } else {
            Log.w(TAG, "Image file does not exist: " + imagePath);
            return true; // Consider it successful if file doesn't exist
        }
    }

    /**
     * Check if image file exists and is valid
     */
    public static boolean isValidImageFile(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return false;

        File file = new File(imagePath);
        if (!file.exists() || file.length() == 0) return false;

        // Try to decode just the dimensions to verify it's a valid image
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        return options.outWidth > 0 && options.outHeight > 0;
    }

    /**
     * Get image dimensions without loading the full bitmap
     */
    public static int[] getImageDimensions(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return new int[]{0, 0};

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        return new int[]{options.outWidth, options.outHeight};
    }

    /**
     * Clean up old image files (older than specified days)
     */
    public static int cleanupOldImages(Context context, int daysOld) {
        File imageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY);
        if (!imageDir.exists()) return 0;

        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
        int deletedCount = 0;

        File[] files = imageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                        Log.d(TAG, "Deleted old image: " + file.getName());
                    }
                }
            }
        }

        Log.i(TAG, "Cleaned up " + deletedCount + " old image files");
        return deletedCount;
    }

    /**
     * Get total size of all image files in MB
     */
    public static float getTotalImagesSizeMB(Context context) {
        File imageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIRECTORY);
        if (!imageDir.exists()) return 0;

        long totalSize = 0;
        File[] files = imageDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                }
            }
        }

        return totalSize / (1024.0f * 1024.0f);
    }

    /**
     * Create a thumbnail version of the image
     */
    public static Bitmap createThumbnail(String imagePath, int thumbnailSize) {
        if (imagePath == null || thumbnailSize <= 0) return null;

        // Load with reduced size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        // Calculate sample size for thumbnail
        options.inSampleSize = calculateInSampleSize(options, thumbnailSize, thumbnailSize);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        if (bitmap == null) return null;

        // Apply rotation
        bitmap = rotateImageIfRequired(bitmap, imagePath);

        // Create square thumbnail
        return createSquareThumbnail(bitmap, thumbnailSize);
    }

    /**
     * Create square thumbnail from bitmap
     */
    private static Bitmap createSquareThumbnail(Bitmap bitmap, int size) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate crop dimensions for square
        int cropSize = Math.min(width, height);
        int xOffset = (width - cropSize) / 2;
        int yOffset = (height - cropSize) / 2;

        // Create square crop
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, cropSize, cropSize);

        // Scale to desired size
        Bitmap thumbnail = Bitmap.createScaledBitmap(croppedBitmap, size, size, true);

        // Clean up
        if (croppedBitmap != bitmap && croppedBitmap != thumbnail) {
            croppedBitmap.recycle();
        }
        if (bitmap != thumbnail) {
            bitmap.recycle();
        }

        return thumbnail;
    }
}