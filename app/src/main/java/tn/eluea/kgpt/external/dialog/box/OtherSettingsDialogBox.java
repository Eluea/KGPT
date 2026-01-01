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
package tn.eluea.kgpt.external.dialog.box;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.settings.OtherSettingsType;

public class OtherSettingsDialogBox extends DialogBox {

    public OtherSettingsDialogBox(DialogBoxManager dialogManager, Activity parent,
                                  Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        // Load settings from ContentProvider
        Bundle otherSettingsInput = loadOtherSettings();
        
        View layout = getParent().getLayoutInflater().inflate(R.layout.dialog_other_settings, null);
        LinearLayout settingsContainer = layout.findViewById(R.id.settings_container);
        MaterialButton btnCancel = layout.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = layout.findViewById(R.id.btn_save);
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);
        if (headerIcon != null) headerIcon.setColorFilter(iconColor);
        
        for (OtherSettingsType type : OtherSettingsType.values()) {
            if (type.nature == OtherSettingsType.Nature.Boolean) {
                View itemView = getParent().getLayoutInflater().inflate(R.layout.listview_item_checkbox, settingsContainer, false);
                
                TextView titleView = itemView.findViewById(R.id.text_title);
                TextView descView = itemView.findViewById(R.id.text_desc);
                MaterialSwitch switchView = itemView.findViewById(R.id.checkbox);
                
                titleView.setText(type.title);
                descView.setText(type.description);
                
                // Get value from ContentProvider
                boolean currentValue = otherSettingsInput.getBoolean(type.name(), (Boolean) type.defaultValue);
                switchView.setChecked(currentValue);
                
                itemView.setOnClickListener(v -> {
                    switchView.setChecked(!switchView.isChecked());
                    getConfig().otherExtras.putBoolean(type.name(), switchView.isChecked());
                });
                
                settingsContainer.addView(itemView);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(layout)
                .create();
        
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            switchToDialog(DialogType.Settings);
        });
        
        btnSave.setOnClickListener(v -> {
            // Save immediately to ContentProvider and notify listeners
            getConfig().saveToProvider();
            
            // Send broadcast to notify listeners of the change
            android.content.Intent broadcastIntent = new android.content.Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_OTHER_SETTINGS, getConfig().otherExtras);
            getContext().sendBroadcast(broadcastIntent);
            
            // Go back to settings instead of closing
            switchToDialog(DialogType.Settings);
        });

        return dialog;
    }
}
