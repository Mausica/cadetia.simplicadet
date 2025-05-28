package com.cadetia.simplicadet.dao;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;

public class LanguagePreferences {
    private static final String PREF_NAME = "language_preferences";
    private static final String KEY_LANGUAGE = "selected_language";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private SharedPreferences sharedPreferences;
    private Context context;

    public LanguagePreferences(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setLanguage(String languageCode) {
        sharedPreferences.edit()
                .putString(KEY_LANGUAGE, languageCode)
                .apply();
    }

    public String getLanguage() {
        String savedLanguage = sharedPreferences.getString(KEY_LANGUAGE, null);

        // If no language is saved, return system default
        if (savedLanguage == null || savedLanguage.isEmpty()) {
            return getSystemLanguage();
        }

        return savedLanguage;
    }

    private String getSystemLanguage() {
        String systemLang = Locale.getDefault().getLanguage();
        String systemCountry = Locale.getDefault().getCountry();

        // Return full locale code like "en_GB", "es_ES", etc.
        String fullLocale = systemLang + "_" + systemCountry;

        // Check if this matches any of your supported languages
        String[] supportedLanguages = {"en_GB", "es_ES", "fr_FR", "ro_RO"};

        for (String supportedLang : supportedLanguages) {
            if (supportedLang.equals(fullLocale)) {
                return supportedLang;
            }
        }

        // Check if just language code matches (without country)
        for (String supportedLang : supportedLanguages) {
            if (supportedLang.startsWith(systemLang + "_")) {
                return supportedLang;
            }
        }

        // Default to English if system language not supported
        return "en_GB";
    }

    public boolean isLanguageSet() {
        return sharedPreferences.contains(KEY_LANGUAGE) &&
                !sharedPreferences.getString(KEY_LANGUAGE, "").isEmpty();
    }

    public boolean isFirstLaunch() {
        return !sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, false);
    }

    public void setFirstLaunchComplete() {
        sharedPreferences.edit()
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .apply();
    }

    public void clearLanguagePreferences() {
        sharedPreferences.edit().clear().apply();
    }
}