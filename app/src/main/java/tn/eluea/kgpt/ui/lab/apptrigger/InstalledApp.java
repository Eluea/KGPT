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

import android.graphics.drawable.Drawable;

/**
 * Model class representing an installed app
 */
public class InstalledApp {
    private String appName;
    private String packageName;
    private String activityName;  // The launcher activity class name
    private Drawable icon;
    private boolean isSystemApp;

    public InstalledApp(String appName, String packageName, String activityName, Drawable icon, boolean isSystemApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.activityName = activityName;
        this.icon = icon;
        this.isSystemApp = isSystemApp;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }
}
