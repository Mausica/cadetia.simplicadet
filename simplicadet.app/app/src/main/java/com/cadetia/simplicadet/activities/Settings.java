package com.cadetia.simplicadet.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.dao.ThemePreferences;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.BlurController;
import eightbitlab.com.blurview.RenderScriptBlur;

public class Settings extends AppCompatActivity {

    private BlurView blurView;
    private View topBarBlurBackground;
    private ScrollView scrollView;
    private ThemePreferences themePreferences;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch themeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        blurView = findViewById(R.id.blur_view);
        topBarBlurBackground = findViewById(R.id.top_bar_blur_background);
        scrollView = findViewById(R.id.settings_scroll_view);

        themePreferences = new ThemePreferences(this);
        themeSwitch = findViewById(R.id.theme_switch);
        setupThemeSwitch();

        findViewById(R.id.back_button).setOnClickListener(v -> {
            navigateBackWithAnimation();
        });

        setupBlurView();
        setupScrollListener();
    }

    private void setupThemeSwitch() {
        // Set initial state
        themeSwitch.setChecked(themePreferences.isDarkMode());

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            themePreferences.setThemeMode(newMode);
            applyTheme();
        });
    }

    private void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(themePreferences.getThemeMode());
        recreate(); // Recreate activity to apply theme immediately
    }
    private void navigateBackWithAnimation() {
        finish();
        overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateBackWithAnimation();
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