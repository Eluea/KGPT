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
package tn.eluea.kgpt.ui.main.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.main.adapters.AdditionalApiKeysAdapter;

public class ApiKeysFragment extends Fragment implements AdditionalApiKeysAdapter.OnKeyActionListener {

    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_THEME = "theme_mode";

    private TextInputEditText etGeminiKey;
    private ImageView ivGeminiStatus;
    private MaterialButton btnSaveGemini, btnAddKey;
    private RecyclerView rvAdditionalKeys;
    private View rootView;

    private AdditionalApiKeysAdapter adapter;
    private List<LanguageModel> additionalModels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_api_keys, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        initViews(view);
        setupKeyboardHandling(view);
        applyAmoledIfNeeded();
        loadGeminiKey();
        setupListeners();
        setupAdditionalKeys();

        // Apply candy colors when Material You is disabled
        // Candy colors removed
        // tn.eluea.kgpt.util.CandyColorHelper.applyToViewHierarchy(requireContext(),
        // rootView);
    }

    private void setupKeyboardHandling(View view) {
        // Handle keyboard insets to scroll content when keyboard appears
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Calculate base padding in pixels (110dp)
            float density = view.getContext().getResources().getDisplayMetrics().density;
            int basePaddingPx = (int) (110 * density);

            // Add padding at bottom when keyboard is visible
            // We use max of IME and system bars to ensure we cover navigation bar
            // and add the base padding (dock space) on top of that.
            int bottomInset = Math.max(imeInsets.bottom, systemBars.bottom);

            // Find the scroll view content and update its padding
            if (v instanceof NestedScrollView) {
                View content = ((NestedScrollView) v).getChildAt(0);
                if (content != null) {
                    content.setPadding(
                            content.getPaddingLeft(),
                            content.getPaddingTop(),
                            content.getPaddingRight(),
                            basePaddingPx + bottomInset);
                }
            }

            return windowInsets;
        });
    }

    private void initViews(View view) {
        etGeminiKey = view.findViewById(R.id.et_gemini_key);
        ivGeminiStatus = view.findViewById(R.id.iv_gemini_status);
        btnSaveGemini = view.findViewById(R.id.btn_save_gemini);
        btnAddKey = view.findViewById(R.id.btn_add_key);
        rvAdditionalKeys = view.findViewById(R.id.rv_additional_keys);
    }

    private void applyAmoledIfNeeded() {
        SharedPreferences prefs = requireContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = prefs.getBoolean(PREF_AMOLED, false);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);

        if (isDarkMode && isAmoled) {
            if (rootView instanceof ViewGroup) {
                View scrollContent = ((ViewGroup) rootView).getChildAt(0);
                if (scrollContent != null) {
                    scrollContent
                            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));
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

    private void loadGeminiKey() {
        if (SPManager.isReady()) {
            String key = SPManager.getInstance().getApiKey(LanguageModel.Gemini);
            if (key != null && !key.isEmpty()) {
                etGeminiKey.setText(key);
                ivGeminiStatus.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupListeners() {
        btnSaveGemini.setOnClickListener(v -> saveGeminiKey());
        btnAddKey.setOnClickListener(v -> showAddKeyBottomSheet());
    }

    private void saveGeminiKey() {
        String key = etGeminiKey.getText() != null ? etGeminiKey.getText().toString().trim() : "";

        if (key.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an API key", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!SPManager.isReady()) {
            Toast.makeText(requireContext(), "Settings not available", Toast.LENGTH_SHORT).show();
            return;
        }

        SPManager.getInstance().setApiKey(LanguageModel.Gemini, key);
        ivGeminiStatus.setVisibility(View.VISIBLE);

        // Send broadcast to sync with Xposed module
        sendConfigBroadcast();

        Toast.makeText(requireContext(), "Gemini API key saved", Toast.LENGTH_SHORT).show();
    }

    /**
     * Send broadcast to Xposed module to sync configuration
     */
    private void sendConfigBroadcast() {
        if (!SPManager.isReady())
            return;

        SPManager sp = SPManager.getInstance();

        Intent broadcastIntent = new Intent("tn.eluea.kgpt.DIALOG_RESULT");

        // Add selected model
        broadcastIntent.putExtra("tn.eluea.kgpt.config.SELECTED_MODEL", sp.getLanguageModel().name());

        // Add all model configurations
        broadcastIntent.putExtra("tn.eluea.kgpt.config.model", sp.getConfigBundle());

        requireContext().sendBroadcast(broadcastIntent);
    }

    private void setupAdditionalKeys() {
        additionalModels.clear();

        if (SPManager.isReady()) {
            for (LanguageModel model : LanguageModel.values()) {
                if (model != LanguageModel.Gemini) {
                    String key = SPManager.getInstance().getApiKey(model);
                    if (key != null && !key.isEmpty()) {
                        additionalModels.add(model);
                    }
                }
            }
        }

        adapter = new AdditionalApiKeysAdapter(requireContext(), additionalModels, this);

        rvAdditionalKeys.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdditionalKeys.setAdapter(adapter);
    }

    private void showAddKeyBottomSheet() {
        // Get available models
        List<LanguageModel> availableModels = new ArrayList<>();
        for (LanguageModel model : LanguageModel.values()) {
            if (model != LanguageModel.Gemini && !additionalModels.contains(model)) {
                availableModels.add(model);
            }
        }

        if (availableModels.isEmpty()) {
            Toast.makeText(requireContext(), "All providers already added", Toast.LENGTH_SHORT).show();
            return;
        }

        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_add_api_key, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet bottomSheet = new FloatingBottomSheet(requireContext());
        bottomSheet.setContentView(sheetView);

        SharedPreferences prefs = requireContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        boolean isAmoled = prefs.getBoolean(PREF_AMOLED, false);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);

        LinearLayout providersContainer = sheetView.findViewById(R.id.providers_container);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_cancel);

        // Add provider options
        for (LanguageModel model : availableModels) {
            View optionView = LayoutInflater.from(requireContext()).inflate(R.layout.item_provider_option,
                    providersContainer, false);

            TextView tvName = optionView.findViewById(R.id.tv_provider_name);
            MaterialCardView card = optionView.findViewById(R.id.card_provider);

            tvName.setText(model.label);

            // Apply AMOLED to card if needed
            if (isDarkMode && isAmoled) {
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_amoled));
                card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.divider_dark));
            }

            card.setOnClickListener(v -> {
                additionalModels.add(model);
                adapter.notifyItemInserted(additionalModels.size() - 1);
                bottomSheet.dismiss();
            });

            providersContainer.addView(optionView);
        }

        btnCancel.setOnClickListener(v -> bottomSheet.dismiss());

        bottomSheet.show();
    }

    @Override
    public void onKeySaved(LanguageModel model, String key) {
        if (SPManager.isReady()) {
            SPManager.getInstance().setApiKey(model, key);
            sendConfigBroadcast();
            Toast.makeText(requireContext(), model.label + " API key saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onKeyDeleted(LanguageModel model, int position) {
        if (SPManager.isReady()) {
            SPManager.getInstance().setApiKey(model, "");
            sendConfigBroadcast();
        }
        additionalModels.remove(position);
        adapter.notifyItemRemoved(position);
        Toast.makeText(requireContext(), model.label + " key removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references to prevent memory leaks
        if (rvAdditionalKeys != null) {
            rvAdditionalKeys.setAdapter(null);
        }
        adapter = null;
        rootView = null;
        etGeminiKey = null;
        ivGeminiStatus = null;
        btnSaveGemini = null;
        btnAddKey = null;
        rvAdditionalKeys = null;

    }
}
