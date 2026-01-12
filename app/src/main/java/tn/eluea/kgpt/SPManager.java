/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * This file is part of KGPT.
 * Based on original code from KeyboardGPT by Mino260806.
 * Original: https://github.com/Mino260806/KeyboardGPT
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt;

import android.content.Context;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

import tn.eluea.kgpt.instruction.command.Commands;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.listener.ConfigInfoProvider;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.text.parse.ParsePattern;

/**
 * Unified configuration manager that uses ContentProvider as single source of
 * truth.
 * Works in both KGPT app context and Xposed module (Gboard) context.
 */
public class SPManager implements ConfigInfoProvider {
    protected static final String PREF_MODULE_VERSION = "module_version";
    protected static final String PREF_LANGUAGE_MODEL = "language_model_v2";
    protected static final String PREF_GEN_AI_COMMANDS = "gen_ai_commands";
    protected static final String PREF_PARSE_PATTERNS = "parse_patterns";
    protected static final String PREF_OTHER_SETTING = "other_setting.%s";

    private final ConfigClient mClient;
    private List<GenerativeAICommand> generativeAICommands = List.of();
    private static SPManager instance = null;

    public static void init(Context context) {
        instance = new SPManager(context);
    }

    public static SPManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Missing call to SPManager.init(Context)");
        }
        return instance;
    }

    public static boolean isReady() {
        return instance != null;
    }

    private SPManager(Context context) {
        mClient = new ConfigClient(context);
        updateVersion();
        initializeDefaultCommands();
        initializeDefaultPatterns();
        updateGenerativeAICommands();
    }

    private void initializeDefaultCommands() {
        String existing = mClient.getString(PREF_GEN_AI_COMMANDS, null);
        if (existing == null || existing.equals("[]")) {
            setGenerativeAICommands(Commands.getDefaultCommands());
        }
    }

    private void initializeDefaultPatterns() {
        String existing = mClient.getString(PREF_PARSE_PATTERNS, null);
        if (existing == null) {
            // Initialize with default patterns
            setParsePatterns(ParsePattern.getDefaultPatterns());
        }
    }

    private void updateVersion() {
        int version = getVersion();
        if (version != BuildConfig.VERSION_CODE) {
            mClient.putInt(PREF_MODULE_VERSION, BuildConfig.VERSION_CODE);
        }
    }

    public int getVersion() {
        return mClient.getInt(PREF_MODULE_VERSION, -1);
    }

    public boolean hasLanguageModel() {
        return mClient.contains(PREF_LANGUAGE_MODEL);
    }

    @Override
    public LanguageModel getLanguageModel() {
        String languageModelName = mClient.getString(PREF_LANGUAGE_MODEL, null);
        if (languageModelName == null) {
            languageModelName = LanguageModel.Gemini.name();
        }
        return LanguageModel.valueOf(languageModelName);
    }

    public void setLanguageModel(LanguageModel model) {
        mClient.putString(PREF_LANGUAGE_MODEL, model.name());
    }

    public void setLanguageModelField(LanguageModel model, LanguageModelField field, String value) {
        if (model == null || field == null) {
            tn.eluea.kgpt.util.Logger.log("setLanguageModelField: model or field is null");
            return;
        }
        String entryName = String.format("%s." + field, model.name());
        mClient.putString(entryName, value);
    }

    public String getLanguageModelField(LanguageModel model, LanguageModelField field) {
        String entryName = String.format("%s." + field, model.name());
        return mClient.getString(entryName, model.getDefault(field));
    }

    public void setApiKey(LanguageModel model, String apiKey) {
        setLanguageModelField(model, LanguageModelField.ApiKey, apiKey);
    }

    public String getApiKey(LanguageModel model) {
        return getLanguageModelField(model, LanguageModelField.ApiKey);
    }

    public void setSubModel(LanguageModel model, String subModel) {
        setLanguageModelField(model, LanguageModelField.SubModel, subModel);
    }

    public String getSubModel(LanguageModel model) {
        return getLanguageModelField(model, LanguageModelField.SubModel);
    }

    public void setBaseUrl(LanguageModel model, String baseUrl) {
        setLanguageModelField(model, LanguageModelField.BaseUrl, baseUrl);
    }

    public String getBaseUrl(LanguageModel model) {
        return getLanguageModelField(model, LanguageModelField.BaseUrl);
    }

    public void setGenerativeAICommandsRaw(String commands) {
        mClient.putString(PREF_GEN_AI_COMMANDS, commands);
        updateGenerativeAICommands();
    }

    public String getGenerativeAICommandsRaw() {
        return mClient.getString(PREF_GEN_AI_COMMANDS, "[]");
    }

    public void setGenerativeAICommands(List<GenerativeAICommand> commands) {
        setGenerativeAICommandsRaw(Commands.encodeCommands(commands));
    }

    public List<GenerativeAICommand> getGenerativeAICommands() {
        // Always get fresh data
        updateGenerativeAICommands();
        return generativeAICommands;
    }

    public void setParsePatterns(List<ParsePattern> parsePatterns) {
        setParsePatternsRaw(ParsePattern.encode(parsePatterns));
    }

    public void setParsePatternsRaw(String patternsRaw) {
        mClient.putString(PREF_PARSE_PATTERNS, patternsRaw);
    }

    public List<ParsePattern> getParsePatterns() {
        return ParsePattern.decode(getParsePatternsRaw());
    }

    public String getParsePatternsRaw() {
        return mClient.getString(PREF_PARSE_PATTERNS, null);
    }

    private void updateGenerativeAICommands() {
        String raw = mClient.getString(PREF_GEN_AI_COMMANDS, "[]");
        generativeAICommands = Collections.unmodifiableList(Commands.decodeCommands(raw));
    }

    @Override
    public Bundle getConfigBundle() {
        Bundle bundle = new Bundle();
        for (LanguageModel model : LanguageModel.values()) {
            Bundle configBundle = new Bundle();
            for (LanguageModelField field : LanguageModelField.values()) {
                configBundle.putString(field.name, getLanguageModelField(model, field));
            }
            bundle.putBundle(model.name(), configBundle);
        }
        return bundle;
    }

    @Override
    public Bundle getOtherSettings() {
        Bundle otherSettings = new Bundle();
        for (OtherSettingsType type : OtherSettingsType.values()) {
            switch (type.nature) {
                case Boolean:
                    otherSettings.putBoolean(type.name(), (Boolean) getOtherSetting(type));
                    break;
                case String:
                    otherSettings.putString(type.name(), (String) getOtherSetting(type));
                    break;
                case Integer:
                    otherSettings.putInt(type.name(), (Integer) getOtherSetting(type));
                    break;
            }
        }
        return otherSettings;
    }

    public void setOtherSetting(OtherSettingsType type, Object value) {
        String key = String.format(PREF_OTHER_SETTING, type.name());
        switch (type.nature) {
            case Boolean:
                mClient.putBoolean(key, (Boolean) value);
                break;
            case String:
                mClient.putString(key, (String) value);
                break;
            case Integer:
                mClient.putInt(key, (Integer) value);
                break;
        }
    }

    public Object getOtherSetting(OtherSettingsType type) {
        String key = String.format(PREF_OTHER_SETTING, type.name());
        switch (type.nature) {
            case Boolean:
                return mClient.getBoolean(key, (Boolean) type.defaultValue);
            case String:
                return mClient.getString(key, (String) type.defaultValue);
            case Integer:
                return mClient.getInt(key, (Integer) type.defaultValue);
            default:
                return type.defaultValue;
        }
    }

    public Boolean getEnableLogs() {
        return (Boolean) getOtherSetting(OtherSettingsType.EnableLogs);
    }

    public Boolean getEnableExternalInternet() {
        return (Boolean) getOtherSetting(OtherSettingsType.EnableExternalInternet);
    }

    public void setSearchEngine(String searchEngine) {
        setOtherSetting(OtherSettingsType.SearchEngine, searchEngine);
    }

    public String getSearchEngine() {
        return (String) getOtherSetting(OtherSettingsType.SearchEngine);
    }

    public String getSearchUrl(String query) {
        String engine = getSearchEngine();
        return buildSearchUrl(engine, query);
    }

    // Update Settings
    public boolean getUpdateCheckEnabled() {
        return (Boolean) getOtherSetting(OtherSettingsType.UpdateCheckEnabled);
    }

    public void setUpdateCheckEnabled(boolean enabled) {
        setOtherSetting(OtherSettingsType.UpdateCheckEnabled, enabled);
    }

    public int getUpdateCheckInterval() {
        return (Integer) getOtherSetting(OtherSettingsType.UpdateCheckInterval);
    }

    public void setUpdateCheckInterval(int hours) {
        setOtherSetting(OtherSettingsType.UpdateCheckInterval, hours);
    }

    public String getUpdateDownloadPath() {
        return (String) getOtherSetting(OtherSettingsType.UpdateDownloadPath);
    }

    public void setUpdateDownloadPath(String path) {
        setOtherSetting(OtherSettingsType.UpdateDownloadPath, path);
    }

    public static String getSearchUrlFromKGPT(Context context, String query) {
        return buildSearchUrl("duckduckgo", query);
    }

    private static String buildSearchUrl(String engine, String query) {
        String encodedQuery;
        try {
            encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            encodedQuery = query;
        }

        switch (engine) {
            case "google":
                return "https://www.google.com/search?q=" + encodedQuery;
            case "bing":
                return "https://www.bing.com/search?q=" + encodedQuery;
            case "yahoo":
                return "https://search.yahoo.com/search?p=" + encodedQuery;
            case "yandex":
                return "https://yandex.com/search/?text=" + encodedQuery;
            case "brave":
                return "https://search.brave.com/search?q=" + encodedQuery;
            case "ecosia":
                return "https://www.ecosia.org/search?q=" + encodedQuery;
            case "qwant":
                return "https://www.qwant.com/?q=" + encodedQuery;
            case "startpage":
                return "https://www.startpage.com/do/dsearch?query=" + encodedQuery;
            case "perplexity":
                return "https://www.perplexity.ai/?q=" + encodedQuery;
            case "phind":
                return "https://www.phind.com/search?q=" + encodedQuery;
            case "duckduckgo":
            default:
                return "https://duckduckgo.com/?q=" + encodedQuery;
        }
    }

    /**
     * Register a listener for config changes
     */
    public void registerConfigChangeListener(ConfigClient.OnConfigChangeListener listener) {
        mClient.registerGlobalListener(listener);
    }

    /**
     * Get the underlying ConfigClient for advanced usage
     */
    public ConfigClient getConfigClient() {
        return mClient;
    }

    public boolean isAmoledTheme() {
        // Use raw key "amoled_mode" to match SettingsFragment implementation
        return mClient.getBoolean("amoled_mode", false);
    }
}
