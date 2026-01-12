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
package tn.eluea.kgpt.backup;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the available backup/restore options.
 */
public class BackupOptions {

    public enum Option {
        COMMANDS("commands", "AI Commands"),
        PATTERNS("patterns", "Trigger Patterns"),
        LANGUAGE_MODEL("language_model", "AI Model Settings"),
        APPEARANCE("appearance", "Appearance"),
        BLUR_SETTINGS("blur_settings", "Blur Settings"),
        GENERAL_SETTINGS("general_settings", "General Settings"),
        APP_TRIGGERS("app_triggers", "App Triggers (LAB)"),
        TEXT_ACTIONS("text_actions", "Text Actions (LAB)"),
        SENSITIVE_DATA("sensitive_data", "API Keys & Credentials");

        public final String key;
        public final String displayName;

        Option(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }
    }

    private final Set<Option> selectedOptions;

    public BackupOptions() {
        // Default: all options selected EXCEPT sensitive data
        this.selectedOptions = EnumSet.allOf(Option.class);
        this.selectedOptions.remove(Option.SENSITIVE_DATA);
    }

    public BackupOptions(Set<Option> options) {
        this.selectedOptions = EnumSet.copyOf(options);
    }

    public boolean isSelected(Option option) {
        return selectedOptions.contains(option);
    }

    public void setSelected(Option option, boolean selected) {
        if (selected) {
            selectedOptions.add(option);
        } else {
            selectedOptions.remove(option);
        }
    }

    public void selectAll() {
        selectedOptions.addAll(EnumSet.allOf(Option.class));
    }

    public void deselectAll() {
        selectedOptions.clear();
    }

    public boolean isAllSelected() {
        return selectedOptions.size() == Option.values().length;
    }

    public boolean hasAnySelected() {
        return !selectedOptions.isEmpty();
    }

    public Set<Option> getSelectedOptions() {
        return EnumSet.copyOf(selectedOptions);
    }

    public int getSelectedCount() {
        return selectedOptions.size();
    }

    /**
     * Create options from available keys in backup file
     */
    public static BackupOptions fromAvailableKeys(Set<String> availableKeys) {
        Set<Option> available = EnumSet.noneOf(Option.class);
        for (Option option : Option.values()) {
            if (availableKeys.contains(option.key)) {
                available.add(option);
            }
        }
        return new BackupOptions(available);
    }
}
