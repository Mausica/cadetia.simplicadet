package com.cadetia.simplicadet.entities;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.cadetia.simplicadet.R;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class DialogConfirm {

    private static BlurView blurBackground;

    public static void show(Context context, String title, String message, Runnable onConfirm, Boolean cancel) {
        // Inflate the custom view
        View dialogView = LayoutInflater.from(context).inflate(R.layout.popup_confirm, null);
        Activity activity = (Activity) context;

        // Create and configure blur background
        setupBlurBackground(activity);

        // Build the dialog with your transparent style
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TransparentDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(cancel);

        // Animate blur in
        animateBlur(true);

        // Handle dismiss
        dialog.setOnDismissListener(dialogInterface -> animateBlur(false));

        // Find views and setup buttons (existing code)
        TextView titleText = dialogView.findViewById(R.id.titleText);
        TextView messageText = dialogView.findViewById(R.id.messageText);
        Button okButton = dialogView.findViewById(R.id.confirmButton);

        if (titleText != null) titleText.setText(title);
        if (messageText != null) messageText.setText(message);

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });

        dialog.show();
    }

    private static void setupBlurBackground(Activity activity) {
        if (blurBackground != null) return;

        ViewGroup rootView = activity.findViewById(android.R.id.content);
        View decorView = activity.getWindow().getDecorView();

        blurBackground = new BlurView(activity);
        blurBackground.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Configure blur
        blurBackground.setupWith(rootView)
                .setFrameClearDrawable(decorView.getBackground())
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