package com.plantcare.diseasedetector.ui.history;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.plantcare.diseasedetector.R;

/**
 * Dialog fragment for filtering scan results in History Activity
 * Provides comprehensive filtering options for better data organization
 */
public class FilterDialogFragment extends DialogFragment {

    // Filter criteria container
    public static class FilterCriteria {
        // Health status filters
        public boolean showHealthy = true;
        public boolean showDiseased = true;

        // Time period filters
        public boolean filterByTime = false;
        public TimePeriod timePeriod = TimePeriod.ALL_TIME;

        // Confidence level filter
        public boolean filterByConfidence = false;
        public float minConfidence = 0.0f;
        public float maxConfidence = 1.0f;

        // Plant type filter
        public boolean filterByPlantType = false;
        public String selectedPlantType = "";

        // Sorting options
        public SortOption sortBy = SortOption.DATE_DESC;

        // Disease severity filter
        public boolean filterBySeverity = false;
        public SeverityLevel severityLevel = SeverityLevel.ALL;

        public enum TimePeriod {
            ALL_TIME("All Time"),
            TODAY("Today"),
            THIS_WEEK("This Week"),
            THIS_MONTH("This Month"),
            LAST_30_DAYS("Last 30 Days");

            public final String displayName;
            TimePeriod(String displayName) { this.displayName = displayName; }
        }

        public enum SortOption {
            DATE_DESC("Newest First"),
            DATE_ASC("Oldest First"),
            CONFIDENCE_DESC("High Confidence First"),
            CONFIDENCE_ASC("Low Confidence First"),
            PLANT_NAME("Plant Name A-Z"),
            HEALTH_STATUS("Health Status");

            public final String displayName;
            SortOption(String displayName) { this.displayName = displayName; }
        }

        public enum SeverityLevel {
            ALL("All Severities"),
            HEALTHY("Healthy Only"),
            LOW("Low Severity"),
            MEDIUM("Medium Severity"),
            HIGH("High Severity");

            public final String displayName;
            SeverityLevel(String displayName) { this.displayName = displayName; }
        }
    }

    // Interface for filter results
    public interface FilterListener {
        void onFiltersApplied(FilterCriteria criteria);
        void onFiltersReset();
    }

    private FilterListener listener;
    private FilterCriteria currentCriteria;

    // UI Components
    private CheckBox cbShowHealthy, cbShowDiseased;
    private CheckBox cbFilterByTime, cbFilterByConfidence, cbFilterByPlantType, cbFilterBySeverity;
    private ChipGroup chipGroupTimePeriod, chipGroupPlantTypes;
    private RadioGroup rgSortOptions;
    private SeekBar seekBarMinConfidence, seekBarMaxConfidence;
    private TextView tvMinConfidence, tvMaxConfidence;
    private TextView tvActiveFiltersCount;

    public static FilterDialogFragment newInstance(FilterCriteria currentCriteria) {
        FilterDialogFragment fragment = new FilterDialogFragment();
        fragment.currentCriteria = currentCriteria != null ? currentCriteria : new FilterCriteria();
        return fragment;
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filter, null);

        initializeViews(view);
        populateCurrentValues();
        setupListeners();

