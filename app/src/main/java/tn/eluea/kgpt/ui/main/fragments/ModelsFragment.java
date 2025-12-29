package tn.eluea.kgpt.ui.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.main.adapters.ModelsAdapter;

public class ModelsFragment extends Fragment implements ModelsAdapter.OnModelSelectedListener {

    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_THEME = "theme_mode";

    private RecyclerView rvModels;
    private ChipGroup chipGroupSubmodels;
    private TextInputEditText etSubModel;
    private MaterialButton btnSave;
    private FrameLayout btnInfo;
    private View rootView;

    private ModelsAdapter adapter;
    private LanguageModel selectedModel;

    // Sub model presets for each provider (validated and working models)
    private static final Map<LanguageModel, String[]> SUB_MODEL_PRESETS = new HashMap<>();
    
    // All valid model names for validation
    private static final Map<LanguageModel, java.util.Set<String>> VALID_MODELS = new HashMap<>();
    
    static {
        // Gemini models - validated from Google API
        SUB_MODEL_PRESETS.put(LanguageModel.Gemini, new String[]{
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.5-flash-lite",
            "gemini-3-flash-preview",
            "gemini-3-pro-preview",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
        });
        VALID_MODELS.put(LanguageModel.Gemini, new java.util.HashSet<>(java.util.Arrays.asList(
            "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite",
            "gemini-3-flash-preview", "gemini-3-pro-preview", "gemini-3-pro-image-preview",
            "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-2.0-flash-001",
            "gemini-2.0-flash-exp", "gemini-2.0-flash-lite-001",
            "gemini-2.5-flash-preview-09-2025", "gemini-2.5-flash-lite-preview-09-2025",
            "gemini-flash-latest", "gemini-flash-lite-latest", "gemini-pro-latest"
        )));
        
        // ChatGPT models
        SUB_MODEL_PRESETS.put(LanguageModel.ChatGPT, new String[]{
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-4",
            "gpt-3.5-turbo"
        });
        VALID_MODELS.put(LanguageModel.ChatGPT, new java.util.HashSet<>(java.util.Arrays.asList(
            "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo",
            "gpt-4-turbo-preview", "gpt-4-0125-preview", "gpt-4-1106-preview"
        )));
        
        // Groq models
        SUB_MODEL_PRESETS.put(LanguageModel.Groq, new String[]{
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "llama3-70b-8192",
            "mixtral-8x7b-32768",
            "gemma2-9b-it"
        });
        VALID_MODELS.put(LanguageModel.Groq, new java.util.HashSet<>(java.util.Arrays.asList(
            "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "llama3-70b-8192",
            "llama3-8b-8192", "mixtral-8x7b-32768", "gemma2-9b-it", "gemma-7b-it"
        )));
        
        // OpenRouter models
        SUB_MODEL_PRESETS.put(LanguageModel.OpenRouter, new String[]{
            "google/gemini-2.0-flash-exp:free",
            "meta-llama/llama-3.2-3b-instruct:free",
            "mistralai/mistral-7b-instruct:free",
            "openai/gpt-4o-mini"
        });
        VALID_MODELS.put(LanguageModel.OpenRouter, null); // Allow any for OpenRouter
        
        // Claude models
        SUB_MODEL_PRESETS.put(LanguageModel.Claude, new String[]{
            "claude-sonnet-4-20250514",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229"
        });
        VALID_MODELS.put(LanguageModel.Claude, new java.util.HashSet<>(java.util.Arrays.asList(
            "claude-sonnet-4-20250514", "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307"
        )));
        
        // Mistral models
        SUB_MODEL_PRESETS.put(LanguageModel.Mistral, new String[]{
            "mistral-large-latest",
            "mistral-medium-latest",
            "mistral-small-latest",
            "open-mistral-7b",
            "open-mixtral-8x7b"
        });
        VALID_MODELS.put(LanguageModel.Mistral, new java.util.HashSet<>(java.util.Arrays.asList(
            "mistral-large-latest", "mistral-medium-latest", "mistral-small-latest",
            "open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b"
        )));
    }
    
