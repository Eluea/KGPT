/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Helper class to get world-readable SharedPreferences for LSPosed XSharedPreferences.
 * 
 * According to LSPosed wiki (API 93+):
 * - Module must use Context.MODE_WORLD_READABLE when saving preferences
 * - Must use credential-protected storage (default), NOT device-protected
 * - XSharedPreferences in hooked app can then read these preferences
 */
public class WorldReadablePrefs {
    private static final String TAG = "KGPT_WorldReadable";
    public static final String PREF_NAME = "keyboard_gpt";
    
    private static SharedPreferences sPrefs = null;
    
    /**
     * Get world-readable SharedPreferences.
     * This is required for LSPosed's New XSharedPreferences to work.
     */
    @SuppressWarnings("deprecation")
    public static SharedPreferences getPrefs(Context context) {
        if (sPrefs != null) {
            return sPrefs;
        }

        try {
            // Use MODE_WORLD_READABLE - LSPosed hooks this to make it work
            sPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE);
            Log.d(TAG, "Created world-readable SharedPreferences");
        } catch (SecurityException e) {
            // Fallback if not running under LSPosed or feature not enabled
            Log.w(TAG, "MODE_WORLD_READABLE not available, using MODE_PRIVATE", e);
            sPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        return sPrefs;
    }

    // NOTE: Do NOT use "does MODE_WORLD_READABLE throw?" as an LSPosed-activation
    // heuristic. The framework's checkMode() only runs on the first access of a
    // prefs file per process; once any code opens the file with MODE_PRIVATE the
    // cached instance is returned and no SecurityException ever fires. Module
    // activation must be detected via XposedService binding instead.
}
