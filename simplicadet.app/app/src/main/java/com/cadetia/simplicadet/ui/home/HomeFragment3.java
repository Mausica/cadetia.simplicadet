package com.cadetia.simplicadet.ui.home;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.cadetia.simplicadet.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment3 extends Fragment {

    private AnimatorSet mSetRightOut;
    private AnimatorSet mSetLeftIn;
    private boolean mIsBackVisible = false;
    private View mCardFrontLayout;
    private View mCardBackLayout;
    private TextView mCardFrontText;
    private TextView mCardBackText;
    private final List<Flashcard> flashcards = new ArrayList<>();
    private int currentCardIndex = 0;
    private GestureDetector gestureDetector;

    // Minimum swipe distance (in pixels)
    private static final int SWIPE_THRESHOLD = 100;
    // Minimum swipe velocity
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

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
        showCurrentFlashcard();
        setupGestureDetector();

        View cardContainer = view.findViewById(R.id.card_container);
        cardContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // Handle tap to flip card
                flipCard();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Check if horizontal swipe is more prominent than vertical
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - go to previous card
                            showPreviousCard();
                        } else {
                            // Swipe left - go to next card
                            showNextCard();
                        }
                        return true;
                    }
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
            // Ensure index is within bounds (this handles the looping)
            currentCardIndex = ((currentCardIndex % flashcards.size()) + flashcards.size()) % flashcards.size();

            Flashcard currentCard = flashcards.get(currentCardIndex);
            mCardFrontText.setText(currentCard.question);
            mCardBackText.setText(currentCard.answer);

            // Always reset to front view when showing new card (but don't animate)
            mCardFrontLayout.setVisibility(View.VISIBLE);
            mCardBackLayout.setVisibility(View.GONE);
            mIsBackVisible = false;

            // Ensure proper positioning
            mCardFrontLayout.setTranslationX(0f);
            mCardFrontLayout.setAlpha(1f);
            mCardBackLayout.setTranslationX(0f);
            mCardBackLayout.setAlpha(1f);
        }
    }

    private void showNextCard() {
        if (!flashcards.isEmpty()) {
            currentCardIndex = (currentCardIndex + 1) % flashcards.size();
            animateCardSwipe(true);
        }
    }

    private void showPreviousCard() {
        if (!flashcards.isEmpty()) {
            currentCardIndex = (currentCardIndex - 1 + flashcards.size()) % flashcards.size();
            animateCardSwipe(false);
        }
    }

    private void animateCardSwipe(boolean isSwipeLeft) {
        // Get screen width for smooth animation
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        float endX = isSwipeLeft ? -screenWidth : screenWidth;
        float startX = isSwipeLeft ? screenWidth : -screenWidth;

        // Animate both cards to ensure smooth transition
        View currentVisibleCard = mIsBackVisible ? mCardBackLayout : mCardFrontLayout;
        View currentHiddenCard = mIsBackVisible ? mCardFrontLayout : mCardBackLayout;

        // First, slide out the current visible card
        currentVisibleCard.animate()
                .translationX(endX)
                .alpha(0.3f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    // Reset both cards' transformations completely
                    resetCardTransformations();

                    // Update the card content
                    showCurrentFlashcard();

                    // Prepare the new card from the opposite side
                    View newVisibleCard = mCardFrontLayout; // Always show front of new card
                    newVisibleCard.setTranslationX(startX);
                    newVisibleCard.setAlpha(0.3f);
                    newVisibleCard.setVisibility(View.VISIBLE);

                    // Slide in the new card
                    newVisibleCard.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                            .withEndAction(() -> {
                                // Clean up: reset the old card position
                                currentVisibleCard.setTranslationX(0f);
                                currentVisibleCard.setAlpha(1f);
                            })
                            .start();
                })
                .start();
    }

    private void resetCardTransformations() {
        // Reset all possible transformations on both cards
        mCardFrontLayout.setRotationY(0f);
        mCardFrontLayout.setRotationX(0f);
        mCardFrontLayout.setRotation(0f);
        mCardFrontLayout.setScaleX(1f);
        mCardFrontLayout.setScaleY(1f);
        mCardFrontLayout.setTranslationX(0f);
        mCardFrontLayout.setTranslationY(0f);
        mCardFrontLayout.setAlpha(1f);

        mCardBackLayout.setRotationY(0f);
        mCardBackLayout.setRotationX(0f);
        mCardBackLayout.setRotation(0f);
        mCardBackLayout.setScaleX(1f);
        mCardBackLayout.setScaleY(1f);
        mCardBackLayout.setTranslationX(0f);
        mCardBackLayout.setTranslationY(0f);
        mCardBackLayout.setAlpha(1f);

        // Clear any ongoing animations
        mCardFrontLayout.clearAnimation();
        mCardBackLayout.clearAnimation();
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

    // Public methods for external navigation (if needed)
    public void nextCard() {
        showNextCard();
    }

    public void previousCard() {
        showPreviousCard();
    }

    public int getCurrentCardIndex() {
        return currentCardIndex;
    }

    public int getTotalCards() {
        return flashcards.size();
    }
}