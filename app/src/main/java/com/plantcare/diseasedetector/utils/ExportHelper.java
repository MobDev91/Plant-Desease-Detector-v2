package com.plantcare.diseasedetector.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for exporting scan data to various formats
 */
public class ExportHelper {

    private static final String TAG = "ExportHelper";
    private static final String EXPORT_FOLDER = "PlantDiseaseDetector";

    private Context context;
    private AppDatabase database;
    private ExecutorService executor;

    public ExportHelper(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Export scan data to CSV format
     */
    public void exportToCSV() {
        executor.execute(() -> {
            try {
                List<ScanResult> scanResults = database.scanResultDao().getAllScanResults();

                if (scanResults.isEmpty()) {
                    showToast("No scan data to export");
                    return;
                }

                File csvFile = createExportFile("scan_data.csv");
                FileWriter writer = new FileWriter(csvFile);

                // Write CSV header
                writer.append("Date,Time,Plant_Type,Disease,Confidence,Health_Status,Image_Path,Notes\n");

                // Write data rows
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                for (ScanResult result : scanResults) {
                    writer.append(dateFormat.format(result.getScanDate())).append(",");
                    writer.append(timeFormat.format(result.getScanDate())).append(",");
                    writer.append("\"").append(escapeCSV(result.getDisplayName())).append("\",");
                    writer.append("\"").append(escapeCSV(result.getHealthStatusText())).append("\",");
                    writer.append(String.valueOf(result.getConfidence())).append(",");
                    writer.append(result.isHealthy() ? "Healthy" : "Disease Detected").append(",");
                    writer.append("\"").append(escapeCSV(result.getImagePath())).append("\",");
                    writer.append("\"").append(escapeCSV(result.getNotes() != null ? result.getNotes() : "")).append("\"");
                    writer.append("\n");
                }

                writer.flush();
                writer.close();

                showToast("CSV export completed: " + csvFile.getName());
                shareFile(csvFile, "text/csv");

            } catch (Exception e) {
                Log.e(TAG, "Error exporting to CSV", e);
                showToast("CSV export failed: " + e.getMessage());
            }
        });
    }

    /**
     * Export scan data to PDF report
     */
    public void exportToPDF() {
        executor.execute(() -> {
            try {
                List<ScanResult> scanResults = database.scanResultDao().getAllScanResults();

                if (scanResults.isEmpty()) {
                    showToast("No scan data to export");
                    return;
                }

                File pdfFile = createExportFile("plant_disease_report.pdf");
                PdfDocument document = new PdfDocument();

                // Create pages
                int pageNumber = 1;
                int itemsPerPage = 5;
                int currentItem = 0;

                while (currentItem < scanResults.size()) {
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();

                    drawPDFPage(canvas, scanResults, currentItem, Math.min(currentItem + itemsPerPage, scanResults.size()), pageNumber);

                    document.finishPage(page);
                    currentItem += itemsPerPage;
                    pageNumber++;
                }

                // Write PDF to file
                FileOutputStream outputStream = new FileOutputStream(pdfFile);
                document.writeTo(outputStream);
                document.close();
                outputStream.close();

                showToast("PDF report generated: " + pdfFile.getName());
                shareFile(pdfFile, "application/pdf");

            } catch (Exception e) {
                Log.e(TAG, "Error exporting to PDF", e);
                showToast("PDF export failed: " + e.getMessage());
            }
        });
    }

    /**
     * Export scan data to JSON format
     */
    public void exportToJSON() {
        executor.execute(() -> {
            try {
                List<ScanResult> scanResults = database.scanResultDao().getAllScanResults();

                if (scanResults.isEmpty()) {
                    showToast("No scan data to export");
                    return;
                }

                JSONObject exportData = new JSONObject();
                exportData.put("export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                exportData.put("app_version", "1.0.0");
                exportData.put("total_scans", scanResults.size());

                JSONArray scansArray = new JSONArray();

                for (ScanResult result : scanResults) {
                    JSONObject scanObject = new JSONObject();
                    scanObject.put("id", result.getId());
                    scanObject.put("scan_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.getScanDate()));
                    scanObject.put("plant_name", result.getDisplayName());
                    scanObject.put("predicted_class", result.getPredictedClass());
                    scanObject.put("confidence", result.getConfidence());
                    scanObject.put("is_healthy", result.isHealthy());
                    scanObject.put("health_status", result.getHealthStatusText());
                    scanObject.put("image_path", result.getImagePath());
                    scanObject.put("notes", result.getNotes());
                    scanObject.put("location", result.getLocation());
                    scanObject.put("treatment_applied", result.getTreatmentApplied());

                    if (result.getFollowUpDate() != null) {
                        scanObject.put("follow_up_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(result.getFollowUpDate()));
                    }

                    scansArray.put(scanObject);
                }

                exportData.put("scans", scansArray);

                // Write to file
                File jsonFile = createExportFile("scan_data.json");
                FileWriter writer = new FileWriter(jsonFile);
                writer.write(exportData.toString(2)); // Pretty print with 2-space indentation
                writer.close();

                showToast("JSON export completed: " + jsonFile.getName());
                shareFile(jsonFile, "application/json");

            } catch (Exception e) {
                Log.e(TAG, "Error exporting to JSON", e);
                showToast("JSON export failed: " + e.getMessage());
            }
        });
    }

    /**
     * Generate plant health summary report
     */
    public void generateHealthSummaryReport() {
        executor.execute(() -> {
            try {
                List<ScanResult> scanResults = database.scanResultDao().getAllScanResults();

                if (scanResults.isEmpty()) {
                    showToast("No scan data to export");
                    return;
                }

                // Calculate statistics
                int totalScans = scanResults.size();
                int healthyScans = 0;
                int diseasedScans = 0;
                String mostCommonDisease = "None";
                float averageConfidence = 0f;

                for (ScanResult result : scanResults) {
                    if (result.isHealthy()) {
                        healthyScans++;
                    } else {
                        diseasedScans++;
                    }
                    averageConfidence += result.getConfidence();
                }
                averageConfidence /= totalScans;

                // Create summary report
                StringBuilder report = new StringBuilder();
                report.append("PLANT HEALTH SUMMARY REPORT\n");
                report.append("Generated: ").append(DateUtils.getFormattedDateTime(new Date())).append("\n\n");
                report.append("OVERVIEW\n");
                report.append("========\n");
                report.append("Total Scans: ").append(totalScans).append("\n");
                report.append("Healthy Plants: ").append(healthyScans).append(" (").append(String.format("%.1f%%", (healthyScans * 100.0f / totalScans))).append(")\n");
                report.append("Diseased Plants: ").append(diseasedScans).append(" (").append(String.format("%.1f%%", (diseasedScans * 100.0f / totalScans))).append(")\n");
                report.append("Average Confidence: ").append(String.format("%.1f%%", averageConfidence * 100)).append("\n\n");

                report.append("RECENT SCANS\n");
                report.append("============\n");
                int recentCount = Math.min(10, scanResults.size());
                for (int i = 0; i < recentCount; i++) {
                    ScanResult result = scanResults.get(i);
                    report.append(DateUtils.getFormattedDate(result.getScanDate())).append(" - ");
                    report.append(result.getDisplayName()).append(" - ");
                    report.append(result.getHealthStatusText()).append(" (");
                    report.append(result.getConfidencePercentage()).append(")\n");
                }

                // Save report
                File reportFile = createExportFile("health_summary_report.txt");
                FileWriter writer = new FileWriter(reportFile);
                writer.write(report.toString());
                writer.close();

                showToast("Health summary report generated");
                shareFile(reportFile, "text/plain");

            } catch (Exception e) {
                Log.e(TAG, "Error generating health summary", e);
                showToast("Report generation failed: " + e.getMessage());
            }
        });
    }

    /**
     * Draw PDF page content
     */
    private void drawPDFPage(Canvas canvas, List<ScanResult> scanResults, int startIndex, int endIndex, int pageNumber) {
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(24);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setFakeBoldText(true);

        Paint headerPaint = new Paint();
        headerPaint.setTextSize(16);
        headerPaint.setColor(Color.BLACK);
        headerPaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setTextSize(12);
        textPaint.setColor(Color.BLACK);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(1);

        // Title
        canvas.drawText("Plant Disease Detection Report", 50, 50, titlePaint);
        canvas.drawText("Generated: " + DateUtils.getFormattedDateTime(new Date()), 50, 80, textPaint);
        canvas.drawLine(50, 90, 545, 90, linePaint);

        int yPosition = 120;

        for (int i = startIndex; i < endIndex; i++) {
            ScanResult result = scanResults.get(i);

            // Scan entry
            canvas.drawText("Scan #" + (i + 1), 50, yPosition, headerPaint);
            yPosition += 25;

            canvas.drawText("Date: " + DateUtils.getFormattedDateTime(result.getScanDate()), 70, yPosition, textPaint);
            yPosition += 20;

            canvas.drawText("Plant: " + result.getDisplayName(), 70, yPosition, textPaint);
            yPosition += 20;

            canvas.drawText("Status: " + result.getHealthStatusText(), 70, yPosition, textPaint);
            yPosition += 20;

            canvas.drawText("Confidence: " + result.getConfidencePercentage(), 70, yPosition, textPaint);
            yPosition += 20;

            if (result.getNotes() != null && !result.getNotes().isEmpty()) {
                canvas.drawText("Notes: " + result.getNotes(), 70, yPosition, textPaint);
                yPosition += 20;
            }

            // Separator line
            canvas.drawLine(50, yPosition + 5, 545, yPosition + 5, linePaint);
            yPosition += 30;
        }

        // Page number
        canvas.drawText("Page " + pageNumber, 500, 820, textPaint);
    }

    /**
     * Create export file in the appropriate directory
     */
    private File createExportFile(String fileName) throws IOException {
        File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), EXPORT_FOLDER);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String[] nameParts = fileName.split("\\.");
        String nameWithTimestamp = nameParts[0] + "_" + timestamp + "." + nameParts[1];

        return new File(exportDir, nameWithTimestamp);
    }

    /**
     * Share exported file
     */
    private void shareFile(File file, String mimeType) {
        try {
            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Plant Disease Detection Export");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Share export file");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);

        } catch (Exception e) {
            Log.e(TAG, "Error sharing file", e);
            showToast("Failed to share file: " + e.getMessage());
        }
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    /**
     * Show toast message on main thread
     */
    private void showToast(String message) {
        // Post to main thread
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Get export statistics
     */
    public void getExportStatistics(ExportStatsCallback callback) {
        executor.execute(() -> {
            try {
                int totalScans = database.scanResultDao().getTotalScanCount();
                int healthyScans = database.scanResultDao().getHealthyScanCount();
                int diseasedScans = database.scanResultDao().getDiseasedScanCount();

                long estimatedSize = calculateExportSize();

                ExportStatistics stats = new ExportStatistics();
                stats.totalScans = totalScans;
                stats.healthyScans = healthyScans;
                stats.diseasedScans = diseasedScans;
                stats.estimatedCSVSize = estimatedSize;
                stats.estimatedPDFSize = estimatedSize * 2; // PDF is typically larger
                stats.estimatedJSONSize = estimatedSize * 1.5f; // JSON with formatting

                callback.onStatsReady(stats);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Calculate estimated export size
     */
    private long calculateExportSize() {
        // Rough estimation based on average data per scan
        int scanCount = database.scanResultDao().getTotalScanCount();
        return scanCount * 200L; // ~200 bytes per scan average
    }

    /**
     * Export statistics data class
     */
    public static class ExportStatistics {
        public int totalScans;
        public int healthyScans;
        public int diseasedScans;
        public long estimatedCSVSize;
        public double estimatedPDFSize;
        public double estimatedJSONSize;
    }

    /**
     * Callback interface for export statistics
     */
    public interface ExportStatsCallback {
        void onStatsReady(ExportStatistics stats);
        void onError(String error);
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}