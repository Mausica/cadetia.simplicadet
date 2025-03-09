package com.cadetia.simplicadet.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.entities.LoadingView;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.QuestionModel;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private static final long QUESTION_TIME = 10000; // 10 secunde
    private int totalScore = 0; // Punctajul total
    private long remainingTime = QUESTION_TIME;
    private LoadingView loadingView;


    private void onQAFinished() {
        Intent intent = new Intent();
        intent.putExtra("totalScore", totalScore);
        setResult(Activity.RESULT_OK, intent);
        finish();
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
        loadingView = findViewById(R.id.loadingView);


        nextButton.setVisibility(View.GONE);

        setOptionClickListeners();

        Intent intent = getIntent();
        String categoryId = intent.getStringExtra("categoryId");
        String testId = intent.getStringExtra("testId");

        loadQuestions(categoryId, testId);

        nextButton.setOnClickListener(v -> {
            if (nextButton.getText().toString().equals("Finish")) {
                animateButtonOnClickNull(nextButton);
                handler.postDelayed(() -> {
                    setResult(Activity.RESULT_OK);
                    onQAFinished();
                }, 100);
            }
        });
    }

    private void loadQuestions(String categoryId, String testId) {
        loadingView.startLoadingAnimation(R.raw.uni_loading, true);

        DbQuery.loadQuestions(categoryId, testId, new MyCompleteListener() {
            @Override
            public void onSucces() {
                if (!DbQuery.g_quesList.isEmpty()) {
                    preloadImages(() -> {
                        loadingView.stopLoadingAnimation();
                        Collections.shuffle(DbQuery.g_quesList);
                        displayQuestion(currentQuestionIndex);
                        textNumberTests.setText(" of " + DbQuery.g_quesList.size());
                    });
                } else {
                    showNoQuestionsMessage();
                }
            }

            @Override
            public void onFailure() {

                loadingView.stopLoadingAnimation();
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

        EmitterConfig emitterConfig = new Emitter(100L, java.util.concurrent.TimeUnit.MILLISECONDS).max(50);
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
        if (countDownTimer != null) {
            countDownTimer.cancel();
            progressBar.setProgress(0);
        }
        setNonClickable();
        animateLayout(layout);
        layout.setBackgroundResource(R.drawable.home_background_quizz);
        checkAnswer(optionTextView.getText().toString());
        handler.postDelayed(() -> {
            if (currentQuestionIndex < DbQuery.g_quesList.size() - 1) {
                currentQuestionIndex++;
                displayQuestion(currentQuestionIndex);
                currentQuestion.setUserSelectedAnswer(null);
                resetOptionBackgrounds();
            }
        }, 3000);
    }

    private void checkAnswer(String selectedAnswer) {
        currentQuestion.setUserSelectedAnswer(selectedAnswer);
        if (selectedAnswer.equals(currentQuestion.getCorrectAnswer())) {
            totalScore += currentQuestion.getPoints();
            highlightCorrectAnswer(true);
            explodeOnCorrectAnswer(getSelectedTextView(currentQuestion.getCorrectAnswer()));
        } else {
            totalScore += 1;
            highlightCorrectAnswer(false);
        }
    }

    private void highlightCorrectAnswer(boolean isCorrect) {
        if (isCorrect) {
            getSelectedTextView(currentQuestion.getCorrectAnswer()).setBackgroundResource(R.drawable.background_task_correct);
        } else {
            if (currentQuestion.getUserSelectedAnswer() != null) {
                getSelectedTextView(currentQuestion.getUserSelectedAnswer()).setBackgroundResource(R.drawable.background_task_incorrect);
            }
            getSelectedTextView(currentQuestion.getCorrectAnswer()).setBackgroundResource(R.drawable.background_task_correct);
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
            return null;
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

    private void preloadImages(Runnable onPreloadComplete) {
        List<String> imageUrls = new ArrayList<>();
        for (QuestionModel question : DbQuery.g_quesList) {
            if (question.getImage() != null && !question.getImage().isEmpty()) {
                imageUrls.add(question.getImage());
            }
        }

        if (imageUrls.isEmpty()) {
            onPreloadComplete.run();
            return;
        }

        final int[] loadedImages = {0};

        for (String imageUrl : imageUrls) {

            RequestOptions requestOptions = new RequestOptions()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            Glide.with(this)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(questionImage);
            loadedImages[0]++;

            if (loadedImages[0] == imageUrls.size()) {
                onPreloadComplete.run();
            }
        }
    }


    private void displayQuestion(int index) {
        if (index >= 0 && index < DbQuery.g_quesList.size()) {
            QuestionModel question = DbQuery.g_quesList.get(index);
            currentQuestion = question;

            questionTextView.setText(question.getQuestion());

            List<Pair<String, Boolean>> options = new ArrayList<>();
            options.add(new Pair<>(question.getOptionA(), question.getCorrectAnswer().equals("A")));
            options.add(new Pair<>(question.getOptionB(), question.getCorrectAnswer().equals("B")));
            options.add(new Pair<>(question.getOptionC(), question.getCorrectAnswer().equals("C")));
            options.add(new Pair<>(question.getOptionD(), question.getCorrectAnswer().equals("D")));

            Collections.shuffle(options);

            optionATextView.setText(options.get(0).first);
            optionBTextView.setText(options.get(1).first);
            optionCTextView.setText(options.get(2).first);
            optionDTextView.setText(options.get(3).first);

            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).second) {
                    currentQuestion.setCorrectAnswer(new String[]{"A", "B", "C", "D"}[i]);
                    break;
                }
            }

            textNumberQuestion.setText("Question " + (index + 1));
            if (question.getImage() != null && !question.getImage().isEmpty()) {
                questionImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(question.getImage()).into(questionImage);
            } else {
                questionImage.setVisibility(View.GONE);
            }

            startQuestionTimer();
        } else {
            showNoQuestionsMessage();
        }
    }


    private void startQuestionTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        progressBar.setMax((int) QUESTION_TIME);
        progressBar.setProgress((int) QUESTION_TIME);

        countDownTimer = new CountDownTimer(QUESTION_TIME, 30) {
            @Override
            public void onTick(long millisUntilFinished) {
                progressBar.setProgress((int) millisUntilFinished);
                remainingTime = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                remainingTime = 0;
                setNonClickable();
                highlightCorrectAnswer(false);
                handler.postDelayed(() -> {
                    if (currentQuestionIndex < DbQuery.g_quesList.size() - 1) {
                        currentQuestionIndex++;
                        displayQuestion(currentQuestionIndex);
                        currentQuestion.setUserSelectedAnswer(null);
                        resetOptionBackgrounds();
                    }
                }, 3000);
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