        builder.setView(view)
                .setTitle("Filter Scans")
                .setPositiveButton("Apply", (dialog, id) -> applyFilters())
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss())
                .setNeutralButton("Reset", (dialog, id) -> resetFilters());

        return builder.create();
    }

    private void initializeViews(View view) {
        // Health status checkboxes
        cbShowHealthy = view.findViewById(R.id.cb_show_healthy);
        cbShowDiseased = view.findViewById(R.id.cb_show_diseased);

        // Filter enable checkboxes
        cbFilterByTime = view.findViewById(R.id.cb_filter_by_time);
        cbFilterByConfidence = view.findViewById(R.id.cb_filter_by_confidence);
        cbFilterByPlantType = view.findViewById(R.id.cb_filter_by_plant_type);
        cbFilterBySeverity = view.findViewById(R.id.cb_filter_by_severity);

        // Filter options
        chipGroupTimePeriod = view.findViewById(R.id.chip_group_time_period);
        chipGroupPlantTypes = view.findViewById(R.id.chip_group_plant_types);
        rgSortOptions = view.findViewById(R.id.rg_sort_options);

        // Confidence sliders
        seekBarMinConfidence = view.findViewById(R.id.seekbar_min_confidence);
        seekBarMaxConfidence = view.findViewById(R.id.seekbar_max_confidence);
        tvMinConfidence = view.findViewById(R.id.tv_min_confidence);
        tvMaxConfidence = view.findViewById(R.id.tv_max_confidence);

        // Active filters indicator
        tvActiveFiltersCount = view.findViewById(R.id.tv_active_filters_count);

        setupTimePeriodChips();
        setupPlantTypeChips();
        setupSortRadioButtons();
    }

    private void setupTimePeriodChips() {
        chipGroupTimePeriod.removeAllViews();

        for (FilterCriteria.TimePeriod period : FilterCriteria.TimePeriod.values()) {
            Chip chip = new Chip(requireContext());
            chip.setText(period.displayName);
            chip.setCheckable(true);
            chip.setTag(period);
            chipGroupTimePeriod.addView(chip);
        }
    }

    private void setupPlantTypeChips() {
        chipGroupPlantTypes.removeAllViews();

        // Common plant types (you can make this dynamic based on database)
        String[] plantTypes = {
                "Apple", "Tomato", "Potato", "Corn", "Grape",
                "Cherry", "Peach", "Pepper", "Strawberry", "Orange"
        };

        for (String plantType : plantTypes) {
            Chip chip = new Chip(requireContext());
            chip.setText(plantType);
            chip.setCheckable(true);
            chip.setTag(plantType);
            chipGroupPlantTypes.addView(chip);
        }
    }

    private void setupSortRadioButtons() {
        rgSortOptions.removeAllViews();

        for (FilterCriteria.SortOption option : FilterCriteria.SortOption.values()) {
            RadioButton radioButton = new RadioButton(requireContext());
            radioButton.setText(option.displayName);
            radioButton.setTag(option);
            rgSortOptions.addView(radioButton);
        }
    }

    private void populateCurrentValues() {
        if (currentCriteria == null) return;

        // Health status
        cbShowHealthy.setChecked(currentCriteria.showHealthy);
        cbShowDiseased.setChecked(currentCriteria.showDiseased);

        // Filter toggles
        cbFilterByTime.setChecked(currentCriteria.filterByTime);
        cbFilterByConfidence.setChecked(currentCriteria.filterByConfidence);
        cbFilterByPlantType.setChecked(currentCriteria.filterByPlantType);
        cbFilterBySeverity.setChecked(currentCriteria.filterBySeverity);

        // Time period selection
        if (currentCriteria.filterByTime) {
            for (int i = 0; i < chipGroupTimePeriod.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupTimePeriod.getChildAt(i);
                if (chip.getTag() == currentCriteria.timePeriod) {
                    chip.setChecked(true);
                    break;
                }
            }
        }

        // Plant type selection
        if (currentCriteria.filterByPlantType && !currentCriteria.selectedPlantType.isEmpty()) {
            for (int i = 0; i < chipGroupPlantTypes.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupPlantTypes.getChildAt(i);
                if (currentCriteria.selectedPlantType.equals(chip.getTag())) {
                    chip.setChecked(true);
                    break;
                }
            }
        }

        // Sort option
        for (int i = 0; i < rgSortOptions.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) rgSortOptions.getChildAt(i);
            if (radioButton.getTag() == currentCriteria.sortBy) {
                radioButton.setChecked(true);
                break;
            }
        }

        // Confidence range
        seekBarMinConfidence.setProgress((int) (currentCriteria.minConfidence * 100));
        seekBarMaxConfidence.setProgress((int) (currentCriteria.maxConfidence * 100));
        updateConfidenceLabels();

        updateActiveFiltersCount();
    }

    private void setupListeners() {
        // Confidence seekbars
        seekBarMinConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Ensure min doesn't exceed max
                    if (progress > seekBarMaxConfidence.getProgress()) {
                        seekBar.setProgress(seekBarMaxConfidence.getProgress());
                    }
                    updateConfidenceLabels();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarMaxConfidence.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Ensure max doesn't go below min
                    if (progress < seekBarMinConfidence.getProgress()) {
                        seekBar.setProgress(seekBarMinConfidence.getProgress());
                    }
                    updateConfidenceLabels();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Filter enable/disable listeners
        cbFilterByTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chipGroupTimePeriod.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateActiveFiltersCount();
        });

        cbFilterByConfidence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            seekBarMinConfidence.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            seekBarMaxConfidence.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            tvMinConfidence.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            tvMaxConfidence.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateActiveFiltersCount();
        });

        cbFilterByPlantType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chipGroupPlantTypes.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateActiveFiltersCount();
        });

        // Chip selection listeners
        chipGroupTimePeriod.setOnCheckedStateChangeListener((group, checkedIds) -> updateActiveFiltersCount());
        chipGroupPlantTypes.setOnCheckedStateChangeListener((group, checkedIds) -> updateActiveFiltersCount());

        // Health status change listeners
        cbShowHealthy.setOnCheckedChangeListener((buttonView, isChecked) -> updateActiveFiltersCount());
        cbShowDiseased.setOnCheckedChangeListener((buttonView, isChecked) -> updateActiveFiltersCount());
    }

    private void updateConfidenceLabels() {
        float minConf = seekBarMinConfidence.getProgress() / 100.0f;
        float maxConf = seekBarMaxConfidence.getProgress() / 100.0f;

        tvMinConfidence.setText(String.format("Min: %.0f%%", minConf * 100));
        tvMaxConfidence.setText(String.format("Max: %.0f%%", maxConf * 100));
    }

    private void updateActiveFiltersCount() {
        int activeCount = 0;

        // Count health status filters
        if (!cbShowHealthy.isChecked() || !cbShowDiseased.isChecked()) {
            activeCount++;
        }

        // Count time filter
        if (cbFilterByTime.isChecked() && chipGroupTimePeriod.getCheckedChipIds().size() > 0) {
            activeCount++;
        }

        // Count confidence filter
        if (cbFilterByConfidence.isChecked()) {
            float min = seekBarMinConfidence.getProgress() / 100.0f;
            float max = seekBarMaxConfidence.getProgress() / 100.0f;
            if (min > 0.0f || max < 1.0f) {
                activeCount++;
            }
        }

        // Count plant type filter
        if (cbFilterByPlantType.isChecked() && chipGroupPlantTypes.getCheckedChipIds().size() > 0) {
            activeCount++;
        }

        // Count severity filter
        if (cbFilterBySeverity.isChecked()) {
            activeCount++;
        }

        // Update display
        if (activeCount == 0) {
            tvActiveFiltersCount.setText("No active filters");
            tvActiveFiltersCount.setTextColor(getResources().getColor(R.color.gray_medium, null));
        } else {
            tvActiveFiltersCount.setText(activeCount + " active filter" + (activeCount > 1 ? "s" : ""));
            tvActiveFiltersCount.setTextColor(getResources().getColor(R.color.green_primary, null));
        }
    }

    private void applyFilters() {
        FilterCriteria criteria = new FilterCriteria();

        // Health status
        criteria.showHealthy = cbShowHealthy.isChecked();
        criteria.showDiseased = cbShowDiseased.isChecked();

        // Time filter
        criteria.filterByTime = cbFilterByTime.isChecked();
        if (criteria.filterByTime) {
            for (int chipId : chipGroupTimePeriod.getCheckedChipIds()) {
                Chip chip = chipGroupTimePeriod.findViewById(chipId);
                if (chip != null) {
                    criteria.timePeriod = (FilterCriteria.TimePeriod) chip.getTag();
                    break;
                }
            }
        }

        // Confidence filter
        criteria.filterByConfidence = cbFilterByConfidence.isChecked();
        if (criteria.filterByConfidence) {
            criteria.minConfidence = seekBarMinConfidence.getProgress() / 100.0f;
            criteria.maxConfidence = seekBarMaxConfidence.getProgress() / 100.0f;
        }

        // Plant type filter
        criteria.filterByPlantType = cbFilterByPlantType.isChecked();
        if (criteria.filterByPlantType) {
            for (int chipId : chipGroupPlantTypes.getCheckedChipIds()) {
                Chip chip = chipGroupPlantTypes.findViewById(chipId);
                if (chip != null) {
                    criteria.selectedPlantType = (String) chip.getTag();
                    break;
                }
            }
        }

        // Sort option
        for (int i = 0; i < rgSortOptions.getChildCount(); i++) {
            RadioButton radioButton = (RadioButton) rgSortOptions.getChildAt(i);
            if (radioButton.isChecked()) {
                criteria.sortBy = (FilterCriteria.SortOption) radioButton.getTag();
                break;
            }
        }

        // Severity filter
        criteria.filterBySeverity = cbFilterBySeverity.isChecked();

        if (listener != null) {
            listener.onFiltersApplied(criteria);
        }

        dismiss();
    }

    private void resetFilters() {
        if (listener != null) {
            listener.onFiltersReset();
        }
        dismiss();
    }

    /**
     * Helper method to get user-friendly filter summary
     */
    public static String getFilterSummary(FilterCriteria criteria) {
        if (criteria == null) return "No filters";

        StringBuilder summary = new StringBuilder();
        int filterCount = 0;

        // Health status
        if (!criteria.showHealthy || !criteria.showDiseased) {
            if (criteria.showHealthy && !criteria.showDiseased) {
                summary.append("Healthy only");
            } else if (!criteria.showHealthy && criteria.showDiseased) {
                summary.append("Diseased only");
            }
            filterCount++;
        }

        // Time period
        if (criteria.filterByTime) {
            if (filterCount > 0) summary.append(", ");
            summary.append(criteria.timePeriod.displayName);
            filterCount++;
        }

        // Confidence
        if (criteria.filterByConfidence) {
            if (filterCount > 0) summary.append(", ");
            summary.append(String.format("%.0f%%-%.0f%% confidence",
                    criteria.minConfidence * 100, criteria.maxConfidence * 100));
            filterCount++;
        }

        // Plant type
        if (criteria.filterByPlantType && !criteria.selectedPlantType.isEmpty()) {
            if (filterCount > 0) summary.append(", ");
            summary.append(criteria.selectedPlantType + " plants");
            filterCount++;
        }

        return filterCount == 0 ? "No filters" : summary.toString();
    }

    /**
     * Check if criteria has any active filters
     */
    public static boolean hasActiveFilters(FilterCriteria criteria) {
        if (criteria == null) return false;

        return (!criteria.showHealthy || !criteria.showDiseased) ||
                criteria.filterByTime ||
                criteria.filterByConfidence ||
                criteria.filterByPlantType ||
                criteria.filterBySeverity;
    }
}