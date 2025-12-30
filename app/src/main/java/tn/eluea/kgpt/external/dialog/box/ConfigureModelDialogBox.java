package tn.eluea.kgpt.external.dialog.box;

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
import tn.eluea.kgpt.external.ConfigContainer;
import tn.eluea.kgpt.external.dialog.DialogBoxManager;
import tn.eluea.kgpt.external.dialog.DialogType;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;

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
            "gemini-flash-latest", "gemini-flash-lite-latest", "gemini-pro-latest"
        )));
        MODEL_PRESETS.put(LanguageModel.Gemini, new String[]{"gemini-2.5-flash", "gemini-2.5-pro", "gemini-3-flash-preview"});
        
        // ChatGPT models
        VALID_MODELS.put(LanguageModel.ChatGPT, new HashSet<>(Arrays.asList(
            "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo",
            "gpt-4-turbo-preview", "gpt-4-0125-preview", "gpt-4-1106-preview"
        )));
        MODEL_PRESETS.put(LanguageModel.ChatGPT, new String[]{"gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"});
        
        // Groq models
        VALID_MODELS.put(LanguageModel.Groq, new HashSet<>(Arrays.asList(
            "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "llama3-70b-8192",
            "llama3-8b-8192", "mixtral-8x7b-32768", "gemma2-9b-it", "gemma-7b-it"
        )));
        MODEL_PRESETS.put(LanguageModel.Groq, new String[]{"llama-3.3-70b-versatile", "llama-3.1-8b-instant"});
        
        // OpenRouter - allow any
        VALID_MODELS.put(LanguageModel.OpenRouter, null);
        MODEL_PRESETS.put(LanguageModel.OpenRouter, new String[]{"google/gemini-2.0-flash-exp:free"});
        
        // Claude models
        VALID_MODELS.put(LanguageModel.Claude, new HashSet<>(Arrays.asList(
            "claude-sonnet-4-20250514", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307"
        )));
        MODEL_PRESETS.put(LanguageModel.Claude, new String[]{"claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022"});
        
        // Mistral models
        VALID_MODELS.put(LanguageModel.Mistral, new HashSet<>(Arrays.asList(
            "mistral-large-latest", "mistral-medium-latest", "mistral-small-latest",
            "open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b"
        )));
        MODEL_PRESETS.put(LanguageModel.Mistral, new String[]{"mistral-small-latest", "mistral-large-latest"});
    }
    
    public ConfigureModelDialogBox(DialogBoxManager dialogManager, Activity parent,
                                   Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }
    
    private boolean isValidModelName(LanguageModel model, String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) return false;
        Set<String> validSet = VALID_MODELS.get(model);
        if (validSet == null) return true; // Allow any for OpenRouter
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

        Bundle modelConfig = getConfig().languageModelsConfig.getBundle(getConfig().selectedModel.name());
        if (modelConfig == null) {
            throw new RuntimeException("No model " + getConfig().selectedModel.name());
        }

        View layout = getParent().getLayoutInflater().inflate(R.layout.dialog_configue_model, null);
        LinearLayout fieldsContainer = layout.findViewById(R.id.fields_container);
        TextView tvTitle = layout.findViewById(R.id.tv_title);
        MaterialButton btnCancel = layout.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = layout.findViewById(R.id.btn_save);
        ImageView headerIcon = layout.findViewById(R.id.iv_header_icon);
        
        int iconColor = ContextCompat.getColor(getContext(), R.color.primary);
        if (headerIcon != null) headerIcon.setColorFilter(iconColor);
        
        tvTitle.setText(getConfig().selectedModel.label + " Configuration");

        Bundle tempModelConfig = new Bundle();
        TextInputEditText subModelEditText = null;
        
        for (LanguageModelField field : LanguageModelField.values()) {
            View fieldView = getParent().getLayoutInflater().inflate(R.layout.dialog_configure_model_field, fieldsContainer, false);
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
                public void afterTextChanged(Editable s) {}
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tempModelConfig.putString(field.name, s.toString());
                }
            });
            
            fieldsContainer.addView(fieldView);
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(layout)
                .create();
        
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            switchToDialog(DialogType.ChoseModel);
        });
        
        final TextInputEditText finalSubModelEditText = subModelEditText;
        btnSave.setOnClickListener(v -> {
            // Validate SubModel before saving
            String subModelValue = tempModelConfig.getString(LanguageModelField.SubModel.name);
            if (subModelValue == null && finalSubModelEditText != null) {
                subModelValue = finalSubModelEditText.getText() != null ? 
                    finalSubModelEditText.getText().toString() : "";
            }
            
            if (subModelValue != null && !subModelValue.isEmpty() && 
                !isValidModelName(getConfig().selectedModel, subModelValue)) {
                
                String suggested = getSuggestedModel(getConfig().selectedModel, subModelValue);
                
                new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                    .setTitle("Invalid Model Name")
                    .setMessage("\"" + subModelValue + "\" may not be valid.\n\nSuggested: " + suggested)
                    .setPositiveButton("Use Suggested", (d, w) -> {
                        tempModelConfig.putString(LanguageModelField.SubModel.name, suggested);
                        if (finalSubModelEditText != null) {
                            finalSubModelEditText.setText(suggested);
                        }
                        modelConfig.putAll(tempModelConfig);
                        
                        // Save immediately to ContentProvider and notify listeners
                        getConfig().saveToProvider();
                        
                        // Send broadcast to notify listeners of the change
                        android.content.Intent broadcastIntent = new android.content.Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
                        broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_SELECTED_MODEL, getConfig().selectedModel.name());
                        broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL, getConfig().languageModelsConfig);
                        getContext().sendBroadcast(broadcastIntent);
                        
                        // Go back to model selection instead of closing
                        switchToDialog(DialogType.ChoseModel);
                    })
                    .setNegativeButton("Use Anyway", (d, w) -> {
                        modelConfig.putAll(tempModelConfig);
                        
                        // Save immediately to ContentProvider and notify listeners
                        getConfig().saveToProvider();
                        
                        // Send broadcast to notify listeners of the change
                        android.content.Intent broadcastIntent = new android.content.Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
                        broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_SELECTED_MODEL, getConfig().selectedModel.name());
                        broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL, getConfig().languageModelsConfig);
                        getContext().sendBroadcast(broadcastIntent);
                        
                        // Go back to model selection instead of closing
                        switchToDialog(DialogType.ChoseModel);
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
                return;
            }
            
            modelConfig.putAll(tempModelConfig);
            
            // Save immediately to ContentProvider and notify listeners
            getConfig().saveToProvider();
            
            // Send broadcast to notify listeners of the change
            android.content.Intent broadcastIntent = new android.content.Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_SELECTED_MODEL, getConfig().selectedModel.name());
            broadcastIntent.putExtra(tn.eluea.kgpt.ui.UiInteractor.EXTRA_CONFIG_LANGUAGE_MODEL, getConfig().languageModelsConfig);
            getContext().sendBroadcast(broadcastIntent);
            
            // Go back to model selection instead of closing
            switchToDialog(DialogType.ChoseModel);
        });

        return dialog;
    }

}
