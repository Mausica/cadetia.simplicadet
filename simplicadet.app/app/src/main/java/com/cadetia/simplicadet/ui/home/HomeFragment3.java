package com.cadetia.simplicadet.ui.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import com.otaliastudios.zoom.ZoomLayout;

public class HomeFragment3 extends Fragment {

    // now configurable:
    private String formationFormula = "*****\n";

    private static final String STATE_IS_ROTATED       = "isRotated";
    private static final String STATE_ORIGINAL_WIDTH   = "originalWidth";
    private static final String STATE_ORIGINAL_HEIGHT  = "originalHeight";

    private static final String[] SOLDIERS = {
            "Radulescu Marius", "Stanescu Maria",
            "Ionescu Andrei", "Constantinescu Elena", "Popa Florin"
    };

    //private int platoonCount = 6;
    private String[] platoonLabels = {
            "Mic-dejun",
            "Prânz",
            "Cină"
    };

    private int platoonCount = platoonLabels.length;

    private static final int STATE_PRESENT = 0;
    private static final int STATE_HOME    = 1;
    private static final int STATE_ABSENT  = 2;

    private int originalWidth  = -1;
    private int originalHeight = -1;

    private ZoomLayout zoomLayout;
    private boolean isRotated = false;

    // we'll size these once we know platoonCount
    private int[] presentCount;
    private int[] homeCount;
    private int[] absentCount;

