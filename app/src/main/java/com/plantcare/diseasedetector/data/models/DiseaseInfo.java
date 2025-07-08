package com.plantcare.diseasedetector.data.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

/**
 * Entity class for storing disease information in the database
 */
@Entity(tableName = "disease_info")
public class DiseaseInfo {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "disease_name")
    private String diseaseName;

    @ColumnInfo(name = "plant_type")
    private String plantType;

    @ColumnInfo(name = "scientific_name")
    private String scientificName;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "symptoms")
    private String symptoms;

    @ColumnInfo(name = "causes")
    private String causes;

    @ColumnInfo(name = "prevention_tips")
    private String preventionTips;

    @ColumnInfo(name = "treatment_options")
    private String treatmentOptions;

    @ColumnInfo(name = "severity_level")
    private String severityLevel;

    @ColumnInfo(name = "affected_parts")
    private String affectedParts;

    @ColumnInfo(name = "environmental_factors")
    private String environmentalFactors;

    @ColumnInfo(name = "spread_method")
    private String spreadMethod;

    @ColumnInfo(name = "optimal_conditions")
    private String optimalConditions;

    @ColumnInfo(name = "recovery_time")
    private String recoveryTime;

    @ColumnInfo(name = "image_urls")
    private String imageUrls;

    @ColumnInfo(name = "is_common")
    private boolean isCommon;

    @ColumnInfo(name = "is_treatable")
    private boolean isTreatable;

    // Constructors
    public DiseaseInfo() {}

    public DiseaseInfo(String diseaseName, String plantType, String description) {
        this.diseaseName = diseaseName;
        this.plantType = plantType;
        this.description = description;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }

    public String getPreventionTips() {
        return preventionTips;
    }

    public void setPreventionTips(String preventionTips) {
        this.preventionTips = preventionTips;
    }

    public String getTreatmentOptions() {
        return treatmentOptions;
    }

    public void setTreatmentOptions(String treatmentOptions) {
        this.treatmentOptions = treatmentOptions;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public String getAffectedParts() {
        return affectedParts;
    }

    public void setAffectedParts(String affectedParts) {
        this.affectedParts = affectedParts;
    }

    public String getEnvironmentalFactors() {
        return environmentalFactors;
    }

    public void setEnvironmentalFactors(String environmentalFactors) {
        this.environmentalFactors = environmentalFactors;
    }

    public String getSpreadMethod() {
        return spreadMethod;
    }

    public void setSpreadMethod(String spreadMethod) {
        this.spreadMethod = spreadMethod;
    }

    public String getOptimalConditions() {
        return optimalConditions;
    }

    public void setOptimalConditions(String optimalConditions) {
        this.optimalConditions = optimalConditions;
    }

    public String getRecoveryTime() {
        return recoveryTime;
    }

    public void setRecoveryTime(String recoveryTime) {
        this.recoveryTime = recoveryTime;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public boolean isCommon() {
        return isCommon;
    }

    public void setCommon(boolean common) {
        isCommon = common;
    }

    public boolean isTreatable() {
        return isTreatable;
    }

    public void setTreatable(boolean treatable) {
        isTreatable = treatable;
    }

    // Utility methods

    /**
     * Get symptoms as a list
     */
    public String[] getSymptomsArray() {
        if (symptoms != null && !symptoms.isEmpty()) {
            return symptoms.split("\\|");
        }
        return new String[0];
    }

    /**
     * Get treatment options as a list
     */
    public String[] getTreatmentArray() {
        if (treatmentOptions != null && !treatmentOptions.isEmpty()) {
            return treatmentOptions.split("\\|");
        }
        return new String[0];
    }

    /**
     * Get prevention tips as a list
     */
    public String[] getPreventionArray() {
        if (preventionTips != null && !preventionTips.isEmpty()) {
            return preventionTips.split("\\|");
        }
        return new String[0];
    }

    /**
     * Get affected parts as a list
     */
    public String[] getAffectedPartsArray() {
        if (affectedParts != null && !affectedParts.isEmpty()) {
            return affectedParts.split("\\|");
        }
        return new String[0];
    }

    /**
     * Get disease severity as enum
     */
    public Severity getSeverity() {
        if (severityLevel == null) return Severity.MEDIUM;

        switch (severityLevel.toUpperCase()) {
            case "HIGH":
            case "SEVERE":
                return Severity.HIGH;
            case "LOW":
            case "MILD":
                return Severity.LOW;
            default:
                return Severity.MEDIUM;
        }
    }

    /**
     * Get formatted disease name for display
     */
    public String getFormattedName() {
        if (diseaseName == null) return "Unknown Disease";
        return diseaseName.replace("_", " ").trim();
    }

    /**
     * Get display color based on severity
     */
    public int getSeverityColor() {
        switch (getSeverity()) {
            case HIGH:
                return android.R.color.holo_red_dark;
            case MEDIUM:
                return android.R.color.holo_orange_dark;
            case LOW:
                return android.R.color.holo_green_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    /**
     * Check if disease matches the predicted class
     */
    public boolean matchesPrediction(String predictedClass) {
        if (predictedClass == null || diseaseName == null) return false;

        String normalizedPrediction = predictedClass.toLowerCase()
                .replace("___", " ")
                .replace("_", " ");
        String normalizedDisease = diseaseName.toLowerCase()
                .replace("_", " ");

        return normalizedPrediction.contains(normalizedDisease) ||
                normalizedDisease.contains(normalizedPrediction);
    }

    /**
     * Severity enum
     */
    public enum Severity {
        LOW("Low", "Mild symptoms, easy to treat"),
        MEDIUM("Medium", "Moderate symptoms, requires attention"),
        HIGH("High", "Severe symptoms, immediate action needed");

        private final String displayName;
        private final String description;

        Severity(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "DiseaseInfo{" +
                "id=" + id +
                ", diseaseName='" + diseaseName + '\'' +
                ", plantType='" + plantType + '\'' +
                ", severity='" + severityLevel + '\'' +
                ", treatable=" + isTreatable +
                '}';
    }
}