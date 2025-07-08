package com.plantcare.diseasedetector.ui.history;

import com.plantcare.diseasedetector.data.models.ScanResult;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for calculating various statistics from scan results
 * Provides analytics and insights for the History Activity
 */
public class StatisticsHelper {

    /**
     * Statistics data container
     */
    public static class ScanStatistics {
        public int totalScans;
        public int healthyScans;
        public int diseasedScans;
        public int scansThisWeek;
        public int scansThisMonth;
        public int scansToday;
        public float averageConfidence;
        public float healthyPercentage;
        public float diseasedPercentage;
        public String mostCommonPlant;
        public String mostCommonDisease;
        public int highConfidenceScans; // > 80%
        public int mediumConfidenceScans; // 60-80%
        public int lowConfidenceScans; // < 60%
        public Date lastScanDate;
        public Date firstScanDate;
        public Map<String, Integer> plantTypeCount;
        public Map<String, Integer> diseaseTypeCount;
        public Map<String, Integer> dailyScansThisWeek;
        public Map<String, Integer> monthlyScansThisYear;

        public ScanStatistics() {
            plantTypeCount = new HashMap<>();
            diseaseTypeCount = new HashMap<>();
            dailyScansThisWeek = new HashMap<>();
            monthlyScansThisYear = new HashMap<>();
        }
    }

    /**
     * Calculate comprehensive statistics from scan results
     */
    public static ScanStatistics calculateStatistics(List<ScanResult> scanResults) {
        ScanStatistics stats = new ScanStatistics();

        if (scanResults == null || scanResults.isEmpty()) {
            return stats;
        }

        // Basic counts
        stats.totalScans = scanResults.size();

        // Initialize variables for calculations
        float totalConfidence = 0f;
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();

        // Time boundaries
        Date weekAgo = getDateDaysAgo(7);
        Date monthAgo = getDateDaysAgo(30);
        Date todayStart = getTodayStart();

        // Initialize tracking maps
        Map<String, Integer> plantCounts = new HashMap<>();
        Map<String, Integer> diseaseCounts = new HashMap<>();

        // Process each scan result
        for (ScanResult scan : scanResults) {
            // Confidence tracking
            totalConfidence += scan.getConfidence();

            if (scan.getConfidence() >= 0.8f) {
                stats.highConfidenceScans++;
            } else if (scan.getConfidence() >= 0.6f) {
                stats.mediumConfidenceScans++;
            } else {
                stats.lowConfidenceScans++;
            }

            // Health status tracking
            if (scan.isHealthy()) {
                stats.healthyScans++;
            } else {
                stats.diseasedScans++;

                // Track disease types
                String disease = scan.getDiseaseName();
                if (disease != null && !disease.isEmpty()) {
                    diseaseCounts.put(disease, diseaseCounts.getOrDefault(disease, 0) + 1);
                }
            }

            // Date-based tracking
            Date scanDate = scan.getScanDate();
            if (scanDate != null) {
                // Update first/last scan dates
                if (stats.firstScanDate == null || scanDate.before(stats.firstScanDate)) {
                    stats.firstScanDate = scanDate;
                }
                if (stats.lastScanDate == null || scanDate.after(stats.lastScanDate)) {
                    stats.lastScanDate = scanDate;
                }

                // Time period counts
                if (scanDate.after(weekAgo)) {
                    stats.scansThisWeek++;
                }
                if (scanDate.after(monthAgo)) {
                    stats.scansThisMonth++;
                }
                if (scanDate.after(todayStart)) {
                    stats.scansToday++;
                }

                // Daily tracking for this week
                String dayKey = getDayKey(scanDate);
                stats.dailyScansThisWeek.put(dayKey,
                        stats.dailyScansThisWeek.getOrDefault(dayKey, 0) + 1);

                // Monthly tracking for this year
                String monthKey = getMonthKey(scanDate);
                stats.monthlyScansThisYear.put(monthKey,
                        stats.monthlyScansThisYear.getOrDefault(monthKey, 0) + 1);
            }

            // Plant type tracking
            String plantName = scan.getPlantName();
            if (plantName != null && !plantName.isEmpty()) {
                plantCounts.put(plantName, plantCounts.getOrDefault(plantName, 0) + 1);
            }
        }

        // Calculate derived statistics
        stats.averageConfidence = totalConfidence / stats.totalScans;
        stats.healthyPercentage = (float) stats.healthyScans / stats.totalScans * 100;
        stats.diseasedPercentage = (float) stats.diseasedScans / stats.totalScans * 100;

        // Find most common plant and disease
        stats.mostCommonPlant = getMostCommon(plantCounts);
        stats.mostCommonDisease = getMostCommon(diseaseCounts);

        // Store the maps
        stats.plantTypeCount = plantCounts;
        stats.diseaseTypeCount = diseaseCounts;

        return stats;
    }

    /**
     * Get date that was N days ago
     */
    private static Date getDateDaysAgo(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        return calendar.getTime();
    }

