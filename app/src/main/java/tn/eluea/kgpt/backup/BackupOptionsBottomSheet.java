/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.backup;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.CompoundButtonCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

/**
 * Bottom sheet for selecting backup/restore options.
 */
public class BackupOptionsBottomSheet {

    public interface OnOptionsSelectedListener {
        void onOptionsSelected(BackupOptions options);
    }

    private final Context context;
    private final boolean isRestore;
    private final Set<BackupOptions.Option> availableOptions;
    private final OnOptionsSelectedListener listener;

    private FloatingBottomSheet dialog;
    private BackupOptions options;
    private Map<BackupOptions.Option, CheckBox> checkboxMap;
    private CheckBox cbSelectAll;

    /**
     * Create a backup options bottom sheet
     */
    public static BackupOptionsBottomSheet forBackup(Context context, OnOptionsSelectedListener listener) {
        return new BackupOptionsBottomSheet(context, false, null, listener);
    }

    /**
     * Create a restore options bottom sheet with available options from backup file
     */
    public static BackupOptionsBottomSheet forRestore(Context context,
            Set<BackupOptions.Option> availableOptions, OnOptionsSelectedListener listener) {
        return new BackupOptionsBottomSheet(context, true, availableOptions, listener);
    }

    private BackupOptionsBottomSheet(Context context, boolean isRestore,
            Set<BackupOptions.Option> availableOptions, OnOptionsSelectedListener listener) {
        this.context = context;
        this.isRestore = isRestore;
        this.availableOptions = availableOptions;
        this.listener = listener;
        this.options = new BackupOptions();
        this.checkboxMap = new HashMap<>();
    }

    public void show() {
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_backup_options, null);

        // Apply theme
        BottomSheetHelper.applyTheme(context, sheetView);

        dialog = new FloatingBottomSheet(context);
        dialog.setContentView(sheetView);

        setupViews(sheetView);
        setupListeners(sheetView);

