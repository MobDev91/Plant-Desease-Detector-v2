package com.plantcare.diseasedetector.ui.history;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.utils.DateUtils;
import com.plantcare.diseasedetector.utils.ImageUtils;

import java.util.List;

/**
 * Dialog fragment for confirming deletion of scan results
 * Supports single and multiple scan deletion with detailed information
 */
public class DeleteConfirmationDialog extends DialogFragment {

    public enum DeleteType {
        SINGLE_SCAN("Delete Scan"),
        MULTIPLE_SCANS("Delete Scans"),
        ALL_SCANS("Delete All Scans"),
        FILTERED_SCANS("Delete Filtered Scans");

        public final String title;
        DeleteType(String title) { this.title = title; }
    }

    // Interface for deletion confirmation
    public interface DeleteConfirmationListener {
        void onDeleteConfirmed(List<ScanResult> scansToDelete, boolean alsoDeleteImages);
        void onDeleteCancelled();
    }

    private DeleteConfirmationListener listener;
    private List<ScanResult> scansToDelete;
    private DeleteType deleteType;
    private boolean showImageDeletionOption = true;

    // UI Components
    private ImageView ivScanImage;
    private TextView tvTitle, tvMessage, tvScanInfo, tvWarningText;
    private CheckBox cbDeleteImages;
    private View layoutScanPreview, layoutImageOption;

    public static DeleteConfirmationDialog newInstance(
            List<ScanResult> scansToDelete,
            DeleteType deleteType) {
        return newInstance(scansToDelete, deleteType, true);
    }

    public static DeleteConfirmationDialog newInstance(
            List<ScanResult> scansToDelete,
            DeleteType deleteType,
            boolean showImageDeletionOption) {

        DeleteConfirmationDialog fragment = new DeleteConfirmationDialog();
        fragment.scansToDelete = scansToDelete;
        fragment.deleteType = deleteType;
        fragment.showImageDeletionOption = showImageDeletionOption;
        return fragment;
    }

    public void setDeleteConfirmationListener(DeleteConfirmationListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_delete_confirmation, null);

        initializeViews(view);
        setupDialogContent();

        builder.setView(view)
                .setTitle(deleteType.title)
                .setPositiveButton("Delete", (dialog, id) -> confirmDeletion())
                .setNegativeButton("Cancel", (dialog, id) -> cancelDeletion())
                .setIcon(R.drawable.ic_delete);

        AlertDialog dialog = builder.create();

