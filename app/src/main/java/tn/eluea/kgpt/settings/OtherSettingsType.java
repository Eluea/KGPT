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
                        Nature.String, "duckduckgo"),
        MaterialYouEnabled("Enable Material You", "Use dynamic colors based on wallpaper or custom color.",
                        Nature.Boolean, false),
        MaterialYouUseWallpaper("Use Wallpaper Colors", "Extract colors from device wallpaper (Android 12+).",
                        Nature.Boolean, true),
        MaterialYouSeedColor("Seed Color", "Custom color to generate theme.",
                        Nature.Integer, 0xFF2196F3), // Default Blue
        MaterialYouSingleTone("Use Single Tone", "Use a single color tone for the theme.",
                        Nature.Boolean, false),

        // Update Settings
        UpdateCheckEnabled("Check for Updates", "Automatically check for new versions.",
                        Nature.Boolean, true),
        UpdateCheckInterval("Update Check Interval", "How often to check for updates (in hours).",
                        Nature.Integer, 24), // Default: every 24 hours
        UpdateDownloadPath("Download Path", "Custom path for downloading updates.",
                        Nature.String, ""); // Empty means default Downloads folder

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
                String,
                Integer
        }
}
