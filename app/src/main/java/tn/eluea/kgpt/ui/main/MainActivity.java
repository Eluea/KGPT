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
import tn.eluea.kgpt.ui.main.fragments.AiSettingsFragment;
import tn.eluea.kgpt.ui.main.fragments.ApiKeysFragment;
import tn.eluea.kgpt.ui.main.fragments.HomeFragment;
import tn.eluea.kgpt.ui.main.fragments.ModelsFragment;
import tn.eluea.kgpt.ui.main.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_THEME = "theme_mode";
    private static final String PREF_AMOLED = "amoled_mode";
    private static final String KEY_NAV_INDEX = "nav_index";

    private FrameLayout navHome, navModels, navLab, navSettings;
    private ImageView navHomeIcon, navModelsIcon, navLabIcon, navSettingsIcon;
    private LinearLayout floatingDock;
    private LinearLayout navItemsContainer;
    private LinearLayout dockActionContainer;
    private ImageView dockActionIcon; // For the action icon
    private android.widget.TextView dockActionText; // For action text
    private int currentNavIndex = 0;
    private boolean isAmoledMode = false;

    private tn.eluea.kgpt.ui.view.SnowfallView snowfallView;
    private static final String PREF_WINTER_MODE = "winter_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme is handled globally by KGPTApplication and MaterialYouManager
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        SPManager.init(this);
        initViews();
        setupNavigation();
        setupWindowInsets();
        setupBackStackListener();

        // Initialize Winter Mode
        snowfallView = findViewById(R.id.snowfall_view);
        applyWinterMode();

        // Handle Back Press
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    // Restore Home selection depends on backstack listener, but we can hint it here
                    if (getSupportFragmentManager().getBackStackEntryCount() == 1) { // Will become 0
                        updateNavSelection(0);
                    }
                } else {
                    // Default back behavior (finish activity)
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        // Restore navigation state
        if (savedInstanceState != null) {
            currentNavIndex = savedInstanceState.getInt(KEY_NAV_INDEX, 0);
            loadFragmentForIndex(currentNavIndex);
            updateNavSelection(currentNavIndex);
        } else {
            loadFragment(new HomeFragment());
            updateNavSelection(0);

            // Check for updates on first launch
            checkForUpdates();
        }
    }

    public void applyWinterMode() {
        if (snowfallView == null)
            return;

        SharedPreferences prefs = getSharedPreferences("keyboard_gpt_ui", android.content.Context.MODE_PRIVATE);
        boolean isWinterMode = prefs.getBoolean(PREF_WINTER_MODE, false);

        if (isWinterMode) {
            snowfallView.setVisibility(View.VISIBLE);
            snowfallView.bringToFront();
        } else {
            snowfallView.setVisibility(View.GONE);
        }
    }

    /**
     * Check for updates and show bottom sheet if available.
     * Only shows automatically if auto-update checking is enabled in settings.
     * Manual checks via "Check for Updates" button bypass this setting.
     */
    private void checkForUpdates() {
        // Only check automatically if auto-updates are enabled
        if (!SPManager.isReady() || !SPManager.getInstance().getUpdateCheckEnabled()) {
            // Auto-updates disabled - don't show update card on app launch
            return;
        }

        // Show cached update if available (from background check)
        tn.eluea.kgpt.updater.UpdateInfo cachedUpdate = tn.eluea.kgpt.updater.UpdateWorker.getCachedUpdate(this);

        if (cachedUpdate != null) {
            // Small delay to let the UI settle
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                tn.eluea.kgpt.updater.UpdateBottomSheet.showCachedUpdate(this);
            }, 1000);
        } else {
            // Trigger a fresh background check
            // This will only show the dialog if an update is found
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                tn.eluea.kgpt.updater.UpdateBottomSheet.checkAndShow(this);
            }, 2000);
        }
    }

    private void setupBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            // When backstack becomes empty (all feature fragments popped), we're back at
            // Home
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                // Restore navigation dock when returning to base fragments (Home, Settings,
                // etc.)
                showDockNavigation();
            }
        });
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

    // applyAmoledThemeIfNeeded removed - handled globally by MaterialYouManager

    // Manual AMOLED coloring removed as Theme.KGPT.AMOLED handles it globally.

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navModels = findViewById(R.id.nav_models);
        navLab = findViewById(R.id.nav_lab);
        navSettings = findViewById(R.id.nav_settings);

        navHomeIcon = findViewById(R.id.nav_home_icon);
        navModelsIcon = findViewById(R.id.nav_models_icon);
        navLabIcon = findViewById(R.id.nav_lab_icon);
        navSettingsIcon = findViewById(R.id.nav_settings_icon);

        floatingDock = findViewById(R.id.floating_dock);
        navItemsContainer = findViewById(R.id.nav_items_container);
        dockActionContainer = findViewById(R.id.dock_action_container);
        dockActionIcon = findViewById(R.id.dock_action_icon);
        dockActionText = findViewById(R.id.dock_action_text);
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
                loadFragment(new AiSettingsFragment());
                updateNavSelection(1);
            }
        });

        navLab.setOnClickListener(v -> {
            if (currentNavIndex != 2) {
                loadFragment(new tn.eluea.kgpt.ui.lab.LabFragment());
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
        // Reset snow obstacles for smooth transition
        if (snowfallView != null) {
            snowfallView.shakeOff();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void updateNavSelection(int index) {
        currentNavIndex = index;

        // Reset all
        navHome.setSelected(false);
        navModels.setSelected(false);
        navLab.setSelected(false);
        navSettings.setSelected(false);

        int inactiveColor = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                getResources().getColor(R.color.dock_item_inactive, getTheme()));
        int activeColor = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnPrimary, getResources().getColor(R.color.white, getTheme()));
        int primaryColor = com.google.android.material.color.MaterialColors.getColor(this,
                androidx.appcompat.R.attr.colorPrimary, ContextCompat.getColor(this, R.color.primary));

        navHomeIcon.setColorFilter(inactiveColor);
        navModelsIcon.setColorFilter(inactiveColor);
        navLabIcon.setColorFilter(inactiveColor);
        navSettingsIcon.setColorFilter(inactiveColor);

        // Reset backgrounds
        navHome.setBackgroundTintList(null);
        navModels.setBackgroundTintList(null);
        navLab.setBackgroundTintList(null);
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
                navLab.setSelected(true);
                navLabIcon.setColorFilter(activeColor);
                navLab.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
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
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, new AiInvocationFragment());
        transaction.addToBackStack("ai_invocation");
        transaction.commit();
        updateNavSelection(-1);
    }

    public void navigateToLab() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, new tn.eluea.kgpt.ui.lab.LabFragment());
        transaction.addToBackStack("lab");
        transaction.commit();
        updateNavSelection(-1);
    }

    public void navigateToAppTrigger() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, new tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerFragment());
        transaction.addToBackStack("app_trigger");
        transaction.commit();
        updateNavSelection(-1);
    }

    public void navigateToTextActions() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, new tn.eluea.kgpt.ui.lab.textactions.TextActionsFragment());
        transaction.addToBackStack("text_actions");
        transaction.commit();
        updateNavSelection(-1);
    }

    // onBackPressed removed. Handled by OnBackPressedDispatcher in onCreate.

    public void setDockAction(String text, int iconRes, View.OnClickListener listener) {
        if (floatingDock == null || navItemsContainer == null || dockActionContainer == null)
            return;

        // Check if we're switching from navigation mode to action mode
        boolean isFromNavigation = navItemsContainer.getVisibility() == View.VISIBLE;

        // Check if we're already in action mode (switching between actions)
        boolean isAlreadyInActionMode = dockActionContainer.getVisibility() == View.VISIBLE;

        if (isFromNavigation) {
            // Transition from Navigation to Action mode with animation
            animateNavToActionMode(text, iconRes, listener);
        } else if (isAlreadyInActionMode) {
            // Already in action mode - animate content change
            animateDockContentChange(text, iconRes, listener);
        } else {
            // Fallback: just set it up directly
            navItemsContainer.setVisibility(View.GONE);
            dockActionContainer.setVisibility(View.VISIBLE);
            dockActionText.setText(text);
            dockActionIcon.setImageResource(iconRes);
            applyDockActionStyle();
            floatingDock.setOnClickListener(listener);
        }
    }

    private void animateNavToActionMode(String text, int iconRes, View.OnClickListener listener) {
        // Fade out navigation items
        navItemsContainer.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(120)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Start smooth width transition
                    android.transition.Transition transition = new android.transition.ChangeBounds();
                    transition.setDuration(300);
                    transition.setInterpolator(new android.view.animation.DecelerateInterpolator());
                    android.transition.TransitionManager.beginDelayedTransition(floatingDock, transition);

                    // Hide nav, show action
                    navItemsContainer.setVisibility(View.GONE);
                    navItemsContainer.setAlpha(1f);
                    navItemsContainer.setScaleX(1f);
                    navItemsContainer.setScaleY(1f);

                    // Set up action content
                    dockActionText.setText(text);
                    dockActionIcon.setImageResource(iconRes);
                    applyDockActionStyle();
                    floatingDock.setOnClickListener(listener);

                    // Prepare for animation
                    dockActionContainer.setAlpha(0f);
                    dockActionContainer.setScaleX(0.95f);
                    dockActionContainer.setScaleY(0.95f);
                    dockActionIcon.setTranslationX(15f);
                    dockActionIcon.setAlpha(0f);
                    dockActionText.setTranslationX(15f);
                    dockActionText.setAlpha(0f);
                    dockActionContainer.setVisibility(View.VISIBLE);

                    // Animate container in with expand
                    dockActionContainer.animate()
                            .alpha(1f)
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(100)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> {
                                dockActionContainer.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(120)
                                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                                        .start();
                            })
                            .start();

                    // Animate content sliding in
                    dockActionIcon.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .setStartDelay(30)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();

                    dockActionText.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .setStartDelay(60)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void animateDockContentChange(String newText, int newIconRes, View.OnClickListener newListener) {
        // Animate icon and text separately for smoother effect

        // First, fade out just the content (icon + text) while keeping container
        // visible
        dockActionIcon.animate()
                .alpha(0f)
                .translationX(-10f)
                .setDuration(100)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .start();

        dockActionText.animate()
                .alpha(0f)
                .translationX(-10f)
                .setDuration(100)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Update content while faded out
                    dockActionText.setText(newText);
                    dockActionIcon.setImageResource(newIconRes);
                    applyDockActionStyle();
                    floatingDock.setOnClickListener(newListener);

                    // Reset position for incoming animation
                    dockActionIcon.setTranslationX(10f);
                    dockActionText.setTranslationX(10f);

                    // Subtle stretch/expand on the container
                    dockActionContainer.animate()
                            .scaleX(1.03f)
                            .scaleY(1.03f)
                            .setDuration(80)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> {
                                // Settle back to normal
                                dockActionContainer.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(120)
                                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                                        .start();
                            })
                            .start();

                    // Fade in new content with slide
                    dockActionIcon.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();

                    dockActionText.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .setStartDelay(30) // Slight stagger for elegance
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void applyDockActionStyle() {
        int primaryContainer = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorPrimaryContainer,
                ContextCompat.getColor(this, R.color.primary_light));
        int onPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                ContextCompat.getColor(this, R.color.primary));

        floatingDock.setBackgroundTintList(ColorStateList.valueOf(primaryContainer));
        dockActionText.setTextColor(onPrimaryContainer);
        dockActionIcon.setColorFilter(onPrimaryContainer);
    }

    public void showDockNavigation() {
        if (floatingDock == null || navItemsContainer == null || dockActionContainer == null)
            return;

        // Start smooth width transition
        android.transition.Transition transition = new android.transition.ChangeBounds();
        transition.setDuration(300);
        transition.setInterpolator(new android.view.animation.DecelerateInterpolator());
        android.transition.TransitionManager.beginDelayedTransition(floatingDock, transition);

        dockActionContainer.setVisibility(View.GONE);
        navItemsContainer.setVisibility(View.VISIBLE);

        // Restore Dock Style
        int surfaceContainer = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainer,
                ContextCompat.getColor(this, R.color.container_background));

        floatingDock.setBackgroundTintList(ColorStateList.valueOf(surfaceContainer));
        floatingDock.setOnClickListener(null); // Disable action click
        floatingDock.setClickable(false); // Let events pass to children (but items are clickable)
        // Actually items capture clicks. floating_dock layout click is irrelevant if
        // children handle it.
        // But setting null is safer.
    }

    public void updateSnowObstacles(java.util.List<android.graphics.Rect> obstacles) {
        if (snowfallView != null) {
            snowfallView.updateObstacles(obstacles);
        }
    }

    public void onContentScrolled() {
        if (snowfallView != null) {
            snowfallView.shakeOff();
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (snowfallView != null && snowfallView.getVisibility() == View.VISIBLE) {
            int action = ev.getAction();
            boolean active = (action == android.view.MotionEvent.ACTION_DOWN
                    || action == android.view.MotionEvent.ACTION_MOVE);
            snowfallView.updateFinger(ev.getX(), ev.getY(), active);
        }
        return super.dispatchTouchEvent(ev);
    }
}
