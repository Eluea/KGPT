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

        String escapedSymbol = Pattern.quote(triggerSymbol);

        // Pattern: (preserved text) /command (prompt)$
        // We need to find /command where command is in availableCommands
        Pattern pattern = Pattern.compile("(.*)\\s*/([a-zA-Z0-9_]+)\\s+(.+)" + escapedSymbol + "$");

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String preservedText = matcher.group(1);
            String command = matcher.group(2);
            String prompt = matcher.group(3);

            // Skip if this is the InlineAskCommand (handled separately)
            if (InlineAskCommand.isInlineAskCommand(command)) {
                return null;
            }

            // Check if command exists in available commands (case-insensitive)
            boolean commandExists = false;
            String matchedCommand = null;
            for (String cmd : availableCommands) {
                if (cmd.equalsIgnoreCase(command)) {
                    commandExists = true;
                    matchedCommand = cmd;
                    break;
                }
            }

            if (!commandExists) {
                return null;
            }

            // Find where /command starts
            int commandStart = text.lastIndexOf("/" + command);
            if (commandStart < 0) {
                // Try case-insensitive search
                String lowerText = text.toLowerCase();
                commandStart = lowerText.lastIndexOf("/" + command.toLowerCase());
            }

            if (commandStart < 0) {
                return null;
            }

            // Create groups list for compatibility
            List<String> groups = List.of(
                    matcher.group(0), // Full match
                    command, // The command
                    prompt // The prompt after /command
            );

            return new InlineCommandParseResult(
                    groups,
                    0, // Original indexStart (not used)
                    text.length(), // indexEnd
                    matchedCommand, // Use the matched command (preserves original case)
                    prompt.trim(),
                    preservedText != null ? preservedText : "",
                    commandStart // Where /command starts
            );
        }

        return null;
    }
}
