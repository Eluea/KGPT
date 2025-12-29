package tn.eluea.kgpt.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for accessing ConfigProvider from any context (KGPT app or Xposed module).
 * Provides a simple key-value interface with caching and change notifications.
 */
public class ConfigClient {
    
    private final ContentResolver mResolver;
    private final Map<String, Object> mCache = new HashMap<>();
    private final Map<String, OnConfigChangeListener> mListeners = new HashMap<>();
    private ContentObserver mObserver;
    
    public interface OnConfigChangeListener {
        void onConfigChanged(String key, Object newValue);
    }
    
    public ConfigClient(Context context) {
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
                        // Invalidate cache for this key
                        mCache.remove(key);
                        // Notify listeners
                        Object newValue = getString(key, null);
                        OnConfigChangeListener listener = mListeners.get(key);
                        if (listener != null) {
                            listener.onConfigChanged(key, newValue);
                        }
                        // Also notify global listener
                        OnConfigChangeListener globalListener = mListeners.get("*");
                        if (globalListener != null) {
                            globalListener.onConfigChanged(key, newValue);
                        }
                    } else {
                        // Clear all cache on bulk change
                        mCache.clear();
                    }
                }
            }
        };
        
        try {
            mResolver.registerContentObserver(ConfigProvider.CONTENT_URI, true, mObserver);
        } catch (Exception e) {
            // Provider might not be available yet
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
    
    // String operations
    public String getString(String key, String defaultValue) {
        if (mCache.containsKey(key)) {
            Object cached = mCache.get(key);
            return cached != null ? cached.toString() : defaultValue;
        }
        
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
            // Provider not available, return default
        }
        return defaultValue;
    }
    
    public void putString(String key, String value) {
        mCache.put(key, value);
        
        ContentValues cv = new ContentValues();
        cv.put(ConfigProvider.COLUMN_KEY, key);
        cv.put(ConfigProvider.COLUMN_VALUE, value);
        cv.put(ConfigProvider.COLUMN_TYPE, ConfigProvider.TYPE_STRING);
        
        try {
            mResolver.insert(ConfigProvider.CONTENT_URI, cv);
        } catch (Exception e) {
            // Provider not available
        }
    }
    
    // Boolean operations
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
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
            // Provider not available
        }
    }
    
    // Int operations
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) return defaultValue;
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
            // Provider not available
        }
    }
    
    // Check if key exists
    public boolean contains(String key) {
        return getString(key, null) != null;
    }
    
    // Clear cache
    public void clearCache() {
        mCache.clear();
    }
    
    // Cleanup
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
