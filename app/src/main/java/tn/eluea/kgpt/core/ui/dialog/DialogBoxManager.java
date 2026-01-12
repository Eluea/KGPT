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
package tn.eluea.kgpt.core.ui.dialog;

import android.app.Activity;
import android.os.Bundle;

import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.box.ChoseModelDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.CommandEditDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.CommandListDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.ConfigureModelDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.DialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.OtherSettingsDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.PatternEditDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.PatternListDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.SettingsDialogBox;
import tn.eluea.kgpt.core.ui.dialog.box.WebSearchDialogBox;

public class DialogBoxManager {
    private final Activity mParent;
    private final Bundle mInputBundle;
    private final ConfigContainer mConfig;

    private DialogBox mCurrentDialogBox;

    public DialogBoxManager(Activity parent, Bundle inputBundle, ConfigContainer config) {
        mParent = parent;
        mInputBundle = inputBundle;
        mConfig = config;
    }

    public void showDialog(DialogType type) {
        DialogBox box = buildBox(type);
        mCurrentDialogBox = box;
        // Some dialog boxes (like WebSearch) launch separate activities and return null
        if (box.getDialog() != null) {
            box.getDialog().show();
        }
    }

    /**
     * Switch from current dialog to a new one.
     * Blur is managed by DialogActivity, so no flicker occurs.
     */
    public void switchDialog(DialogType type, android.app.Dialog currentDialog) {
        // Dismiss old dialog instantly
        if (currentDialog != null) {
            if (currentDialog instanceof tn.eluea.kgpt.ui.main.FloatingBottomSheet) {
                ((tn.eluea.kgpt.ui.main.FloatingBottomSheet) currentDialog).dismissInstant();
            } else {
                currentDialog.dismiss();
            }
        }
        
        // Build and show new dialog
        DialogBox box = buildBox(type);
        mCurrentDialogBox = box;
        
        if (box.getDialog() != null) {
            box.getDialog().show();
        }
    }

    private DialogBox buildBox(DialogType dialogType) {
        DialogBox box;
        switch (dialogType) {
            case ChoseModel:
                box = new ChoseModelDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case ConfigureModel:
                box = new ConfigureModelDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case EditCommandsList:
                box = new CommandListDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case EditCommand:
                box = new CommandEditDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case EditPatternList:
                box = new PatternListDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case EditPattern:
                box = new PatternEditDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case WebSearch:
                box = new WebSearchDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case OtherSettings:
                box = new OtherSettingsDialogBox(this, mParent, mInputBundle, mConfig);
                break;
            case Settings:
            default:
                box = new SettingsDialogBox(this, mParent, mInputBundle, mConfig);
                break;
        }
        return box;
    }
}
