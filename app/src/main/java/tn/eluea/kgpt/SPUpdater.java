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
        if (model == null || field == null) {
            tn.eluea.kgpt.util.Logger.log("onLanguageModelFieldChange: model or field is null, ignoring");
            return;
        }
        try {
            mSPManager.setLanguageModelField(model, field, value);
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.log("Error in onLanguageModelFieldChange: " + e.getMessage());
        }
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
        for (String key : otherSettings.keySet()) {
            Object value = otherSettings.get(key);

            // Handle ProcessTextActivity enabling/disabling
            if ("text_actions_enabled".equals(key)) {
                updateProcessTextActivityState((Boolean) value);
                continue;
            }

            try {
                OtherSettingsType type = OtherSettingsType.valueOf(key);
                tn.eluea.kgpt.util.Logger.log("Updating key " + key + " with value " + value);
                mSPManager.setOtherSetting(type, value);
            } catch (IllegalArgumentException e) {
                // Ignore keys that are not part of OtherSettingsType enum
                tn.eluea.kgpt.util.Logger.log("Ignoring generic setting key: " + key);
            }
        }
    }

    private void updateProcessTextActivityState(boolean enabled) {
        try {
            android.content.Context context = tn.eluea.kgpt.KGPTApplication.getContext();
            if (context == null || !context.getPackageName().equals("tn.eluea.kgpt")) {
                // Only local app process can change its own component state
                return;
            }

            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.ComponentName componentName = new android.content.ComponentName(
                    context, tn.eluea.kgpt.features.textactions.ui.ProcessTextActivity.class);

            int state = enabled ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            // Reverting to DONT_KILL_APP to keep settings open, but we need to solve the
            // lag.
            // For now, let's try DONT_KILL_APP again with a different approach if needed.
            // User reported Flag 0 (Kill) didn't work as expected.
            pm.setComponentEnabledSetting(componentName, state, android.content.pm.PackageManager.DONT_KILL_APP);

            String status = enabled ? "ENABLED" : "DISABLED";
            tn.eluea.kgpt.util.Logger.log("ProcessTextActivity state changed to: " + status);
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.error("Failed to update ProcessTextActivity state: " + e.getMessage());
        }
    }
}
