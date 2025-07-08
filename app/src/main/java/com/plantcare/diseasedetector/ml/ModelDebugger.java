package com.plantcare.diseasedetector.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DEBUGGING CLASS: Test if the model is "frozen" or always predicting the same thing
 * 
 * This class helps diagnose why your model might always predict the same class
 * regardless of input image differences.
 */
public class ModelDebugger {

    private static final String TAG = "ModelDebugger";
    private static final float[] NORM_MEAN_RGB = {0.485f, 0.456f, 0.406f};
    private static final float[] NORM_STD_RGB = {0.229f, 0.224f, 0.225f};

    public static class DebugResult {
        public final List<Integer> predictions;
        public final List<Float> confidences;
        public final List<String> testNames;
        public final boolean isModelResponsive;
        public final float outputVariance;

        public DebugResult(List<Integer> predictions, List<Float> confidences, 
                          List<String> testNames, boolean isModelResponsive, float outputVariance) {
            this.predictions = predictions;
            this.confidences = confidences;
            this.testNames = testNames;
            this.isModelResponsive = isModelResponsive;
            this.outputVariance = outputVariance;
        }
    }

    /**
     * CRITICAL DEBUGGING: Test if model responds differently to different inputs
     */
    public static DebugResult testModelResponsiveness(Context context) {
        Log.i(TAG, "🔬 STARTING MODEL RESPONSIVENESS TEST");
        Log.i(TAG, "==========================================");

        List<Integer> predictions = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<String> testNames = new ArrayList<>();
        List<float[]> allOutputs = new ArrayList<>();

        try {
            // Load the model
            PlantDiseaseClassifier classifier = new PlantDiseaseClassifier(context);
            if (!classifier.loadModel()) {
                Log.e(TAG, "❌ Failed to load model for debugging");
                return new DebugResult(predictions, confidences, testNames, false, 0.0f);
            }

            // Test 1: Pure black image
            Log.i(TAG, "🧪 Test 1: Pure black image");
            Bitmap blackImage = createSolidColorBitmap(224, 224, Color.BLACK);
            PlantDiseaseClassifier.ClassificationResult result1 = classifier.predict(blackImage);
            if (result1 != null) {
                predictions.add(result1.classIndex);
                confidences.add(result1.confidence);
                testNames.add("Black Image");
                Log.i(TAG, String.format("   Result: Class %d, Confidence: %.4f", 
                    result1.classIndex, result1.confidence));
            }

            // Test 2: Pure white image
            Log.i(TAG, "🧪 Test 2: Pure white image");
            Bitmap whiteImage = createSolidColorBitmap(224, 224, Color.WHITE);
            PlantDiseaseClassifier.ClassificationResult result2 = classifier.predict(whiteImage);
            if (result2 != null) {
                predictions.add(result2.classIndex);
                confidences.add(result2.confidence);
                testNames.add("White Image");
                Log.i(TAG, String.format("   Result: Class %d, Confidence: %.4f", 
                    result2.classIndex, result2.confidence));
            }

            // Test 3: Pure red image
            Log.i(TAG, "🧪 Test 3: Pure red image");
            Bitmap redImage = createSolidColorBitmap(224, 224, Color.RED);
            PlantDiseaseClassifier.ClassificationResult result3 = classifier.predict(redImage);
            if (result3 != null) {
                predictions.add(result3.classIndex);
                confidences.add(result3.confidence);
                testNames.add("Red Image");
                Log.i(TAG, String.format("   Result: Class %d, Confidence: %.4f", 
                    result3.classIndex, result3.confidence));
            }

            // Test 4: Pure green image
            Log.i(TAG, "🧪 Test 4: Pure green image");
            Bitmap greenImage = createSolidColorBitmap(224, 224, Color.GREEN);
            PlantDiseaseClassifier.ClassificationResult result4 = classifier.predict(greenImage);
            if (result4 != null) {
                predictions.add(result4.classIndex);
                confidences.add(result4.confidence);
                testNames.add("Green Image");
                Log.i(TAG, String.format("   Result: Class %d, Confidence: %.4f", 
                    result4.classIndex, result4.confidence));
            }

            // Test 5: Noise-like pattern
            Log.i(TAG, "🧪 Test 5: Random noise pattern");
            Bitmap noiseImage = createNoisePattern(224, 224);
            PlantDiseaseClassifier.ClassificationResult result5 = classifier.predict(noiseImage);
            if (result5 != null) {
                predictions.add(result5.classIndex);
                confidences.add(result5.confidence);
                testNames.add("Noise Pattern");
                Log.i(TAG, String.format("   Result: Class %d, Confidence: %.4f", 
                    result5.classIndex, result5.confidence));
            }

            // Test 6: Gradient pattern
            Log.i(TAG, "🧪 Test 6: Gradient pattern");
            Bitmap gradientImage = createGradientPattern(224, 224);
            PlantDiseaseClassifier.ClassificationResult result6 = classifier.predict(gradientImage);
            if (result6 != null) {
                predictions.add(result6.classIndex);
                confidences.add(result6.confidence);
                testNames.add("Gradient Pattern");
                Log.i(TAG, String.format("   Result: Class %d, Confidence: %.4f", 
                    result6.classIndex, result6.confidence));
            }

            // Analyze results
            Log.i(TAG, "\n📊 ANALYSIS RESULTS:");
            Log.i(TAG, "==========================================");

            // Check prediction diversity
            Set<Integer> uniquePredictions = new HashSet<>(predictions);
            boolean isResponsive = uniquePredictions.size() > 1;

            Log.i(TAG, String.format("Total tests: %d", predictions.size()));
            Log.i(TAG, String.format("Unique predictions: %d", uniquePredictions.size()));
            Log.i(TAG, String.format("All predictions: %s", predictions.toString()));

            if (!isResponsive) {
                Log.e(TAG, "🚨 CRITICAL PROBLEM: Model always predicts the same class!");
                Log.e(TAG, String.format("   All inputs predict class: %d", 
                    predictions.isEmpty() ? -1 : predictions.get(0)));
                Log.e(TAG, "   This indicates a FROZEN or BROKEN model!");
            } else {
                Log.i(TAG, "✅ Model is responsive - different inputs give different outputs");
            }

            // Check confidence variance
            if (!confidences.isEmpty()) {
                float minConf = confidences.get(0);
                float maxConf = confidences.get(0);
                float sumConf = 0.0f;

                for (float conf : confidences) {
                    minConf = Math.min(minConf, conf);
                    maxConf = Math.max(maxConf, conf);
                    sumConf += conf;
                }

                float avgConf = sumConf / confidences.size();
                float confRange = maxConf - minConf;

                Log.i(TAG, String.format("Confidence stats - Min: %.4f, Max: %.4f, Avg: %.4f, Range: %.4f", 
                    minConf, maxConf, avgConf, confRange));

                if (confRange < 0.01f) {
                    Log.w(TAG, "⚠️  WARNING: Very small confidence range - model might not be learning properly");
                }
            }

            // Calculate output variance (simplified)
            float outputVariance = calculateOutputVariance(confidences);

            // Release model resources
            classifier.release();

            Log.i(TAG, "\n🎯 FINAL DIAGNOSIS:");
            Log.i(TAG, "==========================================");
            
            if (!isResponsive) {
                Log.e(TAG, "🚨 MODEL IS NOT WORKING PROPERLY!");
                Log.e(TAG, "Possible causes:");
                Log.e(TAG, "1. Model weights are frozen/not trained");
                Log.e(TAG, "2. Model conversion error");
                Log.e(TAG, "3. BatchNorm layers not in eval mode");
                Log.e(TAG, "4. Training data was not diverse enough");
                Log.e(TAG, "5. Model architecture issue");
            } else {
                Log.i(TAG, "✅ Model appears to be working correctly");
                Log.i(TAG, "If you're still seeing identical predictions in real use,");
                Log.i(TAG, "the issue might be with specific input preprocessing");
            }

            return new DebugResult(predictions, confidences, testNames, isResponsive, outputVariance);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error during model debugging", e);
            return new DebugResult(predictions, confidences, testNames, false, 0.0f);
        }
    }

