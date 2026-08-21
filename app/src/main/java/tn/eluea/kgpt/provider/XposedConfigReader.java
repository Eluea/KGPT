/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.provider;

import android.util.Log;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Modern Config Reader with dynamic XSharedPreferences reflection fallback.
 */
public class XposedConfigReader {
    private static final String TAG = "KGPT_XposedConfig";
    private static final String KGPT_PACKAGE = "tn.eluea.kgpt";
    private static final String PREF_NAME = "keyboard_gpt";
    
    private static Object xPrefs = null;
    private static boolean prefsAvailable = false;
    private static boolean initialized = false;
    private static final Map<String, Object> cache = new HashMap<>();
    private static long lastReload = 0;
    private static final long RELOAD_INTERVAL = 1000;

    private static Method reloadMethod;
    private static Method getStringMethod;
    private static Method getBooleanMethod;
    private static Method getIntMethod;
    private static Method getFileMethod;
    
    private static synchronized void initPrefs() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        try {
            Class<?> xPrefsClass = Class.forName("de.robv.android.xposed.XSharedPreferences");
            reloadMethod = xPrefsClass.getMethod("reload");
            getStringMethod = xPrefsClass.getMethod("getString", String.class, String.class);
            getBooleanMethod = xPrefsClass.getMethod("getBoolean", String.class, boolean.class);
            getIntMethod = xPrefsClass.getMethod("getInt", String.class, int.class);
            getFileMethod = xPrefsClass.getMethod("getFile");

            xPrefs = xPrefsClass.getConstructor(String.class, String.class).newInstance(KGPT_PACKAGE, PREF_NAME);
            
            File file = (File) getFileMethod.invoke(xPrefs);
            if (file != null && file.canRead()) {
                prefsAvailable = true;
                reloadMethod.invoke(xPrefs);
                Log.d(TAG, "XSharedPreferences initialized successfully via reflection");
            } else {
                prefsAvailable = false;
            }
        } catch (Throwable e) {
            prefsAvailable = false;
            Log.d(TAG, "XSharedPreferences not available in this environment (using ConfigProvider IPC)");
        }
    }
    
    private static void reloadIfNeeded() {
        if (!prefsAvailable || xPrefs == null || reloadMethod == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastReload > RELOAD_INTERVAL) {
            try {
                reloadMethod.invoke(xPrefs);
                if (getFileMethod != null) {
                    File file = (File) getFileMethod.invoke(xPrefs);
                    prefsAvailable = file != null && file.canRead();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to reload: " + e.getMessage());
            }
            cache.clear();
            lastReload = now;
        }
    }
    
    public static void forceReload() {
        if (xPrefs == null) {
            initPrefs();
            return;
        }
        try {
            if (reloadMethod != null) {
                reloadMethod.invoke(xPrefs);
            }
            cache.clear();
            lastReload = System.currentTimeMillis();
        } catch (Exception e) {
            Log.e(TAG, "Force reload failed: " + e.getMessage());
        }
    }

    public static String getString(String key, String defaultValue) {
        initPrefs();
        if (!prefsAvailable || xPrefs == null || getStringMethod == null) {
            return defaultValue;
        }
        reloadIfNeeded();
        if (cache.containsKey(key)) {
            Object value = cache.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        try {
            String value = (String) getStringMethod.invoke(xPrefs, key, null);
            if (value != null) {
                cache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read string: " + key, e);
        }
        return defaultValue;
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        initPrefs();
        if (!prefsAvailable || xPrefs == null || getBooleanMethod == null) {
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
            Boolean value = (Boolean) getBooleanMethod.invoke(xPrefs, key, defaultValue);
            if (value != null) {
                cache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read boolean: " + key, e);
        }
        return defaultValue;
    }
    
    public static int getInt(String key, int defaultValue) {
        initPrefs();
        if (!prefsAvailable || xPrefs == null || getIntMethod == null) {
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
            Integer value = (Integer) getIntMethod.invoke(xPrefs, key, defaultValue);
            if (value != null) {
                cache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read int: " + key, e);
        }
        return defaultValue;
    }
    
    public static synchronized boolean isAvailable() {
        initPrefs();
        return prefsAvailable;
    }

    public static String getDebugInfo() {
        initPrefs();
        return "XSharedPreferences available: " + prefsAvailable;
    }
    
    public static void clearCache() {
        cache.clear();
        lastReload = 0;
    }
}
