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

public class WebSearchParseResult extends ParseResult {
    public final String query;

    protected WebSearchParseResult(List<String> groups, int indexStart, int indexEnd) {
        super(groups, indexStart, indexEnd);
        this.query = groups.get(1);
    }
}
