package com.learn.simplify.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.learn.simplify.R;
import com.learn.simplify.kotlin.AIModel;
import com.learn.simplify.ui.home.HomeFragment3;

public class CreateQuestionBottom extends BottomSheetDialogFragment {

    private EditText addQuestionPrompt;
    private Button addQuestion;
    private ImageView closeQuestion;
    private AIModel aiModel;
    private String sender = "ME"; // Default sender

    public interface OnQuestionAskedListener {
        void onQuestionAsked(String sender, String prompt, String response);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_create_question, container, false);

        addQuestionPrompt = contentView.findViewById(R.id.promptText);
        addQuestion = contentView.findViewById(R.id.questionAsk);
        closeQuestion = contentView.findViewById(R.id.questionClose);

        closeQuestion.setOnClickListener(v -> dismiss());

        addQuestion.setOnClickListener(view -> {
            if (validateFields()) {
                String prompt = addQuestionPrompt.getText().toString();
                askQuestion(prompt); // Ask the question to get the response
                dismiss();
            }
        });

        return contentView;
    }

    private boolean validateFields() {
        if (addQuestionPrompt.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please enter a valid prompt", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    public static CreateQuestionBottom newInstance() {
        return new CreateQuestionBottom();
    }

    private void askQuestion(String prompt) {
        // Show your prompt
        addMessageToChat(sender, prompt);

        // Setup AI model
        aiModel = new ViewModelProvider((FragmentActivity) requireActivity()).get(AIModel.class);

        aiModel.getResponseLiveData().observe(requireActivity(), response -> {
            HomeFragment3 fragment = (HomeFragment3) getParentFragment();
            if (fragment != null) {
                fragment.onQuestionAsked("BEN", prompt, response);
            }
        });

        // Generate the AI response
        aiModel.generateAIContent(prompt);
    }

    private void addMessageToChat(String sender, String message) {
        HomeFragment3 fragment = (HomeFragment3) getParentFragment();
        if (fragment != null) {
            fragment.addMessageToChat(sender, message); // Add the message to the chat history
        }
    }
}
