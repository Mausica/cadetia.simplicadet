package com.cadetia.simplicadet.entities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.R;

import eightbitlab.com.blurview.BlurView;

public class DialogStudentPreview {

    private static BlurView blurBackground;

    public static void show(Context context, String rank, String name, String company, String platoon, String imageUrl) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.popup_student_preview, null);
        Activity activity = (Activity) context;

        setupBlurBackground(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TransparentDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        animateBlur(true);

        dialog.setOnDismissListener(dialogInterface -> animateBlur(false));
        ImageView studentImage = dialogView.findViewById(R.id.studentImage);
        TextView rankText = dialogView.findViewById(R.id.rankText);
        TextView nameText = dialogView.findViewById(R.id.nameText);
        TextView companyPlatoonText = dialogView.findViewById(R.id.companyPlatoonText);
        Button okButton = dialogView.findViewById(R.id.okButton);

        if (rankText != null) rankText.setText(rank);
        if (nameText != null) nameText.setText(name);
        if (companyPlatoonText != null) companyPlatoonText.setText(company + " • " + platoon);

        if (studentImage != null && imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(studentImage);
        }

        if (okButton != null) {
            okButton.setOnClickListener(v -> dialog.dismiss());
        }

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