    public HomeFragment3() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home3, container, false);
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
            originalWidth  = savedInstanceState.getInt(STATE_ORIGINAL_WIDTH, -1);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // capture original dimensions for rotation reset
        zoomLayout = view.findViewById(R.id.zoomLayout);
        zoomLayout.post(() -> {
            originalWidth  = zoomLayout.getWidth();
            originalHeight = zoomLayout.getHeight();
        });

        // parse formula into rows & cols
        String[] rows = formationFormula.split("\\n");
        final int numRows = rows.length;
        final int numCols = rows[0].length();
        final int soldiersPerPlatoon = countChar(formationFormula, '*');

        // init stats arrays
        presentCount = new int[platoonCount];
        homeCount    = new int[platoonCount];
        absentCount  = new int[platoonCount];

        // build UI
        LinearLayout mainContainer = new LinearLayout(requireContext());
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        mainContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewGroup mindMapContainer = view.findViewById(R.id.mindMapContainer);
        mindMapContainer.addView(mainContainer);

        // COMPANY header
        TextView companyHeader = createHeaderView("M4E  DOLJ  2025", 800);
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
        updateCompanyStats(companyStats, soldiersPerPlatoon);
        mainContainer.addView(companyStats);

        // container for platoons
        LinearLayout platoonsContainer = new LinearLayout(requireContext());
        platoonsContainer.setOrientation(LinearLayout.VERTICAL); // change orientation
        platoonsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mainContainer.addView(platoonsContainer);

        int soldierIndex = 0;
        // for each platoon
        for (int p = 0; p < platoonCount; p++) {
            presentCount[p] = soldiersPerPlatoon;
            homeCount[p]    = 0;
            absentCount[p]  = 0;

            LinearLayout platoonContainer = new LinearLayout(requireContext());
            platoonContainer.setOrientation(LinearLayout.VERTICAL);
            platoonContainer.setPadding(20,20,20,20);
            platoonContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            // platoon header & stats
            //TextView platoonHeader = createHeaderView("PLATOON " + (p + 1), 330);
            //platoonContainer.addView(platoonHeader);

            String headerText;
            if (p < platoonLabels.length) {
                headerText = platoonLabels[p];
            } else {
                headerText = "Pluton " + (p+1);
            }
            TextView platoonHeader = createHeaderView(headerText, 330);
            platoonContainer.addView(platoonHeader);

            TextView platoonStats = new TextView(requireContext());
            updatePlatoonStats(p, platoonStats);
            platoonContainer.addView(platoonStats);

            // dynamic grid
            GridLayout grid = new GridLayout(requireContext());
            grid.setRowCount(numRows);
            grid.setColumnCount(numCols);

            int cellSizePx = 100;
            LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                    numCols * cellSizePx,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            gridLp.gravity = Gravity.CENTER_HORIZONTAL;
            grid.setLayoutParams(gridLp);

            int marginPx = 55;
            gridLp.width = numCols * cellSizePx + marginPx;
            grid.setLayoutParams(gridLp);


            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    char ch = rows[r].charAt(c);
                    if (ch == '*') {
                        // inflate soldier
                        View sv = getLayoutInflater().inflate(R.layout.item_formation, grid, false);
                        sv.getLayoutParams().width  = 100;
                        sv.getLayoutParams().height = 100;

                        TextView tv = sv.findViewById(R.id.tv_soldier_initials);
                        String fullName = SOLDIERS[soldierIndex++ % SOLDIERS.length];
                        tv.setText(fullName.split(" ")[0].substring(0,1)
                                + fullName.split(" ")[1].substring(0,1));

                        sv.setTag(R.id.soldier_state_key, STATE_PRESENT);
                        sv.setTag(R.id.platoon_index_key, p);
                        sv.setOnClickListener(v -> {
                            cycleState(v, platoonStats);
                            updateCompanyStats(companyStats, soldiersPerPlatoon);
                        });

                        grid.addView(sv);

                    } else {
                        // empty spacer for alignment
                        View spacer = new View(requireContext());
                        spacer.setLayoutParams(new ViewGroup.LayoutParams(100,100));
                        grid.addView(spacer);
                    }
                }
            }

            platoonContainer.addView(grid);
            platoonsContainer.addView(platoonContainer);
        }
    }

    private int countChar(String s, char find) {
        int cnt = 0;
        for (char c : s.toCharArray()) if (c == find) cnt++;
        return cnt;
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
                TypedValue.COMPLEX_UNIT_DIP,8,requireContext().getResources().getDisplayMetrics()));
        bg.setColor(getThemeColor(R.attr.backgroundLight));
        header.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0,0,0,10);
        header.setLayoutParams(lp);
        return header;
    }

    private void cycleState(View v, TextView platoonStats) {
        int pi = (int)v.getTag(R.id.platoon_index_key);
        int cs = (int)v.getTag(R.id.soldier_state_key);
        int ns = (cs + 1) % 3;
        v.setTag(R.id.soldier_state_key, ns);

        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,8,requireContext().getResources().getDisplayMetrics()));

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
            default: return;
        }
        d.setColor(color);
        v.setBackground(d);
        updatePlatoonStats(pi, platoonStats);
    }

    private void updatePlatoonStats(int i, TextView tv) {
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
        tv.setTypeface(getResources().getFont(R.font.circular_bold));
        tv.setPadding(10,10,10,10);
        tv.setText("P: " + presentCount[i]
                + " I: " + homeCount[i]
                + " A: " + absentCount[i]);
    }

    private void updateCompanyStats(TextView tv, int soldiersPerPlatoon) {
        int totalHome = 0, totalAbsent = 0;
        for (int i = 0; i < platoonCount; i++) {
            totalHome   += homeCount[i];
            totalAbsent += absentCount[i];
        }
        int totalPresent = platoonCount * soldiersPerPlatoon - (totalHome + totalAbsent);
        //tv.setText("P: " + totalPresent
        //        + " H: " + totalHome
        //        + " A: " + totalAbsent);
        tv.setText("JOI, 8 MAI 2025");
    }

    private int getThemeColor(int attr) {
        TypedValue tv = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }


    private void restoreOriginalDimensions() {
        if (zoomLayout != null && originalWidth > 0 && originalHeight > 0) {
            zoomLayout.post(() -> {
                // Reset rotation state
                isRotated = false;

                // Reset transformations
                zoomLayout.setRotation(0);
                zoomLayout.setTranslationX(0);
                zoomLayout.setTranslationY(0);
                zoomLayout.zoomTo(1.0f, true);

                // Restore original dimensions
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

        // Store current values for animation
        float currentRotation = zoomLayout.getRotation();
        float currentTranslationX = zoomLayout.getTranslationX();
        float currentTranslationY = zoomLayout.getTranslationY();
        float currentZoom = zoomLayout.getZoom();

        int currentWidth = zoomLayout.getWidth();
        int currentHeight = zoomLayout.getHeight();

        // Swap width and height for landscape
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

            // Calculate target translations
            float targetTranslationX = (parentWidth - newWidth) / 2f;
            float targetTranslationY = (parentHeight - newHeight) / 2f - 100;

            // Create animation set
            AnimatorSet animatorSet = new AnimatorSet();

            // Rotation animation
            ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(zoomLayout, "rotation", currentRotation, 90f);
            rotationAnim.setDuration(400);
            rotationAnim.setInterpolator(new AccelerateDecelerateInterpolator());

            // Translation animations
            ObjectAnimator translationXAnim = ObjectAnimator.ofFloat(zoomLayout, "translationX",
                    currentTranslationX, targetTranslationX);
            ObjectAnimator translationYAnim = ObjectAnimator.ofFloat(zoomLayout, "translationY",
                    currentTranslationY, targetTranslationY);

            // Play all animations together
            animatorSet.playTogether(rotationAnim, translationXAnim, translationYAnim);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Zoom out slightly at start
                    zoomLayout.zoomTo(currentZoom * 0.9f, true);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Final zoom with bounce effect
                    zoomLayout.zoomTo(0.9f, true);

                    // Ensure final state is correct
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

        // Store current values for animation
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

            // Rotation animation
            ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(zoomLayout, "rotation", currentRotation, 0f);
            rotationAnim.setDuration(400);
            rotationAnim.setInterpolator(new AccelerateDecelerateInterpolator());

            // Translation animations
            ObjectAnimator translationXAnim = ObjectAnimator.ofFloat(zoomLayout, "translationX", currentTranslationX, 0f);
            ObjectAnimator translationYAnim = ObjectAnimator.ofFloat(zoomLayout, "translationY", currentTranslationY, 0f);

            // Play all animations together
            animatorSet.playTogether(rotationAnim, translationXAnim, translationYAnim);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    // Zoom out slightly at start
                    zoomLayout.zoomTo(currentZoom * 0.9f, true);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Final zoom with bounce effect
                    zoomLayout.zoomTo(1.0f, true);

                    // Ensure final state is correct
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