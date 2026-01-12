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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing an app trigger configuration
 */
public class AppTrigger {
    private String packageName;
    private String activityName; // The launcher activity class name (ComponentName)
    private String appName;
    private String trigger;
    private boolean enabled;

    public AppTrigger(String packageName, String activityName, String appName, String trigger) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.appName = appName;
        this.trigger = trigger;
        this.enabled = true;
    }

    // Legacy constructor for backward compatibility
    public AppTrigger(String packageName, String appName, String trigger) {
        this(packageName, null, appName, trigger);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the default trigger from app name (first word)
     */
    public static String getDefaultTrigger(String appName) {
        if (appName == null || appName.isEmpty()) {
            return "";
        }
        String[] parts = appName.trim().split("\\s+");
        return parts[0].toLowerCase();
    }

    public static String encode(List<AppTrigger> triggers) {
        JSONArray jsonArray = new JSONArray();
        for (AppTrigger trigger : triggers) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("packageName", trigger.getPackageName());
                obj.put("activityName", trigger.getActivityName());
                obj.put("appName", trigger.getAppName());
                obj.put("trigger", trigger.getTrigger());
                obj.put("enabled", trigger.isEnabled());
                jsonArray.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray.toString();
    }

    public static List<AppTrigger> decode(String encoded) {
        List<AppTrigger> triggers = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return triggers;
        }
        try {
            JSONArray jsonArray = new JSONArray(encoded);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                AppTrigger trigger = new AppTrigger(
                        obj.getString("packageName"),
                        obj.optString("activityName", null), // Optional for backward compatibility
                        obj.getString("appName"),
                        obj.getString("trigger"));
                trigger.setEnabled(obj.optBoolean("enabled", true));
                triggers.add(trigger);
            }
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.log(e);
        }
        return triggers;
    }
}
