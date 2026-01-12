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
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.settings.OtherSettingsType;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

public class OtherSettingsDialogBox extends DialogBox {

    public OtherSettingsDialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        // Load settings from ContentProvider
        Bundle otherSettingsInput = loadOtherSettings();

        tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(getContext());
        Context themedContext = sheet.getContext();

        // Use themed inflater
        View layout = android.view.LayoutInflater.from(themedContext).inflate(R.layout.dialog_other_settings, null);

        LinearLayout settingsContainer = layout.findViewById(R.id.settings_container);
        MaterialButton btnCancel = layout.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = layout.findViewById(R.id.btn_save);
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        View headerIconContainer = layout.findViewById(R.id.icon_container);

        if (headerIcon != null) {
            tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, headerIcon,
                    headerIconContainer);
        }

        for (OtherSettingsType type : OtherSettingsType.values()) {
            if (type.nature == OtherSettingsType.Nature.Boolean) {
                // User requested to remove Material You settings from this dialog
                if (type == OtherSettingsType.MaterialYouEnabled ||
                        type == OtherSettingsType.MaterialYouUseWallpaper ||
                        type == OtherSettingsType.MaterialYouSeedColor ||
                        type == OtherSettingsType.MaterialYouSingleTone) {
                    continue;
                }

                View itemView = android.view.LayoutInflater.from(themedContext).inflate(R.layout.listview_item_checkbox,
                        settingsContainer, false);

                TextView titleView = itemView.findViewById(R.id.text_title);
                TextView descView = itemView.findViewById(R.id.text_desc);
                MaterialSwitch switchView = itemView.findViewById(R.id.checkbox);

                titleView.setText(type.title);
                descView.setText(type.description);

                // Get value from ContentProvider
                boolean currentValue = otherSettingsInput.getBoolean(type.name(), (Boolean) type.defaultValue);
                switchView.setChecked(currentValue);

                // Apply switch theme for consistency
                tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applySwitchTheme(themedContext, switchView);

                itemView.setOnClickListener(v -> {
                    switchView.setChecked(!switchView.isChecked());
                    getConfig().otherExtras.putBoolean(type.name(), switchView.isChecked());
                });

                // Allow clicking switch directly too
                switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    getConfig().otherExtras.putBoolean(type.name(), isChecked);
                });

                settingsContainer.addView(itemView);
            }
        }

        btnCancel.setOnClickListener(v -> {
            sheet.dismiss();
            switchToDialog(DialogType.Settings);
        });

        btnSave.setOnClickListener(v -> {
            // Save immediately to ContentProvider and notify listeners
            getConfig().saveToProvider();

            // Send broadcast to notify listeners of the change
            android.content.Intent broadcastIntent = new android.content.Intent(
                    tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_OTHER_SETTINGS, getConfig().otherExtras);
            getContext().sendBroadcast(broadcastIntent);

            // Go back to settings instead of closing
            sheet.dismiss();
            switchToDialog(DialogType.Settings);
        });

        // Back Button
        View btnBack = layout.findViewById(R.id.btn_back_header);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                sheet.dismiss();
                switchToDialog(DialogType.Settings);
            });
        }

        // Also tint buttons
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyButtonTheme(themedContext, btnSave);

        sheet.setContentView(layout);
        return sheet;
    }
}
