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
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.widget.CompoundButtonCompat;

import androidx.core.content.ContextCompat;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.llm.LanguageModel;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

public class ChoseModelDialogBox extends DialogBox {
        public ChoseModelDialogBox(DialogBoxManager dialogManager, Activity parent,
                        Bundle inputBundle, ConfigContainer configContainer) {
                super(dialogManager, parent, inputBundle, configContainer);
        }

        @Override
        protected Dialog build() {
                safeguardModelData();

                tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(
                                getContext());
                Context themedContext = sheet.getContext();

                View layout = android.view.LayoutInflater.from(themedContext).inflate(R.layout.dialog_choose_model,
                                null);

                LinearLayout modelsContainer = layout.findViewById(R.id.models_container);

                // Header Icon Tint
                ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
                View headerIconContainer = layout.findViewById(R.id.icon_container);

                if (headerIcon != null) {
                        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext,
                                        headerIcon,
                                        headerIconContainer);
                }

                LanguageModel[] models = LanguageModel.values();
                int selectedIndex = getConfig().selectedModel == null ? 0 : getConfig().selectedModel.ordinal();

                for (int i = 0; i < models.length; i++) {
                        LanguageModel model = models[i];
                        View itemView = android.view.LayoutInflater.from(themedContext).inflate(
                                        R.layout.item_model_option,
                                        modelsContainer, false);

                        TextView tvName = itemView.findViewById(R.id.tv_model_name);
                        CheckBox checkBox = itemView.findViewById(R.id.cb_selected);
                        ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
                        View itemIconContainer = itemView.findViewById(R.id.icon_container);

                        tvName.setText(model.label);
                        checkBox.setChecked(i == selectedIndex);

                        if (i == selectedIndex) {
                                itemIcon.setImageResource(R.drawable.ic_model_selected);
                        } else {
                                itemIcon.setImageResource(R.drawable.ic_model_default);
                        }

                        // Apply Tints
                        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, itemIcon,
                                        itemIconContainer);

                        CompoundButtonCompat.setButtonTintList(checkBox,
                                        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils
                                                        .getCheckboxColorStateList(themedContext));

                        final int index = i;
                        itemView.setOnClickListener(v -> {
                                getConfig().selectedModel = models[index];
                                sheet.dismiss();
                                switchToDialog(DialogType.ConfigureModel);
                        });

                        // Also make checkbox clickable
                        checkBox.setOnClickListener(v -> {
                                getConfig().selectedModel = models[index];
                                sheet.dismiss();
                                switchToDialog(DialogType.ConfigureModel);
                        });

                        modelsContainer.addView(itemView);
                }

                // Back button
                View btnBack = layout.findViewById(R.id.btn_back);
                if (btnBack != null) {
                        btnBack.setOnClickListener(v -> {
                                sheet.dismiss();
                                switchToDialog(DialogType.Settings);
                        });
                }

                sheet.setContentView(layout);
                return sheet;
        }

}
