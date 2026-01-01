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
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.llm.LanguageModel;

public class ChoseModelDialogBox extends DialogBox {
    public ChoseModelDialogBox(DialogBoxManager dialogManager, Activity parent,
                               Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardModelData();

        View layout = getParent().getLayoutInflater().inflate(R.layout.dialog_choose_model, null);
        LinearLayout modelsContainer = layout.findViewById(R.id.models_container);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);
        
        // Set header icon color
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        if (headerIcon != null) headerIcon.setColorFilter(iconColor);
        
        LanguageModel[] models = LanguageModel.values();
        int selectedIndex = getConfig().selectedModel.ordinal();
        
        for (int i = 0; i < models.length; i++) {
            LanguageModel model = models[i];
            View itemView = getParent().getLayoutInflater().inflate(R.layout.item_model_option, modelsContainer, false);
            
            TextView tvName = itemView.findViewById(R.id.tv_model_name);
            RadioButton radio = itemView.findViewById(R.id.radio_selected);
            ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
            
            tvName.setText(model.label);
            radio.setChecked(i == selectedIndex);
            if (itemIcon != null) itemIcon.setColorFilter(iconColor);
            
            final int index = i;
            itemView.setOnClickListener(v -> {
                getConfig().selectedModel = models[index];
                switchToDialog(DialogType.ConfigureModel);
            });
            
            modelsContainer.addView(itemView);
        }
        
        // Back button
        layout.findViewById(R.id.btn_back).setOnClickListener(v -> {
            switchToDialog(DialogType.Settings);
        });

        return new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(layout)
                .create();
    }

}
