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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ContentProvider for KGPT configuration.
 * 
 * IMPORTANT: Uses MODE_WORLD_READABLE for LSPosed's New XSharedPreferences (API
 * 93+).
 * This allows the Xposed module running in keyboard process to read our
 * preferences.
 * 
 * URI patterns:
 * - content://tn.eluea.kgpt.provider/config/{key} - Get/Set a single config
 * value
 * - content://tn.eluea.kgpt.provider/config - Get all config values
 */
public class ConfigProvider extends ContentProvider {

    private static final String TAG = "KGPT_ConfigProvider";

    public static final String AUTHORITY = "tn.eluea.kgpt.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/config");

    private static final String PREF_NAME = "keyboard_gpt";

    private static final int CONFIG_ALL = 1;
    private static final int CONFIG_KEY = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "config", CONFIG_ALL);
        sUriMatcher.addURI(AUTHORITY, "config/*", CONFIG_KEY);
    }

    // Column names
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";
    public static final String COLUMN_TYPE = "type";

    // Type constants
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INT = "int";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_FLOAT = "float";

    private SharedPreferences mPrefs;

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "onCreate: context is null");
            return false;
        }

        // Check if device is unlocked before accessing credential-protected storage
        // This is important for directBootAware providers
        android.os.UserManager userManager = (android.os.UserManager) context.getSystemService(Context.USER_SERVICE);
        if (userManager != null && !userManager.isUserUnlocked()) {
            Log.w(TAG, "onCreate: User not unlocked yet, using device-protected storage");
            // Use device-protected storage for direct boot
            try {
                Context deviceContext = context.createDeviceProtectedStorageContext();
                mPrefs = deviceContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                Log.d(TAG, "onCreate: Using device-protected SharedPreferences");
            } catch (Exception e) {
                Log.e(TAG, "Failed to create device-protected storage", e);
                return false;
            }
        } else {
            // CRITICAL: Use MODE_WORLD_READABLE for LSPosed's New XSharedPreferences
            // LSPosed hooks ContextImpl.checkMode() to allow this on API 93+
            // Must use credential-protected storage (default), NOT device-protected
            try {
                mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_WORLD_READABLE);
                Log.d(TAG, "onCreate: Using MODE_WORLD_READABLE SharedPreferences");
            } catch (SecurityException e) {
                // Fallback for non-LSPosed environments
                Log.w(TAG, "MODE_WORLD_READABLE not available, using MODE_PRIVATE", e);
                mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            }
        }

        Log.d(TAG, "onCreate: SharedPreferences has " + mPrefs.getAll().size() + " entries");

        // Proactively fix permissions on startup
        fixFilePermissions();

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {

        MatrixCursor cursor = new MatrixCursor(new String[] { COLUMN_KEY, COLUMN_VALUE, COLUMN_TYPE });

        switch (sUriMatcher.match(uri)) {
            case CONFIG_KEY:
                String key = uri.getLastPathSegment();
                addRowForKey(cursor, key);
                break;

            case CONFIG_ALL:
                for (String k : mPrefs.getAll().keySet()) {
                    addRowForKey(cursor, k);
                }
                break;
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    private void addRowForKey(MatrixCursor cursor, String key) {
        Object value = mPrefs.getAll().get(key);
        if (value != null) {
            String type = getTypeString(value);
            cursor.addRow(new Object[] { key, String.valueOf(value), type });
        }
    }

    private String getTypeString(Object value) {
        if (value instanceof String)
            return TYPE_STRING;
        if (value instanceof Integer)
            return TYPE_INT;
        if (value instanceof Boolean)
            return TYPE_BOOLEAN;
        if (value instanceof Long)
            return TYPE_LONG;
        if (value instanceof Float)
            return TYPE_FLOAT;
        return TYPE_STRING;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values == null)
            return null;

        String key = values.getAsString(COLUMN_KEY);
        String value = values.getAsString(COLUMN_VALUE);
        String type = values.getAsString(COLUMN_TYPE);

        if (key == null || value == null)
            return null;

        SharedPreferences.Editor editor = mPrefs.edit();

        if (type == null)
            type = TYPE_STRING;

        switch (type) {
            case TYPE_INT:
                editor.putInt(key, Integer.parseInt(value));
                break;
            case TYPE_BOOLEAN:
                editor.putBoolean(key, Boolean.parseBoolean(value));
                break;
            case TYPE_LONG:
                editor.putLong(key, Long.parseLong(value));
                break;
            case TYPE_FLOAT:
                editor.putFloat(key, Float.parseFloat(value));
                break;
            case TYPE_STRING:
            default:
                editor.putString(key, value);
                break;
        }

        // CRITICAL: Use commit() instead of apply() to ensure data is written
        // immediately
        // This is essential for XSharedPreferences to pick up changes
        // LSPosed Wiki: The hooked app reads from the physical file
        boolean success = editor.commit();

        if (success) {
            fixFilePermissions();
        }

        Log.d(TAG, "insert: key=" + key + ", type=" + type + ", success=" + success);

        Uri resultUri = Uri.withAppendedPath(CONTENT_URI, key);

        // Notify observers about the change
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(resultUri, null);
            getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        }

        return resultUri;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void fixFilePermissions() {
        // Manually ensure the file is world-readable
        // This is a backup in case Context.MODE_WORLD_READABLE failed or wasn't enough
        try {
            if (getContext() != null) {
                java.io.File prefsDir = new java.io.File(getContext().getApplicationInfo().dataDir, "shared_prefs");
                java.io.File prefsFile = new java.io.File(prefsDir, PREF_NAME + ".xml");

                if (prefsFile.exists()) {
                    // Set rw-rw-r-- (664)
                    prefsFile.setReadable(true, false);
                    prefsFile.setWritable(true, true); // Owner only writable? No, keep standard

                    // Also ensure directory is executable/readable so we can reach the file
                    // But usually we don't mess with dir permissions unless needed.
                    // Shared_prefs dir usually needs x permission.
                    prefsDir.setExecutable(true, false);
                    prefsDir.setReadable(true, false);

                    Log.d(TAG, "Fixed file permissions for: " + prefsFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to fix file permissions", e);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
            @Nullable String selection, @Nullable String[] selectionArgs) {
        insert(uri, values);
        return 1;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        if (sUriMatcher.match(uri) == CONFIG_KEY) {
            String key = uri.getLastPathSegment();
            mPrefs.edit().remove(key).commit();
            getContext().getContentResolver().notifyChange(uri, null);
            return 1;
        }
        return 0;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case CONFIG_ALL:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".config";
            case CONFIG_KEY:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".config";
            default:
                return null;
        }
    }
}
