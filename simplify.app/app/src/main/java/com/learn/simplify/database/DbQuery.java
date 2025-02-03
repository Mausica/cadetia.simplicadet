package com.learn.simplify.database;

import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.learn.simplify.listeners.MyCompleteListener;
import com.learn.simplify.model.CategoryModel;
import com.learn.simplify.model.QuestionModel;
import com.learn.simplify.model.Quizz;
import com.learn.simplify.model.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DbQuery {

    public static final String TAG = "DbQuery";
    public static FirebaseFirestore g_firestore;
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

    public static void checkIfQuizHasQuestions(String categoryId, String testId, MyCompleteListener listener) {
        g_firestore.collection("QUIZ")
                .document(categoryId)
                .collection("TESTS_LIST")
                .document("TESTS_INFO")
                .collection("QUESTIONS")
                .document(testId)
                .collection("QUESTION_INFO")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        listener.onSucces();
                    } else {
                        listener.onFailure();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking quiz questions: ", e);
                    listener.onFailure();
                });
    }

    public static void loadCategories(MyCompleteListener listener) {
        g_catList.clear();

        g_firestore.collection("QUIZ")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot catDoc : queryDocumentSnapshots) {
                        String catID = catDoc.getId();
                        String catName = catDoc.getString("NAME");
                        int noOfTests;
                        if (catDoc.getLong("NO_OF_TESTS") != null) {
                            noOfTests = catDoc.getLong("NO_OF_TESTS").intValue();
                        } else {
                            noOfTests = 0;
                            // Handle the case when NO_OF_TESTS field is null
                            Log.e(TAG, "NO_OF_TESTS field is null for category ID: " + catID);
                        }

                        // Fetch quizzes for this category
                        List<Quizz> quizzList = new ArrayList<>();
                        g_firestore.collection("QUIZ").document(catID).collection("TESTS_LIST")
                                .get()
                                .addOnSuccessListener(quizzSnapshots -> {
                                    for (QueryDocumentSnapshot quizzDoc : quizzSnapshots) {
                                        for (int i = 1; i <= noOfTests; i++) {
                                            String quizzTitle = quizzDoc.getString("TEST" + i + "_TITLE");
                                            String quizzImage = quizzDoc.getString("TEST" + i + "_IMAGE");
                                            String testId = quizzDoc.getString("TEST" + i + "_ID");

                                            // Check if quiz has questions
                                            checkIfQuizHasQuestions(catID, testId, new MyCompleteListener() {
                                                @Override
                                                public void onSucces() {
                                                    // Quiz has questions, add it to the list with normal alpha
                                                    Quizz quizz = new Quizz(quizzTitle, quizzImage, testId, true);
                                                    quizzList.add(quizz);
                                                    if (quizzList.size() == noOfTests) {
                                                        // Create CategoryModel instance with quizzes and add to list
                                                        CategoryModel category = new CategoryModel(catID, catName, noOfTests, quizzList);
                                                        g_catList.add(category);

                                                        // Notify listener if all categories and quizzes are loaded
                                                        if (g_catList.size() == queryDocumentSnapshots.size()) {
                                                            listener.onSucces();
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure() {
                                                    // Quiz doesn't have questions, add it to the list with lower alpha
                                                    Quizz quizz = new Quizz(quizzTitle, quizzImage, testId, false);
                                                    quizzList.add(quizz);
                                                    if (quizzList.size() == noOfTests) {
                                                        // Create CategoryModel instance with quizzes and add to list
                                                        CategoryModel category = new CategoryModel(catID, catName, noOfTests, quizzList);
                                                        g_catList.add(category);

                                                        // Notify listener if all categories and quizzes are loaded
                                                        if (g_catList.size() == queryDocumentSnapshots.size()) {
                                                            listener.onSucces();
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Handle failure to fetch quizzes for this category
                                    listener.onFailure();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to fetch categories
                    listener.onFailure();
                });
    }

    public static void loadQuestions(String categoryId, String testId, MyCompleteListener completeListener) {
        g_quesList.clear();

        g_firestore.collection("QUIZ")
                .document(categoryId)
                .collection("TESTS_LIST")
                .document("TESTS_INFO")
                .collection("QUESTIONS")
                .document(testId)
                .collection("QUESTION_INFO")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        g_quesList.add(new QuestionModel(
                                doc.getString("QUESTION"),
                                doc.getString("A"),
                                doc.getString("B"),
                                doc.getString("C"),
                                doc.getString("D"),
                                doc.getString("ANSWER"),
                                doc.getString("IMAGE")
                        ));
                    }
                    Log.e(TAG, "Number of questions loaded: " + DbQuery.g_quesList.size());
                    completeListener.onSucces();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading questions: ", e);
                    Log.e(TAG, "Number of questions not loaded: " + DbQuery.g_quesList.size());
                    completeListener.onFailure();
                });

    }
}
