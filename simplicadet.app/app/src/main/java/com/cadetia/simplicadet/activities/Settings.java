package com.cadetia.simplicadet.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.dao.ThemePreferences;

import java.util.Locale;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.BlurController;
import eightbitlab.com.blurview.RenderScriptBlur;

public class Settings extends AppCompatActivity {

    private BlurView blurView;
    private View topBarBlurBackground;
    private ScrollView scrollView;
    private ThemePreferences themePreferences;
    private Switch themeSwitch;

    // Language selection related views
    private RelativeLayout languageLayout;
    private ImageView languageIcon;
    private ImageView languageArrow;
    private TextView languageText;
    private CardView languagePopup;
    private boolean isLanguagePopupShown = false;

    // Supported languages
    private final String[] languageCodes = {"en", "es", "fr", "ro"};
    private final int[] languageIcons = {
            R.raw.language_en,
            R.raw.language_es,
            R.raw.language_fr,
            R.raw.language_ro
    };
    private final String[] languageNames = {
            "English",
            "Español",
            "Français",
            "Română"
    };

    // Current language
    private String currentLanguage = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Load current language
        SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        currentLanguage = prefs.getString("language_code", "en");

        blurView = findViewById(R.id.blur_view);
        topBarBlurBackground = findViewById(R.id.top_bar_blur_background);
        scrollView = findViewById(R.id.settings_scroll_view);

        themePreferences = new ThemePreferences(this);
        themeSwitch = findViewById(R.id.theme_switch);
        setupThemeSwitch();

        // Initialize language selection components
        languageLayout = findViewById(R.id.settings_language);
        languageIcon = findViewById(R.id.language_icon);
        languageArrow = findViewById(R.id.language_arrow);
        languageText = findViewById(R.id.language_text);

        // Set the initial language icon and text
        updateLanguageUI();

        // Setup language popup
        setupLanguagePopup();

        findViewById(R.id.back_button).setOnClickListener(v -> {
            navigateBackWithAnimation();
        });

