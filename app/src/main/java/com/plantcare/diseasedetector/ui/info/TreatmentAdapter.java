package com.plantcare.diseasedetector.ui.info;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.plantcare.diseasedetector.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for displaying treatment steps and recommendations
 */
public class TreatmentAdapter extends RecyclerView.Adapter<TreatmentAdapter.TreatmentViewHolder> {

    private Context context;
    private List<TreatmentStep> treatmentSteps;
    private OnTreatmentClickListener listener;

    public TreatmentAdapter(Context context) {
        this.context = context;
        this.treatmentSteps = new ArrayList<>();
    }

    public void setTreatmentSteps(List<TreatmentStep> steps) {
        this.treatmentSteps.clear();
        if (steps != null) {
            this.treatmentSteps.addAll(steps);
        }
        notifyDataSetChanged();
    }

    public void setOnTreatmentClickListener(OnTreatmentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TreatmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_treatment_step, parent, false);
        return new TreatmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TreatmentViewHolder holder, int position) {
        TreatmentStep step = treatmentSteps.get(position);
        holder.bind(step, position + 1);
    }

    @Override
    public int getItemCount() {
        return treatmentSteps.size();
    }

    /**
     * ViewHolder class for treatment steps
     */
    class TreatmentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private MaterialCardView cardTreatment;
        private ImageView ivStepIcon, ivTreatmentType;
        private TextView tvStepNumber, tvTreatmentTitle, tvTreatmentDescription;
        private TextView tvDuration, tvDifficulty, tvEffectiveness;
        private View layoutDetails;

        public TreatmentViewHolder(@NonNull View itemView) {
            super(itemView);

            cardTreatment = itemView.findViewById(R.id.card_treatment);
            ivStepIcon = itemView.findViewById(R.id.iv_step_icon);
            ivTreatmentType = itemView.findViewById(R.id.iv_treatment_type);
            tvStepNumber = itemView.findViewById(R.id.tv_step_number);
            tvTreatmentTitle = itemView.findViewById(R.id.tv_treatment_title);
            tvTreatmentDescription = itemView.findViewById(R.id.tv_treatment_description);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvDifficulty = itemView.findViewById(R.id.tv_difficulty);
            tvEffectiveness = itemView.findViewById(R.id.tv_effectiveness);
            layoutDetails = itemView.findViewById(R.id.layout_details);

            cardTreatment.setOnClickListener(this);
        }

        public void bind(TreatmentStep step, int stepNumber) {
            // Step number
            tvStepNumber.setText(String.valueOf(stepNumber));

            // Treatment title and description
            tvTreatmentTitle.setText(step.getTitle());
            tvTreatmentDescription.setText(step.getDescription());

            // Duration
            if (step.getDuration() != null && !step.getDuration().isEmpty()) {
                tvDuration.setText(step.getDuration());
                tvDuration.setVisibility(View.VISIBLE);
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            // Difficulty
            setupDifficulty(step.getDifficulty());

            // Effectiveness
            setupEffectiveness(step.getEffectiveness());

            // Step icon based on type
            setupStepIcon(step.getType());

            // Treatment type icon
            setupTreatmentTypeIcon(step.getType());

            // Card styling based on priority
            setupCardStyling(step.getPriority());
        }

        private void setupDifficulty(TreatmentStep.Difficulty difficulty) {
            if (difficulty == null) {
                tvDifficulty.setVisibility(View.GONE);
                return;
            }

            tvDifficulty.setVisibility(View.VISIBLE);
            tvDifficulty.setText(difficulty.getDisplayName());

            int colorRes;
            switch (difficulty) {
                case EASY:
                    colorRes = R.color.healthy_green;
                    break;
                case MEDIUM:
                    colorRes = R.color.warning_orange;
                    break;
                case HARD:
                    colorRes = R.color.disease_red;
                    break;
                default:
                    colorRes = R.color.gray_medium;
            }

            tvDifficulty.setTextColor(ContextCompat.getColor(context, colorRes));
        }

        private void setupEffectiveness(TreatmentStep.Effectiveness effectiveness) {
            if (effectiveness == null) {
                tvEffectiveness.setVisibility(View.GONE);
                return;
            }

            tvEffectiveness.setVisibility(View.VISIBLE);
            tvEffectiveness.setText(effectiveness.getDisplayName());

            int colorRes;
            switch (effectiveness) {
                case HIGH:
                    colorRes = R.color.healthy_green;
                    break;
                case MEDIUM:
                    colorRes = R.color.warning_orange;
                    break;
                case LOW:
                    colorRes = R.color.disease_red;
                    break;
                default:
                    colorRes = R.color.gray_medium;
            }

            tvEffectiveness.setTextColor(ContextCompat.getColor(context, colorRes));
        }

        private void setupStepIcon(TreatmentStep.TreatmentType type) {
            int iconRes;
            switch (type) {
                case CHEMICAL:
                    iconRes = R.drawable.ic_medical;
                    break;
                case ORGANIC:
                    iconRes = R.drawable.ic_plant_care;
                    break;
                case CULTURAL:
                    iconRes = R.drawable.ic_tips;
                    break;
                case BIOLOGICAL:
                    iconRes = R.drawable.ic_health_check;
                    break;
                case PREVENTIVE:
                    iconRes = R.drawable.ic_tips;
                    break;
                default:
                    iconRes = R.drawable.ic_info;
            }

            ivStepIcon.setImageResource(iconRes);
        }

        private void setupTreatmentTypeIcon(TreatmentStep.TreatmentType type) {
            int iconRes;
            int colorRes;

            switch (type) {
                case CHEMICAL:
                    iconRes = R.drawable.ic_medical;
                    colorRes = R.color.disease_red;
                    break;
                case ORGANIC:
                    iconRes = R.drawable.ic_plant_care;
                    colorRes = R.color.healthy_green;
                    break;
                case CULTURAL:
                    iconRes = R.drawable.ic_tips;
                    colorRes = R.color.green_primary;
                    break;
                case BIOLOGICAL:
                    iconRes = R.drawable.ic_health_check;
                    colorRes = R.color.healthy_green;
                    break;
                case PREVENTIVE:
                    iconRes = R.drawable.ic_tips;
                    colorRes = R.color.warning_orange;
                    break;
                default:
                    iconRes = R.drawable.ic_info;
                    colorRes = R.color.gray_medium;
            }

            ivTreatmentType.setImageResource(iconRes);
            ivTreatmentType.setColorFilter(ContextCompat.getColor(context, colorRes));
        }

        private void setupCardStyling(TreatmentStep.Priority priority) {
            if (priority == null) return;

            int strokeColorRes;
            int strokeWidth = 0;

            switch (priority) {
                case HIGH:
                    strokeColorRes = R.color.disease_red;
                    strokeWidth = 4;
                    break;
                case MEDIUM:
                    strokeColorRes = R.color.warning_orange;
                    strokeWidth = 2;
                    break;
                case LOW:
                    strokeColorRes = R.color.gray_medium;
                    strokeWidth = 1;
                    break;
                default:
                    strokeColorRes = R.color.gray_light;
                    strokeWidth = 1;
            }

            cardTreatment.setStrokeColor(ContextCompat.getColor(context, strokeColorRes));
            cardTreatment.setStrokeWidth(strokeWidth);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                TreatmentStep step = treatmentSteps.get(position);
                listener.onTreatmentClick(step, position);
            }
        }
    }

    /**
     * Treatment Step data class
     */
    public static class TreatmentStep {
        private String title;
        private String description;
        private String duration;
        private String materials;
        private String instructions;
        private TreatmentType type;
        private Difficulty difficulty;
        private Effectiveness effectiveness;
        private Priority priority;
        private boolean isRecommended;
        private String notes;

        public TreatmentStep() {}

        public TreatmentStep(String title, String description, TreatmentType type) {
            this.title = title;
            this.description = description;
            this.type = type;
        }

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public String getMaterials() { return materials; }
        public void setMaterials(String materials) { this.materials = materials; }

        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }

        public TreatmentType getType() { return type; }
        public void setType(TreatmentType type) { this.type = type; }

        public Difficulty getDifficulty() { return difficulty; }
        public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

        public Effectiveness getEffectiveness() { return effectiveness; }
        public void setEffectiveness(Effectiveness effectiveness) { this.effectiveness = effectiveness; }

        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }

        public boolean isRecommended() { return isRecommended; }
        public void setRecommended(boolean recommended) { isRecommended = recommended; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        // Enums
        public enum TreatmentType {
            CHEMICAL("Chemical", "Chemical pesticides and fungicides"),
            ORGANIC("Organic", "Natural and organic treatments"),
            CULTURAL("Cultural", "Cultural and management practices"),
            BIOLOGICAL("Biological", "Biological control methods"),
            PREVENTIVE("Preventive", "Prevention strategies");

            private final String displayName;
            private final String description;

            TreatmentType(String displayName, String description) {
                this.displayName = displayName;
                this.description = description;
            }

            public String getDisplayName() { return displayName; }
            public String getDescription() { return description; }
        }

        public enum Difficulty {
            EASY("Easy", "Simple to apply"),
            MEDIUM("Medium", "Requires some experience"),
            HARD("Hard", "Professional application recommended");

            private final String displayName;
            private final String description;

            Difficulty(String displayName, String description) {
                this.displayName = displayName;
                this.description = description;
            }

            public String getDisplayName() { return displayName; }
            public String getDescription() { return description; }
        }

        public enum Effectiveness {
            HIGH("High", "Very effective treatment"),
            MEDIUM("Medium", "Moderately effective"),
            LOW("Low", "Limited effectiveness");

            private final String displayName;
            private final String description;

            Effectiveness(String displayName, String description) {
                this.displayName = displayName;
                this.description = description;
            }

            public String getDisplayName() { return displayName; }
            public String getDescription() { return description; }
        }

        public enum Priority {
            HIGH("High Priority", "Immediate action required"),
            MEDIUM("Medium Priority", "Important but not urgent"),
            LOW("Low Priority", "Optional treatment");

            private final String displayName;
            private final String description;

            Priority(String displayName, String description) {
                this.displayName = displayName;
                this.description = description;
            }

            public String getDisplayName() { return displayName; }
            public String getDescription() { return description; }
        }
    }

    /**
     * Interface for handling treatment clicks
     */
    public interface OnTreatmentClickListener {
        void onTreatmentClick(TreatmentStep step, int position);
    }

    /**
     * Utility method to create treatment steps from disease info
     */
    public static List<TreatmentStep> createTreatmentStepsFromDiseaseInfo(String treatmentOptions) {
        List<TreatmentStep> steps = new ArrayList<>();

        if (treatmentOptions == null || treatmentOptions.isEmpty()) {
            return steps;
        }

        String[] treatments = treatmentOptions.split("\\|");
        for (int i = 0; i < treatments.length; i++) {
            String treatment = treatments[i].trim();
            if (!treatment.isEmpty()) {
                TreatmentStep step = new TreatmentStep();
                step.setTitle("Treatment " + (i + 1));
                step.setDescription(treatment);

                // Determine treatment type based on content
                if (treatment.toLowerCase().contains("fungicide") ||
                        treatment.toLowerCase().contains("pesticide") ||
                        treatment.toLowerCase().contains("chemical")) {
                    step.setType(TreatmentStep.TreatmentType.CHEMICAL);
                    step.setDifficulty(TreatmentStep.Difficulty.MEDIUM);
                    step.setEffectiveness(TreatmentStep.Effectiveness.HIGH);
                } else if (treatment.toLowerCase().contains("organic") ||
                        treatment.toLowerCase().contains("natural") ||
                        treatment.toLowerCase().contains("compost")) {
                    step.setType(TreatmentStep.TreatmentType.ORGANIC);
                    step.setDifficulty(TreatmentStep.Difficulty.EASY);
                    step.setEffectiveness(TreatmentStep.Effectiveness.MEDIUM);
                } else if (treatment.toLowerCase().contains("cultural") ||
                        treatment.toLowerCase().contains("management") ||
                        treatment.toLowerCase().contains("spacing")) {
                    step.setType(TreatmentStep.TreatmentType.CULTURAL);
                    step.setDifficulty(TreatmentStep.Difficulty.EASY);
                    step.setEffectiveness(TreatmentStep.Effectiveness.MEDIUM);
                } else {
                    step.setType(TreatmentStep.TreatmentType.CULTURAL);
                    step.setDifficulty(TreatmentStep.Difficulty.MEDIUM);
                    step.setEffectiveness(TreatmentStep.Effectiveness.MEDIUM);
                }

                step.setPriority(TreatmentStep.Priority.MEDIUM);
                steps.add(step);
            }
        }

        return steps;
    }
}