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
package tn.eluea.kgpt.features.textactions;

import tn.eluea.kgpt.features.textactions.domain.TextAction;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles text action commands that can be triggered by typing specific
 * patterns.
 * 
 * Usage examples:
 * - "hello world $rephrase" â†’ Rephrases "hello world"
 * - "ظ…ط±ط­ط¨ط§ $fix" â†’ Fixes errors in "ظ…ط±ط­ط¨ط§"
 * - "some text $improve" â†’ Improves "some text"
 * - "long text $short" â†’ Shortens the text
 * - "brief $expand" â†’ Expands the text
 * - "casual text $formal" â†’ Makes it formal
 * - "formal text $casual" â†’ Makes it casual
 * - "hello $tr" â†’ Translates "hello"
 */
public class TextActionCommands {

    // Map of command triggers to actions
    private static final Map<String, TextAction> COMMAND_MAP = new HashMap<>();

    static {
        // Rephrase commands
        COMMAND_MAP.put("rephrase", TextAction.REPHRASE);
        COMMAND_MAP.put("rewrite", TextAction.REPHRASE);
        COMMAND_MAP.put("rw", TextAction.REPHRASE);

        // Fix errors commands
        COMMAND_MAP.put("fix", TextAction.FIX_ERRORS);
        COMMAND_MAP.put("correct", TextAction.FIX_ERRORS);
        COMMAND_MAP.put("grammar", TextAction.FIX_ERRORS);

        // Improve commands
        COMMAND_MAP.put("improve", TextAction.IMPROVE);
        COMMAND_MAP.put("better", TextAction.IMPROVE);
        COMMAND_MAP.put("enhance", TextAction.IMPROVE);

        // Expand commands
        COMMAND_MAP.put("expand", TextAction.EXPAND);
        COMMAND_MAP.put("longer", TextAction.EXPAND);
        COMMAND_MAP.put("more", TextAction.EXPAND);

        // Shorten commands
        COMMAND_MAP.put("short", TextAction.SHORTEN);
        COMMAND_MAP.put("shorten", TextAction.SHORTEN);
        COMMAND_MAP.put("brief", TextAction.SHORTEN);
        COMMAND_MAP.put("summarize", TextAction.SHORTEN);

        // Formal commands
        COMMAND_MAP.put("formal", TextAction.FORMAL);
        COMMAND_MAP.put("professional", TextAction.FORMAL);
        COMMAND_MAP.put("pro", TextAction.FORMAL);

        // Casual commands
        COMMAND_MAP.put("casual", TextAction.CASUAL);
        COMMAND_MAP.put("friendly", TextAction.CASUAL);
        COMMAND_MAP.put("chill", TextAction.CASUAL);

        // Translate commands
        COMMAND_MAP.put("tr", TextAction.TRANSLATE);
        COMMAND_MAP.put("translate", TextAction.TRANSLATE);
        COMMAND_MAP.put("trans", TextAction.TRANSLATE);
    }

    // Pattern to match: text $command
    // The $ is followed by the command name
    private static final Pattern ACTION_PATTERN = Pattern.compile("(.+)\\s*\\$([a-zA-Z]+)\\s*$");

    /**
     * Result of parsing a text action command.
     */
    public static class ParseResult {
        public final String text;
        public final TextAction action;
        public final int startIndex;
        public final int endIndex;

        public ParseResult(String text, TextAction action, int startIndex, int endIndex) {
            this.text = text;
            this.action = action;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    /**
     * Try to parse a text action command from the input.
     * 
     * @param input The full text input
     * @return ParseResult if a valid command was found, null otherwise
     */
    public static ParseResult parse(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        Matcher matcher = ACTION_PATTERN.matcher(input);
        if (matcher.find()) {
            String text = matcher.group(1).trim();
            String command = matcher.group(2).toLowerCase();

            TextAction action = COMMAND_MAP.get(command);
            if (action != null && !text.isEmpty()) {
                // Find where the command starts (including the $)
                int commandStart = input.lastIndexOf("$");
                return new ParseResult(text, action, commandStart, input.length());
            }
        }

        return null;
    }

    /**
     * Get all available command triggers for a specific action.
     */
    public static String[] getTriggersForAction(TextAction action) {
        return COMMAND_MAP.entrySet().stream()
                .filter(e -> e.getValue() == action)
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }

    /**
     * Get a formatted help string showing all available commands.
     */
    public static String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Text Action Commands:\n\n");

        for (TextAction action : TextAction.values()) {
            sb.append("â€¢ ").append(action.labelEn).append(": ");
            String[] triggers = getTriggersForAction(action);
            for (int i = 0; i < triggers.length; i++) {
                sb.append("$").append(triggers[i]);
                if (i < triggers.length - 1)
                    sb.append(", ");
            }
            sb.append("\n");
        }

        sb.append("\nExample: \"hello world $rephrase\"");
        return sb.toString();
    }
}
