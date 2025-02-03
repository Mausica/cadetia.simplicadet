package com.learn.simplify.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import nl.dionsegijn.konfetti.core.models.Size;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.learn.simplify.R;
import com.learn.simplify.database.DbQuery;
import com.learn.simplify.listeners.MyCompleteListener;
import com.learn.simplify.model.QuestionModel;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.xml.KonfettiView;

public class QuestionsActivity extends AppCompatActivity {
    private KonfettiView konfettiView = null;
    private TextView questionTextView;
    private TextView optionATextView, optionBTextView, optionCTextView, optionDTextView;
    private LinearLayout layoutA, layoutB, layoutC, layoutD;
    private RoundedImageView questionImage;
    private Button nextButton;
    private int currentQuestionIndex = 0;
    private TextView textNumberQuestion, textNumberTests;
    private QuestionModel currentQuestion;
    private Handler handler = new Handler();
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private static final long QUESTION_TIME = 10000; // 10 seconds
    private int totalScore = 0; // Total score
    private long remainingTime = QUESTION_TIME; // Remaining time for the current question

    private void onQAFinished() {
        int totalScore = calculateTotalScore(); // Calculate the total score
        Intent intent = new Intent();
        intent.putExtra("totalScore", totalScore);
        setResult(Activity.RESULT_OK, intent); // Pass total score as a result
        finish(); // Finish the current activity
    }

