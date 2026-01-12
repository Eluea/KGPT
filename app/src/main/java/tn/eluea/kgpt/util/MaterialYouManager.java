package tn.eluea.kgpt.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.material.color.DynamicColors;

public class MaterialYouManager {
    private static MaterialYouManager instance;
    private final SharedPreferences prefs;

    private static final String PREF_NAME = "material_you_prefs";
    private static final String KEY_USE_WALLPAPER = "use_wallpaper";
    private static final String KEY_SEED_COLOR = "seed_color";
    private static final String KEY_ENABLED = "enabled";

    private MaterialYouManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized MaterialYouManager getInstance(Context context) {
        if (instance == null) {
            instance = new MaterialYouManager(context.getApplicationContext());
        }
        return instance;
    }

    public Context wrapContext(Context baseContext) {
        Context context = baseContext;
        if (isMaterialYouEnabled()) {
            if (isUseWallpaperSource()) {
                context = DynamicColors.wrapContextIfAvailable(baseContext);
            } else {
                // Use custom seed color for context wrapping
                try {
                    com.google.android.material.color.DynamicColorsOptions options = new com.google.android.material.color.DynamicColorsOptions.Builder()
                            .setContentBasedSource(getSeedColor())
                            .build();
                    context = DynamicColors.wrapContextIfAvailable(baseContext, options);
                } catch (NoClassDefFoundError | NoSuchMethodError e) {
                    // Fallback for older versions
                    context = DynamicColors.wrapContextIfAvailable(baseContext);
                }
            }
        }

        // Apply AMOLED overlay if needed
        SharedPreferences appPrefs = baseContext.getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = appPrefs.getBoolean("amoled_mode", false);

        int uiMode = baseContext.getResources().getConfiguration().uiMode;
        boolean isNightModeEnv = (uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isNightModeEnv && isAmoled) {
            context = new android.view.ContextThemeWrapper(context, tn.eluea.kgpt.R.style.ThemeOverlay_KGPT_AMOLED);
        }

        return context;
    }

    public int getSeedColor() {
        return prefs.getInt(KEY_SEED_COLOR, 0xFF4285F4); // Default seed (Blue)
    }

    public void setSeedColor(int color) {
        prefs.edit().putInt(KEY_SEED_COLOR, color).apply();
    }

    public boolean isUseWallpaperSource() {
        return prefs.getBoolean(KEY_USE_WALLPAPER, true);
    }

    public void setUseWallpaperSource(boolean use) {
        prefs.edit().putBoolean(KEY_USE_WALLPAPER, use).apply();
    }

    public boolean isMaterialYouEnabled() {
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public void applyTheme(android.app.Activity activity) {
        if (isMaterialYouEnabled()) {
            if (isUseWallpaperSource()) {
                DynamicColors.applyToActivityIfAvailable(activity);
            } else {
                // For custom seed color, we normally use DynamicColorsOptions.
                // But to keep it simple and compile-safe without checking library version,
                // we'll primarily rely on the basic availability check or just apply it.
                // If custom seed logic is needed and library supports it:
                try {
                    com.google.android.material.color.DynamicColorsOptions options = new com.google.android.material.color.DynamicColorsOptions.Builder()
                            .setContentBasedSource(getSeedColor())
                            .build();
                    DynamicColors.applyToActivityIfAvailable(activity, options);
                } catch (NoClassDefFoundError | NoSuchMethodError e) {
                    // Fallback for older versions
                    DynamicColors.applyToActivityIfAvailable(activity);
                }
            }
        }

        // Apply AMOLED overlay if needed (Overrides background colors to black)
        // This is done AFTER DynamicColors so it takes precedence for background
        // surfaces
        checkAndApplyAmoledOverlay(activity);
    }

    private void checkAndApplyAmoledOverlay(android.app.Activity activity) {
        SharedPreferences appPrefs = activity.getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = appPrefs.getBoolean("amoled_mode", false);

        // Check if Night Mode is actually active in the resources configuration
        // This handles explicit preference AND system default cases correctly.
        int uiMode = activity.getResources().getConfiguration().uiMode;
        boolean isNightModeEnv = (uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        if (isNightModeEnv && isAmoled) {
            activity.getTheme().applyStyle(tn.eluea.kgpt.R.style.ThemeOverlay_KGPT_AMOLED, true);
        }
    }
}
