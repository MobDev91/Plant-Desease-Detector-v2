package com.plantcare.diseasedetector.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.plantcare.diseasedetector.data.models.DiseaseInfo;

import java.util.List;

/**
 * Data Access Object (DAO) for DiseaseInfo entity
 * Provides database operations for disease information
 */
@Dao
public interface DiseaseInfoDao {

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a single disease info record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDiseaseInfo(DiseaseInfo diseaseInfo);

    /**
     * Insert multiple disease info records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllDiseaseInfo(List<DiseaseInfo> diseaseInfoList);

    // ===== UPDATE OPERATIONS =====

    /**
     * Update disease info record
     */
    @Update
    int updateDiseaseInfo(DiseaseInfo diseaseInfo);

    // ===== DELETE OPERATIONS =====

    /**
     * Delete a specific disease info record
     */
    @Delete
    int deleteDiseaseInfo(DiseaseInfo diseaseInfo);

    /**
     * Delete disease info by ID
     */
    @Query("DELETE FROM disease_info WHERE id = :diseaseId")
    int deleteDiseaseInfoById(int diseaseId);

    /**
     * Delete all disease info records
     */
    @Query("DELETE FROM disease_info")
    int deleteAllDiseaseInfo();

    // ===== SELECT OPERATIONS =====

    /**
     * Get all disease information
     */
    @Query("SELECT * FROM disease_info ORDER BY disease_name ASC")
    List<DiseaseInfo> getAllDiseaseInfo();

    /**
     * Get disease info by ID
     */
    @Query("SELECT * FROM disease_info WHERE id = :diseaseId")
    DiseaseInfo getDiseaseInfoById(int diseaseId);

    /**
     * Get disease info by name
     */
    @Query("SELECT * FROM disease_info WHERE disease_name = :diseaseName LIMIT 1")
    DiseaseInfo getDiseaseInfoByName(String diseaseName);

    /**
     * Get disease info by plant type
     */
    @Query("SELECT * FROM disease_info WHERE plant_type = :plantType ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseaseInfoByPlantType(String plantType);

    /**
     * Search disease info by name or plant type
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "disease_name LIKE '%' || :searchQuery || '%' OR " +
            "plant_type LIKE '%' || :searchQuery || '%' OR " +
            "description LIKE '%' || :searchQuery || '%' " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> searchDiseaseInfo(String searchQuery);

    /**
     * Get disease info by severity level
     */
    @Query("SELECT * FROM disease_info WHERE severity_level = :severityLevel ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseaseInfoBySeverity(String severityLevel);

    /**
     * Get treatable diseases
     */
    @Query("SELECT * FROM disease_info WHERE is_treatable = 1 ORDER BY disease_name ASC")
    List<DiseaseInfo> getTreatableDiseases();

    /**
     * Get common diseases
     */
    @Query("SELECT * FROM disease_info WHERE is_common = 1 ORDER BY disease_name ASC")
    List<DiseaseInfo> getCommonDiseases();

    /**
     * Get diseases that match a predicted class
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "disease_name LIKE '%' || :predictedClass || '%' OR " +
            ":predictedClass LIKE '%' || disease_name || '%' " +
            "LIMIT 1")
    DiseaseInfo getDiseaseInfoByPrediction(String predictedClass);

    // ===== SPECIALIZED QUERIES =====

    /**
     * Get diseases affecting specific plant parts
     */
    @Query("SELECT * FROM disease_info WHERE affected_parts LIKE '%' || :plantPart || '%' ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseasesByAffectedPart(String plantPart);

    /**
     * Get diseases by environmental factors
     */
    @Query("SELECT * FROM disease_info WHERE environmental_factors LIKE '%' || :factor || '%' ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseasesByEnvironmentalFactor(String factor);

    /**
     * Get diseases by spread method
     */
    @Query("SELECT * FROM disease_info WHERE spread_method LIKE '%' || :method || '%' ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseasesBySpreadMethod(String method);

    // ===== STATISTICAL QUERIES =====

    /**
     * Get total count of disease records
     */
    @Query("SELECT COUNT(*) FROM disease_info")
    int getTotalDiseaseCount();

    /**
     * Get count of treatable diseases
     */
    @Query("SELECT COUNT(*) FROM disease_info WHERE is_treatable = 1")
    int getTreatableDiseaseCount();

    /**
     * Get count of diseases by plant type
     */
    @Query("SELECT COUNT(*) FROM disease_info WHERE plant_type = :plantType")
    int getDiseaseCountByPlantType(String plantType);

    /**
     * Get count of diseases by severity
     */
    @Query("SELECT COUNT(*) FROM disease_info WHERE severity_level = :severity")
    int getDiseaseCountBySeverity(String severity);

    /**
     * Get unique plant types
     */
    @Query("SELECT DISTINCT plant_type FROM disease_info ORDER BY plant_type ASC")
    List<String> getUniquePlantTypes();

    /**
     * Get unique severity levels
     */
    @Query("SELECT DISTINCT severity_level FROM disease_info ORDER BY severity_level ASC")
    List<String> getUniqueSeverityLevels();

    // ===== ADVANCED QUERIES =====

    /**
     * Get diseases with treatment options
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "treatment_options IS NOT NULL AND " +
            "treatment_options != '' " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseasesWithTreatment();

    /**
     * Get diseases with prevention tips
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "prevention_tips IS NOT NULL AND " +
            "prevention_tips != '' " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseasesWithPrevention();

    /**
     * Get recently accessed diseases (based on ID for demo)
     */
    @Query("SELECT * FROM disease_info ORDER BY id DESC LIMIT :limit")
    List<DiseaseInfo> getRecentDiseases(int limit);

    /**
     * Check if disease info exists for a specific disease
     */
    @Query("SELECT EXISTS(SELECT 1 FROM disease_info WHERE disease_name = :diseaseName)")
    boolean diseaseInfoExists(String diseaseName);

    /**
     * Get disease info for multiple predictions
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "disease_name IN (:diseaseNames) " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseaseInfoForPredictions(List<String> diseaseNames);

    // ===== FILTERING COMBINATIONS =====

    /**
     * Get diseases by plant type and severity
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "plant_type = :plantType AND " +
            "severity_level = :severity " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> getDiseasesByPlantTypeAndSeverity(String plantType, String severity);

    /**
     * Get diseases that are treatable and common
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "is_treatable = 1 AND " +
            "is_common = 1 " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> getTreatableCommonDiseases();

    /**
     * Advanced search with multiple criteria
     */
    @Query("SELECT * FROM disease_info WHERE " +
            "(:plantType IS NULL OR plant_type = :plantType) AND " +
            "(:severity IS NULL OR severity_level = :severity) AND " +
            "(:treatable IS NULL OR is_treatable = :treatable) AND " +
            "(:common IS NULL OR is_common = :common) AND " +
            "(:searchQuery IS NULL OR " +
            " disease_name LIKE '%' || :searchQuery || '%' OR " +
            " description LIKE '%' || :searchQuery || '%' OR " +
            " symptoms LIKE '%' || :searchQuery || '%') " +
            "ORDER BY disease_name ASC")
    List<DiseaseInfo> advancedSearch(String plantType, String severity,
                                     Boolean treatable, Boolean common, String searchQuery);
}