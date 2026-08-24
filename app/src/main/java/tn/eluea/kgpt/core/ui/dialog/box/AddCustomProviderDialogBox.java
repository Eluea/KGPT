/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.core.ui.dialog.box;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.core.data.ConfigContainer;
import tn.eluea.kgpt.core.ui.dialog.DialogBoxManager;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.llm.model.CustomProvider;
import tn.eluea.kgpt.llm.client.CustomProviderClient;
import tn.eluea.kgpt.llm.model.CustomProviderManager;

/**
 * Add Custom Provider — floating dialog version (€ → Choose Model → Add).
 * Mirrors the in-app editor (ModelsFragment) using the same layout and save
 * logic, then selects the new provider and broadcasts the standard
 * DIALOG_RESULT extras so hooked keyboards switch immediately.
 */
public class AddCustomProviderDialogBox extends DialogBox {

    private static final String TAG = "KGPT_AddCustomProvider";

    private static final String EXTRA_SELECTED_PROVIDER_TYPE = "tn.eluea.kgpt.config.SELECTED_PROVIDER_TYPE";
    private static final String EXTRA_SELECTED_CUSTOM_PROVIDER_ID = "tn.eluea.kgpt.config.SELECTED_CUSTOM_PROVIDER_ID";

    public AddCustomProviderDialogBox(DialogBoxManager dialogManager, android.app.Activity parent,
            Bundle inputBundle, ConfigContainer configContainer) {
        super(dialogManager, parent, inputBundle, configContainer);
    }

    @Override
    protected Dialog build() {
        tn.eluea.kgpt.ui.main.FloatingBottomSheet sheet =
                new tn.eluea.kgpt.ui.main.FloatingBottomSheet(getContext());
        android.content.Context themedContext = sheet.getContext();

        View sheetView = LayoutInflater.from(themedContext)
                .inflate(R.layout.bottom_sheet_add_custom_provider, null);
        ViewGroup bottomSheetContainer = sheetView.findViewById(R.id.bottom_sheet_container);

        // Hide delete (creation-only flow)
        MaterialButton btnDeleteProvider = sheetView.findViewById(R.id.btn_delete_provider);
        if (btnDeleteProvider != null) btnDeleteProvider.setVisibility(View.GONE);

        TextView tvTitle = sheetView.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) tvTitle.setText(R.string.title_add_custom_provider);

        TextInputEditText etName = sheetView.findViewById(R.id.et_provider_name);
        TextInputEditText etBaseUrl = sheetView.findViewById(R.id.et_base_url);
        TextInputEditText etChatEndpoint = sheetView.findViewById(R.id.et_chat_endpoint);
        TextInputEditText etAddModel = sheetView.findViewById(R.id.et_add_model);
        MaterialCardView btnAddModelTag = sheetView.findViewById(R.id.btn_add_model_tag);
        LottieAnimationView lottieBtnAddModel = sheetView.findViewById(R.id.lottie_btn_add_model);
        ChipGroup chipGroupModels = sheetView.findViewById(R.id.chip_group_models);
        TextInputEditText etDefaultModel = sheetView.findViewById(R.id.et_default_model);

        // Auth cards
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

        MaterialButton btnTplOllama = sheetView.findViewById(R.id.btn_tpl_ollama);
        MaterialButton btnTplLmstudio = sheetView.findViewById(R.id.btn_tpl_lmstudio);
        MaterialButton btnTplTogether = sheetView.findViewById(R.id.btn_tpl_together);
        MaterialButton btnTplVllm = sheetView.findViewById(R.id.btn_tpl_vllm);
        MaterialButton btnTplDeepinfra = sheetView.findViewById(R.id.btn_tpl_deepinfra);
        MaterialButton[] templateButtons = new MaterialButton[]{btnTplOllama, btnTplLmstudio, btnTplTogether, btnTplVllm, btnTplDeepinfra};

