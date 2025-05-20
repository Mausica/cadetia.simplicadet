package com.cadetia.simplicadet.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.cadetia.simplicadet.R;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.BlurController;
import eightbitlab.com.blurview.RenderScriptBlur;

public class Settings extends AppCompatActivity {

    private BlurView blurView;
    private View topBarBlurBackground;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        blurView = findViewById(R.id.blur_view);
        topBarBlurBackground = findViewById(R.id.top_bar_blur_background);
        scrollView = findViewById(R.id.settings_scroll_view);

        findViewById(R.id.back_button).setOnClickListener(v -> onBackPressed());

        setupBlurView();
        setupScrollListener();
    }

    private void setupBlurView() {
        final View decorView = getWindow().getDecorView();
        final ViewGroup rootView = decorView.findViewById(android.R.id.content);
        final Drawable windowBackground = decorView.getBackground();

        blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(10f)
                .setBlurAutoUpdate(true);
    }

    private void setupScrollListener() {
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float threshold = 50f;
            float alpha = Math.min(1f, scrollY / threshold);
            topBarBlurBackground.setAlpha(alpha);
        });
    }
}