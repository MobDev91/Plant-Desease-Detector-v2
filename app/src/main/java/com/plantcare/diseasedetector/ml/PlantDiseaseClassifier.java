package com.plantcare.diseasedetector.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Plant Disease Classifier - PyTorch Lite Model Version
 * 
 * Updated to use PyTorch Lite models (.ptl) for better mobile compatibility.
 * This provides optimized performance and smaller model size for mobile devices.
 * Includes extensive logging for debugging and performance monitoring.
 */
public class PlantDiseaseClassifier {

    private static final String TAG = "PlantDiseaseClassifier";
    private static final String MODEL_FILE_NAME = "plant_disease_model.ptl";
    private static final int INPUT_SIZE = 224;

    // CRITICAL: These normalization values MUST match your local testing setup
    // ImageNet standard values - verify these match your Python preprocessing
    private static final float[] NORM_MEAN_RGB = {0.485f, 0.456f, 0.406f};
    private static final float[] NORM_STD_RGB = {0.229f, 0.224f, 0.225f};

    private Context context;
    private Module model;
    private boolean isModelLoaded = false;

    // CRITICAL: Class order MUST match your model's output exactly
    // Verify this matches your local model's class order!
    private static final String[] CLASS_NAMES = {
            "Apple___Apple_scab",                                    // Index 0
            "Apple___Black_rot",                                     // Index 1
            "Apple___Cedar_apple_rust",                              // Index 2
            "Apple___healthy",                                       // Index 3
            "Blueberry___healthy",                                   // Index 4
            "Cherry_(including_sour)___Powdery_mildew",             // Index 5
            "Cherry_(including_sour)___healthy",                     // Index 6
            "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",   // Index 7
            "Corn_(maize)___Common_rust_",                          // Index 8
            "Corn_(maize)___Northern_Leaf_Blight",                  // Index 9
            "Corn_(maize)___healthy",                               // Index 10
            "Grape___Black_rot",                                     // Index 11
            "Grape___Esca_(Black_Measles)",                         // Index 12
            "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",           // Index 13
            "Grape___healthy",                                       // Index 14
            "Orange___Haunglongbing_(Citrus_greening)",             // Index 15
            "Peach___Bacterial_spot",                               // Index 16
            "Peach___healthy",                                       // Index 17
            "Pepper,_bell___Bacterial_spot",                        // Index 18
            "Pepper,_bell___healthy",                               // Index 19
            "Potato___Early_blight",                                // Index 20
            "Potato___Late_blight",                                 // Index 21
            "Potato___healthy",                                      // Index 22
            "Raspberry___healthy",                                   // Index 23
            "Soybean___healthy",                                     // Index 24
            "Squash___Powdery_mildew",                              // Index 25
            "Strawberry___Leaf_scorch",                             // Index 26
            "Strawberry___healthy",                                  // Index 27
            "Tomato___Bacterial_spot",                              // Index 28
            "Tomato___Early_blight",                                // Index 29
            "Tomato___Late_blight",                                 // Index 30
            "Tomato___Leaf_Mold",                                   // Index 31
            "Tomato___Septoria_leaf_spot",                          // Index 32
            "Tomato___Spider_mites Two-spotted_spider_mite",        // Index 33
            "Tomato___Target_Spot",                                 // Index 34
            "Tomato___Tomato_Yellow_Leaf_Curl_Virus",              // Index 35
            "Tomato___Tomato_mosaic_virus",                         // Index 36
            "Tomato___healthy",                                      // Index 37
            "Background_without_leaves"                              // Index 38
    };

    public PlantDiseaseClassifier(Context context) {
        this.context = context;
    }

