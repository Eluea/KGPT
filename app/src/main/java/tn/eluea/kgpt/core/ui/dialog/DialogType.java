/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.core.ui.dialog;

public enum DialogType {
    ChoseModel("Choose & Configure Model", true),
    ConfigureModel("Configure Model", false),
    WebSearch("Web Search", false),
    EditCommandsList("Commands List", true),
    EditCommand("Edit Command", false),
    EditPatternList("Patterns List", true),
    EditPattern("Edit Pattern", false),
    Settings("Settings", false),
    OtherSettings("Other Settings", true),
    AddCustomProvider("Add Custom Provider", false),
    ;

    public final String title;
    public final boolean inSettings;

    DialogType(String title, boolean inSettings) {
        this.title = title;
        this.inSettings = inSettings;
    }
}
