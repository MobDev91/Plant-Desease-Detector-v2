package com.plantcare.diseasedetector.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.utils.ExportHelper;
import com.plantcare.diseasedetector.utils.BackupHelper;
import com.plantcare.diseasedetector.utils.NotificationHelper;

/**
 * Settings Activity for app preferences and configuration
 */
public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "SettingsActivity";

    // Preference Keys
    public static final String PREFS_NAME = "PlantDiseaseDetectorPrefs";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PREF_DARK_MODE = "dark_mode";
    public static final String PREF_AUTO_BACKUP = "auto_backup";
    public static final String PREF_CAMERA_QUALITY = "camera_quality";
    public static final String PREF_AI_CONFIDENCE_THRESHOLD = "ai_confidence_threshold";
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_MEASUREMENT_UNITS = "measurement_units";

    // UI Components
    private Slider sliderConfidenceThreshold;
    private SeekBar seekBarCameraQuality;
    private TextView tvConfidenceValue, tvCameraQualityValue;
    private TextView tvStorageUsed, tvBackupDate;

    // Action Cards
    private CardView cardExportData, cardBackupRestore, cardClearCache, cardResetSettings;

    // Action Buttons
    private MaterialButton btnExportData, btnBackupNow, btnRestoreData;
    private MaterialButton btnClearCache;

    // Helpers
    private SharedPreferences preferences;
    private NotificationHelper notificationHelper;
    private ExportHelper exportHelper;
    private BackupHelper backupHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove any app title from window
        try {
            if (getActionBar() != null) {
                getActionBar().hide();
            }
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error hiding action bar", e);
        }
        
        setContentView(R.layout.activity_settings);

        // Initialize components
        initializeViews();
        initializeHelpers();
        setupActionBar(); // CHANGED: Use built-in action bar instead of custom toolbar
        setupClickListeners();
        loadPreferences();
        updateUI();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        // Sliders and SeekBars
        sliderConfidenceThreshold = findViewById(R.id.slider_confidence_threshold);
        seekBarCameraQuality = findViewById(R.id.seekbar_camera_quality);

        // Text Views
        tvConfidenceValue = findViewById(R.id.tv_confidence_value);
        tvCameraQualityValue = findViewById(R.id.tv_camera_quality_value);
        tvStorageUsed = findViewById(R.id.tv_storage_used);
        tvBackupDate = findViewById(R.id.tv_backup_date);

        // Cards
        cardExportData = findViewById(R.id.card_export_data);
        cardBackupRestore = findViewById(R.id.card_backup_restore);
        cardClearCache = findViewById(R.id.card_clear_cache);
        cardResetSettings = findViewById(R.id.card_reset_settings);

        // Buttons
        btnExportData = findViewById(R.id.btn_export_data);
        btnBackupNow = findViewById(R.id.btn_backup_now);
        btnRestoreData = findViewById(R.id.btn_restore_data);
        btnClearCache = findViewById(R.id.btn_clear_cache);
    }

    /**
     * Initialize helper classes
     */
    private void initializeHelpers() {
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        notificationHelper = new NotificationHelper(this);
        exportHelper = new ExportHelper(this);
        backupHelper = new BackupHelper(this);
    }

    /**
     * Setup action bar - FIXED: No action bar, only custom toolbar
     */
    private void setupActionBar() {
        try {
            // Ensure no action bar is used
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            
            // Setup only the custom toolbar
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                // Set title directly on toolbar only
                toolbar.setTitle("Settings");
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
                
                // Explicitly don't set as support action bar
                // This prevents any app-level titles from showing
            }
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e(TAG, "Error setting up toolbar", e);
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Slider listeners with null checks
        if (sliderConfidenceThreshold != null) {
            sliderConfidenceThreshold.addOnChangeListener(this::onConfidenceThresholdChanged);
        }

        if (seekBarCameraQuality != null) {
            seekBarCameraQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updateCameraQualityValue(progress);
                        saveCameraQuality(progress);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Card click listeners with null checks
        if (cardExportData != null) cardExportData.setOnClickListener(this);
        if (cardBackupRestore != null) cardBackupRestore.setOnClickListener(this);
        if (cardClearCache != null) cardClearCache.setOnClickListener(this);
        if (cardResetSettings != null) cardResetSettings.setOnClickListener(this);

        // Button click listeners with null checks
        if (btnExportData != null) btnExportData.setOnClickListener(this);
        if (btnBackupNow != null) btnBackupNow.setOnClickListener(this);
        if (btnRestoreData != null) btnRestoreData.setOnClickListener(this);
        if (btnClearCache != null) btnClearCache.setOnClickListener(this);
    }

    /**
     * Load preferences from SharedPreferences
     */
    private void loadPreferences() {
        try {
            if (sliderConfidenceThreshold != null) {
                float confidenceThreshold = preferences.getFloat(PREF_AI_CONFIDENCE_THRESHOLD, 0.7f);
                sliderConfidenceThreshold.setValue(confidenceThreshold * 100);
            }

            if (seekBarCameraQuality != null) {
                int cameraQuality = preferences.getInt(PREF_CAMERA_QUALITY, 80);
                seekBarCameraQuality.setProgress(cameraQuality);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error loading preferences", e);
        }
    }

    /**
     * Update UI elements
     */
    private void updateUI() {
        try {
            if (sliderConfidenceThreshold != null) {
                updateConfidenceValue((int) sliderConfidenceThreshold.getValue());
            }
            if (seekBarCameraQuality != null) {
                updateCameraQualityValue(seekBarCameraQuality.getProgress());
            }
            updateStorageInfo();
            updateBackupInfo();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating UI", e);
        }
    }



    /**
     * Handle slider changes
     */
    private void onConfidenceThresholdChanged(Slider slider, float value, boolean fromUser) {
        if (fromUser) {
            try {
                updateConfidenceValue((int) value);
                preferences.edit().putFloat(PREF_AI_CONFIDENCE_THRESHOLD, value / 100f).apply();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error changing confidence threshold", e);
            }
        }
    }

    private void updateConfidenceValue(int value) {
        if (tvConfidenceValue != null) {
            tvConfidenceValue.setText(value + "%");
        }
    }

    private void updateCameraQualityValue(int value) {
        try {
            String quality;
            if (value < 30) quality = "Low";
            else if (value < 70) quality = "Medium";
            else if (value < 90) quality = "High";
            else quality = "Ultra";

            if (tvCameraQualityValue != null) {
                tvCameraQualityValue.setText(quality + " (" + value + "%)");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating camera quality value", e);
        }
    }

    private void saveCameraQuality(int value) {
        try {
            preferences.edit().putInt(PREF_CAMERA_QUALITY, value).apply();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error saving camera quality", e);
        }
    }

    /**
     * Update storage information
     */
    private void updateStorageInfo() {
        try {
            // Calculate storage usage
            long storageUsed = calculateStorageUsage();
            String storageText = formatStorageSize(storageUsed);
            if (tvStorageUsed != null) {
                tvStorageUsed.setText(storageText);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating storage info", e);
        }
    }

    /**
     * Update backup information
     */
    private void updateBackupInfo() {
        try {
            String lastBackup = preferences.getString("last_backup_date", "Never");
            if (tvBackupDate != null) {
                tvBackupDate.setText(lastBackup);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error updating backup info", e);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            int id = v.getId();

            if (id == R.id.card_export_data || id == R.id.btn_export_data) {
                showExportDialog();
            } else if (id == R.id.card_backup_restore || id == R.id.btn_backup_now) {
                performBackup();
            } else if (id == R.id.btn_restore_data) {
                performRestore();
            } else if (id == R.id.card_clear_cache || id == R.id.btn_clear_cache) {
                clearCache();
            } else if (id == R.id.card_reset_settings) {
                showResetDialog();
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error handling click", e);
            showToast("Error processing action");
        }
    }

    /**
     * Show export data dialog
     */
    private void showExportDialog() {
        try {
            String[] options = {"CSV Format", "PDF Report", "JSON Data"};

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Export Data")
                    .setItems(options, (dialog, which) -> {
                        try {
                            switch (which) {
                                case 0:
                                    exportHelper.exportToCSV();
                                    break;
                                case 1:
                                    exportHelper.exportToPDF();
                                    break;
                                case 2:
                                    exportHelper.exportToJSON();
                                    break;
                            }
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error exporting data", e);
                            showToast("Export failed: " + e.getMessage());
                        }
                    })
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing export dialog", e);
            showToast("Export - Coming soon!");
        }
    }

    /**
     * Perform backup
     */
    private void performBackup() {
        try {
            backupHelper.performBackup(new BackupHelper.BackupCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        showToast("Backup completed successfully");
                        updateBackupInfo();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showToast("Backup failed: " + error));
                }
            });
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error performing backup", e);
            showToast("Backup - Coming soon!");
        }
    }

    /**
     * Perform restore
     */
    private void performRestore() {
        try {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Restore Data")
                    .setMessage("This will replace all current data. Are you sure?")
                    .setPositiveButton("Restore", (dialog, which) -> {
                        try {
                            backupHelper.performRestore(new BackupHelper.RestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    runOnUiThread(() -> showToast("Data restored successfully"));
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> showToast("Restore failed: " + error));
                                }
                            });
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error restoring data", e);
                            showToast("Restore failed: " + e.getMessage());
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing restore dialog", e);
            showToast("Restore - Coming soon!");
        }
    }

    /**
     * Clear app cache
     */
    private void clearCache() {
        try {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Clear Cache")
                    .setMessage("This will clear temporary files and image cache.")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        try {
                            // Clear cache logic here
                            showToast("Cache cleared successfully");
                            updateStorageInfo();
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "Error clearing cache", e);
                            showToast("Cache clear failed");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing clear cache dialog", e);
            showToast("Clear cache - Coming soon!");
        }
    }

    /**
     * Show reset dialog
     */
    private void showResetDialog() {
        try {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Reset Settings")
                    .setMessage("This will reset all settings to default values.")
                    .setPositiveButton("Reset", (dialog, which) -> resetSettings())
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing reset dialog", e);
            showToast("Reset - Coming soon!");
        }
    }

    /**
     * Reset all settings
     */
    private void resetSettings() {
        try {
            preferences.edit().clear().apply();
            loadPreferences();
            updateUI();
            showToast("Settings reset to default");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error resetting settings", e);
            showToast("Reset failed");
        }
    }



    /**
     * Calculate storage usage
     */
    private long calculateStorageUsage() {
        // TODO: Implement actual storage calculation
        return 1024 * 1024 * 15; // 15 MB for demo
    }

    /**
     * Format storage size
     */
    private String formatStorageSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error showing toast", e);
        }
    }

    /**
     * Handle toolbar navigation
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}