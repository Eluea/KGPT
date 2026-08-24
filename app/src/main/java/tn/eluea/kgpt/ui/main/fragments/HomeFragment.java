/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.ui.cat.CatPeekManager;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.ui.main.MainActivity;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class HomeFragment extends Fragment {

    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_THEME = "theme_mode";

    // P6/I2: service state is owned by LSPosedHelper (single listener);
    // this fragment just mirrors it for the status card.
    private static volatile boolean isServiceBoundActive = false;
    private static XposedService sXposedService = null;
    private static java.lang.ref.WeakReference<HomeFragment> sCurrentInstance = new java.lang.ref.WeakReference<>(null);

    private final Runnable onBind = () -> {
        isServiceBoundActive = true;
        HomeFragment inst = sCurrentInstance.get();
        if (inst != null && inst.isAdded() && inst.getActivity() != null) {
            inst.getActivity().runOnUiThread(inst::updateUI);
        }
    };
    private final Runnable onDeath = () -> {
        isServiceBoundActive = false;
        sXposedService = null;
        HomeFragment inst = sCurrentInstance.get();
        if (inst != null && inst.isAdded() && inst.getActivity() != null) {
            inst.getActivity().runOnUiThread(inst::updateUI);
        }
    };



    // Module status states
    private static final int STATUS_NOT_ACTIVE = 0;
    private static final int STATUS_ACTIVE = 2;

    private LinearLayout statusContainer;
    private ImageView ivStatusIcon;
    private TextView tvStatusTitle, tvStatusDesc;
    private TextView tvVersion, tvSearchEngine;
    private View cardAiSettings, cardWebSearch;
    private MaterialCardView cardHowToUse;
    private FrameLayout btnInfo;
    private View rootView;

    // Quick Action icons for candy colors
    private android.widget.ImageView iconInvocation, iconWebSearch;

    // Cat peek easter egg
    private CatPeekManager catPeekManager;

    private int moduleStatus = STATUS_NOT_ACTIVE;

    private static final String[][] SEARCH_ENGINES = {
            { "duckduckgo", "DuckDuckGo" },
            { "google", "Google" },
            { "google_ai", "Google AI Mode" },
            { "bing", "Bing" },
            { "brave", "Brave Search" },
            { "ecosia", "Ecosia" },
            { "yahoo", "Yahoo" },
            { "yandex", "Yandex" },
            { "qwant", "Qwant" },
            { "startpage", "StartPage" },
            { "perplexity", "Perplexity AI" },
            { "you", "You.com" },
            { "copilot", "Copilot" },
            { "blackbox", "Blackbox AI" }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sCurrentInstance = new java.lang.ref.WeakReference<>(this);
        rootView = view;
        initViews(view);
        applyAmoledIfNeeded();
        // applyCandyColorsIfNeeded(); // Removed
        setupClickListeners();
        updateUI();

        // Delayed checks to catch async LSPosed Service binding
        view.postDelayed(this::updateUI, 300);
        view.postDelayed(this::updateUI, 1000);
    }

    @Override
    public void onResume() {
        super.onResume();
        sCurrentInstance = new java.lang.ref.WeakReference<>(this);
        updateUI();
        // Start cat peek timer
        if (catPeekManager != null) {
            catPeekManager.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop cat peek timer
        if (catPeekManager != null) {
            catPeekManager.stop();
        }
    }

    private void initViews(View view) {
        statusContainer = view.findViewById(R.id.status_container);
        ivStatusIcon = view.findViewById(R.id.iv_status_icon);
        tvStatusTitle = view.findViewById(R.id.tv_status_title);
        tvStatusDesc = view.findViewById(R.id.tv_status_desc);
        tvVersion = view.findViewById(R.id.tv_version);
        tvSearchEngine = view.findViewById(R.id.tv_search_engine);
        cardAiSettings = view.findViewById(R.id.card_ai_settings);
        cardWebSearch = view.findViewById(R.id.card_web_search);
        cardHowToUse = view.findViewById(R.id.card_how_to_use);
        btnInfo = view.findViewById(R.id.btn_info);

        // Initialize icons for candy colors
        iconInvocation = view.findViewById(R.id.icon_invocation);
        iconWebSearch = view.findViewById(R.id.icon_web_search);

        // Initialize cat peek
        View catPeekContainer = view.findViewById(R.id.cat_peek_container);
        catPeekManager = new CatPeekManager(requireContext());
        catPeekManager.init(catPeekContainer);
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
            int dynamicStrokeColor = com.google.android.material.color.MaterialColors.getColor(view,
                    com.google.android.material.R.attr.colorOutlineVariant,
                    ContextCompat.getColor(requireContext(), R.color.divider_dark));
            ((MaterialCardView) view).setStrokeColor(dynamicStrokeColor);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAmoledToCards(group.getChildAt(i));
            }
        }
    }

    /**
     * Apply candy colors to quick action icons when Material You is disabled.
     */
    // CandyColors removed and replaced by manual color setting if needed, but for
    // now we rely on XML attributes.
    // If specific color logic is needed for Material You disabled, it should be
    // done here without the helper.
    // For example, tinting icons with ?attr/colorPrimary.
    // Since the XML layouts already use ?attr/colorPrimary or other thematic
    // attributes, removing this call should revert to default theme behavior.

    private void setupClickListeners() {
        cardAiSettings.setOnClickListener(v -> {
            // Navigate to AI Invocation fragment
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToAiInvocation();
            }
        });

        cardHowToUse.setOnClickListener(v -> {
            // Open how to use dialog or activity
            showHowToUseDialog();
        });

        cardWebSearch.setOnClickListener(v -> {
            showSearchEngineBottomSheet();
        });

        btnInfo.setOnClickListener(v -> showInfoBottomSheet());
    }

    private String getSearchEngineName(String engineId) {
        for (String[] engine : SEARCH_ENGINES) {
            if (engine[0].equals(engineId)) {
                return engine[1];
            }
        }
        return "DuckDuckGo";
    }

    private void showSearchEngineBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_search_engine, null);

        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        LinearLayout optionsContainer = sheetView.findViewById(R.id.options_container);
        String currentEngine = SPManager.isReady() ? SPManager.getInstance().getSearchEngine() : "duckduckgo";

        for (String[] engine : SEARCH_ENGINES) {
            View optionView = LayoutInflater.from(requireContext()).inflate(R.layout.item_search_engine_option,
                    optionsContainer, false);

            TextView tvName = optionView.findViewById(R.id.tv_option_name);
            View checkMark = optionView.findViewById(R.id.check_mark);
            View container = optionView.findViewById(R.id.option_container);

            tvName.setText(engine[1]);
            checkMark.setVisibility(engine[0].equals(currentEngine) ? View.VISIBLE : View.INVISIBLE);

            // Set selected background and text color
            if (engine[0].equals(currentEngine)) {
                container.setBackgroundResource(R.drawable.bg_search_option_selected);

                int colorOnPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(optionView,
                        com.google.android.material.R.attr.colorOnPrimaryContainer);
                tvName.setTextColor(colorOnPrimaryContainer);
            }

            optionView.setOnClickListener(v -> {
                if (SPManager.isReady()) {
                    SPManager.getInstance().setSearchEngine(engine[0]);
                    tvSearchEngine.setText(engine[1]);
                }
                dialog.dismiss();
            });

            optionsContainer.addView(optionView);
        }

        dialog.show();
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

        tvTitle.setText(R.string.about_home_title);
        tvDescription.setText(R.string.about_home_description);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showHowToUseDialog() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_how_to_use, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void updateUI() {
        // CRASH GUARD: this runs from postDelayed callbacks and the static
        // XposedService bind listener — both can fire after the fragment is
        // detached (fast app close), where requireContext()/getResources()
        // throw IllegalStateException and kill the process.
        if (!isAdded()) return;

        // Update version
        tvVersion.setText("v" + BuildConfig.VERSION_NAME);

        // Check module status
        moduleStatus = checkModuleStatus();
        updateStatusCard();

        // Update search engine info
        updateSearchEngineInfo();
    }

    private void updateSearchEngineInfo() {
        if (!isAdded()) return;
        if (SPManager.isReady()) {
            String currentEngine = SPManager.getInstance().getSearchEngine();
            tvSearchEngine.setText(getSearchEngineName(currentEngine));
        }
    }

    // A heartbeat written by MainHook from inside a hooked keyboard process is
    // direct proof LSPosed loaded the module. It goes stale after this window
    // so a disabled module eventually reports NOT_ACTIVE again.
    private static final long HEARTBEAT_FRESH_MS = 24L * 60 * 60 * 1000; // 24h

    private int checkModuleStatus() {
        // Signal 1 (live): Modern Xposed API (100+) — LSPosed delivers the
        // XposedService binder to this module app ONLY when the module is
        // enabled in the manager.
        if (isServiceBoundActive || (sXposedService != null && sXposedService.getApiVersion() > 0)
                || tn.eluea.kgpt.util.LSPosedHelper.isLSPosedActive()) {
            return STATUS_ACTIVE;
        }

        // Signal 2 (ground truth, near-real-time): MainHook only writes this
        // heartbeat from inside processes where the framework ACTUALLY loaded
        // the module (e.g. an active keyboard). Covers cases where the
        // XposedService binder was never delivered to this app instance
        // (enable-after-launch, daemon restarts, delivery races).
        if (isActivationHeartbeatFresh()) {
            return STATUS_ACTIVE;
        }

        return STATUS_NOT_ACTIVE;
    }

    private boolean isActivationHeartbeatFresh() {
        // P1: prefer the Remote Preferences copy (written by MainHook via the
        // framework) — works even if the provider path is unavailable.
        try {
            XposedService svc = tn.eluea.kgpt.util.LSPosedHelper.getService();
            if (svc != null) {
                android.content.SharedPreferences remote =
                        svc.getRemotePreferences("kgpt_heartbeat");
                long ts = remote.getLong("ts", 0L);
                long age = System.currentTimeMillis() - ts;
                if (ts > 0 && age >= 0 && age < HEARTBEAT_FRESH_MS) return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (!SPManager.isReady()) {
                return false;
            }
            String raw = SPManager.getInstance().getConfigClient()
                    .getString("module_activation_heartbeat", null);
            if (raw == null || raw.isEmpty()) {
                return false;
            }
            long heartbeat = Long.parseLong(raw.trim());
            long age = System.currentTimeMillis() - heartbeat;
            return heartbeat > 0 && age >= 0 && age < HEARTBEAT_FRESH_MS;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStatusCard() {
        if (!isAdded()) return; // delayed callbacks may outlive the fragment
        switch (moduleStatus) {
            case STATUS_ACTIVE:
                statusContainer.setBackgroundResource(R.drawable.bg_status_active);
                ivStatusIcon.setImageResource(R.drawable.ic_shield_tick_filled);
                ivStatusIcon.setColorFilter(getResources().getColor(R.color.success, requireContext().getTheme()));
                tvStatusTitle.setText(R.string.module_active);
                tvStatusTitle.setTextColor(getResources().getColor(R.color.success, requireContext().getTheme()));
                tvStatusDesc.setText(R.string.module_active_desc);
                tvStatusDesc.setTextColor(getResources().getColor(R.color.success, requireContext().getTheme()));
                break;

            case STATUS_NOT_ACTIVE:
            default:
                statusContainer.setBackgroundResource(R.drawable.bg_status_inactive);
                ivStatusIcon.setImageResource(R.drawable.ic_shield_cross_filled);
                ivStatusIcon.setColorFilter(getResources().getColor(R.color.error, requireContext().getTheme()));
                tvStatusTitle.setText(R.string.module_inactive);
                tvStatusTitle.setTextColor(getResources().getColor(R.color.error, requireContext().getTheme()));
                tvStatusDesc.setText(R.string.module_inactive_desc);
                tvStatusDesc.setTextColor(getResources().getColor(R.color.error, requireContext().getTheme()));
                break;
        }
    }
}
