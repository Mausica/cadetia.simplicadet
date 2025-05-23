package com.cadetia.simplicadet.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import java.util.Locale;

public class LocaleHelper {
    private static final String PREF_NAME = "LanguagePrefs";
    private static final String KEY_LANGUAGE = "language_code";
    private static final String DEFAULT_LANGUAGE = "en_GB";
    private static final String TAG = "LocaleHelper";

    public static Context setLocale(Context context) {
        return updateResources(context, getLanguage(context));
    }

    public static Context setLocale(Context context, String language) {
        setLanguage(context, language);
        return updateResources(context, language);
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }

    private static void setLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    private static Context updateResources(Context context, String languageCode) {
        Log.d(TAG, "Processing language code: " + languageCode);

        // Parse language code properly
        String[] parts = languageCode.split("_");
        Locale locale;
        if (parts.length > 1) {
            locale = new Locale(parts[0], parts[1]);
        } else {
            locale = new Locale(languageCode);
        }

        Log.d(TAG, "Created locale: " + locale.toLanguageTag());
        Locale.setDefault(locale);

        // Create new configuration context without modifying original
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }

    // Simplified method for immediate locale updates - FIXED VERSION
    public static void updateApplicationLocale(Context context, String languageCode) {
        String[] parts = languageCode.split("_");
        Locale locale;
        if (parts.length > 1) {
            locale = new Locale(parts[0], parts[1]);
        } else {
            locale = new Locale(languageCode);
        }

        Locale.setDefault(locale);

        // Save the language preference first
        setLanguage(context, languageCode);

        // Update configuration - ONLY update the resources, don't recreate context
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    // Check if language has changed without applying it
    public static boolean hasLanguageChanged(Context context, String currentLanguage) {
        String savedLanguage = getLanguage(context);
        return !savedLanguage.equals(currentLanguage);
    }
}