/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 */
package tn.eluea.kgpt.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.features.textactions.domain.TextAction;

public class BackupManager {

    private static final String BACKUP_VERSION = "3";
    private static final String KEY_VERSION = "backup_version";
    private static final String KEY_COMMANDS = "commands";
    private static final String KEY_PATTERNS = "patterns";
    private static final String KEY_LANGUAGE_MODEL = "language_model";
    private static final String KEY_SUB_MODELS = "sub_models";
    private static final String KEY_THEME = "theme";
    private static final String KEY_AMOLED = "amoled";
    private static final String KEY_MATERIAL_YOU_ENABLED = "material_you_enabled";
    private static final String KEY_MATERIAL_YOU_USE_WALLPAPER = "material_you_use_wallpaper";
    private static final String KEY_MATERIAL_YOU_SEED_COLOR = "material_you_seed_color";
    private static final String KEY_MATERIAL_YOU_SINGLE_TONE = "material_you_single_tone";
    private static final String KEY_BLUR_ENABLED = "blur_enabled";
    private static final String KEY_BLUR_MATERIAL_YOU = "blur_material_you";
    private static final String KEY_BLUR_INTENSITY = "blur_intensity";
    private static final String KEY_BLUR_TINT_COLOR = "blur_tint_color";
    private static final String KEY_SEARCH_ENGINE = "search_engine";
    private static final String KEY_ENABLE_LOGS = "enable_logs";
    private static final String KEY_EXTERNAL_INTERNET = "external_internet";
    private static final String KEY_APP_TRIGGERS = "app_triggers";
    private static final String KEY_APP_TRIGGERS_ENABLED = "app_triggers_enabled";
    private static final String KEY_TEXT_ACTIONS_ENABLED = "text_actions_enabled";
    private static final String KEY_TEXT_ACTIONS_LIST = "text_actions_list";
    private static final String KEY_TEXT_ACTIONS_SHOW_LABELS = "text_actions_show_labels";
    private static final String KEY_TEXT_ACTION_PROMPTS = "text_action_prompts";
    private static final String KEY_CUSTOM_TEXT_ACTIONS = "custom_text_actions";
    private static final String KEY_INCLUDED_SECTIONS = "included_sections";
    private static final String KEY_SENSITIVE_DATA = "sensitive_data";

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

    public String createBackup() throws JSONException {
        return createBackup(new BackupOptions());
    }

