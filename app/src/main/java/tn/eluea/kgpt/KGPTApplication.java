/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.UserManager;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Application class for KGPT.
 * Handles global theme initialization to ensure consistent theming across all
 * activities.
 */
public class KGPTApplication extends Application {

    private static final String PREF_NAME = "keyboard_gpt_ui";
    private static final String PREF_THEME = "theme_mode";

    private static KGPTApplication instance;
    private final java.util.List<java.lang.ref.WeakReference<android.app.Activity>> activityList = new java.util.ArrayList<>();

    public static KGPTApplication getInstance() {
        return instance;
    }

    public static android.content.Context getContext() {
        return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize SPManager to ensure we can read prefs
        tn.eluea.kgpt.SPManager.init(this);

        // Apply theme globally before any activity is created
        applyGlobalTheme();

        // Sync component state (ProcessTextActivity) with preferences
        syncComponentState();

        // Initialize Update Checker (WorkManager for periodic checks)
        initializeUpdateChecker();

        // Initialize Material You Manager and register callback for global theming
        tn.eluea.kgpt.util.MaterialYouManager manager = tn.eluea.kgpt.util.MaterialYouManager.getInstance(this);
        registerActivityLifecycleCallbacks(new android.app.Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {
                // This is called BEFORE inflation, ensuring theme applies correctly (API 29+)
                manager.applyTheme(activity);
            }

            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {
                activityList.add(new java.lang.ref.WeakReference<>(activity));
                // Fallback for API < 29: Theme might not apply to already inflated views
                // without recreate,
                // but we call it here just in case specific setup is needed.
                // Note: For full Material You support (API 31+), onActivityPreCreated handles
                // it.
                if (android.os.Build.VERSION.SDK_INT < 29) {
                    manager.applyTheme(activity);
                }
            }

            // Unused callbacks
            @Override
            public void onActivityStarted(android.app.Activity activity) {
            }

            @Override
            public void onActivityResumed(android.app.Activity activity) {
            }

            @Override
            public void onActivityPaused(android.app.Activity activity) {
            }

            @Override
            public void onActivityStopped(android.app.Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(android.app.Activity activity) {
                java.util.Iterator<java.lang.ref.WeakReference<android.app.Activity>> it = activityList.iterator();
                while (it.hasNext()) {
                    java.lang.ref.WeakReference<android.app.Activity> ref = it.next();
                    if (ref.get() == null || ref.get() == activity) {
                        it.remove();
                    }
                }
            }
        });
    }

    public void recreateAllActivities() {
        for (java.lang.ref.WeakReference<android.app.Activity> ref : activityList) {
            android.app.Activity activity = ref.get();
            if (activity != null) {
                activity.recreate();
            }
        }
    }

    private void syncComponentState() {
        try {
            tn.eluea.kgpt.provider.ConfigClient configClient = new tn.eluea.kgpt.provider.ConfigClient(this);
            boolean enabled = configClient.getBoolean("text_actions_enabled", true);
            configClient.destroy();

            android.content.pm.PackageManager pm = getPackageManager();
            android.content.ComponentName componentName = new android.content.ComponentName(
                    this, tn.eluea.kgpt.features.textactions.ui.ProcessTextActivity.class);

            int newState = enabled ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            pm.setComponentEnabledSetting(componentName, newState, android.content.pm.PackageManager.DONT_KILL_APP);
            tn.eluea.kgpt.util.Logger
                    .log("KGPTApplication: ProcessTextActivity state synced to: " + (enabled ? "ENABLED" : "DISABLED"));
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.error("KGPTApplication: Failed to sync component state: " + e.getMessage());
        }
    }

    /**
     * Initialize the update checker.
     * Schedules periodic update checks using WorkManager based on user settings.
     */
    private void initializeUpdateChecker() {
        try {
            if (SPManager.isReady() && SPManager.getInstance().getUpdateCheckEnabled()) {
                int intervalHours = SPManager.getInstance().getUpdateCheckInterval();
                tn.eluea.kgpt.updater.UpdateWorker.scheduleUpdateCheck(this, intervalHours);
                tn.eluea.kgpt.util.Logger
                        .log("KGPTApplication: Update checker scheduled every " + intervalHours + " hours");
            } else {
                tn.eluea.kgpt.updater.UpdateWorker.cancelUpdateCheck(this);
                tn.eluea.kgpt.util.Logger.log("KGPTApplication: Update checker disabled");
            }
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.error("KGPTApplication: Failed to initialize update checker: " + e.getMessage());
        }
    }

    /**
     * Apply the saved theme preference globally.
     * This ensures all activities use the correct theme mode.
     */
    private void applyGlobalTheme() {
        // Check if user is unlocked (device not locked)
        // SharedPreferences are not available before unlock
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);
        if (userManager != null && !userManager.isUserUnlocked()) {
            // Device is locked, use default light theme
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(PREF_THEME, false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Static method to apply theme from any context.
     * Call this when theme preference changes.
     */
    public static void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Restarts the application to apply deep system changes (like component
     * enabling/disabling).
     * This kills the process to ensure system cache refresh.
     */
    public static void restartApp(android.content.Context context) {
        restartApp(context, null);
    }

    public static void restartApp(android.content.Context context, Class<?> targetActivity) {
        try {
            android.content.Intent intent;
            if (targetActivity != null) {
                intent = new android.content.Intent(context, targetActivity);
            } else {
                android.content.pm.PackageManager pm = context.getPackageManager();
                intent = pm.getLaunchIntentForPackage(context.getPackageName());
            }

            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }
            // Kill process to force system cache refresh
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.error("Failed to auto-restart: " + e.getMessage());
        }
    }
}