    /**
     * Get start of today (midnight)
     */
    private static Date getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * Get day key for tracking (e.g., "Mon", "Tue")
     */
    private static String getDayKey(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        return days[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    /**
     * Get month key for tracking (e.g., "Jan", "Feb")
     */
    private static String getMonthKey(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[calendar.get(Calendar.MONTH)];
    }

    /**
     * Get the most common item from a count map
     */
    private static String getMostCommon(Map<String, Integer> countMap) {
        if (countMap.isEmpty()) return "None";

        String mostCommon = "";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }

        return mostCommon.isEmpty() ? "None" : mostCommon;
    }

    /**
     * Calculate statistics for a specific time period
     */
    public static ScanStatistics calculatePeriodStatistics(List<ScanResult> scanResults, int days) {
        Date cutoffDate = getDateDaysAgo(days);

        // Filter scans within the period
        List<ScanResult> periodScans = scanResults.stream()
                .filter(scan -> scan.getScanDate() != null && scan.getScanDate().after(cutoffDate))
                .collect(java.util.stream.Collectors.toList());

        return calculateStatistics(periodScans);
    }

    /**
     * Get scanning frequency analysis
     */
    public static String getScanningFrequency(ScanStatistics stats) {
        if (stats.totalScans == 0) return "No scans yet";

        if (stats.firstScanDate == null || stats.lastScanDate == null) {
            return "Insufficient data";
        }

        long daysBetween = (stats.lastScanDate.getTime() - stats.firstScanDate.getTime()) / (1000 * 60 * 60 * 24);
        if (daysBetween == 0) daysBetween = 1; // At least 1 day

        float scansPerDay = (float) stats.totalScans / daysBetween;

        if (scansPerDay >= 1.0f) {
            return String.format("%.1f scans per day", scansPerDay);
        } else {
            float daysPerScan = 1.0f / scansPerDay;
            return String.format("1 scan every %.1f days", daysPerScan);
        }
    }

    /**
     * Get confidence level description
     */
    public static String getConfidenceDescription(float averageConfidence) {
        if (averageConfidence >= 0.9f) {
            return "Excellent";
        } else if (averageConfidence >= 0.8f) {
            return "Very Good";
        } else if (averageConfidence >= 0.7f) {
            return "Good";
        } else if (averageConfidence >= 0.6f) {
            return "Fair";
        } else {
            return "Needs Improvement";
        }
    }

    /**
     * Get health trend description
     */
    public static String getHealthTrend(ScanStatistics stats) {
        if (stats.totalScans < 5) {
            return "More scans needed for trend analysis";
        }

        if (stats.healthyPercentage >= 80f) {
            return "Excellent plant health";
        } else if (stats.healthyPercentage >= 60f) {
            return "Good plant health";
        } else if (stats.healthyPercentage >= 40f) {
            return "Fair plant health - watch for issues";
        } else {
            return "Many plants need attention";
        }
    }

    /**
     * Get insights and recommendations
     */
    public static String[] getInsights(ScanStatistics stats) {
        if (stats.totalScans == 0) {
            return new String[]{"Start scanning your plants to get insights!"};
        }

        String[] insights = new String[3];

        // Insight 1: Overall health
        if (stats.healthyPercentage >= 70f) {
            insights[0] = "Great job! Most of your plants are healthy.";
        } else {
            insights[0] = String.format("%.0f%% of your plants need attention.", stats.diseasedPercentage);
        }

        // Insight 2: Most common issue
        if (stats.diseasedScans > 0 && !stats.mostCommonDisease.equals("None")) {
            insights[1] = "Most common issue: " + stats.mostCommonDisease;
        } else {
            insights[1] = "No major plant health issues detected.";
        }

        // Insight 3: Scanning pattern
        if (stats.scansThisWeek == 0) {
            insights[2] = "Consider scanning your plants regularly for early detection.";
        } else if (stats.scansThisWeek >= 5) {
            insights[2] = "You're doing great with regular plant monitoring!";
        } else {
            insights[2] = "Regular scanning helps catch issues early.";
        }

        return insights;
    }

    /**
     * Calculate disease severity distribution
     */
    public static Map<String, Integer> getDiseaseSeverityDistribution(List<ScanResult> scanResults) {
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("Healthy", 0);
        distribution.put("Low", 0);
        distribution.put("Medium", 0);
        distribution.put("High", 0);

        for (ScanResult scan : scanResults) {
            if (scan.isHealthy()) {
                distribution.put("Healthy", distribution.get("Healthy") + 1);
            } else {
                ScanResult.SeverityLevel severity = scan.getSeverityLevel();
                String severityKey = severity.name().toLowerCase();
                severityKey = Character.toUpperCase(severityKey.charAt(0)) + severityKey.substring(1);
                distribution.put(severityKey, distribution.getOrDefault(severityKey, 0) + 1);
            }
        }

        return distribution;
    }

    /**
     * Get top plant types by scan count
     */
    public static Map<String, Integer> getTopPlantTypes(ScanStatistics stats, int limit) {
        return stats.plantTypeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }

    /**
     * Calculate scanning streak (consecutive days with scans)
     */
    public static int getCurrentStreak(List<ScanResult> scanResults) {
        if (scanResults.isEmpty()) return 0;

        // Sort by date descending
        scanResults.sort((a, b) -> b.getScanDate().compareTo(a.getScanDate()));

        Calendar calendar = Calendar.getInstance();
        Date today = getTodayStart();
        int streak = 0;

        for (int i = 0; i < 30 && i < scanResults.size(); i++) { // Check last 30 days max
            calendar.setTime(today);
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            Date checkDate = calendar.getTime();

            boolean hasScansOnDay = scanResults.stream()
                    .anyMatch(scan -> isSameDay(scan.getScanDate(), checkDate));

            if (hasScansOnDay) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    /**
     * Check if two dates are on the same day
     */
    private static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}