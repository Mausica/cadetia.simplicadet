package com.cadetia.simplicadet.entities;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.cadetia.simplicadet.R;

public class InstitutionSelectionDialog {

    public interface InstitutionSelectionCallback {
        void onInstitutionSelected(String institution, String accessCode);
        void onCancel();
    }

    public static void show(Context context, InstitutionSelectionCallback callback) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_institution_selection, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);

        RadioGroup radioGroup = view.findViewById(R.id.radio_group);
        RadioButton radioInstitution = view.findViewById(R.id.radio_institution);
        RadioButton radioIndividual = view.findViewById(R.id.radio_individual);
        EditText editAccessCode = view.findViewById(R.id.edit_access_code);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isInstitution = checkedId == R.id.radio_institution;
            editAccessCode.setVisibility(isInstitution ? View.VISIBLE : View.GONE);
        });

        btnConfirm.setOnClickListener(v -> {
            if (radioIndividual.isChecked()) {
                callback.onInstitutionSelected("INDIVIDUAL", "");
                dialog.dismiss();
            } else if (radioInstitution.isChecked()) {
                String accessCode = editAccessCode.getText().toString().trim();

                if (!accessCode.isEmpty()) {
                    callback.onInstitutionSelected("INSTITUTION", accessCode);
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Please enter your access code", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Please select an option", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            callback.onCancel();
            dialog.dismiss();
        });

        dialog.show();
    }
}