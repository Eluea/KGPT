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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client for accessing ConfigProvider.
 * Uses ContentProvider for main app, XSharedPreferences for Xposed module.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class ConfigClient {

    private static final String TAG = "KGPT_ConfigClient";

    private final ContentResolver mResolver;
    private final Context mContext;
    // Thread-safe cache using ConcurrentHashMap
    private final Map<String, Object> mCache = new ConcurrentHashMap<>();
    private final Map<String, OnConfigChangeListener> mListeners = new ConcurrentHashMap<>();
    private ContentObserver mObserver;

    // Flag to check if we're in Xposed context (XSharedPreferences class is
    // available)
    private static final boolean IS_XPOSED_CONTEXT;
    static {
        boolean xposedAvailable = false;
        try {
            Class.forName("de.robv.android.xposed.XSharedPreferences");
            xposedAvailable = true;
        } catch (ClassNotFoundException e) {
            xposedAvailable = false;
        }
        IS_XPOSED_CONTEXT = xposedAvailable;
    }

    public interface OnConfigChangeListener {
        void onConfigChanged(String key, Object newValue);
    }

    public ConfigClient(Context context) {
        mContext = context;
        mResolver = context.getContentResolver();
        setupObserver();
    }

    private void setupObserver() {
        mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null) {
                    String key = uri.getLastPathSegment();
                    if (key != null && !key.equals("config")) {
                        mCache.remove(key);
                        Object newValue = getString(key, null);
                        OnConfigChangeListener listener = mListeners.get(key);
                        if (listener != null) {
                            listener.onConfigChanged(key, newValue);
                        }
                        OnConfigChangeListener globalListener = mListeners.get("*");
                        if (globalListener != null) {
                            globalListener.onConfigChanged(key, newValue);
                        }
                    } else {
                        mCache.clear();
                    }
                }
            }
        };

        try {
            mResolver.registerContentObserver(ConfigProvider.CONTENT_URI, true, mObserver);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register observer", e);
        }
    }

    public void registerListener(String key, OnConfigChangeListener listener) {
        mListeners.put(key, listener);
    }

    public void registerGlobalListener(OnConfigChangeListener listener) {
        mListeners.put("*", listener);
    }

    public void unregisterListener(String key) {
        mListeners.remove(key);
    }

    public String getString(String key, String defaultValue) {
        // FORCE REFRESH: By-pass cache check for Strings to ensure freshness via
        // ContentProvider
        // Cache is only checked as a fallback if provider query fails.

        // Always try ContentProvider first (Single Source of Truth)
        // This works in Xposed context because the provider is exported
        try {
            Uri uri = Uri.withAppendedPath(ConfigProvider.CONTENT_URI, key);
            Cursor cursor = mResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String value = cursor.getString(cursor.getColumnIndexOrThrow(ConfigProvider.COLUMN_VALUE));
                        mCache.put(key, value);
                        return value;
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            // Log.d(TAG, "Provider query failed for: " + key + " - " + e.getMessage());
        }

        // Fallback: In Xposed context, try XSharedPreferences
        if (IS_XPOSED_CONTEXT && XposedConfigReader.isAvailable()) {
            try {
                String value = XposedConfigReader.getString(key, null);
                if (value != null) {
                    mCache.put(key, value);
                    return value;
                }
            } catch (Exception e) {
                Log.d(TAG, "XSharedPreferences failed for: " + key + " - " + e.getMessage());
            }
        }

        return defaultValue;
    }

    public void putString(String key, String value) {
        if (key == null) {
            Log.w(TAG, "putString called with null key - ignoring");
            return;
        }

        try {
            if (value != null) {
                mCache.put(key, value);
            } else {
                mCache.remove(key);
            }
        } catch (Exception e) {
            Log.w(TAG, "Cache operation failed for key: " + key, e);
        }

        ContentValues cv = new ContentValues();
        cv.put(ConfigProvider.COLUMN_KEY, key);
        cv.put(ConfigProvider.COLUMN_VALUE, value);
        cv.put(ConfigProvider.COLUMN_TYPE, ConfigProvider.TYPE_STRING);

        try {
            mResolver.insert(ConfigProvider.CONTENT_URI, cv);
        } catch (Exception e) {
            Log.w(TAG, "Provider insert failed for: " + key, e);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        // Check cache first
        if (mCache.containsKey(key)) {
            Object cached = mCache.get(key);
            if (cached instanceof Boolean) {
                return (Boolean) cached;
            }
            try {
                return Boolean.parseBoolean(cached.toString());
            } catch (Exception e) {
                // Ignore
            }
        }

        // Always try ContentProvider first
        try {
            Uri uri = Uri.withAppendedPath(ConfigProvider.CONTENT_URI, key);
            Cursor cursor = mResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String value = cursor.getString(cursor.getColumnIndexOrThrow(ConfigProvider.COLUMN_VALUE));
                        if (value != null) {
                            boolean boolValue = Boolean.parseBoolean(value);
                            mCache.put(key, boolValue);
                            return boolValue;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            // Log.d(TAG, "Provider query failed for boolean: " + key);
        }

        // Fallback: In Xposed context, try XSharedPreferences
        if (IS_XPOSED_CONTEXT && XposedConfigReader.isAvailable()) {
            try {
                boolean value = XposedConfigReader.getBoolean(key, defaultValue);
                mCache.put(key, value);
                return value;
            } catch (Exception e) {
                Log.d(TAG, "XSharedPreferences getBoolean failed for: " + key + " - " + e.getMessage());
            }
        }

        return defaultValue;
    }

    public void putBoolean(String key, boolean value) {
        mCache.put(key, value);

        ContentValues cv = new ContentValues();
        cv.put(ConfigProvider.COLUMN_KEY, key);
        cv.put(ConfigProvider.COLUMN_VALUE, String.valueOf(value));
        cv.put(ConfigProvider.COLUMN_TYPE, ConfigProvider.TYPE_BOOLEAN);

        try {
            mResolver.insert(ConfigProvider.CONTENT_URI, cv);
        } catch (Exception e) {
            Log.w(TAG, "Provider insert failed for: " + key, e);
        }
    }

    public int getInt(String key, int defaultValue) {
        // Check cache first
        if (mCache.containsKey(key)) {
            Object cached = mCache.get(key);
            if (cached instanceof Number) {
                return ((Number) cached).intValue();
            }
            try {
                return Integer.parseInt(cached.toString());
            } catch (Exception e) {
                // Ignore
            }
        }

        // Always try ContentProvider first
        try {
            Uri uri = Uri.withAppendedPath(ConfigProvider.CONTENT_URI, key);
            Cursor cursor = mResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String value = cursor.getString(cursor.getColumnIndexOrThrow(ConfigProvider.COLUMN_VALUE));
                        if (value != null) {
                            int intValue = Integer.parseInt(value);
                            mCache.put(key, intValue);
                            return intValue;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            // Log.d(TAG, "Provider query failed for int: " + key);
        }

        // Fallback: In Xposed context, try XSharedPreferences
        if (IS_XPOSED_CONTEXT && XposedConfigReader.isAvailable()) {
            try {
                int value = XposedConfigReader.getInt(key, defaultValue);
                mCache.put(key, value);
                return value;
            } catch (Exception e) {
                Log.d(TAG, "XSharedPreferences getInt failed for: " + key + " - " + e.getMessage());
            }
        }

        String value = getString(key, null);
        if (value == null)
            return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void putInt(String key, int value) {
        mCache.put(key, value);

        ContentValues cv = new ContentValues();
        cv.put(ConfigProvider.COLUMN_KEY, key);
        cv.put(ConfigProvider.COLUMN_VALUE, String.valueOf(value));
        cv.put(ConfigProvider.COLUMN_TYPE, ConfigProvider.TYPE_INT);

        try {
            mResolver.insert(ConfigProvider.CONTENT_URI, cv);
        } catch (Exception e) {
            Log.w(TAG, "Provider insert failed for: " + key, e);
        }
    }

    public boolean contains(String key) {
        return getString(key, null) != null;
    }

    public void clearCache() {
        mCache.clear();
        // Only call XposedConfigReader if in Xposed context
        if (IS_XPOSED_CONTEXT) {
            try {
                XposedConfigReader.clearCache();
            } catch (Exception e) {
                // Ignore - not in Xposed context
            }
        }
    }

    public void destroy() {
        try {
            mResolver.unregisterContentObserver(mObserver);
        } catch (Exception e) {
            // Ignore
        }
        mListeners.clear();
        mCache.clear();
    }
}
