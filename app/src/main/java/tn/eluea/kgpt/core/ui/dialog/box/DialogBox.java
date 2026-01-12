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
package tn.eluea.kgpt.core.ui.dialog.box;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.instruction.command.Commands;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.ui.UiInteractor;

/**
 * Base class for all dialog boxes in the floating settings UI.
 * 
 * Now reads data from ContentProvider (single source of truth) instead of
 * Intent.
 */
public abstract class DialogBox {
    private final Activity mParent;
    private final Dialog mDialog;
    private final Bundle mInputBundle;
    private final ConfigContainer mConfigContainer;
    private final DialogBoxManager mManager;
    private final ConfigClient mClient;

    private boolean canClose = true;

    public DialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        mManager = dialogManager;
        mParent = parent;
        mInputBundle = inputBundle;
        mConfigContainer = configContainer;
        mClient = new ConfigClient(parent);
        mDialog = build();
        if (mDialog != null) {
            tn.eluea.kgpt.ui.main.BottomSheetHelper.applyBlur(mDialog);
            mDialog.setOnDismissListener(d -> {
                if (canClose) {
                    returnToKeyboard();
                } else {
                    canClose = true;
                }
            });
        }
    }

    protected abstract Dialog build();

    public Dialog getDialog() {
        return mDialog;
    }

    public ConfigContainer getConfig() {
        return mConfigContainer;
    }

    public Bundle getInput() {
        return mInputBundle;
    }

    public Context getContext() {
        return mParent;
    }

    public Activity getParent() {
        return mParent;
    }

    protected ConfigClient getClient() {
        return mClient;
    }

    public void switchToDialog(DialogType type) {
        canClose = false;
        mManager.switchDialog(type, mDialog);
    }

    protected void returnToKeyboard() {
        // Save to ContentProvider (single source of truth)
        getConfig().saveToProvider();

        // Also send broadcast for in-memory listeners (TextParser, CommandManager)
        Intent broadcastIntent = new Intent(UiInteractor.ACTION_DIALOG_RESULT);
        getConfig().fillIntent(broadcastIntent);
        getContext().sendBroadcast(broadcastIntent);

        getParent().finish();
    }

    protected void silentDismiss() {
        canClose = false;
        if (mDialog != null) {
            // Use dismissInstant if available to keep blur continuous
            if (mDialog instanceof tn.eluea.kgpt.ui.main.FloatingBottomSheet) {
                ((tn.eluea.kgpt.ui.main.FloatingBottomSheet) mDialog).dismissInstant();
            } else {
                mDialog.dismiss();
            }
        }
    }

    /**
     * Load commands from ContentProvider (single source of truth)
     */
    protected void safeguardCommands() {
        if (getConfig().commands == null) {
            // Read from ContentProvider
            String commandsRaw = mClient.getString("gen_ai_commands", "[]");
            getConfig().commands = Commands.decodeCommands(commandsRaw);
        }
    }

    /**
     * Load patterns from ContentProvider (single source of truth)
     */
    protected void safeguardPatterns() {
        if (getConfig().patterns == null) {
            // Read from ContentProvider
            String patternsRaw = mClient.getString("parse_patterns", null);
            getConfig().patterns = ParsePattern.decode(patternsRaw);
        }
    }

    /**
     * Load model data from ContentProvider (single source of truth)
     */
    protected void safeguardModelData() {
        if (getConfig().selectedModel == null) {
            String modelName = mClient.getString("language_model_v2", LanguageModel.Gemini.name());
            getConfig().selectedModel = LanguageModel.valueOf(modelName);
        }
        if (getConfig().languageModelsConfig == null) {
            // Build config bundle from ContentProvider
            Bundle bundle = new Bundle();
            for (LanguageModel model : LanguageModel.values()) {
                Bundle modelBundle = new Bundle();
                for (tn.eluea.kgpt.llm.LanguageModelField field : tn.eluea.kgpt.llm.LanguageModelField.values()) {
                    String key = String.format("%s." + field, model.name());
                    String value = mClient.getString(key, model.getDefault(field));
                    modelBundle.putString(field.name, value);
                }
                bundle.putBundle(model.name(), modelBundle);
            }
            getConfig().languageModelsConfig = bundle;
        }
    }

    /**
     * Load other settings from ContentProvider
     */
    protected Bundle loadOtherSettings() {
        Bundle otherSettings = new Bundle();
        for (tn.eluea.kgpt.settings.OtherSettingsType type : tn.eluea.kgpt.settings.OtherSettingsType.values()) {
            String key = String.format("other_setting.%s", type.name());
            switch (type.nature) {
                case Boolean:
                    otherSettings.putBoolean(type.name(), mClient.getBoolean(key, (Boolean) type.defaultValue));
                    break;
                case String:
                    otherSettings.putString(type.name(), mClient.getString(key, (String) type.defaultValue));
                    break;
            }
        }
        return otherSettings;
    }
}
