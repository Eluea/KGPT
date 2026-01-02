/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * This file is part of KGPT.
 * Based on original code from KeyboardGPT by Mino260806.
 * Original: https://github.com/Mino260806/KeyboardGPT
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.settings;

public enum OtherSettingsType {
    EnableLogs("Enable logging", "Disable for performance. You won't be able to report errors.",
            Nature.Boolean, true),
    EnableExternalInternet("Use external internet service",
            "Recommended to keep on unless chat completion is not working.",
            Nature.Boolean, true),
    SearchEngine("Search Engine", "Default search engine for web searches.",
            Nature.String, "duckduckgo");

    public final String title;
    public final String description;
    public final Nature nature;
    public final Object defaultValue;

    OtherSettingsType(String title, String description, Nature nature, Object defaultValue) {
        this.title = title;
        this.description = description;
        this.nature = nature;
        this.defaultValue = defaultValue;
    }

    public enum Nature {
        Boolean,
        String
    }
}
