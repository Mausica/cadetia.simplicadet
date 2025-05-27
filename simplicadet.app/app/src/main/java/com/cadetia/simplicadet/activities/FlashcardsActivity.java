package com.cadetia.simplicadet.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.cadetia.simplicadet.R;
import java.util.ArrayList;
import java.util.List;

public class FlashcardsActivity extends AppCompatActivity {
    private AnimatorSet mSetRightOut, mSetLeftIn;
    private boolean mIsBackVisible = false;
    private View mCardFrontLayout, mCardBackLayout;
    private TextView mCardFrontText, mCardBackText;
    private final List<Flashcard> flashcards = new ArrayList<>();
    private int currentCardIndex = 0;
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100, SWIPE_VELOCITY_THRESHOLD = 100;

    private static class Flashcard {
        String question, answer;
        Flashcard(String question, String answer) { this.question = question; this.answer = answer; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);
        mCardBackLayout = findViewById(R.id.card_back);
        mCardFrontLayout = findViewById(R.id.card_front);
        mCardFrontText = findViewById(R.id.text_view_front);
        mCardBackText = findViewById(R.id.text_view_back);
        Button finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(v -> finish());
        initializeFlashcards();
        loadAnimations();
        changeCameraDistance();
        showCurrentFlashcard();
        setupGestureDetector();
        findViewById(R.id.card_container).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) { return true; }

            @Override
            public boolean onSingleTapUp(MotionEvent e) { flipCard(); return true; }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) showPreviousCard();
                    else showNextCard();
                    return true;
                }
                return false;
            }
        });
    }

    private void initializeFlashcards() {
        flashcards.add(new Flashcard("What is the capital of France?", "Paris"));
        flashcards.add(new Flashcard("2 + 2 × 2", "6"));
        flashcards.add(new Flashcard("Chemical symbol for gold", "Au"));
        flashcards.add(new Flashcard("Largest planet in solar system", "Jupiter"));
        flashcards.add(new Flashcard("What is the speed of light?", "299,792,458 m/s"));
        flashcards.add(new Flashcard("Who painted the Mona Lisa?", "Leonardo da Vinci"));
        flashcards.add(new Flashcard("What is the smallest prime number?", "2"));
        flashcards.add(new Flashcard("Capital of Japan", "Tokyo"));
    }

    private void showCurrentFlashcard() {
        if (!flashcards.isEmpty()) {
            currentCardIndex = ((currentCardIndex % flashcards.size()) + flashcards.size()) % flashcards.size();
            Flashcard currentCard = flashcards.get(currentCardIndex);
            mCardFrontText.setText(currentCard.question);
            mCardBackText.setText(currentCard.answer);
            mCardFrontLayout.setVisibility(View.VISIBLE);
            mCardBackLayout.setVisibility(View.GONE);
            mIsBackVisible = false;
            mCardFrontLayout.setTranslationX(0f);
            mCardFrontLayout.setAlpha(1f);
            mCardBackLayout.setTranslationX(0f);
            mCardBackLayout.setAlpha(1f);
        }
    }

    private void showNextCard() { if (!flashcards.isEmpty()) { currentCardIndex = (currentCardIndex + 1) % flashcards.size(); animateCardSwipe(true); } }

    private void showPreviousCard() { if (!flashcards.isEmpty()) { currentCardIndex = (currentCardIndex - 1 + flashcards.size()) % flashcards.size(); animateCardSwipe(false); } }

    private void animateCardSwipe(boolean isSwipeLeft) {
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float endX = isSwipeLeft ? -screenWidth : screenWidth;
        float startX = isSwipeLeft ? screenWidth : -screenWidth;
        View currentVisibleCard = mIsBackVisible ? mCardBackLayout : mCardFrontLayout;
        currentVisibleCard.animate().translationX(endX).alpha(0.3f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    resetCardTransformations();
                    showCurrentFlashcard();
                    View newVisibleCard = mCardFrontLayout;
                    newVisibleCard.setTranslationX(startX);
                    newVisibleCard.setAlpha(0.3f);
                    newVisibleCard.setVisibility(View.VISIBLE);
                    newVisibleCard.animate().translationX(0f).alpha(1f).setDuration(300)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                            .withEndAction(() -> { currentVisibleCard.setTranslationX(0f); currentVisibleCard.setAlpha(1f); }).start();
                }).start();
    }

    private void resetCardTransformations() {
        mCardFrontLayout.setRotationY(0f); mCardFrontLayout.setRotationX(0f); mCardFrontLayout.setRotation(0f);
        mCardFrontLayout.setScaleX(1f); mCardFrontLayout.setScaleY(1f); mCardFrontLayout.setTranslationX(0f);
        mCardFrontLayout.setTranslationY(0f); mCardFrontLayout.setAlpha(1f);
        mCardBackLayout.setRotationY(0f); mCardBackLayout.setRotationX(0f); mCardBackLayout.setRotation(0f);
        mCardBackLayout.setScaleX(1f); mCardBackLayout.setScaleY(1f); mCardBackLayout.setTranslationX(0f);
        mCardBackLayout.setTranslationY(0f); mCardBackLayout.setAlpha(1f);
        mCardFrontLayout.clearAnimation(); mCardBackLayout.clearAnimation();
    }

    private void changeCameraDistance() {
        int distance = 8000;
        float scale = getResources().getDisplayMetrics().density * distance;
        mCardFrontLayout.setCameraDistance(scale);
        mCardBackLayout.setCameraDistance(scale);
    }

    private void loadAnimations() {
        mSetRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.out_animation);
        mSetLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.in_animation);
    }

    private void flipCard() {
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

    public void nextCard() { showNextCard(); }
    public void previousCard() { showPreviousCard(); }
    public int getCurrentCardIndex() { return currentCardIndex; }
    public int getTotalCards() { return flashcards.size(); }
}