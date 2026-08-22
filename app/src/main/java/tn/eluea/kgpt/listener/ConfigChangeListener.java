/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.listener;

import android.os.Bundle;

import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;

public interface ConfigChangeListener {
    void onLanguageModelChange(LanguageModel model);

    void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value);

    void onCommandsChange(String commandsRaw);

    void onPatternsChange(String patternsRaw);

    void onOtherSettingsChange(Bundle otherSettings);

    default void onCustomProviderChange(tn.eluea.kgpt.llm.model.CustomProvider provider) {}
}
