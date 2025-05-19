package com.cadetia.simplicadet.ui.home;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.cadetia.simplicadet.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeFragment3 extends Fragment {

    private AnimatorSet mSetRightOut;
    private AnimatorSet mSetLeftIn;
    private boolean mIsBackVisible = false;
    private View mCardFrontLayout;
    private View mCardBackLayout;
    private TextView mCardFrontText;
    private TextView mCardBackText;
    private final List<Flashcard> flashcards = new ArrayList<>();
    private final Random random = new Random();

    // Simple data class for flashcards
    private static class Flashcard {
        String question;
        String answer;

        Flashcard(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    public HomeFragment3() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home3, container, false);
        mCardBackLayout = view.findViewById(R.id.card_back);
        mCardFrontLayout = view.findViewById(R.id.card_front);

        // Initialize sample flashcards
        initializeFlashcards();

        // Get references to TextViews
        mCardFrontText = view.findViewById(R.id.text_view_front);
        mCardBackText = view.findViewById(R.id.text_view_back);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadAnimations();
        changeCameraDistance();
        showRandomFlashcard();

        View cardContainer = view.findViewById(R.id.card_container);
        cardContainer.setOnClickListener(v -> flipCard(v));
    }


    private void initializeFlashcards() {

        flashcards.add(new Flashcard("What is the capital of France?", "Paris"));
        flashcards.add(new Flashcard("2 + 2 × 2", "6"));
        flashcards.add(new Flashcard("Chemical symbol for gold", "Au"));
        flashcards.add(new Flashcard("Largest planet in solar system", "Jupiter"));
    }

    private void showRandomFlashcard() {
        if (!flashcards.isEmpty()) {
            Flashcard randomCard = flashcards.get(random.nextInt(flashcards.size()));
            mCardFrontText.setText(randomCard.question);
            mCardBackText.setText(randomCard.answer);
        }
    }

    private void changeCameraDistance() {
        int distance = 8000;
        float scale = getResources().getDisplayMetrics().density * distance;
        mCardFrontLayout.setCameraDistance(scale);
        mCardBackLayout.setCameraDistance(scale);
    }

    private void loadAnimations() {
        mSetRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.animator.out_animation);
        mSetLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(requireContext(), R.animator.in_animation);
    }

    public void flipCard(View view) {
        if (!mIsBackVisible) {
            mCardFrontLayout.setVisibility(View.VISIBLE);
            mCardBackLayout.setVisibility(View.VISIBLE);
            mSetRightOut.setTarget(mCardFrontLayout);
            mSetLeftIn.setTarget(mCardBackLayout);
            mSetRightOut.start();
            mSetLeftIn.start();
            mIsBackVisible = true;
        } else {
            mSetRightOut.setTarget(mCardBackLayout);
            mSetLeftIn.setTarget(mCardFrontLayout);
            mSetRightOut.start();
            mSetLeftIn.start();
            mIsBackVisible = false;
        }
    }
}