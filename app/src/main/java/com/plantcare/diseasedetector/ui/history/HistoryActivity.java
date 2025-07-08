package com.plantcare.diseasedetector.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.ui.base.BaseActivity;
import com.plantcare.diseasedetector.ui.camera.CameraActivity;
import com.plantcare.diseasedetector.ui.results.ResultsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * History Activity - Display and manage scan history with improved performance and stability
 */
public class HistoryActivity extends BaseActivity implements View.OnClickListener, HistoryAdapter.OnItemClickListener {

    private static final String TAG = "HistoryActivity";

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private MaterialButton btnFilter;
    private ImageButton btnViewToggle;
    private RecyclerView rvHistory;
    private FloatingActionButton fabScan;
    private View layoutEmptyState, loadingOverlay, scrollFilterChips, layoutSortOptions;
    private MaterialButton btnStartScanning;

    // Statistics
    private TextView tvTotalScans, tvHealthyScans, tvDiseasedScans, tvResultsCount;

    // Filter and Sort Components
    private ChipGroup chipGroupFilters, chipGroupSort;
    private Chip chipHealthy, chipDiseased, chipThisWeek, chipThisMonth, chipHighConfidence;
    private Chip chipSortDate, chipSortConfidence, chipSortPlant;

    // Data
    private HistoryAdapter historyAdapter;
    private List<ScanResult> allScanResults;
    private List<ScanResult> filteredScanResults;
    private AppDatabase database;
    private ExecutorService databaseExecutor;

    // State
    private boolean isGridView = false;
    private boolean isFilterVisible = false;
    private String currentSearchQuery = "";
    private volatile boolean isLoadingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize components
        initializeComponents();
        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupRecyclerView();
        setupSearchAndFilter();

