package com.cadetia.simplicadet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.cadetia.simplicadet.model.LearningPathModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class LearningPathHelper {

    private static final String PREF_NAME = "LearningPathPrefs";
    private static final String KEY_PATH_DATA = "pathData";
    private static final String KEY_PATH_COUNT = "pathCount";

    /**
     * Generate a learning path with specified number of nodes
     * @param context Application context
     * @param nodeCount Number of nodes to create
     * @param categoryTitle Base title for the nodes
     * @return List of LearningPathModel objects
     */
    public static List<LearningPathModel> generateLearningPath(Context context, int nodeCount, String categoryTitle) {
        List<LearningPathModel> pathList = new ArrayList<>();

        // Load existing progress if any
        List<LearningPathModel> savedPath = loadLearningPath(context);

        for (int i = 0; i < nodeCount; i++) {
            boolean isCompleted = false;
            boolean isUnlocked = (i == 0); // First node is always unlocked

            // Check if we have saved progress for this position
            if (savedPath != null && i < savedPath.size()) {
                LearningPathModel savedNode = savedPath.get(i);
                isCompleted = savedNode.isCompleted();
                isUnlocked = savedNode.isUnlocked();
            }

            String nodeTitle = categoryTitle + " " + (i + 1);
            LearningPathModel node = new LearningPathModel(i, nodeTitle, isCompleted, isUnlocked, i);
            pathList.add(node);
        }

        // Save the generated path
        saveLearningPath(context, pathList);

        return pathList;
    }

    /**
     * Save learning path progress to SharedPreferences
     */
    public static void saveLearningPath(Context context, List<LearningPathModel> pathList) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(pathList);

        editor.putString(KEY_PATH_DATA, json);
        editor.putInt(KEY_PATH_COUNT, pathList.size());
        editor.apply();
    }

    /**
     * Load learning path progress from SharedPreferences
     */
    public static List<LearningPathModel> loadLearningPath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PATH_DATA, null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<LearningPathModel>>(){}.getType();
            return gson.fromJson(json, type);
        }

        return new ArrayList<>();
    }

    /**
     * Mark a node as completed and unlock the next one
     */
    public static void completeNode(Context context, List<LearningPathModel> pathList, int position) {
        if (position >= 0 && position < pathList.size()) {
            // Mark current node as completed
            pathList.get(position).setCompleted(true);

            // Unlock next node if it exists
            if (position + 1 < pathList.size()) {
                pathList.get(position + 1).setUnlocked(true);
            }

            // Save progress
            saveLearningPath(context, pathList);
        }
    }

    /**
     * Reset all progress in the learning path
     */
    public static void resetLearningPath(Context context, List<LearningPathModel> pathList) {
        for (int i = 0; i < pathList.size(); i++) {
            LearningPathModel node = pathList.get(i);
            node.setCompleted(false);
            node.setUnlocked(i == 0); // Only first node unlocked
        }

        saveLearningPath(context, pathList);
    }

    /**
     * Get the number of completed nodes
     */
    public static int getCompletedNodesCount(List<LearningPathModel> pathList) {
        int count = 0;
        for (LearningPathModel node : pathList) {
            if (node.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get progress percentage
     */
    public static int getProgressPercentage(List<LearningPathModel> pathList) {
        if (pathList.isEmpty()) return 0;

        int completedCount = getCompletedNodesCount(pathList);
        return (completedCount * 100) / pathList.size();
    }

    /**
     * Check if the entire path is completed
     */
    public static boolean isPathCompleted(List<LearningPathModel> pathList) {
        for (LearningPathModel node : pathList) {
            if (!node.isCompleted()) {
                return false;
            }
        }
        return true;
    }
}