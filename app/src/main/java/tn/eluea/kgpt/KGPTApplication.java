package tn.eluea.kgpt;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.UserManager;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Application class for KGPT.
 * Handles global theme initialization to ensure consistent theming across all activities.
 */
public class KGPTApplication extends Application {
    
    private static final String PREF_NAME = "keyboard_gpt_ui";
    private static final String PREF_THEME = "theme_mode";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Apply theme globally before any activity is created
        applyGlobalTheme();
    }
    
    /**
     * Apply the saved theme preference globally.
     * This ensures all activities use the correct theme mode.
     */
    private void applyGlobalTheme() {
        // Check if user is unlocked (device not locked)
        // SharedPreferences are not available before unlock
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);
        if (userManager != null && !userManager.isUserUnlocked()) {
            // Device is locked, use default light theme
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    /**
     * Static method to apply theme from any context.
     * Call this when theme preference changes.
     */
    public static void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
