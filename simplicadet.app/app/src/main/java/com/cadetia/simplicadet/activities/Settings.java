package com.cadetia.simplicadet.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.dao.LanguagePreferences;
import com.cadetia.simplicadet.dao.LocaleHelper;
import com.cadetia.simplicadet.dao.ThemePreferences;
import com.cadetia.simplicadet.entities.DialogConfirm;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

import eightbitlab.com.blurview.BlurView;

public class Settings extends BaseActivity {

    private BlurView blurView;
    private View topBarBlurBackground;
    private ScrollView scrollView;
    private ThemePreferences themePreferences;
    private Switch themeSwitch;
    private TextView settingsName, settingsEmail, charName, charDescription;
    private ImageView settingsImage, languageIcon, languageArrow;
    private RelativeLayout languageLayout, clearButton;
    private TextView languageText;
    private Button logoutButton;
    private EditText editDescription;
    private final int MAX_NAME_LENGTH = 60;
    private final int MAX_DESC_LENGTH = 90;
    private static final int LANGUAGE_SELECTOR_REQUEST = 1001;

    private final String[] languageCodes = {"en_GB", "es_ES", "fr_FR", "ro_RO"};
    private final String[] languageNames = {"English", "Español", "Français", "Română"};
    private final int[] languageIcons = {R.raw.language_en, R.raw.language_es, R.raw.language_fr, R.raw.language_ro};
    private String currentLanguage = "en_GB";

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

        themePreferences = new ThemePreferences(this);
        if (!themePreferences.isThemeSet()) {
            int systemTheme = getSystemDefaultTheme();
            themePreferences.setThemeMode(systemTheme);
            AppCompatDelegate.setDefaultNightMode(systemTheme);
        } else {
            AppCompatDelegate.setDefaultNightMode(themePreferences.getThemeMode());
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        currentLanguage = LocaleHelper.getLanguage(this);

        initializeViews();
        setupThemeSwitch();
        updateLanguageUI();
        setupEventListeners();
        retrieveUserData();
        setupBlurView();
        setupScrollListener();
        setupCharacterCounters();
    }

    private void initializeViews() {
        blurView = findViewById(R.id.blur_view);
        topBarBlurBackground = findViewById(R.id.top_bar_blur_background);
        scrollView = findViewById(R.id.settings_scroll_view);
        themeSwitch = findViewById(R.id.theme_switch);
        languageLayout = findViewById(R.id.settings_language);
        languageIcon = findViewById(R.id.language_icon);
        languageArrow = findViewById(R.id.language_arrow);
        languageText = findViewById(R.id.language_text);
        logoutButton = findViewById(R.id.logout_button);
        clearButton = findViewById(R.id.settings_clear);
        editDescription = findViewById(R.id.settings_about);
        charName = findViewById(R.id.char_name);
        charDescription = findViewById(R.id.char_description);
        settingsName = findViewById(R.id.settings_name);
        settingsEmail = findViewById(R.id.settings_email);
        settingsImage = findViewById(R.id.settings_image);
    }

    private void setupEventListeners() {
        logoutButton.setOnClickListener(v -> {
            animateButtonOnClick(logoutButton);
            handleLogout();
        });

        clearButton.setOnClickListener(v -> {
            animateButtonOnClick(clearButton);
            handleClearCache();
        });

        languageLayout.setOnClickListener(v -> {
            animateButtonOnClick(languageLayout);
            Intent intent = new Intent(this, LanguageSelector.class);
            startActivityForResult(intent, LANGUAGE_SELECTOR_REQUEST);
            overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
        });

        findViewById(R.id.back_button).setOnClickListener(v -> navigateBackWithAnimation());
    }

    private void handleLogout() {
        DialogConfirm.show(this, getString(R.string.logout), getString(R.string.logout_confirmation), () -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, true);
    }

    private void handleClearCache() {
        DialogConfirm.show(this, getString(R.string.clear_cache), getString(R.string.clear_cache_confirmation),
                this::clearAppCache, true);
    }

    private String getSystemLanguage() {
        String systemLang = Locale.getDefault().getLanguage();
        String systemCountry = Locale.getDefault().getCountry();
        String fullLocale = systemLang + "_" + systemCountry;

        for (String supportedLang : languageCodes) {
            if (supportedLang.equals(fullLocale)) return supportedLang;
        }
        for (String supportedLang : languageCodes) {
            if (supportedLang.startsWith(systemLang + "_")) return supportedLang;
        }
        return "en_GB";
    }

    private int getSystemDefaultTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES ? AppCompatDelegate.MODE_NIGHT_YES :
                nightModeFlags == Configuration.UI_MODE_NIGHT_NO ? AppCompatDelegate.MODE_NIGHT_NO :
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
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
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "");
        String userPhoto = sharedPreferences.getString("userPhoto", "");
        String userEmail = sharedPreferences.getString("userEmail", "");

        settingsName.setText(userName);
        settingsEmail.setText(userEmail);

        int nameLength = userName.length();
        charName.setText(nameLength + "/" + MAX_NAME_LENGTH);

        final int errorColor = ContextCompat.getColor(this, R.color.red);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.textLight, typedValue, true);
        final int textLightColor = typedValue.data;

        charName.setTextColor(nameLength > MAX_NAME_LENGTH ? errorColor : textLightColor);

        if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")) {
            Glide.with(this).load(R.raw.guest_civil).into(settingsImage);
        } else {
            Glide.with(this).load(userPhoto).into(settingsImage);
        }
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
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start())
                .start();
    }

    private void setupCharacterCounters() {
        final int errorColor = ContextCompat.getColor(this, R.color.red);
        final TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.textLight, typedValue, true);
        final int textLightColor = typedValue.data;

        editDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s.length();
                charDescription.setText(length + "/" + MAX_DESC_LENGTH);
                charDescription.setTextColor(length > MAX_DESC_LENGTH ? errorColor : textLightColor);
            }
        });
    }

    private void clearAppCache() {
        try {
            deleteDir(getCacheDir());
            if (getExternalCacheDir() != null) {
                deleteDir(getExternalCacheDir());
            }
            new Thread(() -> {
                Glide.get(this).clearDiskCache();
                runOnUiThread(() -> {});
            }).start();
            Glide.get(this).clearMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new java.io.File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LANGUAGE_SELECTOR_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean languageChanged = data.getBooleanExtra("language_changed", false);
            if (languageChanged) {
                currentLanguage = data.getStringExtra("new_language");
                Intent intent = getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                overridePendingTransition(0, 0);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        }
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