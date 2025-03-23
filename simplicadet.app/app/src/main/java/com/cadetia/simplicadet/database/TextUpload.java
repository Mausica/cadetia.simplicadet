package com.cadetia.simplicadet.database;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUpload {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "TextUpload";

    public void uploadQuestionsFromText(Context context, Uri fileUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            String[] parts = content.toString().split("ANSWERS");
            String questionsSection = parts[0].trim();
            String answersSection = parts.length > 1 ? parts[1].trim() : "";

            String category = extractValue(questionsSection, "CAT: ", "\n");
            String testTitle = extractValue(questionsSection, "NAME: ", "\n");
            String creator = extractValue(questionsSection, "CREATOR: ", "\n");
            String imageUrl = extractValue(questionsSection, "IMAGE: ", "\n");

            createInfoDocument(category, testTitle, creator, imageUrl);

            updateCategoryInfo(category, testTitle);

            List<QuestionModel> questions = parseQuestions(questionsSection, answersSection);

            final AtomicInteger successCount = new AtomicInteger(0);
            final AtomicInteger existingCount = new AtomicInteger(0);
            final AtomicInteger totalToProcess = new AtomicInteger(questions.size());

            for (QuestionModel question : questions) {
                checkAndUploadQuestion(category, testTitle, question, successCount, existingCount, totalToProcess, context);
            }

            reader.close();
            inputStream.close();

        } catch (Exception e) {
            Log.e(TAG, "Eroare la procesarea fișierului text", e);
            Toast.makeText(context, "Eroare la încărcare!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createInfoDocument(String category, String testTitle, String creator, String imageUrl) {
        Map<String, Object> infoData = new HashMap<>();
        infoData.put("createdBy", creator);
        infoData.put("imageUrl", imageUrl);

        db.collection("QUIZES")
                .document(category)
                .collection(testTitle)
                .document("Info")
                .set(infoData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Document Info creat/actualizat cu succes"))
                .addOnFailureListener(e -> Log.e(TAG, "Eroare la crearea/actualizarea documentului Info", e));
    }

    private void checkAndUploadQuestion(String category, String testTitle, QuestionModel question,
                                        AtomicInteger successCount, AtomicInteger existingCount,
                                        AtomicInteger totalToProcess, Context context) {
        db.collection("QUIZES")
                .document(category)
                .collection(testTitle)
                .whereEqualTo("question", question.getQuestion())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && querySnapshot.isEmpty()) {
                            // Întrebarea nu există, o adăugăm
                            Map<String, Object> questionData = new HashMap<>();
                            questionData.put("question", question.getQuestion());
                            if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
                                questionData.put("imageUrl", question.getImageUrl());
                            }
                            questionData.put("options", question.getOptions());
                            questionData.put("correctAnswerIndex", question.getCorrectAnswerIndex());
                            questionData.put("points", 1); // Valoare implicită, poate fi modificată

                            db.collection("QUIZES")
                                    .document(category)
                                    .collection(testTitle)
                                    .add(questionData)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d(TAG, "Întrebare adăugată: " + documentReference.getId());
                                        if (successCount.incrementAndGet() + existingCount.get() == totalToProcess.get()) {
                                            showCompletionMessage(context, successCount.get(), existingCount.get());
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Eroare la încărcarea întrebării", e);
                                        if (successCount.get() + existingCount.incrementAndGet() == totalToProcess.get()) {
                                            showCompletionMessage(context, successCount.get(), existingCount.get());
                                        }
                                    });
                        } else {
                            // Întrebarea există deja
                            Log.d(TAG, "Întrebare deja existentă: " + question.getQuestion());
                            if (successCount.get() + existingCount.incrementAndGet() == totalToProcess.get()) {
                                showCompletionMessage(context, successCount.get(), existingCount.get());
                            }
                        }
                    } else {
                        Log.e(TAG, "Eroare la verificarea existenței întrebării", task.getException());
                        if (successCount.get() + existingCount.incrementAndGet() == totalToProcess.get()) {
                            showCompletionMessage(context, successCount.get(), existingCount.get());
                        }
                    }
                });
    }

    private void showCompletionMessage(Context context, int successCount, int existingCount) {
        String message = "Încărcare finalizată! " + successCount + " întrebări noi adăugate, "
                + existingCount + " întrebări existente ignorate.";
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void updateCategoryInfo(String category, String testTitle) {
        db.collection("QUIZES").document(category).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> categoryData = new HashMap<>();
                    List<String> subcollections = new ArrayList<>();
                    int noTests = 1;

                    if (documentSnapshot.exists()) {
                        List<String> existingSubcollections = (List<String>) documentSnapshot.get("subcollections");
                        if (existingSubcollections != null) {
                            subcollections.addAll(existingSubcollections);
                            if (!subcollections.contains(testTitle)) {
                                subcollections.add(testTitle);
                            }
                            noTests = subcollections.size();
                        } else {
                            subcollections.add(testTitle);
                        }
                    } else {
                        subcollections.add(testTitle);
                    }

                    categoryData.put("noTests", noTests);
                    categoryData.put("subcollections", subcollections);

                    db.collection("QUIZES")
                            .document(category)
                            .set(categoryData)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Categorie actualizată: " + category))
                            .addOnFailureListener(e -> Log.e(TAG, "Eroare la actualizarea categoriei", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Eroare la verificarea categoriei", e));
    }

    private List<QuestionModel> parseQuestions(String questionsContent, String answersContent) {
        List<QuestionModel> questions = new ArrayList<>();

        Pattern questionPattern = Pattern.compile("(\\d+)\\. (.*?)(?=\\d+\\. |ANSWERS|$)", Pattern.DOTALL);
        Matcher questionMatcher = questionPattern.matcher(questionsContent);

        while (questionMatcher.find()) {
            String questionText = questionMatcher.group(2).trim();

            String[] questionLines = questionText.split("\n", 2);
            String question = questionLines[0].trim();
            String remainingText = questionLines.length > 1 ? questionLines[1].trim() : "";

            String imageUrl = "";
            if (remainingText.contains("IMAGE:")) {
                Pattern imagePattern = Pattern.compile("IMAGE:(.*?)(?=\\n|$)");
                Matcher imageMatcher = imagePattern.matcher(remainingText);
                if (imageMatcher.find()) {
                    imageUrl = imageMatcher.group(1).trim();
                    remainingText = remainingText.replaceFirst("IMAGE:.*?(?=\\n|$)", "").trim();
                }
            }

            List<String> options = new ArrayList<>();
            Pattern optionPattern = Pattern.compile("[A-D]\\. (.*?)(?=\\n[A-D]\\. |$)", Pattern.DOTALL);
            Matcher optionMatcher = optionPattern.matcher(remainingText);

            while (optionMatcher.find() && options.size() < 4) {
                options.add(optionMatcher.group(1).trim());
            }
            
            int correctIndex = -1;
            if (!answersContent.isEmpty()) {
                String questionNumber = questionMatcher.group(1);
                Pattern answerPattern = Pattern.compile(
                        "^\\s*" + questionNumber + "\\.\\s*([A-D])",
                        Pattern.MULTILINE
                );
                Matcher answerMatcher = answerPattern.matcher(answersContent);
                if (answerMatcher.find()) {
                    String correctAnswer = answerMatcher.group(1);
                    correctIndex = correctAnswer.charAt(0) - 'A';
                }
            }

            QuestionModel questionModel = new QuestionModel(question, imageUrl, options, correctIndex);
            questions.add(questionModel);
        }

        return questions;
    }

    private String extractValue(String content, String prefix, String suffix) {
        int startIndex = content.indexOf(prefix);
        if (startIndex != -1) {
            startIndex += prefix.length();
            int endIndex = content.indexOf(suffix, startIndex);
            if (endIndex != -1) {
                return content.substring(startIndex, endIndex).trim();
            }
        }
        return "";
    }

    private static class QuestionModel {
        private final String question;
        private final String imageUrl;
        private final List<String> options;
        private final int correctAnswerIndex;

        public QuestionModel(String question, String imageUrl, List<String> options, int correctAnswerIndex) {
            this.question = question;
            this.imageUrl = imageUrl;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
        }

        public String getQuestion() {
            return question;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public List<String> getOptions() {
            return options;
        }

        public int getCorrectAnswerIndex() {
            return correctAnswerIndex;
        }
    }
}