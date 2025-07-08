package com.plantcare.diseasedetector.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.models.ScanResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Helper class for backup and restore operations
 */
public class BackupHelper {

    private static final String TAG = "BackupHelper";
    private static final String BACKUP_FOLDER = "PlantDiseaseDetector/Backups";
    private static final String BACKUP_FILE_PREFIX = "plant_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".zip";
    private static final String DATA_FILE_NAME = "scan_data.json";
    private static final String PREFS_FILE_NAME = "preferences.json";
    private static final String IMAGES_FOLDER = "images";

    private Context context;
    private AppDatabase database;
    private ExecutorService executor;
    private NotificationHelper notificationHelper;

    public BackupHelper(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.notificationHelper = new NotificationHelper(context);
    }

    /**
     * Perform full backup of app data
     */
    public void performBackup(BackupCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting backup process");

                // Create backup directory
                File backupDir = createBackupDirectory();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File backupFile = new File(backupDir, BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION);

                // Create temporary directory for backup files
                File tempDir = new File(context.getCacheDir(), "backup_temp");
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }

                // Export scan data
                File dataFile = exportScanData(tempDir);

                // Export preferences
                File prefsFile = exportPreferences(tempDir);

                // Create images directory and copy scan images
                File imagesDir = new File(tempDir, IMAGES_FOLDER);
                imagesDir.mkdirs();
                copyImages(imagesDir);

                // Create ZIP backup file
                createZipBackup(backupFile, tempDir);

                // Clean up temporary files
                deleteDirectory(tempDir);

                // Update backup preferences
                updateBackupPreferences(backupFile.getAbsolutePath());

                Log.d(TAG, "Backup completed: " + backupFile.getAbsolutePath());
                notificationHelper.sendBackupCompleteNotification(true, "Backup completed successfully");