    /**
     * Check if a model name is valid for the given provider
     */
    private boolean isValidModelName(LanguageModel model, String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return false;
        }
        
        java.util.Set<String> validSet = VALID_MODELS.get(model);
        if (validSet == null) {
            // Allow any model name for providers without validation (like OpenRouter)
            return true;
        }
        
        return validSet.contains(modelName.trim());
    }
    
    /**
     * Get suggested model name if the entered one is invalid
     */
    private String getSuggestedModel(LanguageModel model, String invalidName) {
        if (invalidName == null) return model.getDefault(LanguageModelField.SubModel);
        
        String[] presets = SUB_MODEL_PRESETS.get(model);
        if (presets == null || presets.length == 0) {
            return model.getDefault(LanguageModelField.SubModel);
        }
        
        // Try to find a similar model name
        String lowerInvalid = invalidName.toLowerCase();
        for (String preset : presets) {
            if (preset.toLowerCase().contains(lowerInvalid) || 
                lowerInvalid.contains(preset.toLowerCase().replace("-preview", ""))) {
                return preset;
            }
        }
        
        // Return first preset as default
        return presets[0];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_models, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        initViews(view);
        applyAmoledIfNeeded();
        setupRecyclerView();
        setupSaveButton();
        setupInfoButton();
        loadCurrentSettings();
    }

    private void initViews(View view) {
        rvModels = view.findViewById(R.id.rv_models);
        chipGroupSubmodels = view.findViewById(R.id.chip_group_submodels);
        etSubModel = view.findViewById(R.id.et_sub_model);
        btnSave = view.findViewById(R.id.btn_save);
        btnInfo = view.findViewById(R.id.btn_info);
    }

    private void applyAmoledIfNeeded() {
        SharedPreferences prefs = requireContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = prefs.getBoolean(PREF_AMOLED, false);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);

        if (isDarkMode && isAmoled) {
            if (rootView instanceof ViewGroup) {
                View scrollContent = ((ViewGroup) rootView).getChildAt(0);
                if (scrollContent != null) {
                    scrollContent.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
                }
            }
            rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
            applyAmoledToCards(rootView);
        }
    }

    private void applyAmoledToCards(View view) {
        if (view instanceof MaterialCardView) {
            ((MaterialCardView) view).setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.surface_amoled)
            );
            ((MaterialCardView) view).setStrokeColor(
                ContextCompat.getColor(requireContext(), R.color.divider_dark)
            );
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAmoledToCards(group.getChildAt(i));
            }
        }
    }

    private void setupInfoButton() {
        btnInfo.setOnClickListener(v -> showInfoBottomSheet());
    }

    private void showInfoBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_info, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        // Set content
        TextView tvTitle = sheetView.findViewById(R.id.tv_info_title);
        TextView tvDescription = sheetView.findViewById(R.id.tv_info_description);
        
        tvTitle.setText("About AI Models");
        tvDescription.setText("Choose your preferred AI provider and model.\n\n" +
                "• Select Provider: Tap on a model card to select it (Gemini, ChatGPT, Claude, etc.)\n\n" +
                "• Sub Model: Choose a preset sub-model or enter a custom model name.\n\n" +
                "• Save: Don't forget to tap 'Save Configuration' to apply your changes.\n\n" +
                "Note: Make sure you have the API key for your selected provider in the API Keys section.");

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupRecyclerView() {
        List<LanguageModel> models = Arrays.asList(LanguageModel.values());
        
        if (SPManager.isReady()) {
            selectedModel = SPManager.getInstance().getLanguageModel();
        } else {
            selectedModel = LanguageModel.Gemini;
        }
        
        adapter = new ModelsAdapter(models, selectedModel, this);
        rvModels.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvModels.setAdapter(adapter);
    }

    private void setupSubModelChips(LanguageModel model) {
        chipGroupSubmodels.removeAllViews();
        
        String[] presets = SUB_MODEL_PRESETS.get(model);
        if (presets == null) return;

        String currentSubModel = "";
        if (SPManager.isReady()) {
            currentSubModel = SPManager.getInstance().getSubModel(model);
        }
        if (currentSubModel == null || currentSubModel.isEmpty()) {
            currentSubModel = model.getDefault(LanguageModelField.SubModel);
        }

        for (String preset : presets) {
            Chip chip = new Chip(requireContext());
            chip.setText(preset);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setTextColor(getResources().getColorStateList(R.color.chip_text_selector, requireContext().getTheme()));
            chip.setChipStrokeColorResource(R.color.divider_color);
            chip.setChipStrokeWidth(1f);
            
            if (preset.equals(currentSubModel)) {
                chip.setChecked(true);
            }
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    etSubModel.setText(preset);
                }
            });
            
            chipGroupSubmodels.addView(chip);
        }
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveConfiguration());
    }

    private void loadCurrentSettings() {
        if (selectedModel != null) {
            loadModelSettings(selectedModel);
        }
    }

    private void loadModelSettings(LanguageModel model) {
        setupSubModelChips(model);
        
        if (!SPManager.isReady()) {
            etSubModel.setText(model.getDefault(LanguageModelField.SubModel));
            return;
        }
        
        SPManager sp = SPManager.getInstance();

        String subModel = sp.getSubModel(model);
        if (subModel == null || subModel.isEmpty()) {
            subModel = model.getDefault(LanguageModelField.SubModel);
        }
        etSubModel.setText(subModel);
    }

    @Override
    public void onModelSelected(LanguageModel model) {
        selectedModel = model;
        loadModelSettings(model);
    }

    private void saveConfiguration() {
        if (selectedModel == null) {
            Toast.makeText(requireContext(), "Please select a model", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!SPManager.isReady()) {
            Toast.makeText(requireContext(), "Settings not available", Toast.LENGTH_SHORT).show();
            return;
        }

        SPManager sp = SPManager.getInstance();

        // Save selected model
        sp.setLanguageModel(selectedModel);

        // Validate and save sub model
        String subModel = etSubModel.getText() != null ? etSubModel.getText().toString().trim() : "";
        
        if (subModel.isEmpty()) {
            subModel = selectedModel.getDefault(LanguageModelField.SubModel);
        }
        
        final String finalSubModel = subModel;
        
        // Validate model name
        if (!isValidModelName(selectedModel, finalSubModel)) {
            String suggested = getSuggestedModel(selectedModel, finalSubModel);
            
            // Show warning dialog
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Invalid Model Name")
                .setMessage("The model \"" + finalSubModel + "\" may not be valid.\n\nDid you mean: " + suggested + "?\n\nUsing an invalid model name will cause API errors.")
                .setPositiveButton("Use Suggested", (dialog, which) -> {
                    etSubModel.setText(suggested);
                    sp.setSubModel(selectedModel, suggested);
                    sendConfigBroadcast();
                    Toast.makeText(requireContext(), "Configuration saved with " + suggested, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Use Anyway", (dialog, which) -> {
                    sp.setSubModel(selectedModel, finalSubModel);
                    sendConfigBroadcast();
                    Toast.makeText(requireContext(), "Configuration saved (model may not work)", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Cancel", null)
                .show();
            return;
        }
        
        sp.setSubModel(selectedModel, finalSubModel);

        // Send broadcast to Xposed module to sync settings
        sendConfigBroadcast();

        Toast.makeText(requireContext(), "Configuration saved", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Send broadcast to Xposed module to sync configuration
     */
    private void sendConfigBroadcast() {
        if (!SPManager.isReady()) return;
        
        SPManager sp = SPManager.getInstance();
        
        Intent broadcastIntent = new Intent("tn.eluea.kgpt.DIALOG_RESULT");
        
        // Add selected model
        broadcastIntent.putExtra("tn.eluea.kgpt.config.SELECTED_MODEL", selectedModel.name());
        
        // Add all model configurations
        broadcastIntent.putExtra("tn.eluea.kgpt.config.model", sp.getConfigBundle());
        
        requireContext().sendBroadcast(broadcastIntent);
    }
}
