package com.learn.simplify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.learn.simplify.R;

public class StartTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_test);

        // Set the status bar color to transparent
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));

        // Find the back button
        ImageButton backButton = findViewById(R.id.start_back_button);
        Button startTestButton = findViewById(R.id.start_test_button);

        // Set OnClickListener to finish the activity when the back button is clicked
        backButton.setOnClickListener(v -> finish());
        startTestButton.setOnClickListener(v -> animateButtonOnClick(startTestButton, QuestionsActivity.class));

    }

    private void animateButtonOnClick(Button button, Class<?> targetActivityClass) {
        button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .withEndAction(() -> {
                            // Open the specified activity with a fade transition
                            Intent intent = new Intent(button.getContext(), targetActivityClass);
                            startActivityWithFade(intent);
                        })
                        .start())
                .start();
    }

    private void startActivityWithFade(Intent intent) {
        // Start activity with fade transition
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
