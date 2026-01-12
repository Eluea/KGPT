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

public class FormatParseResultFactory implements ParseResultFactory {
    private final ConversionMethod conversionMethod;

    public FormatParseResultFactory(ConversionMethod conversionMethod) {
        this.conversionMethod = conversionMethod;
    }

    @Override
    public ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd) {
        return new FormatParseResult(groups, indexStart, indexEnd, conversionMethod);
    }
}
