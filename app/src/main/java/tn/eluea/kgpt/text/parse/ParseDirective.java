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
package tn.eluea.kgpt.text.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.eluea.kgpt.text.parse.result.ParseResult;
import tn.eluea.kgpt.text.parse.result.ParseResultFactory;

public class ParseDirective {
    private final Pattern pattern;

    private final ParseResultFactory factory;

    public ParseDirective(Pattern pattern, ParseResultFactory factory) {
        this.pattern = pattern;
        this.factory = factory;
    }

    public ParseResult parse(String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            List<String> groups = new ArrayList<>();
            for (int i = 0; i < matcher.groupCount() + 1; i++) {
                groups.add(matcher.group(i));
            }
            return factory.getParseResult(groups, matcher.start(), matcher.end());
        }
        return null;
    }

    /**
     * Parse with a forced start index for the result.
     * Used when the trigger applies to a substring, but the command (e.g. /ask)
     * preceeding it should also be consumed/replaced.
     *
     * @param text          The text to match against (usually a suffix)
     * @param offset        The offset of 'text' within the original full buffer
     * @param startOverride The absolute index to use as the start of the
     *                      ParseResult
     */
    public ParseResult parseWithStartOverride(String text, int offset, int startOverride) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            List<String> groups = new ArrayList<>();
            for (int i = 0; i < matcher.groupCount() + 1; i++) {
                groups.add(matcher.group(i));
            }
            // Use startOverride for start, and calculated absolute end
            return factory.getParseResult(groups, startOverride, matcher.end() + offset);
        }
        return null;
    }
}
