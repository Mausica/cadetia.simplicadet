package com.cadetia.simplicadet.database;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.RankModel;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.QuestionModel;
import com.cadetia.simplicadet.model.Quizz;
import com.cadetia.simplicadet.model.UserModel;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DbQuery {

    public static final String TAG = "DbQuery";
    public static FirebaseFirestore g_firestore;

    public static List<JournalEntry> g_journalList = new ArrayList<>();
    public static List<CategoryModel> g_catList = new ArrayList<>();
    public static List<QuestionModel> g_quesList = new ArrayList<>();
    public static List<RankModel> g_rankList = new ArrayList<>();
    public static List<JournalEntry> g_militaryJournalList = new ArrayList<>();
    public static List<JournalEntry> g_homeJournalList = new ArrayList<>();

    public static int g_selected_cat_index = 0;
    public static int g_selected_test_index = 0;

    // Added separate lists for military and home fragments
    public static List<CategoryModel> g_militaryCatList = new ArrayList<>();
    public static List<CategoryModel> g_homeCatList = new ArrayList<>();

    public interface UserScoreListener {
        void onUserScoresReceived(List<UserModel> userList);
        void onFailure();
    }

    public static void getUsersSortedByScore(UserScoreListener userScoreListener) {
        g_firestore.collection("USERS")
                .orderBy("TOTAL_SCORE", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserModel> userList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UserModel user = document.toObject(UserModel.class);
                        userList.add(user);
                    }
                    userScoreListener.onUserScoresReceived(userList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users sorted by score: ", e);
                    userScoreListener.onFailure();
                });
    }

    public static void createUserData(String email, String name, String photo, MyCompleteListener completeListener) {
        // Check if user already exists
        g_firestore.collection("USERS")
                .document(email) // Use email as document ID
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User already exists, update user's name and photo
                        DocumentReference userDocRef = documentSnapshot.getReference();
                        Map<String, Object> updates = new ArrayMap<>();
                        updates.put("NAME", name);
                        updates.put("PHOTO", photo);

                        userDocRef.update(updates)
                                .addOnSuccessListener(unused -> completeListener.onSucces())
                                .addOnFailureListener(e -> completeListener.onFailure());
                    } else {
                        // User does not exist, proceed with user creation
                        Map<String, Object> userData = new ArrayMap<>();
                        userData.put("EMAIL_ID", email);
                        userData.put("NAME", name);
                        userData.put("PHOTO", photo);
                        userData.put("TOTAL_SCORE", 0);

                        // Create a new user with email as document ID
                        g_firestore.collection("USERS")
                                .document(email)
                                .set(userData)
                                .addOnSuccessListener(unused -> {
                                    // Increment the count
                                    incrementUserCount(completeListener);
                                })
                                .addOnFailureListener(e -> {
                                    // Handle failure to create user
                                    completeListener.onFailure();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to check user existence
                    completeListener.onFailure();
                });
    }

    private static void incrementUserCount(MyCompleteListener completeListener) {
        DocumentReference countDoc = g_firestore.collection("USERS").document("TOTAL_USERS");
        countDoc.update("COUNT", FieldValue.increment(1))
                .addOnSuccessListener(unused -> completeListener.onSucces())
                .addOnFailureListener(e -> completeListener.onFailure());
    }

    public static void updateTotalScore(String email, int quizScore, MyCompleteListener completeListener) {
        DocumentReference userDocRef = g_firestore.collection("USERS").document(email);
        userDocRef.update("TOTAL_SCORE", FieldValue.increment(quizScore))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Total score updated successfully.");
                    completeListener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating total score: ", e);
                    completeListener.onFailure();
                });
    }
    // Add these lists

    // Modify loadJournals() to categorize by JOURNAL_TAG
    public static void loadJournals(MyCompleteListener listener) {
        g_journalList.clear();
        g_militaryJournalList.clear();
        g_homeJournalList.clear();

        g_firestore.collection("JOURNAL")
                .orderBy("JOURNAL_DATE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        JournalEntry entry = new JournalEntry(
                                doc.getString("JOURNAL_TITLE"),
                                doc.getString("JOURNAL_SUBTITLE"),
                                doc.getString("JOURNAL_DATE"),
                                doc.getString("JOURNAL_IMAGE"),
                                doc.getString("JOURNAL_LINK")
                        );

                        // Check for JOURNAL_TAG (default to home if missing)
                        String tag = doc.getString("JOURNAL_TAG");
                        if ("CNMTV".equals(tag)) {
                            g_militaryJournalList.add(entry); // Military journals
                        } else {
                            g_homeJournalList.add(entry); // Home journals
                        }

                        g_journalList.add(entry); // Optional: keep global list
                    }
                    listener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading journals: ", e);
                    listener.onFailure();
                });
    }

    // Add military/home journal loaders (similar to categories)
    public static void loadMilitaryJournals(Context context, MyCompleteListener listener) {
        if (g_militaryJournalList.isEmpty()) {
            loadJournals(listener); // Reload if empty
        } else {
            listener.onSucces();
        }
    }

    public static void loadHomeJournals(Context context, MyCompleteListener listener) {
        if (g_homeJournalList.isEmpty()) {
            loadJournals(listener); // Reload if empty
        } else {
            listener.onSucces();
        }
    }

    public static void loadRanks(MyCompleteListener completeListener) {
        g_rankList.clear();

        // Correct path to access the RANKS document
        FirebaseFirestore.getInstance()
                .document("MILITARY/RO/CNMTV/RANKS")  // This is a document path (even segments)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Loop through each field in the RANKS document
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                try {
                                    String rankId = entry.getKey();
                                    ArrayList<String> rankData = (ArrayList<String>) entry.getValue();

                                    if (rankData != null && rankData.size() >= 2) {
                                        String name = rankData.get(0);
                                        String imageUrl = rankData.get(1);
                                        g_rankList.add(new RankModel(name, imageUrl));
                                    }
                                } catch (Exception e) {
                                    Log.e("DbQuery", "Error parsing rank data: " + e.getMessage());
                                }
                            }
                        }
                    }
                    completeListener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e("DbQuery", "Error loading ranks: " + e.getMessage());
                    completeListener.onFailure();
                });
    }

    public static void loadCategories(Context context, MyCompleteListener listener) {
        g_catList.clear();
        g_militaryCatList.clear();
        g_homeCatList.clear();

        Log.d(TAG, "Starting loadCategories with new structure");

        // First, we'll get all categories by grouping quizzes
        g_firestore.collection("QUIZZES")
                .get()
                .addOnSuccessListener(quizSnapshots -> {
                    Map<String, List<Quizz>> cnmtvCategoriesMap = new HashMap<>();
                    Map<String, List<Quizz>> otherCategoriesMap = new HashMap<>();

                    for (QueryDocumentSnapshot quizDoc : quizSnapshots) {
                        try {
                            String quizId = quizDoc.getId();
                            String title = quizDoc.getString("title");
                            String category = quizDoc.getString("category");
                            String imageUrl = quizDoc.getString("imageUrl");
                            String createdBy = quizDoc.getString("createdBy");

                            // Check for tags - extract the tag field (could be a string or array)
                            Object tagsObj = quizDoc.get("tags");
                            boolean isCNMTV = false;

                            // Process different tag formats
                            if (tagsObj instanceof String) {
                                isCNMTV = "Pills".equals(tagsObj);
                            } else if (tagsObj instanceof List) {
                                List<String> tags = (List<String>) tagsObj;
                                isCNMTV = tags.contains("Pills");
                            }

                            if (category == null || category.isEmpty()) {
                                Log.w(TAG, "Quiz missing category: " + quizId);
                                continue;
                            }

                            // Create quiz object
                            Quizz quiz = new Quizz(title, imageUrl, quizId, true, createdBy);

                            // Add to the appropriate category list based on tag
                            Map<String, List<Quizz>> targetMap = isCNMTV ? cnmtvCategoriesMap : otherCategoriesMap;

                            if (!targetMap.containsKey(category)) {
                                targetMap.put(category, new ArrayList<>());
                            }
                            targetMap.get(category).add(quiz);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing quiz: " + e.getMessage());
                        }
                    }

                    // Convert maps to category lists
                    // CNMTV categories for military fragment
                    for (Map.Entry<String, List<Quizz>> entry : cnmtvCategoriesMap.entrySet()) {
                        String categoryName = entry.getKey();
                        List<Quizz> quizzes = entry.getValue();

                        CategoryModel category = new CategoryModel(
                                categoryName,
                                categoryName,
                                quizzes.size(),
                                quizzes
                        );
                        g_militaryCatList.add(category);
                    }

                    // Other categories for home fragment
                    for (Map.Entry<String, List<Quizz>> entry : otherCategoriesMap.entrySet()) {
                        String categoryName = entry.getKey();
                        List<Quizz> quizzes = entry.getValue();

                        CategoryModel category = new CategoryModel(
                                categoryName,
                                categoryName,
                                quizzes.size(),
                                quizzes
                        );
                        g_homeCatList.add(category);
                    }

                    // Combine all categories for backward compatibility
                    g_catList.addAll(g_militaryCatList);
                    g_catList.addAll(g_homeCatList);

                    Log.d(TAG, "Loaded " + g_militaryCatList.size() + " CNMTV categories");
                    Log.d(TAG, "Loaded " + g_homeCatList.size() + " other categories");

                    listener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories: " + e.getMessage());
                    Toast.makeText(context, "Error loading categories", Toast.LENGTH_LONG).show();
                    listener.onFailure();
                });
    }

    public static void loadMilitaryCategories(Context context, MyCompleteListener listener) {
        // First ensure all categories are loaded
        if (g_militaryCatList.isEmpty() && g_homeCatList.isEmpty()) {
            loadCategories(context, new MyCompleteListener() {
                @Override
                public void onSucces() {
                    listener.onSucces();
                }

                @Override
                public void onFailure() {
                    listener.onFailure();
                }
            });
        } else {
            // Categories already loaded
            listener.onSucces();
        }
    }

    public static void loadHomeCategories(Context context, MyCompleteListener listener) {
        // First ensure all categories are loaded
        if (g_militaryCatList.isEmpty() && g_homeCatList.isEmpty()) {
            loadCategories(context, new MyCompleteListener() {
                @Override
                public void onSucces() {
                    listener.onSucces();
                }

                @Override
                public void onFailure() {
                    listener.onFailure();
                }
            });
        } else {
            // Categories already loaded
            listener.onSucces();
        }
    }

    public static void loadQuestions(String categoryId, String quizId, MyCompleteListener completeListener) {
        g_quesList.clear();
        Log.d(TAG, "Loading questions for quiz: " + quizId);

        g_firestore.collection("QUIZZES").document(quizId).collection("QUESTIONS")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            List<String> options = (List<String>) doc.get("options");
                            if (options == null || options.size() < 4) {
                                Log.w(TAG, "Question with insufficient options: " + doc.getId());
                                continue;
                            }

                            Long correctAnswerIndex = doc.getLong("correctAnswerIndex");
                            Long points = doc.getLong("points");
                            if (correctAnswerIndex == null || points == null) {
                                Log.w(TAG, "Question missing correctAnswerIndex or points: " + doc.getId());
                                continue;
                            }

                            g_quesList.add(new QuestionModel(
                                    doc.getString("question"),
                                    doc.getString("imageUrl"),
                                    options,
                                    correctAnswerIndex.intValue(),
                                    points.intValue()
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing question: " + e.getMessage());
                        }
                    }

                    Log.d(TAG, "Loaded " + g_quesList.size() + " questions");
                    completeListener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading questions: " + e.getMessage());
                    completeListener.onFailure();
                });
    }

}