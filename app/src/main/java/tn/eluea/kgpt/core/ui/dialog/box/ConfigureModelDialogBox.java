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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import tn.eluea.kgpt.llm.LanguageModelField;
import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

import android.content.Context;

public class ConfigureModelDialogBox extends DialogBox {

        // Valid model names for validation
        private static final Map<LanguageModel, Set<String>> VALID_MODELS = new HashMap<>();
        private static final Map<LanguageModel, String[]> MODEL_PRESETS = new HashMap<>();

        static {
                // Gemini models
                VALID_MODELS.put(LanguageModel.Gemini, new HashSet<>(Arrays.asList(
                                "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite",
                                "gemini-3-flash-preview", "gemini-3-pro-preview", "gemini-3-pro-image-preview",
                                "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-2.0-flash-001",
                                "gemini-2.0-flash-exp", "gemini-2.0-flash-lite-001",
                                "gemini-2.5-flash-preview-09-2025", "gemini-2.5-flash-lite-preview-09-2025",
                                "gemini-flash-latest", "gemini-flash-lite-latest", "gemini-pro-latest")));
                MODEL_PRESETS.put(LanguageModel.Gemini,
                                new String[] { "gemini-2.5-flash", "gemini-2.5-pro", "gemini-3-flash-preview" });

                // ChatGPT/OpenAI models (December 2025)
                VALID_MODELS.put(LanguageModel.ChatGPT, new HashSet<>(Arrays.asList(
                                // GPT-5 series (Latest)
                                "gpt-5.2", "gpt-5.1", "gpt-5", "gpt-5-mini", "gpt-5-nano",
                                "gpt-5.2-pro", "gpt-5-pro",
                                // GPT-4.1 series
                                "gpt-4.1", "gpt-4.1-mini", "gpt-4.1-nano",
                                // GPT-4o series
                                "gpt-4o", "gpt-4o-mini",
                                // Reasoning models
                                "o3", "o3-mini", "o3-pro", "o4-mini",
                                // Legacy
                                "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo")));
                MODEL_PRESETS.put(LanguageModel.ChatGPT,
                                new String[] { "gpt-5", "gpt-4o", "gpt-4.1", "o3-mini", "o4-mini" });

                // Groq models (December 2025)
                VALID_MODELS.put(LanguageModel.Groq, new HashSet<>(Arrays.asList(
                                // Production models
                                "llama-3.3-70b-versatile", "llama-3.1-8b-instant",
                                // OpenAI open-weight models
                                "openai/gpt-oss-120b", "openai/gpt-oss-20b",
                                // Llama 4 Preview
                                "meta-llama/llama-4-maverick-17b-128e-instruct",
                                "meta-llama/llama-4-scout-17b-16e-instruct",
                                // Compound systems
                                "groq/compound", "groq/compound-mini",
                                // Qwen
                                "qwen/qwen3-32b",
                                // Kimi
                                "moonshotai/kimi-k2-instruct-0905",
                                // Whisper
                                "whisper-large-v3", "whisper-large-v3-turbo")));
                MODEL_PRESETS.put(LanguageModel.Groq, new String[] { "llama-3.3-70b-versatile",
                                "meta-llama/llama-4-maverick-17b-128e-instruct", "groq/compound" });

                // OpenRouter - allow any model (supports 400+ models including free ones)
                VALID_MODELS.put(LanguageModel.OpenRouter, null);
                MODEL_PRESETS.put(LanguageModel.OpenRouter, new String[] {
                                "google/gemini-2.0-flash-exp:free",
                                "meta-llama/llama-3.3-70b-instruct:free",
                                "qwen/qwen-2.5-72b-instruct:free",
                                "deepseek/deepseek-chat-v3-0324:free",
                                "microsoft/phi-4:free"
                });

                // Claude/Anthropic models (December 2025)
                VALID_MODELS.put(LanguageModel.Claude, new HashSet<>(Arrays.asList(
                                // Claude 4.5 series (Latest)
                                "claude-opus-4-5-20250630", "claude-sonnet-4-5-20250630", "claude-haiku-4-5-20250630",
                                // Claude 4 series
                                "claude-opus-4-20250514", "claude-sonnet-4-20250514", "claude-opus-4-1-20250414",
                                // Claude 3.5 series
                                "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022",
                                // Claude 3 series (Legacy)
                                "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307")));
                MODEL_PRESETS.put(LanguageModel.Claude,
                                new String[] { "claude-opus-4-5-20250630", "claude-sonnet-4-5-20250630",
                                                "claude-haiku-4-5-20250630" });

                // Mistral models (December 2025)
                VALID_MODELS.put(LanguageModel.Mistral, new HashSet<>(Arrays.asList(
                                // Magistral series (Latest - Reasoning)
                                "magistral-medium-2507", "magistral-small-2507",
                                // Devstral (Coding)
                                "devstral-small-2505",
                                // Mistral Small 3.x
                                "mistral-small-2503", "mistral-small-2501", "mistral-small-latest",
                                // Codestral
                                "codestral-2501", "codestral-latest",
                                // Ministral
                                "ministral-3b-2410", "ministral-8b-2410",
                                // Pixtral (Vision)
                                "pixtral-12b-2409",
                                // OCR
                                "mistral-ocr-2503",
                                // Legacy
                                "mistral-large-latest", "open-mistral-7b", "open-mixtral-8x7b")));
                MODEL_PRESETS.put(LanguageModel.Mistral, new String[] { "magistral-medium-2507", "mistral-small-latest",
                                "devstral-small-2505", "codestral-latest" });
        }

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
                        String lowerInvalid = invalidName.toLowerCase();
                        for (String preset : presets) {
                                if (preset.toLowerCase().contains(lowerInvalid) ||
                                                lowerInvalid.contains(preset.toLowerCase().replace("-preview", ""))) {
                                        return preset;
                                }
                        }
                }
                return presets[0];
        }

        @Override
        protected Dialog build() {
                safeguardModelData();

                tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(
                                getContext());
                Context themedContext = sheet.getContext();

                Bundle modelConfig = getConfig().languageModelsConfig.getBundle(getConfig().selectedModel.name());
                if (modelConfig == null) {
                        throw new RuntimeException("No model " + getConfig().selectedModel.name());
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

                tvTitle.setText(getConfig().selectedModel.label + " Configuration");

                Bundle tempModelConfig = new Bundle();
                TextInputEditText subModelEditText = null;

                for (LanguageModelField field : LanguageModelField.values()) {
                        View fieldView = android.view.LayoutInflater.from(themedContext).inflate(
                                        R.layout.dialog_configure_model_field,
                                        fieldsContainer, false);
                        TextInputLayout inputLayout = fieldView.findViewById(R.id.field_layout);
                        TextInputEditText editText = fieldView.findViewById(R.id.field_edit);

                        inputLayout.setHint(field.title);
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
                                                .setTitle("Unknown Model Name")
                                                .setMessage("The model \"" + subModelValue
                                                                + "\" is not in our verified list.\nIt might be new or incorrect.\n\nDid you mean: "
                                                                + suggested + "?")
                                                .setPositiveButton("Use Suggestion", (d, w) -> {
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
                                                .setNegativeButton("Use Anyway", (d, w) -> {
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
                                                .setNeutralButton("Cancel", null)
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
