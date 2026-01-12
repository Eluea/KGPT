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
package tn.eluea.kgpt.core.data;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;

import tn.eluea.kgpt.instruction.command.Commands;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.provider.ConfigProvider;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.ui.UiInteractor;

/**
 * Container for configuration changes made in the floating dialog.
 * 
 * Now saves directly to ContentProvider (single source of truth).
 * Both KGPT app and Xposed module read from the same ContentProvider.
 */
public class ConfigContainer {
    public Bundle languageModelsConfig;

    public LanguageModel selectedModel;

    public List<GenerativeAICommand> commands;
    public List<ParsePattern> patterns;

    public int focusCommandIndex = -1;

    public int focusPatternIndex = -1;

    public Bundle otherExtras = new Bundle();

    private ConfigClient mClient;

    /**
     * Initialize with context to access ContentProvider
     */
    public void initClient(Context context) {
        mClient = new ConfigClient(context);
    }

    /**
     * Save all changes to ContentProvider.
     * This is the single source of truth - both app and Xposed module read from
     * here.
     */
    public void saveToProvider() {
        if (mClient == null)
            return;

        // Save selected model
        if (selectedModel != null) {
            mClient.putString("language_model_v2", selectedModel.name());
        }

        // Save language model configurations
        if (languageModelsConfig != null) {
            for (LanguageModel model : LanguageModel.values()) {
                Bundle modelBundle = languageModelsConfig.getBundle(model.name());
                if (modelBundle != null) {
                    for (LanguageModelField field : LanguageModelField.values()) {
                        String value = modelBundle.getString(field.name);
                        if (value != null) {
                            String key = String.format("%s." + field, model.name());
                            mClient.putString(key, value);
                        }
                    }
                }
            }
        }

        // Save commands
        if (commands != null) {
            mClient.putString("gen_ai_commands", Commands.encodeCommands(commands));
        }

        // Save patterns
        if (patterns != null) {
            mClient.putString("parse_patterns", ParsePattern.encode(patterns));
        }

        // Save other settings
        if (!otherExtras.isEmpty()) {
            for (OtherSettingsType type : OtherSettingsType.values()) {
                if (otherExtras.containsKey(type.name())) {
                    String key = String.format("other_setting.%s", type.name());
                    switch (type.nature) {
                        case Boolean:
                            mClient.putBoolean(key, otherExtras.getBoolean(type.name()));
                            break;
                        case String:
                            mClient.putString(key, otherExtras.getString(type.name()));
                            break;
                    }
                }
            }
        }
    }

    /**
     * Fill the broadcast intent with all configuration changes.
     * Still used for notifying listeners about changes.
     */
    public void fillIntent(Intent intent) {
        if (selectedModel != null)
            intent.putExtra(UiInteractor.EXTRA_CONFIG_SELECTED_MODEL, selectedModel.name());
        if (languageModelsConfig != null)
            intent.putExtra(UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL, languageModelsConfig);
        if (commands != null)
            intent.putExtra(UiInteractor.EXTRA_COMMAND_LIST, Commands.encodeCommands(commands));
        if (patterns != null)
            intent.putExtra(UiInteractor.EXTRA_PATTERN_LIST, ParsePattern.encode(patterns));
        if (!otherExtras.isEmpty()) {
            intent.putExtra(UiInteractor.EXTRA_OTHER_SETTINGS, otherExtras);
        }

        // Save to ContentProvider (single source of truth)
        saveToProvider();
    }
}
