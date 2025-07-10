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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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

    // Firebase data
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
    
    private static class Student {
        String name;
        int height;
        int platoon;
        String initials;

        Student(String name, int height, int platoon) {
            this.name = name;
            this.height = height;
            this.platoon = platoon;
            this.initials = generateInitials(name);
        }

        private String generateInitials(String fullName) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 2) {
                return parts[0].substring(0, 1).toUpperCase() +
                        parts[1].substring(0, 1).toUpperCase();
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

        // Initialize zoom layout
        zoomLayout = view.findViewById(R.id.zoomLayout);
        if (zoomLayout != null) {
            zoomLayout.post(() -> {
                originalWidth = zoomLayout.getWidth();
                originalHeight = zoomLayout.getHeight();
            });
        } else {
            Log.e(TAG, "zoomLayout is null");
        }

        // Load Firebase data
        loadFirebaseData();
    }

    private void loadFirebaseData() {
        Log.d(TAG, "Starting Firebase data load...");

        // First, get the STATS document to get CP and EC values
        db.collection("MILITARY")
                .document("RO")
                .collection("CNMTV")
                .document("STUDENTS")
                .collection("2025")
                .document("STATS")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "STATS document loaded successfully");

                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        Log.d(TAG, "STATS data: " + data);

                        if (data != null) {
                            // Get CP (company number)
                            Object cpObj = data.get("CP");
                            if (cpObj instanceof Number) {
                                companyNumber = ((Number) cpObj).intValue();
                                Log.d(TAG, "Company number: " + companyNumber);
                            } else {
                                Log.w(TAG, "CP is not a number: " + cpObj);
                                companyNumber = 1; // Default fallback
                            }

                            // Get EC array (students per platoon)
                            Object ecObj = data.get("EC");
                            Log.d(TAG, "EC object: " + ecObj + " (type: " + (ecObj != null ? ecObj.getClass().getSimpleName() : "null") + ")");

                            if (ecObj instanceof List) {
                                // EC is an array/list
                                List<Object> ecList = (List<Object>) ecObj;
                                platoonCount = ecList.size();
                                ecCounts = new int[platoonCount];

                                Log.d(TAG, "EC is a List with " + platoonCount + " platoons");

                                for (int i = 0; i < platoonCount; i++) {
                                    Object count = ecList.get(i);
                                    if (count instanceof Number) {
                                        ecCounts[i] = ((Number) count).intValue();
                                        Log.d(TAG, "EC[" + i + "] = " + ecCounts[i]);
                                    } else {
                                        Log.w(TAG, "EC[" + i + "] is not a number: " + count);
                                        ecCounts[i] = 0; // Default fallback
                                    }
                                }
                            } else if (ecObj instanceof Map) {
                                // EC is a map (your original code)
                                Map<String, Object> ecMap = (Map<String, Object>) ecObj;
                                platoonCount = ecMap.size();
                                ecCounts = new int[platoonCount];

                                Log.d(TAG, "EC is a Map with " + platoonCount + " platoons");

                                for (int i = 0; i < platoonCount; i++) {
                                    Object count = ecMap.get(String.valueOf(i));
                                    if (count instanceof Number) {
                                        ecCounts[i] = ((Number) count).intValue();
                                        Log.d(TAG, "EC[" + i + "] = " + ecCounts[i]);
                                    } else {
                                        Log.w(TAG, "EC[" + i + "] is not a number: " + count);
                                        ecCounts[i] = 0; // Default fallback
                                    }
                                }
                            } else {
                                Log.e(TAG, "EC is neither a List nor a Map: " + ecObj);
                                // Fallback: create a simple setup with dummy data
                                createFallbackData();
                                return;
                            }

                            // Validate that we have valid data
                            if (platoonCount <= 0 || ecCounts == null) {
                                Log.e(TAG, "Invalid platoon data after parsing");
                                createFallbackData();
                                return;
                            }

                            // Initialize stats arrays
                            presentCount = new int[platoonCount];
                            homeCount = new int[platoonCount];
                            absentCount = new int[platoonCount];

                            // Initialize platoon students lists
                            platoonStudents.clear();
                            for (int i = 0; i < platoonCount; i++) {
                                platoonStudents.add(new ArrayList<>());
                            }

                            // Now load student data
                            loadStudentData();
                        } else {
                            Log.e(TAG, "STATS document data is null");
                            createFallbackData();
                        }
                    } else {
                        Log.e(TAG, "STATS document does not exist");
                        createFallbackData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading STATS", e);
                    createFallbackData();
                });
    }

    private void createFallbackData() {
        Log.d(TAG, "Creating fallback data...");

        companyNumber = 2;
        platoonCount = 3;
        ecCounts = new int[]{24, 25, 26};

        presentCount = new int[platoonCount];
        homeCount = new int[platoonCount];
        absentCount = new int[platoonCount];

        platoonStudents.clear();
        for (int i = 0; i < platoonCount; i++) {
            platoonStudents.add(new ArrayList<>());

            // Add some dummy students for testing
            for (int j = 0; j < Math.min(ecCounts[i], 5); j++) {
                Student student = new Student("Student " + (j + 1) + " Platoon" + (i + 1), 175 + j, i);
                platoonStudents.get(i).add(student);
            }
        }

        Log.d(TAG, "Fallback data created, building UI...");
        buildUI();
    }

    private void loadStudentData() {
        Log.d(TAG, "Loading student data...");

        db.collection("MILITARY")
                .document("RO")
                .collection("CNMTV")
                .document("STUDENTS")
                .collection("2025")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Student query successful, found " + queryDocumentSnapshots.size() + " documents");

                    allStudents.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Log.d(TAG, "Processing document: " + doc.getId());

                        if (!"STATS".equals(doc.getId())) {
                            Map<String, Object> data = doc.getData();
                            Log.d(TAG, "Document data: " + data);

                            if (data != null) {
                                String name = (String) data.get("NAME");
                                Object heightObj = data.get("HEIGHT");
                                Object platoonObj = data.get("PLUTON");

                                Log.d(TAG, "Name: " + name + ", Height: " + heightObj + ", Platoon: " + platoonObj);

                                if (name != null && heightObj instanceof Number && platoonObj instanceof Number) {
                                    int height = ((Number) heightObj).intValue();
                                    int platoon = ((Number) platoonObj).intValue();

                                    Student student = new Student(name, height, platoon);
                                    allStudents.add(student);

                                    Log.d(TAG, "Created student: " + student.name + " (initials: " + student.initials + ", platoon: " + student.platoon + ")");

                                    // Add to appropriate platoon list (adjust for 0-based indexing)
                                    if (platoon >= 1 && platoon <= platoonCount) {
                                        platoonStudents.get(platoon - 1).add(student);
                                        Log.d(TAG, "Added student to platoon " + (platoon - 1));
                                    } else {
                                        Log.w(TAG, "Student platoon " + platoon + " is out of range (1-" + platoonCount + ")");
                                    }
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Total students loaded: " + allStudents.size());

                    // Sort students by height (tallest first) within each platoon
                    for (int i = 0; i < platoonStudents.size(); i++) {
                        List<Student> platoonList = platoonStudents.get(i);
                        Log.d(TAG, "Platoon " + i + " has " + platoonList.size() + " students");

                        Collections.sort(platoonList, new Comparator<Student>() {
                            @Override
                            public int compare(Student s1, Student s2) {
                                return Integer.compare(s2.height, s1.height); // Descending order
                            }
                        });
                    }

                    // Build the UI
                    Log.d(TAG, "Building UI...");
                    buildUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading students", e);
                    // Build UI with empty data
                    buildUI();
                });
    }

    private void buildUI() {
        Log.d(TAG, "Building UI...");

        if (getView() == null) {
            Log.e(TAG, "getView() is null, cannot build UI");
            return;
        }

        ViewGroup mindMapContainer = getView().findViewById(R.id.mindMapContainer);
        if (mindMapContainer == null) {
            Log.e(TAG, "mindMapContainer is null");
            return;
        }

        Log.d(TAG, "Clearing existing views...");
        mindMapContainer.removeAllViews();

        LinearLayout mainContainer = new LinearLayout(requireContext());
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        mainContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        mindMapContainer.addView(mainContainer);
        Log.d(TAG, "Main container added");

        // COMPANY header - Fixed to use CP value
        String companyTitle = companyName + " " + companyNumber;
        Log.d(TAG, "Creating company header: " + companyTitle);
        TextView companyHeader = createHeaderView(companyTitle, 800);
        mainContainer.addView(companyHeader);

        // COMPANY stats
        TextView companyStats = new TextView(requireContext());
        companyStats.setGravity(Gravity.CENTER);
        companyStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        companyStats.setTypeface(getResources().getFont(R.font.circular_bold));
        companyStats.setPadding(10, 10, 10, 20);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statsParams.gravity = Gravity.CENTER_HORIZONTAL;
        companyStats.setLayoutParams(statsParams);
        mainContainer.addView(companyStats);
        Log.d(TAG, "Company stats added");

        // Container for platoons
        LinearLayout platoonsContainer = new LinearLayout(requireContext());
        platoonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        platoonsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mainContainer.addView(platoonsContainer);
        Log.d(TAG, "Platoons container added");

        // Check if we have data to build platoons
        if (platoonCount <= 0 || ecCounts == null) {
            Log.e(TAG, "No platoon data available - platoonCount: " + platoonCount + ", ecCounts: " +
                    (ecCounts != null ? java.util.Arrays.toString(ecCounts) : "null"));

            // Add a simple text view indicating no data
            TextView noDataText = new TextView(requireContext());
            noDataText.setText("No data available");
            noDataText.setGravity(Gravity.CENTER);
            noDataText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            mainContainer.addView(noDataText);
            return;
        }

        // Build each platoon
        Log.d(TAG, "Building " + platoonCount + " platoons...");
        for (int p = 0; p < platoonCount; p++) {
            Log.d(TAG, "Building platoon " + p + " with " + ecCounts[p] + " students");
            buildPlatoon(p, platoonsContainer, companyStats);
        }

        // Update company stats
        updateCompanyStats(companyStats);
        Log.d(TAG, "UI build completed");
    }

    private void buildPlatoon(int platoonIndex, LinearLayout platoonsContainer, TextView companyStats) {
        int studentsInPlatoon = ecCounts[platoonIndex];
        String formationFormula = generateFormationFormula(studentsInPlatoon);

        // Initialize stats for this platoon
        presentCount[platoonIndex] = studentsInPlatoon;
        homeCount[platoonIndex] = 0;
        absentCount[platoonIndex] = 0;

        LinearLayout platoonContainer = new LinearLayout(requireContext());
        platoonContainer.setOrientation(LinearLayout.VERTICAL);
        platoonContainer.setPadding(20, 20, 20, 20);
        platoonContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Find the actual platoon number from students data
        int actualPlatoonNumber = platoonIndex + 1; // Default fallback
        if (!platoonStudents.get(platoonIndex).isEmpty()) {
            actualPlatoonNumber = platoonStudents.get(platoonIndex).get(0).platoon;
        }

        // Platoon header
        TextView platoonHeader = createHeaderView("Plutonul " + actualPlatoonNumber, 330);
        platoonContainer.addView(platoonHeader);

        // Platoon stats
        TextView platoonStats = new TextView(requireContext());
        updatePlatoonStats(platoonIndex, platoonStats);
        platoonContainer.addView(platoonStats);

        // Parse formation formula
        String[] rows = formationFormula.split("\\n");
        final int numRows = rows.length;
        final int numCols = rows[0].length();

        // Dynamic grid
        GridLayout grid = new GridLayout(requireContext());
        grid.setRowCount(numRows);
        grid.setColumnCount(numCols);

        int cellSizePx = 100;
        int marginPx = 55;
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                numCols * cellSizePx + marginPx,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        gridLp.gravity = Gravity.CENTER_HORIZONTAL;
        grid.setLayoutParams(gridLp);

        // Get students for this platoon
        List<Student> students = platoonStudents.get(platoonIndex);
        int studentIndex = 0;

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                char ch = rows[r].charAt(c);
                if (ch == '*') {
                    // Create soldier view
                    View sv = getLayoutInflater().inflate(R.layout.item_formation, grid, false);
                    sv.getLayoutParams().width = 100;
                    sv.getLayoutParams().height = 100;

                    TextView tv = sv.findViewById(R.id.tv_soldier_initials);

                    // Set initials from actual student data or empty if no student
                    if (studentIndex < students.size()) {
                        Student student = students.get(studentIndex);
                        tv.setText(student.initials);
                    } else {
                        tv.setText(""); // Empty space for missing students
                    }

                    sv.setTag(R.id.soldier_state_key, STATE_PRESENT);
                    sv.setTag(R.id.platoon_index_key, platoonIndex);
                    sv.setOnClickListener(v -> {
                        cycleState(v, platoonStats);
                        updateCompanyStats(companyStats);
                    });

                    grid.addView(sv);
                    studentIndex++;
                } else {
                    // Empty spacer for alignment
                    View spacer = new View(requireContext());
                    spacer.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
                    grid.addView(spacer);
                }
            }
        }

        platoonContainer.addView(grid);
        platoonsContainer.addView(platoonContainer);
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
        bg.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));
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
        d.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));

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
        tv.setText("EP: " + presentCount[i]
                + " M: " + homeCount[i]
                + " EA: " + absentCount[i]);
    }

    private void updateCompanyStats(TextView tv) {
        int totalHome = 0, totalAbsent = 0, totalPresent = 0;
        for (int i = 0; i < platoonCount; i++) {
            totalHome += homeCount[i];
            totalAbsent += absentCount[i];
            totalPresent += presentCount[i];
        }
        tv.setText("EP: " + totalPresent
                + " M: " + totalHome
                + " EA: " + totalAbsent);
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

            ObjectAnimator translationXAnim = ObjectAnimator.ofFloat(zoomLayout, "translationX",
                    currentTranslationX, targetTranslationX);
            ObjectAnimator translationYAnim = ObjectAnimator.ofFloat(zoomLayout, "translationY",
                    currentTranslationY, targetTranslationY);

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