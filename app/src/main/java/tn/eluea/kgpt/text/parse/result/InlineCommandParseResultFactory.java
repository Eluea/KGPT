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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tn.eluea.kgpt.instruction.command.InlineAskCommand;

/**
 * Factory for creating InlineCommandParseResult.
 * Handles any /command that preserves text before it.
 * 
 * Usage: "Some text /command prompt$"
 * Result: Only "prompt" is processed with the command, "Some text " remains
 */
public class InlineCommandParseResultFactory {

    // Single-entry compiled-pattern cache. parse() runs on every keystroke on
    // the keyboard's main thread; inputs (symbol + command set) change only on
    // config changes, so memoizing by that key removes per-keystroke
    // Pattern.compile cost entirely.
    private static final Object CACHE_LOCK = new Object();
    private static String cachedKey = null;
    private static Pattern cachedPattern = null;

    private static Pattern getPattern(String triggerSymbol, Set<String> availableCommands) {
        String key = triggerSymbol + "|" + String.join(",", availableCommands);
        synchronized (CACHE_LOCK) {
            if (cachedPattern != null && key.equals(cachedKey)) {
                return cachedPattern;
            }
        }

        String escapedSymbol = Pattern.quote(triggerSymbol);

        // Sort commands by length (descending) to match longest triggers first
        // e.g. match "fixer" before "fix"
        List<String> sortedCommands = new java.util.ArrayList<>(availableCommands);
        sortedCommands.sort((s1, s2) -> s2.length() - s1.length());

        StringBuilder cmdPattern = new StringBuilder();
        for (String cmd : sortedCommands) {
            if (cmdPattern.length() > 0)
                cmdPattern.append("|");
            cmdPattern.append(Pattern.quote(cmd));
        }

        String separatorPattern = "(?:\\s+/|\\s+|(?<=^)/|(?<=^))";
        String regex = "(?si)(.*)" + separatorPattern + "(" + cmdPattern.toString() + ")\\s+(.+)" + escapedSymbol + "$";

        Pattern pattern = Pattern.compile(regex);
        synchronized (CACHE_LOCK) {
            cachedKey = key;
            cachedPattern = pattern;
        }
        return pattern;
    }

    /**
     * Parse text for inline command
     *
     * @param text              The full text to parse
     * @param triggerSymbol     The trigger symbol (default $)
     * @param availableCommands Set of available command prefixes
     * @return InlineCommandParseResult if matched, null otherwise
     */
    public static InlineCommandParseResult parse(String text, String triggerSymbol, Set<String> availableCommands) {
        if (text == null || text.isEmpty() || availableCommands == null || availableCommands.isEmpty()) {
            return null;
        }

        Matcher matcher = getPattern(triggerSymbol, availableCommands).matcher(text);

        if (matcher.find()) {
            String preservedText = matcher.group(1);
            String command = matcher.group(2);
            String prompt = matcher.group(3);

            // Skip if this is the InlineAskCommand (handled separately)
            if (InlineAskCommand.isInlineAskCommand(command)) {
                return null;
            }

            // Identify the exact matched command string
            String matchedCommand = command;
            for (String cmd : availableCommands) {
                if (cmd.equalsIgnoreCase(command)) {
                    matchedCommand = cmd;
                    break;
                }
            }

            // Calculate start index.
            // We can check text between group 1 end and group 2 start to find if slash was
            // used.
            int g1End = matcher.end(1);
            int g2Start = matcher.start(2);
            String separator = text.substring(g1End, g2Start);

            int commandStartPos = g2Start;
            if (separator.contains("/")) {
                commandStartPos = text.lastIndexOf("/", g2Start);
                if (commandStartPos < g1End)
                    commandStartPos = g2Start;
            }

            // Return result
            return new InlineCommandParseResult(
                    List.of(matcher.group(0), command, prompt),
                    0,
                    text.length(),
                    matchedCommand,
                    prompt.trim(),
                    preservedText != null ? preservedText : "",
                    commandStartPos);
        }

        return null;
    }
}
