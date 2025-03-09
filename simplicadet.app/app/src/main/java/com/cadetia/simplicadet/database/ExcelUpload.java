package com.cadetia.simplicadet.database;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelUpload {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void uploadQuestions(Context context, Uri fileUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Prima foaie

            String lastCategory = "";
            String lastTestTitle = "";
            String createdBy = "";
            String quizImageUrl = "";

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Sari peste antet

                String category = row.getCell(0).getStringCellValue();
                String testTitle = row.getCell(1).getStringCellValue();
                String newCreatedBy = row.getCell(2).getStringCellValue();
                String newQuizImageUrl = row.getCell(3).getStringCellValue();

                // Dacă este un test nou, adăugăm metadatele testului
                if (!category.equals(lastCategory) || !testTitle.equals(lastTestTitle)) {
                    lastCategory = category;
                    lastTestTitle = testTitle;
                    createdBy = newCreatedBy;
                    quizImageUrl = newQuizImageUrl;

                    Map<String, Object> quizInfo = new HashMap<>();
                    quizInfo.put("createdBy", createdBy);
                    quizInfo.put("imageUrl", quizImageUrl);

                    db.collection("QUIZES")
                            .document(category)
                            .collection(testTitle)
                            .document("Info")
                            .set(quizInfo);
                }

                String question = row.getCell(4).getStringCellValue();
                String imageUrl = row.getCell(5).getStringCellValue();
                String option1 = row.getCell(6).getStringCellValue();
                String option2 = row.getCell(7).getStringCellValue();
                String option3 = row.getCell(8).getStringCellValue();
                String option4 = row.getCell(9).getStringCellValue();
                int correctAnswerIndex = (int) row.getCell(10).getNumericCellValue();
                int points = (int) row.getCell(11).getNumericCellValue();

                List<String> options = Arrays.asList(option1, option2, option3, option4);

                Map<String, Object> questionData = new HashMap<>();
                questionData.put("question", question);
                questionData.put("imageUrl", imageUrl);
                questionData.put("options", options);
                questionData.put("correctAnswerIndex", correctAnswerIndex);
                questionData.put("points", points);

                db.collection("QUIZES")
                        .document(category)
                        .collection(testTitle)
                        .add(questionData)
                        .addOnSuccessListener(documentReference -> Log.d("ExcelUpload", "Întrebare adăugată: " + documentReference.getId()))
                        .addOnFailureListener(e -> Log.e("ExcelUpload", "Eroare la încărcare", e));
            }

            workbook.close();
            inputStream.close();
            Toast.makeText(context, "Încărcare finalizată!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ExcelUpload", "Eroare la citirea fișierului", e);
            Toast.makeText(context, "Eroare la încărcare!", Toast.LENGTH_SHORT).show();
        }
    }
}