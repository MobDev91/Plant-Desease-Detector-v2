package com.plantcare.diseasedetector;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.ui.base.BaseActivity;
import com.plantcare.diseasedetector.ui.camera.CameraActivity;
import com.plantcare.diseasedetector.ui.history.HistoryActivity;
import com.plantcare.diseasedetector.ui.main.RecentScansAdapter;
import com.plantcare.diseasedetector.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Activity - Entry point of the Plant Disease Detection App
 * Now extends BaseActivity for improved stability and performance
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    // Constants
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;

    // UI Components
    private MaterialButton btnScanPlant;
    private CardView cardHistory, cardPlantGuide, cardStatistics, cardSettings;
    private RecyclerView rvRecentScans;
    private View layoutEmptyState;
    private View tvViewAll;

    // Data
    private RecentScansAdapter recentScansAdapter;
    private List<ScanResult> recentScansList;
    private AppDatabase database;
    private ExecutorService databaseExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize components
        initializeDatabase();
        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        // Load data
        loadRecentScans();

        // Check permissions
        checkPermissions();
    }

    /**
     * Initialize database with proper error handling
     */
    private void initializeDatabase() {
        try {
            database = AppDatabase.getInstance(this);
            databaseExecutor = Executors.newSingleThreadExecutor();
            logLifecycle("Database initialized");
        } catch (Exception e) {
            handleError("database initialization", e);
        }
    }

    /**
     * Initialize all UI components with null checks
     */
    private void initializeViews() {
        try {
            // Main action button
            btnScanPlant = findViewById(R.id.btn_scan_plant);

            // Feature cards
            cardHistory = findViewById(R.id.card_history);
            cardPlantGuide = findViewById(R.id.card_plant_guide);
            cardStatistics = findViewById(R.id.card_statistics);
            cardSettings = findViewById(R.id.card_settings);

            // Recent scans section
            rvRecentScans = findViewById(R.id.rv_recent_scans);
            layoutEmptyState = findViewById(R.id.layout_empty_state);
            tvViewAll = findViewById(R.id.tv_view_all);

            logLifecycle("Views initialized");
        } catch (Exception e) {
            handleError("view initialization", e);
        }
    }

    /**
     * Setup click listeners with null checks
     */
    private void setupClickListeners() {
        try {
            if (btnScanPlant != null) btnScanPlant.setOnClickListener(this);
            if (cardHistory != null) cardHistory.setOnClickListener(this);
            if (cardPlantGuide != null) cardPlantGuide.setOnClickListener(this);
            if (cardStatistics != null) cardStatistics.setOnClickListener(this);
            if (cardSettings != null) cardSettings.setOnClickListener(this);
            if (tvViewAll != null) tvViewAll.setOnClickListener(this);

            logLifecycle("Click listeners setup");
        } catch (Exception e) {
            handleError("click listener setup", e);
        }
    }

    /**
     * Setup RecyclerView with proper error handling
     */
    private void setupRecyclerView() {
        try {
            if (rvRecentScans != null) {
                recentScansList = new ArrayList<>();
                recentScansAdapter = new RecentScansAdapter(this, recentScansList);

                rvRecentScans.setLayoutManager(new LinearLayoutManager(this));
                rvRecentScans.setAdapter(recentScansAdapter);
                rvRecentScans.setNestedScrollingEnabled(false);

                // Set item click listener with safety check
                recentScansAdapter.setOnItemClickListener(scanResult -> {
                    if (isActivitySafe() && scanResult != null) {
                        openScanResultDetail(scanResult);
                    }
                });

                logLifecycle("RecyclerView setup completed");
            }
        } catch (Exception e) {
            handleError("RecyclerView setup", e);
        }
    }

    /**
     * Load recent scans with improved error handling and lifecycle checks
     */
    private void loadRecentScans() {
        if (!isActivitySafe() || database == null || databaseExecutor == null) {
            return;
        }

        try {
            databaseExecutor.execute(() -> {
                try {
                    List<ScanResult> scans = database.scanResultDao().getRecentScans(5);

                    runOnUiThreadSafely(() -> {
                        try {
                            if (recentScansList != null && recentScansAdapter != null) {
                                recentScansList.clear();

                                if (scans != null && !scans.isEmpty()) {
                                    recentScansList.addAll(scans);
                                    recentScansAdapter.notifyDataSetChanged();

                                    // Show RecyclerView, hide empty state
                                    if (rvRecentScans != null) rvRecentScans.setVisibility(View.VISIBLE);
                                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                                } else {
                                    // Show empty state
                                    if (rvRecentScans != null) rvRecentScans.setVisibility(View.GONE);
                                    if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                                }
                            }
                        } catch (Exception e) {
                            handleError("UI update in loadRecentScans", e);
                        }
                    });
                } catch (Exception e) {
                    handleError("database query in loadRecentScans", e);
                }
            });
        } catch (Exception e) {
            handleError("loadRecentScans", e);
        }
    }

    /**
     * Check and request necessary permissions
     */
    private void checkPermissions() {
        try {
            List<String> permissionsNeeded = new ArrayList<>();

            // Check camera permission
            if (!PermissionUtils.hasCameraPermission(this)) {
                permissionsNeeded.add(Manifest.permission.CAMERA);
            }

            // Check storage permission (for Android < 10)
            if (!PermissionUtils.hasStoragePermission(this)) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            // Request permissions if needed
            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this,
                        permissionsNeeded.toArray(new String[0]),
                        CAMERA_PERMISSION_REQUEST_CODE
                );
            }
        } catch (Exception e) {
            handleError("permission check", e);
        }
    }

    @Override
    public void onClick(View v) {
        if (!isActivitySafe() || v == null) {
            return;
        }

        try {
            int id = v.getId();

            if (id == R.id.btn_scan_plant) {
                openCamera();
            } else if (id == R.id.card_history || id == R.id.tv_view_all) {
                openHistory();
            } else if (id == R.id.card_plant_guide) {
                openPlantGuide();
            } else if (id == R.id.card_statistics) {
                openStatistics();
            } else if (id == R.id.card_settings) {
                openSettings();
            }
        } catch (Exception e) {
            handleError("click handling", e);
        }
    }

    /**
     * Open camera for plant scanning with permission check
     */
    private void openCamera() {
        try {
            if (PermissionUtils.hasCameraPermission(this)) {
                Intent intent = new Intent(this, CameraActivity.class);
                startActivitySafely(intent);
            } else {
                // Request camera permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE
                );
            }
        } catch (Exception e) {
            handleError("opening camera", e);
        }
    }

    /**
     * Open scan history
     */
    private void openHistory() {
        try {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivitySafely(intent);
        } catch (Exception e) {
            handleError("opening history", e);
        }
    }

    /**
     * Open plant guide/disease information
     */
    private void openPlantGuide() {
        try {
            Intent intent = new Intent(this, com.plantcare.diseasedetector.ui.info.PlantCareGuideActivity.class);
            startActivitySafely(intent);
        } catch (Exception e) {
            handleError("opening plant guide", e);
        }
    }

    /**
     * Open statistics/analytics
     */
    private void openStatistics() {
        // TODO: Implement statistics activity
        showToastSafely("Statistics - Coming Soon!");
    }

    /**
     * Open settings
     */
    private void openSettings() {
        try {
            Intent intent = new Intent(this, com.plantcare.diseasedetector.ui.settings.SettingsActivity.class);
            startActivitySafely(intent);
        } catch (Exception e) {
            handleError("opening settings", e);
        }
    }

    /**
     * Open scan result detail
     */
    private void openScanResultDetail(ScanResult scanResult) {
        try {
            if (scanResult != null) {
                Intent intent = new Intent(this, com.plantcare.diseasedetector.ui.results.ResultsActivity.class);
                intent.putExtra(com.plantcare.diseasedetector.ui.results.ResultsActivity.EXTRA_SCAN_RESULT_ID, scanResult.getId());
                startActivitySafely(intent);
            }
        } catch (Exception e) {
            handleError("opening scan result detail", e);
        }
    }

    /**
     * Handle permission request results with improved error handling
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        try {
            if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
                boolean allPermissionsGranted = true;

                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }

                if (allPermissionsGranted) {
                    showToastSafely("Permissions granted! You can now scan plants.");
                } else {
                    showToastSafely("Camera permission is required to scan plants.");
                }
            }
        } catch (Exception e) {
            handleError("permission result handling", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Refresh recent scans when returning to main activity
            loadRecentScans();
        } catch (Exception e) {
            handleError("onResume", e);
        }
    }

    @Override
    protected void cleanupResources() {
        super.cleanupResources();
        try {
            // Cleanup database executor
            shutdownExecutorSafely(databaseExecutor);

            // Clear adapter references
            if (recentScansAdapter != null) {
                recentScansAdapter.setOnItemClickListener(null);
            }

            // Clear lists
            if (recentScansList != null) {
                recentScansList.clear();
            }

            // Perform memory cleanup
            performMemoryCleanup();

            logLifecycle("MainActivity resources cleaned up");
        } catch (Exception e) {
            handleError("resource cleanup", e);
        }
    }

    @Override
    protected void onWindowFocusGained() {
        super.onWindowFocusGained();
        try {
            // Refresh data when window gains focus
            if (isActivitySafe()) {
                loadRecentScans();
            }
        } catch (Exception e) {
            handleError("window focus gained", e);
        }
    }
}