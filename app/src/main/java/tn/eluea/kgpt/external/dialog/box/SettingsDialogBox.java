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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Arrays;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;

public class SettingsDialogBox extends DialogBox {
    
    // Icons for each dialog type
    private static final int[] DIALOG_ICONS = {
        R.drawable.ic_cpu_filled,           // ChoseModel
        R.drawable.ic_document_text_filled, // EditCommandsList
        R.drawable.ic_document_text_filled, // EditPatternList
        R.drawable.ic_setting_filled        // OtherSettings
    };
    
    public SettingsDialogBox(DialogBoxManager dialogManager, Activity parent,
                             Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        DialogType[] items = Arrays.stream(DialogType.values())
                .filter(t -> t.inSettings).toArray(DialogType[]::new);

        // Inflate custom layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_settings, null);
        LinearLayout optionsContainer = dialogView.findViewById(R.id.options_container);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);

        // Add options dynamically
        for (int i = 0; i < items.length; i++) {
            DialogType type = items[i];
            View optionView = LayoutInflater.from(getContext()).inflate(R.layout.item_settings_option, optionsContainer, false);
            
            TextView tvTitle = optionView.findViewById(R.id.tv_title);
            ImageView ivIcon = optionView.findViewById(R.id.iv_icon);
            ImageView ivArrow = optionView.findViewById(R.id.iv_arrow);
            
            tvTitle.setText(type.title);
            
            // Set icon based on index
            if (i < DIALOG_ICONS.length) {
                ivIcon.setImageResource(DIALOG_ICONS[i]);
            }
            
            // Apply icon color filter
            ivIcon.setColorFilter(iconColor);
            if (ivArrow != null) ivArrow.setColorFilter(iconColor);
            
            optionView.setOnClickListener(v -> {
                getDialog().dismiss();
                switchToDialog(type);
            });
            
            optionsContainer.addView(optionView);
        }

        return new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();
    }
}
