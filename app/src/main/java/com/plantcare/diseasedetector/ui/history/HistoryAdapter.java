package com.plantcare.diseasedetector.ui.history;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.utils.DateUtils;
import com.plantcare.diseasedetector.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adapter for displaying scan history in both list and grid view modes
 * Supports search, filtering, and various user interactions
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    private Context context;
    private List<ScanResult> scanResults;
    private boolean isGridView;
    private OnItemClickListener listener;
    private ExecutorService imageExecutor;

    public HistoryAdapter(Context context, List<ScanResult> scanResults, boolean isGridView) {
        this.context = context;
        this.scanResults = scanResults;
        this.isGridView = isGridView;
        this.imageExecutor = Executors.newFixedThreadPool(3); // For loading images
    }

    /**
     * Interface for handling item interactions
     */
    public interface OnItemClickListener {
        void onItemClick(ScanResult scanResult);
        void onMenuClick(ScanResult scanResult, View view);
        void onShareClick(ScanResult scanResult);
        void onDeleteClick(ScanResult scanResult);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Set grid view mode
     */
    public void setGridView(boolean gridView) {
        if (this.isGridView != gridView) {
            this.isGridView = gridView;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isGridView ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_GRID) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_history_grid, parent, false);
            return new GridViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_history_scan, parent, false);
            return new ListViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ScanResult scanResult = scanResults.get(position);

        if (holder instanceof ListViewHolder) {
            ((ListViewHolder) holder).bind(scanResult);
        } else if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(scanResult);
        }
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    /**
     * List view holder for detailed scan information
     */
    class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView ivPlantImage;
        private TextView tvPlantName, tvHealthStatus, tvScanDate, tvConfidenceText;
        private TextView tvConfidenceBadge, tvSeverityBadge;
        private ProgressBar progressConfidence;
        private ImageButton btnMenu;
        private View viewStatusIndicator, layoutExpandableDetails;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);

            ivPlantImage = itemView.findViewById(R.id.iv_plant_image);
            tvPlantName = itemView.findViewById(R.id.tv_plant_name);
            tvHealthStatus = itemView.findViewById(R.id.tv_health_status);
            tvScanDate = itemView.findViewById(R.id.tv_scan_date);
            tvConfidenceText = itemView.findViewById(R.id.tv_confidence_text);
            tvConfidenceBadge = itemView.findViewById(R.id.tv_confidence_badge);
            tvSeverityBadge = itemView.findViewById(R.id.tv_severity_badge);
            progressConfidence = itemView.findViewById(R.id.progress_confidence);
            btnMenu = itemView.findViewById(R.id.btn_menu);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            layoutExpandableDetails = itemView.findViewById(R.id.layout_expandable_details);

            // Set click listeners
            itemView.setOnClickListener(this);
            btnMenu.setOnClickListener(this);
        }

        public void bind(ScanResult scanResult) {
            // Load plant image
            loadPlantImage(scanResult);

            // Set plant name
            tvPlantName.setText(scanResult.getDisplayName());

            // Set health status
            setHealthStatus(scanResult);

            // Set scan date
            setScanDate(scanResult);

            // Set confidence
            setConfidence(scanResult);

            // Set disease information
            setDiseaseInfo(scanResult);

            // Set severity level
            setSeverityLevel(scanResult);
        }

        private void loadPlantImage(ScanResult scanResult) {
            // Set placeholder first
            ivPlantImage.setImageResource(R.drawable.placeholder_plant);

            if (scanResult.getImagePath() != null && !scanResult.getImagePath().isEmpty()) {
                imageExecutor.execute(() -> {
                    Bitmap thumbnail = ImageUtils.createThumbnail(scanResult.getImagePath(), 200);

                    if (thumbnail != null) {
                        // Update UI on main thread
                        itemView.post(() -> ivPlantImage.setImageBitmap(thumbnail));
                    }
                });
            }
        }

        private void setHealthStatus(ScanResult scanResult) {
            if (scanResult.isHealthy()) {
                tvHealthStatus.setText("Healthy");
                tvHealthStatus.setTextColor(ContextCompat.getColor(context, R.color.healthy_green));
                viewStatusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.healthy_green));
            } else {
                tvHealthStatus.setText("Disease Detected");
                tvHealthStatus.setTextColor(ContextCompat.getColor(context, R.color.disease_red));
                viewStatusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.disease_red));
            }
        }

        private void setScanDate(ScanResult scanResult) {
            if (scanResult.getScanDate() != null) {
                String friendlyDate = DateUtils.getFriendlyDate(scanResult.getScanDate());
                tvScanDate.setText(friendlyDate);
            }
        }

        private void setConfidence(ScanResult scanResult) {
            float confidence = scanResult.getConfidence();
            int confidencePercent = Math.round(confidence * 100);

            tvConfidenceText.setText(confidencePercent + "%");
            tvConfidenceBadge.setText(confidencePercent + "%");
            progressConfidence.setProgress(confidencePercent);

            // Set color based on confidence level
            int colorRes;
            if (confidence >= 0.8f) {
                colorRes = R.color.healthy_green;
            } else if (confidence >= 0.6f) {
                colorRes = R.color.warning_orange;
            } else {
                colorRes = R.color.disease_red;
            }

            progressConfidence.setProgressTintList(
                    ContextCompat.getColorStateList(context, colorRes)
            );
        }

        private void setDiseaseInfo(ScanResult scanResult) {
            // Disease info is shown in health status, no separate field needed
        }

        private void setSeverityLevel(ScanResult scanResult) {
            ScanResult.SeverityLevel severity = scanResult.getSeverityLevel();

            if (severity != ScanResult.SeverityLevel.HEALTHY && !scanResult.isHealthy()) {
                tvSeverityBadge.setVisibility(View.VISIBLE);
                tvSeverityBadge.setText(severity.name());

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
                        colorRes = R.color.gray_light;
                }

                tvSeverityBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, colorRes)
                );
            } else {
                tvSeverityBadge.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION || listener == null) return;

            ScanResult scanResult = scanResults.get(position);
            int id = v.getId();

            if (id == R.id.btn_menu) {
                listener.onMenuClick(scanResult, v);
            } else {
                // Main item click
                listener.onItemClick(scanResult);
            }
        }
    }

    /**
     * Grid view holder for compact scan display
     */
    class GridViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private ImageView ivPlantImage, ivHealthBadge;
        private TextView tvPlantName, tvScanDate, tvConfidence;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);

            ivPlantImage = itemView.findViewById(R.id.iv_plant_image);
            ivHealthBadge = itemView.findViewById(R.id.iv_health_badge);
            tvPlantName = itemView.findViewById(R.id.tv_plant_name);
            tvScanDate = itemView.findViewById(R.id.tv_scan_date);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);

            // Set click listeners
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void bind(ScanResult scanResult) {
            // Load plant image
            loadPlantImage(scanResult);

            // Set plant name (truncated for grid)
            String plantName = scanResult.getDisplayName();
            if (plantName.length() > 15) {
                plantName = plantName.substring(0, 12) + "...";
            }
            tvPlantName.setText(plantName);

            // Set scan date (short format)
            setScanDate(scanResult);

            // Set confidence
            setConfidence(scanResult);

            // Set health badge
            setHealthBadge(scanResult);
        }

        private void loadPlantImage(ScanResult scanResult) {
            // Set placeholder first
            ivPlantImage.setImageResource(R.drawable.placeholder_plant);

            if (scanResult.getImagePath() != null && !scanResult.getImagePath().isEmpty()) {
                imageExecutor.execute(() -> {
                    Bitmap thumbnail = ImageUtils.createThumbnail(scanResult.getImagePath(), 150);

                    if (thumbnail != null) {
                        // Update UI on main thread
                        itemView.post(() -> ivPlantImage.setImageBitmap(thumbnail));
                    }
                });
            }
        }

        private void setScanDate(ScanResult scanResult) {
            if (scanResult.getScanDate() != null) {
                String shortDate = DateUtils.getShortDate(scanResult.getScanDate());
                tvScanDate.setText(shortDate);
            }
        }

        private void setConfidence(ScanResult scanResult) {
            int confidencePercent = Math.round(scanResult.getConfidence() * 100);
            tvConfidence.setText(confidencePercent + "%");

            // Set text color based on confidence
            int colorRes;
            if (scanResult.getConfidence() >= 0.8f) {
                colorRes = R.color.healthy_green;
            } else if (scanResult.getConfidence() >= 0.6f) {
                colorRes = R.color.warning_orange;
            } else {
                colorRes = R.color.disease_red;
            }

            tvConfidence.setTextColor(ContextCompat.getColor(context, colorRes));
        }

        private void setHealthBadge(ScanResult scanResult) {
            if (scanResult.isHealthy()) {
                ivHealthBadge.setImageResource(R.drawable.ic_health_check);
                ivHealthBadge.setColorFilter(ContextCompat.getColor(context, R.color.healthy_green));
            } else {
                ivHealthBadge.setImageResource(R.drawable.ic_warning);
                ivHealthBadge.setColorFilter(ContextCompat.getColor(context, R.color.disease_red));
            }
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(scanResults.get(position));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                listener.onMenuClick(scanResults.get(position), v);
                return true;
            }
            return false;
        }
    }

    /**
     * Get scan result at position
     */
    public ScanResult getScanResult(int position) {
        if (position >= 0 && position < scanResults.size()) {
            return scanResults.get(position);
        }
        return null;
    }

    /**
     * Remove scan result at position
     */
    public void removeScanResult(int position) {
        if (position >= 0 && position < scanResults.size()) {
            scanResults.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, scanResults.size() - position);
        }
    }

    /**
     * Update specific scan result
     */
    public void updateScanResult(int position, ScanResult scanResult) {
        if (position >= 0 && position < scanResults.size()) {
            scanResults.set(position, scanResult);
            notifyItemChanged(position);
        }
    }

    /**
     * Clear all scan results
     */
    public void clearResults() {
        int size = scanResults.size();
        scanResults.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Get total number of healthy scans
     */
    public int getHealthyScansCount() {
        int count = 0;
        for (ScanResult scan : scanResults) {
            if (scan.isHealthy()) count++;
        }
        return count;
    }

    /**
     * Get total number of diseased scans
     */
    public int getDiseasedScansCount() {
        int count = 0;
        for (ScanResult scan : scanResults) {
            if (!scan.isHealthy()) count++;
        }
        return count;
    }

    /**
     * Get average confidence of all scans
     */
    public float getAverageConfidence() {
        if (scanResults.isEmpty()) return 0f;

        float total = 0f;
        for (ScanResult scan : scanResults) {
            total += scan.getConfidence();
        }
        return total / scanResults.size();
    }

    /**
     * Release resources
     */
    public void release() {
        if (imageExecutor != null && !imageExecutor.isShutdown()) {
            imageExecutor.shutdown();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        release();
    }
}