        // Make delete button red
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(R.color.disease_red, null));
        });

        return dialog;
    }

    private void initializeViews(View view) {
        ivScanImage = view.findViewById(R.id.iv_scan_image);
        tvTitle = view.findViewById(R.id.tv_title);
        tvMessage = view.findViewById(R.id.tv_message);
        tvScanInfo = view.findViewById(R.id.tv_scan_info);
        tvWarningText = view.findViewById(R.id.tv_warning_text);
        cbDeleteImages = view.findViewById(R.id.cb_delete_images);
        layoutScanPreview = view.findViewById(R.id.layout_scan_preview);
        layoutImageOption = view.findViewById(R.id.layout_image_option);
    }

    private void setupDialogContent() {
        if (scansToDelete == null || scansToDelete.isEmpty()) {
            dismiss();
            return;
        }

        setupTitle();
        setupMessage();
        setupScanPreview();
        setupImageDeletionOption();
        setupWarningText();
    }

    private void setupTitle() {
        String title;
        switch (deleteType) {
            case SINGLE_SCAN:
                title = "Delete this scan?";
                break;
            case MULTIPLE_SCANS:
                title = "Delete " + scansToDelete.size() + " scans?";
                break;
            case ALL_SCANS:
                title = "Delete all scans?";
                break;
            case FILTERED_SCANS:
                title = "Delete " + scansToDelete.size() + " filtered scans?";
                break;
            default:
                title = "Delete scans?";
        }
        tvTitle.setText(title);
    }

    private void setupMessage() {
        String message;
        switch (deleteType) {
            case SINGLE_SCAN:
                ScanResult scan = scansToDelete.get(0);
                message = "Are you sure you want to delete the scan of " +
                        scan.getDisplayName() + "?";
                break;
            case MULTIPLE_SCANS:
                message = "Are you sure you want to delete " + scansToDelete.size() +
                        " selected scans?";
                break;
            case ALL_SCANS:
                message = "Are you sure you want to delete all " + scansToDelete.size() +
                        " scans? This will clear your entire scan history.";
                break;
            case FILTERED_SCANS:
                message = "Are you sure you want to delete all " + scansToDelete.size() +
                        " scans matching the current filters?";
                break;
            default:
                message = "Are you sure you want to delete the selected scans?";
        }
        tvMessage.setText(message);
    }

    private void setupScanPreview() {
        if (deleteType == DeleteType.SINGLE_SCAN && scansToDelete.size() == 1) {
            layoutScanPreview.setVisibility(View.VISIBLE);
            ScanResult scan = scansToDelete.get(0);

            // Load scan image
            if (scan.getImagePath() != null && !scan.getImagePath().isEmpty()) {
                new Thread(() -> {
                    android.graphics.Bitmap thumbnail = ImageUtils.createThumbnail(scan.getImagePath(), 120);
                    if (thumbnail != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> ivScanImage.setImageBitmap(thumbnail));
                    }
                }).start();
            } else {
                ivScanImage.setImageResource(R.drawable.placeholder_plant);
            }

            // Setup scan info
            StringBuilder scanInfo = new StringBuilder();
            scanInfo.append("Plant: ").append(scan.getDisplayName()).append("\n");
            scanInfo.append("Status: ").append(scan.getHealthStatusText()).append("\n");
            scanInfo.append("Confidence: ").append(scan.getConfidencePercentage()).append("\n");
            if (scan.getScanDate() != null) {
                scanInfo.append("Date: ").append(DateUtils.getFormattedDateTime(scan.getScanDate()));
            }

            tvScanInfo.setText(scanInfo.toString());
        } else {
            layoutScanPreview.setVisibility(View.GONE);
        }
    }

    private void setupImageDeletionOption() {
        if (showImageDeletionOption && hasImagesAttached()) {
            layoutImageOption.setVisibility(View.VISIBLE);

            int imageCount = countImagesAttached();
            String imageText;

            if (imageCount == 1) {
                imageText = "Also delete the associated image file";
            } else {
                imageText = "Also delete " + imageCount + " associated image files";
            }

            cbDeleteImages.setText(imageText);
            cbDeleteImages.setChecked(true); // Default to deleting images
        } else {
            layoutImageOption.setVisibility(View.GONE);
        }
    }

    private void setupWarningText() {
        String warningText;
        switch (deleteType) {
            case SINGLE_SCAN:
                warningText = "This action cannot be undone.";
                break;
            case ALL_SCANS:
                warningText = "⚠️ This will permanently delete your entire scan history and cannot be undone.";
                break;
            case MULTIPLE_SCANS:
            case FILTERED_SCANS:
                warningText = "This action cannot be undone. Consider exporting your data first.";
                break;
            default:
                warningText = "This action cannot be undone.";
        }

        tvWarningText.setText(warningText);

        // Make warning more prominent for destructive actions
        if (deleteType == DeleteType.ALL_SCANS) {
            tvWarningText.setTextColor(getResources().getColor(R.color.disease_red, null));
            tvWarningText.setTextSize(14f);
        }
    }

    private boolean hasImagesAttached() {
        if (scansToDelete == null) return false;

        return scansToDelete.stream()
                .anyMatch(scan -> scan.getImagePath() != null && !scan.getImagePath().isEmpty());
    }

    private int countImagesAttached() {
        if (scansToDelete == null) return 0;

        return (int) scansToDelete.stream()
                .filter(scan -> scan.getImagePath() != null && !scan.getImagePath().isEmpty())
                .count();
    }

    private void confirmDeletion() {
        boolean alsoDeleteImages = cbDeleteImages.isChecked();

        if (listener != null) {
            listener.onDeleteConfirmed(scansToDelete, alsoDeleteImages);
        }

        dismiss();
    }

    private void cancelDeletion() {
        if (listener != null) {
            listener.onDeleteCancelled();
        }

        dismiss();
    }

    /**
     * Convenience method for single scan deletion
     */
    public static void showSingleScanDeletion(
            androidx.fragment.app.FragmentManager fragmentManager,
            ScanResult scanResult,
            DeleteConfirmationListener listener) {

        DeleteConfirmationDialog dialog = newInstance(
                java.util.Collections.singletonList(scanResult),
                DeleteType.SINGLE_SCAN
        );
        dialog.setDeleteConfirmationListener(listener);
        dialog.show(fragmentManager, "delete_confirmation");
    }

    /**
     * Convenience method for multiple scan deletion
     */
    public static void showMultipleScanDeletion(
            androidx.fragment.app.FragmentManager fragmentManager,
            List<ScanResult> scanResults,
            DeleteConfirmationListener listener) {

        DeleteType type = scanResults.size() == 1 ?
                DeleteType.SINGLE_SCAN : DeleteType.MULTIPLE_SCANS;

        DeleteConfirmationDialog dialog = newInstance(scanResults, type);
        dialog.setDeleteConfirmationListener(listener);
        dialog.show(fragmentManager, "delete_confirmation");
    }

    /**
     * Convenience method for deleting all scans
     */
    public static void showDeleteAllScans(
            androidx.fragment.app.FragmentManager fragmentManager,
            List<ScanResult> allScans,
            DeleteConfirmationListener listener) {

        DeleteConfirmationDialog dialog = newInstance(allScans, DeleteType.ALL_SCANS);
        dialog.setDeleteConfirmationListener(listener);
        dialog.show(fragmentManager, "delete_confirmation");
    }

    /**
     * Get deletion summary for analytics/logging
     */
    public static String getDeletionSummary(List<ScanResult> deletedScans, boolean imagesDeleted) {
        if (deletedScans == null || deletedScans.isEmpty()) {
            return "No scans deleted";
        }

        int healthyCount = (int) deletedScans.stream().filter(ScanResult::isHealthy).count();
        int diseasedCount = deletedScans.size() - healthyCount;
        int imageCount = (int) deletedScans.stream()
                .filter(scan -> scan.getImagePath() != null && !scan.getImagePath().isEmpty())
                .count();

        StringBuilder summary = new StringBuilder();
        summary.append("Deleted ").append(deletedScans.size()).append(" scan");
        if (deletedScans.size() > 1) summary.append("s");

        if (healthyCount > 0 && diseasedCount > 0) {
            summary.append(" (").append(healthyCount).append(" healthy, ")
                    .append(diseasedCount).append(" diseased)");
        }

        if (imagesDeleted && imageCount > 0) {
            summary.append(" and ").append(imageCount).append(" image file");
            if (imageCount > 1) summary.append("s");
        }

        return summary.toString();
    }
}