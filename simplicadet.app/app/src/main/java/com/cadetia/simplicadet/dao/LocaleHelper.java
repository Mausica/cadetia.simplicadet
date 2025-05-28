package com.cadetia.simplicadet.dao;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {
    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    public static Context setLocale(Context context, String language) {
        persist(context, language);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        }

        return updateResourcesLegacy(context, language);
    }

    public static Context setLocale(Context context) {
        return setLocale(context, getLanguage(context));
    }

    public static String getLanguage(Context context) {
        LanguagePreferences languagePreferences = new LanguagePreferences(context);
        String savedLanguage = languagePreferences.getLanguage();

        if (savedLanguage == null || savedLanguage.isEmpty()) {
            // Return system default if no language is saved
            return getSystemLanguage();
        }

        return savedLanguage;
    }

    private static String getSystemLanguage() {
        String systemLang = Locale.getDefault().getLanguage();
        String systemCountry = Locale.getDefault().getCountry();

        // Return full locale code like "en_GB", "es_ES", etc.
        return systemLang + "_" + systemCountry;
    }

    private static void persist(Context context, String language) {
        LanguagePreferences languagePreferences = new LanguagePreferences(context);
        languagePreferences.setLanguage(language);
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = getLocaleFromString(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = getLocaleFromString(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }

    private static Locale getLocaleFromString(String language) {
        if (language == null || language.isEmpty()) {
            return Locale.getDefault();
        }

        String[] parts = language.split("_");
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(language);
        }
    }

    public static void updateApplicationLocale(Context context, String language) {
        Locale locale = getLocaleFromString(language);
        Locale.setDefault(locale);

        Resources resources = context.getApplicationContext().getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Also persist the language
        persist(context, language);
    }
}