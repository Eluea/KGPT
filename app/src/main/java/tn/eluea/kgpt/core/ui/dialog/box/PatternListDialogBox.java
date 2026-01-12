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

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;

public class PatternListDialogBox extends DialogBox {
    public PatternListDialogBox(DialogBoxManager dialogManager, Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        safeguardPatterns();

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

        tvTitle.setText("Trigger Symbols");
        // Consistent icon with Settings
        ivIcon.setImageResource(R.drawable.ic_document_code_filled);

        // Apply Tint
        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, ivIcon, iconContainer);

        btnNew.setVisibility(View.GONE);

        if (getConfig().patterns.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("No patterns");
        } else {
            // ... existing loop ...
            for (int i = 0; i < getConfig().patterns.size(); i++) {
                ParsePattern pattern = getConfig().patterns.get(i);
                View itemView = android.view.LayoutInflater.from(themedContext).inflate(R.layout.item_list_option,
                        itemsContainer, false);

                TextView tvName = itemView.findViewById(R.id.tv_item_name);
                TextView tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
                ImageView itemIcon = itemView.findViewById(R.id.iv_icon);
                View itemIconContainer = itemView.findViewById(R.id.icon_container);
                ImageView arrowIcon = itemView.findViewById(R.id.iv_arrow);

                // Show enabled/disabled status in title
                String title = pattern.getType().title;
                if (!pattern.isEnabled()) {
                    title += " (Disabled)";
                }
                tvName.setText(title);

                // Show user-friendly symbol instead of regex
                String symbol = getDisplaySymbol(pattern);
                if (tvSubtitle != null) {
                    tvSubtitle.setText("Trigger: " + symbol);
                    tvSubtitle.setVisibility(View.VISIBLE);
                }

                itemIcon.setImageResource(R.drawable.ic_document_code_filled);

                // Apply Tint
                tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, itemIcon,
                        itemIconContainer);

                // Manually tint arrow
                if (arrowIcon != null) {
                    tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext, arrowIcon,
                            null);
                }

                // Set alpha based on enabled state
                itemView.setAlpha(pattern.isEnabled() ? 1.0f : 0.5f);

                // Allow editing for all editable patterns
                if (pattern.getType().editable) {
                    final int index = i;
                    itemView.setOnClickListener(v -> {
                        getConfig().focusPatternIndex = index;
                        sheet.dismiss();
                        switchToDialog(DialogType.EditPattern);
                    });
                } else {
                    // Maybe show toast or explain?
                    // Previously logic was to only add click listener if editable.
                    // Keep as is.
                }

                itemsContainer.addView(itemView);
            }
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                sheet.dismiss();
                switchToDialog(DialogType.Settings);
            });
        }

        sheet.setContentView(layout);
        return sheet;
    }

    /**
     * Extract and display user-friendly symbol from pattern
     */
    private String getDisplaySymbol(ParsePattern pattern) {
        String regex = pattern.getPattern().pattern();
        String symbol = PatternType.regexToSymbol(regex);

        if (symbol != null && !symbol.isEmpty()) {
            return "\"" + symbol + "\"";
        }

        // Fallback to default symbol
        return "\"" + pattern.getType().defaultSymbol + "\"";
    }
}
