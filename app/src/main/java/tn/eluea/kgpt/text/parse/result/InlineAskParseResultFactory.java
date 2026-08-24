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
package tn.eluea.kgpt.text.parse.result;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.eluea.kgpt.instruction.command.InlineAskCommand;

/**
 * Factory for creating InlineAskParseResult.
 * Handles the special /ask command that preserves text before it.
 */
public class InlineAskParseResultFactory implements ParseResultFactory {

    // Single-entry compiled-pattern cache (runs on every keystroke; inputs
    // change only when the user renames the ask prefix or the trigger symbol).
    private static final Object CACHE_LOCK = new Object();
    private static String cachedKey = null;
    private static Pattern cachedPattern = null;

    private static Pattern getPattern(String commandPrefix, String triggerSymbol) {
        String key = commandPrefix + "|" + triggerSymbol;
        synchronized (CACHE_LOCK) {
            if (cachedPattern != null && key.equals(cachedKey)) {
                return cachedPattern;
            }
        }
        String escapedSymbol = Pattern.quote(triggerSymbol);
        Pattern pattern = Pattern
                .compile("(.*)\\s*/" + Pattern.quote(commandPrefix) + "\\s+(.+)" + escapedSymbol + "$");
        synchronized (CACHE_LOCK) {
            cachedKey = key;
            cachedPattern = pattern;
        }
        return pattern;
    }

    public InlineAskParseResultFactory() {
    }

    @Override
    public ParseResult getParseResult(List<String> groups, int indexStart, int indexEnd) {
        // This factory needs special handling - it's called from ParseDirective
        // but we need to re-parse to find the /ask position
        return null; // Not used directly
    }

    /**
     * Parse text for inline ask command
     * 
     * @param text          The full text to parse
     * @param triggerSymbol The trigger symbol (default $)
     * @return InlineAskParseResult if matched, null otherwise
     */
    public static InlineAskParseResult parse(String text, String triggerSymbol) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Get current prefix (may have been customized by user)
        String commandPrefix = InlineAskCommand.getPrefix();

        Matcher matcher = getPattern(commandPrefix, triggerSymbol).matcher(text);
        if (matcher.find()) {
            String preservedText = matcher.group(1);
            String prompt = matcher.group(2);

            // Find where /command starts
            int askStart = text.lastIndexOf("/" + commandPrefix);
            if (askStart < 0) {
                return null;
            }

            // Create groups list for compatibility
            List<String> groups = List.of(
                    matcher.group(0), // Full match
                    prompt // The prompt after /ask
            );

            return new InlineAskParseResult(
                    groups,
                    0, // Original indexStart (not used)
                    text.length(), // indexEnd
                    preservedText != null ? preservedText : "",
                    askStart // Where /ask command starts
            );
        }

        return null;
    }
}
