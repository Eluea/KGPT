/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.ui.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.llm.client.CustomProviderClient;
import tn.eluea.kgpt.llm.model.CustomProvider;
import tn.eluea.kgpt.llm.model.CustomProviderManager;
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
    private View rootView;

    private ModelsAdapter adapter;
    private LanguageModel selectedModel;
    private CustomProvider selectedCustomProvider;

    // Sub model presets for each provider (validated and working models)
    private static final Map<LanguageModel, String[]> SUB_MODEL_PRESETS = new HashMap<>();

    // All valid model names for validation
    private static final Map<LanguageModel, java.util.Set<String>> VALID_MODELS = new HashMap<>();

    static {
        // Gemini models - August 2026 latest (Free via Google AI Studio)
        SUB_MODEL_PRESETS.put(LanguageModel.Gemini, new String[] {
                "gemini-3.7-flash",
                "gemini-3.6-flash",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite",
                "gemini-2.5-flash",
                "gemini-2.5-pro"
        });
        VALID_MODELS.put(LanguageModel.Gemini, new java.util.HashSet<>(java.util.Arrays.asList(
                "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite",
                "gemini-3.1-pro-preview", "gemini-3.1-flash-lite",
                "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite",
                "gemini-2.0-flash", "gemini-2.0-flash-lite")));

        // ChatGPT models - August 2026 latest (GPT-5.6 generation)
        SUB_MODEL_PRESETS.put(LanguageModel.ChatGPT, new String[] {
                "gpt-5.6-sol",
                "gpt-5.6-terra",
                "gpt-5.6-luna",
                "gpt-5.6-cyber",
                "gpt-5",
                "gpt-oss-120b",
                "gpt-oss-20b"
        });
        VALID_MODELS.put(LanguageModel.ChatGPT, new java.util.HashSet<>(java.util.Arrays.asList(
                "gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna", "gpt-5.6-cyber",
                "gpt-5", "gpt-4o", "gpt-4o-mini",
                "gpt-oss-120b", "gpt-oss-20b")));

        // Groq models - August 2026 latest (Free LPU inference)
        SUB_MODEL_PRESETS.put(LanguageModel.Groq, new String[] {
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "qwen/qwen3-vl-32b-instruct",
                "minimaxai/minimax-m2.5",
                "groq/compound"
        });
        VALID_MODELS.put(LanguageModel.Groq, null); // Allow any - Groq catalog changes frequently

        // OpenRouter models - August 2026 (Free tier available)
        SUB_MODEL_PRESETS.put(LanguageModel.OpenRouter, new String[] {
                "openrouter/free",
                "nvidia/nemotron-3-ultra:free",
                "openai/gpt-oss-120b:free",
                "qwen/qwen3-coder:free",
                "google/gemini-2.5-flash:free",
                "openai/gpt-4o-mini"
        });
        VALID_MODELS.put(LanguageModel.OpenRouter, null); // Allow any for OpenRouter

        // Claude models - August 2026 latest (Claude 5 generation)
        SUB_MODEL_PRESETS.put(LanguageModel.Claude, new String[] {
                "claude-opus-5",
                "claude-fable-5",
                "claude-sonnet-5",
                "claude-haiku-4-5-20251001"
        });
        VALID_MODELS.put(LanguageModel.Claude, new java.util.HashSet<>(java.util.Arrays.asList(
                "claude-opus-5", "claude-fable-5", "claude-sonnet-5",
                "claude-haiku-4-5-20251001", "claude-haiku-4-5",
                "claude-sonnet-4-20250514", "claude-opus-4-5-20250630",
                "claude-sonnet-4-5-20250630")));

        // Mistral models - August 2026 latest
        SUB_MODEL_PRESETS.put(LanguageModel.Mistral, new String[] {
                "mistral-large-latest",
                "mistral-medium-3.5",
                "mistral-small-latest",
                "codestral-latest",
                "ministral-3-latest"
        });
        VALID_MODELS.put(LanguageModel.Mistral, new java.util.HashSet<>(java.util.Arrays.asList(
                "mistral-large-latest", "mistral-medium-3.5", "mistral-small-latest",
                "codestral-latest", "ministral-3-latest",
                "mistral-small-2506", "codestral-2501")));

        // Chutes models - August 2026 latest
        SUB_MODEL_PRESETS.put(LanguageModel.Chutes, new String[] {
                "deepseek-ai/DeepSeek-V3.2",
                "deepseek-ai/DeepSeek-V4-Flash-0731",
                "deepseek-ai/DeepSeek-R1",
                "meta-llama/Llama-3.3-70B-Instruct",
                "Qwen/Qwen2.5-72B-Instruct"
        });
        VALID_MODELS.put(LanguageModel.Chutes, null); // Allow any model

        // Perplexity models - August 2026 latest (Search-grounded)
        SUB_MODEL_PRESETS.put(LanguageModel.Perplexity, new String[] {
                "sonar-pro",
                "sonar",
                "sonar-reasoning-pro",
                "sonar-deep-research"
        });
        VALID_MODELS.put(LanguageModel.Perplexity, new java.util.HashSet<>(java.util.Arrays.asList(
                "sonar-pro", "sonar", "sonar-reasoning-pro", "sonar-deep-research")));

        // GLM (ZhipuAI / Z.ai) models - August 2026 latest (GLM-5 generation)
        SUB_MODEL_PRESETS.put(LanguageModel.GLM, new String[] {
                "glm-5.3",
                "glm-5.2",
                "glm-5.1",
                "glm-5",
                "glm-4-plus",
                "glm-4-flash"
        });
        VALID_MODELS.put(LanguageModel.GLM, new java.util.HashSet<>(java.util.Arrays.asList(
                "glm-5.3", "glm-5.2", "glm-5.1", "glm-5",
                "glm-4-plus", "glm-4-flash", "glm-4-air")));

        // Grok (xAI) models - August 2026 latest
        SUB_MODEL_PRESETS.put(LanguageModel.Grok, new String[] {
                "grok-4.6",
                "grok-4.5",
                "grok-4.3",
                "grok-4",
                "grok-3",
                "grok-2-latest",
                "grok-2",
                "grok-beta"
        });
        VALID_MODELS.put(LanguageModel.Grok, new java.util.HashSet<>(java.util.Arrays.asList(
                "grok-4.6", "grok-4.5", "grok-4.3", "grok-4", "grok-3",
                "grok-2-latest", "grok-2", "grok-2-vision-1212", "grok-beta")));

        // DeepSeek models - August 2026 latest
        SUB_MODEL_PRESETS.put(LanguageModel.DeepSeek, new String[] {
                "deepseek-chat",
                "deepseek-reasoner",
                "deepseek-v4-pro",
                "deepseek-v4-flash",
                "deepseek-coder"
        });
        VALID_MODELS.put(LanguageModel.DeepSeek, new java.util.HashSet<>(java.util.Arrays.asList(
                "deepseek-chat", "deepseek-reasoner", "deepseek-v4-pro",
                "deepseek-v4-flash", "deepseek-coder",
                "deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-R1")));

        // Kimi (Moonshot AI) models - August 2026 latest
        SUB_MODEL_PRESETS.put(LanguageModel.Kimi, new String[] {
                "kimi-k3",
                "kimi-k2.6",
                "kimi-k2.7-code",
                "kimi-k2.5",
                "moonshot-v1-auto",
                "moonshot-v1-8k",
                "moonshot-v1-32k",
                "moonshot-v1-128k"
        });
        VALID_MODELS.put(LanguageModel.Kimi, new java.util.HashSet<>(java.util.Arrays.asList(
                "kimi-k3", "kimi-k2.6", "kimi-k2.7-code", "kimi-k2.5", "kimi-k2-instruct",
                "moonshot-v1-auto", "moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")));
    }

    private boolean isValidModelName(LanguageModel model, String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return false;
        }

        java.util.Set<String> validSet = VALID_MODELS.get(model);
        if (validSet == null) {
            return true;
        }

        return validSet.contains(modelName.trim());
    }

    private String getSuggestedModel(LanguageModel model, String invalidName) {
        if (invalidName == null)
            return model.getDefault(LanguageModelField.SubModel);

        String[] presets = SUB_MODEL_PRESETS.get(model);
        if (presets == null || presets.length == 0) {
            return model.getDefault(LanguageModelField.SubModel);
        }

        String lowerInvalid = invalidName.toLowerCase(java.util.Locale.ROOT);
        for (String preset : presets) {
            if (preset.toLowerCase(java.util.Locale.ROOT).contains(lowerInvalid) ||
                    lowerInvalid.contains(preset.toLowerCase(java.util.Locale.ROOT).replace("-preview", ""))) {
                return preset;
            }
        }

        return presets[0];
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
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
        loadCurrentSettings();
    }

    private void initViews(View view) {
        rvModels = view.findViewById(R.id.rv_models);
        chipGroupSubmodels = view.findViewById(R.id.chip_group_submodels);
        etSubModel = view.findViewById(R.id.et_sub_model);
        btnSave = view.findViewById(R.id.btn_save);

        MaterialCardView cardAddCustomProvider = view.findViewById(R.id.card_add_custom_provider);
        if (cardAddCustomProvider != null) {
            cardAddCustomProvider.setOnClickListener(v -> showAddOrEditCustomProviderDialog(null));
        }
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
                    ContextCompat.getColor(requireContext(), R.color.surface_amoled));
            ((MaterialCardView) view).setStrokeColor(
                    ContextCompat.getColor(requireContext(), R.color.divider_dark));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAmoledToCards(group.getChildAt(i));
            }
        }
    }

    private void setupRecyclerView() {
        List<LanguageModel> models = Arrays.asList(LanguageModel.values());
        List<CustomProvider> customProviders = CustomProviderManager.getInstance().getCustomProviders();

        if (CustomProviderManager.getInstance().isCustomProviderSelected()) {
            selectedCustomProvider = CustomProviderManager.getInstance().getSelectedCustomProvider();
            selectedModel = null;
        } else {
            selectedModel = SPManager.isReady() ? SPManager.getInstance().getLanguageModel() : LanguageModel.Gemini;
            selectedCustomProvider = null;
        }

        String customId = selectedCustomProvider != null ? selectedCustomProvider.getId() : null;
        adapter = new ModelsAdapter(models, customProviders, selectedModel, customId, this);
        rvModels.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        rvModels.setAdapter(adapter);
    }

    private void refreshData() {
        List<LanguageModel> models = Arrays.asList(LanguageModel.values());
        List<CustomProvider> customProviders = CustomProviderManager.getInstance().getCustomProviders();
        String customId = selectedCustomProvider != null ? selectedCustomProvider.getId() : null;
        if (adapter != null) {
            adapter.updateData(models, customProviders, selectedModel, customId);
        }
    }

    private void setupSubModelChips(LanguageModel model) {
        chipGroupSubmodels.removeAllViews();

        String[] presets = SUB_MODEL_PRESETS.get(model);
        if (presets == null)
            return;

        String currentSubModel = "";
        if (SPManager.isReady()) {
            currentSubModel = SPManager.getInstance().getSubModel(model);
        }
        if (currentSubModel == null || currentSubModel.isEmpty()) {
            currentSubModel = model.getDefault(LanguageModelField.SubModel);
        }

        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(rootView,
                androidx.appcompat.R.attr.colorPrimary);
        int colorOnPrimary = com.google.android.material.color.MaterialColors.getColor(rootView,
                com.google.android.material.R.attr.colorOnPrimary);
        int colorSurfaceContainerHigh = com.google.android.material.color.MaterialColors.getColor(rootView,
                com.google.android.material.R.attr.colorSurfaceContainerHigh);
        int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(rootView,
                com.google.android.material.R.attr.colorOnSurface);
        int colorDivider = ContextCompat.getColor(requireContext(), R.color.divider_color);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
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
            Chip chip = new Chip(requireContext());
            chip.setText(preset);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(bgStateList);
            chip.setTextColor(textStateList);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(colorDivider));
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

    private void setupSubModelChipsForCustom(CustomProvider provider) {
        chipGroupSubmodels.removeAllViews();
        if (provider == null) return;

        List<String> models = provider.getModels();
        String currentSubModel = CustomProviderManager.getInstance().getCustomProviderSubModel(provider.getId());
        if (currentSubModel == null || currentSubModel.isEmpty()) {
            currentSubModel = provider.getDefaultModel();
        }

        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(rootView,
                androidx.appcompat.R.attr.colorPrimary);
        int colorOnPrimary = com.google.android.material.color.MaterialColors.getColor(rootView,
                com.google.android.material.R.attr.colorOnPrimary);
        int colorSurfaceContainerHigh = com.google.android.material.color.MaterialColors.getColor(rootView,
                com.google.android.material.R.attr.colorSurfaceContainerHigh);
        int colorOnSurface = com.google.android.material.color.MaterialColors.getColor(rootView,
                com.google.android.material.R.attr.colorOnSurface);
        int colorDivider = ContextCompat.getColor(requireContext(), R.color.divider_color);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
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

        for (String m : models) {
            Chip chip = new Chip(requireContext());
            chip.setText(m);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(bgStateList);
            chip.setTextColor(textStateList);
            chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(colorDivider));
            chip.setChipStrokeWidth(1f);

            if (m.equals(currentSubModel)) {
                chip.setChecked(true);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    etSubModel.setText(m);
                }
            });

            chipGroupSubmodels.addView(chip);
        }
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveConfiguration());
    }

    private void loadCurrentSettings() {
        if (selectedCustomProvider != null) {
            loadCustomProviderSettings(selectedCustomProvider);
        } else if (selectedModel != null) {
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

    private void loadCustomProviderSettings(CustomProvider provider) {
        setupSubModelChipsForCustom(provider);
        String subModel = CustomProviderManager.getInstance().getCustomProviderSubModel(provider.getId());
        if (subModel == null || subModel.isEmpty()) {
            subModel = provider.getDefaultModel();
        }
        etSubModel.setText(subModel);
    }

    @Override
    public void onBuiltinModelSelected(LanguageModel model) {
        selectedModel = model;
        selectedCustomProvider = null;
        CustomProviderManager.getInstance().setSelectedProviderType(CustomProviderManager.TYPE_BUILTIN);
        loadModelSettings(model);
    }

    @Override
    public void onCustomProviderSelected(CustomProvider provider) {
        selectedCustomProvider = provider;
        selectedModel = null;
        CustomProviderManager.getInstance().setSelectedCustomProviderId(provider.getId());
        loadCustomProviderSettings(provider);
    }

    @Override
    public void onEditCustomProvider(CustomProvider provider) {
        showAddOrEditCustomProviderDialog(provider);
    }

    private void showAddOrEditCustomProviderDialog(@Nullable CustomProvider existing) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add_custom_provider, null);
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        ViewGroup bottomSheetContainer = sheetView.findViewById(R.id.bottom_sheet_container);

        TextView tvTitle = sheetView.findViewById(R.id.tv_dialog_title);
        TextInputEditText etName = sheetView.findViewById(R.id.et_provider_name);
        TextInputEditText etBaseUrl = sheetView.findViewById(R.id.et_base_url);
        TextInputEditText etChatEndpoint = sheetView.findViewById(R.id.et_chat_endpoint);
        TextInputEditText etAddModel = sheetView.findViewById(R.id.et_add_model);
        MaterialCardView btnAddModelTag = sheetView.findViewById(R.id.btn_add_model_tag);
        LottieAnimationView lottieBtnAddModel = sheetView.findViewById(R.id.lottie_btn_add_model);
        ChipGroup chipGroupModels = sheetView.findViewById(R.id.chip_group_models);
        TextInputEditText etDefaultModel = sheetView.findViewById(R.id.et_default_model);

        // Auth Selection Cards
        MaterialCardView cardAuthBearer = sheetView.findViewById(R.id.card_auth_bearer);
        MaterialCardView cardAuthCustomHeader = sheetView.findViewById(R.id.card_auth_custom_header);
        MaterialCardView cardAuthQueryParam = sheetView.findViewById(R.id.card_auth_query_param);
        MaterialCardView cardAuthNone = sheetView.findViewById(R.id.card_auth_none);

        ImageView ivCheckBearer = sheetView.findViewById(R.id.iv_check_bearer);
        ImageView ivCheckCustomHeader = sheetView.findViewById(R.id.iv_check_custom_header);
        ImageView ivCheckQueryParam = sheetView.findViewById(R.id.iv_check_query_param);
        ImageView ivCheckNone = sheetView.findViewById(R.id.iv_check_none);

        TextView tvAuthBearerTitle = sheetView.findViewById(R.id.tv_auth_bearer_title);
        TextView tvAuthBearerSub = sheetView.findViewById(R.id.tv_auth_bearer_sub);
        TextView tvAuthCustomHeaderTitle = sheetView.findViewById(R.id.tv_auth_custom_header_title);
        TextView tvAuthCustomHeaderSub = sheetView.findViewById(R.id.tv_auth_custom_header_sub);
        TextView tvAuthQueryParamTitle = sheetView.findViewById(R.id.tv_auth_query_param_title);
        TextView tvAuthQueryParamSub = sheetView.findViewById(R.id.tv_auth_query_param_sub);
        TextView tvAuthNoneTitle = sheetView.findViewById(R.id.tv_auth_none_title);
        TextView tvAuthNoneSub = sheetView.findViewById(R.id.tv_auth_none_sub);

        LinearLayout layoutCustomHeaderFields = sheetView.findViewById(R.id.layout_custom_header_fields);
        LinearLayout layoutQueryParamFields = sheetView.findViewById(R.id.layout_query_param_fields);
        TextInputEditText etCustomHeaderName = sheetView.findViewById(R.id.et_custom_header_name);
        TextInputEditText etCustomAuthPrefix = sheetView.findViewById(R.id.et_custom_auth_prefix);
        TextInputEditText etQueryParamName = sheetView.findViewById(R.id.et_query_param_name);

        TextInputEditText etApiKey = sheetView.findViewById(R.id.et_api_key);
        LottieAnimationView lottieEyeToggle = sheetView.findViewById(R.id.lottie_eye_toggle);

        // Template buttons
        MaterialButton btnTplOllama = sheetView.findViewById(R.id.btn_tpl_ollama);
        MaterialButton btnTplLmstudio = sheetView.findViewById(R.id.btn_tpl_lmstudio);
        MaterialButton btnTplTogether = sheetView.findViewById(R.id.btn_tpl_together);
        MaterialButton btnTplVllm = sheetView.findViewById(R.id.btn_tpl_vllm);
        MaterialButton btnTplDeepinfra = sheetView.findViewById(R.id.btn_tpl_deepinfra);
        MaterialButton[] templateButtons = new MaterialButton[]{btnTplOllama, btnTplLmstudio, btnTplTogether, btnTplVllm, btnTplDeepinfra};

        // Tabs
        com.google.android.material.tabs.TabLayout tabLayout = sheetView.findViewById(R.id.tab_layout_provider);
        LinearLayout tabContentServer = sheetView.findViewById(R.id.tab_content_server);
        LinearLayout tabContentModelsAuth = sheetView.findViewById(R.id.tab_content_models_auth);
        LinearLayout tabContentAdvancedTest = sheetView.findViewById(R.id.tab_content_advanced_test);

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int pos = tab.getPosition();
                tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                tabContentServer.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                tabContentModelsAuth.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                tabContentAdvancedTest.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        if (tabLayout.getTabCount() > 0) {
            com.google.android.material.tabs.TabLayout.Tab firstTab = tabLayout.getTabAt(0);
            if (firstTab != null) {
                firstTab.select();
            }
        }
        tabContentServer.setVisibility(View.VISIBLE);
        tabContentModelsAuth.setVisibility(View.GONE);
        tabContentAdvancedTest.setVisibility(View.GONE);

        // Color helper attributes
        int colorPrimary = MaterialColors.getColor(sheetView, androidx.appcompat.R.attr.colorPrimary, Color.CYAN);
        int colorOnPrimary = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        int colorPrimaryContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorPrimaryContainer, Color.DKGRAY);
        int colorOnPrimaryContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.WHITE);
        int colorOnSurface = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
        int colorOnSurfaceVariant = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
        int colorOutlineVariant = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY);

        // Auth state tracking
        final CustomProvider.AuthType[] currentAuthType = new CustomProvider.AuthType[]{CustomProvider.AuthType.BEARER_TOKEN};

        java.util.function.BiConsumer<CustomProvider.AuthType, Boolean> updateAuthCardsUi = (selectedType, animate) -> {
            currentAuthType[0] = selectedType;
            if (animate) {
                tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
            }

            int strokeWidth1dp = Math.round(1 * sheetView.getResources().getDisplayMetrics().density);

            // Bearer Card
            boolean isBearer = (selectedType == CustomProvider.AuthType.BEARER_TOKEN);
            cardAuthBearer.setCardBackgroundColor(isBearer ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthBearer.setStrokeWidth(isBearer ? 0 : strokeWidth1dp);
            cardAuthBearer.setStrokeColor(isBearer ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckBearer.setVisibility(isBearer ? View.VISIBLE : View.GONE);
            tvAuthBearerTitle.setTextColor(isBearer ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthBearerSub.setTextColor(isBearer ? colorOnPrimaryContainer : colorOnSurfaceVariant);

            // Custom Header Card
            boolean isCustomHeader = (selectedType == CustomProvider.AuthType.CUSTOM_HEADER);
            cardAuthCustomHeader.setCardBackgroundColor(isCustomHeader ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthCustomHeader.setStrokeWidth(isCustomHeader ? 0 : strokeWidth1dp);
            cardAuthCustomHeader.setStrokeColor(isCustomHeader ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckCustomHeader.setVisibility(isCustomHeader ? View.VISIBLE : View.GONE);
            tvAuthCustomHeaderTitle.setTextColor(isCustomHeader ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthCustomHeaderSub.setTextColor(isCustomHeader ? colorOnPrimaryContainer : colorOnSurfaceVariant);
            layoutCustomHeaderFields.setVisibility(isCustomHeader ? View.VISIBLE : View.GONE);

            // Query Param Card
            boolean isQueryParam = (selectedType == CustomProvider.AuthType.QUERY_PARAM);
            cardAuthQueryParam.setCardBackgroundColor(isQueryParam ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthQueryParam.setStrokeWidth(isQueryParam ? 0 : strokeWidth1dp);
            cardAuthQueryParam.setStrokeColor(isQueryParam ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckQueryParam.setVisibility(isQueryParam ? View.VISIBLE : View.GONE);
            tvAuthQueryParamTitle.setTextColor(isQueryParam ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthQueryParamSub.setTextColor(isQueryParam ? colorOnPrimaryContainer : colorOnSurfaceVariant);
            layoutQueryParamFields.setVisibility(isQueryParam ? View.VISIBLE : View.GONE);

            // None Card
            boolean isNone = (selectedType == CustomProvider.AuthType.NO_AUTH);
            cardAuthNone.setCardBackgroundColor(isNone ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthNone.setStrokeWidth(isNone ? 0 : strokeWidth1dp);
            cardAuthNone.setStrokeColor(isNone ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckNone.setVisibility(isNone ? View.VISIBLE : View.GONE);
            tvAuthNoneTitle.setTextColor(isNone ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthNoneSub.setTextColor(isNone ? colorOnPrimaryContainer : colorOnSurfaceVariant);
        };

        cardAuthBearer.setOnClickListener(v -> updateAuthCardsUi.accept(CustomProvider.AuthType.BEARER_TOKEN, true));
        cardAuthCustomHeader.setOnClickListener(v -> updateAuthCardsUi.accept(CustomProvider.AuthType.CUSTOM_HEADER, true));
        cardAuthQueryParam.setOnClickListener(v -> updateAuthCardsUi.accept(CustomProvider.AuthType.QUERY_PARAM, true));
        cardAuthNone.setOnClickListener(v -> updateAuthCardsUi.accept(CustomProvider.AuthType.NO_AUTH, true));

        // Initial Auth styling
        updateAuthCardsUi.accept(CustomProvider.AuthType.BEARER_TOKEN, false);

        // Template chip styling helper
        java.util.function.BiConsumer<MaterialButton, Boolean> setTemplateBtnState = (btn, isSel) -> {
            if (btn == null) return;
            if (isSel) {
                btn.setBackgroundTintList(ColorStateList.valueOf(colorPrimaryContainer));
                btn.setTextColor(colorOnPrimaryContainer);
                btn.setStrokeWidth(0);
                btn.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                btn.setTextColor(colorOnSurfaceVariant);
                btn.setStrokeWidth(Math.round(1 * btn.getResources().getDisplayMetrics().density));
                btn.setStrokeColor(ColorStateList.valueOf(colorOutlineVariant));
            }
        };

        // Reset all templates to outline
        for (MaterialButton btn : templateButtons) {
            setTemplateBtnState.accept(btn, false);
        }

        // Lottie Eye password toggle: 0 -> 15 (midpoint: eye closes) on first tap, 15 -> 30 (end: eye re-opens) on second tap
        final boolean[] isPasswordVisible = new boolean[]{false};
        if (lottieEyeToggle != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieEyeToggle, colorOnSurfaceVariant);
            lottieEyeToggle.setSpeed(1.5f);
            lottieEyeToggle.setMinAndMaxFrame(0, 30);
            lottieEyeToggle.setFrame(0);
            lottieEyeToggle.setOnClickListener(v -> {
                isPasswordVisible[0] = !isPasswordVisible[0];
                if (isPasswordVisible[0]) {
                    etApiKey.setTransformationMethod(null);
                    lottieEyeToggle.setMinAndMaxFrame(0, 15);
                    lottieEyeToggle.setSpeed(1.5f);
                    lottieEyeToggle.playAnimation();
                } else {
                    etApiKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    lottieEyeToggle.setMinAndMaxFrame(15, 30);
                    lottieEyeToggle.setSpeed(1.5f);
                    lottieEyeToggle.playAnimation();
                }
                if (etApiKey.getText() != null) {
                    etApiKey.setSelection(etApiKey.getText().length());
                }
            });
        }

        // Lottie ADD model tag button
        if (lottieBtnAddModel != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieBtnAddModel, colorOnPrimary);
        }

        MaterialSwitch switchSystem = sheetView.findViewById(R.id.switch_system_message);
        TextInputEditText etMaxTokens = sheetView.findViewById(R.id.et_max_tokens);
        TextInputEditText etTemperature = sheetView.findViewById(R.id.et_temperature);
        TextInputEditText etTopP = sheetView.findViewById(R.id.et_top_p);
        TextInputEditText etCustomHeaders = sheetView.findViewById(R.id.et_custom_headers);

        MaterialButton btnTest = sheetView.findViewById(R.id.btn_test_connection);
        LinearLayout layoutTestingProgress = sheetView.findViewById(R.id.layout_testing_progress);
        LottieAnimationView lottieTesting = sheetView.findViewById(R.id.lottie_testing);
        MaterialCardView cardTestResult = sheetView.findViewById(R.id.card_test_result);
        ImageView ivTestResultIcon = sheetView.findViewById(R.id.iv_test_result_icon);
        TextView tvTestResultMessage = sheetView.findViewById(R.id.tv_test_result_message);

        if (lottieTesting != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieTesting, colorPrimary);
        }

        MaterialButton btnSaveProvider = sheetView.findViewById(R.id.btn_save_provider);
        MaterialButton btnDeleteProvider = sheetView.findViewById(R.id.btn_delete_provider);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_cancel);

        List<String> currentModelsList = new ArrayList<>();

        // Helper to refresh model chips in dialog
        Runnable refreshDialogChips = () -> {
            chipGroupModels.removeAllViews();
            for (String m : currentModelsList) {
                Chip chip = new Chip(requireContext());
                chip.setText(m);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                    currentModelsList.remove(m);
                    if (m.equals(etDefaultModel.getText().toString().trim())) {
                        etDefaultModel.setText(currentModelsList.isEmpty() ? "" : currentModelsList.get(0));
                    }
                    chipGroupModels.removeView(chip);
                });
                chipGroupModels.addView(chip);
            }
        };

        btnAddModelTag.setOnClickListener(v -> {
            if (lottieBtnAddModel != null) {
                lottieBtnAddModel.playAnimation();
            }
            String newModel = etAddModel.getText() != null ? etAddModel.getText().toString().trim() : "";
            if (!newModel.isEmpty()) {
                if (!currentModelsList.contains(newModel)) {
                    tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                    currentModelsList.add(newModel);
                    refreshDialogChips.run();
                    if (etDefaultModel.getText() == null || etDefaultModel.getText().toString().trim().isEmpty()) {
                        etDefaultModel.setText(newModel);
                    }
                }
                etAddModel.setText("");
            }
        });

        // Quick template presets with Toggle (select/deselect and clear auto-fill)
        final MaterialButton[] selectedTemplate = new MaterialButton[]{null};

        Runnable clearTemplateFields = () -> {
            etName.setText("");
            etBaseUrl.setText("");
            etChatEndpoint.setText("/chat/completions");
            currentModelsList.clear();
            refreshDialogChips.run();
            etDefaultModel.setText("");
            updateAuthCardsUi.accept(CustomProvider.AuthType.BEARER_TOKEN, false);
        };

        java.util.function.BiConsumer<MaterialButton, Runnable> handleTemplateClick = (btn, applyValues) -> {
            tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
            if (selectedTemplate[0] == btn) {
                // Deselect
                selectedTemplate[0] = null;
                setTemplateBtnState.accept(btn, false);
                clearTemplateFields.run();
            } else {
                // Select
                selectedTemplate[0] = btn;
                for (MaterialButton b : templateButtons) {
                    setTemplateBtnState.accept(b, b == btn);
                }
                applyValues.run();
            }
        };

        btnTplOllama.setOnClickListener(v -> handleTemplateClick.accept(btnTplOllama, () -> {
            etName.setText("Ollama (Local)");
            etBaseUrl.setText("http://192.168.1.100:11434/v1");
            etChatEndpoint.setText("/chat/completions");
            currentModelsList.clear();
            currentModelsList.add("llama3.3");
            currentModelsList.add("qwen2.5-coder");
            currentModelsList.add("deepseek-r1:8b");
            refreshDialogChips.run();
            etDefaultModel.setText("llama3.3");
            updateAuthCardsUi.accept(CustomProvider.AuthType.NO_AUTH, false);
        }));

        btnTplLmstudio.setOnClickListener(v -> handleTemplateClick.accept(btnTplLmstudio, () -> {
            etName.setText("LM Studio");
            etBaseUrl.setText("http://192.168.1.100:1234/v1");
            etChatEndpoint.setText("/chat/completions");
            currentModelsList.clear();
            currentModelsList.add("meta-llama-3.1-8b");
            currentModelsList.add("deepseek-r1");
            refreshDialogChips.run();
            etDefaultModel.setText("meta-llama-3.1-8b");
            updateAuthCardsUi.accept(CustomProvider.AuthType.NO_AUTH, false);
        }));

        btnTplTogether.setOnClickListener(v -> handleTemplateClick.accept(btnTplTogether, () -> {
            etName.setText("Together AI");
            etBaseUrl.setText("https://api.together.xyz/v1");
            etChatEndpoint.setText("/chat/completions");
            currentModelsList.clear();
            currentModelsList.add("meta-llama/Llama-3.3-70B-Instruct-Turbo");
            currentModelsList.add("deepseek-ai/DeepSeek-R1");
            currentModelsList.add("Qwen/Qwen2.5-72B-Instruct-Turbo");
            refreshDialogChips.run();
            etDefaultModel.setText("meta-llama/Llama-3.3-70B-Instruct-Turbo");
            updateAuthCardsUi.accept(CustomProvider.AuthType.BEARER_TOKEN, false);
        }));

        btnTplVllm.setOnClickListener(v -> handleTemplateClick.accept(btnTplVllm, () -> {
            etName.setText("vLLM / LocalAI");
            etBaseUrl.setText("http://192.168.1.100:8000/v1");
            etChatEndpoint.setText("/chat/completions");
            currentModelsList.clear();
            currentModelsList.add("default");
            refreshDialogChips.run();
            etDefaultModel.setText("default");
            updateAuthCardsUi.accept(CustomProvider.AuthType.NO_AUTH, false);
        }));

        btnTplDeepinfra.setOnClickListener(v -> handleTemplateClick.accept(btnTplDeepinfra, () -> {
            etName.setText("DeepInfra");
            etBaseUrl.setText("https://api.deepinfra.com/v1/openai");
            etChatEndpoint.setText("/chat/completions");
            currentModelsList.clear();
            currentModelsList.add("meta-llama/Meta-Llama-3.1-70B-Instruct");
            currentModelsList.add("deepseek-ai/DeepSeek-R1");
            refreshDialogChips.run();
            etDefaultModel.setText("meta-llama/Meta-Llama-3.1-70B-Instruct");
            updateAuthCardsUi.accept(CustomProvider.AuthType.BEARER_TOKEN, false);
        }));

        // Populate existing data if editing
        if (existing != null) {
            tvTitle.setText(R.string.title_edit_custom_provider);
            etName.setText(existing.getName());
            etBaseUrl.setText(existing.getBaseUrl());
            etChatEndpoint.setText(existing.getChatEndpoint());
            currentModelsList.addAll(existing.getModels());
            refreshDialogChips.run();
            etDefaultModel.setText(existing.getDefaultModel());

            updateAuthCardsUi.accept(existing.getAuthType(), false);
            if (existing.getAuthType() == CustomProvider.AuthType.CUSTOM_HEADER) {
                etCustomHeaderName.setText(existing.getCustomHeaderName());
                etCustomAuthPrefix.setText(existing.getAuthPrefix());
            }

            etApiKey.setText(CustomProviderManager.getInstance().getCustomProviderApiKey(existing.getId()));
            switchSystem.setChecked(existing.isSupportsSystemMessage());
            etMaxTokens.setText(existing.getMaxTokens());
            etTemperature.setText(existing.getTemperature());
            etTopP.setText(existing.getTopP());
            etCustomHeaders.setText(existing.getCustomHeadersJson());

            btnDeleteProvider.setVisibility(View.VISIBLE);
            btnDeleteProvider.setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.btn_delete_provider)
                        .setMessage(getString(R.string.msg_confirm_delete_provider, existing.getName()))
                        .setPositiveButton(R.string.btn_delete_provider, (d, w) -> {
                            CustomProviderManager.getInstance().deleteCustomProvider(existing.getId());
                            selectedCustomProvider = null;
                            selectedModel = LanguageModel.Gemini;
                            CustomProviderManager.getInstance().setSelectedProviderType(CustomProviderManager.TYPE_BUILTIN);
                            refreshData();
                            loadModelSettings(selectedModel);
                            sendConfigBroadcast();
                            dialog.dismiss();
                            Toast.makeText(requireContext(), R.string.msg_provider_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        }

        // Test connection
        btnTest.setOnClickListener(v -> {
            String baseUrl = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";
            if (baseUrl.isEmpty()) {
                Toast.makeText(requireContext(), R.string.hint_base_url, Toast.LENGTH_SHORT).show();
                return;
            }

            CustomProvider testProv = new CustomProvider();
            testProv.setBaseUrl(baseUrl);
            testProv.setChatEndpoint(etChatEndpoint.getText() != null ? etChatEndpoint.getText().toString().trim() : "/chat/completions");
            String defM = etDefaultModel.getText() != null ? etDefaultModel.getText().toString().trim() : "";
            testProv.setDefaultModel(defM.isEmpty() ? (!currentModelsList.isEmpty() ? currentModelsList.get(0) : "default") : defM);

            CustomProvider.AuthType authType = currentAuthType[0];
            testProv.setAuthType(authType);
            if (authType == CustomProvider.AuthType.CUSTOM_HEADER) {
                testProv.setCustomHeaderName(etCustomHeaderName.getText() != null ? etCustomHeaderName.getText().toString().trim() : "x-api-key");
                testProv.setAuthPrefix(etCustomAuthPrefix.getText() != null ? etCustomAuthPrefix.getText().toString() : "");
            }

            String key = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
            tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
            if (btnTest != null) btnTest.setVisibility(View.GONE);
            if (layoutTestingProgress != null) layoutTestingProgress.setVisibility(View.VISIBLE);
            if (cardTestResult != null) cardTestResult.setVisibility(View.GONE);

            CustomProviderClient.testConnection(testProv, key, new CustomProviderClient.TestCallback() {
                @Override
                public void onSuccess(String response) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                            if (btnTest != null) btnTest.setVisibility(View.VISIBLE);
                            if (layoutTestingProgress != null) layoutTestingProgress.setVisibility(View.GONE);
                            if (cardTestResult != null) {
                                cardTestResult.setVisibility(View.VISIBLE);
                                cardTestResult.setCardBackgroundColor(colorPrimaryContainer);
                                cardTestResult.setStrokeWidth(0);
                                ivTestResultIcon.setImageResource(R.drawable.ic_check);
                                ivTestResultIcon.setImageTintList(ColorStateList.valueOf(colorOnPrimaryContainer));
                                tvTestResultMessage.setTextColor(colorOnPrimaryContainer);
                                tvTestResultMessage.setText(R.string.msg_connection_success);
                            }
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                            if (btnTest != null) btnTest.setVisibility(View.VISIBLE);
                            if (layoutTestingProgress != null) layoutTestingProgress.setVisibility(View.GONE);
                            if (cardTestResult != null) {
                                int colorErrorContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorErrorContainer, Color.RED);
                                int colorOnErrorContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnErrorContainer, Color.WHITE);
                                cardTestResult.setVisibility(View.VISIBLE);
                                cardTestResult.setCardBackgroundColor(colorErrorContainer);
                                cardTestResult.setStrokeWidth(0);
                                ivTestResultIcon.setImageResource(R.drawable.ic_close);
                                ivTestResultIcon.setImageTintList(ColorStateList.valueOf(colorOnErrorContainer));
                                tvTestResultMessage.setTextColor(colorOnErrorContainer);
                                tvTestResultMessage.setText(getString(R.string.msg_connection_failed, error));
                            }
                        });
                    }
                }
            });
        });

        // Save button
        btnSaveProvider.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String baseUrl = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";
            String endpoint = etChatEndpoint.getText() != null ? etChatEndpoint.getText().toString().trim() : "/chat/completions";
            String defaultModel = etDefaultModel.getText() != null ? etDefaultModel.getText().toString().trim() : "";

            if (name.isEmpty() || baseUrl.isEmpty() || currentModelsList.isEmpty()) {
                Toast.makeText(requireContext(), R.string.msg_fill_required_fields, Toast.LENGTH_LONG).show();
                return;
            }

            if (defaultModel.isEmpty()) {
                defaultModel = currentModelsList.get(0);
            }

            CustomProvider providerToSave = existing != null ? existing : new CustomProvider();
            providerToSave.setName(name);
            providerToSave.setBaseUrl(baseUrl);
            providerToSave.setChatEndpoint(endpoint);
            providerToSave.setModels(currentModelsList);
            providerToSave.setDefaultModel(defaultModel);

            CustomProvider.AuthType authType = currentAuthType[0];
            providerToSave.setAuthType(authType);
            if (authType == CustomProvider.AuthType.CUSTOM_HEADER) {
                providerToSave.setCustomHeaderName(etCustomHeaderName.getText() != null ? etCustomHeaderName.getText().toString().trim() : "x-api-key");
                providerToSave.setAuthPrefix(etCustomAuthPrefix.getText() != null ? etCustomAuthPrefix.getText().toString() : "");
            }

            providerToSave.setSupportsSystemMessage(switchSystem.isChecked());
            providerToSave.setMaxTokens(etMaxTokens.getText() != null ? etMaxTokens.getText().toString().trim() : "4096");
            providerToSave.setTemperature(etTemperature.getText() != null ? etTemperature.getText().toString().trim() : "1.0");
            providerToSave.setTopP(etTopP.getText() != null ? etTopP.getText().toString().trim() : "1.0");
            providerToSave.setCustomHeadersJson(etCustomHeaders.getText() != null ? etCustomHeaders.getText().toString().trim() : "");

            CustomProviderManager.getInstance().saveCustomProvider(providerToSave);

            String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
            if (!apiKey.isEmpty()) {
                CustomProviderManager.getInstance().setCustomProviderApiKey(providerToSave.getId(), apiKey);
            }

            CustomProviderManager.getInstance().setCustomProviderSubModel(providerToSave.getId(), defaultModel);

            // Select this custom provider
            selectedCustomProvider = providerToSave;
            selectedModel = null;
            CustomProviderManager.getInstance().setSelectedCustomProviderId(providerToSave.getId());

            refreshData();
            loadCustomProviderSettings(providerToSave);
            sendConfigBroadcast();

            dialog.dismiss();
            Toast.makeText(requireContext(), R.string.msg_provider_saved, Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveConfiguration() {
        if (!SPManager.isReady()) {
            Toast.makeText(requireContext(), R.string.msg_settings_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        String subModel = etSubModel.getText() != null ? etSubModel.getText().toString().trim() : "";

        if (selectedCustomProvider != null) {
            if (subModel.isEmpty()) {
                subModel = selectedCustomProvider.getDefaultModel();
            }
            CustomProviderManager.getInstance().setCustomProviderSubModel(selectedCustomProvider.getId(), subModel);
            CustomProviderManager.getInstance().setSelectedCustomProviderId(selectedCustomProvider.getId());
            sendConfigBroadcast();
            Toast.makeText(requireContext(), R.string.msg_config_saved, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedModel == null) {
            Toast.makeText(requireContext(), R.string.msg_select_model_toast, Toast.LENGTH_SHORT).show();
            return;
        }

        SPManager sp = SPManager.getInstance();
        CustomProviderManager.getInstance().setSelectedProviderType(CustomProviderManager.TYPE_BUILTIN);
        sp.setLanguageModel(selectedModel);

        if (subModel.isEmpty()) {
            subModel = selectedModel.getDefault(LanguageModelField.SubModel);
        }

        final String finalSubModel = subModel;

        if (!isValidModelName(selectedModel, finalSubModel)) {
            String suggested = getSuggestedModel(selectedModel, finalSubModel);

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Invalid Model Name")
                    .setMessage("The model \"" + finalSubModel + "\" may not be valid.\n\nDid you mean: " + suggested
                            + "?\n\nUsing an invalid model name will cause API errors.")
                    .setPositiveButton("Use Suggested", (dialog, which) -> {
                        etSubModel.setText(suggested);
                        sp.setSubModel(selectedModel, suggested);
                        sendConfigBroadcast();
                        Toast.makeText(requireContext(), getString(R.string.msg_config_saved_with) + suggested,
                                Toast.LENGTH_SHORT)
                                .show();
                    })
                    .setNegativeButton("Use Anyway", (dialog, which) -> {
                        sp.setSubModel(selectedModel, finalSubModel);
                        sendConfigBroadcast();
                        Toast.makeText(requireContext(), R.string.msg_config_saved_warning, Toast.LENGTH_SHORT)
                                .show();
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
            return;
        }

        sp.setSubModel(selectedModel, finalSubModel);
        sendConfigBroadcast();
        Toast.makeText(requireContext(), R.string.msg_config_saved, Toast.LENGTH_SHORT).show();
    }

    private void sendConfigBroadcast() {
        if (!SPManager.isReady())
            return;

        SPManager sp = SPManager.getInstance();
        Intent broadcastIntent = new Intent("tn.eluea.kgpt.DIALOG_RESULT");

        if (selectedCustomProvider != null) {
            broadcastIntent.putExtra("tn.eluea.kgpt.config.SELECTED_PROVIDER_TYPE", CustomProviderManager.TYPE_CUSTOM);
            broadcastIntent.putExtra("tn.eluea.kgpt.config.SELECTED_CUSTOM_PROVIDER_ID", selectedCustomProvider.getId());
        } else if (selectedModel != null) {
            broadcastIntent.putExtra("tn.eluea.kgpt.config.SELECTED_PROVIDER_TYPE", CustomProviderManager.TYPE_BUILTIN);
            broadcastIntent.putExtra("tn.eluea.kgpt.config.SELECTED_MODEL", selectedModel.name());
        }

        broadcastIntent.putExtra("tn.eluea.kgpt.config.model", sp.getConfigBundle());
        requireContext().sendBroadcast(broadcastIntent);
    }
}
