package com.cadetia.simplicadet.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.dao.LanguagePreferences;
import com.cadetia.simplicadet.dao.LocaleHelper;

import eightbitlab.com.blurview.BlurView;

public class LanguageSelector extends BaseActivity {

    private BlurView blurView;
    private View topBarBlurBackground;
    private ScrollView scrollView;
    private LinearLayout languageListContainer;
    private String currentLanguage;

    private final String[] languageCodes = {"en_GB", "es_ES", "fr_FR", "ro_RO"};
    private final String[] languageNames = {"English, UK", "Español", "Français", "Română"};
    private final String[] languageNativeNames = {"English, United Kingdom", "Spanish", "French", "Romanian"};
    private final String[] languageFlags = {"language_en.png", "language_es.png", "language_fr.png", "language_ro.png"};

    @Override
    protected void attachBaseContext(Context newBase) {
        LanguagePreferences langPrefs = new LanguagePreferences(newBase);
        String savedLanguage = langPrefs.getLanguage();

        if (savedLanguage == null || savedLanguage.isEmpty()) {
            savedLanguage = getSystemLanguage();
            langPrefs.setLanguage(savedLanguage);
        }

        super.attachBaseContext(LocaleHelper.setLocale(newBase, savedLanguage));
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
        setContentView(R.layout.activity_language_selector);

        currentLanguage = LocaleHelper.getLanguage(this);

        initializeViews();
        setupBlurView();
        setupScrollListener();
        setupLanguageList();
        setupBackButton();
    }

    private void initializeViews() {
        blurView = findViewById(R.id.blur_view_language);
        topBarBlurBackground = findViewById(R.id.language_blur_background);
        scrollView = findViewById(R.id.language_scroll_view);
        languageListContainer = findViewById(R.id.language_list_container);
    }

    private void setupBackButton() {
        findViewById(R.id.language_back_button).setOnClickListener(v -> {
            animateButtonOnClick(v);
            navigateBackWithAnimation();
        });
    }

    private void setupLanguageList() {
        languageListContainer.removeAllViews();

        for (int i = 0; i < languageCodes.length; i++) {
            View languageItem = createLanguageItem(i);
            languageListContainer.addView(languageItem);
        }
    }

    private View createLanguageItem(int index) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View item = inflater.inflate(R.layout.item_language, languageListContainer, false);

        int backgroundResource;
        if (languageCodes.length == 1) {
            backgroundResource = R.drawable.settings_item_top;
        } else if (index == 0) {
            backgroundResource = R.drawable.settings_item_top;
        } else if (index == languageCodes.length - 1) {
            backgroundResource = R.drawable.settings_item_bottom;
        } else {
            backgroundResource = R.drawable.settings_item_middle;
        }

        item.setBackgroundResource(backgroundResource);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (index < languageCodes.length - 1) {
            params.setMargins(0, 0, 0, 3);
        }
        item.setLayoutParams(params);

        ImageView flagIcon = item.findViewById(R.id.flag_icon);
        TextView primaryName = item.findViewById(R.id.primary_name);
        TextView secondaryName = item.findViewById(R.id.secondary_name);
        ImageView selectionIndicator = item.findViewById(R.id.selection_indicator);

        int flagResId = getResources().getIdentifier(
                languageFlags[index].replace(".png", ""),
                "raw",
                getPackageName()
        );

        if (flagResId != 0) {
            try {
                flagIcon.setImageResource(flagResId);
            } catch (Exception e) {
                flagIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        primaryName.setText(languageNames[index]);
        secondaryName.setText(languageNativeNames[index]);

        boolean isSelected = languageCodes[index].equals(currentLanguage);
        selectionIndicator.setImageResource(isSelected ?
                R.drawable.button_radio_checked : R.drawable.button_radio_unchecked);

        final int langIndex = index;
        item.setOnClickListener(v -> {
            animateButtonOnClick(v);
            selectLanguage(languageCodes[langIndex]);
        });

        return item;
    }

    private void selectLanguage(String languageCode) {
        if (!languageCode.equals(currentLanguage)) {
            LanguagePreferences languagePreferences = new LanguagePreferences(this);
            languagePreferences.setLanguage(languageCode);

            LocaleHelper.updateApplicationLocale(this, languageCode);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("language_changed", true);
            resultIntent.putExtra("new_language", languageCode);
            setResult(RESULT_OK, resultIntent);

            finish();
            overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
        } else {
            navigateBackWithAnimation();
        }
    }

    private String getSystemLanguage() {
        String systemLang = java.util.Locale.getDefault().getLanguage();
        String systemCountry = java.util.Locale.getDefault().getCountry();
        String fullLocale = systemLang + "_" + systemCountry;

        for (String supportedLang : languageCodes) {
            if (supportedLang.equals(fullLocale)) {
                return supportedLang;
            }
        }

        for (String supportedLang : languageCodes) {
            if (supportedLang.startsWith(systemLang + "_")) {
                return supportedLang;
            }
        }

        return "en_GB";
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

    private void animateButtonOnClick(View button) {
        button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                .start();
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
}