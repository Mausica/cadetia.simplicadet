package com.cadetia.simplicadet.entities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.cadetia.simplicadet.R;
import eightbitlab.com.blurview.BlurView;

public class InstitutionSelectionDialog {

    private static BlurView blurBackground;
    private static boolean isIndividualSelected = true;

    public interface InstitutionSelectionCallback {
        void onInstitutionSelected(String institution, String accessCode);
        void onCancel();
    }

    public static void show(Context context, InstitutionSelectionCallback callback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_institution_selection, null);
        Activity activity = (Activity) context;

        setupBlurBackground(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TransparentDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        animateBlur(true);

        dialog.setOnDismissListener(dialogInterface -> animateBlur(false));

        // Find views
        LinearLayout individualOption = dialogView.findViewById(R.id.individual_option);
        LinearLayout institutionOption = dialogView.findViewById(R.id.institution_option);
        View individualIndicator = dialogView.findViewById(R.id.individual_indicator);
        View institutionIndicator = dialogView.findViewById(R.id.institution_indicator);
        EditText editAccessCode = dialogView.findViewById(R.id.edit_access_code);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Set up click listeners for options
        individualOption.setOnClickListener(v -> {
            isIndividualSelected = true;
            updateSelectionIndicators(individualIndicator, institutionIndicator, editAccessCode);
        });

        institutionOption.setOnClickListener(v -> {
            isIndividualSelected = false;
            updateSelectionIndicators(individualIndicator, institutionIndicator, editAccessCode);
        });

        // Initialize selection state
        updateSelectionIndicators(individualIndicator, institutionIndicator, editAccessCode);

        btnConfirm.setOnClickListener(v -> {
            if (isIndividualSelected) {
                dialog.dismiss();
                if (callback != null) callback.onInstitutionSelected("INDIVIDUAL", "");
            } else {
                String accessCode = editAccessCode.getText().toString().trim();
                if (!accessCode.isEmpty()) {
                    dialog.dismiss();
                    if (callback != null) callback.onInstitutionSelected("INSTITUTION", accessCode);
                } else {
                    Toast.makeText(context, "Please enter your access code", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onCancel();
        });

        dialog.show();
    }

    private static void updateSelectionIndicators(View individualIndicator, View institutionIndicator, EditText editAccessCode) {
        if (isIndividualSelected) {
            individualIndicator.setBackgroundResource(R.drawable.circle_selected);
            institutionIndicator.setBackgroundResource(R.drawable.circle_unselected);
            editAccessCode.setVisibility(View.GONE);
        } else {
            individualIndicator.setBackgroundResource(R.drawable.circle_unselected);
            institutionIndicator.setBackgroundResource(R.drawable.circle_selected);
            editAccessCode.setVisibility(View.VISIBLE);
        }
    }

    private static void setupBlurBackground(Activity activity) {
        if (blurBackground != null) return;

        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();

        blurBackground = new BlurView(activity);
        blurBackground.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        blurBackground.setupWith(rootView)
                .setFrameClearDrawable(rootView.getBackground())
                .setBlurRadius(12f)
                .setBlurAutoUpdate(true);

        blurBackground.setOverlayColor(activity.getResources().getColor(R.color.focus));
        blurBackground.setAlpha(0f);

        rootView.addView(blurBackground);
    }

    private static void animateBlur(boolean show) {
        if (blurBackground == null) return;

        float targetAlpha = show ? 0.6f : 0f;
        long duration = 300;

        blurBackground.animate()
                .alpha(targetAlpha)
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (!show) {
                        ViewGroup parent = (ViewGroup) blurBackground.getParent();
                        if (parent != null) parent.removeView(blurBackground);
                        blurBackground = null;
                    }
                })
                .start();
    }
}