        // Load data
        loadStatistics();
        loadScanHistory();
    }

    /**
     * Initialize components with proper error handling
     */
    private void initializeComponents() {
        try {
            database = AppDatabase.getInstance(this);
            databaseExecutor = Executors.newSingleThreadExecutor();
            allScanResults = new ArrayList<>();
            filteredScanResults = new ArrayList<>();

            logLifecycle("Components initialized");
        } catch (Exception e) {
            handleError("component initialization", e);
        }
    }

    /**
     * Initialize all UI components with null checks
     */
    private void initializeViews() {
        try {
            toolbar = findViewById(R.id.toolbar);
            etSearch = findViewById(R.id.et_search);
            btnFilter = findViewById(R.id.btn_filter);
            btnViewToggle = findViewById(R.id.btn_view_toggle);
            rvHistory = findViewById(R.id.rv_history);
            fabScan = findViewById(R.id.fab_scan);
            layoutEmptyState = findViewById(R.id.layout_empty_state);
            loadingOverlay = findViewById(R.id.loading_overlay);
            btnStartScanning = findViewById(R.id.btn_start_scanning);
            scrollFilterChips = findViewById(R.id.scroll_filter_chips);
            layoutSortOptions = findViewById(R.id.layout_sort_options);

            // Statistics
            tvTotalScans = findViewById(R.id.tv_total_scans);
            tvHealthyScans = findViewById(R.id.tv_healthy_scans);
            tvDiseasedScans = findViewById(R.id.tv_diseased_scans);
            tvResultsCount = findViewById(R.id.tv_results_count);

            // Filter chips
            chipGroupFilters = findViewById(R.id.chip_group_filters);
            chipHealthy = findViewById(R.id.chip_healthy);
            chipDiseased = findViewById(R.id.chip_diseased);
            chipThisWeek = findViewById(R.id.chip_this_week);
            chipThisMonth = findViewById(R.id.chip_this_month);
            chipHighConfidence = findViewById(R.id.chip_high_confidence);

            // Sort chips
            chipGroupSort = findViewById(R.id.chip_group_sort);
            chipSortDate = findViewById(R.id.chip_sort_date);
            chipSortConfidence = findViewById(R.id.chip_sort_confidence);
            chipSortPlant = findViewById(R.id.chip_sort_plant);

            logLifecycle("Views initialized");
        } catch (Exception e) {
            handleError("view initialization", e);
        }
    }

    /**
     * Setup toolbar with safe ActionBar handling
     */
    private void setupToolbar() {
        if (toolbar != null) {
            try {
                // Only set support action bar if current theme doesn't provide one
                if (getSupportActionBar() == null) {
                    setSupportActionBar(toolbar);
                }

                // Configure action bar if available
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    getSupportActionBar().setTitle(""); // Title is in collapsing toolbar
                }
            } catch (IllegalStateException e) {
                // If there's a conflict, just configure the toolbar directly
                toolbar.setTitle("Scan History");
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        }
    }

    /**
     * Setup click listeners with null checks
     */
    private void setupClickListeners() {
        try {
            if (btnFilter != null) btnFilter.setOnClickListener(this);
            if (btnViewToggle != null) btnViewToggle.setOnClickListener(this);
            if (fabScan != null) fabScan.setOnClickListener(this);
            if (btnStartScanning != null) btnStartScanning.setOnClickListener(this);

            // Chip listeners with null checks
            if (chipGroupFilters != null) {
                chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (isActivitySafe()) {
                        applyFilters();
                    }
                });
            }

            if (chipGroupSort != null) {
                chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (isActivitySafe()) {
                        applySorting();
                    }
                });
            }

            logLifecycle("Click listeners setup");
        } catch (Exception e) {
            handleError("click listener setup", e);
        }
    }

    /**
     * Setup RecyclerView with improved error handling
     */
    private void setupRecyclerView() {
        try {
            if (rvHistory != null) {
                historyAdapter = new HistoryAdapter(this, filteredScanResults, isGridView);
                historyAdapter.setOnItemClickListener(this);

                rvHistory.setLayoutManager(new LinearLayoutManager(this));
                rvHistory.setAdapter(historyAdapter);

                logLifecycle("RecyclerView setup completed");
            }
        } catch (Exception e) {
            handleError("RecyclerView setup", e);
        }
    }

    /**
     * Setup search and filter functionality with proper error handling
     */
    private void setupSearchAndFilter() {
        try {
            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (isActivitySafe()) {
                            currentSearchQuery = s != null ? s.toString().trim() : "";
                            applyFilters();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }

            logLifecycle("Search and filter setup completed");
        } catch (Exception e) {
            handleError("search and filter setup", e);
        }
    }

    /**
     * Load statistics from database with improved error handling
     */
    private void loadStatistics() {
        if (!isActivitySafe() || database == null || databaseExecutor == null) {
            return;
        }

        try {
            databaseExecutor.execute(() -> {
                try {
                    int totalScans = database.scanResultDao().getTotalScanCount();
                    int healthyScans = database.scanResultDao().getHealthyScanCount();
                    int diseasedScans = database.scanResultDao().getDiseasedScanCount();

                    runOnUiThreadSafely(() -> {
                        try {
                            if (tvTotalScans != null) tvTotalScans.setText(String.valueOf(totalScans));
                            if (tvHealthyScans != null) tvHealthyScans.setText(String.valueOf(healthyScans));
                            if (tvDiseasedScans != null) tvDiseasedScans.setText(String.valueOf(diseasedScans));
                        } catch (Exception e) {
                            handleError("statistics UI update", e);
                        }
                    });
                } catch (Exception e) {
                    handleError("statistics database query", e);
                }
            });
        } catch (Exception e) {
            handleError("loadStatistics", e);
        }
    }

    /**
     * Load scan history from database with lifecycle and state checks
     */
    private void loadScanHistory() {
        if (!isActivitySafe() || database == null || databaseExecutor == null || isLoadingData) {
            return;
        }

        try {
            isLoadingData = true;
            showLoading(true);

            databaseExecutor.execute(() -> {
                try {
                    List<ScanResult> scans = database.scanResultDao().getAllScanResults();

                    runOnUiThreadSafely(() -> {
                        try {
                            isLoadingData = false;
                            showLoading(false);

                            if (allScanResults != null) {
                                allScanResults.clear();
                                if (scans != null) {
                                    allScanResults.addAll(scans);
                                }
                                applyFilters();
                                updateEmptyState();
                            }
                        } catch (Exception e) {
                            isLoadingData = false;
                            handleError("scan history UI update", e);
                        }
                    });
                } catch (Exception e) {
                    isLoadingData = false;
                    handleError("scan history database query", e);
                }
            });
        } catch (Exception e) {
            isLoadingData = false;
            handleError("loadScanHistory", e);
        }
    }

    /**
     * Apply filters and search with proper error handling
     */
    private void applyFilters() {
        if (!isActivitySafe() || filteredScanResults == null || allScanResults == null) {
            return;
        }

        try {
            filteredScanResults.clear();

            for (ScanResult scan : allScanResults) {
                if (matchesSearchQuery(scan) && matchesFilters(scan)) {
                    filteredScanResults.add(scan);
                }
            }

            applySorting();
            updateResultsCount();

            if (historyAdapter != null) {
                historyAdapter.notifyDataSetChanged();
            }
            updateEmptyState();
        } catch (Exception e) {
            handleError("apply filters", e);
        }
    }

    /**
     * Check if scan matches search query with null safety
     */
    private boolean matchesSearchQuery(ScanResult scan) {
        if (scan == null || currentSearchQuery.isEmpty()) return true;

        try {
            String query = currentSearchQuery.toLowerCase();
            return (scan.getDisplayName() != null && scan.getDisplayName().toLowerCase().contains(query)) ||
                    (scan.getHealthStatusText() != null && scan.getHealthStatusText().toLowerCase().contains(query)) ||
                    (scan.getPredictedClass() != null && scan.getPredictedClass().toLowerCase().contains(query));
        } catch (Exception e) {
            handleError("search query matching", e);
            return true; // Default to include if error occurs
        }
    }

    /**
     * Check if scan matches selected filters with improved error handling
     */
    private boolean matchesFilters(ScanResult scan) {
        if (scan == null) return false;

        try {
            // Health status filter
            if (chipHealthy != null && chipHealthy.isChecked() && !scan.isHealthy()) return false;
            if (chipDiseased != null && chipDiseased.isChecked() && scan.isHealthy()) return false;

            // Date filters
            if (chipThisWeek != null && chipThisWeek.isChecked() && !isThisWeek(scan.getScanDate())) return false;
            if (chipThisMonth != null && chipThisMonth.isChecked() && !isThisMonth(scan.getScanDate())) return false;

            // Confidence filter
            if (chipHighConfidence != null && chipHighConfidence.isChecked() && scan.getConfidence() < 0.8f) return false;

            return true;
        } catch (Exception e) {
            handleError("filter matching", e);
            return true; // Default to include if error occurs
        }
    }

    /**
     * Apply sorting to filtered results with error handling
     */
    private void applySorting() {
        if (!isActivitySafe() || filteredScanResults == null) {
            return;
        }

        try {
            if (chipSortDate != null && chipSortDate.isChecked()) {
                Collections.sort(filteredScanResults, (a, b) -> {
                    if (a == null || b == null) return 0;
                    Date dateA = a.getScanDate();
                    Date dateB = b.getScanDate();
                    if (dateA == null || dateB == null) return 0;
                    return dateB.compareTo(dateA);
                });
            } else if (chipSortConfidence != null && chipSortConfidence.isChecked()) {
                Collections.sort(filteredScanResults, (a, b) -> {
                    if (a == null || b == null) return 0;
                    return Float.compare(b.getConfidence(), a.getConfidence());
                });
            } else if (chipSortPlant != null && chipSortPlant.isChecked()) {
                Collections.sort(filteredScanResults, (a, b) -> {
                    if (a == null || b == null) return 0;
                    String nameA = a.getDisplayName();
                    String nameB = b.getDisplayName();
                    if (nameA == null || nameB == null) return 0;
                    return nameA.compareTo(nameB);
                });
            }
        } catch (Exception e) {
            handleError("apply sorting", e);
        }
    }

    /**
     * Check if date is within this week with null safety
     */
    private boolean isThisWeek(Date date) {
        if (date == null) return false;
        try {
            long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            return date.getTime() > weekAgo;
        } catch (Exception e) {
            handleError("this week check", e);
            return false;
        }
    }

    /**
     * Check if date is within this month with null safety
     */
    private boolean isThisMonth(Date date) {
        if (date == null) return false;
        try {
            long monthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            return date.getTime() > monthAgo;
        } catch (Exception e) {
            handleError("this month check", e);
            return false;
        }
    }

    /**
     * Update results count text with null safety
     */
    private void updateResultsCount() {
        try {
            if (tvResultsCount != null && filteredScanResults != null) {
                String countText;
                int count = filteredScanResults.size();
                if (count == 1) {
                    countText = "1 scan found";
                } else {
                    countText = count + " scans found";
                }
                tvResultsCount.setText(countText);
            }
        } catch (Exception e) {
            handleError("update results count", e);
        }
    }

    /**
     * Update empty state visibility with proper null checks
     */
    private void updateEmptyState() {
        try {
            if (allScanResults == null || filteredScanResults == null) {
                return;
            }

            if (allScanResults.isEmpty()) {
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                if (rvHistory != null) rvHistory.setVisibility(View.GONE);
            } else if (filteredScanResults.isEmpty()) {
                // TODO: Show "no results for filters" state
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
            } else {
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            handleError("update empty state", e);
        }
    }

    /**
     * Show/hide loading overlay with null safety
     */
    private void showLoading(boolean show) {
        try {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            handleError("show loading", e);
        }
    }

    @Override
    public void onClick(View v) {
        if (!isActivitySafe() || v == null) {
            return;
        }

        try {
            int id = v.getId();

            if (id == R.id.btn_filter) {
                toggleFilters();
            } else if (id == R.id.btn_view_toggle) {
                toggleViewMode();
            } else if (id == R.id.fab_scan || id == R.id.btn_start_scanning) {
                openCamera();
            }
        } catch (Exception e) {
            handleError("click handling", e);
        }
    }

    /**
     * Toggle filter visibility with proper error handling
     */
    private void toggleFilters() {
        try {
            isFilterVisible = !isFilterVisible;

            if (scrollFilterChips != null) {
                scrollFilterChips.setVisibility(isFilterVisible ? View.VISIBLE : View.GONE);
            }
            if (layoutSortOptions != null) {
                layoutSortOptions.setVisibility(isFilterVisible ? View.VISIBLE : View.GONE);
            }

            // Update button text
            if (btnFilter != null) {
                btnFilter.setText(isFilterVisible ? "Hide Filters" : "Filter");
            }
        } catch (Exception e) {
            handleError("toggle filters", e);
        }
    }

    /**
     * Toggle between list and grid view with proper error handling
     */
    private void toggleViewMode() {
        try {
            isGridView = !isGridView;

            if (btnViewToggle != null) {
                btnViewToggle.setImageResource(isGridView ? R.drawable.ic_view_list : R.drawable.ic_view_grid);
            }

            // Update adapter view mode
            if (historyAdapter != null) {
                historyAdapter.setGridView(isGridView);
            }

            // Update RecyclerView layout manager
            if (rvHistory != null) {
                if (isGridView) {
                    rvHistory.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
                } else {
                    rvHistory.setLayoutManager(new LinearLayoutManager(this));
                }
            }
        } catch (Exception e) {
            handleError("toggle view mode", e);
        }
    }

    /**
     * Open camera for new scan
     */
    private void openCamera() {
        try {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivitySafely(intent);
        } catch (Exception e) {
            handleError("opening camera", e);
        }
    }

    /**
     * Handle scan item click with null safety
     */
    @Override
    public void onItemClick(ScanResult scanResult) {
        if (!isActivitySafe() || scanResult == null) {
            return;
        }

        try {
            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra(ResultsActivity.EXTRA_SCAN_RESULT_ID, scanResult.getId());
            startActivitySafely(intent);
        } catch (Exception e) {
            handleError("scan item click", e);
        }
    }

    /**
     * Handle scan item menu click - Enhanced with popup menu
     */
    @Override
    public void onMenuClick(ScanResult scanResult, View view) {
        if (!isActivitySafe() || scanResult == null || view == null) {
            return;
        }

        try {
            // Create popup menu
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);
            popup.getMenuInflater().inflate(R.menu.menu_history_item, popup.getMenu());

            // Set menu item click listener
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.menu_save_to_gallery) {
                    saveScanToGallery(scanResult);
                } else if (itemId == R.id.menu_export) {
                    exportScanData(scanResult);
                } else if (itemId == R.id.menu_delete) {
                    deleteScanResult(scanResult);
                }
                
                return true;
            });

            popup.show();

        } catch (Exception e) {
            handleError("menu click", e);
        }
    }

    /**
     * Handle scan item share - Removed (not needed)
     */
    @Override
    public void onShareClick(ScanResult scanResult) {
        // No longer used - menu simplified
    }

    /**
     * Handle scan item delete - Enhanced implementation
     */
    @Override
    public void onDeleteClick(ScanResult scanResult) {
        deleteScanResult(scanResult);
    }

    // Enhanced Menu Action Methods

    /**
     * Save scan image to device gallery
     */
    private void saveScanToGallery(ScanResult scanResult) {
        if (scanResult.getImagePath() == null || scanResult.getImagePath().isEmpty()) {
            showToastSafely("No image to save");
            return;
        }

        try {
            databaseExecutor.execute(() -> {
                try {
                    android.graphics.Bitmap bitmap = com.plantcare.diseasedetector.utils.ImageUtils.getDisplayBitmap(scanResult.getImagePath());
                    if (bitmap != null) {
                        String savedPath = android.provider.MediaStore.Images.Media.insertImage(
                                getContentResolver(),
                                bitmap,
                                "Plant_Scan_" + scanResult.getDisplayName() + "_" + System.currentTimeMillis(),
                                "Plant disease scan: " + scanResult.getHealthStatusText()
                        );

                        runOnUiThreadSafely(() -> {
                            if (savedPath != null) {
                                showToastSafely("Image saved to gallery");
                            } else {
                                showToastSafely("Failed to save image");
                            }
                        });
                    } else {
                        runOnUiThreadSafely(() -> showToastSafely("Failed to load image"));
                    }
                } catch (Exception e) {
                    runOnUiThreadSafely(() -> {
                        handleError("save to gallery", e);
                        showToastSafely("Error saving image");
                    });
                }
            });
        } catch (Exception e) {
            handleError("save scan to gallery", e);
        }
    }

    /**
     * Export scan data to external storage
     */
    private void exportScanData(ScanResult scanResult) {
        try {
            databaseExecutor.execute(() -> {
                try {
                    org.json.JSONObject jsonData = new org.json.JSONObject();
                    jsonData.put("id", scanResult.getId());
                    jsonData.put("plantName", scanResult.getDisplayName());
                    jsonData.put("healthStatus", scanResult.getHealthStatusText());
                    jsonData.put("confidence", scanResult.getConfidence());
                    jsonData.put("predictedClass", scanResult.getPredictedClass());
                    jsonData.put("isHealthy", scanResult.isHealthy());
                    jsonData.put("severityLevel", scanResult.getSeverityLevel().name());
                    
                    if (scanResult.getScanDate() != null) {
                        jsonData.put("scanDate", scanResult.getScanDate().getTime());
                        jsonData.put("scanDateFormatted", com.plantcare.diseasedetector.utils.DateUtils.getFormattedDateTime(scanResult.getScanDate()));
                    }
                    
                    if (scanResult.getNotes() != null && !scanResult.getNotes().isEmpty()) {
                        jsonData.put("notes", scanResult.getNotes());
                    }
                    
                    if (scanResult.getLocation() != null && !scanResult.getLocation().isEmpty()) {
                        jsonData.put("location", scanResult.getLocation());
                    }

                    String fileName = "plant_scan_" + scanResult.getId() + "_" + System.currentTimeMillis() + ".json";
                    java.io.File externalDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
                    if (externalDir != null) {
                        java.io.File exportFile = new java.io.File(externalDir, fileName);
                        
                        try (java.io.FileWriter writer = new java.io.FileWriter(exportFile)) {
                            writer.write(jsonData.toString(2));
                        }

                        runOnUiThreadSafely(() -> {
                            showToastSafely("Data exported to Documents folder");
                            shareExportedFile(exportFile);
                        });
                    } else {
                        runOnUiThreadSafely(() -> showToastSafely("External storage not available"));
                    }

                } catch (Exception e) {
                    runOnUiThreadSafely(() -> handleError("export scan data", e));
                }
            });
        } catch (Exception e) {
            handleError("export scan data", e);
        }
    }

    /**
     * Share exported file
     */
    private void shareExportedFile(java.io.File file) {
        try {
            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Plant Scan Data Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Plant disease scan data exported from Plant Disease Detector");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share exported data"));

        } catch (Exception e) {
            handleError("share exported file", e);
        }
    }

    /**
     * Delete scan result with confirmation
     */
    private void deleteScanResult(ScanResult scanResult) {
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Delete Scan Result");
            builder.setMessage("Are you sure you want to delete this scan result for " + 
                    scanResult.getDisplayName() + "? This action cannot be undone.");
            
            builder.setPositiveButton("Delete", (dialog, which) -> {
                performDeleteScan(scanResult);
            });
            
            builder.setNegativeButton("Cancel", null);
            
            builder.show();

        } catch (Exception e) {
            handleError("delete scan result", e);
        }
    }

    /**
     * Perform actual deletion of scan result
     */
    private void performDeleteScan(ScanResult scanResult) {
        try {
            databaseExecutor.execute(() -> {
                try {
                    database.scanResultDao().deleteScanResult(scanResult);

                    if (scanResult.getImagePath() != null && !scanResult.getImagePath().isEmpty()) {
                        try {
                            java.io.File imageFile = new java.io.File(scanResult.getImagePath());
                            if (imageFile.exists()) {
                                imageFile.delete();
                            }
                        } catch (Exception e) {
                            android.util.Log.w(TAG, "Could not delete image file", e);
                        }
                    }

                    runOnUiThreadSafely(() -> {
                        if (allScanResults != null) {
                            allScanResults.remove(scanResult);
                        }
                        if (filteredScanResults != null) {
                            filteredScanResults.remove(scanResult);
                        }
                        
                        if (historyAdapter != null) {
                            historyAdapter.notifyDataSetChanged();
                        }
                        
                        updateEmptyState();
                        updateResultsCount();
                        loadStatistics();
                        
                        showToastSafely("Scan result deleted");
                    });

                } catch (Exception e) {
                    runOnUiThreadSafely(() -> handleError("perform delete scan", e));
                }
            });
        } catch (Exception e) {
            handleError("perform delete scan", e);
        }
    }

    /**
     * Handle toolbar navigation
     */
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Refresh data when returning to activity
            if (!isLoadingData) {
                loadStatistics();
                loadScanHistory();
            }
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
            if (historyAdapter != null) {
                historyAdapter.setOnItemClickListener(null);
            }

            // Clear lists
            if (allScanResults != null) {
                allScanResults.clear();
            }
            if (filteredScanResults != null) {
                filteredScanResults.clear();
            }

            // Reset flags
            isLoadingData = false;

            // Perform memory cleanup
            performMemoryCleanup();

            logLifecycle("HistoryActivity resources cleaned up");
        } catch (Exception e) {
            handleError("resource cleanup", e);
        }
    }

    @Override
    protected void onWindowFocusGained() {
        super.onWindowFocusGained();
        try {
            // Refresh data when window gains focus
            if (isActivitySafe() && !isLoadingData) {
                loadStatistics();
                loadScanHistory();
            }
        } catch (Exception e) {
            handleError("window focus gained", e);
        }
    }
}