        com.google.android.material.tabs.TabLayout tabLayout = sheetView.findViewById(R.id.tab_layout_provider);
        LinearLayout tabContentServer = sheetView.findViewById(R.id.tab_content_server);
        LinearLayout tabContentModelsAuth = sheetView.findViewById(R.id.tab_content_models_auth);
        LinearLayout tabContentAdvancedTest = sheetView.findViewById(R.id.tab_content_advanced_test);

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int pos = tab.getPosition();
                tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                tabContentServer.setVisibility(pos == 0 ? View.VISIBLE : View.GONE);
                tabContentModelsAuth.setVisibility(pos == 1 ? View.VISIBLE : View.GONE);
                tabContentAdvancedTest.setVisibility(pos == 2 ? View.VISIBLE : View.GONE);
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
        if (tabLayout.getTabCount() > 0 && tabLayout.getTabAt(0) != null) {
            tabLayout.getTabAt(0).select();
        }
        tabContentServer.setVisibility(View.VISIBLE);
        tabContentModelsAuth.setVisibility(View.GONE);
        tabContentAdvancedTest.setVisibility(View.GONE);

        // Theme colors
        int colorPrimary = MaterialColors.getColor(sheetView, androidx.appcompat.R.attr.colorPrimary, Color.CYAN);
        int colorOnPrimary = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        int colorPrimaryContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorPrimaryContainer, Color.DKGRAY);
        int colorOnPrimaryContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.WHITE);
        int colorOnSurface = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
        int colorOnSurfaceVariant = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
        int colorOutlineVariant = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY);

        // Auth state
        final CustomProvider.AuthType[] currentAuthType = new CustomProvider.AuthType[]{CustomProvider.AuthType.BEARER_TOKEN};
        final int strokeWidth1dp = Math.round(1 * sheetView.getResources().getDisplayMetrics().density);

        java.util.function.BiConsumer<CustomProvider.AuthType, Boolean> updateAuthCardsUi = (selectedType, animate) -> {
            currentAuthType[0] = selectedType;
            if (animate) {
                tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
            }
            boolean isBearer = selectedType == CustomProvider.AuthType.BEARER_TOKEN;
            cardAuthBearer.setCardBackgroundColor(isBearer ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthBearer.setStrokeWidth(isBearer ? 0 : strokeWidth1dp);
            cardAuthBearer.setStrokeColor(isBearer ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckBearer.setVisibility(isBearer ? View.VISIBLE : View.GONE);
            tvAuthBearerTitle.setTextColor(isBearer ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthBearerSub.setTextColor(isBearer ? colorOnPrimaryContainer : colorOnSurfaceVariant);

            boolean isCustomHeader = selectedType == CustomProvider.AuthType.CUSTOM_HEADER;
            cardAuthCustomHeader.setCardBackgroundColor(isCustomHeader ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthCustomHeader.setStrokeWidth(isCustomHeader ? 0 : strokeWidth1dp);
            cardAuthCustomHeader.setStrokeColor(isCustomHeader ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckCustomHeader.setVisibility(isCustomHeader ? View.VISIBLE : View.GONE);
            tvAuthCustomHeaderTitle.setTextColor(isCustomHeader ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthCustomHeaderSub.setTextColor(isCustomHeader ? colorOnPrimaryContainer : colorOnSurfaceVariant);
            layoutCustomHeaderFields.setVisibility(isCustomHeader ? View.VISIBLE : View.GONE);

            boolean isQueryParam = selectedType == CustomProvider.AuthType.QUERY_PARAM;
            cardAuthQueryParam.setCardBackgroundColor(isQueryParam ? colorPrimaryContainer : Color.TRANSPARENT);
            cardAuthQueryParam.setStrokeWidth(isQueryParam ? 0 : strokeWidth1dp);
            cardAuthQueryParam.setStrokeColor(isQueryParam ? Color.TRANSPARENT : colorOutlineVariant);
            ivCheckQueryParam.setVisibility(isQueryParam ? View.VISIBLE : View.GONE);
            tvAuthQueryParamTitle.setTextColor(isQueryParam ? colorOnPrimaryContainer : colorOnSurface);
            tvAuthQueryParamSub.setTextColor(isQueryParam ? colorOnPrimaryContainer : colorOnSurfaceVariant);
            layoutQueryParamFields.setVisibility(isQueryParam ? View.VISIBLE : View.GONE);

            boolean isNone = selectedType == CustomProvider.AuthType.NO_AUTH;
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
        updateAuthCardsUi.accept(CustomProvider.AuthType.BEARER_TOKEN, false);

        // Template buttons styling
        java.util.function.BiConsumer<MaterialButton, Boolean> setTemplateBtnState = (btn, isSel) -> {
            if (btn == null) return;
            if (isSel) {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimaryContainer));
                btn.setTextColor(colorOnPrimaryContainer);
                btn.setStrokeWidth(0);
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
            } else {
                btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
                btn.setTextColor(colorOnSurfaceVariant);
                btn.setStrokeWidth(strokeWidth1dp);
                btn.setStrokeColor(android.content.res.ColorStateList.valueOf(colorOutlineVariant));
            }
        };
        for (MaterialButton btn : templateButtons) setTemplateBtnState.accept(btn, false);

        // Eye toggle for API key
        final boolean[] isPasswordVisible = {false};
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
                } else {
                    etApiKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    lottieEyeToggle.setMinAndMaxFrame(15, 30);
                }
                lottieEyeToggle.setSpeed(1.5f);
                lottieEyeToggle.playAnimation();
                if (etApiKey.getText() != null) etApiKey.setSelection(etApiKey.getText().length());
            });
        }
        if (lottieBtnAddModel != null) tn.eluea.kgpt.util.LottieHelper.tint(lottieBtnAddModel, colorOnPrimary);

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
        if (lottieTesting != null) tn.eluea.kgpt.util.LottieHelper.tint(lottieTesting, colorPrimary);

        MaterialButton btnSaveProvider = sheetView.findViewById(R.id.btn_save_provider);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_cancel);

        List<String> currentModelsList = new ArrayList<>();

        Runnable refreshDialogChips = () -> {
            chipGroupModels.removeAllViews();
            for (String m : currentModelsList) {
                Chip chip = new Chip(themedContext);
                chip.setText(m);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                    currentModelsList.remove(m);
                    if (etDefaultModel.getText() != null && m.equals(etDefaultModel.getText().toString().trim())) {
                        etDefaultModel.setText(currentModelsList.isEmpty() ? "" : currentModelsList.get(0));
                    }
                    chipGroupModels.removeView(chip);
                });
                chipGroupModels.addView(chip);
            }
        };

        btnAddModelTag.setOnClickListener(v -> {
            if (lottieBtnAddModel != null) lottieBtnAddModel.playAnimation();
            String newModel = etAddModel.getText() != null ? etAddModel.getText().toString().trim() : "";
            if (!newModel.isEmpty() && !currentModelsList.contains(newModel)) {
                tn.eluea.kgpt.util.TransitionHelper.beginTransition(bottomSheetContainer, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                currentModelsList.add(newModel);
                refreshDialogChips.run();
                if (etDefaultModel.getText() == null || etDefaultModel.getText().toString().trim().isEmpty()) {
                    etDefaultModel.setText(newModel);
                }
            }
            etAddModel.setText("");
        });

        // Templates
        final MaterialButton[] selectedTemplate = {null};

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
                selectedTemplate[0] = null;
                setTemplateBtnState.accept(btn, false);
                clearTemplateFields.run();
            } else {
                selectedTemplate[0] = btn;
                for (MaterialButton b : templateButtons) setTemplateBtnState.accept(b, b == btn);
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

        // Test connection
        btnTest.setOnClickListener(v -> {
            String baseUrl = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";
            if (baseUrl.isEmpty()) {
                Toast.makeText(themedContext, R.string.hint_base_url, Toast.LENGTH_SHORT).show();
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
            btnTest.setVisibility(View.GONE);
            layoutTestingProgress.setVisibility(View.VISIBLE);
            cardTestResult.setVisibility(View.GONE);

            CustomProviderClient.testConnection(testProv, key, new CustomProviderClient.TestCallback() {
                @Override public void onSuccess(String response) {
                    mParent.runOnUiThread(() -> {
                        btnTest.setVisibility(View.VISIBLE);
                        layoutTestingProgress.setVisibility(View.GONE);
                        cardTestResult.setVisibility(View.VISIBLE);
                        cardTestResult.setCardBackgroundColor(colorPrimaryContainer);
                        cardTestResult.setStrokeWidth(0);
                        ivTestResultIcon.setImageResource(R.drawable.ic_check);
                        ivTestResultIcon.setImageTintList(android.content.res.ColorStateList.valueOf(colorOnPrimaryContainer));
                        tvTestResultMessage.setTextColor(colorOnPrimaryContainer);
                        tvTestResultMessage.setText(R.string.msg_connection_success);
                    });
                }

                @Override public void onError(String error) {
                    mParent.runOnUiThread(() -> {
                        btnTest.setVisibility(View.VISIBLE);
                        layoutTestingProgress.setVisibility(View.GONE);
                        cardTestResult.setVisibility(View.VISIBLE);
                        int colorErrorContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorErrorContainer, Color.RED);
                        int colorOnErrorContainer = MaterialColors.getColor(sheetView, com.google.android.material.R.attr.colorOnErrorContainer, Color.WHITE);
                        cardTestResult.setCardBackgroundColor(colorErrorContainer);
                        cardTestResult.setStrokeWidth(0);
                        ivTestResultIcon.setImageResource(R.drawable.ic_close);
                        ivTestResultIcon.setImageTintList(android.content.res.ColorStateList.valueOf(colorOnErrorContainer));
                        tvTestResultMessage.setTextColor(colorOnErrorContainer);
                        tvTestResultMessage.setText(themedContext.getString(R.string.msg_connection_failed, error));
                    });
                }
            });
        });

        // Save
        btnSaveProvider.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String baseUrl = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";
            String endpoint = etChatEndpoint.getText() != null ? etChatEndpoint.getText().toString().trim() : "/chat/completions";
            String defaultModel = etDefaultModel.getText() != null ? etDefaultModel.getText().toString().trim() : "";

            if (name.isEmpty() || baseUrl.isEmpty() || currentModelsList.isEmpty()) {
                Toast.makeText(themedContext, R.string.msg_fill_required_fields, Toast.LENGTH_LONG).show();
                return;
            }
            if (defaultModel.isEmpty()) defaultModel = currentModelsList.get(0);

            CustomProvider providerToSave = new CustomProvider();
            providerToSave.setName(name);
            providerToSave.setBaseUrl(baseUrl);
            providerToSave.setChatEndpoint(endpoint);
            providerToSave.setModels(currentModelsList);
            providerToSave.setDefaultModel(defaultModel);
            providerToSave.setAuthType(currentAuthType[0]);
            if (currentAuthType[0] == CustomProvider.AuthType.CUSTOM_HEADER) {
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

            // Select it immediately
            CustomProviderManager.getInstance().setSelectedProviderType(CustomProviderManager.TYPE_CUSTOM);
            CustomProviderManager.getInstance().setSelectedCustomProviderId(providerToSave.getId());

            // Broadcast so hooked keyboards switch live
            try {
                Intent result = new Intent(tn.eluea.kgpt.ui.UiInteractor.ACTION_DIALOG_RESULT);
                result.putExtra(EXTRA_SELECTED_PROVIDER_TYPE, CustomProviderManager.TYPE_CUSTOM);
                result.putExtra(EXTRA_SELECTED_CUSTOM_PROVIDER_ID, providerToSave.getId());
                getContext().sendBroadcast(result);
            } catch (Throwable ignored) {}

            Toast.makeText(themedContext, R.string.msg_provider_saved, Toast.LENGTH_SHORT).show();
            sheet.dismiss();
            switchToDialog(DialogType.ChoseModel);
        });

        btnCancel.setOnClickListener(v -> {
            sheet.dismiss();
            switchToDialog(DialogType.ChoseModel);
        });

        sheet.setContentView(sheetView);
        return sheet;
    }
}
