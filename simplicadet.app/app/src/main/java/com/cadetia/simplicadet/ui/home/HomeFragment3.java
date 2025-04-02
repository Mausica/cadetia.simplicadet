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

    private static final int PLATOON_COUNT = 6;
    private static final int SOLDIERS_PER_PLATOON = 27;
    private static final int TOTAL_SOLDIERS = PLATOON_COUNT * SOLDIERS_PER_PLATOON;
    private static final String STATE_IS_ROTATED = "isRotated";
    private static final String STATE_ORIGINAL_WIDTH = "originalWidth";
    private static final String STATE_ORIGINAL_HEIGHT = "originalHeight";

    private static final String[] SOLDIERS = {
            "Radulescu Marius", "Grama Bianca", "Popescu Ion", "Stanescu Maria",
            "Ionescu Andrei", "Constantinescu Elena", "Popa Florin", "Diaconescu Raluca",
            "Marinescu Cristian", "Negulescu Ana"
    };

    private static final int STATE_PRESENT = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_ABSENT = 2;

    private int originalWidth = -1;
    private int originalHeight = -1;

    private ZoomLayout zoomLayout;

    private boolean isRotated = false;
    private final int[] presentCount = new int[PLATOON_COUNT];
    private final int[] homeCount = new int[PLATOON_COUNT];
    private final int[] absentCount = new int[PLATOON_COUNT];

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
            originalWidth = savedInstanceState.getInt(STATE_ORIGINAL_WIDTH, -1);
            originalHeight = savedInstanceState.getInt(STATE_ORIGINAL_HEIGHT, -1);

            // If we were rotated, restore that state
            if (isRotated && zoomLayout != null) {
                zoomLayout.post(this::applyRotationState);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Reset to portrait when leaving the fragment
        if (isRotated && zoomLayout != null) {
            resetToPortrait();
        }
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        zoomLayout = view.findViewById(R.id.zoomLayout);
        zoomLayout.post(() -> {
            originalWidth = zoomLayout.getWidth();
            originalHeight = zoomLayout.getHeight();
        });

        // Main vertical container
        LinearLayout mainContainer = new LinearLayout(requireContext());
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        ViewGroup mindMapContainer = view.findViewById(R.id.mindMapContainer);
        mindMapContainer.addView(mainContainer);

        // COMPANY header (centered, 600px width)
        TextView companyHeader = createHeaderView("COMPANY", 600);
        mainContainer.addView(companyHeader);

        // COMPANY stats (centered below header)
        TextView companyStats = new TextView(requireContext());
        companyStats.setGravity(Gravity.CENTER);
        companyStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        companyStats.setTypeface(getResources().getFont(R.font.circular_bold));
        companyStats.setPadding(10, 10, 10, 20);
        updateCompanyStats(companyStats);

        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statsParams.gravity = Gravity.CENTER_HORIZONTAL;
        companyStats.setLayoutParams(statsParams);
        mainContainer.addView(companyStats);

        // Container for all platoons (now using LinearLayout)
        LinearLayout platoonsContainer = new LinearLayout(requireContext());
        platoonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams platoonsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        platoonsContainer.setLayoutParams(platoonsParams);
        mainContainer.addView(platoonsContainer);

        int soldierIndex = 0;

        for (int i = 0; i < PLATOON_COUNT; i++) {
            presentCount[i] = SOLDIERS_PER_PLATOON;
            homeCount[i] = 0;
            absentCount[i] = 0;

            LinearLayout platoonContainer = new LinearLayout(requireContext());
            platoonContainer.setOrientation(LinearLayout.VERTICAL);
            platoonContainer.setPadding(20, 20, 20, 20);

            // Create Platoon Header
            TextView platoonHeader = createHeaderView("PLATOON " + (i + 1), 330);
            platoonContainer.addView(platoonHeader);

            TextView platoonStats = new TextView(requireContext());
            updatePlatoonStats(i, platoonStats);
            platoonContainer.addView(platoonStats);

            GridLayout gridLayout = new GridLayout(requireContext());
            gridLayout.setColumnCount(3);
            gridLayout.setRowCount(9);

            for (int j = 0; j < SOLDIERS_PER_PLATOON; j++) {
                View soldierView = getLayoutInflater().inflate(R.layout.item_formation, gridLayout, false);
                soldierView.getLayoutParams().width = 100;
                soldierView.getLayoutParams().height = 100;

                TextView tvInitials = soldierView.findViewById(R.id.tv_soldier_initials);
                String fullName = SOLDIERS[soldierIndex % SOLDIERS.length];
                String[] nameParts = fullName.split(" ");
                String initials = nameParts[0].substring(0, 1) + nameParts[1].substring(0, 1);
                tvInitials.setText(initials);
                soldierIndex++;

                soldierView.setTag(R.id.soldier_state_key, STATE_PRESENT);
                soldierView.setTag(R.id.platoon_index_key, i);
                soldierView.setOnClickListener(v -> {
                    cycleState(v, platoonStats);
                    updateCompanyStats(companyStats);
                });
                gridLayout.addView(soldierView);
            }

            platoonContainer.addView(gridLayout);
            platoonsContainer.addView(platoonContainer);
        }
    }

    private TextView createHeaderView(String text, int width) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        header.setTypeface(getResources().getFont(R.font.circular_bold));
        header.setGravity(Gravity.CENTER);
        header.setPadding(10, 10, 10, 10);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));
        drawable.setColor(getThemeColor(R.attr.backgroundLight));
        header.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                width, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, 0, 0, 10);
        header.setLayoutParams(params);

        return header;
    }

    private void cycleState(View v, TextView platoonStats) {
        int platoonIndex = (int) v.getTag(R.id.platoon_index_key);
        int currentState = (int) v.getTag(R.id.soldier_state_key);
        int newState = (currentState + 1) % 3;
        v.setTag(R.id.soldier_state_key, newState);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));

        int backgroundColor;

        switch (newState) {
            case STATE_PRESENT:
                backgroundColor = getThemeColor(R.attr.backgroundLight);
                presentCount[platoonIndex]++;
                absentCount[platoonIndex]--;
                break;

            case STATE_HOME:
                backgroundColor = getThemeColor(R.attr.backgroundDark);
                presentCount[platoonIndex]--;
                homeCount[platoonIndex]++;
                break;

            case STATE_ABSENT:
                backgroundColor = Color.parseColor("#D9FA5A50");
                homeCount[platoonIndex]--;
                absentCount[platoonIndex]++;
                break;

            default:
                return;
        }

        drawable.setColor(backgroundColor);
        v.setBackground(drawable);
        updatePlatoonStats(platoonIndex, platoonStats);
    }

    private void updatePlatoonStats(int platoonIndex, TextView platoonStats) {
        platoonStats.setGravity(Gravity.CENTER);
        platoonStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        platoonStats.setTypeface(getResources().getFont(R.font.circular_bold));
        platoonStats.setPadding(10, 10, 10, 10);
        platoonStats.setText("P: " + presentCount[platoonIndex] + " H: " + homeCount[platoonIndex] + " A: " + absentCount[platoonIndex]);
    }

    private void updateCompanyStats(TextView companyStats) {
        int totalPresent = TOTAL_SOLDIERS; // Initialize with TOTAL_SOLDIERS
        int totalHome = 0;
        int totalAbsent = 0;
        for (int i = 0; i < PLATOON_COUNT; i++) {
            totalHome += homeCount[i];
            totalAbsent += absentCount[i];
        }
        // Subtract absent and home soldiers from total present
        totalPresent -= (totalHome + totalAbsent);
        companyStats.setText("P: " + totalPresent + " H: " + totalHome + " A: " + totalAbsent);
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
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