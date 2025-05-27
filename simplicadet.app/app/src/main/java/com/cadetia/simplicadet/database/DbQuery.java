package com.cadetia.simplicadet.database;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static int g_selected_cat_index = 0;
    public static int g_selected_test_index = 0;

    public static List<CategoryModel> g_militaryCatList = new ArrayList<>();
    public static List<CategoryModel> g_homeCatList = new ArrayList<>();

    public static class LearningPath {
        public String id;
        public String title;
        public List<LearningPathNode> nodes;
        public LearningPath(String id, String title, List<LearningPathNode> nodes) { this.id = id; this.title = title; this.nodes = nodes; }
    }

    public static class LearningPathNode {
        public String id;
        public int type;
        public String title;
        public LearningPathNode(String id, int type, String title) { this.id = id; this.type = type; this.title = title; }
    }

    public static class FlashcardModel {
        public String question, answer, frontImage, backImage;
        public FlashcardModel(String q, String a, String fI, String bI) {
            this.question = q; this.answer = a; this.frontImage = fI; this.backImage = bI;
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

    public static void createUserData(String email, String name, String photo, MyCompleteListener completeListener) {
        g_firestore.collection("USERS").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> updates = new ArrayMap<>();
                        updates.put("NAME", name); updates.put("PHOTO", photo);
                        documentSnapshot.getReference().update(updates)
                                .addOnSuccessListener(unused -> completeListener.onSucces())
                                .addOnFailureListener(e -> completeListener.onFailure());
                    } else {
                        Map<String, Object> userData = new ArrayMap<>();
                        userData.put("EMAIL_ID", email); userData.put("NAME", name);
                        userData.put("PHOTO", photo); userData.put("TOTAL_SCORE", 0);
                        g_firestore.collection("USERS").document(email).set(userData)
                                .addOnSuccessListener(unused -> incrementUserCount(completeListener))
                                .addOnFailureListener(e -> completeListener.onFailure());
                    }
                })
                .addOnFailureListener(e -> completeListener.onFailure());
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

    public static void loadJournals(MyCompleteListener listener) {
        g_journalList.clear(); g_militaryJournalList.clear(); g_homeJournalList.clear();
        if (g_firestore != null) {
            g_firestore.collection("JOURNAL").orderBy("JOURNAL_DATE").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            JournalEntry entry = new JournalEntry(doc.getString("JOURNAL_TITLE"), doc.getString("JOURNAL_SUBTITLE"), doc.getString("JOURNAL_DATE"), doc.getString("JOURNAL_IMAGE"), doc.getString("JOURNAL_LINK"));
                            if ("CNMTV".equals(doc.getString("JOURNAL_TAG"))) g_militaryJournalList.add(entry); else g_homeJournalList.add(entry);
                            g_journalList.add(entry);
                        }
                        listener.onSucces();
                    })
                    .addOnFailureListener(e -> listener.onFailure());
        } else { listener.onFailure(); }
    }

    public static void loadMilitaryJournals(Context context, MyCompleteListener listener) { if (g_militaryJournalList.isEmpty()) loadJournals(listener); else listener.onSucces(); }
    public static void loadHomeJournals(Context context, MyCompleteListener listener) { if (g_homeJournalList.isEmpty()) loadJournals(listener); else listener.onSucces(); }

    public static void loadRanks(MyCompleteListener completeListener) {
        g_rankList.clear();
        FirebaseFirestore.getInstance().document("MILITARY/RO/CNMTV/RANKS").get()
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

    public static void loadCategories(Context context, MyCompleteListener listener) {
        g_catList.clear(); g_militaryCatList.clear(); g_homeCatList.clear();
        g_firestore.collection("QUIZZES").get()
                .addOnSuccessListener(quizSnapshots -> {
                    Map<String, List<Quizz>> cnmtvMap = new HashMap<>(), otherMap = new HashMap<>();
                    for (QueryDocumentSnapshot quizDoc : quizSnapshots) {
                        try {
                            String category = quizDoc.getString("category"); if (category == null || category.isEmpty()) continue;
                            Object tagsObj = quizDoc.get("tags");
                            boolean isCNMTV = (tagsObj instanceof String && "CNMTV".equals(tagsObj)) || (tagsObj instanceof List && ((List<String>) tagsObj).contains("CNMTV"));
                            Quizz quiz = new Quizz(quizDoc.getString("title"), quizDoc.getString("imageUrl"), quizDoc.getId(), true, quizDoc.getString("createdBy"));
                            Map<String, List<Quizz>> targetMap = isCNMTV ? cnmtvMap : otherMap;
                            if (!targetMap.containsKey(category)) targetMap.put(category, new ArrayList<>());
                            targetMap.get(category).add(quiz);
                        } catch (Exception e) { Log.e(TAG, "Error processing quiz: " + e.getMessage()); }
                    }
                    for (Map.Entry<String, List<Quizz>> entry : cnmtvMap.entrySet()) g_militaryCatList.add(new CategoryModel(entry.getKey(), entry.getKey(), entry.getValue().size(), entry.getValue()));
                    for (Map.Entry<String, List<Quizz>> entry : otherMap.entrySet()) g_homeCatList.add(new CategoryModel(entry.getKey(), entry.getKey(), entry.getValue().size(), entry.getValue()));
                    g_catList.addAll(g_militaryCatList); g_catList.addAll(g_homeCatList);
                    listener.onSucces();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "Error: " + e.getMessage()); Toast.makeText(context, "Error", Toast.LENGTH_LONG).show(); listener.onFailure(); });
    }

    public static void loadMilitaryCategories(Context context, MyCompleteListener listener) { if (g_militaryCatList.isEmpty() && g_homeCatList.isEmpty()) loadCategories(context, listener); else listener.onSucces(); }
    public static void loadHomeCategories(Context context, MyCompleteListener listener) { if (g_militaryCatList.isEmpty() && g_homeCatList.isEmpty()) loadCategories(context, listener); else listener.onSucces(); }

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
        g_firestore.collection("LEARNING").limit(1).get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                Log.e(TAG, "No learning path found");
                listener.onFailure();
                return;
            }

            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
            String pathId = doc.getId();
            String title = doc.getString("pathTitle");

            // Get the arrays from Firebase
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

            // Convert type strings to integers
            List<Long> nodeTypes = new ArrayList<>();
            for (String typeString : nodeTypeStrings) {
                // Trim whitespace and newlines from the type string
                String trimmedType = typeString != null ? typeString.trim() : "";
                Log.d(TAG, "Processing type: '" + trimmedType + "' (original: '" + typeString + "')");

                if ("QUIZZES".equals(trimmedType)) {
                    nodeTypes.add(0L);
                } else if ("FLASHCARDS".equals(trimmedType)) {
                    nodeTypes.add(1L);
                } else {
                    Log.w(TAG, "Unknown node type: '" + trimmedType + "'");
                    nodeTypes.add(0L); // Default to quiz
                }
            }

            // Create tasks to fetch each node document
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
                            // For QUIZZES - use "title" field
                            nodeTitle = nodeDoc.getString("title");
                        } else {
                            // For FLASHCARDS - use "cardTitle" field
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
}