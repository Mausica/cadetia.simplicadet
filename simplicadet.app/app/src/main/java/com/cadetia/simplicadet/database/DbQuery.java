package com.cadetia.simplicadet.database;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.cadetia.simplicadet.model.JournalEntry;
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

    public static int g_selected_cat_index = 0;
    public static int g_selected_test_index = 0;

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

    public static void loadCategories(Context context, MyCompleteListener listener) {
        g_catList.clear();
        Log.e(TAG, "Starting loadCategories");

        g_firestore.collection("QUIZES")
                .get()
                .addOnSuccessListener(categorySnapshots -> {
                    int totalCategories = categorySnapshots.size();
                    AtomicInteger processedCategories = new AtomicInteger(0);

                    if (totalCategories == 0) {
                        listener.onSucces();
                        return;
                    }

                    for (QueryDocumentSnapshot catDoc : categorySnapshots) {
                        String catID = catDoc.getId();
                        int noOfTests = catDoc.contains("noTests") ? catDoc.getLong("noTests").intValue() : 0;
                        List<String> subCollectionNames = (List<String>) catDoc.get("subcollections");

                        if (subCollectionNames == null || subCollectionNames.isEmpty()) {
                            Log.e(TAG, "No subcollections found for category: " + catID);
                            if (processedCategories.incrementAndGet() == totalCategories) {
                                listener.onSucces();
                            }
                            continue;
                        }

                        List<Quizz> quizzList = new ArrayList<>();
                        AtomicInteger processedTests = new AtomicInteger(0);

                        for (String subCollectionName : subCollectionNames) {
                            g_firestore.collection("QUIZES").document(catID)
                                    .collection(subCollectionName)
                                    .document("Info")
                                    .get()
                                    .addOnSuccessListener(infoDoc -> {
                                        if (infoDoc.exists()) {
                                            String quizzImage = infoDoc.getString("imageUrl");
                                            String createdBy = infoDoc.getString("createdBy");
                                            Quizz quizz = new Quizz(subCollectionName, quizzImage, subCollectionName, true, createdBy);
                                            quizzList.add(quizz);
                                        } else {
                                            Log.e(TAG, "Document 'Info' does not exist in subcollection: " + subCollectionName);
                                            Toast.makeText(context, "Incompatible quiz: " + subCollectionName, Toast.LENGTH_LONG).show();
                                        }

                                        if (processedTests.incrementAndGet() == subCollectionNames.size()) {
                                            createCategory(catID, noOfTests, quizzList, processedCategories, totalCategories, listener);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error fetching 'Info' document: " + e.getMessage());
                                        if (processedTests.incrementAndGet() == subCollectionNames.size()) {
                                            createCategory(catID, noOfTests, quizzList, processedCategories, totalCategories, listener);
                                        }
                                        Toast.makeText(context, "Error fetching 'Info' document", Toast.LENGTH_LONG).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading categories: " + e.getMessage());
                    Toast.makeText(context, "Error loading categories", Toast.LENGTH_LONG).show();
                    listener.onFailure();
                });
    }



    private static void createCategory(String catID, int noTests, List<Quizz> quizzes,
                                       AtomicInteger counter, int total, MyCompleteListener listener) {
        CategoryModel category = new CategoryModel(catID, catID, noTests, quizzes);
        g_catList.add(category);

        if (counter.incrementAndGet() == total) {
            listener.onSucces();
        }
    }

    
    public static void loadQuestions(String categoryId, String testTitle, MyCompleteListener completeListener) {
        g_quesList.clear();

        g_firestore.collection("QUIZES").document(categoryId).collection(testTitle)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (!doc.getId().equals("Info")) {
                            List<String> options = (List<String>) doc.get("options");

                            if (options == null || options.size() < 4) {
                                continue;
                            }

                            g_quesList.add(new QuestionModel(
                                    doc.getString("question"),
                                    doc.getString("imageUrl"),
                                    options,
                                    doc.getLong("correctAnswerIndex").intValue(),
                                    doc.getLong("points").intValue()
                            ));
                        }
                    }
                    completeListener.onSucces();
                })
                .addOnFailureListener(e -> completeListener.onFailure());
    }


    public static void loadJournals(MyCompleteListener listener) {
        g_journalList.clear();

        g_firestore.collection("JOURNAL").orderBy("JOURNAL_DATE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        g_journalList.add(new JournalEntry(
                                doc.getString("JOURNAL_TITLE"),
                                doc.getString("JOURNAL_SUBTITLE"),
                                doc.getString("JOURNAL_DATE"),
                                doc.getString("JOURNAL_IMAGE"),
                                doc.getString("JOURNAL_LINK")
                        ));
                    }
                    listener.onSucces();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading journals: ", e);
                    listener.onFailure();
                });
    }


}
