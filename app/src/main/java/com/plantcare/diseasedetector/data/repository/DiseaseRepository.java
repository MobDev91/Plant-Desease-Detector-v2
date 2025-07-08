package com.plantcare.diseasedetector.data.repository;

import android.content.Context;
import android.util.Log;

import com.plantcare.diseasedetector.data.database.AppDatabase;
import com.plantcare.diseasedetector.data.database.DiseaseInfoDao;
import com.plantcare.diseasedetector.data.models.DiseaseInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository class for managing disease information data
 * Handles data loading, caching, and database operations
 */
public class DiseaseRepository {

    private static final String TAG = "DiseaseRepository";
    private static final String DISEASE_DATABASE_FILE = "disease_database.json";

    private Context context;
    private AppDatabase database;
    private DiseaseInfoDao diseaseInfoDao;
    private ExecutorService executor;

    // Singleton instance
    private static volatile DiseaseRepository INSTANCE;

    private DiseaseRepository(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.diseaseInfoDao = database.diseaseInfoDao();
        this.executor = Executors.newFixedThreadPool(2);
    }

    /**
     * Get singleton instance
     */
    public static DiseaseRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DiseaseRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DiseaseRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // ===== DATA LOADING METHODS =====

    /**
     * Initialize repository with disease data
     */
    public void initializeDiseaseData(RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // Check if data already exists
                int count = diseaseInfoDao.getTotalDiseaseCount();
                if (count > 0) {
                    Log.i(TAG, "Disease data already loaded (" + count + " records)");
                    if (callback != null) callback.onSuccess(true);
                    return;
                }

                // Load data from JSON
                List<DiseaseInfo> diseaseList = loadDiseaseDataFromJson();
                if (!diseaseList.isEmpty()) {
                    diseaseInfoDao.insertAllDiseaseInfo(diseaseList);
                    Log.i(TAG, "Loaded " + diseaseList.size() + " disease records");
                    if (callback != null) callback.onSuccess(true);
                } else {
                    Log.w(TAG, "No disease data loaded");
                    if (callback != null) callback.onError("No disease data found");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error initializing disease data", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Load disease data from JSON file
     */
    private List<DiseaseInfo> loadDiseaseDataFromJson() {
        List<DiseaseInfo> diseaseList = new ArrayList<>();

        try {
            String jsonString = loadJSONFromAsset(DISEASE_DATABASE_FILE);
            if (jsonString == null) {
                // If JSON file doesn't exist, create sample data
                return createSampleDiseaseData();
            }

            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray diseasesArray = jsonObject.getJSONArray("diseases");

            for (int i = 0; i < diseasesArray.length(); i++) {
                JSONObject diseaseObj = diseasesArray.getJSONObject(i);
                DiseaseInfo diseaseInfo = parseDiseaseFromJSON(diseaseObj);
                if (diseaseInfo != null) {
                    diseaseList.add(diseaseInfo);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing disease JSON", e);
            return createSampleDiseaseData();
        }

        return diseaseList;
    }

    /**
     * Parse DiseaseInfo from JSON object
     */
    private DiseaseInfo parseDiseaseFromJSON(JSONObject jsonObject) {
        try {
            DiseaseInfo diseaseInfo = new DiseaseInfo();

            diseaseInfo.setDiseaseName(jsonObject.optString("disease_name", ""));
            diseaseInfo.setPlantType(jsonObject.optString("plant_type", ""));
            diseaseInfo.setScientificName(jsonObject.optString("scientific_name", ""));
            diseaseInfo.setDescription(jsonObject.optString("description", ""));
            diseaseInfo.setSymptoms(jsonObject.optString("symptoms", ""));
            diseaseInfo.setCauses(jsonObject.optString("causes", ""));
            diseaseInfo.setPreventionTips(jsonObject.optString("prevention_tips", ""));
            diseaseInfo.setTreatmentOptions(jsonObject.optString("treatment_options", ""));
            diseaseInfo.setSeverityLevel(jsonObject.optString("severity_level", "Medium"));
            diseaseInfo.setAffectedParts(jsonObject.optString("affected_parts", ""));
            diseaseInfo.setEnvironmentalFactors(jsonObject.optString("environmental_factors", ""));
            diseaseInfo.setSpreadMethod(jsonObject.optString("spread_method", ""));
            diseaseInfo.setOptimalConditions(jsonObject.optString("optimal_conditions", ""));
            diseaseInfo.setRecoveryTime(jsonObject.optString("recovery_time", ""));
            diseaseInfo.setImageUrls(jsonObject.optString("image_urls", ""));
            diseaseInfo.setCommon(jsonObject.optBoolean("is_common", false));
            diseaseInfo.setTreatable(jsonObject.optBoolean("is_treatable", true));

            return diseaseInfo;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing disease object", e);
            return null;
        }
    }

    /**
     * Load JSON string from assets
     */
    private String loadJSONFromAsset(String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.w(TAG, "JSON file not found: " + fileName);
            return null;
        }
    }

    /**
     * Create sample disease data for demo
     */
    private List<DiseaseInfo> createSampleDiseaseData() {
        List<DiseaseInfo> sampleData = new ArrayList<>();

        // Apple Scab
        DiseaseInfo appleScab = new DiseaseInfo();
        appleScab.setDiseaseName("Apple Scab");
        appleScab.setPlantType("Apple");
        appleScab.setScientificName("Venturia inaequalis");
        appleScab.setDescription("A fungal disease that causes dark, scabby lesions on apple leaves and fruit.");
        appleScab.setSymptoms("Dark olive-green spots on leaves|Brown or black spots on fruit|Premature leaf drop|Reduced fruit quality");
        appleScab.setCauses("Fungal infection|Wet spring weather|Poor air circulation|Infected plant debris");
        appleScab.setPreventionTips("Choose resistant varieties|Improve air circulation|Remove fallen leaves|Apply preventive fungicides");
        appleScab.setTreatmentOptions("Fungicide sprays|Copper-based treatments|Organic sulfur sprays|Cultural controls");
        appleScab.setSeverityLevel("Medium");
        appleScab.setAffectedParts("Leaves|Fruit|Young shoots");
        appleScab.setEnvironmentalFactors("High humidity|Cool wet weather|Poor air circulation");
        appleScab.setSpreadMethod("Wind-blown spores|Rain splash|Infected debris");
        appleScab.setRecoveryTime("2-4 weeks with treatment");
        appleScab.setCommon(true);
        appleScab.setTreatable(true);
        sampleData.add(appleScab);

        // Tomato Early Blight
        DiseaseInfo tomatoBlight = new DiseaseInfo();
        tomatoBlight.setDiseaseName("Early Blight");
        tomatoBlight.setPlantType("Tomato");
        tomatoBlight.setScientificName("Alternaria solani");
        tomatoBlight.setDescription("A fungal disease causing brown spots with concentric rings on tomato leaves.");
        tomatoBlight.setSymptoms("Brown spots with target-like rings|Yellow halos around spots|Lower leaves affected first|Fruit lesions possible");
        tomatoBlight.setCauses("Fungal infection|Warm humid conditions|Plant stress|Poor nutrition");
        tomatoBlight.setPreventionTips("Crop rotation|Adequate spacing|Avoid overhead watering|Mulching to prevent soil splash");
        tomatoBlight.setTreatmentOptions("Fungicide applications|Remove affected foliage|Improve air circulation|Balanced fertilization");
        tomatoBlight.setSeverityLevel("Medium");
        tomatoBlight.setAffectedParts("Leaves|Stems|Fruit");
        tomatoBlight.setEnvironmentalFactors("Warm temperatures|High humidity|Extended leaf wetness");
        tomatoBlight.setSpreadMethod("Wind dispersal|Water splash|Contaminated tools");
        tomatoBlight.setRecoveryTime("3-6 weeks");
        tomatoBlight.setCommon(true);
        tomatoBlight.setTreatable(true);
        sampleData.add(tomatoBlight);

        // Grape Black Rot
        DiseaseInfo grapeBlackRot = new DiseaseInfo();
        grapeBlackRot.setDiseaseName("Black Rot");
        grapeBlackRot.setPlantType("Grape");
        grapeBlackRot.setScientificName("Guignardia bidwellii");
        grapeBlackRot.setDescription("A serious fungal disease of grapes causing fruit mummification.");
        grapeBlackRot.setSymptoms("Circular brown spots on leaves|Shriveled black fruit|Fruit drops prematurely|Cane lesions");
        grapeBlackRot.setCauses("Fungal pathogen|Warm wet weather|Poor air circulation|Infected plant material");
        grapeBlackRot.setPreventionTips("Prune for air circulation|Remove infected material|Apply preventive sprays|Avoid overhead irrigation");
        grapeBlackRot.setTreatmentOptions("Fungicide programs|Cultural controls|Sanitation practices|Resistant varieties");
        grapeBlackRot.setSeverityLevel("High");
        grapeBlackRot.setAffectedParts("Fruit|Leaves|Shoots|Tendrils");
        grapeBlackRot.setEnvironmentalFactors("Warm humid weather|Extended wetness|Poor ventilation");
        grapeBlackRot.setSpreadMethod("Rain splash|Wind|Infected debris");
        grapeBlackRot.setRecoveryTime("Season-long management");
        grapeBlackRot.setCommon(true);
        grapeBlackRot.setTreatable(true);
        sampleData.add(grapeBlackRot);

        return sampleData;
    }

    // ===== PUBLIC API METHODS =====

    /**
     * Get all disease information
     */
    public void getAllDiseases(RepositoryCallback<List<DiseaseInfo>> callback) {
        executor.execute(() -> {
            try {
                List<DiseaseInfo> diseases = diseaseInfoDao.getAllDiseaseInfo();
                if (callback != null) callback.onSuccess(diseases);
            } catch (Exception e) {
                Log.e(TAG, "Error getting all diseases", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Get disease info by ID
     */
    public void getDiseaseById(int diseaseId, RepositoryCallback<DiseaseInfo> callback) {
        executor.execute(() -> {
            try {
                DiseaseInfo disease = diseaseInfoDao.getDiseaseInfoById(diseaseId);
                if (callback != null) callback.onSuccess(disease);
            } catch (Exception e) {
                Log.e(TAG, "Error getting disease by ID", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Get disease info for a prediction
     */
    public void getDiseaseForPrediction(String predictedClass, RepositoryCallback<DiseaseInfo> callback) {
        executor.execute(() -> {
            try {
                // Clean up the predicted class
                String cleanedClass = cleanPredictedClass(predictedClass);
                DiseaseInfo disease = diseaseInfoDao.getDiseaseInfoByPrediction(cleanedClass);

                if (disease == null) {
                    // Try searching by disease name
                    disease = diseaseInfoDao.getDiseaseInfoByName(cleanedClass);
                }

                if (callback != null) callback.onSuccess(disease);
            } catch (Exception e) {
                Log.e(TAG, "Error getting disease for prediction", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Search diseases
     */
    public void searchDiseases(String query, RepositoryCallback<List<DiseaseInfo>> callback) {
        executor.execute(() -> {
            try {
                List<DiseaseInfo> diseases = diseaseInfoDao.searchDiseaseInfo(query);
                if (callback != null) callback.onSuccess(diseases);
            } catch (Exception e) {
                Log.e(TAG, "Error searching diseases", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Get diseases by plant type
     */
    public void getDiseasesByPlantType(String plantType, RepositoryCallback<List<DiseaseInfo>> callback) {
        executor.execute(() -> {
            try {
                List<DiseaseInfo> diseases = diseaseInfoDao.getDiseaseInfoByPlantType(plantType);
                if (callback != null) callback.onSuccess(diseases);
            } catch (Exception e) {
                Log.e(TAG, "Error getting diseases by plant type", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Get common diseases
     */
    public void getCommonDiseases(RepositoryCallback<List<DiseaseInfo>> callback) {
        executor.execute(() -> {
            try {
                List<DiseaseInfo> diseases = diseaseInfoDao.getCommonDiseases();
                if (callback != null) callback.onSuccess(diseases);
            } catch (Exception e) {
                Log.e(TAG, "Error getting common diseases", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Get treatable diseases
     */
    public void getTreatableDiseases(RepositoryCallback<List<DiseaseInfo>> callback) {
        executor.execute(() -> {
            try {
                List<DiseaseInfo> diseases = diseaseInfoDao.getTreatableDiseases();
                if (callback != null) callback.onSuccess(diseases);
            } catch (Exception e) {
                Log.e(TAG, "Error getting treatable diseases", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Advanced search with filters
     */
    public void advancedSearch(String plantType, String severity, Boolean treatable,
                               Boolean common, String searchQuery,
                               RepositoryCallback<List<DiseaseInfo>> callback) {
        executor.execute(() -> {
            try {
                List<DiseaseInfo> diseases = diseaseInfoDao.advancedSearch(
                        plantType, severity, treatable, common, searchQuery);
                if (callback != null) callback.onSuccess(diseases);
            } catch (Exception e) {
                Log.e(TAG, "Error in advanced search", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Get repository statistics
     */
    public void getStatistics(RepositoryCallback<DiseaseStatistics> callback) {
        executor.execute(() -> {
            try {
                DiseaseStatistics stats = new DiseaseStatistics();
                stats.totalDiseases = diseaseInfoDao.getTotalDiseaseCount();
                stats.treatableDiseases = diseaseInfoDao.getTreatableDiseaseCount();
                stats.uniquePlantTypes = diseaseInfoDao.getUniquePlantTypes().size();
                stats.severityLevels = diseaseInfoDao.getUniqueSeverityLevels();

                if (callback != null) callback.onSuccess(stats);
            } catch (Exception e) {
                Log.e(TAG, "Error getting statistics", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    // ===== UTILITY METHODS =====

    /**
     * Clean predicted class for database search
     */
    private String cleanPredictedClass(String predictedClass) {
        if (predictedClass == null) return "";

        return predictedClass
                .replace("___", " ")
                .replace("_", " ")
                .trim();
    }

    /**
     * Check if disease data is loaded
     */
    public void isDiseaseDataLoaded(RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                int count = diseaseInfoDao.getTotalDiseaseCount();
                if (callback != null) callback.onSuccess(count > 0);
            } catch (Exception e) {
                Log.e(TAG, "Error checking disease data", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Refresh disease data
     */
    public void refreshDiseaseData(RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // Clear existing data
                diseaseInfoDao.deleteAllDiseaseInfo();

                // Reload data
                List<DiseaseInfo> diseaseList = loadDiseaseDataFromJson();
                if (!diseaseList.isEmpty()) {
                    diseaseInfoDao.insertAllDiseaseInfo(diseaseList);
                    Log.i(TAG, "Refreshed " + diseaseList.size() + " disease records");
                    if (callback != null) callback.onSuccess(true);
                } else {
                    if (callback != null) callback.onError("No disease data to refresh");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error refreshing disease data", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    // ===== CALLBACK INTERFACE =====

    /**
     * Generic callback interface for repository operations
     */
    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    // ===== STATISTICS CLASS =====

    /**
     * Statistics about disease database
     */
    public static class DiseaseStatistics {
        public int totalDiseases;
        public int treatableDiseases;
        public int uniquePlantTypes;
        public List<String> severityLevels;

        public DiseaseStatistics() {
            this.severityLevels = new ArrayList<>();
        }

        public double getTreatablePercentage() {
            if (totalDiseases == 0) return 0.0;
            return (double) treatableDiseases / totalDiseases * 100.0;
        }

        @Override
        public String toString() {
            return "DiseaseStatistics{" +
                    "totalDiseases=" + totalDiseases +
                    ", treatableDiseases=" + treatableDiseases +
                    ", uniquePlantTypes=" + uniquePlantTypes +
                    ", severityLevels=" + severityLevels.size() +
                    '}';
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}