package com.cadetia.simplicadet.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemePreferences {
    private static final String PREF_NAME = "theme_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_THEME_SET = "theme_set";

    private SharedPreferences sharedPreferences;
    private Context context;

    public ThemePreferences(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setThemeMode(int themeMode) {
        sharedPreferences.edit()
                .putInt(KEY_THEME_MODE, themeMode)
                .putBoolean(KEY_THEME_SET, true)
                .apply();
    }

    public int getThemeMode() {
        // If theme has never been set, return system default
        if (!isThemeSet()) {
            return getSystemDefaultTheme();
        }
        return sharedPreferences.getInt(KEY_THEME_MODE, getSystemDefaultTheme());
    }

    public boolean isDarkMode() {
        int themeMode = getThemeMode();

        switch (themeMode) {
            case AppCompatDelegate.MODE_NIGHT_YES:
                return true;
            case AppCompatDelegate.MODE_NIGHT_NO:
                return false;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                // Check system theme
                int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    public boolean isThemeSet() {
        return sharedPreferences.getBoolean(KEY_THEME_SET, false);
    }

    private int getSystemDefaultTheme() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case Configuration.UI_MODE_NIGHT_NO:
                return AppCompatDelegate.MODE_NIGHT_NO;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public void clearThemePreferences() {
        sharedPreferences.edit().clear().apply();
    }

    public void resetToSystemDefault() {
        int systemTheme = getSystemDefaultTheme();
        setThemeMode(systemTheme);
    }
}