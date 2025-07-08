package com.plantcare.diseasedetector.ui.results;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.plantcare.diseasedetector.MainActivity;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.ui.camera.CameraActivity;
import com.plantcare.diseasedetector.ui.history.HistoryActivity;
import com.plantcare.diseasedetector.utils.DateUtils;
import com.plantcare.diseasedetector.utils.ImageUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Results Activity - Display AI analysis results with detailed information
 */
public class ResultsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ResultsActivity";

    // UI Components
    private MaterialToolbar toolbar;
    private ImageView ivPlantImage, ivStatusIcon;
    private TextView tvPlantName, tvHealthStatus, tvConfidenceDetail, tvConfidenceBadge;
    private TextView tvScanDate, tvDiseaseDescription, tvSeverityLevel;
    private ProgressBar progressConfidence;
    private RecyclerView rvTopPredictions;
    private CardView cardDiseaseInfo, cardHealthyTips;
    private View layoutSeverity;

    // Action Buttons
    private MaterialButton btnSaveResult, btnShareResult, btnViewTreatment;
    private MaterialButton btnLearnMore, btnScanAnother, btnViewHistory;

    // Data
    private ScanResult scanResult;
    private AppDatabase database;
    private TopPredictionsAdapter topPredictionsAdapter;
    private ExecutorService executor;

    // Intent Extra Keys
    public static final String EXTRA_SCAN_RESULT_ID = "scan_result_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Initialize components
        initializeViews();
        initializeExecutor();
        initializeDatabase();
        setupToolbar();
        setupClickListeners();
        setupRecyclerView();

        // Load scan result
        loadScanResult();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivPlantImage = findViewById(R.id.iv_plant_image);
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        tvPlantName = findViewById(R.id.tv_plant_name);
        tvHealthStatus = findViewById(R.id.tv_health_status);
        tvConfidenceDetail = findViewById(R.id.tv_confidence_detail);
        tvConfidenceBadge = findViewById(R.id.tv_confidence_badge);
        tvScanDate = findViewById(R.id.tv_scan_date);
        tvDiseaseDescription = findViewById(R.id.tv_disease_description);
        tvSeverityLevel = findViewById(R.id.tv_severity_level);
        progressConfidence = findViewById(R.id.progress_confidence);
        rvTopPredictions = findViewById(R.id.rv_top_predictions);
        cardDiseaseInfo = findViewById(R.id.card_disease_info);
        cardHealthyTips = findViewById(R.id.card_healthy_tips);
        layoutSeverity = findViewById(R.id.layout_severity);

        // Buttons
        btnSaveResult = findViewById(R.id.btn_save_result);
        btnShareResult = findViewById(R.id.btn_share_result);
        btnViewTreatment = findViewById(R.id.btn_view_treatment);
        btnLearnMore = findViewById(R.id.btn_learn_more);
        btnScanAnother = findViewById(R.id.btn_scan_another);
        btnViewHistory = findViewById(R.id.btn_view_history);
    }

    /**
     * Initialize executor for background tasks
     */
    private void initializeExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize database
     */
    private void initializeDatabase() {
        database = AppDatabase.getInstance(this);
    }

    /**
     * Setup toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnSaveResult.setOnClickListener(this);
        btnShareResult.setOnClickListener(this);
        btnViewTreatment.setOnClickListener(this);
        btnLearnMore.setOnClickListener(this);
        btnScanAnother.setOnClickListener(this);
        btnViewHistory.setOnClickListener(this);
        ivPlantImage.setOnClickListener(this);
    }

    /**
     * Setup RecyclerView for top predictions
     */
    private void setupRecyclerView() {
        topPredictionsAdapter = new TopPredictionsAdapter(this);
        rvTopPredictions.setLayoutManager(new LinearLayoutManager(this));
        rvTopPredictions.setAdapter(topPredictionsAdapter);
    }

    /**
     * Load scan result from database
     */
    private void loadScanResult() {
        int scanResultId = getIntent().getIntExtra(EXTRA_SCAN_RESULT_ID, -1);
        if (scanResultId == -1) {
            showToast("Invalid scan result");
            finish();
            return;
        }

        executor.execute(() -> {
            scanResult = database.scanResultDao().getScanResultById(scanResultId);

            runOnUiThread(() -> {
                if (scanResult != null) {
                    displayResults();
                } else {
                    showToast("Scan result not found");
                    finish();
                }
            });
        });
    }

    /**
     * Display the scan results in UI
     */
    private void displayResults() {
        // Load and display plant image
        loadPlantImage();

        // Set plant name
        tvPlantName.setText(scanResult.getDisplayName());

        // Set health status
        setHealthStatus();

        // Set confidence information
        setConfidenceInfo();

        // Set scan date
        setScanDate();

        // Show appropriate cards based on health status
        showRelevantCards();

        // Load top predictions
        loadTopPredictions();
    }

    /**
     * Load and display plant image
     */
    private void loadPlantImage() {
        if (scanResult.getImagePath() != null && !scanResult.getImagePath().isEmpty()) {
            executor.execute(() -> {
                Bitmap bitmap = ImageUtils.getDisplayBitmap(scanResult.getImagePath());

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        ivPlantImage.setImageBitmap(bitmap);
                    } else {
                        ivPlantImage.setImageResource(R.drawable.placeholder_plant);
                    }
                });
            });
        } else {
            ivPlantImage.setImageResource(R.drawable.placeholder_plant);
        }
    }

    /**
     * Set health status information
     */
    private void setHealthStatus() {
        if (scanResult.isHealthy()) {
            // Healthy plant
            tvHealthStatus.setText("Healthy Plant");
            tvHealthStatus.setTextColor(ContextCompat.getColor(this, R.color.healthy_green));
            ivStatusIcon.setImageResource(R.drawable.ic_health_check);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.healthy_green));
            layoutSeverity.setVisibility(View.GONE);
        } else {
            // Disease detected
            String diseaseText = scanResult.getHealthStatusText();
            tvHealthStatus.setText(diseaseText);
            tvHealthStatus.setTextColor(ContextCompat.getColor(this, R.color.disease_red));
            ivStatusIcon.setImageResource(R.drawable.ic_warning);
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.disease_red));

            // Show severity indicator
            setSeverityLevel();
        }
    }

    /**
     * Set severity level for diseases
     */
    private void setSeverityLevel() {
        ScanResult.SeverityLevel severity = scanResult.getSeverityLevel();

        if (severity != ScanResult.SeverityLevel.HEALTHY) {
            layoutSeverity.setVisibility(View.VISIBLE);
            tvSeverityLevel.setText(severity.name());

            // Set background color based on severity
            int colorRes;
            switch (severity) {
                case HIGH:
                    colorRes = R.color.disease_red;
                    break;
                case MEDIUM:
                    colorRes = R.color.warning_orange;
                    break;
                case LOW:
                    colorRes = R.color.gray_medium;
                    break;
                default:
                    colorRes = R.color.gray_medium;
            }

            layoutSeverity.setBackgroundTintList(
                    ContextCompat.getColorStateList(this, colorRes));
        } else {
            layoutSeverity.setVisibility(View.GONE);
        }
    }

    /**
     * Set confidence information
     */
    private void setConfidenceInfo() {
        float confidence = scanResult.getConfidence();
        int confidencePercent = Math.round(confidence * 100);

        // Set confidence badge
        tvConfidenceBadge.setText(confidencePercent + "%");

        // Set confidence detail
        tvConfidenceDetail.setText(String.format("Confidence: %.1f%%", confidence * 100));

        // Set progress bar
        progressConfidence.setProgress(confidencePercent);

        // Change progress color based on confidence level
        int progressColor;
        if (confidence >= 0.8f) {
            progressColor = R.color.healthy_green;
        } else if (confidence >= 0.6f) {
            progressColor = R.color.warning_orange;
        } else {
            progressColor = R.color.disease_red;
        }

        progressConfidence.setProgressTintList(
                ContextCompat.getColorStateList(this, progressColor));
    }

    /**
     * Set scan date
     */
    private void setScanDate() {
        if (scanResult.getScanDate() != null) {
            String dateText = DateUtils.getFriendlyDate(scanResult.getScanDate()) +
                    ", " + DateUtils.getFormattedTime(scanResult.getScanDate());
            tvScanDate.setText(dateText);
        }
    }

    /**
     * Show relevant cards based on health status
     */
    private void showRelevantCards() {
        if (scanResult.isHealthy()) {
            cardDiseaseInfo.setVisibility(View.GONE);
            cardHealthyTips.setVisibility(View.VISIBLE);
        } else {
            cardDiseaseInfo.setVisibility(View.VISIBLE);
            cardHealthyTips.setVisibility(View.GONE);

            // Set disease description
            setDiseaseDescription();
        }
    }

    /**
     * Set disease description
     */
    private void setDiseaseDescription() {
        // Get basic disease description based on predicted class
        String description = getDiseaseDescription(scanResult.getPredictedClass());
        tvDiseaseDescription.setText(description);
    }

    /**
     * Get disease description by class name
     */
    private String getDiseaseDescription(String className) {
        if (className == null) return "No description available.";

        // Basic descriptions for common diseases
        if (className.contains("scab")) {
            return "A fungal disease that causes dark, scabby lesions on leaves and fruit. It thrives in wet, cool conditions and can reduce fruit quality.";
        } else if (className.contains("rot")) {
            return "A fungal disease that causes rotting of plant tissues. It can affect leaves, stems, and fruit, leading to significant crop loss.";
        } else if (className.contains("rust")) {
            return "A fungal disease characterized by rust-colored spots on leaves. It can cause premature leaf drop and weaken the plant.";
        } else if (className.contains("blight")) {
            return "A disease that causes rapid browning and death of plant tissues. It can spread quickly in favorable conditions.";
        } else if (className.contains("mildew")) {
            return "A fungal disease that appears as white, powdery growth on plant surfaces. It can reduce photosynthesis and plant vigor.";
        } else if (className.contains("spot")) {
            return "A disease characterized by spots on leaves and stems. It can cause defoliation and reduce plant health.";
        } else if (className.contains("virus")) {
            return "A viral disease that can cause various symptoms including yellowing, curling, and stunted growth. Spread by insects or mechanical means.";
        } else {
            return "A plant disease that requires proper identification and treatment. Consult agricultural extension services for specific management strategies.";
        }
    }

    /**
     * Load top predictions for the RecyclerView
     */
    private void loadTopPredictions() {
        // Create mock top predictions based on confidence
        // In a real implementation, you would get this from the ML model
        TopPrediction[] topPredictions = createTopPredictions();
        topPredictionsAdapter.updatePredictions(topPredictions);
    }

    /**
     * Create top predictions array
     */
    private TopPrediction[] createTopPredictions() {
        // For demo purposes, create 3 predictions with decreasing confidence
        TopPrediction[] predictions = new TopPrediction[3];

        // Main prediction
        predictions[0] = new TopPrediction(
                scanResult.getPredictedClass(),
                scanResult.getConfidence(),
                1
        );

        // Generate mock secondary predictions
        predictions[1] = new TopPrediction(
                getAlternativePrediction(scanResult.getPredictedClass()),
                Math.max(0.1f, scanResult.getConfidence() - 0.2f),
                2
        );

        predictions[2] = new TopPrediction(
                getAlternativePrediction2(scanResult.getPredictedClass()),
                Math.max(0.05f, scanResult.getConfidence() - 0.4f),
                3
        );

        return predictions;
    }

    /**
     * Get alternative prediction for demo
     */
    private String getAlternativePrediction(String mainPrediction) {
        if (mainPrediction.contains("Apple")) {
            return mainPrediction.contains("healthy") ? "Apple___Apple_scab" : "Apple___healthy";
        } else if (mainPrediction.contains("Tomato")) {
            return mainPrediction.contains("healthy") ? "Tomato___Early_blight" : "Tomato___healthy";
        } else {
            return "Background_without_leaves";
        }
    }

    /**
     * Get second alternative prediction for demo
     */
    private String getAlternativePrediction2(String mainPrediction) {
        if (mainPrediction.contains("Apple")) {
            return "Apple___Black_rot";
        } else if (mainPrediction.contains("Tomato")) {
            return "Tomato___Late_blight";
        } else {
            return "Background_without_leaves";
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_save_result) {
            saveResultToGallery();
        } else if (id == R.id.btn_share_result) {
            shareResult();
        } else if (id == R.id.btn_view_treatment) {
            viewTreatmentInfo();
        } else if (id == R.id.btn_learn_more) {
            learnMoreAboutPlantCare();
        } else if (id == R.id.btn_scan_another) {
            scanAnotherPlant();
        } else if (id == R.id.btn_view_history) {
            viewScanHistory();
        } else if (id == R.id.iv_plant_image) {
            viewFullImage();
        }
    }

    /**
     * Save result image to device gallery
     */
    private void saveResultToGallery() {
        if (scanResult.getImagePath() == null) {
            showToast("No image to save");
            return;
        }

        try {
            // Load the image
            Bitmap bitmap = ImageUtils.getDisplayBitmap(scanResult.getImagePath());
            if (bitmap != null) {
                // Save to gallery
                String savedPath = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        "Plant_Scan_" + System.currentTimeMillis(),
                        "Plant disease scan result"
                );

                if (savedPath != null) {
                    showToast("Image saved to gallery");
                } else {
                    showToast("Failed to save image");
                }
            } else {
                showToast("Failed to load image");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving image to gallery", e);
            showToast("Error saving image");
        }
    }

    /**
     * Share scan result
     */
    private void shareResult() {
        try {
            // Create share text
            String shareText = createShareText();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Plant Disease Scan Result");

            // Add image if available
            if (scanResult.getImagePath() != null) {
                File imageFile = new File(scanResult.getImagePath());
                if (imageFile.exists()) {
                    Uri imageUri = FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".fileprovider",
                            imageFile
                    );
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            startActivity(Intent.createChooser(shareIntent, "Share scan result"));

        } catch (Exception e) {
            Log.e(TAG, "Error sharing result", e);
            showToast("Error sharing result");
        }
    }

    /**
     * Create share text
     */
    private String createShareText() {
        StringBuilder text = new StringBuilder();
        text.append("🌱 Plant Disease Scan Result\n\n");
        text.append("Plant: ").append(scanResult.getDisplayName()).append("\n");
        text.append("Status: ").append(scanResult.getHealthStatusText()).append("\n");
        text.append("Confidence: ").append(scanResult.getConfidencePercentage()).append("\n");
        text.append("Scanned: ").append(DateUtils.getFormattedDateTime(scanResult.getScanDate())).append("\n\n");
        text.append("Analyzed with Plant Disease Detector AI");

        return text.toString();
    }

    /**
     * View treatment information
     */
    private void viewTreatmentInfo() {
        // TODO: Implement disease treatment activity
        showToast("Treatment information - Coming soon!");
    }

    /**
     * Learn more about plant care
     */
    private void learnMoreAboutPlantCare() {
        // TODO: Implement plant care guide
        showToast("Plant care guide - Coming soon!");
    }

    /**
     * Scan another plant
     */
    private void scanAnotherPlant() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * View scan history
     */
    private void viewScanHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    /**
     * View full image
     */
    private void viewFullImage() {
        // TODO: Implement full-screen image viewer
        showToast("Full image view - Coming soon!");
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackPressed() {
        // Navigate back to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Helper class for top predictions
     */
    public static class TopPrediction {
        public final String className;
        public final float confidence;
        public final int rank;

        public TopPrediction(String className, float confidence, int rank) {
            this.className = className;
            this.confidence = confidence;
            this.rank = rank;
        }

        public String getFormattedClassName() {
            return className.replace("___", ": ").replace("_", " ");
        }

        public String getConfidencePercentage() {
            return String.format("%.1f%%", confidence * 100);
        }
    }
}