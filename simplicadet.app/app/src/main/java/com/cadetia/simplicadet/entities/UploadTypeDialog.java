package com.cadetia.simplicadet.entities;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import com.cadetia.simplicadet.R;

public class UploadTypeDialog {

    public interface UploadTypeCallback {
        void onQuizUpload();
        void onStudentUpload();
        void onCancel();
    }

    public static void show(Context context, UploadTypeCallback callback) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_upload_type, null);
        dialog.setContentView(view);

        Button btnQuizUpload = view.findViewById(R.id.btn_quiz_upload);
        Button btnStudentUpload = view.findViewById(R.id.btn_student_upload);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnQuizUpload.setOnClickListener(v -> {
            callback.onQuizUpload();
            dialog.dismiss();
        });

        btnStudentUpload.setOnClickListener(v -> {
            callback.onStudentUpload();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            callback.onCancel();
            dialog.dismiss();
        });

        dialog.show();
    }
}