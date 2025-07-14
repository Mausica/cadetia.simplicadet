package com.cadetia.simplicadet.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;

import java.util.ArrayList;
import java.util.List;

public class Flashcards extends AppCompatActivity {
    private AnimatorSet mSetRightOut, mSetLeftIn;
    private boolean mIsBackVisible = false;
    private View mCardFrontLayout, mCardBackLayout;
    private TextView mCardFrontText, mCardBackText;
    private ImageView mCardFrontImage, mCardBackImage;
    private List<DbQuery.FlashcardModel> flashcards = new ArrayList<>();
    private int currentCardIndex = 0;
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100, SWIPE_VELOCITY_THRESHOLD = 100;
    private int position = -1;
    private String flashcardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        mCardBackLayout = findViewById(R.id.card_back);
        mCardFrontLayout = findViewById(R.id.card_front);
        mCardFrontText = findViewById(R.id.text_view_front);
        mCardBackText = findViewById(R.id.text_view_back);
        mCardFrontImage = findViewById(R.id.front_image);
        mCardBackImage = findViewById(R.id.back_image);
        Button finishButton = findViewById(R.id.finish_button);

        flashcardId = getIntent().getStringExtra("flashcardId");
        position = getIntent().getIntExtra("position", -1);

        finishButton.setOnClickListener(v -> finishActivityWithResult());

        loadFlashcardsFromDb();
        loadAnimations();
        changeCameraDistance();
        setupGestureDetector();
        findViewById(R.id.card_container).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void finishActivityWithResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("position", position);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void loadFlashcardsFromDb() {
        DbQuery.loadFlashcards(flashcardId, new MyCompleteListener() {
            @Override public void onSucces() {
                flashcards = DbQuery.g_flashcardList;
                if (!flashcards.isEmpty()) showCurrentFlashcard();
                else { Toast.makeText(Flashcards.this, "No flashcards.", Toast.LENGTH_SHORT).show(); finish(); }
            }
            @Override public void onFailure() { Toast.makeText(Flashcards.this, "Failed to load.", Toast.LENGTH_SHORT).show(); finish(); }
        });
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent e) { flipCard(); return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(vX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) showPreviousCard(); else showNextCard();
                    return true;
                }
                return false;
            }
        });
    }

    private void showCurrentFlashcard() {
        if (!flashcards.isEmpty()) {
            currentCardIndex = ((currentCardIndex % flashcards.size()) + flashcards.size()) % flashcards.size();
            DbQuery.FlashcardModel c = flashcards.get(currentCardIndex);
            mCardFrontText.setText(c.question); mCardBackText.setText(c.answer);
            loadImage(c.frontImage, mCardFrontImage); loadImage(c.backImage, mCardBackImage);
            mCardFrontLayout.setVisibility(View.VISIBLE); mCardBackLayout.setVisibility(View.GONE);
            mIsBackVisible = false; resetCardTransformations();
        }
    }

    private void loadImage(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) { imageView.setVisibility(View.VISIBLE); Glide.with(this).load(url).into(imageView); }
        else imageView.setVisibility(View.GONE);
    }

    private void showNextCard() { if (!flashcards.isEmpty()) { currentCardIndex = (currentCardIndex + 1) % flashcards.size(); animateCardSwipe(true); } }
    private void showPreviousCard() { if (!flashcards.isEmpty()) { currentCardIndex = (currentCardIndex - 1 + flashcards.size()) % flashcards.size(); animateCardSwipe(false); } }

    private void animateCardSwipe(boolean isSwipeLeft) {
        float endX = isSwipeLeft ? -getResources().getDisplayMetrics().widthPixels : getResources().getDisplayMetrics().widthPixels;
        View current = mIsBackVisible ? mCardBackLayout : mCardFrontLayout;
        current.animate().translationX(endX).alpha(0.3f).setDuration(300)
                .withEndAction(() -> {
                    showCurrentFlashcard(); View next = mCardFrontLayout;
                    next.setTranslationX(-endX); next.setAlpha(0.3f); next.setVisibility(View.VISIBLE);
                    next.animate().translationX(0f).alpha(1f).setDuration(300).start();
                }).start();
    }

    private void resetCardTransformations() {
        View[] v = {mCardFrontLayout, mCardBackLayout};
        for(View view : v) { view.setRotationY(0f); view.setRotationX(0f); view.setRotation(0f); view.setScaleX(1f); view.setScaleY(1f); view.setTranslationX(0f); view.setTranslationY(0f); view.setAlpha(1f); view.clearAnimation(); }
    }

    private void changeCameraDistance() {
        float s = getResources().getDisplayMetrics().density * 8000;
        mCardFrontLayout.setCameraDistance(s); mCardBackLayout.setCameraDistance(s);
    }

    private void loadAnimations() {
        mSetRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.out_animation);
        mSetLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.in_animation);
    }

    private void flipCard() {
        View front = mIsBackVisible ? mCardBackLayout : mCardFrontLayout; View back = mIsBackVisible ? mCardFrontLayout : mCardBackLayout;
        mCardFrontLayout.setVisibility(View.VISIBLE); mCardBackLayout.setVisibility(View.VISIBLE);
        mSetRightOut.setTarget(front); mSetLeftIn.setTarget(back);
        mSetRightOut.start(); mSetLeftIn.start();
        mIsBackVisible = !mIsBackVisible;
    }
}