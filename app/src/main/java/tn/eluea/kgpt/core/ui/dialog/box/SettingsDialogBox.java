/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.core.ui.dialog.box;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Arrays;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

public class SettingsDialogBox extends DialogBox {


    public SettingsDialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(getContext());
        Context themedContext = sheet.getContext();

        View layout = android.view.LayoutInflater.from(themedContext).inflate(R.layout.dialog_settings, null);

        // Apply tints to header icon if present
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        View headerContainer = layout.findViewById(R.id.icon_container);
        if (headerIcon != null) {
            tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, headerIcon,
                    headerContainer);
        }

        LinearLayout optionsContainer = layout.findViewById(R.id.options_container);

        // 1. Choose Model
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.addSettingsOption(
                optionsContainer,
                themedContext.getString(R.string.settings_option_choose_model),
                R.drawable.ic_cpu_filled,
                themedContext,
                v -> {
                    sheet.dismiss();
                    switchToDialog(DialogType.ChoseModel);
                });

        // 2. Commands List
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.addSettingsOption(
                optionsContainer,
                themedContext.getString(R.string.dialog_title_commands_list),
                R.drawable.ic_command_filled,
                themedContext,
                v -> {
                    sheet.dismiss();
                    switchToDialog(DialogType.EditCommandsList);
                });

        // 3. Patterns List
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.addSettingsOption(
                optionsContainer,
                themedContext.getString(R.string.settings_option_patterns_list),
                R.drawable.ic_document_code_filled,
                themedContext,
                v -> {
                    sheet.dismiss();
                    switchToDialog(DialogType.EditPatternList);
                });

        // 4. Other Settings
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.addSettingsOption(
                optionsContainer,
                themedContext.getString(R.string.title_other_settings),
                R.drawable.ic_setting_filled,
                themedContext,
                v -> {
                    sheet.dismiss();
                    switchToDialog(DialogType.OtherSettings);
                });

        sheet.setContentView(layout);
        return sheet;
    }
}
