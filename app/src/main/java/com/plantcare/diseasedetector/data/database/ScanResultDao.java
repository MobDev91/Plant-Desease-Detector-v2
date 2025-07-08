package com.plantcare.diseasedetector.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.plantcare.diseasedetector.data.models.ScanResult;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object for ScanResult entity
 * Provides methods to interact with the scan_results table
 */
@Dao
public interface ScanResultDao {

    /**
     * Insert a new scan result
     */
    @Insert
    long insertScanResult(ScanResult scanResult);

    /**
     * Insert multiple scan results
     */
    @Insert
    void insertScanResults(List<ScanResult> scanResults);

    /**
     * Update an existing scan result
     */
    @Update
    void updateScanResult(ScanResult scanResult);

    /**
     * Delete a scan result
     */
    @Delete
    void deleteScanResult(ScanResult scanResult);

    /**
     * Delete scan result by ID
     */
    @Query("DELETE FROM scan_results WHERE id = :id")
    void deleteScanResultById(int id);

    /**
     * Delete all scan results - ADDED METHOD TO FIX COMPILATION ERROR
     */
    @Query("DELETE FROM scan_results")
    void deleteAllScanResults();

    /**
     * Get all scan results ordered by scan date (newest first)
     */
    @Query("SELECT * FROM scan_results ORDER BY scan_date DESC")
    List<ScanResult> getAllScanResults();

    /**
     * Get recent scan results (limited number)
     */
    @Query("SELECT * FROM scan_results ORDER BY scan_date DESC LIMIT :limit")
    List<ScanResult> getRecentScans(int limit);

    /**
     * Get scan result by ID
     */
    @Query("SELECT * FROM scan_results WHERE id = :id")
    ScanResult getScanResultById(int id);

    /**
     * Get scan results by plant name
     */
    @Query("SELECT * FROM scan_results WHERE plant_name LIKE :plantName ORDER BY scan_date DESC")
    List<ScanResult> getScanResultsByPlantName(String plantName);

    /**
     * Get healthy scan results only
     */
    @Query("SELECT * FROM scan_results WHERE is_healthy = 1 ORDER BY scan_date DESC")
    List<ScanResult> getHealthyScanResults();

    /**
     * Get diseased scan results only
     */
    @Query("SELECT * FROM scan_results WHERE is_healthy = 0 ORDER BY scan_date DESC")
    List<ScanResult> getDiseasedScanResults();

    /**
     * Get scan results by confidence threshold
     */
    @Query("SELECT * FROM scan_results WHERE confidence >= :minConfidence ORDER BY confidence DESC, scan_date DESC")
    List<ScanResult> getScanResultsByConfidence(float minConfidence);

    /**
     * Get scan results within date range
     */
    @Query("SELECT * FROM scan_results WHERE scan_date BETWEEN :startDate AND :endDate ORDER BY scan_date DESC")
    List<ScanResult> getScanResultsByDateRange(Date startDate, Date endDate);

    /**
     * Get scan results for specific disease
     */
    @Query("SELECT * FROM scan_results WHERE disease_name LIKE :diseaseName ORDER BY scan_date DESC")
    List<ScanResult> getScanResultsByDisease(String diseaseName);

    /**
     * Search scan results by plant name or disease name
     */
    @Query("SELECT * FROM scan_results WHERE plant_name LIKE :searchQuery OR disease_name LIKE :searchQuery OR predicted_class LIKE :searchQuery ORDER BY scan_date DESC")
    List<ScanResult> searchScanResults(String searchQuery);

    /**
     * Get total count of scan results
     */
    @Query("SELECT COUNT(*) FROM scan_results")
    int getTotalScanCount();

    /**
     * Get count of healthy scans
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE is_healthy = 1")
    int getHealthyScanCount();

    /**
     * Get count of diseased scans
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE is_healthy = 0")
    int getDiseasedScanCount();

    /**
     * Get count of scans for today
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE DATE(scan_date) = DATE('now')")
    int getTodayScanCount();

    /**
     * Get count of scans for this week
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE scan_date >= datetime('now', '-7 days')")
    int getWeeklyScanCount();

    /**
     * Get count of scans for this month
     */
    @Query("SELECT COUNT(*) FROM scan_results WHERE scan_date >= datetime('now', '-30 days')")
    int getMonthlyScanCount();

    /**
     * Get most common diseases (top 5)
     */
    @Query("SELECT disease_name, COUNT(*) as count FROM scan_results WHERE is_healthy = 0 AND disease_name IS NOT NULL GROUP BY disease_name ORDER BY count DESC LIMIT 5")
    List<DiseaseCount> getMostCommonDiseases();

    /**
     * Get most scanned plants (top 5)
     */
    @Query("SELECT plant_name, COUNT(*) as count FROM scan_results WHERE plant_name IS NOT NULL GROUP BY plant_name ORDER BY count DESC LIMIT 5")
    List<PlantCount> getMostScannedPlants();

    /**
     * Get scans that need follow-up
     */
    @Query("SELECT * FROM scan_results WHERE is_healthy = 0 AND follow_up_date IS NOT NULL AND follow_up_date >= DATE('now') ORDER BY follow_up_date ASC")
    List<ScanResult> getScansNeedingFollowUp();

    /**
     * Get average confidence for healthy scans
     */
    @Query("SELECT AVG(confidence) FROM scan_results WHERE is_healthy = 1")
    float getAverageHealthyConfidence();

    /**
     * Get average confidence for diseased scans
     */
    @Query("SELECT AVG(confidence) FROM scan_results WHERE is_healthy = 0")
    float getAverageDiseasedConfidence();

    /**
     * Delete old scan results (older than specified days)
     */
    @Query("DELETE FROM scan_results WHERE scan_date < datetime('now', '-' || :days || ' days')")
    void deleteOldScanResults(int days);

    /**
     * Get scan results with low confidence (might need review)
     */
    @Query("SELECT * FROM scan_results WHERE confidence < :threshold ORDER BY scan_date DESC")
    List<ScanResult> getLowConfidenceScans(float threshold);

    /**
     * Helper classes for aggregate queries
     */
    class DiseaseCount {
        public String disease_name;
        public int count;
    }

    class PlantCount {
        public String plant_name;
        public int count;
    }
}