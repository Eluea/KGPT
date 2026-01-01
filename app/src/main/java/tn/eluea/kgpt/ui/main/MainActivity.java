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
package tn.eluea.kgpt.ui.main;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.ui.main.fragments.AiInvocationFragment;
import tn.eluea.kgpt.ui.main.fragments.ApiKeysFragment;
import tn.eluea.kgpt.ui.main.fragments.HomeFragment;
import tn.eluea.kgpt.ui.main.fragments.ModelsFragment;
import tn.eluea.kgpt.ui.main.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_THEME = "theme_mode";
    private static final String PREF_AMOLED = "amoled_mode";
    private static final String KEY_NAV_INDEX = "nav_index";

    private FrameLayout navHome, navModels, navApi, navSettings;
    private ImageView navHomeIcon, navModelsIcon, navApiIcon, navSettingsIcon;
    private LinearLayout floatingDock;
    private int currentNavIndex = 0;
    private boolean isAmoledMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply AMOLED theme if needed (dark mode is handled by KGPTApplication)
        applyAmoledThemeIfNeeded();
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        SPManager.init(this);
        initViews();
        setupNavigation();
        setupWindowInsets();
        applyAmoledColors();

        // Restore navigation state
        if (savedInstanceState != null) {
            currentNavIndex = savedInstanceState.getInt(KEY_NAV_INDEX, 0);
            loadFragmentForIndex(currentNavIndex);
            updateNavSelection(currentNavIndex);
        } else {
            loadFragment(new HomeFragment());
            updateNavSelection(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_NAV_INDEX, currentNavIndex);
    }

    private void loadFragmentForIndex(int index) {
        Fragment fragment;
        switch (index) {
            case 1:
                fragment = new ModelsFragment();
                break;
            case 2:
                fragment = new ApiKeysFragment();
                break;
            case 3:
                fragment = new SettingsFragment();
                break;
            default:
                fragment = new HomeFragment();
                break;
        }
        loadFragment(fragment);
    }

    /**
     * Apply AMOLED theme if enabled.
     * Note: Dark mode is handled globally by KGPTApplication.
     */
    private void applyAmoledThemeIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("keyboard_gpt_ui", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);
        isAmoledMode = prefs.getBoolean(PREF_AMOLED, false);

        if (isDarkMode && isAmoledMode) {
            setTheme(R.style.Theme_KeyboardGPT_AMOLED);
        }
    }

    private void applyAmoledColors() {
        if (isAmoledMode) {
            // Apply AMOLED colors to dock
            floatingDock.setBackgroundResource(R.drawable.bg_dock_amoled);
            
            // Apply AMOLED background to main view
            View coordinator = findViewById(R.id.coordinator);
            coordinator.setBackgroundColor(ContextCompat.getColor(this, R.color.background_amoled));
        }
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navModels = findViewById(R.id.nav_models);
        navApi = findViewById(R.id.nav_api);
        navSettings = findViewById(R.id.nav_settings);

        navHomeIcon = findViewById(R.id.nav_home_icon);
        navModelsIcon = findViewById(R.id.nav_models_icon);
        navApiIcon = findViewById(R.id.nav_api_icon);
        navSettingsIcon = findViewById(R.id.nav_settings_icon);
        
        floatingDock = findViewById(R.id.floating_dock);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            if (currentNavIndex != 0) {
                loadFragment(new HomeFragment());
                updateNavSelection(0);
            }
        });

        navModels.setOnClickListener(v -> {
            if (currentNavIndex != 1) {
                loadFragment(new ModelsFragment());
                updateNavSelection(1);
            }
        });

        navApi.setOnClickListener(v -> {
            if (currentNavIndex != 2) {
                loadFragment(new ApiKeysFragment());
                updateNavSelection(2);
            }
        });

        navSettings.setOnClickListener(v -> {
            if (currentNavIndex != 3) {
                loadFragment(new SettingsFragment());
                updateNavSelection(3);
            }
        });
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.coordinator);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void updateNavSelection(int index) {
        currentNavIndex = index;

        // Reset all
        navHome.setSelected(false);
        navModels.setSelected(false);
        navApi.setSelected(false);
        navSettings.setSelected(false);

        int inactiveColor = getResources().getColor(R.color.dock_item_inactive, getTheme());
        int activeColor = getResources().getColor(R.color.white, getTheme());
        int primaryColor = ContextCompat.getColor(this, R.color.primary);

        navHomeIcon.setColorFilter(inactiveColor);
        navModelsIcon.setColorFilter(inactiveColor);
        navApiIcon.setColorFilter(inactiveColor);
        navSettingsIcon.setColorFilter(inactiveColor);

        // Reset backgrounds
        navHome.setBackgroundTintList(null);
        navModels.setBackgroundTintList(null);
        navApi.setBackgroundTintList(null);
        navSettings.setBackgroundTintList(null);

        // Set selected with dynamic theme color
        switch (index) {
            case 0:
                navHome.setSelected(true);
                navHomeIcon.setColorFilter(activeColor);
                navHome.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                break;
            case 1:
                navModels.setSelected(true);
                navModelsIcon.setColorFilter(activeColor);
                navModels.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                break;
            case 2:
                navApi.setSelected(true);
                navApiIcon.setColorFilter(activeColor);
                navApi.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                break;
            case 3:
                navSettings.setSelected(true);
                navSettingsIcon.setColorFilter(activeColor);
                navSettings.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
                break;
        }
    }

    public void navigateToModels() {
        loadFragment(new ModelsFragment());
        updateNavSelection(1);
    }

    public void navigateToApiKeys() {
        loadFragment(new ApiKeysFragment());
        updateNavSelection(2);
    }

    public void navigateToAiInvocation() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        transaction.replace(R.id.fragment_container, new AiInvocationFragment());
        transaction.addToBackStack("ai_invocation");
        transaction.commit();
        updateNavSelection(-1);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            // Restore Home selection after going back
            updateNavSelection(0);
        } else {
            super.onBackPressed();
        }
    }
}
