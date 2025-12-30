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

import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Config reader for Xposed module context.
 * Uses LSPosed's New XSharedPreferences (API 93+) to read KGPT's SharedPreferences.
 * 
 * According to LSPosed Wiki:
 * - Module must use Context.MODE_WORLD_READABLE when saving preferences
 * - XSharedPreferences(packageName, prefFileName) must be used (NOT File constructor)
 * - reload() should be called to get fresh values
 * - getFile().canRead() should be checked before reading
 */
public class XposedConfigReader {
    private static final String TAG = "KGPT_XposedConfig";
    private static final String KGPT_PACKAGE = "tn.eluea.kgpt";
    private static final String PREF_NAME = "keyboard_gpt";
    
    private static XSharedPreferences xPrefs = null;
    private static boolean prefsAvailable = false;
    private static boolean initialized = false;
    private static Map<String, Object> cache = new HashMap<>();
    private static long lastReload = 0;
    private static final long RELOAD_INTERVAL = 1000;
    
    /**
     * Initialize XSharedPreferences
     */
    private static synchronized void initPrefs() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        try {
            // LSPosed Wiki: Use XSharedPreferences(String packageName, String prefFileName)
            xPrefs = new XSharedPreferences(KGPT_PACKAGE, PREF_NAME);
            
            // Check if we can read the file
            File file = xPrefs.getFile();
            
            if (file != null && file.canRead()) {
                prefsAvailable = true;
                Log.d(TAG, "XSharedPreferences initialized successfully");
                Log.d(TAG, "  Package: " + KGPT_PACKAGE);
                Log.d(TAG, "  PrefName: " + PREF_NAME);
                Log.d(TAG, "  File: " + file.getAbsolutePath());
                Log.d(TAG, "  Readable: true");
                
                // Force initial reload
                xPrefs.reload();
                Log.d(TAG, "  Initial reload: success");
            } else {
                prefsAvailable = false;
                Log.w(TAG, "XSharedPreferences file not readable!");
                Log.w(TAG, "  File path: " + (file != null ? file.getAbsolutePath() : "null"));
                Log.w(TAG, "  File exists: " + (file != null && file.exists()));
                Log.w(TAG, "  Make sure KGPT app has been opened at least once");
                Log.w(TAG, "  Make sure xposedsharedprefs meta-data is set in AndroidManifest.xml");
            }
        } catch (Throwable e) {
            Log.e(TAG, "Failed to init XSharedPreferences: " + e.getMessage(), e);
            prefsAvailable = false;
        }
    }
    
    /**
     * Reload preferences if needed
     */
    private static void reloadIfNeeded() {
        if (!prefsAvailable || xPrefs == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastReload > RELOAD_INTERVAL) {
            try {
                xPrefs.reload();
                
                // Re-check readability after reload
                File file = xPrefs.getFile();
                boolean wasAvailable = prefsAvailable;
                prefsAvailable = file != null && file.canRead();
                
                if (wasAvailable != prefsAvailable) {
                    Log.d(TAG, "Prefs availability changed: " + wasAvailable + " -> " + prefsAvailable);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to reload: " + e.getMessage());
            }
            cache.clear();
            lastReload = now;
        }
    }
    
    /**
     * Force reload preferences immediately
     */
    public static void forceReload() {
        if (xPrefs == null) {
            initPrefs();
            return;
        }
        
        try {
            xPrefs.reload();
            cache.clear();
            lastReload = System.currentTimeMillis();
            Log.d(TAG, "Force reload completed");
        } catch (Exception e) {
            Log.e(TAG, "Force reload failed: " + e.getMessage());
        }
    }

    /**
     * Get string value from XSharedPreferences
     */
    public static String getString(String key, String defaultValue) {
        initPrefs();
        
        if (!prefsAvailable || xPrefs == null) {
            return defaultValue;
        }
        
        reloadIfNeeded();
        
        if (cache.containsKey(key)) {
            Object value = cache.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        
        try {
            String value = xPrefs.getString(key, null);
            if (value != null) {
                cache.put(key, value);
                Log.d(TAG, "getString(" + key + ") = " + (value.length() > 50 ? value.substring(0, 50) + "..." : value));
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read string: " + key, e);
        }
        
        return defaultValue;
    }
    
    /**
     * Get boolean value from XSharedPreferences
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        initPrefs();
        
        if (!prefsAvailable || xPrefs == null) {
            return defaultValue;
        }
        
        reloadIfNeeded();
        
        if (cache.containsKey(key)) {
            Object value = cache.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        
        try {
            boolean value = xPrefs.getBoolean(key, defaultValue);
            cache.put(key, value);
            Log.d(TAG, "getBoolean(" + key + ") = " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read boolean: " + key, e);
        }
        
        return defaultValue;
    }
    
    /**
     * Get int value from XSharedPreferences
     */
    public static int getInt(String key, int defaultValue) {
        initPrefs();
        
        if (!prefsAvailable || xPrefs == null) {
            return defaultValue;
        }
        
        reloadIfNeeded();
        
        if (cache.containsKey(key)) {
            Object value = cache.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        
        try {
            int value = xPrefs.getInt(key, defaultValue);
            cache.put(key, value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read int: " + key, e);
        }
        
        return defaultValue;
    }
    
    /**
     * Check if XSharedPreferences is available and working
     * Synchronized to ensure thread safety with initPrefs()
     */
    public static synchronized boolean isAvailable() {
        initPrefs();
        return prefsAvailable;
    }
    
    /**
     * Get debug info about XSharedPreferences status
     */
    public static String getDebugInfo() {
        initPrefs();
        
        StringBuilder sb = new StringBuilder();
        sb.append("XSharedPreferences Debug Info:\n");
        sb.append("  Package: ").append(KGPT_PACKAGE).append("\n");
        sb.append("  Pref Name: ").append(PREF_NAME).append("\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  Available: ").append(prefsAvailable).append("\n");
        
        if (xPrefs != null) {
            try {
                File file = xPrefs.getFile();
                sb.append("  File: ").append(file.getAbsolutePath()).append("\n");
                sb.append("  File exists: ").append(file.exists()).append("\n");
                sb.append("  File readable: ").append(file.canRead()).append("\n");
            } catch (Exception e) {
                sb.append("  Error getting file info: ").append(e.getMessage()).append("\n");
            }
        }
        return sb.toString();
    }
    
    /**
     * Clear cache to force reload
     */
    public static void clearCache() {
        cache.clear();
        lastReload = 0;
    }
}
