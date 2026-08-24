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
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.util.ModelCatalog;
import tn.eluea.kgpt.llm.LanguageModelField;
import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.HorizontalScrollView;
import android.content.Context;

public class ConfigureModelDialogBox extends DialogBox {

        // Valid model names for validation
        // H1: single source of truth shared with the in-app editor
        private static final Map<LanguageModel, String[]> MODEL_PRESETS = tn.eluea.kgpt.util.ModelCatalog.PRESETS;
        private static final Map<LanguageModel, Set<String>> VALID_MODELS = tn.eluea.kgpt.util.ModelCatalog.VALID;


        public ConfigureModelDialogBox(DialogBoxManager dialogManager, Activity parent,
                        Bundle inputBundle, ConfigContainer configContainer) {
                super(dialogManager, parent, inputBundle, configContainer);
        }

        private boolean isValidModelName(LanguageModel model, String modelName) {
                if (modelName == null || modelName.trim().isEmpty())
                        return false;
                Set<String> validSet = VALID_MODELS.get(model);
                if (validSet == null)
                        return true; // Allow any for OpenRouter
                return validSet.contains(modelName.trim());
        }

        private String getSuggestedModel(LanguageModel model, String invalidName) {
                String[] presets = MODEL_PRESETS.get(model);
                if (presets == null || presets.length == 0) {
                        return model.getDefault(LanguageModelField.SubModel);
                }

                if (invalidName != null) {
                        String lowerInvalid = invalidName.toLowerCase(java.util.Locale.ROOT);
                        for (String preset : presets) {
                                if (preset.toLowerCase(java.util.Locale.ROOT).contains(lowerInvalid) ||
                                                lowerInvalid.contains(preset.toLowerCase(java.util.Locale.ROOT)
                                                                .replace("-preview", ""))) {
                                        return preset;
                                }
                        }
                }
                return presets[0];
        }

