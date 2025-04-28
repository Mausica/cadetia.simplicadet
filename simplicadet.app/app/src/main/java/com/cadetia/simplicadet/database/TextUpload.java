package com.cadetia.simplicadet.database;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

            // Extract quiz metadata
            String title = extractValue(questionsSection, "NAME: ", "\n");
            String category = extractValue(questionsSection, "CAT: ", "\n");
            String creator = extractValue(questionsSection, "CREATOR: ", "\n");
            String imageUrl = extractValue(questionsSection, "IMAGE: ", "\n");
            String difficulty = extractValue(questionsSection, "DIFFICULTY: ", "\n");
            String tagsStr = extractValue(questionsSection, "TAGS: ", "\n");
            List<String> tags = new ArrayList<>();
            if (!tagsStr.isEmpty()) {
                tags = Arrays.asList(tagsStr.split(",\\s*"));
            }

            // Parse questions
            List<QuestionModel> questions = parseQuestions(questionsSection, answersSection);

            // Create new quiz document
            createQuizDocument(title, category, creator, imageUrl, difficulty, tags, questions, context);

            reader.close();
            inputStream.close();

        } catch (Exception e) {
            Log.e(TAG, "Eroare la procesarea fișierului text", e);
            Toast.makeText(context, "Eroare la încărcare!", Toast.LENGTH_SHORT).show();
        }
    }

    private void createQuizDocument(String title, String category, String creator,
                                    String imageUrl, String difficulty, List<String> tags,
                                    List<QuestionModel> questions, Context context) {
        // Create quiz document
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("title", title);
        quizData.put("category", category);
        quizData.put("createdBy", creator);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            quizData.put("imageUrl", imageUrl);
        }
        quizData.put("difficulty", difficulty.isEmpty() ? "Medium" : difficulty);
        quizData.put("tags", tags);
        quizData.put("createdAt", new Timestamp(new Date()));

        db.collection("QUIZZES")
                .add(quizData)
                .addOnSuccessListener(documentReference -> {
                    String quizId = documentReference.getId();
                    Log.d(TAG, "Quiz creat cu ID: " + quizId);

                    // Upload questions as subcollection
                    uploadQuestions(quizId, questions, context);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Eroare la crearea quiz-ului", e);
                    Toast.makeText(context, "Eroare la crearea quiz-ului!", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadQuestions(String quizId, List<QuestionModel> questions, Context context) {
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicInteger totalToProcess = new AtomicInteger(questions.size());

        for (QuestionModel question : questions) {
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("question", question.getQuestion());
            questionData.put("options", question.getOptions());
            questionData.put("correctAnswerIndex", question.getCorrectAnswerIndex());
            questionData.put("points", question.getPoints());

            if (question.getImageUrl() != null && !question.getImageUrl().isEmpty()) {
                questionData.put("imageUrl", question.getImageUrl());
            }

            db.collection("QUIZZES")
                    .document(quizId)
                    .collection("QUESTIONS")
                    .add(questionData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Întrebare adăugată: " + documentReference.getId());
                        if (successCount.incrementAndGet() + failureCount.get() == totalToProcess.get()) {
                            showCompletionMessage(context, successCount.get(), failureCount.get());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Eroare la încărcarea întrebării", e);
                        if (successCount.get() + failureCount.incrementAndGet() == totalToProcess.get()) {
                            showCompletionMessage(context, successCount.get(), failureCount.get());
                        }
                    });
        }
    }

    private void showCompletionMessage(Context context, int successCount, int failureCount) {
        String message = "Încărcare finalizată! " + successCount + " întrebări adăugate, "
                + failureCount + " întrebări cu eroare.";
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private List<QuestionModel> parseQuestions(String questionsContent, String answersContent) {
        List<QuestionModel> questions = new ArrayList<>();

        // Updated pattern to better handle spacing between question components
        Pattern questionPattern = Pattern.compile("(\\d+)\\. (.*?)(?=\\d+\\. |ANSWERS|$)", Pattern.DOTALL);
        Matcher questionMatcher = questionPattern.matcher(questionsContent);

        while (questionMatcher.find()) {
            String questionText = questionMatcher.group(2).trim();

            // Extract the main question text (first line)
            String[] lines = questionText.split("\n", 2);
            String question = lines[0].trim();
            String remainingText = lines.length > 1 ? lines[1].trim() : "";

            // Extract image URL if exists
            String imageUrl = "";
            Pattern imagePattern = Pattern.compile("IMAGE:\\s*(.*?)\\s*$", Pattern.MULTILINE);
            Matcher imageMatcher = imagePattern.matcher(remainingText);
            if (imageMatcher.find()) {
                imageUrl = imageMatcher.group(1).trim();
                // Remove the IMAGE line from remaining text
                remainingText = remainingText.replaceAll("IMAGE:\\s*.*?$(?m)", "").trim();
            }

            // Extract points if exists
            int points = 1; // Default value
            Pattern pointsPattern = Pattern.compile("POINTS:\\s*(\\d+)\\s*$", Pattern.MULTILINE);
            Matcher pointsMatcher = pointsPattern.matcher(remainingText);
            if (pointsMatcher.find()) {
                try {
                    points = Integer.parseInt(pointsMatcher.group(1).trim());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid points format, using default");
                }
                // Remove the POINTS line from remaining text
                remainingText = remainingText.replaceAll("POINTS:\\s*\\d+\\s*$(?m)", "").trim();
            }

            // Extract options
            List<String> options = new ArrayList<>();
            Pattern optionPattern = Pattern.compile("([A-D])\\.\\s*(.*?)(?=\\s*[A-D]\\.|$)", Pattern.DOTALL);
            Matcher optionMatcher = optionPattern.matcher(remainingText);

            while (optionMatcher.find() && options.size() < 4) {
                options.add(optionMatcher.group(2).trim());
            }

            // Determine correct answer
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

            QuestionModel questionModel = new QuestionModel(question, imageUrl, options, correctIndex, points);
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
        private final int points;

        public QuestionModel(String question, String imageUrl, List<String> options, int correctAnswerIndex, int points) {
            this.question = question;
            this.imageUrl = imageUrl;
            this.options = options;
            this.correctAnswerIndex = correctAnswerIndex;
            this.points = points;
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

        public int getPoints() {
            return points;
        }
    }
}