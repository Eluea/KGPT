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
import android.os.Bundle;
import android.view.View;
import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.instruction.command.AbstractCommand;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

public class CommandListDialogBox extends DialogBox {
    public CommandListDialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardCommands();

        tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(getContext());
        Context themedContext = sheet.getContext();

        View layout = android.view.LayoutInflater.from(themedContext).inflate(R.layout.dialog_list, null);

        LinearLayout itemsContainer = layout.findViewById(R.id.items_container);
        TextView tvTitle = layout.findViewById(R.id.tv_title);
        ImageView ivIcon = layout.findViewById(R.id.iv_icon);
        View iconContainer = layout.findViewById(R.id.icon_container);
        TextView tvEmpty = layout.findViewById(R.id.tv_empty);
        View btnBack = layout.findViewById(R.id.btn_back);
        View btnNew = layout.findViewById(R.id.btn_new);
        com.airbnb.lottie.LottieAnimationView btnNewLottie = layout.findViewById(R.id.btn_new_lottie);

        tvTitle.setText(R.string.dialog_title_commands_list);
        ivIcon.setImageResource(R.drawable.ic_command_filled);

        // Apply tint to header icon
        // Note: DialogUiUtils takes care of checking if views are null
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, ivIcon, iconContainer);

        btnNew.setVisibility(View.VISIBLE);
        if (btnNewLottie != null) {
            int primaryColor = com.google.android.material.color.MaterialColors.getColor(themedContext,
                    androidx.appcompat.R.attr.colorPrimary, android.graphics.Color.WHITE);
            tn.eluea.kgpt.util.LottieHelper.playOnce(btnNewLottie, primaryColor);
        }

        if (getConfig().commands.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.no_commands);
        } else {
            for (int i = 0; i < getConfig().commands.size(); i++) {
                AbstractCommand command = getConfig().commands.get(i);
                View itemView = android.view.LayoutInflater.from(themedContext).inflate(R.layout.item_list_option,
                        itemsContainer,
                        false);

                TextView tvName = itemView.findViewById(R.id.tv_item_name);
                ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
                View itemIconContainer = itemView.findViewById(R.id.icon_container);
                ImageView arrowIcon = itemView.findViewById(R.id.iv_arrow);

                tvName.setText(command.getCommandPrefix());
                itemIcon.setImageResource(R.drawable.ic_command_filled);

                // Apply tint to list item
                tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, itemIcon,
                        itemIconContainer);

                // Manually tint arrow for consistency
                if (arrowIcon != null) {
                    tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, arrowIcon,
                            null);
                }

                final int index = i;
                itemView.setOnClickListener(v -> {
                    sheet.dismiss();
                    getConfig().focusCommandIndex = index;
                    switchToDialog(DialogType.EditCommand);
                });

                itemsContainer.addView(itemView);
            }
        }

        btnBack.setOnClickListener(v -> {
            sheet.dismiss();
            switchToDialog(DialogType.Settings);
        });
        btnNew.setOnClickListener(v -> {
            sheet.dismiss();
            getConfig().focusCommandIndex = -1;
            switchToDialog(DialogType.EditCommand);
        });

        sheet.setContentView(layout);
        return sheet;
    }
}
