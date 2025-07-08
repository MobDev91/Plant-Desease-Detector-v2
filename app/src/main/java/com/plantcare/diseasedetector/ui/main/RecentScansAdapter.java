package com.plantcare.diseasedetector.ui.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.utils.DateUtils;

import java.io.File;
import java.util.List;

/**
 * RecyclerView Adapter for displaying recent plant scan results
 * Shows plant image, name, health status, confidence, and scan date
 */
public class RecentScansAdapter extends RecyclerView.Adapter<RecentScansAdapter.ViewHolder> {

    private Context context;
    private List<ScanResult> scanResults;
    private OnItemClickListener onItemClickListener;

    /**
     * Interface for handling item click events
     */
    public interface OnItemClickListener {
        void onItemClick(ScanResult scanResult);
    }

    /**
     * Constructor
     */
    public RecentScansAdapter(Context context, List<ScanResult> scanResults) {
        this.context = context;
        this.scanResults = scanResults;
    }

    /**
     * Set item click listener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_scan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult scanResult = scanResults.get(position);
        holder.bind(scanResult);
    }

    @Override
    public int getItemCount() {
        return scanResults != null ? scanResults.size() : 0;
    }

    /**
     * ViewHolder class for recent scan items
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivPlantImage;
        private TextView tvPlantName;
        private TextView tvHealthStatus;
        private TextView tvConfidence;
        private TextView tvScanDate;
        private View viewStatusIndicator;
        private ImageView ivActionArrow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            initializeViews();
            setupClickListener();
        }

        /**
         * Initialize all views
         */
        private void initializeViews() {
            ivPlantImage = itemView.findViewById(R.id.iv_plant_image);
            tvPlantName = itemView.findViewById(R.id.tv_plant_name);
            tvHealthStatus = itemView.findViewById(R.id.tv_health_status);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            tvScanDate = itemView.findViewById(R.id.tv_scan_date);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            ivActionArrow = itemView.findViewById(R.id.iv_action_arrow);
        }

        /**
         * Setup click listener for the item
         */
        private void setupClickListener() {
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(scanResults.get(position));
                    }
                }
            });
        }

        /**
         * Bind data to views
         */
        public void bind(ScanResult scanResult) {
            // Set plant image
            loadPlantImage(scanResult.getImagePath());

            // Set plant name (extract from predicted class)
            String plantName = extractPlantName(scanResult.getPredictedClass());
            tvPlantName.setText(plantName);

            // Set health status and colors
            setHealthStatus(scanResult);

            // Set confidence
            String confidenceText = String.format("Confidence: %.1f%%", scanResult.getConfidence() * 100);
            tvConfidence.setText(confidenceText);

            // Set scan date
            String timeAgo = DateUtils.getTimeAgo(scanResult.getScanDate());
            tvScanDate.setText(timeAgo);
        }

        /**
         * Load plant image from file path
         */
        private void loadPlantImage(String imagePath) {
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        // Load image efficiently
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4; // Scale down to reduce memory usage
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

                        if (bitmap != null) {
                            ivPlantImage.setImageBitmap(bitmap);
                        } else {
                            setDefaultImage();
                        }
                    } else {
                        setDefaultImage();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setDefaultImage();
                }
            } else {
                setDefaultImage();
            }
        }

        /**
         * Set default placeholder image
         */
        private void setDefaultImage() {
            ivPlantImage.setImageResource(R.drawable.placeholder_plant);
        }

        /**
         * Extract plant name from predicted class
         * Example: "Apple___Apple_scab" -> "Apple"
         */
        private String extractPlantName(String predictedClass) {
            if (predictedClass == null || predictedClass.isEmpty()) {
                return "Unknown Plant";
            }

            // Split by "___" and take the first part
            String[] parts = predictedClass.split("___");
            if (parts.length > 0) {
                // Replace underscores with spaces and capitalize
                String plantName = parts[0].replace("_", " ");
                return capitalizeWords(plantName);
            }

            return "Unknown Plant";
        }

        /**
         * Set health status with appropriate colors
         */
        private void setHealthStatus(ScanResult scanResult) {
            String predictedClass = scanResult.getPredictedClass();
            boolean isHealthy = isPlantHealthy(predictedClass);

            if (isHealthy) {
                // Healthy plant
                tvHealthStatus.setText("Healthy");
                tvHealthStatus.setTextColor(ContextCompat.getColor(context, R.color.healthy_green));
                viewStatusIndicator.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.healthy_green)
                );
            } else {
                // Disease detected
                String diseaseName = extractDiseaseName(predictedClass);
                tvHealthStatus.setText(diseaseName);
                tvHealthStatus.setTextColor(ContextCompat.getColor(context, R.color.disease_red));
                viewStatusIndicator.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.disease_red)
                );
            }
        }

        /**
         * Check if plant is healthy based on predicted class
         */
        private boolean isPlantHealthy(String predictedClass) {
            return predictedClass != null &&
                    (predictedClass.toLowerCase().contains("healthy") ||
                            predictedClass.toLowerCase().contains("background"));
        }

        /**
         * Extract disease name from predicted class
         * Example: "Apple___Apple_scab" -> "Apple Scab"
         */
        private String extractDiseaseName(String predictedClass) {
            if (predictedClass == null || predictedClass.isEmpty()) {
                return "Unknown Disease";
            }

            // Split by "___" and take the second part (disease name)
            String[] parts = predictedClass.split("___");
            if (parts.length > 1) {
                String diseaseName = parts[1].replace("_", " ");
                return capitalizeWords(diseaseName);
            }

            // If no "___" found, try to extract meaningful name
            String diseaseName = predictedClass.replace("_", " ");
            return capitalizeWords(diseaseName);
        }

        /**
         * Capitalize first letter of each word
         */
        private String capitalizeWords(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }

            String[] words = text.split(" ");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                if (i > 0) {
                    result.append(" ");
                }

                String word = words[i];
                if (!word.isEmpty()) {
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase());
                    }
                }
            }

            return result.toString();
        }
    }

    /**
     * Update the scan results list
     */
    public void updateScanResults(List<ScanResult> newScanResults) {
        this.scanResults = newScanResults;
        notifyDataSetChanged();
    }

    /**
     * Add a new scan result to the beginning of the list
     */
    public void addScanResult(ScanResult scanResult) {
        if (scanResults != null) {
            scanResults.add(0, scanResult);
            notifyItemInserted(0);
        }
    }

    /**
     * Remove a scan result at specific position
     */
    public void removeScanResult(int position) {
        if (scanResults != null && position >= 0 && position < scanResults.size()) {
            scanResults.remove(position);
            notifyItemRemoved(position);
        }
    }
}