        @Override
        protected Dialog build() {
                // FORCE populate GLM presets to ensure they exist
                VALID_MODELS.put(LanguageModel.GLM, new HashSet<>(Arrays.asList(
                                "glm-4", "glm-4-plus", "glm-4-air", "glm-4-airx", "glm-4-long",
                                "glm-4-flashx", "glm-4-flash", "glm-4-9b",
                                "glm-4-0520", "glm-3-turbo")));
                MODEL_PRESETS.put(LanguageModel.GLM, new String[] {
                                "glm-4", "glm-4-plus", "glm-4-flash", "glm-4-air", "glm-3-turbo"
                });

                safeguardModelData();
                Context context = getContext();
                if (context != null) {
                        // Debug toast to see if presets are recognized
                        String[] presetsLines = MODEL_PRESETS.get(LanguageModel.GLM);
                        int count = presetsLines != null ? presetsLines.length : 0;
                        // android.widget.Toast.makeText(context, "Debug: GLM Presets count = " + count,
                        // android.widget.Toast.LENGTH_SHORT).show();
                }
                if (context != null) {
                        android.widget.Toast
                                        .makeText(context, "Config Dialog Opened: " + getConfig().selectedModel.name(),
                                                        android.widget.Toast.LENGTH_SHORT)
                                        .show();
                        android.util.Log.e("KGPT_DEBUG", "Dialog OPENED for " + getConfig().selectedModel.name());
                }

                tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(
                                getContext());
                Context themedContext = sheet.getContext();

                Bundle modelConfig = getConfig().languageModelsConfig.getBundle(getConfig().selectedModel.name());
                if (modelConfig == null) {
                        Log.w("KGPT_ConfigModel", "No model bundle for " + getConfig().selectedModel.name() + " — using defaults");
                }

                View layout = android.view.LayoutInflater.from(themedContext).inflate(R.layout.dialog_configue_model,
                                null);

                LinearLayout fieldsContainer = layout.findViewById(R.id.fields_container);
                TextView tvTitle = layout.findViewById(R.id.tv_title);
                MaterialButton btnCancel = layout.findViewById(R.id.btn_cancel);
                MaterialButton btnSave = layout.findViewById(R.id.btn_save);

                // Apply Header Tints
                ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
                View headerIconContainer = layout.findViewById(R.id.icon_container);
                if (headerIcon != null) {
                        tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyMaterialYouTints(themedContext,
                                        headerIcon,
                                        headerIconContainer);
                }

                tvTitle.setText(themedContext.getString(R.string.dialog_title_config_model,
                                getConfig().selectedModel.label));

                Bundle tempModelConfig = new Bundle();
                TextInputEditText subModelEditText = null;

                for (LanguageModelField field : LanguageModelField.values()) {
                        View fieldView = android.view.LayoutInflater.from(themedContext).inflate(
                                        R.layout.dialog_configure_model_field,
                                        fieldsContainer, false);
                        TextInputLayout inputLayout = fieldView.findViewById(R.id.field_layout);
                        TextInputEditText editText = fieldView.findViewById(R.id.field_edit);

                        inputLayout.setHint(themedContext.getString(field.titleResId));
                        editText.setInputType(field.inputType);

                        String fieldValue = modelConfig.getString(field.name);
                        editText.setText(fieldValue != null ? fieldValue : getConfig().selectedModel.getDefault(field));

                        if (field == LanguageModelField.SubModel) {
                                subModelEditText = editText;
                        }

                        editText.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void afterTextChanged(Editable s) {
                                }

                                @Override
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                @Override
                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                        tempModelConfig.putString(field.name, s.toString());
                                }
                        });

                        fieldsContainer.addView(fieldView);

                        // Add chips for SubModel if presets exist
                        if (field == LanguageModelField.SubModel) {
                                String[] presets = MODEL_PRESETS.get(getConfig().selectedModel);

                                // FORCE DEBUG
                                String msg = "Model: " + getConfig().selectedModel + ", Presets: "
                                                + (presets != null ? presets.length : "null");
                                android.util.Log.e("KGPT_DEBUG", msg);
                                // Toast.makeText(themedContext, msg, Toast.LENGTH_LONG).show();

                                if (presets != null && presets.length > 0) {
                                        HorizontalScrollView scrollView = new HorizontalScrollView(themedContext);
                                        LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                                        scrollViewParams.setMargins(0, 8, 0, 24); // Add margins
                                        scrollView.setLayoutParams(scrollViewParams);
                                        scrollView.setHorizontalScrollBarEnabled(false);

                                        ChipGroup chipGroup = new ChipGroup(themedContext);
                                        chipGroup.setLayoutParams(new ViewGroup.LayoutParams(
                                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                                        ViewGroup.LayoutParams.WRAP_CONTENT));
                                        chipGroup.setSingleLine(true);
                                        chipGroup.setSelectionRequired(false);

                                        // Resolve colors for chips to match ModelsFragment style
                                        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(
                                                        themedContext,
                                                        androidx.appcompat.R.attr.colorPrimary,
                                                        android.graphics.Color.BLACK);
                                        int colorOnPrimary = com.google.android.material.color.MaterialColors.getColor(
                                                        themedContext,
                                                        com.google.android.material.R.attr.colorOnPrimary,
                                                        android.graphics.Color.WHITE);
                                        int colorSurfaceContainerHigh = com.google.android.material.color.MaterialColors
                                                        .getColor(themedContext,
                                                                        com.google.android.material.R.attr.colorSurfaceContainerHigh,
                                                                        android.graphics.Color.LTGRAY);
                                        int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(
                                                        themedContext,
                                                        com.google.android.material.R.attr.colorOnSurface,
                                                        android.graphics.Color.BLACK);
                                        int colorDivider = androidx.core.content.ContextCompat.getColor(themedContext,
                                                        tn.eluea.kgpt.R.color.divider_color);

                                        int[][] states = new int[][] {
                                                        new int[] { android.R.attr.state_pressed }, // Pressed state
                                                        new int[] {} // Default state
                                        };

                                        android.content.res.ColorStateList bgStateList = new android.content.res.ColorStateList(
                                                        states,
                                                        new int[] {
                                                                        colorPrimary,
                                                                        colorSurfaceContainerHigh
                                                        });

                                        android.content.res.ColorStateList textStateList = new android.content.res.ColorStateList(
                                                        states,
                                                        new int[] {
                                                                        colorOnPrimary,
                                                                        colorOnSurface
                                                        });

                                        for (String preset : presets) {
                                                Chip chip = new Chip(themedContext);
                                                chip.setText(preset);
                                                chip.setCheckable(false); // We want click action, not toggle state in
                                                                          // this dialog
                                                chip.setClickable(true);

                                                // Apply Dynamic Colors
                                                chip.setChipBackgroundColor(bgStateList);
                                                chip.setTextColor(textStateList);

                                                // Stroke
                                                chip.setChipStrokeColor(android.content.res.ColorStateList
                                                                .valueOf(colorDivider));
                                                chip.setChipStrokeWidth(1f);
                                                // Ensure at least min height for touch target
                                                chip.setMinHeight(100);

                                                chip.setOnClickListener(v -> {
                                                        if (editText != null) {
                                                                editText.setText(preset);
                                                                editText.clearFocus();
                                                        }
                                                });
                                                chipGroup.addView(chip);
                                        }

                                        scrollView.addView(chipGroup);
                                        fieldsContainer.addView(scrollView);
                                }
                        }
                }

                btnCancel.setOnClickListener(v -> {
                        sheet.dismiss();
                        switchToDialog(DialogType.ChoseModel); // Go back to ChoseModel
                });

                final TextInputEditText finalSubModelEditText = subModelEditText;
                btnSave.setOnClickListener(v -> {
                        // Validate SubModel before saving
                        String subModelValue = tempModelConfig.getString(LanguageModelField.SubModel.name);
                        if (subModelValue == null && finalSubModelEditText != null) {
                                subModelValue = finalSubModelEditText.getText() != null
                                                ? finalSubModelEditText.getText().toString()
                                                : "";
                        }

                        if (subModelValue != null && !subModelValue.isEmpty() &&
                                        !isValidModelName(getConfig().selectedModel, subModelValue)) {

                                String suggested = getSuggestedModel(getConfig().selectedModel, subModelValue);

                                new AlertDialog.Builder(themedContext)
                                                .setTitle(R.string.dialog_title_unknown_model)
                                                .setMessage(themedContext.getString(R.string.dialog_msg_unknown_model,
                                                                subModelValue, suggested))
                                                .setPositiveButton(R.string.btn_use_suggestion, (d, w) -> {
                                                        tempModelConfig.putString(LanguageModelField.SubModel.name,
                                                                        suggested);
                                                        if (finalSubModelEditText != null) {
                                                                finalSubModelEditText.setText(suggested);
                                                        }
                                                        modelConfig.putAll(tempModelConfig);

                                                        // Save immediately to ContentProvider and notify listeners
                                                        getConfig().saveToProvider();

                                                        // Send broadcast to notify listeners of the change
                                                        android.content.Intent broadcastIntent = new android.content.Intent(
                                                                        tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
                                                        broadcastIntent.putExtra(
                                                                        tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_SELECTED_MODEL,
                                                                        getConfig().selectedModel.name());
                                                        broadcastIntent.putExtra(
                                                                        tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL,
                                                                        getConfig().languageModelsConfig);
                                                        getContext().sendBroadcast(broadcastIntent);

                                                        // Go back to model selection instead of closing
                                                        sheet.dismiss();
                                                        switchToDialog(DialogType.ChoseModel);
                                                })
                                                .setNegativeButton(R.string.btn_use_anyway, (d, w) -> {
                                                        modelConfig.putAll(tempModelConfig);

                                                        getConfig().saveToProvider();

                                                        android.content.Intent broadcastIntent = new android.content.Intent(
                                                                        tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
                                                        broadcastIntent.putExtra(
                                                                        tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_SELECTED_MODEL,
                                                                        getConfig().selectedModel.name());
                                                        broadcastIntent.putExtra(
                                                                        tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL,
                                                                        getConfig().languageModelsConfig);
                                                        getContext().sendBroadcast(broadcastIntent);

                                                        sheet.dismiss();
                                                        switchToDialog(DialogType.ChoseModel);
                                                })
                                                .setNeutralButton(R.string.cancel, null)
                                                .show();
                                return;
                        }

                        modelConfig.putAll(tempModelConfig);

                        getConfig().saveToProvider();

                        android.content.Intent broadcastIntent = new android.content.Intent(
                                        tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
                        broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_SELECTED_MODEL,
                                        getConfig().selectedModel.name());
                        broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL,
                                        getConfig().languageModelsConfig);
                        getContext().sendBroadcast(broadcastIntent);

                        sheet.dismiss();
                        switchToDialog(DialogType.ChoseModel);
                });

                // Tints
                tn.eluea.kgpt.core.ui.dialog.utils.DialogUiUtils.applyButtonTheme(themedContext, btnSave);

                // Back Header
                View btnBackHeader = layout.findViewById(R.id.btn_back_header);
                if (btnBackHeader != null) {
                        btnBackHeader.setOnClickListener(v -> {
                                sheet.dismiss();
                                switchToDialog(DialogType.ChoseModel);
                        });
                }

                sheet.setContentView(layout);
                return sheet;
        }

}
