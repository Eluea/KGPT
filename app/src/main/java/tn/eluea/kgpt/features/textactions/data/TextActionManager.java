
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
package tn.eluea.kgpt.features.textactions.data;

import android.content.Context;

import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.features.textactions.domain.CustomTextAction;
import tn.eluea.kgpt.features.textactions.TextActionPrompts;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages text actions configuration and state.
 */
public class TextActionManager {

    private static final String PREF_NAME = "keyboard_gpt";
    private static final String PREF_TEXT_ACTIONS_ENABLED = "text_actions_enabled";
    private static final String PREF_TEXT_ACTIONS_LIST = "text_actions_list";
    private static final String PREF_TEXT_ACTIONS_SHOW_LABELS = "text_actions_show_labels";

    private final Context context;
    private final SharedPreferences prefs;
    private boolean featureEnabled;
    private boolean showLabels;
    private Set<TextAction> enabledActions;

    public TextActionManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.enabledActions = new HashSet<>();
        reloadConfig();
    }

    /**
     * Reload configuration from preferences.
     */
    public void reloadConfig() {
        featureEnabled = prefs.getBoolean(PREF_TEXT_ACTIONS_ENABLED, false);
        showLabels = prefs.getBoolean(PREF_TEXT_ACTIONS_SHOW_LABELS, true);

        String actionsJson = prefs.getString(PREF_TEXT_ACTIONS_LIST, null);
        enabledActions = decodeEnabledActions(actionsJson);
    }

    /**
     * Check if the text actions feature is enabled.
     */
    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    /**
     * Check if labels should be shown under icons.
     */
    public boolean shouldShowLabels() {
        return showLabels;
    }

    /**
     * Get list of enabled actions in order.
     */
    public List<TextAction> getEnabledActions() {
        if (enabledActions.isEmpty()) {
            // Return default actions if none configured
            return Arrays.asList(
                    TextAction.REPHRASE,
                    TextAction.FIX_ERRORS,
                    TextAction.IMPROVE,
                    TextAction.EXPAND,
                    TextAction.SHORTEN,
                    TextAction.FORMAL,
                    TextAction.CASUAL,
                    TextAction.TRANSLATE);
        }

        List<TextAction> result = new ArrayList<>();
        for (TextAction action : TextAction.values()) {
            if (enabledActions.contains(action)) {
                result.add(action);
            }
        }
        return result;
    }

    /**
     * Check if a specific action is enabled.
     */
    public boolean isActionEnabled(TextAction action) {
        if (enabledActions.isEmpty()) {
            // Default enabled actions (all enabled by default now)
            return true;
        }
        return enabledActions.contains(action);
    }

    /**
     * Decode enabled actions from JSON string.
     */
    private Set<TextAction> decodeEnabledActions(String json) {
        Set<TextAction> actions = new HashSet<>();
        if (json == null || json.isEmpty()) {
            return actions;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String actionName = array.getString(i);
                try {
                    actions.add(TextAction.valueOf(actionName));
                } catch (IllegalArgumentException ignored) {
                    // Unknown action, skip
                }
            }
        } catch (JSONException e) {
            tn.eluea.kgpt.util.Logger.log(e);
        }

        return actions;
    }

    /**
     * Encode enabled actions to JSON string.
     */
    public static String encodeEnabledActions(Set<TextAction> actions) {
        JSONArray array = new JSONArray();
        for (TextAction action : actions) {
            array.put(action.name());
        }
        return array.toString();
    }

    /**
     * Encode enabled actions list to JSON string.
     */
    public static String encodeEnabledActions(List<TextAction> actions) {
        return encodeEnabledActions(new HashSet<>(actions));
    }

    /**
     * Get the configured prompt for an action, or default if not set.
     */
    public String getActionPrompt(TextAction action) {
        String key = "text_action_prompt_" + action.name();
        String customPrompt = prefs.getString(key, null);
        if (customPrompt != null && !customPrompt.isEmpty()) {
            return customPrompt;
        }
        return TextActionPrompts.getSystemMessage(action);
    }

    /**
     * Set a custom prompt for an action.
     */
    public void setActionPrompt(TextAction action, String prompt) {
        String key = "text_action_prompt_" + action.name();
        prefs.edit().putString(key, prompt).apply();
    }

    private static final String PREF_CUSTOM_TEXT_ACTIONS = "custom_text_actions";

    /**
     * Get list of custom actions.
     */
    public List<CustomTextAction> getCustomActions() {
        String json = prefs.getString(PREF_CUSTOM_TEXT_ACTIONS, "[]");
        List<CustomTextAction> actions = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                actions.add(new CustomTextAction(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("prompt"),
                        obj.getBoolean("enabled")));
            }
        } catch (JSONException e) {
            tn.eluea.kgpt.util.Logger.log(e);
        }
        return actions;
    }

    /**
     * Save list of custom actions.
     */
    public void saveCustomActions(List<CustomTextAction> actions) {
        JSONArray array = new JSONArray();
        try {
            for (CustomTextAction action : actions) {
                JSONObject obj = new JSONObject();
                obj.put("id", action.id);
                obj.put("name", action.name);
                obj.put("prompt", action.prompt);
                obj.put("enabled", action.enabled);
                array.put(obj);
            }
            prefs.edit().putString(PREF_CUSTOM_TEXT_ACTIONS, array.toString()).apply();
        } catch (JSONException e) {
            tn.eluea.kgpt.util.Logger.log(e);
        }
    }

    public void addCustomAction(String name, String prompt) {
        List<CustomTextAction> actions = getCustomActions();
        actions.add(new CustomTextAction(name, prompt, true));
        saveCustomActions(actions);
    }

    public void updateCustomAction(CustomTextAction updatedAction) {
        List<CustomTextAction> actions = getCustomActions();
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).id.equals(updatedAction.id)) {
                actions.set(i, updatedAction);
                break;
            }
        }
        saveCustomActions(actions);
    }

    public void deleteCustomAction(String id) {
        List<CustomTextAction> actions = getCustomActions();
        actions.removeIf(a -> a.id.equals(id));
        saveCustomActions(actions);
    }

    /**
     * Reset prompt to default for an action.
     */
    public void resetActionPrompt(TextAction action) {
        String key = "text_action_prompt_" + action.name();
        prefs.edit().remove(key).apply();
    }
}
