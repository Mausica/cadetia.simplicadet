package com.cadetia.simplicadet.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;
import android.widget.FrameLayout;

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
import com.google.firebase.auth.FirebaseUser;

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
    private RelativeLayout languageLayout, clearButton, privacyButton, termsButton, deleteButton, passwordButton, helpButton;
    private TextView languageText;
    private Button logoutButton;
    private EditText editDescription;
    private final int MAX_NAME_LENGTH = 60;
    private final int MAX_DESC_LENGTH = 90;
    private static final int LANGUAGE_SELECTOR_REQUEST = 1001;
    private FirebaseAuth mAuth;

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

        mAuth = FirebaseAuth.getInstance();

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
        privacyButton = findViewById(R.id.settings_privacy);
        termsButton = findViewById(R.id.settings_terms);
        deleteButton = findViewById(R.id.settings_delete);
        passwordButton = findViewById(R.id.settings_password);
        helpButton = findViewById(R.id.settings_help);
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

        privacyButton.setOnClickListener(v -> {
            animateButtonOnClick(privacyButton);
            openPrivacyPolicy();
        });

        termsButton.setOnClickListener(v -> {
            animateButtonOnClick(termsButton);
            openFeedbackForm();
        });

        deleteButton.setOnClickListener(v -> {
            animateButtonOnClick(deleteButton);
            handleAccountDeletion();
        });

        passwordButton.setOnClickListener(v -> {
            animateButtonOnClick(passwordButton);
            handlePasswordReset();
        });

        helpButton.setOnClickListener(v -> {
            animateButtonOnClick(helpButton);
            openHelpEmail();
        });

        languageLayout.setOnClickListener(v -> {
            animateButtonOnClick(languageLayout);
            Intent intent = new Intent(this, LanguageSelector.class);
            startActivityForResult(intent, LANGUAGE_SELECTOR_REQUEST);
            overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
        });

        findViewById(R.id.back_button).setOnClickListener(v -> navigateBackWithAnimation());
    }

    private void openHelpEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mausica.contact@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Help - SimpliCadet");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "");

            Intent chooser = Intent.createChooser(emailIntent, "Send Email");
            if (chooser.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                Toast.makeText(this, getString(R.string.error_no_email_app), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_sending_email), Toast.LENGTH_SHORT).show();
        }
    }

    private void openPrivacyPolicy() {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mausica.github.io/simplicadet.legal/"));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_opening_browser), Toast.LENGTH_SHORT).show();
        }
    }

    private void openFeedbackForm() {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/ykFkVshf5Hf3oEi29"));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_opening_browser), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAccountDeletion() {
        DialogConfirm.show(this, getString(R.string.delete_account), getString(R.string.delete_account_confirmation), () -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                currentUser.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, getString(R.string.account_deleted_success), Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                if (task.getException() != null && task.getException().getMessage() != null &&
                                        task.getException().getMessage().contains("requires-recent-login")) {
                                    Toast.makeText(this, getString(R.string.account_deletion_reauth_required), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, getString(R.string.error_deleting_account), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Toast.makeText(this, getString(R.string.error_not_logged_in), Toast.LENGTH_SHORT).show();
            }
        }, true);
    }

    private void handlePasswordReset() {
        DialogConfirm.show(this, getString(R.string.reset_password), getString(R.string.reset_password_confirmation), () -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userEmail = currentUser.getEmail();
                if (userEmail != null) {
                    mAuth.sendPasswordResetEmail(userEmail)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, getString(R.string.password_reset_email_sent), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, getString(R.string.error_sending_reset_email), Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(this, getString(R.string.error_no_email), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.error_not_logged_in), Toast.LENGTH_SHORT).show();
            }
        }, true);
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
            animateThemeMorph(newMode);
        });
    }

    private void animateThemeMorph(final int newMode) {
        final ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        final View overlay = new View(this);
        overlay.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
        overlay.setAlpha(0f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        );
        root.addView(overlay, params);

        overlay.animate()
                .alpha(1f)
                .setDuration(200)
                .withEndAction(() -> {
                    AppCompatDelegate.setDefaultNightMode(newMode);
                    new Handler().postDelayed(() -> {
                        overlay.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction(() -> root.removeView(overlay))
                                .start();
                    }, 150);
                })
                .start();
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
                overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
            }
        }
    }

    private void navigateBackWithAnimation() {
        Intent intent = new Intent(Settings.this, Home.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateBackWithAnimation();
    }
}