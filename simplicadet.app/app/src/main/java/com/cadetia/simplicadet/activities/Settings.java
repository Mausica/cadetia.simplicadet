package com.cadetia.simplicadet.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
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
import com.cadetia.simplicadet.entities.DialogConfirm;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.BlurController;
import eightbitlab.com.blurview.RenderScriptBlur;

public class Settings extends AppCompatActivity {

    private BlurView blurView;
    private View topBarBlurBackground;
    private View dimBackground;
    private ScrollView scrollView;
    private ThemePreferences themePreferences;
    private Switch themeSwitch;

    // Language selection related views
    private RelativeLayout languageLayout;
    private ImageView languageIcon;
    private ImageView languageArrow;
    private TextView languageText;
    private CardView languagePopup;
    private Button logout_button;
    FirebaseAuth firebaseAuth;
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
        logout_button = findViewById(R.id.logout_button);
        logout_button.setOnClickListener(v -> {
            animateButtonOnClick(logout_button);
            firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.signOut();
            DialogConfirm.show(
                    this,
                    "Logout",
                    "Are you sure you want to log out?",
                    () -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    },
                    true
            );

        });

        updateLanguageUI();
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
        languagePopup.setElevation(getResources().getDimensionPixelSize(R.dimen._24sdp));
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
        ViewGroup rootLayout = findViewById(android.R.id.content);
        rootLayout.addView(languagePopup);

        // Set click listener for language layout
        languageLayout.setOnClickListener(v -> {
            toggleLanguagePopup();
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
            // Change language
            changeLanguage(languageCodes[langIndex]);

            // Hide popup without animating the button
            toggleLanguagePopup();
        });

        return option;
    }

    private void toggleLanguagePopup() {
        isLanguagePopupShown = !isLanguagePopupShown;

        // Arrow rotation animation (keep existing code)
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

        ViewGroup rootView = findViewById(android.R.id.content);

        if (isLanguagePopupShown) {
            if (dimBackground == null) {
                dimBackground = new BlurView(this);
                dimBackground.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                // Configure BlurView to only affect background
                View decorView = getWindow().getDecorView();
                ViewGroup rootContainer = decorView.findViewById(android.R.id.content);

                ((BlurView) dimBackground).setupWith(rootContainer)
                        .setFrameClearDrawable(decorView.getBackground())
                        .setBlurRadius(12f)
                        .setBlurAutoUpdate(true);

                ((BlurView) dimBackground).setOverlayColor(
                        ContextCompat.getColor(this, R.color.focus)
                );
                dimBackground.setAlpha(0f);
                dimBackground.setOnClickListener(v -> toggleLanguagePopup());

                // Add blur view at position 1 (below popup but above main content)
                rootView.addView(dimBackground, 1);
            }

            // Make sure popup is at the top
            languagePopup.bringToFront();

            // Keep existing positioning and animation code
            languagePopup.setVisibility(View.VISIBLE);
            languagePopup.setAlpha(0f);
            languagePopup.setElevation(getResources().getDimensionPixelSize(R.dimen._16sdp));

            languagePopup.post(() -> {
                int[] buttonLocation = new int[2];
                languageLayout.getLocationInWindow(buttonLocation);
                int popupY = buttonLocation[1] + languageLayout.getHeight() +
                        getResources().getDimensionPixelSize(R.dimen._16sdp);

                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int screenHeight = metrics.heightPixels;
                int popupBottom = popupY + languagePopup.getHeight();
                int overflow = popupBottom - screenHeight;

                if (overflow > 0) {
                    popupY -= overflow;
                }

                languagePopup.setY(popupY - getStatusBarHeight());

                AnimatorSet fadeInSet = new AnimatorSet();
                ObjectAnimator popupFadeIn = ObjectAnimator.ofFloat(languagePopup, "alpha", 1f);
                ObjectAnimator blurFadeIn = ObjectAnimator.ofFloat(dimBackground, "alpha", 0.6f);

                fadeInSet.playTogether(popupFadeIn, blurFadeIn);
                fadeInSet.setDuration(300);
                fadeInSet.setInterpolator(new AccelerateDecelerateInterpolator());
                fadeInSet.start();
            });
        } else {
            if (dimBackground != null) {
                AnimatorSet fadeOutSet = new AnimatorSet();
                ObjectAnimator popupFadeOut = ObjectAnimator.ofFloat(languagePopup, "alpha", 0f);
                ObjectAnimator blurFadeOut = ObjectAnimator.ofFloat(dimBackground, "alpha", 0f);

                fadeOutSet.playTogether(popupFadeOut, blurFadeOut);
                fadeOutSet.setDuration(300);
                fadeOutSet.setInterpolator(new AccelerateDecelerateInterpolator());
                fadeOutSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        languagePopup.setVisibility(View.GONE);
                        languagePopup.setElevation(0f);
                        rootView.removeView(dimBackground);
                        dimBackground = null;
                    }
                });
                fadeOutSet.start();
            } else {
                languagePopup.setVisibility(View.GONE);
                languagePopup.setElevation(0f);
            }
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isLanguagePopupShown && ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if touch is outside the popup and language button
            int[] popupLocation = new int[2];
            languagePopup.getLocationOnScreen(popupLocation);
            int[] buttonLocation = new int[2];
            languageLayout.getLocationOnScreen(buttonLocation);

            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();

            // Check if touch is outside both the popup and the button
            if (!isPointInsideView(x, y, languagePopup) &&
                    !isPointInsideView(x, y, languageLayout)) {
                toggleLanguagePopup();
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isPointInsideView(int x, int y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        return (x > viewX && x < (viewX + view.getWidth()) &&
                y > viewY && y < (viewY + view.getHeight()));
    }

    private void changeLanguage(String languageCode) {
        if (!languageCode.equals(currentLanguage)) {
            // Save the new language preference
            SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
            prefs.edit().putString("language_code", languageCode).apply();

            currentLanguage = languageCode;

            // Update UI immediately before recreation
            updateLanguageUI();

            // Apply the language change
            setLocale(languageCode);

            // Delay recreation to allow UI updates
            new Handler().postDelayed(() -> recreate(), 100);
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
            toggleLanguagePopup();
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

    private void animateButtonOnClick(Button button) {
        button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100) // Set a shorter animation duration (in milliseconds)
                .setInterpolator(new LinearInterpolator()) // Use a LinearInterpolator for a snappy animation
                .withEndAction(() -> button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start())
                .start();
    }
}