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
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;
import tn.eluea.kgpt.instruction.command.SimpleGenerativeAICommand;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

public class CommandEditDialogBox extends DialogBox {
    public CommandEditDialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardCommands();

        tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(getContext());

        // Use Activity Context directly to ensure Theme and Dynamic Colors are
        // preserved and TextInputLayout works
        Context themedContext = getContext();

        View layout = android.view.LayoutInflater.from(themedContext).inflate(R.layout.dialog_command_edit, null);

        TextInputEditText prefixEditText = layout.findViewById(R.id.edit_prefix);
        TextInputEditText messageEditText = layout.findViewById(R.id.edit_message);
        TextView tvTitle = layout.findViewById(R.id.tv_title);
        MaterialButton btnCancel = layout.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = layout.findViewById(R.id.btn_save);
        MaterialButton btnDelete = layout.findViewById(R.id.btn_delete);
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        View headerIconContainer = layout.findViewById(R.id.icon_container);

        if (headerIcon != null) {
            tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, headerIcon,
                    headerIconContainer);
        }

        final int commandIndex = getConfig().focusCommandIndex;
        if (commandIndex >= 0) {
            if (commandIndex >= getConfig().commands.size()) {
                // Should not happen, but safe guard
                try {
                    sheet.dismiss();
                } catch (Exception e) {
                }
                return sheet;
            }
            GenerativeAICommand command = getConfig().commands.get(commandIndex);
            prefixEditText.setText(command.getCommandPrefix());
            messageEditText.setText(command.getTweakMessage());
            tvTitle.setText("Edit Command");
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("New Command");
            btnDelete.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(v -> {
            sheet.dismiss();
            switchToDialog(DialogType.EditCommandsList);
        });

        btnSave.setOnClickListener(v -> {
            int commandPos = commandIndex;

            String prefix = prefixEditText.getText().toString().trim();
            String message = messageEditText.getText().toString();
            long similarCount = getConfig().commands.stream().filter((c) -> prefix.equals(c.getCommandPrefix()))
                    .count();
            if ((commandPos == -1 && similarCount >= 1)
                    || (commandPos >= 0 && similarCount >= 2)) {
                Toast.makeText(getContext(), "There is another command with same name", Toast.LENGTH_LONG).show();
                return;
            }

            if (commandPos >= 0) {
                getConfig().commands.remove(commandPos);
            } else {
                commandPos = getConfig().commands.size();
            }

            getConfig().commands.add(commandPos, new SimpleGenerativeAICommand(prefix, message));

            // Save immediately to ContentProvider and notify listeners
            getConfig().saveToProvider();

            // Send broadcast to notify CommandManager of the change
            android.content.Intent broadcastIntent = new android.content.Intent(
                    tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_COMMAND_LIST,
                    tn.eluea.kgpt.instruction.command.Commands.encodeCommands(getConfig().commands));
            getContext().sendBroadcast(broadcastIntent);

            // Go back to command list instead of closing
            sheet.dismiss();
            switchToDialog(DialogType.EditCommandsList);
        });

        btnDelete.setOnClickListener(v -> {
            getConfig().commands.remove(commandIndex);

            // Save immediately to ContentProvider and notify listeners
            getConfig().saveToProvider();

            // Send broadcast to notify CommandManager of the change
            android.content.Intent broadcastIntent = new android.content.Intent(
                    tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_COMMAND_LIST,
                    tn.eluea.kgpt.instruction.command.Commands.encodeCommands(getConfig().commands));
            getContext().sendBroadcast(broadcastIntent);

            sheet.dismiss();
            switchToDialog(DialogType.EditCommandsList);
        });

        // Back Button
        View btnBackHeader = layout.findViewById(R.id.btn_back_header);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> {
                sheet.dismiss();
                switchToDialog(DialogType.EditCommandsList);
            });
        }

        // Tints
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyButtonTheme(themedContext, btnSave);

        sheet.setContentView(layout);
        return sheet;
    }

}