    private int calculateTotalScore() {
        // Logic to calculate total score
        return totalScore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));

        konfettiView = findViewById(R.id.konfettiView);
        questionTextView = findViewById(R.id.text_main_question);
        optionATextView = findViewById(R.id.vA);
        optionBTextView = findViewById(R.id.vB);
        optionCTextView = findViewById(R.id.vC);
        optionDTextView = findViewById(R.id.vD);
        layoutA = findViewById(R.id.layoutA);
        layoutB = findViewById(R.id.layoutB);
        layoutC = findViewById(R.id.layoutC);
        layoutD = findViewById(R.id.layoutD);
        questionImage = findViewById(R.id.imageQuizz);
        nextButton = findViewById(R.id.questions_next_button);
        progressBar = findViewById(R.id.progress_bar);
        textNumberQuestion = findViewById(R.id.text_number_question);
        textNumberTests = findViewById(R.id.text_number_tests);

        nextButton.setVisibility(View.GONE); // Hide the next button initially

        setOptionClickListeners();

        // Retrieve the categoryId and testId from the intent
        Intent intent = getIntent();
        String categoryId = intent.getStringExtra("categoryId");
        String testId = intent.getStringExtra("testId");

        loadQuestions(categoryId, testId);

        nextButton.setOnClickListener(v -> {
            if (nextButton.getText().toString().equals("Finish")) {
                animateButtonOnClickNull(nextButton);
                handler.postDelayed(() -> {
                    setResult(Activity.RESULT_OK); // Set the result to OK
                    onQAFinished(); ; // Finish the activity
                }, 100);
            }
        });
    }

    private void loadQuestions(String categoryId, String testId) {
        DbQuery.loadQuestions(categoryId, testId, new MyCompleteListener() {
            @Override
            public void onSucces() {
                if (!DbQuery.g_quesList.isEmpty()) {
                    displayQuestion(currentQuestionIndex);
                    textNumberTests.setText(" of " + DbQuery.g_quesList.size());
                } else {
                    // Handle case when no questions are available
                    showNoQuestionsMessage();
                }
            }

            @Override
            public void onFailure() {
                // Handle failure
            }
        });
    }

    private void setOptionClickListeners() {
        layoutA.setOnClickListener(v -> onOptionClick(layoutA, optionATextView));
        layoutB.setOnClickListener(v -> onOptionClick(layoutB, optionBTextView));
        layoutC.setOnClickListener(v -> onOptionClick(layoutC, optionCTextView));
        layoutD.setOnClickListener(v -> onOptionClick(layoutD, optionDTextView));
    }

    private void setNonClickable() {
        layoutA.setEnabled(false);
        layoutB.setEnabled(false);
        layoutC.setEnabled(false);
        layoutD.setEnabled(false);
    }

    public void explodeOnCorrectAnswer(View correctAnswerView) {
        int[] location = new int[2];
        correctAnswerView.getLocationOnScreen(location);

        float x = location[0] + correctAnswerView.getWidth() / 2f;
        float y = location[1] + correctAnswerView.getHeight() / 2f;

        Rect rect = new Rect();
        konfettiView.getGlobalVisibleRect(rect);
        x -= rect.left;
        y -= rect.top;

        EmitterConfig emitterConfig = new Emitter(100L, TimeUnit.MILLISECONDS).max(50);
        konfettiView.start(
                new PartyFactory(emitterConfig)
                        .spread(360)
                        .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                        .setSpeedBetween(0f, 30f)
                        .position(new Position.Absolute(x, y))
                        .build());
    }

    private void setClickable() {
        layoutA.setEnabled(true);
        layoutB.setEnabled(true);
        layoutC.setEnabled(true);
        layoutD.setEnabled(true);
    }

    private void onOptionClick(LinearLayout layout, TextView optionTextView) {
        // Cancel the timer when an option is clicked
        if (countDownTimer != null) {
            countDownTimer.cancel();
            progressBar.setProgress(0);
        }
        setNonClickable();
        animateLayout(layout);
        layout.setBackgroundResource(R.drawable.home_background_quizz); // Highlight selected option
        checkAnswer(optionTextView.getText().toString()); // Check the selected answer
        // Delay to move to the next question
        handler.postDelayed(() -> {
            if (currentQuestionIndex < DbQuery.g_quesList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion(currentQuestionIndex);
                currentQuestion.setUserSelectedAnswer(null);
                resetOptionBackgrounds();
            }
        }, 3000); // 3 seconds delay
    }

    private void checkAnswer(String selectedAnswer) {
        currentQuestion.setUserSelectedAnswer(selectedAnswer); // Store the user-selected answer
        if (selectedAnswer.equals(currentQuestion.getCorrectAnswer())) {
            // Correct answer
            int scoreForQuestion = calculateScoreForQuestion();
            totalScore += scoreForQuestion; // Update the total score
            highlightCorrectAnswer(true);
            explodeOnCorrectAnswer(getSelectedTextView(currentQuestion.getCorrectAnswer()));
        } else {
            // Incorrect answer
            totalScore += 1; // Add 1 point for an incorrect answer
            highlightCorrectAnswer(false);
        }
    }

    private int calculateScoreForQuestion() {
        // Calculate the score based on the remaining time
        int timeRemainingSeconds = (int) (remainingTime / 1000);
        return Math.min(10, timeRemainingSeconds + 1); // Return score from 1 to 10
    }

    private void highlightCorrectAnswer(boolean isCorrect) {
        if (isCorrect) {
            // Highlight correct answer
            getSelectedTextView(currentQuestion.getCorrectAnswer()).setBackgroundResource(R.drawable.background_correct);
        } else {
            // Highlight selected answer and correct answer
            if (currentQuestion.getUserSelectedAnswer() != null) {
                getSelectedTextView(currentQuestion.getUserSelectedAnswer()).setBackgroundResource(R.drawable.background_incorrect);
            }
            getSelectedTextView(currentQuestion.getCorrectAnswer()).setBackgroundResource(R.drawable.background_correct);
        }
        if (!(currentQuestionIndex < DbQuery.g_quesList.size() - 1)) {
            handler.postDelayed(() -> {
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setText("Finish");
            }, 1000);
        }
    }

    private void resetOptionBackgrounds() {
        optionATextView.setBackgroundResource(R.drawable.home_background_quizz);
        optionBTextView.setBackgroundResource(R.drawable.home_background_quizz);
        optionCTextView.setBackgroundResource(R.drawable.home_background_quizz);
        optionDTextView.setBackgroundResource(R.drawable.home_background_quizz);
        setClickable();
        progressBar.setProgress((int) QUESTION_TIME);
        startQuestionTimer();
    }

    private TextView getSelectedTextView(String answer) {
        if (answer == null) {
            return null; // Return null if the answer is null to avoid NullPointerException
        }
        if (answer.equals(optionATextView.getText().toString())) {
            return optionATextView;
        } else if (answer.equals(optionBTextView.getText().toString())) {
            return optionBTextView;
        } else if (answer.equals(optionCTextView.getText().toString())) {
            return optionCTextView;
        } else {
            return optionDTextView;
        }
    }

    private void showNoQuestionsMessage() {
        questionTextView.setText("No questions available for this test.");
        Log.e("TAG", "Error loading questions: ");
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Finish");
    }

    private void displayQuestion(int index) {
        if (index >= 0 && index < DbQuery.g_quesList.size()) {
            QuestionModel question = DbQuery.g_quesList.get(index);
            currentQuestion = question; // Set current question

            questionTextView.setText(question.getQuestion());
            optionATextView.setText(question.getOptionA());
            optionBTextView.setText(question.getOptionB());
            optionCTextView.setText(question.getOptionC());
            optionDTextView.setText(question.getOptionD());
            textNumberQuestion.setText("Question " + (index + 1));
            if (question.getImage() != null && !question.getImage().isEmpty()) {
                questionImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(question.getImage()).into(questionImage);
            } else {
                questionImage.setVisibility(View.GONE);
            }

            startQuestionTimer();
        } else {
            // Handle the case when index is out of bounds
            showNoQuestionsMessage();
        }
    }

    private void startQuestionTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Cancel the previous timer
        }

        progressBar.setMax((int) QUESTION_TIME);
        progressBar.setProgress((int) QUESTION_TIME);

        countDownTimer = new CountDownTimer(QUESTION_TIME, 30) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressBar.setProgress((int) millisUntilFinished);
                remainingTime = millisUntilFinished; // Update the remaining time
            }

            @Override
            public void onFinish() {
                // Time is up, highlight the correct answer and move to the next question
                remainingTime = 0; // No time remaining
                setNonClickable();
                highlightCorrectAnswer(false);
                handler.postDelayed(() -> {
                    if (currentQuestionIndex < DbQuery.g_quesList.size() - 1) {
                        currentQuestionIndex++;
                        displayQuestion(currentQuestionIndex);
                        currentQuestion.setUserSelectedAnswer(null);
                        resetOptionBackgrounds();
                    }
                }, 3000); // 3 seconds delay

            }
        }.start();
    }

    private void animateButtonOnClickNull(Button button) {
        button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .start())
                .start();
    }

    private void animateLayout(View layout) {
        layout.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> layout.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .start())
                .start();
    }

}
