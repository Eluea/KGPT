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
    REPHRASE("Rephrase", "Rephrase", R.drawable.ic_message_text_filled, "#4CAF50"),
    FIX_ERRORS("Fix Errors", "Fix Errors", R.drawable.ic_shield_tick_filled, "#2196F3"),
    IMPROVE("Improve", "Improve", R.drawable.ic_lamp_charge_filled, "#9C27B0"),
    EXPAND("Expand", "Expand", R.drawable.ic_arrow_circle_right_filled, "#FF9800"),
    SHORTEN("Shorten", "Shorten", R.drawable.ic_close_circle_filled, "#F44336"),
    FORMAL("Formal", "Formal", R.drawable.ic_document_text_filled, "#607D8B"),
    CASUAL("Casual", "Casual", R.drawable.ic_palette_filled, "#E91E63"),
    TRANSLATE("Translate", "Translate", R.drawable.ic_global_search_filled, "#00BCD4");

    public final String labelEn;
    public final String labelAr;
    public final int iconRes;
    public final String color;

    TextAction(String labelEn, String labelAr, int iconRes, String color) {
        this.labelEn = labelEn;
        this.labelAr = labelAr;
        this.iconRes = iconRes;
        this.color = color;
    }

    public String getLabel(boolean isArabic) {
        return labelEn;
    }
}