    /**
     * Get the second highest score for comparison
     */
    private float getSecondHighestScore(float[] scores, int excludeIndex) {
        float secondHighest = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < scores.length; i++) {
            if (i != excludeIndex && scores[i] > secondHighest) {
                secondHighest = scores[i];
            }
        }
        return secondHighest;
    }

    /**
     * Load the PyTorch model with extensive diagnostics
     */
    public boolean loadModel() {
        try {
            Log.i(TAG, "=== MODEL LOADING DIAGNOSTICS ===");
            
            String modelPath = assetFilePath(MODEL_FILE_NAME);
            if (modelPath == null) {
                Log.e(TAG, "Model file not found in assets: " + MODEL_FILE_NAME);
                return false;
            }

            File modelFile = new File(modelPath);
            Log.i(TAG, "Model file path: " + modelPath);
            Log.i(TAG, "Model file size: " + modelFile.length() + " bytes");
            Log.i(TAG, "Model file exists: " + modelFile.exists());

            model = LiteModuleLoader.load(modelPath);
            isModelLoaded = true;
            
            Log.i(TAG, "PyTorch Lite model loaded successfully (.ptl format)");
            Log.i(TAG, "Model type: PyTorch Mobile Lite version");
            Log.i(TAG, "Expected class count: " + CLASS_NAMES.length);
            Log.i(TAG, "Input size: " + INPUT_SIZE + "x" + INPUT_SIZE);
            Log.i(TAG, "Normalization mean: " + Arrays.toString(NORM_MEAN_RGB));
            Log.i(TAG, "Normalization std: " + Arrays.toString(NORM_STD_RGB));
            Log.i(TAG, "=== MODEL LOADING COMPLETE ===");
            
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error loading PyTorch Lite model (.ptl format)", e);
            
            // Check for specific CUDA error
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("CUDA")) {
                Log.e(TAG, "======================== CUDA ERROR DETECTED ========================");
                Log.e(TAG, "Your model contains CUDA operations that are not supported on mobile!");
                Log.e(TAG, "SOLUTION: Convert your model to mobile-compatible format:");
                Log.e(TAG, "1. Load your model on CPU: model = torch.load('model.pt', map_location='cpu')");
                Log.e(TAG, "2. Trace for mobile: traced = torch.jit.trace(model, sample_input)");
                Log.e(TAG, "3. Save mobile version: traced._save_for_lite_interpreter('mobile_model.ptl')");
                Log.e(TAG, "=====================================================================");
            } else {
                Log.e(TAG, "Make sure you're using the PyTorch Mobile Lite library");
                Log.e(TAG, "Required dependencies: org.pytorch:pytorch_android_lite:1.12.2");
            }
            
            isModelLoaded = false;
            return false;
        }
    }

    /**
     * DIAGNOSTIC: Predict with extensive logging
     */
    public ClassificationResult predict(Bitmap bitmap) {
        if (!isModelLoaded || model == null) {
            Log.e(TAG, "PyTorch model not loaded");
            return null;
        }

        if (bitmap == null) {
            Log.e(TAG, "Null bitmap provided for prediction");
            return null;
        }

        try {
            Log.i(TAG, "=== PREDICTION DIAGNOSTICS START ===");
            Log.i(TAG, "Input bitmap dimensions: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            Log.i(TAG, "Input bitmap config: " + bitmap.getConfig());
            
            return runDetailedPrediction(bitmap);

        } catch (Exception e) {
            Log.e(TAG, "Error during prediction", e);
            return null;
        }
    }

    /**
     * DIAGNOSTIC: Detailed prediction with step-by-step logging
     */
    private ClassificationResult runDetailedPrediction(Bitmap bitmap) {
        try {
            // Step 1: Resize image
            Log.d(TAG, "Step 1: Resizing image to " + INPUT_SIZE + "x" + INPUT_SIZE);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
            if (resizedBitmap == null) {
                Log.e(TAG, "Failed to resize bitmap");
                return null;
            }
            Log.d(TAG, "Resized bitmap: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

            // Step 2: Log some pixel values for debugging
            logSamplePixels(resizedBitmap);

            // Step 3: Convert to tensor
            Log.d(TAG, "Step 3: Converting bitmap to tensor");
            Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                    resizedBitmap,
                    NORM_MEAN_RGB,
                    NORM_STD_RGB
            );
            
            long[] tensorShape = inputTensor.shape();
            Log.d(TAG, "Input tensor shape: " + Arrays.toString(tensorShape));
            Log.d(TAG, "Expected tensor shape: [1, 3, 224, 224]");

            // Step 4: Run model inference
            Log.d(TAG, "Step 4: Running model inference");
            long startTime = System.currentTimeMillis();
            
            Tensor outputTensor = model.forward(IValue.from(inputTensor)).toTensor();
            
            long inferenceTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Inference completed in " + inferenceTime + "ms");
            
            // Step 5: Get output and analyze
            float[] scores = outputTensor.getDataAsFloatArray();
            long[] outputShape = outputTensor.shape();
            
            Log.d(TAG, "Output tensor shape: " + Arrays.toString(outputShape));
            Log.d(TAG, "Output scores length: " + (scores != null ? scores.length : "null"));
            Log.d(TAG, "Expected scores length: " + CLASS_NAMES.length);
            
            if (scores == null || scores.length == 0) {
                Log.e(TAG, "Invalid model output - no scores returned");
                return null;
            }

            if (scores.length != CLASS_NAMES.length) {
                Log.e(TAG, "CRITICAL: Score length mismatch! Got " + scores.length + ", expected " + CLASS_NAMES.length);
                Log.e(TAG, "This indicates a class label ordering problem!");
            }

            // Step 6: Log raw scores (ENHANCED DEBUGGING)
            logTopRawScores(scores, 10); // Show top 10 instead of 5

            // Enhanced debugging - Log first 15 scores to see distribution
            Log.d(TAG, "=== DETAILED SCORES DEBUG ===");
            for (int i = 0; i < Math.min(15, scores.length); i++) {
                String className = (i < CLASS_NAMES.length) ? CLASS_NAMES[i] : "Unknown_" + i;
                Log.d(TAG, String.format("Class %2d (%-30s): %.6f", i, className, scores[i]));
            }

            // Check if all scores are identical (frozen model indicator)
            boolean allSame = true;
            float firstScore = scores[0];
            float minScore = firstScore;
            float maxScore = firstScore;
            
            for (int i = 1; i < Math.min(20, scores.length); i++) {
                minScore = Math.min(minScore, scores[i]);
                maxScore = Math.max(maxScore, scores[i]);
                if (Math.abs(scores[i] - firstScore) > 1e-6) {
                    allSame = false;
                }
            }
            
            Log.d(TAG, String.format("Score statistics: Min=%.6f, Max=%.6f, Range=%.6f", 
                minScore, maxScore, maxScore - minScore));
            
            if (allSame) {
                Log.e(TAG, "🚨 CRITICAL: All scores are identical! Model weights may be frozen.");
                Log.e(TAG, "Identical score value: " + firstScore);
                Log.e(TAG, "This indicates a model conversion problem.");
            } else if (maxScore - minScore < 0.01) {
                Log.w(TAG, "⚠️  WARNING: Very small score range - model may not be learning properly");
            } else {
                Log.d(TAG, "✅ Scores vary appropriately - model is responsive");
            }
            Log.d(TAG, "=== END DETAILED DEBUG ===");

            // Step 7: Find max and apply softmax
            int maxIndex = 0;
            float maxScoreValue = scores[0];
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > maxScoreValue) {
                    maxScoreValue = scores[i];
                    maxIndex = i;
                }
            }
            
            // Log the winning prediction details (after finding maxIndex)
            Log.d(TAG, "=== PREDICTION ANALYSIS ===");
            Log.d(TAG, String.format("Winning index: %d", maxIndex));
            Log.d(TAG, String.format("Winning class: %s", CLASS_NAMES[maxIndex]));
            Log.d(TAG, String.format("Raw winning score: %.6f", maxScoreValue));
            Log.d(TAG, String.format("Score margin over 2nd place: %.6f", 
                maxScoreValue - getSecondHighestScore(scores, maxIndex)));
            Log.d(TAG, "=== END PREDICTION ANALYSIS ===");

            float probability = softmax(scores, maxIndex);

            // Step 8: Validate result
            if (maxIndex < 0 || maxIndex >= CLASS_NAMES.length) {
                Log.e(TAG, "Invalid class index: " + maxIndex);
                return null;
            }

            String predictedClass = CLASS_NAMES[maxIndex];
            
            // Step 9: Log final result
            Log.i(TAG, "=== PREDICTION RESULT ===");
            Log.i(TAG, "Predicted class: " + predictedClass);
            Log.i(TAG, "Class index: " + maxIndex);
            Log.i(TAG, "Raw score: " + maxScoreValue);
            Log.i(TAG, "Softmax probability: " + probability);
            Log.i(TAG, "Confidence percentage: " + String.format("%.1f%%", probability * 100));
            Log.i(TAG, "=== PREDICTION DIAGNOSTICS END ===");
            
            return new ClassificationResult(predictedClass, maxIndex, probability);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in detailed prediction", e);
            return null;
        }
    }

    /**
     * DIAGNOSTIC: Log sample pixel values with enhanced debugging
     */
    private void logSamplePixels(Bitmap bitmap) {
        try {
            Log.d(TAG, "=== ENHANCED PIXEL ANALYSIS ===");
            int centerX = bitmap.getWidth() / 2;
            int centerY = bitmap.getHeight() / 2;
            
            // Sample multiple pixels for better analysis
            int[] positions = {
                bitmap.getPixel(centerX, centerY),           // Center
                bitmap.getPixel(0, 0),                       // Top-left
                bitmap.getPixel(bitmap.getWidth()-1, 0),     // Top-right
                bitmap.getPixel(0, bitmap.getHeight()-1),    // Bottom-left
                bitmap.getPixel(bitmap.getWidth()-1, bitmap.getHeight()-1) // Bottom-right
            };
            String[] posNames = {"Center", "Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right"};
            
            float totalR = 0, totalG = 0, totalB = 0;
            
            for (int i = 0; i < positions.length; i++) {
                int pixel = positions[i];
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                totalR += r; totalG += g; totalB += b;
                
                float normR = (r / 255.0f - NORM_MEAN_RGB[0]) / NORM_STD_RGB[0];
                float normG = (g / 255.0f - NORM_MEAN_RGB[1]) / NORM_STD_RGB[1];
                float normB = (b / 255.0f - NORM_MEAN_RGB[2]) / NORM_STD_RGB[2];
                
                Log.d(TAG, String.format("%s pixel - RGB: (%d,%d,%d) -> Normalized: (%.3f,%.3f,%.3f)", 
                    posNames[i], r, g, b, normR, normG, normB));
            }
            
            // Calculate average pixel values
            float avgR = totalR / positions.length;
            float avgG = totalG / positions.length;
            float avgB = totalB / positions.length;
            
            Log.d(TAG, String.format("Average RGB: (%.1f, %.1f, %.1f)", avgR, avgG, avgB));
            
            // Check for suspicious patterns
            if (avgR == avgG && avgG == avgB) {
                Log.w(TAG, "⚠️  WARNING: Image appears to be grayscale (R=G=B)");
            }
            
            if (avgR < 10 && avgG < 10 && avgB < 10) {
                Log.w(TAG, "⚠️  WARNING: Image appears very dark - may affect predictions");
            }
            
            if (avgR > 245 && avgG > 245 && avgB > 245) {
                Log.w(TAG, "⚠️  WARNING: Image appears very bright - may affect predictions");
            }
            
            Log.d(TAG, "Current normalization: ImageNet standard");
            Log.d(TAG, "Mean: [" + NORM_MEAN_RGB[0] + ", " + NORM_MEAN_RGB[1] + ", " + NORM_MEAN_RGB[2] + "]");
            Log.d(TAG, "Std:  [" + NORM_STD_RGB[0] + ", " + NORM_STD_RGB[1] + ", " + NORM_STD_RGB[2] + "]");
            Log.d(TAG, "=== END ENHANCED PIXEL ANALYSIS ===");
            
        } catch (Exception e) {
            Log.w(TAG, "Error in enhanced pixel analysis", e);
        }
    }

    /**
     * DIAGNOSTIC: Log top raw scores before softmax
     */
    private void logTopRawScores(float[] scores, int topN) {
        try {
            Log.d(TAG, "=== TOP " + topN + " RAW SCORES ===");
            
            Integer[] indices = new Integer[scores.length];
            for (int i = 0; i < scores.length; i++) {
                indices[i] = i;
            }
            
            Arrays.sort(indices, (a, b) -> Float.compare(scores[b], scores[a]));
            
            for (int i = 0; i < Math.min(topN, indices.length); i++) {
                int idx = indices[i];
                String className = (idx < CLASS_NAMES.length) ? CLASS_NAMES[idx] : "UNKNOWN_INDEX_" + idx;
                Log.d(TAG, String.format("Rank %d: %s (index %d) = %.6f", 
                    i + 1, className, idx, scores[idx]));
            }
            
            Log.d(TAG, "=== END TOP SCORES ===");
            
        } catch (Exception e) {
            Log.w(TAG, "Error logging top scores", e);
        }
    }

    /**
     * Apply softmax with diagnostic logging
     */
    private float softmax(float[] scores, int targetIndex) {
        if (scores == null || scores.length == 0 || targetIndex < 0 || targetIndex >= scores.length) {
            Log.w(TAG, "Invalid softmax parameters");
            return 0.0f;
        }

        try {
            for (float score : scores) {
                if (Float.isNaN(score) || Float.isInfinite(score)) {
                    Log.w(TAG, "Invalid score detected in softmax");
                    return 0.0f;
                }
            }

            double maxScore = scores[0];
            for (float score : scores) {
                maxScore = Math.max(maxScore, score);
            }

            double sum = 0.0;
            for (float score : scores) {
                sum += Math.exp(score - maxScore);
            }

            if (sum == 0.0 || Double.isNaN(sum) || Double.isInfinite(sum)) {
                Log.w(TAG, "Invalid softmax sum: " + sum);
                return 0.0f;
            }

            double result = Math.exp(scores[targetIndex] - maxScore) / sum;
            
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                Log.w(TAG, "Softmax produced invalid result");
                return 0.0f;
            }

            float finalResult = (float) Math.max(0.0, Math.min(1.0, result));
            
            Log.d(TAG, "Softmax calculation:");
            Log.d(TAG, "  Raw score: " + scores[targetIndex]);
            Log.d(TAG, "  Max score: " + maxScore);
            Log.d(TAG, "  Sum: " + sum);
            Log.d(TAG, "  Final probability: " + finalResult);
            
            return finalResult;

        } catch (Exception e) {
            Log.w(TAG, "Error in softmax calculation", e);
            return 0.0f;
        }
    }

    private String assetFilePath(String assetName) {
        File file = new File(context.getFilesDir(), assetName);

        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream inputStream = context.getAssets().open(assetName);
             FileOutputStream outputStream = new FileOutputStream(file)) {

            byte[] buffer = new byte[4 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return file.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error copying asset file: " + assetName, e);
            return null;
        }
    }

    public boolean isModelLoaded() {
        return isModelLoaded;
    }

    public static String[] getClassNames() {
        return Arrays.copyOf(CLASS_NAMES, CLASS_NAMES.length);
    }

    public static int getNumClasses() {
        return CLASS_NAMES.length;
    }

    public void release() {
        if (model != null) {
            model = null;
        }
        isModelLoaded = false;
        Log.i(TAG, "Model resources released");
    }

    /**
     * Classification Result Class
     */
    public static class ClassificationResult {
        public final String className;
        public final int classIndex;
        public final float confidence;

        public ClassificationResult(String className, int classIndex, float confidence) {
            this.className = className;
            this.classIndex = classIndex;
            this.confidence = confidence;
        }

        public String getFormattedClassName() {
            return className.replace("___", ": ").replace("_", " ");
        }

        public String getPlantType() {
            if (isBackground()) {
                return "No Plant Detected";
            }
            
            String[] parts = className.split("___");
            if (parts.length > 0) {
                return parts[0].replace("_", " ");
            }
            return "Unknown Plant";
        }

        public String getDiseaseType() {
            if (isBackground()) {
                return "Background Image";
            }
            
            String[] parts = className.split("___");
            if (parts.length > 1) {
                String disease = parts[1].replace("_", " ");
                return disease.equals("healthy") ? "Healthy" : disease;
            }
            return "Unknown";
        }

        public boolean isBackground() {
            return className.contains("Background") || className.contains("background");
        }

        public boolean isHealthy() {
            return !isBackground() && className.toLowerCase().contains("healthy");
        }

        public String getConfidencePercentage() {
            return String.format("%.1f%%", confidence * 100);
        }

        public String getSeverityLevel() {
            if (isBackground()) {
                return "No Plant";
            }
            
            if (isHealthy()) {
                return "Healthy";
            }

            if (confidence >= 0.8f) {
                return "High Confidence";
            } else if (confidence >= 0.6f) {
                return "Medium Confidence";
            } else {
                return "Low Confidence";
            }
        }

        @Override
        public String toString() {
            return String.format("ClassificationResult{className='%s', confidence=%.2f, plantType='%s', diseaseType='%s', isBackground=%s}",
                    className, confidence, getPlantType(), getDiseaseType(), isBackground());
        }
    }
}