    public String createBackup(BackupOptions options) throws JSONException {
        JSONObject backup = new JSONObject();
        backup.put(KEY_VERSION, BACKUP_VERSION);
        JSONArray includedSections = new JSONArray();

        if (options.isSelected(BackupOptions.Option.COMMANDS)) {
            backup.put(KEY_COMMANDS, spManager.getGenerativeAICommandsRaw());
            includedSections.put(BackupOptions.Option.COMMANDS.key);
        }

        if (options.isSelected(BackupOptions.Option.PATTERNS)) {
            String patternsRaw = spManager.getParsePatternsRaw();
            if (patternsRaw != null)
                backup.put(KEY_PATTERNS, patternsRaw);
            includedSections.put(BackupOptions.Option.PATTERNS.key);
        }

        if (options.isSelected(BackupOptions.Option.LANGUAGE_MODEL)) {
            backup.put(KEY_LANGUAGE_MODEL, spManager.getLanguageModel().name());
            JSONObject subModels = new JSONObject();
            for (LanguageModel model : LanguageModel.values()) {
                String subModel = spManager.getSubModel(model);
                if (subModel != null && !subModel.isEmpty())
                    subModels.put(model.name(), subModel);
            }
            backup.put(KEY_SUB_MODELS, subModels);
            includedSections.put(BackupOptions.Option.LANGUAGE_MODEL.key);
        }

        if (options.isSelected(BackupOptions.Option.SENSITIVE_DATA)) {
            JSONObject sensitiveData = new JSONObject();
            for (LanguageModel model : LanguageModel.values()) {
                JSONObject modelConfig = new JSONObject();
                for (tn.eluea.kgpt.llm.LanguageModelField field : tn.eluea.kgpt.llm.LanguageModelField.values()) {
                    String val = spManager.getLanguageModelField(model, field);
                    if (val != null) {
                        modelConfig.put(field.name(), val);
                    }
                }
                if (modelConfig.length() > 0) {
                    sensitiveData.put(model.name(), modelConfig);
                }
            }
            backup.put(KEY_SENSITIVE_DATA, sensitiveData);
            includedSections.put(BackupOptions.Option.SENSITIVE_DATA.key);
        }

        if (options.isSelected(BackupOptions.Option.APPEARANCE)) {
            backup.put(KEY_THEME, uiPrefs.getBoolean("theme_mode", false));
            backup.put(KEY_AMOLED, uiPrefs.getBoolean("amoled_mode", false));
            backup.put(KEY_MATERIAL_YOU_ENABLED,
                    (Boolean) spManager.getOtherSetting(OtherSettingsType.MaterialYouEnabled));
            backup.put(KEY_MATERIAL_YOU_USE_WALLPAPER,
                    (Boolean) spManager.getOtherSetting(OtherSettingsType.MaterialYouUseWallpaper));
            backup.put(KEY_MATERIAL_YOU_SEED_COLOR,
                    (Integer) spManager.getOtherSetting(OtherSettingsType.MaterialYouSeedColor));
            backup.put(KEY_MATERIAL_YOU_SINGLE_TONE,
                    (Boolean) spManager.getOtherSetting(OtherSettingsType.MaterialYouSingleTone));
            includedSections.put(BackupOptions.Option.APPEARANCE.key);
        }

        if (options.isSelected(BackupOptions.Option.BLUR_SETTINGS)) {
            backup.put(KEY_BLUR_ENABLED, uiPrefs.getBoolean("blur_enabled", true));
            backup.put(KEY_BLUR_MATERIAL_YOU, uiPrefs.getBoolean("material_you_blur", false));
            backup.put(KEY_BLUR_INTENSITY, uiPrefs.getInt("blur_intensity", 25));
            backup.put(KEY_BLUR_TINT_COLOR, uiPrefs.getInt("blur_tint_color", 0));
            includedSections.put(BackupOptions.Option.BLUR_SETTINGS.key);
        }

        if (options.isSelected(BackupOptions.Option.GENERAL_SETTINGS)) {
            backup.put(KEY_SEARCH_ENGINE, spManager.getSearchEngine());
            backup.put(KEY_ENABLE_LOGS, spManager.getEnableLogs());
            backup.put(KEY_EXTERNAL_INTERNET, spManager.getEnableExternalInternet());
            includedSections.put(BackupOptions.Option.GENERAL_SETTINGS.key);
        }

        if (options.isSelected(BackupOptions.Option.APP_TRIGGERS)) {
            String appTriggersRaw = configClient.getString("app_triggers", null);
            if (appTriggersRaw != null)
                backup.put(KEY_APP_TRIGGERS, appTriggersRaw);
            backup.put(KEY_APP_TRIGGERS_ENABLED, configClient.getBoolean("app_triggers_enabled", false));
            includedSections.put(BackupOptions.Option.APP_TRIGGERS.key);
        }

        if (options.isSelected(BackupOptions.Option.TEXT_ACTIONS)) {
            backup.put(KEY_TEXT_ACTIONS_ENABLED, configClient.getBoolean("text_actions_enabled", false));
            String textActionsList = configClient.getString("text_actions_list", null);
            if (textActionsList != null)
                backup.put(KEY_TEXT_ACTIONS_LIST, textActionsList);
            backup.put(KEY_TEXT_ACTIONS_SHOW_LABELS, configClient.getBoolean("text_actions_show_labels", true));

            JSONObject actionPrompts = new JSONObject();
            SharedPreferences mainPrefs = context.getSharedPreferences("keyboard_gpt", Context.MODE_PRIVATE);
            for (TextAction action : TextAction.values()) {
                String key = "text_action_prompt_" + action.name();
                String prompt = mainPrefs.getString(key, null);
                if (prompt != null)
                    actionPrompts.put(action.name(), prompt);
            }
            if (actionPrompts.length() > 0)
                backup.put(KEY_TEXT_ACTION_PROMPTS, actionPrompts);

            String customActions = configClient.getString("custom_text_actions", null);
            if (customActions != null)
                backup.put(KEY_CUSTOM_TEXT_ACTIONS, customActions);
            includedSections.put(BackupOptions.Option.TEXT_ACTIONS.key);
        }

        backup.put(KEY_INCLUDED_SECTIONS, includedSections);
        return backup.toString(2);
    }

