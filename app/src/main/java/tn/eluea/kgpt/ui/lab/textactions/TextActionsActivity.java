/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.ui.lab.textactions;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

import java.util.HashSet;
import java.util.Set;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.features.textactions.data.TextActionManager;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;

public class TextActionsActivity extends AppCompatActivity {

    private static final String PREF_TEXT_ACTIONS_ENABLED = "text_actions_enabled";
    private static final String PREF_TEXT_ACTIONS_LIST = "text_actions_list";
    private static final String PREF_TEXT_ACTIONS_SHOW_LABELS = "text_actions_show_labels";

    private ConfigClient configClient;
    private MaterialSwitch switchEnabled;
    private MaterialSwitch switchShowLabels;
    private LinearLayout actionsListLayout;
    private View contentContainer;
    private View emptyState;
    private Set<TextAction> enabledActions = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_actions);

        configClient = new ConfigClient(this);
        initViews();
        loadSettings();
        applyAmoledIfNeeded();

        // Candy colors removed
        // tn.eluea.kgpt.util.CandyColorHelper.applyToViewHierarchy(this,
        // findViewById(android.R.id.content));
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        switchEnabled = findViewById(R.id.switch_enabled);
        switchShowLabels = findViewById(R.id.switch_show_labels);
        actionsListLayout = findViewById(R.id.actions_list_layout);
        contentContainer = findViewById(R.id.content_container);
        emptyState = findViewById(R.id.empty_state);

        // Initialize dock action
        View dockContainer = findViewById(R.id.dock_action_container);
        TextView dockText = findViewById(R.id.dock_action_text);
        dockText.setText("Add Custom Action");
        dockContainer.setOnClickListener(v -> showAddCustomActionDialog(new TextActionManager(this)));

        // Main toggle
        // Main toggle
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Do not save immediately. Show restart dialog.
            showRestartRequiredDialog(isChecked);
        });

        // Show labels toggle - listener will be set in loadSettings()
    }

    private void loadSettings() {
        boolean isEnabled = configClient.getBoolean(PREF_TEXT_ACTIONS_ENABLED, false);
        boolean showLabels = configClient.getBoolean(PREF_TEXT_ACTIONS_SHOW_LABELS, true);
        String actionsJson = configClient.getString(PREF_TEXT_ACTIONS_LIST, null);

        // Avoid triggering listener during recursive setChecked
        switchEnabled.setOnCheckedChangeListener(null);
        switchEnabled.setChecked(isEnabled);
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Do not save immediately. Show restart dialog.
            showRestartRequiredDialog(isChecked);
        });

        switchShowLabels.setOnCheckedChangeListener(null);
        switchShowLabels.setChecked(showLabels);
        switchShowLabels.setOnCheckedChangeListener((buttonView, isChecked) -> {
            configClient.putBoolean(PREF_TEXT_ACTIONS_SHOW_LABELS, isChecked);
            showSaveConfirmation();
        });

        updateContentVisibility(isEnabled);

        enabledActions = decodeEnabledActions(actionsJson);
        if (enabledActions.isEmpty()) {
            for (TextAction a : TextAction.values())
                enabledActions.add(a);
        }

        populateActionsList();
    }

    private void populateActionsList() {
        actionsListLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        TextActionManager actionManager = new TextActionManager(this);

        // Resolve dynamic colors
        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(actionsListLayout,
                androidx.appcompat.R.attr.colorPrimary);
        int colorSecondary = com.google.android.material.color.MaterialColors.getColor(actionsListLayout,
                com.google.android.material.R.attr.colorSecondary);
        int colorTertiary = com.google.android.material.color.MaterialColors.getColor(actionsListLayout,
                com.google.android.material.R.attr.colorTertiary);

        // Standard Actions
        int index = 0;
        for (TextAction action : TextAction.values()) {
            View view = inflater.inflate(R.layout.item_text_action_setting, actionsListLayout, false);

            ImageView icon = view.findViewById(R.id.action_icon);
            TextView title = view.findViewById(R.id.action_title);
            TextView subtitle = view.findViewById(R.id.action_subtitle);
            MaterialSwitch switchAction = view.findViewById(R.id.switch_action);

            icon.setImageResource(action.iconRes);

            // Apply dynamic seed-derived colors (alternating for variety)
            int color;
            int mod = index % 2; // Cycle Primary -> Secondary
            if (mod == 0)
                color = colorPrimary;
            else
                color = colorSecondary;

            icon.setColorFilter(color);
            index++;

            title.setText(action.labelEn);
            subtitle.setText(action.getLabel(false));

            boolean isActionEnabled = enabledActions.contains(action);
            switchAction.setChecked(isActionEnabled);

            switchAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    enabledActions.add(action);
                } else {
                    enabledActions.remove(action);
                }
                saveEnabledActions();
                showSaveConfirmation();
            });

            view.setOnClickListener(v -> showEditPromptDialog(action, actionManager));

            actionsListLayout.addView(view);
        }

        // Custom Actions
        java.util.List<tn.eluea.kgpt.features.textactions.domain.CustomTextAction> customActions = actionManager
                .getCustomActions();
        for (tn.eluea.kgpt.features.textactions.domain.CustomTextAction action : customActions) {
            View view = inflater.inflate(R.layout.item_text_action_setting, actionsListLayout, false);

            ImageView icon = view.findViewById(R.id.action_icon);
            TextView title = view.findViewById(R.id.action_title);
            TextView subtitle = view.findViewById(R.id.action_subtitle);
            MaterialSwitch switchAction = view.findViewById(R.id.switch_action);

            // Unified Icon for Custom Actions - Magic Star Filled
            icon.setImageResource(R.drawable.ic_star_filled);
            // Use Primary for Custom Actions to make them stand out as "Main" feature
            icon.setColorFilter(colorPrimary);

            title.setText(action.name);
            subtitle.setText("Custom Action");

            switchAction.setChecked(action.enabled);

            switchAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                action.enabled = isChecked;
                actionManager.updateCustomAction(action);
            });

            view.setOnClickListener(v -> showEditCustomActionDialog(action, actionManager));

            actionsListLayout.addView(view);
        }
    }

    private void showAddCustomActionDialog(TextActionManager actionManager) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this,
                R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_custom_text_action, null);
        builder.setView(view);

        com.google.android.material.textfield.TextInputEditText etName = view.findViewById(R.id.et_name);
        com.google.android.material.textfield.TextInputEditText etPrompt = view.findViewById(R.id.et_prompt);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String prompt = etPrompt.getText().toString().trim();

            if (name.isEmpty() || prompt.isEmpty()) {
                Snackbar.make(findViewById(R.id.root_layout), "Please enter name and prompt", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            actionManager.addCustomAction(name, prompt);
            populateActionsList();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditCustomActionDialog(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action,
            TextActionManager actionManager) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this,
                R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_custom_text_action, null);
        builder.setView(view);

        com.google.android.material.textfield.TextInputEditText etName = view.findViewById(R.id.et_name);
        com.google.android.material.textfield.TextInputEditText etPrompt = view.findViewById(R.id.et_prompt);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save);
        com.google.android.material.button.MaterialButton btnDelete = view.findViewById(R.id.btn_delete);

        etName.setText(action.name);
        etPrompt.setText(action.prompt);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String prompt = etPrompt.getText().toString().trim();

            if (name.isEmpty() || prompt.isEmpty()) {
                Snackbar.make(findViewById(R.id.root_layout), "Please enter name and prompt", Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            action.name = name;
            action.prompt = prompt;
            actionManager.updateCustomAction(action);
            populateActionsList();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            showDeleteCustomActionBottomSheet(action, actionManager, dialog);
        });

        dialog.show();
    }

    private void showDeleteCustomActionBottomSheet(tn.eluea.kgpt.features.textactions.domain.CustomTextAction action,
            TextActionManager actionManager,
            androidx.appcompat.app.AlertDialog editDialog) {
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_delete_custom_action, null);

        FloatingBottomSheet bottomSheet = new FloatingBottomSheet(this);
        bottomSheet.setContentView(view);

        TextView tvActionName = view.findViewById(R.id.tv_action_name);
        com.google.android.material.button.MaterialButton btnDelete = view.findViewById(R.id.btn_delete);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        tvActionName.setText(action.name);

        btnDelete.setOnClickListener(v -> {
            actionManager.deleteCustomAction(action.id);
            bottomSheet.dismiss();
            editDialog.dismiss();
            populateActionsList();
        });

        btnCancel.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    private void showEditPromptDialog(TextAction action, TextActionManager actionManager) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this,
                R.style.AlertDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text_action_prompt, null);
        builder.setView(view);

        ImageView icon = view.findViewById(R.id.iv_action_icon);
        TextView title = view.findViewById(R.id.tv_title);
        com.google.android.material.textfield.TextInputEditText input = view.findViewById(R.id.et_prompt);
        com.google.android.material.button.MaterialButton btnReset = view.findViewById(R.id.btn_reset);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save);

        icon.setImageResource(action.iconRes);
        try {
            icon.setColorFilter(Color.parseColor(action.color));
        } catch (Exception e) {
            icon.setColorFilter(Color.WHITE);
        }

        title.setText("Edit " + action.labelEn + " Prompt");
        input.setText(actionManager.getActionPrompt(action));

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String newPrompt = input.getText().toString().trim();
            if (!newPrompt.isEmpty()) {
                actionManager.setActionPrompt(action, newPrompt);
            }
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            actionManager.resetActionPrompt(action);
            input.setText(actionManager.getActionPrompt(action));
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateContentVisibility(boolean enabled) {
        contentContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private void saveEnabledActions() {
        String json = TextActionManager.encodeEnabledActions(enabledActions);
        configClient.putString(PREF_TEXT_ACTIONS_LIST, json);
    }

    private Set<TextAction> decodeEnabledActions(String json) {
        Set<TextAction> actions = new HashSet<>();
        if (json == null || json.isEmpty()) {
            return actions;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String actionName = array.getString(i);
                try {
                    actions.add(TextAction.valueOf(actionName));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return actions;
    }

    private void showSaveConfirmation() {
        // Silent save - no visual confirmation
    }

    private void applyAmoledIfNeeded() {
        boolean isDarkMode = BottomSheetHelper.isDarkMode(this);
        boolean isAmoled = BottomSheetHelper.isAmoledMode(this);

        if (isDarkMode && isAmoled) {
            View root = findViewById(R.id.root_layout);
            root.setBackgroundColor(ContextCompat.getColor(this, R.color.background_amoled));
        }

        if (isDarkMode) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    private void showRestartRequiredDialog(boolean newEnabledState) {
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_restart_required, null);

        final tn.eluea.kgpt.ui.main.FloatingBottomSheet bottomSheetDialog = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(
                this);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false); // Force user choice

        View btnRestartContainer = view.findViewById(R.id.btn_restart_container);
        android.widget.TextView tvTag = view.findViewById(R.id.tv_countdown_tag);
        com.google.android.material.button.MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        // Initial text
        if (tvTag != null)
            tvTag.setText("6s");

        // Timer Logic
        android.os.CountDownTimer timer = new android.os.CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvTag != null && bottomSheetDialog.isShowing()) {
                    tvTag.setText((millisUntilFinished / 1000 + 1) + "s");
                }
            }

            @Override
            public void onFinish() {
                if (bottomSheetDialog.isShowing()) {
                    performRestart(newEnabledState);
                    bottomSheetDialog.dismiss();
                }
            }
        };
        timer.start();

        if (btnRestartContainer != null) {
            btnRestartContainer.setOnClickListener(v -> {
                timer.cancel();
                performRestart(newEnabledState);
                bottomSheetDialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                timer.cancel();
                // Revert switch without triggering listener
                switchEnabled.setOnCheckedChangeListener(null);
                switchEnabled.setChecked(!newEnabledState);
                switchEnabled
                        .setOnCheckedChangeListener((buttonView, isChecked) -> showRestartRequiredDialog(isChecked));
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void performRestart(boolean newEnabledState) {
        configClient.putBoolean(PREF_TEXT_ACTIONS_ENABLED, newEnabledState);
        updateContentVisibility(newEnabledState);

        // Give a small delay for preference to save before killing
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            tn.eluea.kgpt.KGPTApplication.restartApp(this, TextActionsActivity.class);
        }, 300);
    }
}
