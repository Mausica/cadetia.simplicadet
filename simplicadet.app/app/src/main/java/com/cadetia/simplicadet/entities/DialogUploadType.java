package com.cadetia.simplicadet.entities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.cadetia.simplicadet.R;
import eightbitlab.com.blurview.BlurView;

public class DialogUploadType {

    private static BlurView blurBackground;

    public interface UploadTypeCallback {
        void onQuizUpload();
        void onStudentUpload();
        void onCancel();
    }

    public static void show(Context context, UploadTypeCallback callback) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_upload_type, null);
        Activity activity = (Activity) context;

        setupBlurBackground(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TransparentDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        animateBlur(true);

        dialog.setOnDismissListener(dialogInterface -> animateBlur(false));

        Button quizUploadButton = dialogView.findViewById(R.id.btn_quiz_upload);
        Button studentUploadButton = dialogView.findViewById(R.id.btn_student_upload);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);

        quizUploadButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onQuizUpload();
        });

        studentUploadButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onStudentUpload();
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onCancel();
        });

        dialog.show();
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