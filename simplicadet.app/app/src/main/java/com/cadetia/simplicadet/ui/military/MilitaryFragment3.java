package com.cadetia.simplicadet.ui.military;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.entities.DialogStudentPreview;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.otaliastudios.zoom.ZoomLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MilitaryFragment3 extends Fragment {

    private static final String TAG = "MilitaryFragment3";
    private static final String STATE_IS_ROTATED = "isRotated";
    private static final String STATE_ORIGINAL_WIDTH = "originalWidth";
    private static final String STATE_ORIGINAL_HEIGHT = "originalHeight";
    private static final int STATE_PRESENT = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_ABSENT = 2;

    private int originalWidth = -1;
    private int originalHeight = -1;
    private ZoomLayout zoomLayout;
    private boolean isRotated = false;
    private FirebaseFirestore db;
    private String companyName = "Compania";
    private int companyNumber = 0;
    private int platoonCount = 0;
    private int[] ecCounts;
    private List<Student> allStudents = new ArrayList<>();
    private List<List<Student>> platoonStudents = new ArrayList<>();
    private int[] presentCount;
    private int[] homeCount;
    private int[] absentCount;
    private List<String> rankNames = new ArrayList<>();

    private static class Student {
        String name;
        int height;
        int platoon;
        String initials;
        String rank;
        String imageUrl;
        String documentId;

        Student(String name, int height, int platoon, String rank, String imageUrl, String documentId) {
            this.name = name;
            this.height = height;
            this.platoon = platoon;
            this.rank = rank != null ? rank : "Unknown";
            this.imageUrl = imageUrl;
            this.documentId = documentId;
            this.initials = generateInitials(name);
        }

        private String generateInitials(String fullName) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 2) {
                return parts[0].substring(0, 1).toUpperCase() + parts[1].substring(0, 1).toUpperCase();
            } else if (parts.length == 1 && parts[0].length() >= 2) {
                return parts[0].substring(0, 2).toUpperCase();
            } else {
                return parts[0].substring(0, 1).toUpperCase();
            }
        }
    }

    public MilitaryFragment3() {
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_military3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        zoomLayout = view.findViewById(R.id.zoomLayout);
        if (zoomLayout != null) {
            zoomLayout.post(() -> {
                originalWidth = zoomLayout.getWidth();
                originalHeight = zoomLayout.getHeight();
            });
        }
        loadRanks();
    }

    private void loadRanks() {
        db.collection("MILITARY").document("RO").collection("CNMTV").document("RANKS")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            rankNames.clear();
                            rankNames.add("Unknown");
                            for (int i = 1; i <= 8; i++) {
                                Object rankData = data.get(String.valueOf(i));
                                String rankName = "Unknown";

                                if (rankData instanceof List) {
                                    List<?> rankArray = (List<?>) rankData;
                                    if (rankArray.size() > 0 && rankArray.get(0) instanceof String) {
                                        rankName = (String) rankArray.get(0);
                                    }
                                }

                                rankNames.add(rankName);
                                Log.d(TAG, "Loaded rank " + i + ": " + rankName);
                            }
                        }
                    }
                    loadFirebaseData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load ranks", e);
                    rankNames.clear();
                    for (int i = 0; i <= 8; i++) rankNames.add("Unknown");
                    loadFirebaseData();
                });
    }

    private String getRankString(Object rankObj) {
        if (rankObj instanceof Long) {
            int rank = ((Long) rankObj).intValue();
            if (rankNames != null && rank >= 0 && rank < rankNames.size()) {
                String rankName = rankNames.get(rank);
                return (rankName != null && !rankName.isEmpty()) ? rankName : "Unknown";
            }
            Log.w(TAG, "Invalid rank index: " + rank + ", rankNames size: " + (rankNames != null ? rankNames.size() : "null"));
            return "Unknown";
        } else if (rankObj instanceof Integer) {
            int rank = ((Integer) rankObj).intValue();
            if (rankNames != null && rank >= 0 && rank < rankNames.size()) {
                String rankName = rankNames.get(rank);
                return (rankName != null && !rankName.isEmpty()) ? rankName : "Unknown";
            }
            Log.w(TAG, "Invalid rank index: " + rank + ", rankNames size: " + (rankNames != null ? rankNames.size() : "null"));
            return "Unknown";
        }
        return rankObj != null ? rankObj.toString() : "Unknown";
    }

    private void loadFirebaseData() {
        db.collection("MILITARY").document("RO").collection("CNMTV").document("STUDENTS").collection("2025").document("STATS")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            Object cpObj = data.get("CP");
                            if (cpObj instanceof Number) {
                                companyNumber = ((Number) cpObj).intValue();
                            } else {
                                companyNumber = 1;
                            }
                            Object ecObj = data.get("EC");
                            if (ecObj instanceof List) {
                                List<Object> ecList = (List<Object>) ecObj;
                                platoonCount = ecList.size();
                                ecCounts = new int[platoonCount];
                                for (int i = 0; i < platoonCount; i++) {
                                    Object count = ecList.get(i);
                                    if (count instanceof Number) {
                                        ecCounts[i] = ((Number) count).intValue();
                                    } else {
                                        ecCounts[i] = 0;
                                    }
                                }
                            } else if (ecObj instanceof Map) {
                                Map<String, Object> ecMap = (Map<String, Object>) ecObj;
                                platoonCount = ecMap.size();
                                ecCounts = new int[platoonCount];
                                for (int i = 0; i < platoonCount; i++) {
                                    Object count = ecMap.get(String.valueOf(i));
                                    if (count instanceof Number) {
                                        ecCounts[i] = ((Number) count).intValue();
                                    } else {
                                        ecCounts[i] = 0;
                                    }
                                }
                            } else {
                                createFallbackData();
                                return;
                            }
                            if (platoonCount <= 0 || ecCounts == null) {
                                createFallbackData();
                                return;
                            }
                            presentCount = new int[platoonCount];
                            homeCount = new int[platoonCount];
                            absentCount = new int[platoonCount];
                            platoonStudents.clear();
                            for (int i = 0; i < platoonCount; i++) {
                                platoonStudents.add(new ArrayList<>());
                            }
                            loadStudentData();
                        } else {
                            createFallbackData();
                        }
                    } else {
                        createFallbackData();
                    }
                })
                .addOnFailureListener(e -> createFallbackData());
    }

    private void createFallbackData() {
        companyNumber = 2;
        platoonCount = 3;
        ecCounts = new int[]{24, 25, 26};
        presentCount = new int[platoonCount];
        homeCount = new int[platoonCount];
        absentCount = new int[platoonCount];
        platoonStudents.clear();
        for (int i = 0; i < platoonCount; i++) {
            platoonStudents.add(new ArrayList<>());
            for (int j = 0; j < Math.min(ecCounts[i], 5); j++) {
                Student student = new Student("Student " + (j + 1) + " Platoon" + (i + 1), 175 + j, i, "Unknown", "url", "id");
                platoonStudents.get(i).add(student);
            }
        }
        buildUI();
    }

    private void loadStudentData() {
        db.collection("MILITARY").document("RO").collection("CNMTV").document("STUDENTS").collection("2025")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStudents.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (!"STATS".equals(doc.getId())) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                String name = (String) data.get("NAME");
                                Object heightObj = data.get("HEIGHT");
                                Object platoonObj = data.get("PLUTON");
                                Object rankObj = data.get("RANK");
                                String imageUrl = (String) data.get("IMAGE");
                                if (name != null && heightObj instanceof Number && platoonObj instanceof Number) {
                                    int height = ((Number) heightObj).intValue();
                                    int platoon = ((Number) platoonObj).intValue();
                                    String rank = getRankString(rankObj);
                                    Student student = new Student(name, height, platoon, rank, imageUrl, doc.getId());
                                    allStudents.add(student);
                                    if (platoon >= 1 && platoon <= platoonCount) {
                                        platoonStudents.get(platoon - 1).add(student);
                                    }
                                }
                            }
                        }
                    }
                    for (int i = 0; i < platoonStudents.size(); i++) {
                        List<Student> platoonList = platoonStudents.get(i);
                        Collections.sort(platoonList, new Comparator<Student>() {
                            @Override
                            public int compare(Student s1, Student s2) {
                                return Integer.compare(s2.height, s1.height);
                            }
                        });
                    }
                    buildUI();
                })
                .addOnFailureListener(e -> buildUI());
    }

    private void buildUI() {
        if (getView() == null) return;
        ViewGroup mindMapContainer = getView().findViewById(R.id.mindMapContainer);
        if (mindMapContainer == null) return;
        mindMapContainer.removeAllViews();
        LinearLayout mainContainer = new LinearLayout(requireContext());
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        mainContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mindMapContainer.addView(mainContainer);
        String companyTitle = companyName + " " + companyNumber;
        TextView companyHeader = createHeaderView(companyTitle, 800);
        mainContainer.addView(companyHeader);
        TextView companyStats = new TextView(requireContext());
        companyStats.setGravity(Gravity.CENTER);
        companyStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        companyStats.setTypeface(getResources().getFont(R.font.circular_bold));
        companyStats.setPadding(10, 10, 10, 20);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statsParams.gravity = Gravity.CENTER_HORIZONTAL;
        companyStats.setLayoutParams(statsParams);
        mainContainer.addView(companyStats);
        LinearLayout platoonsContainer = new LinearLayout(requireContext());
        platoonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        platoonsContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainContainer.addView(platoonsContainer);
        if (platoonCount <= 0 || ecCounts == null) {
            TextView noDataText = new TextView(requireContext());
            noDataText.setText("No data available");
            noDataText.setGravity(Gravity.CENTER);
            noDataText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            mainContainer.addView(noDataText);
            return;
        }
        for (int p = 0; p < platoonCount; p++) {
            buildPlatoon(p, platoonsContainer, companyStats);
        }
        updateCompanyStats(companyStats);
    }

    private void buildPlatoon(int platoonIndex, LinearLayout platoonsContainer, TextView companyStats) {
        int studentsInPlatoon = ecCounts[platoonIndex];
        String formationFormula = generateFormationFormula(studentsInPlatoon);
        presentCount[platoonIndex] = studentsInPlatoon;
        homeCount[platoonIndex] = 0;
        absentCount[platoonIndex] = 0;
        LinearLayout platoonContainer = new LinearLayout(requireContext());
        platoonContainer.setOrientation(LinearLayout.VERTICAL);
        platoonContainer.setPadding(20, 20, 20, 20);
        platoonContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int actualPlatoonNumber = platoonIndex + 1;
        if (!platoonStudents.get(platoonIndex).isEmpty()) {
            actualPlatoonNumber = platoonStudents.get(platoonIndex).get(0).platoon;
        }
        TextView platoonHeader = createHeaderView("Plutonul " + actualPlatoonNumber, 330);
        platoonContainer.addView(platoonHeader);
        TextView platoonStats = new TextView(requireContext());
        updatePlatoonStats(platoonIndex, platoonStats);
        platoonContainer.addView(platoonStats);
        String[] rows = formationFormula.split("\\n");
        final int numRows = rows.length;
        final int numCols = rows[0].length();
        GridLayout grid = new GridLayout(requireContext());
        grid.setRowCount(numRows);
        grid.setColumnCount(numCols);
        int cellSizePx = 100;
        int marginPx = 55;
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(numCols * cellSizePx + marginPx, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridLp.gravity = Gravity.CENTER_HORIZONTAL;
        grid.setLayoutParams(gridLp);
        List<Student> students = platoonStudents.get(platoonIndex);
        int studentIndex = 0;
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                char ch = rows[r].charAt(c);
                if (ch == '*') {
                    View sv = getLayoutInflater().inflate(R.layout.item_formation, grid, false);
                    sv.getLayoutParams().width = 100;
                    sv.getLayoutParams().height = 100;
                    TextView tv = sv.findViewById(R.id.tv_soldier_initials);
                    Student currentStudent = null;
                    if (studentIndex < students.size()) {
                        currentStudent = students.get(studentIndex);
                        tv.setText(currentStudent.initials);
                    } else {
                        tv.setText("");
                    }
                    sv.setTag(R.id.soldier_state_key, STATE_PRESENT);
                    sv.setTag(R.id.platoon_index_key, platoonIndex);
                    sv.setTag(R.id.student_data_key, currentStudent);
                    sv.setOnClickListener(v -> {
                        cycleState(v, platoonStats);
                        updateCompanyStats(companyStats);
                    });
                    final Student finalStudent = currentStudent;
                    sv.setOnLongClickListener(v -> {
                        if (finalStudent != null) {
                            showStudentPreview(finalStudent, platoonIndex);
                        }
                        return true;
                    });
                    grid.addView(sv);
                    studentIndex++;
                } else {
                    View spacer = new View(requireContext());
                    spacer.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
                    grid.addView(spacer);
                }
            }
        }
        platoonContainer.addView(grid);
        platoonsContainer.addView(platoonContainer);
    }

    private void showStudentPreview(Student student, int platoonIndex) {
        if (student == null) return;
        String rank = student.rank != null ? student.rank : "Unknown";
        String companyText = companyName + " " + companyNumber;
        String platoonText = "Plutonul " + student.platoon;
        DialogStudentPreview.show(requireActivity(), rank, student.name, companyText, platoonText, student.imageUrl);
    }

    private String generateFormationFormula(int studentCount) {
        if (studentCount <= 0) return "";
        int completeRows = studentCount / 3;
        int remainder = studentCount % 3;
        StringBuilder formula = new StringBuilder();
        if (remainder > 0) {
            for (int i = 0; i < completeRows; i++) {
                formula.append("***\n");
            }
            if (remainder == 1) {
                formula.append("-*-\n");
            } else if (remainder == 2) {
                formula.append("*-*\n");
            }
            formula.append("***");
        } else {
            for (int i = 0; i < completeRows; i++) {
                if (i == completeRows - 1) {
                    formula.append("***");
                } else {
                    formula.append("***\n");
                }
            }
        }
        return formula.toString();
    }

    private TextView createHeaderView(String text, int width) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        header.setTypeface(getResources().getFont(R.font.circular_bold));
        header.setGravity(Gravity.CENTER);
        header.setPadding(10, 10, 10, 10);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));
        bg.setColor(getThemeColor(R.attr.backgroundLight));
        header.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0, 0, 0, 10);
        header.setLayoutParams(lp);
        return header;
    }

    private void cycleState(View v, TextView platoonStats) {
        int pi = (int) v.getTag(R.id.platoon_index_key);
        int cs = (int) v.getTag(R.id.soldier_state_key);
        int ns = (cs + 1) % 3;
        v.setTag(R.id.soldier_state_key, ns);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));
        int color;
        switch (ns) {
            case STATE_PRESENT:
                color = getThemeColor(R.attr.backgroundLight);
                presentCount[pi]++;
                absentCount[pi]--;
                break;
            case STATE_HOME:
                color = getThemeColor(R.attr.backgroundDark);
                presentCount[pi]--;
                homeCount[pi]++;
                break;
            case STATE_ABSENT:
                color = Color.parseColor("#D9FA5A50");
                homeCount[pi]--;
                absentCount[pi]++;
                break;
            default:
                return;
        }
        d.setColor(color);
        v.setBackground(d);
        updatePlatoonStats(pi, platoonStats);
    }

    private void updatePlatoonStats(int i, TextView tv) {
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(getResources().getFont(R.font.circular_bold));
        tv.setPadding(10, 10, 10, 10);
        tv.setText("EP: " + presentCount[i] + " M: " + homeCount[i] + " EA: " + absentCount[i]);
    }

    private void updateCompanyStats(TextView tv) {
        int totalHome = 0, totalAbsent = 0, totalPresent = 0;
        for (int i = 0; i < platoonCount; i++) {
            totalHome += homeCount[i];
            totalAbsent += absentCount[i];
            totalPresent += presentCount[i];
        }
        tv.setText("EP: " + totalPresent + " M: " + totalHome + " EA: " + totalAbsent);
    }

    private int getThemeColor(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_ROTATED, isRotated);
        outState.putInt(STATE_ORIGINAL_WIDTH, originalWidth);
        outState.putInt(STATE_ORIGINAL_HEIGHT, originalHeight);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            isRotated = savedInstanceState.getBoolean(STATE_IS_ROTATED, false);
            originalWidth = savedInstanceState.getInt(STATE_ORIGINAL_WIDTH, -1);
            originalHeight = savedInstanceState.getInt(STATE_ORIGINAL_HEIGHT, -1);
            if (isRotated && zoomLayout != null) {
                zoomLayout.post(this::applyRotationState);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRotated && zoomLayout != null) {
            resetToPortrait();
        }
    }

    private void restoreOriginalDimensions() {
        if (zoomLayout != null && originalWidth > 0 && originalHeight > 0) {
            zoomLayout.post(() -> {
                isRotated = false;
                zoomLayout.setRotation(0);
                zoomLayout.setTranslationX(0);
                zoomLayout.setTranslationY(0);
                zoomLayout.zoomTo(1.0f, true);
                ViewGroup.LayoutParams params = zoomLayout.getLayoutParams();
                params.width = originalWidth;
                params.height = originalHeight;
                zoomLayout.setLayoutParams(params);
                zoomLayout.requestLayout();
            });
        }
    }

    private void applyRotationState() {
        if (isRotated) {
            rotateToLandscape();
        } else {
            resetToPortrait();
        }
    }

    private void rotateToLandscape() {
        if (zoomLayout == null) return;
        ViewGroup parent = (ViewGroup) zoomLayout.getParent();
        ViewGroup.LayoutParams params = zoomLayout.getLayoutParams();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        float currentRotation = zoomLayout.getRotation();
        float currentTranslationX = zoomLayout.getTranslationX();
        float currentTranslationY = zoomLayout.getTranslationY();
        float currentZoom = zoomLayout.getZoom();
        int currentWidth = zoomLayout.getWidth();
        int currentHeight = zoomLayout.getHeight();
        params.width = currentHeight;
        params.height = currentWidth;
        zoomLayout.setLayoutParams(params);
        zoomLayout.post(() -> {
            int newWidth = zoomLayout.getWidth();
            int newHeight = zoomLayout.getHeight();
            float pivotX = newWidth / 2f;
            float pivotY = newHeight / 2f;
            zoomLayout.setPivotX(pivotX);
            zoomLayout.setPivotY(pivotY);
            float targetTranslationX = (parentWidth - newWidth) / 2f;
            float targetTranslationY = (parentHeight - newHeight) / 2f - 100;
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(zoomLayout, "rotation", currentRotation, 90f);
            rotationAnim.setDuration(400);
            rotationAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            ObjectAnimator translationXAnim = ObjectAnimator.ofFloat(zoomLayout, "translationX", currentTranslationX, targetTranslationX);
            ObjectAnimator translationYAnim = ObjectAnimator.ofFloat(zoomLayout, "translationY", currentTranslationY, targetTranslationY);
            animatorSet.playTogether(rotationAnim, translationXAnim, translationYAnim);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    zoomLayout.zoomTo(currentZoom * 0.9f, true);
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    zoomLayout.zoomTo(0.9f, true);
                    zoomLayout.setRotation(90f);
                    zoomLayout.setTranslationX(targetTranslationX);
                    zoomLayout.setTranslationY(targetTranslationY);
                }
            });
            animatorSet.start();
        });
    }

    private void resetToPortrait() {
        if (zoomLayout == null || originalWidth <= 0 || originalHeight <= 0) return;
        isRotated = false;
        float currentRotation = zoomLayout.getRotation();
        float currentTranslationX = zoomLayout.getTranslationX();
        float currentTranslationY = zoomLayout.getTranslationY();
        float currentZoom = zoomLayout.getZoom();
        ViewGroup.LayoutParams params = zoomLayout.getLayoutParams();
        params.width = originalWidth;
        params.height = originalHeight;
        zoomLayout.setLayoutParams(params);
        zoomLayout.post(() -> {
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(zoomLayout, "rotation", currentRotation, 0f);
            rotationAnim.setDuration(400);
            rotationAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            ObjectAnimator translationXAnim = ObjectAnimator.ofFloat(zoomLayout, "translationX", currentTranslationX, 0f);
            ObjectAnimator translationYAnim = ObjectAnimator.ofFloat(zoomLayout, "translationY", currentTranslationY, 0f);
            animatorSet.playTogether(rotationAnim, translationXAnim, translationYAnim);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    zoomLayout.zoomTo(currentZoom * 0.9f, true);
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    zoomLayout.zoomTo(1.0f, true);
                    zoomLayout.setRotation(0f);
                    zoomLayout.setTranslationX(0f);
                    zoomLayout.setTranslationY(0f);
                }
            });
            animatorSet.start();
        });
    }

    public void rotateZoomLayout() {
        if (zoomLayout != null) {
            isRotated = !isRotated;
            applyRotationState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        restoreOriginalDimensions();
    }
}