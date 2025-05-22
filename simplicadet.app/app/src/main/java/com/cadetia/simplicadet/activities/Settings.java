package com.cadetia.simplicadet.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
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
import com.cadetia.simplicadet.dao.LanguagePreferences;
import com.cadetia.simplicadet.dao.LocaleHelper;
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

    private RelativeLayout languageLayout;
    private ImageView languageIcon;
    private ImageView languageArrow;
    private TextView languageText;
    private CardView languagePopup;
    private Button logout_button;
    FirebaseAuth firebaseAuth;
    private boolean isLanguagePopupShown = false;

    private final String[] languageCodes = {"en_GB", "es_ES", "fr_FR", "ro_RO"};
    private final String[] languageNames = {"English", "Español", "Français", "Română"};
    private final int[] languageIcons = {
            R.raw.language_en,
            R.raw.language_es,
            R.raw.language_fr,
            R.raw.language_ro
    };

    private String currentLanguage = "en";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            int uiMode = overrideConfiguration.uiMode;
            overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
            overrideConfiguration.uiMode = uiMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Get current language AFTER locale is set by attachBaseContext
        currentLanguage = LocaleHelper.getLanguage(this);

        blurView = findViewById(R.id.blur_view);
        topBarBlurBackground = findViewById(R.id.top_bar_blur_background);
        scrollView = findViewById(R.id.settings_scroll_view);

        themePreferences = new ThemePreferences(this);
        themeSwitch = findViewById(R.id.theme_switch);
        setupThemeSwitch();

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
                    getString(R.string.logout), // Use string resource
                    getString(R.string.logout_confirmation), // Use string resource
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
        languagePopup = new CardView(this);
        languagePopup.setRadius(getResources().getDimensionPixelSize(R.dimen._8sdp));
        languagePopup.setElevation(getResources().getDimensionPixelSize(R.dimen._24sdp));
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.backgroundLight, typedValue, true);
        int color = typedValue.data;
        languagePopup.setCardBackgroundColor(color);
        languagePopup.setVisibility(View.GONE);

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

        LinearLayout languageOptions = new LinearLayout(this);
        languageOptions.setOrientation(LinearLayout.VERTICAL);
        languageOptions.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < languageCodes.length; i++) {
            RelativeLayout option = createLanguageOption(i);
            languageOptions.addView(option);
        }

        languagePopup.addView(languageOptions);

        ViewGroup rootLayout = findViewById(android.R.id.content);
        rootLayout.addView(languagePopup);

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

        ImageView flagIcon = new ImageView(this);
        flagIcon.setId(View.generateViewId());
        RelativeLayout.LayoutParams flagParams = new RelativeLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen._24sdp),
                getResources().getDimensionPixelSize(R.dimen._24sdp)
        );
        flagParams.addRule(RelativeLayout.CENTER_VERTICAL);
        flagIcon.setLayoutParams(flagParams);
        flagIcon.setImageResource(languageIcons[index]);

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

        final int langIndex = index;
        option.setOnClickListener(v -> {
            changeLanguage(languageCodes[langIndex]);
            toggleLanguagePopup();
        });

        return option;
    }

    private void toggleLanguagePopup() {
        isLanguagePopupShown = !isLanguagePopupShown;

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

                rootView.addView(dimBackground, 1);
            }

            languagePopup.bringToFront();

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
            int[] popupLocation = new int[2];
            languagePopup.getLocationOnScreen(popupLocation);
            int[] buttonLocation = new int[2];
            languageLayout.getLocationOnScreen(buttonLocation);

            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();

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
            LanguagePreferences languagePreferences = new LanguagePreferences(this);
            languagePreferences.setLanguage(languageCode);
            currentLanguage = languageCode;
            LocaleHelper.updateApplicationLocale(this, languageCode);
            recreate();
        }
    }

    private void updateLanguageUI() {
        int index = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                index = i;
                break;
            }
        }

        languageIcon.setImageResource(languageIcons[index]);
        languageText.setText(languageNames[index]);
    }

    private void setupThemeSwitch() {
        themeSwitch.setChecked(themePreferences.isDarkMode());

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            themePreferences.setThemeMode(newMode);
            applyTheme();
        });
    }

    private void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(themePreferences.getThemeMode());
        recreate();
    }

    private void navigateBackWithAnimation() {
        finish();
        overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
    }

    @Override
    public void onBackPressed() {
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
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start())
                .start();
    }
}