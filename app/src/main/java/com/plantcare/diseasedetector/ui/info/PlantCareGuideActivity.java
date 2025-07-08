package com.plantcare.diseasedetector.ui.info;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.ui.camera.CameraActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display general plant care guide and best practices
 */
public class PlantCareGuideActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "PlantCareGuideActivity";

    // Intent Extra Keys
    public static final String EXTRA_PLANT_TYPE = "plant_type";
    public static final String EXTRA_DISEASE_NAME = "disease_name";

    // UI Components
    private MaterialToolbar toolbar;
    private ImageView ivPlantImage;
    private FloatingActionButton fabScanPlant;

    // Quick Action Cards
    private CardView cardWatering, cardFertilizing, cardPruning, cardPestControl;
    
    // Content Cards (Initially Hidden)
    private CardView cardWateringContent, cardFertilizingContent, cardPruningContent, cardPestControlContent;
    private TextView tvWateringContent, tvFertilizingContent, tvPruningContent, tvPestControlContent;

    // Data
    private String plantType;
    private String diseaseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_care_guide);

        // Get intent extras
        getIntentExtras();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupClickListeners();

        // Load care guide
        loadPlantCareGuide();
    }

    /**
     * Get data from intent extras
     */
    private void getIntentExtras() {
        Intent intent = getIntent();
        plantType = intent.getStringExtra(EXTRA_PLANT_TYPE);
        diseaseName = intent.getStringExtra(EXTRA_DISEASE_NAME);

        if (plantType == null) {
            plantType = "General";
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivPlantImage = findViewById(R.id.iv_plant_image);
        fabScanPlant = findViewById(R.id.fab_scan_plant);

        // Quick action cards
        cardWatering = findViewById(R.id.card_watering);
        cardFertilizing = findViewById(R.id.card_fertilizing);
        cardPruning = findViewById(R.id.card_pruning);
        cardPestControl = findViewById(R.id.card_pest_control);
        
        // Content cards (initially hidden)
        cardWateringContent = findViewById(R.id.card_watering_content);
        cardFertilizingContent = findViewById(R.id.card_fertilizing_content);
        cardPruningContent = findViewById(R.id.card_pruning_content);
        cardPestControlContent = findViewById(R.id.card_pest_control_content);
        
        // Content text views
        tvWateringContent = findViewById(R.id.tv_watering_content);
        tvFertilizingContent = findViewById(R.id.tv_fertilizing_content);
        tvPruningContent = findViewById(R.id.tv_pruning_content);
        tvPestControlContent = findViewById(R.id.tv_pest_control_content);
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
                    getSupportActionBar().setTitle("Plant Care Guide");
                }
            } catch (IllegalStateException e) {
                // If there's a conflict, just configure the toolbar directly
                toolbar.setTitle("Plant Care Guide");
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        fabScanPlant.setOnClickListener(this);

        // Quick action cards
        cardWatering.setOnClickListener(this);
        cardFertilizing.setOnClickListener(this);
        cardPruning.setOnClickListener(this);
        cardPestControl.setOnClickListener(this);
    }

    /**
     * Load plant care guide based on plant type
     */
    private void loadPlantCareGuide() {
        // Set plant image
        setPlantImage();
    }

    /**
     * Get plant description
     */
    private String getPlantDescription() {
        switch (plantType.toLowerCase()) {
            case "tomato":
                return "Tomatoes are warm-season crops that require full sun, well-drained soil, and consistent watering. They are susceptible to various diseases but respond well to proper care.";
            case "apple":
                return "Apple trees require full sun, well-drained soil, and regular pruning. They need cross-pollination and are susceptible to various fungal diseases.";
            case "grape":
                return "Grapes thrive in warm, dry climates with good air circulation. They require well-drained soil and regular pruning for optimal fruit production.";
            case "potato":
                return "Potatoes grow best in cool weather with loose, well-drained soil. They require consistent moisture and are susceptible to blight diseases.";
            default:
                return "Proper plant care involves understanding your plant's specific needs for water, light, nutrients, and protection from pests and diseases.";
        }
    }

    /**
     * Set plant image based on type
     */
    private void setPlantImage() {
        // Always use the large guide manual icon for plant care guide
        ivPlantImage.setImageResource(R.drawable.ic_guide_manual_large);
        ivPlantImage.setColorFilter(ContextCompat.getColor(this, R.color.green_primary));
    }

    /**
     * Get watering guide for plant type
     */
    private String getWateringGuide() {
        switch (plantType.toLowerCase()) {
            case "tomato":
                return "Water tomatoes deeply 1-2 times per week. Keep soil consistently moist but not waterlogged. Water at the base to avoid wetting leaves.";
            case "apple":
                return "Apple trees need deep watering weekly during growing season. Reduce watering in fall to help trees prepare for winter.";
            case "grape":
                return "Grapes prefer deep, infrequent watering. Water thoroughly once per week, allowing soil to dry between waterings.";
            default:
                return "Most plants need consistent moisture. Water when top inch of soil feels dry. Water deeply rather than frequently.";
        }
    }

    /**
     * Get fertilizing guide
     */
    private String getFertilizingGuide() {
        switch (plantType.toLowerCase()) {
            case "tomato":
                return "Use balanced fertilizer (10-10-10) every 2-3 weeks. Switch to low-nitrogen, high-phosphorus fertilizer when flowering begins.";
            case "apple":
                return "Apply balanced fertilizer in early spring. Add compost around the base annually for long-term soil health.";
            default:
                return "Use balanced fertilizer according to plant needs. Organic compost improves soil structure and provides slow-release nutrients.";
        }
    }

    /**
     * Get pruning guide
     */
    private String getPruningGuide() {
        switch (plantType.toLowerCase()) {
            case "tomato":
                return "Remove suckers between main stem and branches. Prune lower leaves that touch the ground to prevent disease.";
            case "apple":
                return "Prune in late winter to remove dead, diseased, or crossing branches. Maintain open center for good air circulation.";
            default:
                return "Remove dead, diseased, or damaged parts regularly. Prune for shape and air circulation.";
        }
    }

    /**
     * Get pest control guide
     */
    private String getPestControlGuide() {
        return "Monitor plants regularly for signs of pests. Use integrated pest management: encourage beneficial insects, use organic treatments first, and apply chemicals only when necessary.";
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.fab_scan_plant) {
            openCamera();
        } else if (id == R.id.card_watering) {
            showCategoryGuide("Watering");
        } else if (id == R.id.card_fertilizing) {
            showCategoryGuide("Fertilizing");
        } else if (id == R.id.card_pruning) {
            showCategoryGuide("Pruning");
        } else if (id == R.id.card_pest_control) {
            showCategoryGuide("Pest Control");
        }
    }
    /**
     * Show category-specific guide
     */
    private void showCategoryGuide(String category) {
        // Hide all content cards first
        hideAllContentCards();
        
        // Show the relevant content card and update its text
        switch (category) {
            case "Watering":
                cardWateringContent.setVisibility(View.VISIBLE);
                tvWateringContent.setText(getWateringGuide());
                break;
            case "Fertilizing":
                cardFertilizingContent.setVisibility(View.VISIBLE);
                tvFertilizingContent.setText(getFertilizingGuide());
                break;
            case "Pruning":
                cardPruningContent.setVisibility(View.VISIBLE);
                tvPruningContent.setText(getPruningGuide());
                break;
            case "Pest Control":
                cardPestControlContent.setVisibility(View.VISIBLE);
                tvPestControlContent.setText(getPestControlGuide());
                break;
        }
    }
    
    /**
     * Hide all content cards
     */
    private void hideAllContentCards() {
        if (cardWateringContent != null) cardWateringContent.setVisibility(View.GONE);
        if (cardFertilizingContent != null) cardFertilizingContent.setVisibility(View.GONE);
        if (cardPruningContent != null) cardPruningContent.setVisibility(View.GONE);
        if (cardPestControlContent != null) cardPestControlContent.setVisibility(View.GONE);
    }



    /**
     * Open camera for plant scanning
     */
    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
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
}