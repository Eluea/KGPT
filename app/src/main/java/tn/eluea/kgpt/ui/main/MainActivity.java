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
package tn.eluea.kgpt.ui.main;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.Intent;
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

    public static final String EXTRA_OPEN_ADD_CUSTOM_PROVIDER = "tn.eluea.kgpt.OPEN_ADD_CUSTOM_PROVIDER";

    private static final String PREF_THEME = "theme_mode";
    private static final String PREF_AMOLED = "amoled_mode";
    private static final String KEY_NAV_INDEX = "nav_index";

    private FrameLayout navHome, navModels, navLab, navSettings;
    private View navSlidingIndicator;
    private com.airbnb.lottie.LottieAnimationView navHomeLottie, navModelsLottie, navLabLottie, navSettingsLottie;
    private LinearLayout floatingDock;
    private FrameLayout navItemsWrapper;
    private LinearLayout navItemsContainer;
    private LinearLayout dockActionContainer;
    private ImageView dockActionIcon; // For the action icon
    private com.airbnb.lottie.LottieAnimationView dockActionLottie; // For Lottie action icon
    private android.widget.TextView dockActionText; // For action text
    private int currentNavIndex = 0;
    private boolean isAmoledMode = false;

    private tn.eluea.kgpt.ui.view.SnowfallView snowfallView;
    private static final String PREF_WINTER_MODE = "winter_mode";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(tn.eluea.kgpt.util.LocaleHelper.onAttach(newBase));
    }

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
        maybeOpenAddCustomProvider(getIntent());

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        maybeOpenAddCustomProvider(intent);
    }

    private void maybeOpenAddCustomProvider(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_ADD_CUSTOM_PROVIDER, false)) return;
        intent.removeExtra(EXTRA_OPEN_ADD_CUSTOM_PROVIDER);
        navigateToModels();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(androidx.fragment.app.FragmentManager fm, androidx.fragment.app.Fragment f, View v, Bundle savedInstanceState) {
                if (f instanceof ModelsFragment) {
                    fm.unregisterFragmentLifecycleCallbacks(this);
                    v.post(() -> ((ModelsFragment) f).openAddCustomProviderDialog());
                }
            }
        }, true);
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
                fragment = new tn.eluea.kgpt.ui.lab.LabFragment();
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

        navHomeLottie = findViewById(R.id.nav_home_lottie);
        navModelsLottie = findViewById(R.id.nav_models_lottie);
        navLabLottie = findViewById(R.id.nav_lab_lottie);
        navSettingsLottie = findViewById(R.id.nav_settings_lottie);

        floatingDock = findViewById(R.id.floating_dock);
        navItemsWrapper = findViewById(R.id.nav_items_wrapper);
        navSlidingIndicator = findViewById(R.id.nav_sliding_indicator);
        navItemsContainer = findViewById(R.id.nav_items_container);
        dockActionContainer = findViewById(R.id.dock_action_container);
        dockActionIcon = findViewById(R.id.dock_action_icon);
        dockActionLottie = findViewById(R.id.dock_action_lottie);
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
                // Unified with restore path (loadFragmentForIndex) and
                // navigateToModels — three screens shared one dock slot before.
                loadFragment(new ModelsFragment());
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

    private int navGeneration = 0;

    private void updateNavSelection(int index) {
        currentNavIndex = index;
        final int gen = ++navGeneration;

        // Reset all selection states
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

        FrameLayout targetView;
        com.airbnb.lottie.LottieAnimationView targetLottie;

        switch (index) {
            case 1:
                targetView = navModels;
                targetLottie = navModelsLottie;
                break;
            case 2:
                targetView = navLab;
                targetLottie = navLabLottie;
                break;
            case 3:
                targetView = navSettings;
                targetLottie = navSettingsLottie;
                break;
            case 0:
            default:
                targetView = navHome;
                targetLottie = navHomeLottie;
                break;
        }

        // Reset all NON-target icons to static frame 0 with inactive color.
        // (The target is intentionally NOT reset here — resetting it queued a
        // deferred re-apply that fired AFTER playOnce() and killed the active
        // animation, leaving the selected icon dark and the transition cut.)
        applyNavState(navHomeLottie, 0, inactiveColor, gen, false, navHomeLottie != targetLottie);
        applyNavState(navModelsLottie, 0, inactiveColor, gen, false, navModelsLottie != targetLottie);
        applyNavState(navLabLottie, 0, inactiveColor, gen, false, navLabLottie != targetLottie);
        applyNavState(navSettingsLottie, 0, inactiveColor, gen, false, navSettingsLottie != targetLottie);

        if (index < 0) {
            if (navSlidingIndicator != null) {
                navSlidingIndicator.setVisibility(View.GONE);
            }
            return;
        }

        if (navSlidingIndicator != null) {
            navSlidingIndicator.setVisibility(View.VISIBLE);
        }

        if (targetView != null) {
            targetView.setSelected(true);
            // Active icon: animated one-shot transition in the active color.
            applyNavState(targetLottie, 0, activeColor, gen, true, true);

            if (navSlidingIndicator != null) {
                navSlidingIndicator.setBackgroundTintList(ColorStateList.valueOf(primaryColor));

                Runnable movePill = () -> {
                    float targetX = targetView.getX();
                    navSlidingIndicator.animate()
                            .x(targetX)
                            .setDuration(280)
                            .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                            .start();
                };

                if (targetView.getWidth() == 0) {
                    targetView.post(movePill);
                } else {
                    movePill.run();
                }
            }
        }
    }

    /**
     * Apply a Lottie nav state, generation-guarded.
     *
     * Lottie compositions load ASYNC — a frame/color set before the
     * composition is ready is silently dropped (the invisible-white-home-icon
     * bug), so we re-apply once after layout. The generation tag makes stale
     * deferred applies from a previous selection a no-op, and the deferred
     * re-apply is skipped entirely when the composition is already loaded so
     * playOnce() animations are never cut mid-flight (the dark-selected-icon
     * + non-smooth-slide regression).
     */
    private void applyNavState(com.airbnb.lottie.LottieAnimationView lottie, int frame,
            int color, int gen, boolean playActive, boolean apply) {
        if (lottie == null || !apply) return;
        lottie.setTag(gen);

        if (playActive) {
            tn.eluea.kgpt.util.LottieHelper.playOnce(lottie, color);
        } else {
            tn.eluea.kgpt.util.LottieHelper.setStaticFrame(lottie, frame, color);
        }

        lottie.post(() -> {
            try {
                Object tag = lottie.getTag();
                if (!(tag instanceof Integer) || (Integer) tag != gen) return; // stale selection
                if (lottie.getComposition() == null) {
                    // Composition finished parsing after our first attempt — retry once
                    if (playActive) {
                        tn.eluea.kgpt.util.LottieHelper.playOnce(lottie, color);
                    } else {
                        tn.eluea.kgpt.util.LottieHelper.setStaticFrame(lottie, frame, color);
                    }
                }
            } catch (Throwable ignored) {}
        });
    }

    public void navigateToModels() {
        loadFragment(new ModelsFragment());
        updateNavSelection(1);
    }

    public void navigateToApiKeys() {
        loadFragment(new ApiKeysFragment());
        updateNavSelection(-1); // ApiKeys is not a dock destination
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
        if (floatingDock == null || dockActionContainer == null)
            return;

        View navView = (navItemsWrapper != null) ? navItemsWrapper : navItemsContainer;
        if (navView == null) return;

        // Check if we're switching from navigation mode to action mode
        boolean isFromNavigation = navView.getVisibility() == View.VISIBLE;

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
            navView.setVisibility(View.GONE);
            dockActionContainer.setVisibility(View.VISIBLE);
            dockActionText.setText(text);
            setupDockActionIcon(iconRes);
            applyDockActionStyle();
            floatingDock.setOnClickListener(listener);
        }
    }

    private void animateNavToActionMode(String text, int iconRes, View.OnClickListener listener) {
        View navView = (navItemsWrapper != null) ? navItemsWrapper : navItemsContainer;
        if (navView == null) return;

        // Cancel any pending animations
        navView.animate().cancel();
        dockActionContainer.animate().cancel();
        dockActionText.animate().cancel();
        if (dockActionIcon != null) dockActionIcon.animate().cancel();
        if (dockActionLottie != null) dockActionLottie.animate().cancel();

        // Fade out navigation items wrapper (including sliding indicator pill)
        navView.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Hide nav wrapper completely
                    navView.setVisibility(View.GONE);
                    navView.setAlpha(1f);
                    navView.setScaleX(1f);
                    navView.setScaleY(1f);

                    // Set up action content
                    dockActionText.setText(text);
                    setupDockActionIcon(iconRes);
                    applyDockActionStyle();
                    floatingDock.setOnClickListener(v -> {
                        if (dockActionLottie != null && dockActionLottie.getVisibility() == View.VISIBLE) {
                            int onPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(this,
                                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                                    ContextCompat.getColor(this, R.color.primary));
                            tn.eluea.kgpt.util.LottieHelper.playOnce(dockActionLottie, onPrimaryContainer);
                        }
                        if (listener != null) {
                            listener.onClick(v);
                        }
                    });

                    // Prepare for animation
                    dockActionContainer.setAlpha(0f);
                    dockActionContainer.setScaleX(0.95f);
                    dockActionContainer.setScaleY(0.95f);
                    dockActionIcon.setTranslationX(10f);
                    dockActionIcon.setAlpha(0f);
                    dockActionLottie.setTranslationX(10f);
                    dockActionLottie.setAlpha(0f);
                    dockActionText.setTranslationX(10f);
                    dockActionText.setAlpha(0f);
                    dockActionContainer.setVisibility(View.VISIBLE);

                    // Animate container in with subtle overshoot
                    dockActionContainer.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(160)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                            .start();

                    // Animate content sliding in
                    View activeIconView = (iconRes == R.drawable.ic_add) ? dockActionLottie : dockActionIcon;
                    activeIconView.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(160)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();

                    dockActionText.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(160)
                            .setStartDelay(20)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void animateDockContentChange(String newText, int newIconRes, View.OnClickListener newListener) {
        // Animate icon and text separately for smoother effect
        View currentIconView = (dockActionLottie != null && dockActionLottie.getVisibility() == View.VISIBLE) ? dockActionLottie : dockActionIcon;
        currentIconView.animate()
                .alpha(0f)
                .translationX(-10f)
                .setDuration(80)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .start();

        dockActionText.animate()
                .alpha(0f)
                .translationX(-10f)
                .setDuration(80)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    // Update content while faded out
                    dockActionText.setText(newText);
                    setupDockActionIcon(newIconRes);
                    applyDockActionStyle();
                    floatingDock.setOnClickListener(v -> {
                        if (dockActionLottie != null && dockActionLottie.getVisibility() == View.VISIBLE) {
                            int onPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(this,
                                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                                    ContextCompat.getColor(this, R.color.primary));
                            tn.eluea.kgpt.util.LottieHelper.playOnce(dockActionLottie, onPrimaryContainer);
                        }
                        if (newListener != null) {
                            newListener.onClick(v);
                        }
                    });

                    // Reset position for incoming animation
                    dockActionIcon.setTranslationX(10f);
                    dockActionLottie.setTranslationX(10f);
                    dockActionText.setTranslationX(10f);

                    // Fade in new content with slide
                    View incomingIconView = (newIconRes == R.drawable.ic_add) ? dockActionLottie : dockActionIcon;
                    incomingIconView.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(150)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();

                    dockActionText.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(150)
                            .setStartDelay(20)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                })
                .start();
    }

    private void setupDockActionIcon(int iconRes) {
        int onPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                ContextCompat.getColor(this, R.color.primary));

        if (iconRes == R.drawable.ic_add) {
            if (dockActionIcon != null) dockActionIcon.setVisibility(View.GONE);
            if (dockActionLottie != null) {
                dockActionLottie.setVisibility(View.VISIBLE);
                tn.eluea.kgpt.util.LottieHelper.playOnce(dockActionLottie, onPrimaryContainer);
            }
        } else {
            if (dockActionLottie != null) dockActionLottie.setVisibility(View.GONE);
            if (dockActionIcon != null) {
                dockActionIcon.setVisibility(View.VISIBLE);
                dockActionIcon.setImageResource(iconRes);
                dockActionIcon.setColorFilter(onPrimaryContainer);
            }
        }
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
        if (dockActionIcon != null) {
            dockActionIcon.setColorFilter(onPrimaryContainer);
        }
        if (dockActionLottie != null && dockActionLottie.getVisibility() == View.VISIBLE) {
            tn.eluea.kgpt.util.LottieHelper.tint(dockActionLottie, onPrimaryContainer);
        }
    }

    public void showDockNavigation() {
        if (floatingDock == null || dockActionContainer == null)
            return;

        View navView = (navItemsWrapper != null) ? navItemsWrapper : navItemsContainer;
        if (navView == null) return;

        // Cancel any pending animations
        dockActionContainer.animate().cancel();
        dockActionText.animate().cancel();
        if (dockActionIcon != null) dockActionIcon.animate().cancel();
        if (dockActionLottie != null) dockActionLottie.animate().cancel();
        navView.animate().cancel();

        dockActionContainer.setVisibility(View.GONE);
        dockActionContainer.setAlpha(0f);

        navView.setVisibility(View.VISIBLE);
        navView.setAlpha(1f);
        navView.setScaleX(1f);
        navView.setScaleY(1f);

        // Restore Dock Style
        int surfaceContainer = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurfaceContainer,
                ContextCompat.getColor(this, R.color.container_background));

        floatingDock.setBackgroundTintList(ColorStateList.valueOf(surfaceContainer));
        floatingDock.setOnClickListener(null); // Disable action click
        floatingDock.setClickable(false); // Let events pass to children

        // Re-align the sliding pill on the current active tab
        updateNavSelection(currentNavIndex >= 0 ? currentNavIndex : 0);
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
