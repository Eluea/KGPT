/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.settings;

import tn.eluea.kgpt.R;

public enum OtherSettingsType {
        EnableLogs(R.string.setting_enable_logging, R.string.setting_desc_enable_logging,
                        Nature.Boolean, false),
        EnableExternalInternet(R.string.setting_external_internet,
                        R.string.setting_desc_external_internet,
                        Nature.Boolean, true),
        SearchEngine(R.string.setting_search_engine, R.string.setting_desc_search_engine,
                        Nature.String, "duckduckgo"),
        MaterialYouEnabled(R.string.setting_material_you, R.string.setting_desc_material_you,
                        Nature.Boolean, false),
        MaterialYouUseWallpaper(R.string.setting_wallpaper_colors, R.string.setting_desc_wallpaper_colors,
                        Nature.Boolean, true),
        MaterialYouSeedColor(R.string.setting_seed_color, R.string.setting_desc_seed_color,
                        Nature.Integer, 0xFF2196F3),
        MaterialYouSingleTone(R.string.setting_single_tone, R.string.setting_desc_single_tone,
                        Nature.Boolean, false),

        // Update Settings
        UpdateCheckEnabled(R.string.setting_check_updates, R.string.setting_desc_check_updates,
                        Nature.Boolean, true),
        UpdateCheckInterval(R.string.setting_update_interval, R.string.setting_desc_update_interval,
                        Nature.Integer, 24),
        UpdateDownloadPath(R.string.setting_download_path, R.string.setting_desc_download_path,
                        Nature.String, "");

        public final int titleResId;
        public final int descriptionResId;
        public final Nature nature;
        public final Object defaultValue;

        OtherSettingsType(int titleResId, int descriptionResId, Nature nature, Object defaultValue) {
                this.titleResId = titleResId;
                this.descriptionResId = descriptionResId;
                this.nature = nature;
                this.defaultValue = defaultValue;
        }

        public enum Nature {
                Boolean,
                String,
                Integer
        }
}