        dialog.show();
    }

    private void setupViews(View view) {
        // Update title and icon based on mode
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvDescription = view.findViewById(R.id.tv_description);
        ImageView ivIcon = view.findViewById(R.id.iv_icon);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm);

        if (isRestore) {
            tvTitle.setText("Restore Settings");
            tvDescription.setText("Select what you want to restore");
            ivIcon.setImageResource(R.drawable.ic_cloud_restore_arrow_filled);
            btnConfirm.setText("Restore");
            btnConfirm.setIconResource(R.drawable.ic_cloud_restore_arrow_filled);
        } else {
            tvTitle.setText("Backup Settings");
            tvDescription.setText("Select what you want to backup");
            ivIcon.setImageResource(R.drawable.ic_cloud_backup_arrow_filled);
            btnConfirm.setText("Backup");
        }

        // Setup checkboxes
        cbSelectAll = view.findViewById(R.id.cb_select_all);

        // Apply primary color tint to select all checkbox
        ColorStateList checkboxTint = createCheckboxColorStateList(view.getContext());
        CompoundButtonCompat.setButtonTintList(cbSelectAll, checkboxTint);

        setupCheckbox(view, R.id.cb_commands, R.id.option_commands, BackupOptions.Option.COMMANDS);
        setupCheckbox(view, R.id.cb_patterns, R.id.option_patterns, BackupOptions.Option.PATTERNS);
        setupCheckbox(view, R.id.cb_model, R.id.option_model, BackupOptions.Option.LANGUAGE_MODEL);
        setupCheckbox(view, R.id.cb_appearance, R.id.option_appearance, BackupOptions.Option.APPEARANCE);
        setupCheckbox(view, R.id.cb_blur, R.id.option_blur, BackupOptions.Option.BLUR_SETTINGS);
        setupCheckbox(view, R.id.cb_general, R.id.option_general, BackupOptions.Option.GENERAL_SETTINGS);
        setupCheckbox(view, R.id.cb_app_triggers, R.id.option_app_triggers, BackupOptions.Option.APP_TRIGGERS);
        setupCheckbox(view, R.id.cb_text_actions, R.id.option_text_actions, BackupOptions.Option.TEXT_ACTIONS);
        setupCheckbox(view, R.id.cb_sensitive_data, R.id.option_sensitive_data, BackupOptions.Option.SENSITIVE_DATA);

        // For restore mode, disable unavailable options
        if (isRestore && availableOptions != null) {
            for (BackupOptions.Option option : BackupOptions.Option.values()) {
                if (!availableOptions.contains(option)) {
                    CheckBox cb = checkboxMap.get(option);
                    if (cb != null) {
                        cb.setChecked(false);
                        cb.setEnabled(false);
                        options.setSelected(option, false);

                        // Dim the parent container
                        View parent = (View) cb.getParent();
                        if (parent != null) {
                            parent.setAlpha(0.4f);
                        }
                    }
                }
            }
        }

        updateSelectAllState();
    }

    private void setupCheckbox(View view, int checkboxId, int containerId, BackupOptions.Option option) {
        CheckBox cb = view.findViewById(checkboxId);
        View container = view.findViewById(containerId);

        checkboxMap.put(option, cb);
        cb.setChecked(options.isSelected(option));

        // Apply primary color tint to checkbox
        ColorStateList checkboxTint = createCheckboxColorStateList(view.getContext());
        CompoundButtonCompat.setButtonTintList(cb, checkboxTint);

        // Make entire row clickable
        container.setOnClickListener(v -> {
            if (cb.isEnabled()) {
                cb.setChecked(!cb.isChecked());
            }
        });

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            options.setSelected(option, isChecked);
            animateCheckbox(cb, isChecked);
            updateSelectAllState();
        });
    }

    private void animateCheckbox(CheckBox cb, boolean isChecked) {
        if (isChecked) {
            // Scale up animation with overshoot
            cb.setScaleX(0.8f);
            cb.setScaleY(0.8f);
            cb.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .start();
        } else {
            // Scale down then back up
            cb.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        cb.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
        }
    }

    private void setupListeners(View view) {
        // Select All
        View selectAllContainer = view.findViewById(R.id.select_all_container);
        selectAllContainer.setOnClickListener(v -> {
            cbSelectAll.setChecked(!cbSelectAll.isChecked());
        });

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingProgrammatically) {
                for (Map.Entry<BackupOptions.Option, CheckBox> entry : checkboxMap.entrySet()) {
                    CheckBox cb = entry.getValue();
                    if (cb.isEnabled()) {
                        cb.setChecked(isChecked);
                    }
                }
            }
        });

        // Cancel button
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        // Confirm button
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            if (options.hasAnySelected()) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onOptionsSelected(options);
                }
            }
        });
    }

    private boolean isUpdatingProgrammatically = false;

    private ColorStateList createCheckboxColorStateList(Context context) {
        int colorPrimary = MaterialColors.getColor(context, androidx.appcompat.R.attr.colorPrimary,
                Color.BLACK);
        int colorUnchecked = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline,
                Color.GRAY);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // checked
                new int[] { -android.R.attr.state_checked } // unchecked
        };

        int[] colors = new int[] {
                colorPrimary,
                colorUnchecked
        };

        return new ColorStateList(states, colors);
    }

    private void updateSelectAllState() {
        int enabledCount = 0;
        int checkedCount = 0;

        for (Map.Entry<BackupOptions.Option, CheckBox> entry : checkboxMap.entrySet()) {
            CheckBox cb = entry.getValue();
            if (cb.isEnabled()) {
                enabledCount++;
                if (cb.isChecked()) {
                    checkedCount++;
                }
            }
        }

        isUpdatingProgrammatically = true;
        cbSelectAll.setChecked(enabledCount > 0 && checkedCount == enabledCount);
        isUpdatingProgrammatically = false;
    }
}
