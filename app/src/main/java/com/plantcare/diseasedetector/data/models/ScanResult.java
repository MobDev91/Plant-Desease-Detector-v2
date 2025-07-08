package com.plantcare.diseasedetector.data.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * Entity class for storing plant scan results in the database
 */
@Entity(tableName = "scan_results")
public class ScanResult {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "predicted_class")
    private String predictedClass;

    @ColumnInfo(name = "predicted_index")
    private int predictedIndex;

    @ColumnInfo(name = "confidence")
    private float confidence;

    @ColumnInfo(name = "scan_date")
    private Date scanDate;

    @ColumnInfo(name = "plant_name")
    private String plantName;

    @ColumnInfo(name = "disease_name")
    private String diseaseName;

    @ColumnInfo(name = "is_healthy")
    private boolean isHealthy;

    @ColumnInfo(name = "location")
    private String location;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "treatment_applied")
    private String treatmentApplied;

    @ColumnInfo(name = "follow_up_date")
    private Date followUpDate;

    // Constructors
    public ScanResult() {
        this.scanDate = new Date();
    }

    public ScanResult(String imagePath, String predictedClass, int predictedIndex,
                      float confidence, String plantName, String diseaseName, boolean isHealthy) {
        this.imagePath = imagePath;
        this.predictedClass = predictedClass;
        this.predictedIndex = predictedIndex;
        this.confidence = confidence;
        this.plantName = plantName;
        this.diseaseName = diseaseName;
        this.isHealthy = isHealthy;
        this.scanDate = new Date();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public void setPredictedClass(String predictedClass) {
        this.predictedClass = predictedClass;
    }

    public int getPredictedIndex() {
        return predictedIndex;
    }

    public void setPredictedIndex(int predictedIndex) {
        this.predictedIndex = predictedIndex;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public void setScanDate(Date scanDate) {
        this.scanDate = scanDate;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTreatmentApplied() {
        return treatmentApplied;
    }

    public void setTreatmentApplied(String treatmentApplied) {
        this.treatmentApplied = treatmentApplied;
    }

    public Date getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(Date followUpDate) {
        this.followUpDate = followUpDate;
    }

    // Utility methods

    /**
     * Get formatted confidence percentage
     */
    public String getConfidencePercentage() {
        return String.format("%.1f%%", confidence * 100);
    }

    /**
     * Get display name for the scan result
     */
    public String getDisplayName() {
        if (plantName != null && !plantName.isEmpty()) {
            return plantName;
        }
        // Extract plant name from predicted class
        if (predictedClass != null && predictedClass.contains("___")) {
            String[] parts = predictedClass.split("___");
            if (parts.length > 0) {
                return parts[0].replace("_", " ");
            }
        }
        return "Unknown Plant";
    }

    /**
     * Get health status text
     */
    public String getHealthStatusText() {
        if (isHealthy) {
            return "Healthy";
        } else if (diseaseName != null && !diseaseName.isEmpty()) {
            return diseaseName;
        } else {
            // Extract disease name from predicted class
            if (predictedClass != null && predictedClass.contains("___")) {
                String[] parts = predictedClass.split("___");
                if (parts.length > 1) {
                    return parts[1].replace("_", " ");
                }
            }
            return "Disease Detected";
        }
    }

    /**
     * Check if this scan needs follow-up
     */
    public boolean needsFollowUp() {
        return !isHealthy && followUpDate != null && followUpDate.after(new Date());
    }

    /**
     * Get severity level based on confidence and health status
     */
    public SeverityLevel getSeverityLevel() {
        if (isHealthy) {
            return SeverityLevel.HEALTHY;
        }

        if (confidence >= 0.8f) {
            return SeverityLevel.HIGH;
        } else if (confidence >= 0.6f) {
            return SeverityLevel.MEDIUM;
        } else {
            return SeverityLevel.LOW;
        }
    }

    /**
     * Enum for severity levels
     */
    public enum SeverityLevel {
        HEALTHY,
        LOW,
        MEDIUM,
        HIGH
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "id=" + id +
                ", plantName='" + plantName + '\'' +
                ", predictedClass='" + predictedClass + '\'' +
                ", confidence=" + confidence +
                ", isHealthy=" + isHealthy +
                ", scanDate=" + scanDate +
                '}';
    }
}