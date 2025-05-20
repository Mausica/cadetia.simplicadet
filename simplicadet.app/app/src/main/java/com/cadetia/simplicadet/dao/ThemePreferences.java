package com.cadetia.simplicadet.dao;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemePreferences {
    private static final String PREF_NAME = "ThemePrefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private final SharedPreferences prefs;

    public ThemePreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public boolean isDarkMode() {
        return getThemeMode() == AppCompatDelegate.MODE_NIGHT_YES;
    }
}
