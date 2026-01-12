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

import tn.eluea.kgpt.text.parse.PatternType;
import tn.eluea.kgpt.text.transform.format.ConversionMethod;

public interface ParseResultFactory {
    static ParseResultFactory of(PatternType type) {
        switch (type) {
            case CommandAI:
                return new AIParseResultFactory();
            case CommandCustom:
                return new CommandParseResultFactory();
            case WebSearch:
                return new WebSearchParseResultFactory();
            case FormatBold:
                return new FormatParseResultFactory(ConversionMethod.BOLD);
            case FormatItalic:
                return new FormatParseResultFactory(ConversionMethod.ITALIC);
            case FormatCrossout:
                return new FormatParseResultFactory(ConversionMethod.CROSSOUT);
            case FormatUnderline:
                return new FormatParseResultFactory(ConversionMethod.UNDERLINE);
            case Settings:
            default:
                return new SettingsParseResultFactory();
        }
    }

    ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd);
}
