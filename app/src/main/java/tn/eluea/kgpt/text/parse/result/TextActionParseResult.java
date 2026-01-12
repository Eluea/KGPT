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
package tn.eluea.kgpt.text.parse.result;

import java.util.List;

import tn.eluea.kgpt.features.textactions.domain.TextAction;

/**
 * Parse result for text action commands like "$rephrase", "$fix", etc.
 */
public class TextActionParseResult extends ParseResult {

    public final String text;
    public final TextAction action;

    public TextActionParseResult(List<String> groups, int indexStart, int indexEnd,
            String text, TextAction action) {
        super(groups, indexStart, indexEnd);
        this.text = text;
        this.action = action;
    }
}
