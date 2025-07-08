package com.plantcare.diseasedetector.ui.info;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.models.DiseaseInfo;
import com.plantcare.diseasedetector.data.repository.DiseaseRepository;
import com.plantcare.diseasedetector.ui.camera.CameraActivity;

/**
 * Activity to display detailed information about a specific plant disease
 */
public class DiseaseInfoActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "DiseaseInfoActivity";

    // Intent Extra Keys
    public static final String EXTRA_DISEASE_ID = "disease_id";
    public static final String EXTRA_DISEASE_NAME = "disease_name";
    public static final String EXTRA_PREDICTED_CLASS = "predicted_class";

    // UI Components
    private MaterialToolbar toolbar;
    private ImageView ivDiseaseImage, ivSeverityIcon;
    private TextView tvDiseaseName, tvScientificName, tvPlantType, tvDescription;
    private TextView tvSeverityLevel, tvTreatableStatus, tvRecoveryTime;
    private ProgressBar progressLoading;
    private View layoutContent, layoutError, layoutEmptyState;
    private TextView tvErrorMessage;

    // Tabs and Content
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private DiseaseInfoPagerAdapter pagerAdapter;

    // Action Components
    private ChipGroup chipGroupTags;
    private MaterialButton btnLearnMore, btnGetTreatment, btnShareInfo;
    private FloatingActionButton fabScanPlant;
    private CardView cardQuickInfo, cardSeverity;

    // Data
    private DiseaseRepository repository;
    private DiseaseInfo diseaseInfo;
    private int diseaseId = -1;
    private String diseaseName;
    private String predictedClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_info);

        // Initialize repository
        repository = DiseaseRepository.getInstance(this);

        // Get intent extras
        getIntentExtras();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupTabs();

        // Load disease information
        loadDiseaseInfo();
    }

    /**
     * Get data from intent extras
     */
    private void getIntentExtras() {
        Intent intent = getIntent();
        diseaseId = intent.getIntExtra(EXTRA_DISEASE_ID, -1);
        diseaseName = intent.getStringExtra(EXTRA_DISEASE_NAME);
        predictedClass = intent.getStringExtra(EXTRA_PREDICTED_CLASS);

        Log.d(TAG, "Disease ID: " + diseaseId + ", Name: " + diseaseName + ", Predicted: " + predictedClass);
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivDiseaseImage = findViewById(R.id.iv_disease_image);
        ivSeverityIcon = findViewById(R.id.iv_severity_icon);
        tvDiseaseName = findViewById(R.id.tv_disease_name);
        tvScientificName = findViewById(R.id.tv_scientific_name);
        tvPlantType = findViewById(R.id.tv_plant_type);
        tvDescription = findViewById(R.id.tv_description);
        tvSeverityLevel = findViewById(R.id.tv_severity_level);
        tvTreatableStatus = findViewById(R.id.tv_treatable_status);
        tvRecoveryTime = findViewById(R.id.tv_recovery_time);
        progressLoading = findViewById(R.id.progress_loading);
        layoutContent = findViewById(R.id.layout_content);
        layoutError = findViewById(R.id.layout_error);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvErrorMessage = findViewById(R.id.tv_error_message);

        // Tabs
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // Action components
        chipGroupTags = findViewById(R.id.chip_group_tags);
        btnLearnMore = findViewById(R.id.btn_learn_more);
        btnGetTreatment = findViewById(R.id.btn_get_treatment);
        btnShareInfo = findViewById(R.id.btn_share_info);
        fabScanPlant = findViewById(R.id.fab_scan_plant);
        cardQuickInfo = findViewById(R.id.card_quick_info);
        cardSeverity = findViewById(R.id.card_severity);
    }

    /**
     * Setup toolbar
     * FIXED: Safely handle ActionBar setup to prevent conflicts
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
                    getSupportActionBar().setTitle("Disease Information");
                }
            } catch (IllegalStateException e) {
                // If there's a conflict, just configure the toolbar directly
                toolbar.setTitle("Disease Information");
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnLearnMore.setOnClickListener(this);
        btnGetTreatment.setOnClickListener(this);
        btnShareInfo.setOnClickListener(this);
        fabScanPlant.setOnClickListener(this);
    }

    /**
     * Setup tabs and ViewPager
     */
    private void setupTabs() {
        pagerAdapter = new DiseaseInfoPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Symptoms");
                            tab.setIcon(R.drawable.ic_medical);
                            break;
                        case 1:
                            tab.setText("Treatment");
                            tab.setIcon(R.drawable.ic_health_check);
                            break;
                        case 2:
                            tab.setText("Prevention");
                            tab.setIcon(R.drawable.ic_tips);
                            break;
                    }
                }
        ).attach();
    }

    /**
     * Load disease information from repository
     */
    private void loadDiseaseInfo() {
        showLoading(true);

        if (diseaseId != -1) {
            // Load by ID
            repository.getDiseaseById(diseaseId, new DiseaseRepository.RepositoryCallback<DiseaseInfo>() {
                @Override
                public void onSuccess(DiseaseInfo result) {
                    runOnUiThread(() -> {
                        diseaseInfo = result;
                        displayDiseaseInfo();
                        showLoading(false);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showError("Failed to load disease information: " + error);
                        showLoading(false);
                    });
                }
            });
        } else if (predictedClass != null) {
            // Load by predicted class
            repository.getDiseaseForPrediction(predictedClass, new DiseaseRepository.RepositoryCallback<DiseaseInfo>() {
                @Override
                public void onSuccess(DiseaseInfo result) {
                    runOnUiThread(() -> {
                        diseaseInfo = result;
                        if (diseaseInfo != null) {
                            displayDiseaseInfo();
                        } else {
                            showEmptyState();
                        }
                        showLoading(false);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showError("Failed to load disease information: " + error);
                        showLoading(false);
                    });
                }
            });
        } else if (diseaseName != null) {
            // Search by name
            repository.searchDiseases(diseaseName, new DiseaseRepository.RepositoryCallback<java.util.List<DiseaseInfo>>() {
                @Override
                public void onSuccess(java.util.List<DiseaseInfo> result) {
                    runOnUiThread(() -> {
                        if (result != null && !result.isEmpty()) {
                            diseaseInfo = result.get(0); // Take first match
                            displayDiseaseInfo();
                        } else {
                            showEmptyState();
                        }
                        showLoading(false);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showError("Failed to load disease information: " + error);
                        showLoading(false);
                    });
                }
            });
        } else {
            showError("No disease information provided");
            showLoading(false);
        }
    }

    /**
     * Display disease information in UI
     */
    private void displayDiseaseInfo() {
        if (diseaseInfo == null) {
            showEmptyState();
            return;
        }

        // Update toolbar title safely
        try {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(diseaseInfo.getFormattedName());
            } else if (toolbar != null) {
                toolbar.setTitle(diseaseInfo.getFormattedName());
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not update toolbar title", e);
        }

        // Basic information
        tvDiseaseName.setText(diseaseInfo.getFormattedName());

        if (diseaseInfo.getScientificName() != null && !diseaseInfo.getScientificName().isEmpty()) {
            tvScientificName.setText(diseaseInfo.getScientificName());
            tvScientificName.setVisibility(View.VISIBLE);
        } else {
            tvScientificName.setVisibility(View.GONE);
        }

        tvPlantType.setText(diseaseInfo.getPlantType());
        tvDescription.setText(diseaseInfo.getDescription());

        // Severity information
        setupSeverityInfo();

        // Recovery time
        if (diseaseInfo.getRecoveryTime() != null && !diseaseInfo.getRecoveryTime().isEmpty()) {
            tvRecoveryTime.setText(diseaseInfo.getRecoveryTime());
            tvRecoveryTime.setVisibility(View.VISIBLE);
        } else {
            tvRecoveryTime.setVisibility(View.GONE);
        }

        // Treatable status
        setupTreatableStatus();

        // Setup tags
        setupTags();

        // Update pager adapter with disease info
        pagerAdapter.setDiseaseInfo(diseaseInfo);

        // Show content
        layoutContent.setVisibility(View.VISIBLE);
    }

    /**
     * Setup severity information display
     */
    private void setupSeverityInfo() {
        DiseaseInfo.Severity severity = diseaseInfo.getSeverity();
        tvSeverityLevel.setText(severity.getDisplayName());

        // Set severity icon and color
        int colorRes;
        int iconRes;
        switch (severity) {
            case HIGH:
                colorRes = R.color.disease_red;
                iconRes = R.drawable.ic_warning;
                break;
            case MEDIUM:
                colorRes = R.color.warning_orange;
                iconRes = R.drawable.ic_info;
                break;
            case LOW:
                colorRes = R.color.healthy_green;
                iconRes = R.drawable.ic_health_check;
                break;
            default:
                colorRes = R.color.gray_medium;
                iconRes = R.drawable.ic_info;
        }

        ivSeverityIcon.setImageResource(iconRes);
        ivSeverityIcon.setColorFilter(ContextCompat.getColor(this, colorRes));
        cardSeverity.setCardBackgroundColor(ContextCompat.getColor(this, colorRes));
    }

    /**
     * Setup treatable status
     */
    private void setupTreatableStatus() {
        if (diseaseInfo.isTreatable()) {
            tvTreatableStatus.setText("Treatable");
            tvTreatableStatus.setTextColor(ContextCompat.getColor(this, R.color.healthy_green));
            btnGetTreatment.setVisibility(View.VISIBLE);
        } else {
            tvTreatableStatus.setText("Management Only");
            tvTreatableStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_orange));
            btnGetTreatment.setText("Management Guide");
        }
    }

    /**
     * Setup tags/chips
     */
    private void setupTags() {
        chipGroupTags.removeAllViews();

        // Add severity chip
        addChip(diseaseInfo.getSeverity().getDisplayName() + " Severity", true);

        // Add treatable chip
        if (diseaseInfo.isTreatable()) {
            addChip("Treatable", false);
        }

        // Add common disease chip
        if (diseaseInfo.isCommon()) {
            addChip("Common Disease", false);
        }

        // Add affected parts chips
        String[] affectedParts = diseaseInfo.getAffectedPartsArray();
        for (String part : affectedParts) {
            if (!part.trim().isEmpty()) {
                addChip(part.trim(), false);
            }
        }
    }

    /**
     * Add chip to chip group
     */
    private void addChip(String text, boolean isImportant) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);

        if (isImportant) {
            chip.setChipBackgroundColorResource(R.color.primary_color);
            chip.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            chip.setChipBackgroundColorResource(R.color.gray_light);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }

        chipGroupTags.addView(chip);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_learn_more) {
            openPlantCareGuide();
        } else if (id == R.id.btn_get_treatment) {
            showTreatmentOptions();
        } else if (id == R.id.btn_share_info) {
            shareInformation();
        } else if (id == R.id.fab_scan_plant) {
            openCamera();
        }
    }

    /**
     * Open plant care guide
     */
    private void openPlantCareGuide() {
        Intent intent = new Intent(this, PlantCareGuideActivity.class);
        if (diseaseInfo != null) {
            intent.putExtra(PlantCareGuideActivity.EXTRA_PLANT_TYPE, diseaseInfo.getPlantType());
            intent.putExtra(PlantCareGuideActivity.EXTRA_DISEASE_NAME, diseaseInfo.getDiseaseName());
        }
        startActivity(intent);
    }

    /**
     * Show treatment options tab
     */
    private void showTreatmentOptions() {
        viewPager.setCurrentItem(1); // Treatment tab
    }

    /**
     * Share disease information
     */
    private void shareInformation() {
        if (diseaseInfo == null) {
            showToast("No information to share");
            return;
        }

        String shareText = createShareText();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Disease Information: " + diseaseInfo.getFormattedName());

        startActivity(Intent.createChooser(shareIntent, "Share Disease Information"));
    }

    /**
     * Create share text
     */
    private String createShareText() {
        StringBuilder text = new StringBuilder();
        text.append("🌱 Disease Information\n\n");
        text.append("Disease: ").append(diseaseInfo.getFormattedName()).append("\n");
        text.append("Plant: ").append(diseaseInfo.getPlantType()).append("\n");
        text.append("Severity: ").append(diseaseInfo.getSeverity().getDisplayName()).append("\n");
        text.append("Treatable: ").append(diseaseInfo.isTreatable() ? "Yes" : "No").append("\n\n");
        text.append("Description: ").append(diseaseInfo.getDescription()).append("\n\n");
        text.append("Learn more with Plant Disease Detector app!");

        return text.toString();
    }

    /**
     * Open camera for new scan
     */
    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    /**
     * Show loading state
     */
    private void showLoading(boolean show) {
        progressLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    /**
     * Show error state
     */
    private void showError(String message) {
        layoutError.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        tvErrorMessage.setText(message);
    }

    /**
     * Show empty state
     */
    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
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
    protected void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.cleanup();
        }
    }
}