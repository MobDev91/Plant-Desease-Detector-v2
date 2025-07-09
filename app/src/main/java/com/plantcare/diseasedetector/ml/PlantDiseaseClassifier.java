package com.plantcare.diseasedetector.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Real PyTorch Plant Disease Classifier
 * Loads and runs actual PyTorch Lite model for plant disease detection
 */
public class PlantDiseaseClassifier {
    private static final String TAG = "PlantClassifier";
    
    // Model configuration
    private static final int INPUT_SIZE = 224;
    private static final String MODEL_NAME = "plant_disease_model.ptl";
    
    // ImageNet normalization (CRITICAL for correct predictions)
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};
    
    // Plant classes (38 classes) - UPDATE this array to match your model's output
    private static final String[] CLASSES = {
        "Apple___Apple_scab",
        "Apple___Black_rot", 
        "Apple___Cedar_apple_rust",
        "Apple___healthy",
        "Blueberry___healthy",
        "Cherry_(including_sour)___Powdery_mildew",
        "Cherry_(including_sour)___healthy",
        "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot",
        "Corn_(maize)___Common_rust_",
        "Corn_(maize)___Northern_Leaf_Blight",
        "Corn_(maize)___healthy",
        "Grape___Black_rot",
        "Grape___Esca_(Black_Measles)",
        "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",
        "Grape___healthy",
        "Orange___Haunglongbing_(Citrus_greening)",
        "Peach___Bacterial_spot",
        "Peach___healthy",
        "Pepper,_bell___Bacterial_spot",
        "Pepper,_bell___healthy",
        "Potato___Early_blight",
        "Potato___Late_blight",
        "Potato___healthy",
        "Raspberry___healthy",
        "Soybean___healthy",
        "Squash___Powdery_mildew",
        "Strawberry___Leaf_scorch",
        "Strawberry___healthy",
        "Tomato___Bacterial_spot",
        "Tomato___Early_blight",
        "Tomato___Late_blight",
        "Tomato___Leaf_Mold",
        "Tomato___Septoria_leaf_spot",
        "Tomato___Spider_mites Two-spotted_spider_mite",
        "Tomato___Target_Spot",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
        "Tomato___Tomato_mosaic_virus",
        "Tomato___healthy"
    };
    
    private Module model;
    private Context context;
    
    public PlantDiseaseClassifier(Context context) {
        this.context = context;
    }
    
    /**
     * Load the PyTorch model from assets
     */
    public boolean loadModel() {
        try {
            Log.d(TAG, "Loading PyTorch model from assets...");
            
            // Copy model from assets to internal storage
            File modelFile = assetFilePath(MODEL_NAME);
            if (modelFile == null || !modelFile.exists()) {
                Log.e(TAG, "Model file not found or failed to copy");
                return false;
            }
            
            Log.d(TAG, "Model file size: " + modelFile.length() + " bytes");
            
            // Load the PyTorch Lite model
            model = LiteModuleLoader.load(modelFile.getAbsolutePath());
            
            // PyTorch Mobile Lite models are already in evaluation mode by default
            // model.eval() is not available in PyTorch Mobile - models are pre-optimized
            
            Log.i(TAG, "✅ PyTorch model loaded successfully");
            Log.i(TAG, "Model classes: " + CLASSES.length);
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading PyTorch model", e);
            return false;
        }
    }
    
    /**
     * Predict plant disease from bitmap using PyTorch model
     */
    public PredictionResult predict(Bitmap bitmap) {
        if (model == null) {
            Log.e(TAG, "PyTorch model not loaded");
            return null;
        }
        
        try {
            Log.d(TAG, "🔍 Starting PyTorch prediction...");
            long startTime = System.currentTimeMillis();
            
            // Preprocess image
            Tensor inputTensor = preprocessImage(bitmap);
            Log.d(TAG, "Input tensor shape: " + java.util.Arrays.toString(inputTensor.shape()));
            
            // Run inference
            IValue output = model.forward(IValue.from(inputTensor));
            Tensor outputTensor = output.toTensor();
            float[] rawScores = outputTensor.getDataAsFloatArray();
            
            Log.d(TAG, "Raw scores length: " + rawScores.length);
            
            // Apply softmax to get probabilities
            float[] probabilities = softmax(rawScores);
            
            // Find best prediction
            int bestIndex = argmax(probabilities);
            float confidence = probabilities[bestIndex];
            
            // Validate prediction
            if (bestIndex < 0 || bestIndex >= CLASSES.length) {
                Log.e(TAG, "Invalid prediction index: " + bestIndex);
                return null;
            }
            
            // Create result
            String className = CLASSES[bestIndex];
            String plantName = extractPlantName(className);
            String diseaseName = extractDiseaseName(className);
            boolean isHealthy = className.toLowerCase().contains("healthy");
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            Log.i(TAG, String.format("🎯 PyTorch Prediction: %s (%.1f%%) - %dms", 
                    className, confidence * 100, processingTime));
            
            // Log top 3 predictions for debugging
            logTopPredictions(probabilities, 3);
            
            return new PredictionResult(className, plantName, diseaseName, confidence, isHealthy);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ PyTorch prediction error", e);
            return null;
        }
    }
    
    /**
     * Preprocess image for PyTorch model input
     */
    private Tensor preprocessImage(Bitmap bitmap) {
        // Resize to model input size
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        
        // Convert to float array with ImageNet normalization
        float[] floatArray = new float[3 * INPUT_SIZE * INPUT_SIZE];
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        
        // Convert pixels to normalized float values in CHW format
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            
            // Extract RGB channels (0-255)
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;
            
            // Apply ImageNet normalization
            float rNorm = (r - MEAN[0]) / STD[0];
            float gNorm = (g - MEAN[1]) / STD[1];
            float bNorm = (b - MEAN[2]) / STD[2];
            
            // Store in CHW format (Channels-Height-Width)
            floatArray[i] = rNorm;                                    // R channel
            floatArray[INPUT_SIZE * INPUT_SIZE + i] = gNorm;         // G channel
            floatArray[2 * INPUT_SIZE * INPUT_SIZE + i] = bNorm;     // B channel
        }
        
        // Clean up
        if (resized != bitmap) {
            resized.recycle();
        }
        
        // Create tensor with shape [1, 3, 224, 224]
        return Tensor.fromBlob(floatArray, new long[]{1, 3, INPUT_SIZE, INPUT_SIZE});
    }
    
    /**
     * Apply softmax to convert raw scores to probabilities
     */
    private float[] softmax(float[] logits) {
        float[] probabilities = new float[logits.length];
        
        // Find max for numerical stability
        float max = Float.NEGATIVE_INFINITY;
        for (float logit : logits) {
            if (logit > max) max = logit;
        }
        
        // Compute exp and sum
        float sum = 0.0f;
        for (int i = 0; i < logits.length; i++) {
            probabilities[i] = (float) Math.exp(logits[i] - max);
            sum += probabilities[i];
        }
        
        // Normalize
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= sum;
        }
        
        return probabilities;
    }
    
    /**
     * Find index of maximum value (argmax)
     */
    private int argmax(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }
    
    /**
     * Log top N predictions for debugging
     */
    private void logTopPredictions(float[] probabilities, int topN) {
        try {
            // Create array of indices
            Integer[] indices = new Integer[probabilities.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }
            
            // Sort by probability (descending)
            java.util.Arrays.sort(indices, (a, b) -> Float.compare(probabilities[b], probabilities[a]));
            
            Log.d(TAG, "🏆 Top " + topN + " predictions:");
            for (int i = 0; i < Math.min(topN, indices.length); i++) {
                int idx = indices[i];
                String className = (idx < CLASSES.length) ? CLASSES[idx] : "Unknown_" + idx;
                Log.d(TAG, String.format("   %d. %s: %.3f (%.1f%%)", 
                        i + 1, className, probabilities[idx], probabilities[idx] * 100));
            }
        } catch (Exception e) {
            Log.w(TAG, "Error logging top predictions", e);
        }
    }
    
    /**
     * Extract plant name from class name
     */
    private String extractPlantName(String className) {
        if (className != null && className.contains("___")) {
            return className.split("___")[0].replace("_", " ");
        }
        return "Unknown Plant";
    }
    
    /**
     * Extract disease name from class name
     */
    private String extractDiseaseName(String className) {
        if (className != null && className.contains("___")) {
            String disease = className.split("___")[1];
            return disease.equals("healthy") ? "Healthy" : disease.replace("_", " ");
        }
        return "Unknown";
    }
    
    /**
     * Copy asset file to internal storage
     */
    private File assetFilePath(String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        
        // Check if file already exists and is valid
        if (file.exists() && file.length() > 0) {
            Log.d(TAG, "Using existing model file: " + file.getAbsolutePath());
            return file;
        }
        
        try (InputStream is = context.getAssets().open(assetName);
             FileOutputStream os = new FileOutputStream(file)) {
            
            byte[] buffer = new byte[4 * 1024];
            int read;
            long totalBytes = 0;
            
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                totalBytes += read;
            }
            os.flush();
            
            Log.d(TAG, "Model copied to: " + file.getAbsolutePath() + " (" + totalBytes + " bytes)");
            return file;
            
        } catch (IOException e) {
            Log.e(TAG, "Error copying asset: " + assetName, e);
            return null;
        }
    }
    
    /**
     * Release model resources
     */
    public void release() {
        if (model != null) {
            model = null;
            Log.d(TAG, "PyTorch model resources released");
        }
    }
    
    /**
     * Get model information
     */
    public String getModelInfo() {
        if (model != null) {
            return "PyTorch Model: " + MODEL_NAME + " (" + CLASSES.length + " classes)";
        }
        return "Model not loaded";
    }
    
    /**
     * Check if model is loaded
     */
    public boolean isModelLoaded() {
        return model != null;
    }
    
    /**
     * Prediction result class
     */
    public static class PredictionResult {
        public final String className;
        public final String plantName;
        public final String diseaseName;
        public final float confidence;
        public final boolean isHealthy;
        
        public PredictionResult(String className, String plantName, String diseaseName, 
                              float confidence, boolean isHealthy) {
            this.className = className;
            this.plantName = plantName;
            this.diseaseName = diseaseName;
            this.confidence = confidence;
            this.isHealthy = isHealthy;
        }
        
        @Override
        public String toString() {
            return String.format("PredictionResult{plant='%s', disease='%s', confidence=%.3f, healthy=%s}",
                    plantName, diseaseName, confidence, isHealthy);
        }
    }
}