    /**
     * Create a solid color bitmap for testing
     */
    private static Bitmap createSolidColorBitmap(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap;
    }

    /**
     * Create a noise pattern bitmap for testing
     */
    private static Bitmap createNoisePattern(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Create pseudo-random pattern based on coordinates
                int r = (x * 13 + y * 17) % 256;
                int g = (x * 19 + y * 23) % 256;
                int b = (x * 29 + y * 31) % 256;
                int color = Color.rgb(r, g, b);
                bitmap.setPixel(x, y, color);
            }
        }
        
        return bitmap;
    }

    /**
     * Create a gradient pattern bitmap for testing
     */
    private static Bitmap createGradientPattern(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int r = (x * 255) / width;
                int g = (y * 255) / height;
                int b = ((x + y) * 255) / (width + height);
                int color = Color.rgb(r, g, b);
                bitmap.setPixel(x, y, color);
            }
        }
        
        return bitmap;
    }

    /**
     * Calculate simple output variance metric
     */
    private static float calculateOutputVariance(List<Float> confidences) {
        if (confidences.size() < 2) return 0.0f;

        float sum = 0.0f;
        for (float conf : confidences) {
            sum += conf;
        }
        float mean = sum / confidences.size();

        float variance = 0.0f;
        for (float conf : confidences) {
            variance += (conf - mean) * (conf - mean);
        }
        variance /= confidences.size();

        return variance;
    }

    /**
     * Enhanced debugging with raw tensor outputs
     * ADVANCED: This method provides more detailed analysis
     */
    public static void testRawModelOutputs(Context context) {
        Log.i(TAG, "🔬 ADVANCED: Testing raw model outputs");
        
        try {
            // Load model directly
            String modelPath = context.getFilesDir() + "/plant_disease_model.ptl";
            Module module = LiteModuleLoader.load(modelPath);

            // Test with different tensor inputs
            Tensor blackTensor = TensorImageUtils.bitmapToFloat32Tensor(
                createSolidColorBitmap(224, 224, Color.BLACK),
                NORM_MEAN_RGB, NORM_STD_RGB);

            Tensor whiteTensor = TensorImageUtils.bitmapToFloat32Tensor(
                createSolidColorBitmap(224, 224, Color.WHITE),
                NORM_MEAN_RGB, NORM_STD_RGB);

            // Get raw outputs
            Tensor blackOutput = module.forward(IValue.from(blackTensor)).toTensor();
            Tensor whiteOutput = module.forward(IValue.from(whiteTensor)).toTensor();

            float[] blackScores = blackOutput.getDataAsFloatArray();
            float[] whiteScores = whiteOutput.getDataAsFloatArray();

            // Log raw outputs for comparison
            Log.d(TAG, "Black image top 5 raw scores:");
            logTopScores(blackScores, 5);
            
            Log.d(TAG, "White image top 5 raw scores:");
            logTopScores(whiteScores, 5);

            // Calculate score differences
            float totalDifference = 0.0f;
            for (int i = 0; i < Math.min(blackScores.length, whiteScores.length); i++) {
                totalDifference += Math.abs(blackScores[i] - whiteScores[i]);
            }
            
            Log.i(TAG, String.format("Total score difference: %.6f", totalDifference));
            
            if (totalDifference < 0.001f) {
                Log.e(TAG, "🚨 CRITICAL: Raw outputs are nearly identical!");
                Log.e(TAG, "This confirms the model is frozen or broken!");
            } else {
                Log.i(TAG, "✅ Raw outputs show variation");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in advanced debugging", e);
        }
    }

    private static void logTopScores(float[] scores, int topN) {
        // Find top N scores and their indices
        for (int rank = 0; rank < topN && rank < scores.length; rank++) {
            int maxIndex = 0;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > scores[maxIndex]) {
                    maxIndex = i;
                }
            }
            Log.d(TAG, String.format("  Rank %d: Index %d = %.6f", 
                rank + 1, maxIndex, scores[maxIndex]));
            scores[maxIndex] = Float.NEGATIVE_INFINITY; // Remove from next search
        }
    }
}
