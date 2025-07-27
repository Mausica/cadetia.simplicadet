package com.cadetia.simplicadet.database;

import static java.sql.Types.NUMERIC;

import android.content.Context;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.QuestionModel;
import com.cadetia.simplicadet.model.Quizz;
import com.cadetia.simplicadet.model.RankModel;
import com.cadetia.simplicadet.model.UserModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DbQuery {

    public static final String TAG = "DbQuery";
    public static FirebaseFirestore g_firestore;
    public static List<JournalEntry> g_journalList = new ArrayList<>();
    public static List<CategoryModel> g_catList = new ArrayList<>();
    public static List<QuestionModel> g_quesList = new ArrayList<>();
    public static List<RankModel> g_rankList = new ArrayList<>();
    public static List<JournalEntry> g_militaryJournalList = new ArrayList<>();
    public static List<JournalEntry> g_homeJournalList = new ArrayList<>();
    public static LearningPath g_learningPath;
    public static List<FlashcardModel> g_flashcardList = new ArrayList<>();
    public static List<CategoryModel> g_militaryCatList = new ArrayList<>();
    public static List<CategoryModel> g_homeCatList = new ArrayList<>();
    public static List<LearningPath> g_allLearningPaths = new ArrayList<>();

    public static void loadAllLearningPaths(MyCompleteListener listener) {
        if (g_firestore == null) {
            listener.onFailure();
            return;
        }

        g_firestore.collection("LEARNING")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        g_allLearningPaths.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                LearningPath learningPath = new LearningPath();
                                learningPath.id = document.getId();
                                learningPath.title = document.getString("pathTitle");
                                Object pathNodesObj = document.get("pathNodes");
                                if (pathNodesObj != null) {
                                    learningPath.nodes = new ArrayList<>();
                                    if (pathNodesObj instanceof List) {
                                        List<?> pathNodesList = (List<?>) pathNodesObj;
                                        if (!pathNodesList.isEmpty()) {
                                            Object firstItem = pathNodesList.get(0);
                                            if (firstItem instanceof Map) {
                                                @SuppressWarnings("unchecked")
                                                List<Map<String, Object>> pathNodesData = (List<Map<String, Object>>) pathNodesList;
                                                for (Map<String, Object> nodeData : pathNodesData) {
                                                    LearningPathNode node = new LearningPathNode();
                                                    node.id = (String) nodeData.get("id");
                                                    node.title = (String) nodeData.get("title");
                                                    Object typeObj = nodeData.get("type");
                                                    if (typeObj instanceof Long) {
                                                        node.type = ((Long) typeObj).intValue();
                                                    } else if (typeObj instanceof String) {
                                                        String typeStr = (String) typeObj;
                                                        node.type = "QUIZZES".equals(typeStr) ? 0 : 1;
                                                    } else {
                                                        node.type = 0;
                                                    }
                                                    learningPath.nodes.add(node);
                                                }
                                            } else if (firstItem instanceof String) {
                                                @SuppressWarnings("unchecked")
                                                List<String> nodeIds = (List<String>) pathNodesList;
                                                List<String> pathTypesData = (List<String>) document.get("pathTypes");
                                                for (int i = 0; i < nodeIds.size(); i++) {
                                                    LearningPathNode node = new LearningPathNode();
                                                    node.id = nodeIds.get(i);
                                                    node.title = "Loading...";
                                                    if (pathTypesData != null && i < pathTypesData.size()) {
                                                        String typeStr = pathTypesData.get(i);
                                                        node.type = "QUIZZES".equals(typeStr.trim()) ? 0 : 1;
                                                    } else {
                                                        node.type = 0;
                                                    }
                                                    learningPath.nodes.add(node);
                                                }
                                            }
                                        }
                                    }
                                }
                                List<String> pathTypesData = (List<String>) document.get("pathTypes");
                                if (pathTypesData != null) {
                                    learningPath.pathTypes = new ArrayList<>(pathTypesData);
                                }
                                g_allLearningPaths.add(learningPath);
                            } catch (Exception e) {
                                Log.e("DbQuery", "Error parsing learning path document: " + document.getId(), e);
                                Log.e("DbQuery", "Document data: " + document.getData());
                            }
                        }
                        fetchNodeTitlesForAllPaths(listener);
                    } else {
                        Log.e("DbQuery", "Error getting learning paths: ", task.getException());
                        listener.onFailure();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DbQuery", "Failed to load learning paths", e);
                    listener.onFailure();
                });
    }

    private static void fetchNodeTitlesForAllPaths(MyCompleteListener listener) {
        List<Task<DocumentSnapshot>> allTasks = new ArrayList<>();
        for (LearningPath path : g_allLearningPaths) {
            if (path.nodes != null) {
                for (LearningPathNode node : path.nodes) {
                    if ("Loading...".equals(node.title)) {
                        String collection = node.type == 0 ? "QUIZZES" : "FLASHCARDS";
                        Task<DocumentSnapshot> task = g_firestore.collection(collection).document(node.id).get();
                        allTasks.add(task);
                    }
                }
            }
        }
        if (allTasks.isEmpty()) {
            listener.onSucces();
            return;
        }
        Tasks.whenAllComplete(allTasks).addOnCompleteListener(task -> {
            int taskIndex = 0;
            for (LearningPath path : g_allLearningPaths) {
                if (path.nodes != null) {
                    for (LearningPathNode node : path.nodes) {
                        if ("Loading...".equals(node.title) && taskIndex < allTasks.size()) {
                            Task<DocumentSnapshot> docTask = allTasks.get(taskIndex);
                            if (docTask.isSuccessful() && docTask.getResult().exists()) {
                                DocumentSnapshot doc = docTask.getResult();
                                if (node.type == 0) {
                                    node.title = doc.getString("title");
                                } else {
                                    node.title = doc.getString("cardTitle");
                                }
                                if (node.title == null) {
                                    node.title = "Untitled";
                                }
                            } else {
                                node.title = "Error loading title";
                            }
                            taskIndex++;
                        }
                    }
                }
            }
            listener.onSucces();
        });
    }

    public static void selectLearningPath(String pathId, MyCompleteListener listener) {
        if (g_firestore == null) {
            listener.onFailure();
            return;
        }
        g_firestore.collection("LEARNING").document(pathId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            try {
                                LearningPath learningPath = new LearningPath();
                                learningPath.id = document.getId();
                                learningPath.title = document.getString("pathTitle");
                                Object pathNodesObj = document.get("pathNodes");
                                if (pathNodesObj != null) {
                                    learningPath.nodes = new ArrayList<>();
                                    if (pathNodesObj instanceof List) {
                                        List<?> pathNodesList = (List<?>) pathNodesObj;
                                        if (!pathNodesList.isEmpty()) {
                                            Object firstItem = pathNodesList.get(0);
                                            if (firstItem instanceof String) {
                                                Log.d("DbQuery", "Processing simple structure (List of Strings)");
                                                @SuppressWarnings("unchecked")
                                                List<String> nodeIds = (List<String>) pathNodesList;
                                                List<String> pathTypesData = (List<String>) document.get("pathTypes");
                                                for (int i = 0; i < nodeIds.size(); i++) {
                                                    LearningPathNode node = new LearningPathNode();
                                                    node.id = nodeIds.get(i);
                                                    node.title = "Loading...";
                                                    if (pathTypesData != null && i < pathTypesData.size()) {
                                                        String typeStr = pathTypesData.get(i);
                                                        node.type = "QUIZZES".equals(typeStr.trim()) ? 0 : 1;
                                                    } else {
                                                        node.type = 0;
                                                    }
                                                    learningPath.nodes.add(node);
                                                }
                                                fetchNodeTitlesForPath(learningPath, listener);
                                                return;
                                            } else if (firstItem instanceof Map) {
                                                Log.d("DbQuery", "Processing complex structure (List of Maps)");
                                                @SuppressWarnings("unchecked")
                                                List<Map<String, Object>> pathNodesData = (List<Map<String, Object>>) pathNodesList;
                                                for (Map<String, Object> nodeData : pathNodesData) {
                                                    LearningPathNode node = new LearningPathNode();
                                                    node.id = (String) nodeData.get("id");
                                                    node.title = (String) nodeData.get("title");
                                                    Object typeObj = nodeData.get("type");
                                                    if (typeObj instanceof Long) {
                                                        node.type = ((Long) typeObj).intValue();
                                                    } else if (typeObj instanceof String) {
                                                        String typeStr = (String) typeObj;
                                                        node.type = "QUIZZES".equals(typeStr) ? 0 : 1;
                                                    } else {
                                                        node.type = 0;
                                                    }
                                                    learningPath.nodes.add(node);
                                                }
                                                g_learningPath = learningPath;
                                                listener.onSucces();
                                                return;
                                            } else {
                                                Log.e("DbQuery", "Unknown pathNodes structure. First item type: " + firstItem.getClass().getSimpleName());
                                            }
                                        }
                                    } else {
                                        Log.e("DbQuery", "pathNodes is not a List. Type: " + pathNodesObj.getClass().getSimpleName());
                                    }
                                }
                                List<String> pathTypesData = (List<String>) document.get("pathTypes");
                                if (pathTypesData != null) {
                                    learningPath.pathTypes = new ArrayList<>(pathTypesData);
                                }
                                g_learningPath = learningPath;
                                listener.onSucces();
                            } catch (Exception e) {
                                Log.e("DbQuery", "Error parsing selected learning path", e);
                                Log.e("DbQuery", "Document data: " + Objects.requireNonNull(document.getData()));
                                listener.onFailure();
                            }
                        } else {
                            Log.e("DbQuery", "Learning path document does not exist: " + pathId);
                            listener.onFailure();
                        }
                    } else {
                        Log.e("DbQuery", "Error getting selected learning path: ", task.getException());
                        listener.onFailure();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DbQuery", "Failed to select learning path", e);
                    listener.onFailure();
                });
    }

    private static void fetchNodeTitlesForPath(LearningPath path, MyCompleteListener listener) {
        if (path.nodes == null || path.nodes.isEmpty()) {
            g_learningPath = path;
            listener.onSucces();
            return;
        }
        Log.d("DbQuery", "Fetching titles for " + path.nodes.size() + " nodes");
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (LearningPathNode node : path.nodes) {
            if ("Loading...".equals(node.title)) {
                String collection = node.type == 0 ? "QUIZZES" : "FLASHCARDS";
                Log.d("DbQuery", "Fetching title for node: " + node.id + " from collection: " + collection);
                Task<DocumentSnapshot> task = g_firestore.collection(collection).document(node.id).get();
                tasks.add(task);
            }
        }
        if (tasks.isEmpty()) {
            Log.d("DbQuery", "No titles to fetch, completing");
            g_learningPath = path;
            listener.onSucces();
            return;
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
            Log.d("DbQuery", "All title fetch tasks completed");
            int taskIndex = 0;
            for (LearningPathNode node : path.nodes) {
                if ("Loading...".equals(node.title) && taskIndex < tasks.size()) {
                    Task<DocumentSnapshot> docTask = tasks.get(taskIndex);
                    if (docTask.isSuccessful() && docTask.getResult() != null && docTask.getResult().exists()) {
                        DocumentSnapshot doc = docTask.getResult();
                        if (node.type == 0) {
                            node.title = doc.getString("title");
                        } else {
                            node.title = doc.getString("cardTitle");
                        }
                        if (node.title == null) {
                            node.title = "Untitled";
                        }
                        Log.d("DbQuery", "Set title for node " + node.id + ": " + node.title);
                    } else {
                        node.title = "Error loading title";
                        Log.e("DbQuery", "Failed to fetch title for node: " + node.id);
                    }
                    taskIndex++;
                }
            }
            g_learningPath = path;
            listener.onSucces();
        });
    }

    public static class LearningPath {
        public String id;
        public String title;
        public List<LearningPathNode> nodes;
        public List<String> pathTypes;

        public LearningPath() {
            this.nodes = new ArrayList<>();
            this.pathTypes = new ArrayList<>();
        }

        public LearningPath(String id, String title, List<LearningPathNode> nodes) {
            this.id = id;
            this.title = title;
            this.nodes = nodes != null ? nodes : new ArrayList<>();
            this.pathTypes = new ArrayList<>();
        }
    }

    public static class LearningPathNode {
        public String id;
        public int type;
        public String title;

        public LearningPathNode() {
        }

        public LearningPathNode(String id, int type, String title) {
            this.id = id;
            this.type = type;
            this.title = title;
        }
    }

    public static class FlashcardModel {
        public String question, answer, frontImage, backImage;

        public FlashcardModel(String q, String a, String fI, String bI) {
            this.question = q;
            this.answer = a;
            this.frontImage = fI;
            this.backImage = bI;
        }
    }

    public interface UserScoreListener {
        void onUserScoresReceived(List<UserModel> userList);
        void onFailure();
    }

    public static void getUsersSortedByScore(UserScoreListener userScoreListener) {
        g_firestore.collection("USERS").orderBy("TOTAL_SCORE", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserModel> userList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) userList.add(document.toObject(UserModel.class));
                    userScoreListener.onUserScoresReceived(userList);
                })
                .addOnFailureListener(e -> userScoreListener.onFailure());
    }

    private static void incrementUserCount(MyCompleteListener completeListener) {
        g_firestore.collection("USERS").document("TOTAL_USERS").update("COUNT", FieldValue.increment(1))
                .addOnSuccessListener(unused -> completeListener.onSucces())
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public static void updateTotalScore(String email, int quizScore, MyCompleteListener completeListener) {
        g_firestore.collection("USERS").document(email).update("TOTAL_SCORE", FieldValue.increment(quizScore))
                .addOnSuccessListener(aVoid -> completeListener.onSucces())
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public static void loadJournals(String institution, MyCompleteListener listener) {
        g_journalList.clear(); g_militaryJournalList.clear(); g_homeJournalList.clear();
        if (g_firestore != null) {
            g_firestore.collection("JOURNAL").orderBy("JOURNAL_DATE").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            JournalEntry entry = new JournalEntry(doc.getString("JOURNAL_TITLE"), doc.getString("JOURNAL_SUBTITLE"), doc.getString("JOURNAL_DATE"), doc.getString("JOURNAL_IMAGE"), doc.getString("JOURNAL_LINK"));
                            if (institution.equals(doc.getString("JOURNAL_TAG"))) g_militaryJournalList.add(entry); else g_homeJournalList.add(entry);
                            g_journalList.add(entry);
                        }
                        listener.onSucces();
                    })
                    .addOnFailureListener(e -> listener.onFailure());
        } else { listener.onFailure(); }
    }

    public static void loadRanks(String institution, MyCompleteListener completeListener) {
        g_rankList.clear();
        FirebaseFirestore.getInstance().document("MILITARY/RO/" + institution + "/RANKS").get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                try {
                                    ArrayList<String> rankData = (ArrayList<String>) entry.getValue();
                                    if (rankData != null && rankData.size() >= 2) g_rankList.add(new RankModel(rankData.get(0), rankData.get(1)));
                                } catch (Exception e) { Log.e(TAG, "Error parsing rank: " + e.getMessage()); }
                            }
                        }
                    }
                    completeListener.onSucces();
                })
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public static void loadCategories(Context context, String institution, MyCompleteListener listener) {
        g_catList.clear();
        g_militaryCatList.clear();
        g_homeCatList.clear();

        g_firestore.collection("QUIZZES").get()
                .addOnSuccessListener(quizSnapshots -> {
                    Map<String, List<Quizz>> institutionMap = new HashMap<>();
                    Map<String, List<Quizz>> otherMap = new HashMap<>();

                    for (QueryDocumentSnapshot quizDoc : quizSnapshots) {
                        try {
                            String category = quizDoc.getString("category");
                            if (category == null || category.isEmpty()) continue;

                            Object tagsObj = quizDoc.get("tags");
                            boolean isInstitution = false;

                            if (tagsObj != null) {
                                if (tagsObj instanceof String) {
                                    // Handle single string tag
                                    String tagString = ((String) tagsObj).trim();
                                    isInstitution = institution.trim().equalsIgnoreCase(tagString);
                                    Log.d(TAG, "String comparison: '" + institution.trim() + "' vs '" + tagString + "' = " + isInstitution);
                                } else if (tagsObj instanceof List) {
                                    // Handle list of tags
                                    List<?> tagsList = (List<?>) tagsObj;
                                    for (Object tag : tagsList) {
                                        if (tag instanceof String) {
                                            String tagString = ((String) tag).trim();
                                            if (institution.trim().equalsIgnoreCase(tagString)) {
                                                isInstitution = true;
                                                Log.d(TAG, "List match found: " + tagString);
                                                break;
                                            }
                                        }
                                    }
                                    Log.d(TAG, "List comparison result: " + isInstitution);
                                }
                            }

                            Quizz quiz = new Quizz(
                                    quizDoc.getString("title"),
                                    quizDoc.getString("imageUrl"),
                                    quizDoc.getId(),
                                    true,
                                    quizDoc.getString("createdBy")
                            );

                            Map<String, List<Quizz>> targetMap = isInstitution ? institutionMap : otherMap;
                            if (!targetMap.containsKey(category)) {
                                targetMap.put(category, new ArrayList<>());
                            }
                            Objects.requireNonNull(targetMap.get(category)).add(quiz);

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing quiz: " + e.getMessage(), e);
                        }
                    }

                    // Create category models
                    for (Map.Entry<String, List<Quizz>> entry : institutionMap.entrySet()) {
                        g_militaryCatList.add(new CategoryModel(
                                entry.getKey(),
                                entry.getKey(),
                                entry.getValue().size(),
                                entry.getValue()
                        ));
                    }

                    for (Map.Entry<String, List<Quizz>> entry : otherMap.entrySet()) {
                        g_homeCatList.add(new CategoryModel(
                                entry.getKey(),
                                entry.getKey(),
                                entry.getValue().size(),
                                entry.getValue()
                        ));
                    }

                    g_catList.addAll(g_militaryCatList);
                    g_catList.addAll(g_homeCatList);

                    listener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories: " + e.getMessage(), e);
                    Toast.makeText(context, "Error loading categories", Toast.LENGTH_LONG).show();
                    listener.onFailure();
                });
    }

    public static void loadMilitaryCategories(Context context, String institution, MyCompleteListener listener) {
        if (g_militaryCatList.isEmpty() && g_homeCatList.isEmpty())
            loadCategories(context, institution, listener);
        else listener.onSucces();
    }

    public static void loadQuestions(String categoryId, String quizId, MyCompleteListener completeListener) {
        g_quesList.clear();
        g_firestore.collection("QUIZZES").document(quizId).collection("QUESTIONS").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            List<String> options = (List<String>) doc.get("options");
                            Long index = doc.getLong("correctAnswerIndex"), points = doc.getLong("points");
                            if (options != null && options.size() >= 4 && index != null && points != null) {
                                g_quesList.add(new QuestionModel(doc.getString("question"), doc.getString("imageUrl"), options, index.intValue(), points.intValue()));
                            }
                        } catch (Exception e) { Log.e(TAG, "Error processing question: " + e.getMessage()); }
                    }
                    completeListener.onSucces();
                })
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public static void loadLearningPath(MyCompleteListener listener) {
        if (g_firestore == null) {
            g_firestore = FirebaseFirestore.getInstance();
        }
        g_firestore.collection("LEARNING").limit(1).get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Log.e(TAG, "No learning path found");
                listener.onFailure();
                return;
            }
            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
            String pathId = doc.getId();
            String title = doc.getString("pathTitle");
            List<String> nodeIds = (List<String>) doc.get("pathNodes");
            List<String> nodeTypeStrings = (List<String>) doc.get("pathTypes");
            Log.d(TAG, "Path ID: " + pathId);
            Log.d(TAG, "Path Title: " + title);
            Log.d(TAG, "Node IDs: " + (nodeIds != null ? nodeIds.toString() : "null"));
            Log.d(TAG, "Node Types: " + (nodeTypeStrings != null ? nodeTypeStrings.toString() : "null"));
            if (nodeIds == null || nodeTypeStrings == null || nodeIds.size() != nodeTypeStrings.size()) {
                Log.e(TAG, "Invalid node data - arrays are null or different sizes");
                listener.onFailure();
                return;
            }
            List<Long> nodeTypes = new ArrayList<>();
            for (String typeString : nodeTypeStrings) {
                String trimmedType = typeString != null ? typeString.trim() : "";
                Log.d(TAG, "Processing type: '" + trimmedType + "' (original: '" + typeString + "')");
                if ("QUIZZES".equals(trimmedType)) {
                    nodeTypes.add(0L);
                } else if ("FLASHCARDS".equals(trimmedType)) {
                    nodeTypes.add(1L);
                } else {
                    Log.w(TAG, "Unknown node type: '" + trimmedType + "'");
                    nodeTypes.add(0L);
                }
            }
            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (int i = 0; i < nodeIds.size(); i++) {
                String id = nodeIds.get(i);
                String collection = nodeTypes.get(i) == 0 ? "QUIZZES" : "FLASHCARDS";
                Log.d(TAG, "Fetching from " + collection + " with ID: " + id);
                tasks.add(g_firestore.collection(collection).document(id).get());
            }
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                List<LearningPathNode> pathNodesList = new ArrayList<>();
                for (int i = 0; i < results.size(); i++) {
                    DocumentSnapshot nodeDoc = (DocumentSnapshot) results.get(i);
                    if (nodeDoc.exists()) {
                        long type = nodeTypes.get(i);
                        String id = nodeIds.get(i);
                        String nodeTitle;
                        if (type == 0) {
                            nodeTitle = nodeDoc.getString("title");
                        } else {
                            nodeTitle = nodeDoc.getString("cardTitle");
                        }
                        Log.d(TAG, "Node " + i + ": ID=" + id + ", Type=" + type + ", Title=" + nodeTitle);
                        pathNodesList.add(new LearningPathNode(id, (int) type, nodeTitle));
                    } else {
                        Log.w(TAG, "Document does not exist for ID: " + nodeIds.get(i));
                    }
                }
                g_learningPath = new LearningPath(pathId, title, pathNodesList);
                Log.d(TAG, "Learning path loaded successfully with " + pathNodesList.size() + " nodes");
                listener.onSucces();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching node documents", e);
                listener.onFailure();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading learning path", e);
            listener.onFailure();
        });
    }

    public static void loadFlashcards(String flashcardId, MyCompleteListener listener) {
        g_flashcardList.clear();
        g_firestore.collection("FLASHCARDS").document(flashcardId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> data = doc.getData();
                        List<Integer> keys = new ArrayList<>();
                        for (String key : data.keySet()) try { keys.add(Integer.parseInt(key)); } catch (NumberFormatException ignored) {}
                        Collections.sort(keys);
                        for (Integer key : keys) {
                            List<String> cardData = (List<String>) data.get(String.valueOf(key));
                            if (cardData != null && cardData.size() >= 2) {
                                g_flashcardList.add(new FlashcardModel(cardData.get(0), cardData.get(1), cardData.size() > 2 ? cardData.get(2) : null, cardData.size() > 3 ? cardData.get(3) : null));
                            }
                        }
                        listener.onSucces();
                    } else { listener.onFailure(); }
                }).addOnFailureListener(e -> listener.onFailure());
    }

    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            default: return "";
        }
    }

    public static void checkUserPermissions(String email, PermissionCallback callback) {
        g_firestore.collection("USERS").document(email).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        boolean isAdmin = doc.getBoolean("ADMIN") != null ? doc.getBoolean("ADMIN") : false;
                        boolean isPremium = doc.getBoolean("PREMIUM") != null ? doc.getBoolean("PREMIUM") : false;
                        String institution = doc.getString("INSTITUTION");
                        callback.onPermissionsReceived(isAdmin, isPremium, institution);
                    } else {
                        callback.onFailure();
                    }
                })
                .addOnFailureListener(e -> callback.onFailure());
    }

    public static void clearAllCache() {
        Log.d(TAG, "Clearing all DbQuery cache");
        g_journalList.clear();
        g_catList.clear();
        g_quesList.clear();
        g_rankList.clear();
        g_militaryJournalList.clear();
        g_homeJournalList.clear();
        g_flashcardList.clear();
        g_militaryCatList.clear();
        g_homeCatList.clear();
        g_allLearningPaths.clear();
        g_learningPath = null;
    }

    public static void uploadStudentsWithAccessCodesDirectly(List<AccessCodeData> accessCodes, MyCompleteListener completeListener) {
        WriteBatch batch = g_firestore.batch();
        for (AccessCodeData accessCode : accessCodes) {
            DocumentReference docRef = g_firestore.collection("ACCESS_CODES").document(accessCode.accessCode);
            Map<String, Object> data = new ArrayMap<>();
            data.put("name", accessCode.name);
            data.put("institution", accessCode.institution);
            data.put("photo", accessCode.photo);
            data.put("height", accessCode.height);
            data.put("pluton", accessCode.pluton);
            data.put("rank", accessCode.rank);
            data.put("year", accessCode.year);
            data.put("createdAt", System.currentTimeMillis());
            data.put("locked", false);
            batch.set(docRef, data);
        }
        batch.commit()
                .addOnSuccessListener(unused -> completeListener.onSucces())
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public interface PermissionCallback {
        void onPermissionsReceived(boolean isAdmin, boolean isPremium, String institution);
        void onFailure();
    }

    public static void uploadAccessCodes(List<AccessCodeData> accessCodes, MyCompleteListener completeListener) {
        WriteBatch batch = g_firestore.batch();
        for (AccessCodeData accessCode : accessCodes) {
            DocumentReference docRef = g_firestore.collection("ACCESS_CODES").document(accessCode.accessCode);
            Map<String, Object> data = new ArrayMap<>();
            data.put("name", accessCode.name);
            data.put("email", accessCode.email);
            data.put("institution", accessCode.institution);
            data.put("photo", accessCode.photo);
            data.put("height", accessCode.height);
            data.put("pluton", accessCode.pluton);
            data.put("rank", accessCode.rank);
            data.put("createdAt", System.currentTimeMillis());
            batch.set(docRef, data);
        }
        batch.commit()
                .addOnSuccessListener(unused -> completeListener.onSucces())
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public interface AccessCodeValidationCallback {
        void onAccessCodeValid(AccessCodeData data);
        void onAccessCodeInvalid();
    }

    public static class AccessCodeData {
        public String accessCode;
        public String name;
        public String email;
        public String institution;
        public String photo;
        public int height;
        public int pluton;
        public int rank;
        public String year;

        public AccessCodeData() {}

        public AccessCodeData(String accessCode, String name, String email, String institution,
                              String photo, int height, int pluton, int rank) {
            this.accessCode = accessCode;
            this.name = name;
            this.email = email;
            this.institution = institution;
            this.photo = photo;
            this.height = height;
            this.pluton = pluton;
            this.rank = rank;
        }
    }

    public static void uploadStudentsWithAccessCodes(Context context, Uri fileUri, String institution, String year, MyCompleteListener completeListener) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            List<AccessCodeData> accessCodes = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String accessCode = getCellValueAsString(row.getCell(0));
                    String name = getCellValueAsString(row.getCell(1));
                    String institutionFromFile = getCellValueAsString(row.getCell(2));
                    String yearFromFile = getCellValueAsString(row.getCell(3));
                    String photo = getCellValueAsString(row.getCell(4));
                    String height = getCellValueAsString(row.getCell(5));
                    String pluton = getCellValueAsString(row.getCell(6));
                    String rank = getCellValueAsString(row.getCell(7));
                    AccessCodeData accessCodeData = new AccessCodeData();
                    accessCodeData.accessCode = accessCode;
                    accessCodeData.name = name;
                    accessCodeData.institution = institutionFromFile.isEmpty() ? institution : institutionFromFile;
                    accessCodeData.year = yearFromFile.isEmpty() ? year : yearFromFile;
                    accessCodeData.photo = photo;
                    accessCodeData.height = Integer.parseInt(height.isEmpty() ? "175" : height);
                    accessCodeData.pluton = Integer.parseInt(pluton.isEmpty() ? "1" : pluton);
                    accessCodeData.rank = Integer.parseInt(rank.isEmpty() ? "0" : rank);
                    accessCodes.add(accessCodeData);
                }
            }
            WriteBatch batch = g_firestore.batch();
            for (AccessCodeData accessCode : accessCodes) {
                DocumentReference docRef = g_firestore.collection("ACCESS_CODES").document(accessCode.accessCode);
                Map<String, Object> data = new ArrayMap<>();
                data.put("name", accessCode.name);
                data.put("institution", accessCode.institution);
                data.put("photo", accessCode.photo);
                data.put("height", accessCode.height);
                data.put("pluton", accessCode.pluton);
                data.put("rank", accessCode.rank);
                data.put("year", accessCode.year);
                data.put("createdAt", System.currentTimeMillis());
                data.put("locked", false);
                batch.set(docRef, data);
            }
            batch.commit()
                    .addOnSuccessListener(unused -> completeListener.onSucces())
                    .addOnFailureListener(e -> completeListener.onFailure());
            workbook.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Error uploading students with access codes", e);
            completeListener.onFailure();
        }
    }

    public static void validateAndLockAccessCode(String accessCode, String email, AccessCodeValidationCallback callback) {
        if (accessCode == null || accessCode.trim().isEmpty()) {
            callback.onAccessCodeInvalid();
            return;
        }
        g_firestore.collection("ACCESS_CODES").document(accessCode.trim()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean locked = doc.getBoolean("locked");
                        if (locked != null && locked) {
                            callback.onAccessCodeInvalid();
                            return;
                        }
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            AccessCodeData accessCodeData = new AccessCodeData();
                            accessCodeData.name = (String) data.get("name");
                            accessCodeData.email = email;
                            accessCodeData.institution = (String) data.get("institution");
                            accessCodeData.photo = (String) data.get("photo");
                            accessCodeData.year = (String) data.get("year");
                            Long height = (Long) data.get("height");
                            Long pluton = (Long) data.get("pluton");
                            Long rank = (Long) data.get("rank");
                            accessCodeData.height = height != null ? height.intValue() : 175;
                            accessCodeData.pluton = pluton != null ? pluton.intValue() : 1;
                            accessCodeData.rank = rank != null ? rank.intValue() : 0;
                            g_firestore.collection("ACCESS_CODES").document(accessCode.trim())
                                    .update("locked", true)
                                    .addOnSuccessListener(unused -> callback.onAccessCodeValid(accessCodeData))
                                    .addOnFailureListener(e -> callback.onAccessCodeInvalid());
                        } else {
                            callback.onAccessCodeInvalid();
                        }
                    } else {
                        callback.onAccessCodeInvalid();
                    }
                })
                .addOnFailureListener(e -> callback.onAccessCodeInvalid());
    }
}