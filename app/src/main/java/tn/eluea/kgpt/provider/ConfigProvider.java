package tn.eluea.kgpt.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ContentProvider that serves as the single source of truth for all KGPT configuration.
 * Both the main app and Xposed module access data through this provider.
 * 
 * URI patterns:
 * - content://tn.eluea.kgpt.provider/config/{key} - Get/Set a single config value
 * - content://tn.eluea.kgpt.provider/config - Get all config values
 */
public class ConfigProvider extends ContentProvider {
    
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
    public boolean onCreate() {
        mPrefs = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return true;
    }
    
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, 
                        @Nullable String selection, @Nullable String[] selectionArgs, 
                        @Nullable String sortOrder) {
        
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_KEY, COLUMN_VALUE, COLUMN_TYPE});
        
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
            cursor.addRow(new Object[]{key, String.valueOf(value), type});
        }
    }
    
    private String getTypeString(Object value) {
        if (value instanceof String) return TYPE_STRING;
        if (value instanceof Integer) return TYPE_INT;
        if (value instanceof Boolean) return TYPE_BOOLEAN;
        if (value instanceof Long) return TYPE_LONG;
        if (value instanceof Float) return TYPE_FLOAT;
        return TYPE_STRING;
    }
    
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values == null) return null;
        
        String key = values.getAsString(COLUMN_KEY);
        String value = values.getAsString(COLUMN_VALUE);
        String type = values.getAsString(COLUMN_TYPE);
        
        if (key == null || value == null) return null;
        
        SharedPreferences.Editor editor = mPrefs.edit();
        
        if (type == null) type = TYPE_STRING;
        
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
        
        editor.apply();
        
        Uri resultUri = Uri.withAppendedPath(CONTENT_URI, key);
        getContext().getContentResolver().notifyChange(resultUri, null);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        
        return resultUri;
    }
    
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, 
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        // Use insert for updates as well
        insert(uri, values);
        return 1;
    }
    
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, 
                      @Nullable String[] selectionArgs) {
        if (sUriMatcher.match(uri) == CONFIG_KEY) {
            String key = uri.getLastPathSegment();
            mPrefs.edit().remove(key).apply();
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
