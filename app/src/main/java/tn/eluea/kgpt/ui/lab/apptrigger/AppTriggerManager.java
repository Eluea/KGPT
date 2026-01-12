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
package tn.eluea.kgpt.ui.lab.apptrigger;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tn.eluea.kgpt.provider.ConfigClient;
import tn.eluea.kgpt.provider.XposedConfigReader;

/**
 * Manager class for app triggers feature.
 * Uses XSharedPreferences (via XposedConfigReader) when running in Xposed
 * module context.
 */
public class AppTriggerManager {
    private static final String TAG = "KGPT_AppTrigger";
    private static final String PREF_APP_TRIGGERS = "app_triggers";
    private static final String PREF_APP_TRIGGERS_ENABLED = "app_triggers_enabled";

    private final Context context;
    private final ConfigClient configClient;
    private List<AppTrigger> appTriggers;

    private static AppTriggerManager instance;

    public AppTriggerManager(Context context) {
        this.context = context;
        this.configClient = new ConfigClient(context);
        loadTriggers();
        instance = this;

        // Register for config changes via ContentProvider
        configClient.registerListener(PREF_APP_TRIGGERS, (key, newValue) -> {
            Log.d(TAG, "Config changed for app_triggers via ContentProvider, reloading...");
            loadTriggers();
        });

        Log.d(TAG, "AppTriggerManager initialized with " + appTriggers.size() + " triggers");
        Log.d(TAG, "XSharedPreferences available: " + XposedConfigReader.isAvailable());
    }

    public static AppTriggerManager getInstance() {
        return instance;
    }

    private void loadTriggers() {
        String encoded = null;

        // Try XSharedPreferences first (works in both app and Xposed module)
        if (XposedConfigReader.isAvailable()) {
            encoded = XposedConfigReader.getString(PREF_APP_TRIGGERS, null);
            Log.d(TAG, "loadTriggers() from XSharedPreferences: "
                    + (encoded != null ? encoded.length() + " chars" : "null"));
        }

        // Fallback to ConfigClient (ContentProvider)
        if (encoded == null) {
            encoded = configClient.getString(PREF_APP_TRIGGERS, null);
            Log.d(TAG, "loadTriggers() from ConfigClient: " + (encoded != null ? encoded.length() + " chars" : "null"));
        }

        appTriggers = AppTrigger.decode(encoded);
        Log.d(TAG, "loadTriggers() - loaded " + appTriggers.size() + " triggers");

        // Log debug info
        if (appTriggers.isEmpty()) {
            Log.d(TAG, XposedConfigReader.getDebugInfo());
        }
    }

    public void reloadTriggers() {
        // Force XSharedPreferences to reload from disk
        XposedConfigReader.forceReload();
        XposedConfigReader.clearCache();
        configClient.clearCache();
        loadTriggers();
        Log.d(TAG, "reloadTriggers() - force reloaded, now have " + appTriggers.size() + " triggers");
    }

    public void saveTriggers() {
        String encoded = AppTrigger.encode(appTriggers);
        configClient.putString(PREF_APP_TRIGGERS, encoded);
        Log.d(TAG, "saveTriggers() - saved " + appTriggers.size() + " triggers, encoded length: " + encoded.length());

        // Force sync to ensure XSharedPreferences can read the changes.
        // ContentProvider write is handled, but we notify Xposed module via broadcast
        // or file observer implicitly.
        // Removed Thread.sleep as it is unreliable.
    }

    public List<AppTrigger> getAppTriggers() {
        loadTriggers();
        return appTriggers;
    }

    public void addTrigger(AppTrigger trigger) {
        for (AppTrigger existing : appTriggers) {
            if (existing.getPackageName().equals(trigger.getPackageName())) {
                return;
            }
        }
        appTriggers.add(trigger);
        saveTriggers();
    }

    public void removeTrigger(AppTrigger trigger) {
        appTriggers.removeIf(t -> t.getPackageName().equals(trigger.getPackageName()));
        saveTriggers();
    }

    public void updateTrigger(AppTrigger trigger) {
        for (int i = 0; i < appTriggers.size(); i++) {
            if (appTriggers.get(i).getPackageName().equals(trigger.getPackageName())) {
                appTriggers.set(i, trigger);
                saveTriggers();
                return;
            }
        }
    }

    public boolean isFeatureEnabled() {
        // Try XSharedPreferences first
        if (XposedConfigReader.isAvailable()) {
            boolean enabled = XposedConfigReader.getBoolean(PREF_APP_TRIGGERS_ENABLED, true);
            Log.d(TAG, "isFeatureEnabled() from XSharedPreferences = " + enabled);
            return enabled;
        }

        // Fallback to ConfigClient
        configClient.clearCache();
        boolean enabled = configClient.getBoolean(PREF_APP_TRIGGERS_ENABLED, true);
        Log.d(TAG, "isFeatureEnabled() from ConfigClient = " + enabled);
        return enabled;
    }

    public void setFeatureEnabled(boolean enabled) {
        configClient.putBoolean(PREF_APP_TRIGGERS_ENABLED, enabled);
        Log.d(TAG, "setFeatureEnabled(" + enabled + ")");

        // Removed unreliable Thread.sleep
    }

    public AppTrigger findByTrigger(String triggerText) {
        if (triggerText == null || triggerText.isEmpty()) {
            return null;
        }
        String lowerTrigger = triggerText.toLowerCase().trim();
        for (AppTrigger trigger : appTriggers) {
            if (trigger.isEnabled() && trigger.getTrigger().toLowerCase().equals(lowerTrigger)) {
                return trigger;
            }
        }
        return null;
    }

    public boolean launchApp(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // Bring existing instance to top
                // Removed FLAG_ACTIVITY_RESET_TASK_IF_NEEDED to preserve app state
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch app: " + packageName, e);
        }
        return false;
    }

    public List<InstalledApp> getInstalledApps() {
        List<InstalledApp> apps = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        int flags = PackageManager.GET_ACTIVITIES
                | PackageManager.MATCH_ALL
                | PackageManager.MATCH_DISABLED_COMPONENTS
                | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;

        List<android.content.pm.PackageInfo> packages = pm.getInstalledPackages(flags);

        for (android.content.pm.PackageInfo packageInfo : packages) {
            String packageName = packageInfo.packageName;
            if (packageName == null || packageName.isEmpty())
                continue;

            try {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                if (appInfo == null)
                    continue;

                Intent defaultIntent = pm.getLaunchIntentForPackage(packageName);
                String defaultActivityName = null;

                if (defaultIntent != null) {
                    android.content.pm.ActivityInfo activityInfo = defaultIntent.resolveActivityInfo(pm, 0);
                    if (activityInfo != null) {
                        defaultActivityName = activityInfo.name;
                    }
                }

                if (defaultActivityName == null && packageInfo.activities != null) {
                    for (android.content.pm.ActivityInfo activity : packageInfo.activities) {
                        if (activity.exported) {
                            defaultActivityName = activity.name;
                            break;
                        }
                    }
                }

                if (defaultActivityName == null)
                    continue;

                String appName = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                apps.add(new InstalledApp(appName, packageName, defaultActivityName, icon, isSystemApp));
                Log.d(TAG, "Found app: " + appName + " -> " + packageName + "/" + defaultActivityName);
            } catch (Exception e) {
                Log.e(TAG, "Error loading app: " + packageName, e);
            }
        }

        Collections.sort(apps, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));
        return apps;
    }

    public boolean isAppAdded(String packageName) {
        for (AppTrigger trigger : appTriggers) {
            if (trigger.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
