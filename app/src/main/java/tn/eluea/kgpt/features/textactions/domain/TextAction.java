/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
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
package tn.eluea.kgpt.features.textactions.domain;

import tn.eluea.kgpt.R;

/**
 * Enum representing available AI text actions that can be performed on selected
 * text.
 */
public enum TextAction {
    REPHRASE(R.string.action_rephrase, R.drawable.ic_message_text_filled, "#4CAF50"),
    FIX_ERRORS(R.string.action_fix_errors, R.drawable.ic_shield_tick_filled, "#2196F3"),
    IMPROVE(R.string.action_improve, R.drawable.ic_lamp_charge_filled, "#9C27B0"),
    EXPAND(R.string.action_expand, R.drawable.ic_arrow_circle_right_filled, "#FF9800"),
    SHORTEN(R.string.action_shorten, R.drawable.ic_close_circle_filled, "#F44336"),
    FORMAL(R.string.action_formal, R.drawable.ic_document_text_filled, "#607D8B"),
    CASUAL(R.string.action_casual, R.drawable.ic_palette_filled, "#E91E63"),
    TRANSLATE(R.string.action_translate, R.drawable.ic_global_search_filled, "#00BCD4"),
    DOWNLOAD(R.string.action_download_media, R.drawable.ic_download_filled, "#FF5722");

    public final int labelRes;
    public final int iconRes;
    public final String color;

    TextAction(int labelRes, int iconRes, String color) {
        this.labelRes = labelRes;
        this.iconRes = iconRes;
        this.color = color;
    }

    public String getLabel(android.content.Context context) {
        if (context == null)
            return "Unknown";
        return context.getString(labelRes);
    }
}
