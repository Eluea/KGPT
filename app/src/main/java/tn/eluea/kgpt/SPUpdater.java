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

import android.os.Bundle;

import tn.eluea.kgpt.listener.ConfigChangeListener;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.ui.UiInteractor;

public class SPUpdater implements ConfigChangeListener {
    private final SPManager mSPManager;

    public SPUpdater() {
        UiInteractor.getInstance().registerConfigChangeListener(this);

        mSPManager = SPManager.getInstance();
    }

    @Override
    public void onLanguageModelChange(LanguageModel model) {
        mSPManager.setLanguageModel(model);
    }

    @Override
    public void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value) {
        mSPManager.setLanguageModelField(model, field, value);
    }

    @Override
    public void onCommandsChange(String commandsRaw) {
        mSPManager.setGenerativeAICommandsRaw(commandsRaw);
    }

    @Override
    public void onPatternsChange(String patternsRaw) {
        mSPManager.setParsePatternsRaw(patternsRaw);
    }

    @Override
    public void onOtherSettingsChange(Bundle otherSettings) {
        for (String key: otherSettings.keySet()) {
            OtherSettingsType type = OtherSettingsType.valueOf(key);
            Object value = otherSettings.get(key);
            MainHook.log("Updating key " + key + " with value " + value);
            mSPManager.setOtherSetting(type, value);
        }
    }
}
