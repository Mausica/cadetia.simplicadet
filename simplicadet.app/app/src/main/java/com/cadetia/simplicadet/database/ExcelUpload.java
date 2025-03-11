package com.cadetia.simplicadet.database;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public class ExcelUpload {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void uploadQuestions(Context context, Uri fileUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Set<String>> categoryTestsMap = new HashMap<>();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                if (row.getCell(0) == null || row.getCell(1) == null) {
                    Log.e("ExcelUpload", "Rând ignorat din cauza valorilor lipsă");
                    continue;
                }

                String category = row.getCell(0).getStringCellValue().trim();
                String testTitle = row.getCell(1).getStringCellValue().trim();

                if (category.isEmpty() || testTitle.isEmpty()) {
                    Log.e("ExcelUpload", "Categorie sau titlu test lipsă, rând ignorat");
                    continue;
                }

                categoryTestsMap.putIfAbsent(category, new HashSet<>());
                categoryTestsMap.get(category).add(testTitle);

                if (row.getCell(4) == null) {
                    Log.e("ExcelUpload", "Întrebare ignorată din cauza valorilor lipsă");
                    continue;
                }

                String question = row.getCell(4).getStringCellValue().trim();

                // Verifică dacă întrebarea există deja în Firestore
                db.collection("QUIZES")
                        .document(category)
                        .collection(testTitle)
                        .whereEqualTo("question", question)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots.isEmpty()) {
                                // Dacă întrebarea nu există, o adăugăm
                                String imageUrl = row.getCell(5) != null ? row.getCell(5).getStringCellValue().trim() : "";
                                String option1 = row.getCell(6).getStringCellValue().trim();
                                String option2 = row.getCell(7).getStringCellValue().trim();
                                String option3 = row.getCell(8).getStringCellValue().trim();
                                String option4 = row.getCell(9).getStringCellValue().trim();
                                int correctAnswerIndex = (int) row.getCell(10).getNumericCellValue();
                                int points = (int) row.getCell(11).getNumericCellValue();

                                List<String> options = List.of(option1, option2, option3, option4);

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
                            } else {
                                Log.d("ExcelUpload", "Întrebare deja existentă: " + question);
                            }
                        })
                        .addOnFailureListener(e -> Log.e("ExcelUpload", "Eroare la verificarea existenței întrebării", e));
            }

            for (Map.Entry<String, Set<String>> entry : categoryTestsMap.entrySet()) {
                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("noTests", entry.getValue().size());
                categoryData.put("subcollections", new ArrayList<>(entry.getValue()));

                db.collection("QUIZES")
                        .document(entry.getKey())
                        .set(categoryData);
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
