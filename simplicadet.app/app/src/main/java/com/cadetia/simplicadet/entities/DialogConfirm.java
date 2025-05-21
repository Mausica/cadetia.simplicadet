package com.cadetia.simplicadet.entities;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.cadetia.simplicadet.R;

public class DialogConfirm {

    public static void show(Context context, String title, String message, Runnable onConfirm, Boolean cancel) {
        // Inflate the custom view
        View dialogView = LayoutInflater.from(context).inflate(R.layout.popup_confirm, null);

        // Build the dialog with your transparent style
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TransparentDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(cancel);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Find views
        TextView titleText = dialogView.findViewById(R.id.titleText);
        TextView messageText = dialogView.findViewById(R.id.messageText);
        Button okButton = dialogView.findViewById(R.id.confirmButton);

        if (titleText != null) {
            titleText.setText(title);
        }

        if (messageText != null) {
            messageText.setText(message);
        }

        okButton.setOnClickListener((View v) -> {
            dialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });

        dialog.show();
    }
}