    public BackupAnalysis analyzeBackup(String backupJson) {
        try {
            JSONObject backup = new JSONObject(backupJson);
            Set<BackupOptions.Option> availableOptions = new HashSet<>();

            if (backup.has(KEY_INCLUDED_SECTIONS)) {
                JSONArray sections = backup.getJSONArray(KEY_INCLUDED_SECTIONS);
                for (int i = 0; i < sections.length(); i++) {
                    String sectionKey = sections.getString(i);
                    for (BackupOptions.Option option : BackupOptions.Option.values()) {
                        if (option.key.equals(sectionKey)) {
                            availableOptions.add(option);
                            break;
                        }
                    }
                }
            } else {
                // Legacy backup detection
                if (backup.has(KEY_COMMANDS))
                    availableOptions.add(BackupOptions.Option.COMMANDS);
                if (backup.has(KEY_PATTERNS))
                    availableOptions.add(BackupOptions.Option.PATTERNS);
                if (backup.has(KEY_LANGUAGE_MODEL))
                    availableOptions.add(BackupOptions.Option.LANGUAGE_MODEL);
                if (backup.has(KEY_THEME) || backup.has(KEY_AMOLED))
                    availableOptions.add(BackupOptions.Option.APPEARANCE);
                if (backup.has(KEY_BLUR_ENABLED))
                    availableOptions.add(BackupOptions.Option.BLUR_SETTINGS);
                if (backup.has(KEY_SEARCH_ENGINE) || backup.has(KEY_ENABLE_LOGS))
                    availableOptions.add(BackupOptions.Option.GENERAL_SETTINGS);
                if (backup.has(KEY_APP_TRIGGERS) || backup.has(KEY_APP_TRIGGERS_ENABLED))
                    availableOptions.add(BackupOptions.Option.APP_TRIGGERS);
                if (backup.has(KEY_TEXT_ACTIONS_ENABLED) || backup.has(KEY_TEXT_ACTIONS_LIST))
                    availableOptions.add(BackupOptions.Option.TEXT_ACTIONS);
                // Check for sensitive data in legacy backups
                if (backup.has(KEY_SENSITIVE_DATA))
                    availableOptions.add(BackupOptions.Option.SENSITIVE_DATA);
            }

            String version = backup.optString(KEY_VERSION, "1");
            return new BackupAnalysis(true, version, availableOptions, null);
        } catch (JSONException e) {
            return new BackupAnalysis(false, null, new HashSet<>(), "Invalid backup file: " + e.getMessage());
        }
    }

    public RestoreResult restoreBackup(String backupJson) {
        return restoreBackup(backupJson, new BackupOptions());
    }

