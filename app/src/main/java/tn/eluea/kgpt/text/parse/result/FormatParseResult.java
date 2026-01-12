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
package tn.eluea.kgpt.text.parse.result;

import java.util.List;

import tn.eluea.kgpt.text.transform.format.ConversionMethod;

public class FormatParseResult extends ParseResult {
    public final String target;

    public final ConversionMethod conversionMethod;

    protected FormatParseResult(List<String> groups, int indexStart, int indexEnd, ConversionMethod conversionMethod) {
        super(groups, indexStart, indexEnd);

        this.target = groups.get(1);
        this.conversionMethod = conversionMethod;
    }
}
