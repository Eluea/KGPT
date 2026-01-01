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

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.instruction.command.AbstractCommand;

public class CommandListDialogBox extends DialogBox {
    public CommandListDialogBox(DialogBoxManager dialogManager, Activity parent,
                                Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardCommands();

        View layout = getParent().getLayoutInflater().inflate(R.layout.dialog_list, null);
        LinearLayout itemsContainer = layout.findViewById(R.id.items_container);
        TextView tvTitle = layout.findViewById(R.id.tv_title);
        ImageView ivIcon = layout.findViewById(R.id.iv_icon);
        TextView tvEmpty = layout.findViewById(R.id.tv_empty);
        MaterialButton btnBack = layout.findViewById(R.id.btn_back);
        MaterialButton btnNew = layout.findViewById(R.id.btn_new);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);
        
        tvTitle.setText("Commands List");
        ivIcon.setImageResource(R.drawable.ic_command_filled);
        ivIcon.setColorFilter(iconColor);
        btnNew.setVisibility(View.VISIBLE);
        
        if (getConfig().commands.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No commands yet");
        } else {
            for (int i = 0; i < getConfig().commands.size(); i++) {
                AbstractCommand command = getConfig().commands.get(i);
                View itemView = getParent().getLayoutInflater().inflate(R.layout.item_list_option, itemsContainer, false);
                
                TextView tvName = itemView.findViewById(R.id.tv_item_name);
                ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
                ImageView arrowIcon = itemView.findViewById(R.id.iv_arrow);
                
                tvName.setText(command.getCommandPrefix());
                itemIcon.setImageResource(R.drawable.ic_command_filled);
                itemIcon.setColorFilter(iconColor);
                if (arrowIcon != null) arrowIcon.setColorFilter(iconColor);
                
                final int index = i;
                itemView.setOnClickListener(v -> {
                    getConfig().focusCommandIndex = index;
                    switchToDialog(DialogType.EditCommand);
                });
                
                itemsContainer.addView(itemView);
            }
        }
        
        btnBack.setOnClickListener(v -> switchToDialog(DialogType.Settings));
        btnNew.setOnClickListener(v -> {
            getConfig().focusCommandIndex = -1;
            switchToDialog(DialogType.EditCommand);
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(layout)
                .create();
        
        return dialog;
    }
}