    public RestoreResult restoreBackup(String backupJson, BackupOptions options) {
        try {
            JSONObject backup = new JSONObject(backupJson);
            int restoredCount = 0;
            List<String> restoredItems = new ArrayList<>();

            if (options.isSelected(BackupOptions.Option.COMMANDS) && backup.has(KEY_COMMANDS)) {
                spManager.setGenerativeAICommandsRaw(backup.getString(KEY_COMMANDS));
                restoredCount++;
                restoredItems.add("AI Commands");
            }

            if (options.isSelected(BackupOptions.Option.PATTERNS) && backup.has(KEY_PATTERNS)) {
                spManager.setParsePatternsRaw(backup.getString(KEY_PATTERNS));
                restoredCount++;
                restoredItems.add("Trigger Patterns");
            }

            if (options.isSelected(BackupOptions.Option.LANGUAGE_MODEL)) {
                if (backup.has(KEY_LANGUAGE_MODEL)) {
                    try {
                        LanguageModel model = LanguageModel.valueOf(backup.getString(KEY_LANGUAGE_MODEL));
                        spManager.setLanguageModel(model);
                        restoredCount++;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (backup.has(KEY_SUB_MODELS)) {
                    JSONObject subModels = backup.getJSONObject(KEY_SUB_MODELS);
                    for (LanguageModel model : LanguageModel.values()) {
                        if (subModels.has(model.name())) {
                            spManager.setSubModel(model, subModels.getString(model.name()));
                        }
                    }
                    restoredItems.add("AI Model Settings");
                }
            }

            if (options.isSelected(BackupOptions.Option.SENSITIVE_DATA) && backup.has(KEY_SENSITIVE_DATA)) {
                JSONObject sensitiveData = backup.getJSONObject(KEY_SENSITIVE_DATA);
                Iterator<String> modelKeys = sensitiveData.keys();
                while (modelKeys.hasNext()) {
                    String modelName = modelKeys.next();
                    try {
                        LanguageModel model = LanguageModel.valueOf(modelName);
                        JSONObject modelConfig = sensitiveData.getJSONObject(modelName);
                        Iterator<String> fieldKeys = modelConfig.keys();
                        while (fieldKeys.hasNext()) {
                            String fieldName = fieldKeys.next();
                            try {
                                tn.eluea.kgpt.llm.LanguageModelField field = tn.eluea.kgpt.llm.LanguageModelField
                                        .valueOf(fieldName);
                                String value = modelConfig.getString(fieldName);
                                spManager.setLanguageModelField(model, field, value);
                            } catch (IllegalArgumentException | JSONException ignored) {
                                // Skip invalid fields
                            }
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Skip invalid models
                    }
                }
                restoredCount++;
                restoredItems.add("API Keys/Configs");
            }

            if (options.isSelected(BackupOptions.Option.APPEARANCE)) {
                boolean restored = false;
                SharedPreferences.Editor editor = uiPrefs.edit();
                if (backup.has(KEY_THEME)) {
                    editor.putBoolean("theme_mode", backup.getBoolean(KEY_THEME));
                    restored = true;
                }
                if (backup.has(KEY_AMOLED)) {
                    editor.putBoolean("amoled_mode", backup.getBoolean(KEY_AMOLED));
                    restored = true;
                }
                editor.apply();
                if (backup.has(KEY_MATERIAL_YOU_ENABLED)) {
                    spManager.setOtherSetting(OtherSettingsType.MaterialYouEnabled,
                            backup.getBoolean(KEY_MATERIAL_YOU_ENABLED));
                    restored = true;
                }
                if (backup.has(KEY_MATERIAL_YOU_USE_WALLPAPER)) {
                    spManager.setOtherSetting(OtherSettingsType.MaterialYouUseWallpaper,
                            backup.getBoolean(KEY_MATERIAL_YOU_USE_WALLPAPER));
                    restored = true;
                }
                if (backup.has(KEY_MATERIAL_YOU_SEED_COLOR)) {
                    spManager.setOtherSetting(OtherSettingsType.MaterialYouSeedColor,
                            backup.getInt(KEY_MATERIAL_YOU_SEED_COLOR));
                    restored = true;
                }
                if (backup.has(KEY_MATERIAL_YOU_SINGLE_TONE)) {
                    spManager.setOtherSetting(OtherSettingsType.MaterialYouSingleTone,
                            backup.getBoolean(KEY_MATERIAL_YOU_SINGLE_TONE));
                    restored = true;
                }
                if (restored) {
                    restoredCount++;
                    restoredItems.add("Appearance");
                }
            }

            if (options.isSelected(BackupOptions.Option.BLUR_SETTINGS)) {
                boolean restored = false;
                SharedPreferences.Editor editor = uiPrefs.edit();
                if (backup.has(KEY_BLUR_ENABLED)) {
                    editor.putBoolean("blur_enabled", backup.getBoolean(KEY_BLUR_ENABLED));
                    restored = true;
                }
                if (backup.has(KEY_BLUR_MATERIAL_YOU)) {
                    editor.putBoolean("material_you_blur", backup.getBoolean(KEY_BLUR_MATERIAL_YOU));
                    restored = true;
                }
                if (backup.has(KEY_BLUR_INTENSITY)) {
                    editor.putInt("blur_intensity", backup.getInt(KEY_BLUR_INTENSITY));
                    restored = true;
                }
                if (backup.has(KEY_BLUR_TINT_COLOR)) {
                    editor.putInt("blur_tint_color", backup.getInt(KEY_BLUR_TINT_COLOR));
                    restored = true;
                }
                editor.apply();
                if (restored) {
                    restoredCount++;
                    restoredItems.add("Blur Settings");
                }
            }

            if (options.isSelected(BackupOptions.Option.GENERAL_SETTINGS)) {
                boolean restored = false;
                if (backup.has(KEY_SEARCH_ENGINE)) {
                    spManager.setSearchEngine(backup.getString(KEY_SEARCH_ENGINE));
                    restored = true;
                }
                if (backup.has(KEY_ENABLE_LOGS)) {
                    spManager.setOtherSetting(OtherSettingsType.EnableLogs, backup.getBoolean(KEY_ENABLE_LOGS));
                    restored = true;
                }
                if (backup.has(KEY_EXTERNAL_INTERNET)) {
                    spManager.setOtherSetting(OtherSettingsType.EnableExternalInternet,
                            backup.getBoolean(KEY_EXTERNAL_INTERNET));
                    restored = true;
                }
                if (restored) {
                    restoredCount++;
                    restoredItems.add("General Settings");
                }
            }

            if (options.isSelected(BackupOptions.Option.APP_TRIGGERS)) {
                boolean restored = false;
                if (backup.has(KEY_APP_TRIGGERS)) {
                    configClient.putString("app_triggers", backup.getString(KEY_APP_TRIGGERS));
                    restored = true;
                }
                if (backup.has(KEY_APP_TRIGGERS_ENABLED)) {
                    configClient.putBoolean("app_triggers_enabled", backup.getBoolean(KEY_APP_TRIGGERS_ENABLED));
                    restored = true;
                }
                if (restored) {
                    restoredCount++;
                    restoredItems.add("App Triggers");
                }
            }

            if (options.isSelected(BackupOptions.Option.TEXT_ACTIONS)) {
                boolean restored = false;
                if (backup.has(KEY_TEXT_ACTIONS_ENABLED)) {
                    configClient.putBoolean("text_actions_enabled", backup.getBoolean(KEY_TEXT_ACTIONS_ENABLED));
                    restored = true;
                }
                if (backup.has(KEY_TEXT_ACTIONS_LIST)) {
                    configClient.putString("text_actions_list", backup.getString(KEY_TEXT_ACTIONS_LIST));
                    restored = true;
                }
                if (backup.has(KEY_TEXT_ACTIONS_SHOW_LABELS)) {
                    configClient.putBoolean("text_actions_show_labels",
                            backup.getBoolean(KEY_TEXT_ACTIONS_SHOW_LABELS));
                    restored = true;
                }
                if (backup.has(KEY_TEXT_ACTION_PROMPTS)) {
                    JSONObject prompts = backup.getJSONObject(KEY_TEXT_ACTION_PROMPTS);
                    SharedPreferences mainPrefs = context.getSharedPreferences("keyboard_gpt", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = mainPrefs.edit();
                    Iterator<String> keys = prompts.keys();
                    while (keys.hasNext()) {
                        String actionName = keys.next();
                        editor.putString("text_action_prompt_" + actionName, prompts.getString(actionName));
                    }
                    editor.apply();
                    restored = true;
                }
                if (backup.has(KEY_CUSTOM_TEXT_ACTIONS)) {
                    configClient.putString("custom_text_actions", backup.getString(KEY_CUSTOM_TEXT_ACTIONS));
                    restored = true;
                }
                if (restored) {
                    restoredCount++;
                    restoredItems.add("Text Actions");
                }
            }

            return new RestoreResult(true, restoredCount, restoredItems, null);
        } catch (JSONException e) {
            return new RestoreResult(false, 0, new ArrayList<>(), "Invalid backup file: " + e.getMessage());
        }
    }

    public boolean saveToFile(Uri uri, String backupJson) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(backupJson.getBytes());
                outputStream.close();
                return true;
            }
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.log(e);
            return false;
        }
        return false;
    }

    public String readFromFile(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                inputStream.close();
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateBackupFilename() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        return "kgpt_backup_" + sdf.format(new Date()) + ".json";
    }

    public static class BackupAnalysis {
        public final boolean valid;
        public final String version;
        public final Set<BackupOptions.Option> availableOptions;
        public final String errorMessage;

        public BackupAnalysis(boolean valid, String version, Set<BackupOptions.Option> availableOptions,
                String errorMessage) {
            this.valid = valid;
            this.version = version;
            this.availableOptions = availableOptions;
            this.errorMessage = errorMessage;
        }
    }

    public static class RestoreResult {
        public final boolean success;
        public final int itemsRestored;
        public final List<String> restoredItems;
        public final String errorMessage;

        public RestoreResult(boolean success, int itemsRestored, List<String> restoredItems, String errorMessage) {
            this.success = success;
            this.itemsRestored = itemsRestored;
            this.restoredItems = restoredItems;
            this.errorMessage = errorMessage;
        }
    }
}
