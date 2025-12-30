package tn.eluea.kgpt.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.text.parse.ParsePattern;

/**
 * Manages backup and restore of KGPT settings.
 * Does NOT backup API keys for security reasons.
 */
public class BackupManager {
    
    private static final String BACKUP_VERSION = "2"; // Updated version for App Triggers support
    private static final String KEY_VERSION = "backup_version";
    private static final String KEY_COMMANDS = "commands";
    private static final String KEY_PATTERNS = "patterns";
    private static final String KEY_SEARCH_ENGINE = "search_engine";
    private static final String KEY_THEME = "theme";
    private static final String KEY_AMOLED = "amoled";
    private static final String KEY_LANGUAGE_MODEL = "language_model";
    private static final String KEY_SUB_MODELS = "sub_models";
    private static final String KEY_ENABLE_LOGS = "enable_logs";
    private static final String KEY_EXTERNAL_INTERNET = "external_internet";
    
    // App Triggers keys
    private static final String KEY_APP_TRIGGERS = "app_triggers";
    private static final String KEY_APP_TRIGGERS_ENABLED = "app_triggers_enabled";
    
    private final Context context;
    private final SPManager spManager;
    private final SharedPreferences uiPrefs;
    private final ConfigClient configClient;
    
    public BackupManager(Context context) {
        this.context = context;
        this.spManager = SPManager.getInstance();
        this.uiPrefs = context.getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        this.configClient = new ConfigClient(context);
    }
    
    /**
     * Create a backup JSON containing all settings except API keys
     */
    public String createBackup() throws JSONException {
        JSONObject backup = new JSONObject();
        
        // Backup version
        backup.put(KEY_VERSION, BACKUP_VERSION);
        
        // Commands (raw JSON string)
        backup.put(KEY_COMMANDS, spManager.getGenerativeAICommandsRaw());
        
        // Patterns (raw JSON string)
        String patternsRaw = spManager.getParsePatternsRaw();
        if (patternsRaw != null) {
            backup.put(KEY_PATTERNS, patternsRaw);
        }
        
        // Search engine
        backup.put(KEY_SEARCH_ENGINE, spManager.getSearchEngine());
        
        // Theme settings
        backup.put(KEY_THEME, uiPrefs.getBoolean("theme_mode", false));
        backup.put(KEY_AMOLED, uiPrefs.getBoolean("amoled_mode", false));
        
        // Current language model
        backup.put(KEY_LANGUAGE_MODEL, spManager.getLanguageModel().name());
        
        // Sub-models for each language model (NOT API keys)
        JSONObject subModels = new JSONObject();
        for (LanguageModel model : LanguageModel.values()) {
            String subModel = spManager.getSubModel(model);
            if (subModel != null && !subModel.isEmpty()) {
                subModels.put(model.name(), subModel);
            }
        }
        backup.put(KEY_SUB_MODELS, subModels);
        
        // Other settings
        backup.put(KEY_ENABLE_LOGS, spManager.getEnableLogs());
        backup.put(KEY_EXTERNAL_INTERNET, spManager.getEnableExternalInternet());
        
        // App Triggers (LAB feature)
        String appTriggersRaw = configClient.getString("app_triggers", null);
        if (appTriggersRaw != null) {
            backup.put(KEY_APP_TRIGGERS, appTriggersRaw);
        }
        backup.put(KEY_APP_TRIGGERS_ENABLED, configClient.getBoolean("app_triggers_enabled", false));
        
        return backup.toString(2); // Pretty print
    }
    
    /**
     * Restore settings from a backup JSON
     */
    public RestoreResult restoreBackup(String backupJson) {
        try {
            JSONObject backup = new JSONObject(backupJson);
            
            // Check version
            String version = backup.optString(KEY_VERSION, "0");
            if (!version.equals(BACKUP_VERSION)) {
                // Handle version migration if needed in future
            }
            
            int restoredCount = 0;
            
            // Restore commands
            if (backup.has(KEY_COMMANDS)) {
                spManager.setGenerativeAICommandsRaw(backup.getString(KEY_COMMANDS));
                restoredCount++;
            }
            
            // Restore patterns
            if (backup.has(KEY_PATTERNS)) {
                spManager.setParsePatternsRaw(backup.getString(KEY_PATTERNS));
                restoredCount++;
            }
            
            // Restore search engine
            if (backup.has(KEY_SEARCH_ENGINE)) {
                spManager.setSearchEngine(backup.getString(KEY_SEARCH_ENGINE));
                restoredCount++;
            }
            
            // Restore theme settings
            if (backup.has(KEY_THEME)) {
                uiPrefs.edit().putBoolean("theme_mode", backup.getBoolean(KEY_THEME)).apply();
                restoredCount++;
            }
            
            if (backup.has(KEY_AMOLED)) {
                uiPrefs.edit().putBoolean("amoled_mode", backup.getBoolean(KEY_AMOLED)).apply();
                restoredCount++;
            }
            
            // Restore language model
            if (backup.has(KEY_LANGUAGE_MODEL)) {
                try {
                    LanguageModel model = LanguageModel.valueOf(backup.getString(KEY_LANGUAGE_MODEL));
                    spManager.setLanguageModel(model);
                    restoredCount++;
                } catch (IllegalArgumentException e) {
                    // Invalid model name, skip
                }
            }
            
            // Restore sub-models
            if (backup.has(KEY_SUB_MODELS)) {
                JSONObject subModels = backup.getJSONObject(KEY_SUB_MODELS);
                for (LanguageModel model : LanguageModel.values()) {
                    if (subModels.has(model.name())) {
                        spManager.setSubModel(model, subModels.getString(model.name()));
                    }
                }
                restoredCount++;
            }
            
            // Restore other settings
            if (backup.has(KEY_ENABLE_LOGS)) {
                spManager.setOtherSetting(OtherSettingsType.EnableLogs, backup.getBoolean(KEY_ENABLE_LOGS));
                restoredCount++;
            }
            
            if (backup.has(KEY_EXTERNAL_INTERNET)) {
                spManager.setOtherSetting(OtherSettingsType.EnableExternalInternet, backup.getBoolean(KEY_EXTERNAL_INTERNET));
                restoredCount++;
            }
            
            // Restore App Triggers (LAB feature)
            if (backup.has(KEY_APP_TRIGGERS)) {
                configClient.putString("app_triggers", backup.getString(KEY_APP_TRIGGERS));
                restoredCount++;
            }
            
            if (backup.has(KEY_APP_TRIGGERS_ENABLED)) {
                configClient.putBoolean("app_triggers_enabled", backup.getBoolean(KEY_APP_TRIGGERS_ENABLED));
                restoredCount++;
            }
            
            return new RestoreResult(true, restoredCount, null);
            
        } catch (JSONException e) {
            return new RestoreResult(false, 0, "Invalid backup file: " + e.getMessage());
        }
    }
    
    /**
     * Save backup to a file
     */
    public boolean saveToFile(Uri uri, String backupJson) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(backupJson.getBytes());
                outputStream.close();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Read backup from a file
     */
    public String readFromFile(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Generate a default backup filename
     */
    public static String generateBackupFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return "kgpt_backup_" + sdf.format(new Date()) + ".json";
    }
    
    /**
     * Result of a restore operation
     */
    public static class RestoreResult {
        public final boolean success;
        public final int itemsRestored;
        public final String errorMessage;
        
        public RestoreResult(boolean success, int itemsRestored, String errorMessage) {
            this.success = success;
            this.itemsRestored = itemsRestored;
            this.errorMessage = errorMessage;
        }
    }
}
