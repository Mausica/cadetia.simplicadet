package com.cadetia.simplicadet.dao;

import android.content.Context;
import android.content.SharedPreferences;

public class LanguagePreferences {
    private static final String PREF_NAME = "LanguagePrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private final SharedPreferences preferences;
    private final Context context;

    public LanguagePreferences(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchComplete() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    public String getCurrentLanguage() {
        return LocaleHelper.getLanguage(context);
    }

    public void setLanguage(String languageCode) {
        LocaleHelper.setLocale(context, languageCode);
    }
}