        setupBlurView();
        setupScrollListener();
    }

    private void setupLanguagePopup() {
        // Create the language popup programmatically
        languagePopup = new CardView(this);
        languagePopup.setRadius(getResources().getDimensionPixelSize(R.dimen._8sdp));
        languagePopup.setElevation(getResources().getDimensionPixelSize(R.dimen._4sdp));
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.backgroundLight, typedValue, true);
        int color = typedValue.data;
        languagePopup.setCardBackgroundColor(color);
        languagePopup.setVisibility(View.GONE);

        // Set layout parameters
        RelativeLayout.LayoutParams popupParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        languagePopup.setContentPadding(
                getResources().getDimensionPixelSize(R.dimen._8sdp),
                getResources().getDimensionPixelSize(R.dimen._8sdp),
                getResources().getDimensionPixelSize(R.dimen._8sdp),
                getResources().getDimensionPixelSize(R.dimen._8sdp)
        );

        popupParams.addRule(RelativeLayout.BELOW, R.id.settings_language);
        popupParams.setMargins(
                getResources().getDimensionPixelSize(R.dimen._16sdp),
                getResources().getDimensionPixelSize(R.dimen._16sdp),
                getResources().getDimensionPixelSize(R.dimen._16sdp),
                0
        );
        languagePopup.setLayoutParams(popupParams);

        // Create a vertical layout for language options
        LinearLayout languageOptions = new LinearLayout(this);
        languageOptions.setOrientation(LinearLayout.VERTICAL);
        languageOptions.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Add language options
        for (int i = 0; i < languageCodes.length; i++) {
            RelativeLayout option = createLanguageOption(i);
            languageOptions.addView(option);
        }

        languagePopup.addView(languageOptions);

        // Add the popup to the root layout
        ViewGroup rootLayout = findViewById(R.id.settings);
        rootLayout.addView(languagePopup);

        // Set click listener for language layout
        languageLayout.setOnClickListener(v -> {
            toggleLanguagePopup(true);
        });
    }

    private RelativeLayout createLanguageOption(int index) {
        RelativeLayout option = new RelativeLayout(this);
        option.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen._50sdp)
        ));
        option.setPadding(
                getResources().getDimensionPixelSize(R.dimen._16sdp),
                0,
                getResources().getDimensionPixelSize(R.dimen._16sdp),
                0
        );

        // Create language flag icon
        ImageView flagIcon = new ImageView(this);
        flagIcon.setId(View.generateViewId());
        RelativeLayout.LayoutParams flagParams = new RelativeLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen._24sdp),
                getResources().getDimensionPixelSize(R.dimen._24sdp)
        );
        flagParams.addRule(RelativeLayout.CENTER_VERTICAL);
        flagIcon.setLayoutParams(flagParams);
        flagIcon.setImageResource(languageIcons[index]);

        // Create language name text
        TextView nameText = new TextView(this);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.addRule(RelativeLayout.CENTER_VERTICAL);
        textParams.addRule(RelativeLayout.RIGHT_OF, flagIcon.getId());
        textParams.setMargins(getResources().getDimensionPixelSize(R.dimen._12sdp), 0, 0, 0);
        nameText.setLayoutParams(textParams);
        nameText.setText(languageNames[index]);

        Typeface typeface = ResourcesCompat.getFont(this, R.font.circular_medium);
        nameText.setTypeface(typeface);

        int[] attrs = new int[]{R.attr.textNormal};
        TypedArray ta = obtainStyledAttributes(attrs);
        int textColor = ta.getColor(0, ContextCompat.getColor(this, android.R.color.black));
        ta.recycle();
        nameText.setTextColor(textColor);
        nameText.setTextSize(getResources().getDimensionPixelSize(R.dimen._5ssp));

        option.addView(flagIcon);
        option.addView(nameText);

        // Add click listener
        final int langIndex = index;
        option.setOnClickListener(v -> {
            // Animate the option
            Animation clickAnimation = AnimationUtils.loadAnimation(this, R.anim.click_animation);
            option.startAnimation(clickAnimation);

            // Change language
            changeLanguage(languageCodes[langIndex]);

            // Hide popup without animating the button
            toggleLanguagePopup(false);
        });

        return option;
    }

    private void toggleLanguagePopup(boolean animateButton) {
        isLanguagePopupShown = !isLanguagePopupShown;

        // Animate the button only if triggered by the button click
        if (animateButton) {
            Animation clickAnimation = AnimationUtils.loadAnimation(this, R.anim.click_animation);
            languageLayout.startAnimation(clickAnimation);
        }

        // Arrow rotation animation
        ObjectAnimator rotateArrow = ObjectAnimator.ofFloat(
                languageArrow,
                "rotation",
                isLanguagePopupShown ? 0 : 90,
                isLanguagePopupShown ? 90 : 0
        );
        rotateArrow.setDuration(300);
        rotateArrow.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animSet = new AnimatorSet();
        animSet.play(rotateArrow);
        animSet.start();

        if (isLanguagePopupShown) {
            // Calculate the button's position on the screen
            int[] buttonLocation = new int[2];
            languageLayout.getLocationOnScreen(buttonLocation);

            // Get the root layout's position to adjust coordinates
            ViewGroup rootLayout = findViewById(R.id.settings);
            int[] rootLocation = new int[2];
            rootLayout.getLocationOnScreen(rootLocation);

            // Calculate Y position relative to the root layout
            int yPosition = buttonLocation[1] - rootLocation[1] + languageLayout.getHeight();

            // Apply margins (adjust if needed)
            int margin = getResources().getDimensionPixelSize(R.dimen._16sdp);
            yPosition += margin;

            // Set the CardView's position
            languagePopup.setY(yPosition);
            languagePopup.setVisibility(View.VISIBLE);
            languagePopup.setAlpha(0f);
            languagePopup.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            languagePopup.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withEndAction(() -> languagePopup.setVisibility(View.GONE))
                    .start();
        }
    }

    private void changeLanguage(String languageCode) {
        if (!languageCode.equals(currentLanguage)) {
            // Save the new language preference
            SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
            prefs.edit().putString("language_code", languageCode).apply();

            currentLanguage = languageCode;

            // Update the language UI
            updateLanguageUI();

            // Apply the language change
            setLocale(languageCode);

            // Restart the activity to apply changes
            recreate();
        }
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void updateLanguageUI() {
        // Find the index of the current language
        int index = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                index = i;
                break;
            }
        }

        // Update icon and text
        languageIcon.setImageResource(languageIcons[index]);
        languageText.setText(languageNames[index]);
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
        // Handle back press when language popup is shown
        if (isLanguagePopupShown) {
            toggleLanguagePopup(false);
            return;
        }
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