                if (callback != null) {
                    callback.onSuccess();
                }

            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                notificationHelper.sendBackupCompleteNotification(false, "Backup failed: " + e.getMessage());

                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Restore data from backup file
     */
    public void performRestore(RestoreCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting restore process");

                // Find the most recent backup file
                File backupFile = findLatestBackupFile();
                if (backupFile == null) {
                    throw new Exception("No backup file found");
                }

                // Create temporary directory for extracted files
                File tempDir = new File(context.getCacheDir(), "restore_temp");
                if (tempDir.exists()) {
                    deleteDirectory(tempDir);
                }
                tempDir.mkdirs();

                // Extract ZIP backup
                extractZipBackup(backupFile, tempDir);

                // Restore scan data
                File dataFile = new File(tempDir, DATA_FILE_NAME);
                if (dataFile.exists()) {
                    restoreScanData(dataFile);
                }

                // Restore preferences
                File prefsFile = new File(tempDir, PREFS_FILE_NAME);
                if (prefsFile.exists()) {
                    restorePreferences(prefsFile);
                }

                // Restore images
                File imagesDir = new File(tempDir, IMAGES_FOLDER);
                if (imagesDir.exists()) {
                    restoreImages(imagesDir);
                }

                // Clean up temporary files
                deleteDirectory(tempDir);

                Log.d(TAG, "Restore completed");

                if (callback != null) {
                    callback.onSuccess();
                }

            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);

                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    /**
     * Export scan data to JSON file
     */
    private File exportScanData(File tempDir) throws Exception {
        List<ScanResult> scanResults = database.scanResultDao().getAllScanResults();

        JSONObject exportData = new JSONObject();
        exportData.put("export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        exportData.put("app_version", "1.0.0");
        exportData.put("total_scans", scanResults.size());

        JSONArray scansArray = new JSONArray();
        for (ScanResult result : scanResults) {
            JSONObject scanObject = new JSONObject();
            scanObject.put("id", result.getId());
            scanObject.put("scan_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.getScanDate()));
            scanObject.put("image_path", result.getImagePath());
            scanObject.put("predicted_class", result.getPredictedClass());
            scanObject.put("predicted_index", result.getPredictedIndex());
            scanObject.put("confidence", result.getConfidence());
            scanObject.put("plant_name", result.getPlantName());
            scanObject.put("disease_name", result.getDiseaseName());
            scanObject.put("is_healthy", result.isHealthy());
            scanObject.put("location", result.getLocation());
            scanObject.put("notes", result.getNotes());
            scanObject.put("treatment_applied", result.getTreatmentApplied());

            if (result.getFollowUpDate() != null) {
                scanObject.put("follow_up_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(result.getFollowUpDate()));
            }

            scansArray.put(scanObject);
        }
        exportData.put("scans", scansArray);

        File dataFile = new File(tempDir, DATA_FILE_NAME);
        FileWriter writer = new FileWriter(dataFile);
        writer.write(exportData.toString(2));
        writer.close();

        return dataFile;
    }

    /**
     * Export app preferences to JSON file
     */
    private File exportPreferences(File tempDir) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("PlantDiseaseDetectorPrefs", Context.MODE_PRIVATE);

        JSONObject prefsObject = new JSONObject();
        for (String key : prefs.getAll().keySet()) {
            Object value = prefs.getAll().get(key);
            prefsObject.put(key, value);
        }

        File prefsFile = new File(tempDir, PREFS_FILE_NAME);
        FileWriter writer = new FileWriter(prefsFile);
        writer.write(prefsObject.toString(2));
        writer.close();

        return prefsFile;
    }

    /**
     * Copy scan images to backup directory
     */
    private void copyImages(File imagesDir) throws IOException {
        List<ScanResult> scanResults = database.scanResultDao().getAllScanResults();

        for (ScanResult result : scanResults) {
            if (result.getImagePath() != null && !result.getImagePath().isEmpty()) {
                File originalImage = new File(result.getImagePath());
                if (originalImage.exists()) {
                    File backupImage = new File(imagesDir, "scan_" + result.getId() + "_" + originalImage.getName());
                    copyFile(originalImage, backupImage);
                }
            }
        }
    }

    /**
     * Create ZIP backup file
     */
    private void createZipBackup(File backupFile, File sourceDir) throws IOException {
        FileOutputStream fos = new FileOutputStream(backupFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        zipDirectory(sourceDir, sourceDir.getName(), zos);

        zos.close();
        fos.close();
    }

    /**
     * Recursively zip directory contents
     */
    private void zipDirectory(File dir, String baseName, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, baseName + "/" + file.getName(), zos);
            } else {
                FileInputStream fis = new FileInputStream(file);
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }

                zos.closeEntry();
                fis.close();
            }
        }
    }

    /**
     * Extract ZIP backup file
     */
    private void extractZipBackup(File backupFile, File destDir) throws IOException {
        FileInputStream fis = new FileInputStream(backupFile);
        ZipInputStream zis = new ZipInputStream(fis);

        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            File destFile = new File(destDir, entry.getName());

            if (entry.isDirectory()) {
                destFile.mkdirs();
            } else {
                // Create parent directories
                destFile.getParentFile().mkdirs();

                FileOutputStream fos = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                fos.close();
            }
            zis.closeEntry();
        }

        zis.close();
        fis.close();
    }

    /**
     * Restore scan data from JSON file
     */
    private void restoreScanData(File dataFile) throws Exception {
        // Clear existing data
        database.scanResultDao().deleteAllScanResults();

        // Read and parse JSON data
        FileReader reader = new FileReader(dataFile);
        StringBuilder jsonBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        int length;
        while ((length = reader.read(buffer)) > 0) {
            jsonBuilder.append(buffer, 0, length);
        }
        reader.close();

        JSONObject exportData = new JSONObject(jsonBuilder.toString());
        JSONArray scansArray = exportData.getJSONArray("scans");

        // Restore scan results
        for (int i = 0; i < scansArray.length(); i++) {
            JSONObject scanObject = scansArray.getJSONObject(i);

            ScanResult result = new ScanResult();
            result.setImagePath(scanObject.optString("image_path"));
            result.setPredictedClass(scanObject.optString("predicted_class"));
            result.setPredictedIndex(scanObject.optInt("predicted_index"));
            result.setConfidence((float) scanObject.optDouble("confidence"));
            result.setPlantName(scanObject.optString("plant_name"));
            result.setDiseaseName(scanObject.optString("disease_name"));
            result.setHealthy(scanObject.optBoolean("is_healthy"));
            result.setLocation(scanObject.optString("location"));
            result.setNotes(scanObject.optString("notes"));
            result.setTreatmentApplied(scanObject.optString("treatment_applied"));

            // Parse dates
            try {
                String scanDateStr = scanObject.optString("scan_date");
                if (!scanDateStr.isEmpty()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    result.setScanDate(dateFormat.parse(scanDateStr));
                }

                String followUpDateStr = scanObject.optString("follow_up_date");
                if (!followUpDateStr.isEmpty()) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    result.setFollowUpDate(dateFormat.parse(followUpDateStr));
                }
            } catch (Exception e) {
                Log.w(TAG, "Error parsing dates for scan result", e);
            }

            database.scanResultDao().insertScanResult(result);
        }
    }

    /**
     * Restore preferences from JSON file - FIXED JSON ITERATION
     */
    private void restorePreferences(File prefsFile) throws Exception {
        FileReader reader = new FileReader(prefsFile);
        StringBuilder jsonBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        int length;
        while ((length = reader.read(buffer)) > 0) {
            jsonBuilder.append(buffer, 0, length);
        }
        reader.close();

        JSONObject prefsObject = new JSONObject(jsonBuilder.toString());
        SharedPreferences.Editor editor = context.getSharedPreferences("PlantDiseaseDetectorPrefs", Context.MODE_PRIVATE).edit();

        // FIXED: Use Iterator instead of for-each loop
        Iterator<String> keys = prefsObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = prefsObject.get(key);
            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float || value instanceof Double) {
                editor.putFloat(key, ((Number) value).floatValue());
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
        }

        editor.apply();
    }

    /**
     * Restore images from backup
     */
    private void restoreImages(File imagesDir) throws IOException {
        File appImagesDir = new File(context.getFilesDir(), "images");
        if (!appImagesDir.exists()) {
            appImagesDir.mkdirs();
        }

        File[] imageFiles = imagesDir.listFiles();
        if (imageFiles == null) return;

        for (File imageFile : imageFiles) {
            if (imageFile.isFile()) {
                File destFile = new File(appImagesDir, imageFile.getName());
                copyFile(imageFile, destFile);
            }
        }
    }

    /**
     * Find the latest backup file
     */
    private File findLatestBackupFile() {
        File backupDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER);
        if (!backupDir.exists()) {
            return null;
        }

        File[] backupFiles = backupDir.listFiles((dir, name) ->
                name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }

        // Find the most recent backup
        File latestBackup = backupFiles[0];
        for (File backup : backupFiles) {
            if (backup.lastModified() > latestBackup.lastModified()) {
                latestBackup = backup;
            }
        }

        return latestBackup;
    }

    /**
     * Schedule automatic backup
     */
    public void scheduleAutoBackup() {
        // TODO: Implement work manager for periodic backups
        Log.d(TAG, "Auto backup scheduled");
    }

    /**
     * Cancel automatic backup
     */
    public void cancelAutoBackup() {
        // TODO: Cancel work manager backup task
        Log.d(TAG, "Auto backup cancelled");
    }

    /**
     * Get list of available backup files
     */
    public List<BackupInfo> getAvailableBackups() {
        // TODO: Implement backup file listing
        return null;
    }

    /**
     * Delete old backup files (keep only last N backups)
     */
    public void cleanupOldBackups(int keepCount) {
        executor.execute(() -> {
            try {
                File backupDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER);
                if (!backupDir.exists()) return;

                File[] backupFiles = backupDir.listFiles((dir, name) ->
                        name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));

                if (backupFiles == null || backupFiles.length <= keepCount) return;

                // Sort by modification time (newest first)
                java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                // Delete old backups
                for (int i = keepCount; i < backupFiles.length; i++) {
                    if (backupFiles[i].delete()) {
                        Log.d(TAG, "Deleted old backup: " + backupFiles[i].getName());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up old backups", e);
            }
        });
    }

    /**
     * Utility methods
     */
    private File createBackupDirectory() throws IOException {
        File backupDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            throw new IOException("Failed to create backup directory");
        }
        return backupDir;
    }

    private void copyFile(File source, File dest) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(dest);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }

        fis.close();
        fos.close();
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    private void updateBackupPreferences(String backupPath) {
        SharedPreferences prefs = context.getSharedPreferences("PlantDiseaseDetectorPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("last_backup_date", new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()))
                .putString("last_backup_path", backupPath)
                .apply();
    }

    /**
     * Callback interfaces
     */
    public interface BackupCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface RestoreCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Backup information class
     */
    public static class BackupInfo {
        public String fileName;
        public long fileSize;
        public Date createDate;
        public int scanCount;

        public BackupInfo(String fileName, long fileSize, Date createDate, int scanCount) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.createDate = createDate;
            this.scanCount = scanCount;
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}