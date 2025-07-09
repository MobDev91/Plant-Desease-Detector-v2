package com.plantcare.diseasedetector.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import com.google.common.util.concurrent.ListenableFuture;
import com.plantcare.diseasedetector.R;
import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.models.ScanResult;
import com.plantcare.diseasedetector.ml.PlantDiseaseClassifier;

import com.plantcare.diseasedetector.ui.results.ResultsActivity;
import com.plantcare.diseasedetector.utils.ImageUtils;
import com.plantcare.diseasedetector.utils.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Camera Activity for capturing plant images and performing AI disease detection
 * FIXED: Resolves image processing error with proper null checks and validation
 */
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CameraActivity";
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int PERMISSION_REQUEST_CODE = 300;

    // UI Components
    private PreviewView cameraPreview;
    private ImageButton btnBack, btnFlash, btnCloseInstructions;
    private CardView btnCapture, btnGallery, btnSwitchCamera, cardInstructions;
    private View loadingOverlay, layoutProgress, indicatorAiReady;
    private TextView tvAiStatus, tvLoadingText, tvProgressText;
    private ProgressBar progressBar;

    // Camera Components
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Camera camera;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private boolean isFlashEnabled = false;

    // AI Model
    private PlantDiseaseClassifier plantClassifier;
    private boolean isModelLoaded = false;

    // Threading
    private ExecutorService cameraExecutor;
    private Handler mainHandler;

    // Database
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Initialize components
        initializeViews();
        initializeExecutors();
        initializeDatabase();

        // Setup click listeners
        setupClickListeners();

        // Check permissions and start camera
        if (hasRequiredPermissions()) {
            startCamera();
            loadAIModel();
        } else {
            requestPermissions();
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        try {
            cameraPreview = findViewById(R.id.camera_preview);
            btnBack = findViewById(R.id.btn_back);
            btnFlash = findViewById(R.id.btn_flash);
            btnCloseInstructions = findViewById(R.id.btn_close_instructions);
            btnCapture = findViewById(R.id.btn_capture);
            btnGallery = findViewById(R.id.btn_gallery);
            btnSwitchCamera = findViewById(R.id.btn_switch_camera);
            cardInstructions = findViewById(R.id.card_instructions);
            loadingOverlay = findViewById(R.id.loading_overlay);
            layoutProgress = findViewById(R.id.layout_progress);
            indicatorAiReady = findViewById(R.id.indicator_ai_ready);
            tvAiStatus = findViewById(R.id.tv_ai_status);
            tvLoadingText = findViewById(R.id.tv_loading_text);
            tvProgressText = findViewById(R.id.tv_progress_text);
            progressBar = findViewById(R.id.progress_bar);

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    /**
     * Initialize executors and handlers
     */
    private void initializeExecutors() {
        cameraExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialize database
     */
    private void initializeDatabase() {
        database = AppDatabase.getInstance(this);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(this);
        if (btnFlash != null) btnFlash.setOnClickListener(this);
        if (btnCloseInstructions != null) btnCloseInstructions.setOnClickListener(this);
        if (btnCapture != null) btnCapture.setOnClickListener(this);
        if (btnGallery != null) btnGallery.setOnClickListener(this);
        if (btnSwitchCamera != null) btnSwitchCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == null) return;

        int id = v.getId();

        if (id == R.id.btn_back) {
            finish();
        } else if (id == R.id.btn_flash) {
            toggleFlash();
        } else if (id == R.id.btn_close_instructions) {
            if (cardInstructions != null) {
                cardInstructions.setVisibility(View.GONE);
            }
        } else if (id == R.id.btn_capture) {
            capturePhoto();
        } else if (id == R.id.btn_gallery) {
            openGallery();
        } else if (id == R.id.btn_switch_camera) {
            switchCamera();
        }
    }

    /**
     * Check if required permissions are granted
     */
    private boolean hasRequiredPermissions() {
        return PermissionUtils.hasCameraPermission(this) &&
                PermissionUtils.hasStoragePermission(this);
    }

    /**
     * Request required permissions
     */
    private void requestPermissions() {
        String[] permissions = PermissionUtils.getRequiredPermissions();
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    /**
     * Start camera preview
     */
    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                showToast("Failed to start camera");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Bind camera preview
     */
    private void bindPreview(ProcessCameraProvider cameraProvider) {
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // Image capture
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        // Unbind all use cases before rebinding
        cameraProvider.unbindAll();

        try {
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);

            // Setup camera controls
            setupCameraControls();

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            showToast("Camera initialization failed");
        }
    }

    /**
     * Setup camera controls (flash, etc.)
     */
    private void setupCameraControls() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            if (btnFlash != null) {
                btnFlash.setVisibility(View.VISIBLE);
                updateFlashIcon();
            }
        } else {
            if (btnFlash != null) {
                btnFlash.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Toggle flash on/off
     */
    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashEnabled = !isFlashEnabled;
            camera.getCameraControl().enableTorch(isFlashEnabled);
            updateFlashIcon();
        }
    }

    /**
     * Update flash icon based on current state
     */
    private void updateFlashIcon() {
        if (btnFlash != null) {
            if (isFlashEnabled) {
                btnFlash.setImageResource(R.drawable.ic_flash_on);
            } else {
                btnFlash.setImageResource(R.drawable.ic_flash_off);
            }
        }
    }

    /**
     * Switch between front and back camera
     */
    private void switchCamera() {
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            bindPreview(cameraProvider);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error switching camera", e);
        }
    }

    /**
     * Capture photo and process with AI
     */
    private void capturePhoto() {
        if (imageCapture == null) {
            showToast("Camera not ready");
            return;
        }

        if (!isModelLoaded) {
            showToast("PyTorch model not ready - please wait for loading to complete");
            Log.w(TAG, "Photo capture blocked - PyTorch model not loaded");
            return;
        }

        // Show progress
        showProgress("Capturing image...");

        // Create output file
        File photoFile = ImageUtils.createImageFile(this);
        if (photoFile == null) {
            hideProgress();
            showToast("Failed to create image file");
            return;
        }

        // Setup image capture output
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Take picture
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "Photo capture succeeded: " + photoFile.getAbsolutePath());
                        processImage(photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        hideProgress();
                        showToast("Photo capture failed");
                    }
                }
        );
    }

    /**
     * Open gallery to select image
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    /**
     * Load AI model in background
     */
    private void loadAIModel() {
        showLoadingOverlay("Loading MOCK AI model...");
        updateAIStatus("Loading...", false);

        cameraExecutor.execute(() -> {
            try {
                plantClassifier = new PlantDiseaseClassifier(this);
                boolean loaded = plantClassifier.loadModel();

                mainHandler.post(() -> {
                    hideLoadingOverlay();
                    if (loaded) {
                        isModelLoaded = true;
                        updateAIStatus("AI Model Ready", true);
                        showToast("PyTorch model loaded successfully!");
                        Log.i(TAG, "PyTorch model successfully loaded and ready for predictions");
                        
                        Log.i(TAG, "Model loaded successfully and ready for use");
                    } else {
                        isModelLoaded = false;
                        updateAIStatus("Model Failed", false);
                        showToast("Failed to load PyTorch model - check model file");
                        Log.e(TAG, "Failed to load the PyTorch model from assets");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading PyTorch model", e);
                mainHandler.post(() -> {
                    isModelLoaded = false;
                    hideLoadingOverlay();
                    updateAIStatus("Model Error", false);
                    showToast("PyTorch model loading error: " + e.getMessage());
                });
            }
        });
    }

    /**
     * FIXED: Process captured image with proper background handling
     */
    private void processImage(String imagePath) {
        if (!isModelLoaded || plantClassifier == null) {
            hideProgress();
            showToast("PyTorch model not loaded - cannot analyze image");
            Log.e(TAG, "Attempted to process image without loaded PyTorch model");
            return;
        }

        showProgress("Analyzing image...");

        cameraExecutor.execute(() -> {
            try {
                // Load and validate image
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap == null) {
                    mainHandler.post(() -> {
                        hideProgress();
                        showToast("Failed to load image");
                    });
                    return;
                }

                Log.d(TAG, "Processing image: " + imagePath + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");

                // Classify with AI model
                PlantDiseaseClassifier.PredictionResult result = plantClassifier.predict(bitmap);

                if (result != null) {
                    // Create and save scan result
                    ScanResult scanResult = createScanResult(imagePath, result);

                    try {
                        long scanId = database.scanResultDao().insertScanResult(scanResult);
                        scanResult.setId((int) scanId);

                        mainHandler.post(() -> {
                            hideProgress();
                            openResults(scanResult);
                        });
                    } catch (Exception dbError) {
                        Log.e(TAG, "Database error when saving scan result", dbError);
                        mainHandler.post(() -> {
                            hideProgress();
                            showToast("Failed to save scan result");
                        });
                    }
                } else {
                    Log.e(TAG, "AI prediction failed");
                    mainHandler.post(() -> {
                        hideProgress();
                        showToast("AI analysis failed - please try again");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing image", e);
                mainHandler.post(() -> {
                    hideProgress();
                    showToast("Image processing error");
                });
            }
        });
    }



    /**
     * Create ScanResult from prediction result
     */
    private ScanResult createScanResult(String imagePath, PlantDiseaseClassifier.PredictionResult result) {
        ScanResult scanResult = new ScanResult();

        // Set basic fields
        scanResult.setImagePath(imagePath != null ? imagePath : "");
        scanResult.setPredictedClass(result.className);
        scanResult.setPredictedIndex(0); // Not needed for minimal implementation
        scanResult.setConfidence(result.confidence);
        scanResult.setPlantName(result.plantName);
        scanResult.setDiseaseName(result.diseaseName);
        scanResult.setHealthy(result.isHealthy);

        Log.d(TAG, "Created ScanResult: " + scanResult.toString());
        return scanResult;
    }



    /**
     * Open results activity
     */
    private void openResults(ScanResult scanResult) {
        try {
            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra("scan_result_id", scanResult.getId());
            startActivity(intent);
            finish(); // Close camera activity
        } catch (Exception e) {
            Log.e(TAG, "Error opening results activity", e);
            showToast("Failed to open results");
        }
    }

    /**
     * Handle gallery image selection
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                processGalleryImage(selectedImageUri);
            }
        }
    }

    /**
     * FIXED: Process image selected from gallery with background detection
     */
    private void processGalleryImage(Uri imageUri) {
        if (!isModelLoaded) {
            showToast("PyTorch model not ready - cannot process gallery image");
            Log.w(TAG, "Gallery processing blocked - PyTorch model not loaded");
            return;
        }

        showProgress("Processing gallery image...");

        cameraExecutor.execute(() -> {
            try {
                // Copy image to app directory
                String imagePath = saveGalleryImage(imageUri);
                if (imagePath != null) {
                    processImage(imagePath); // This will now handle background detection properly
                } else {
                    mainHandler.post(() -> {
                        hideProgress();
                        showToast("Failed to process gallery image");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing gallery image", e);
                mainHandler.post(() -> {
                    hideProgress();
                    showToast("Gallery image processing error");
                });
            }
        });
    }

    /**
     * Save gallery image to app directory
     */
    private String saveGalleryImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            File outputFile = ImageUtils.createImageFile(this);
            if (outputFile == null) return null;

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return outputFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error saving gallery image", e);
            return null;
        }
    }

    /**
     * Show progress indicator - FIXED: Ensure UI thread
     */
    private void showProgress(String message) {
        runOnUiThread(() -> {
            try {
                if (layoutProgress != null) {
                    layoutProgress.setVisibility(View.VISIBLE);
                }
                if (tvProgressText != null) {
                    tvProgressText.setText(message);
                }
                if (btnCapture != null) {
                    btnCapture.setEnabled(false);
                }
                if (btnGallery != null) {
                    btnGallery.setEnabled(false);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error showing progress", e);
            }
        });
    }

    /**
     * Hide progress indicator - FIXED: Ensure UI thread
     */
    private void hideProgress() {
        runOnUiThread(() -> {
            try {
                if (layoutProgress != null) {
                    layoutProgress.setVisibility(View.GONE);
                }
                if (btnCapture != null) {
                    btnCapture.setEnabled(true);
                }
                if (btnGallery != null) {
                    btnGallery.setEnabled(true);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error hiding progress", e);
            }
        });
    }

    /**
     * Show loading overlay - FIXED: Ensure UI thread
     */
    private void showLoadingOverlay(String message) {
        runOnUiThread(() -> {
            try {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.VISIBLE);
                }
                if (tvLoadingText != null) {
                    tvLoadingText.setText(message);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error showing loading overlay", e);
            }
        });
    }

    /**
     * Hide loading overlay - FIXED: Ensure UI thread
     */
    private void hideLoadingOverlay() {
        runOnUiThread(() -> {
            try {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error hiding loading overlay", e);
            }
        });
    }

    /**
     * Update AI status indicator - FIXED: Ensure UI thread
     */
    private void updateAIStatus(String status, boolean ready) {
        runOnUiThread(() -> {
            try {
                if (tvAiStatus != null) {
                    tvAiStatus.setText(status);
                }
                if (indicatorAiReady != null) {
                    if (ready) {
                        indicatorAiReady.setBackgroundTintList(
                                ContextCompat.getColorStateList(this, R.color.healthy_green));
                    } else {
                        indicatorAiReady.setBackgroundTintList(
                                ContextCompat.getColorStateList(this, R.color.warning_orange));
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error updating AI status", e);
            }
        });
    }

    /**
     * Show toast message - FIXED: Ensure UI thread
     */
    private void showToast(String message) {
        runOnUiThread(() -> {
            try {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.w(TAG, "Error showing toast", e);
            }
        });
    }

    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startCamera();
                loadAIModel();
            } else {
                showToast("Camera permissions are required");
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (cameraExecutor != null) {
                cameraExecutor.shutdown();
            }
            if (plantClassifier != null) {
                plantClassifier.release();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error in onDestroy", e);
        }
    }
}