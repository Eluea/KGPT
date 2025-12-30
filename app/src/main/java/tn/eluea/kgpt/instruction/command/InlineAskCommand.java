/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.instruction.command;

/**
 * Special built-in command that allows asking AI while preserving text before the command.
 * 
 * Usage: "Some text I want to keep. /ask What is the weather?$"
 * Result: Only "What is the weather?" is sent to AI, "Some text I want to keep." remains
 * 
 * This command cannot be deleted by the user, but the prefix can be changed.
 */
public class InlineAskCommand extends GenerativeAICommand {
    
    public static final String DEFAULT_PREFIX = "ask";
    public static final String DISPLAY_NAME = "Inline Ask";
    public static final String DESCRIPTION = "Ask AI without replacing previous text";
    
    private static String currentPrefix = DEFAULT_PREFIX;
    
    private static final InlineAskCommand INSTANCE = new InlineAskCommand();
    
    public static InlineAskCommand getInstance() {
        return INSTANCE;
    }
    
    private InlineAskCommand() {
        // Private constructor for singleton
    }

    @Override
    public String getCommandPrefix() {
        return currentPrefix;
    }

    @Override
    public String getTweakMessage() {
        return ""; // No system message modification
    }
    
    /**
     * Update the command prefix
     */
    public static void setPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            currentPrefix = prefix;
        }
    }
    
    /**
     * Get the current prefix
     */
    public static String getPrefix() {
        return currentPrefix;
    }
    
    /**
     * Reset to default prefix
     */
    public static void resetPrefix() {
        currentPrefix = DEFAULT_PREFIX;
    }
    
    /**
     * Check if this is the built-in ask command
     */
    public static boolean isInlineAskCommand(String prefix) {
        return currentPrefix.equalsIgnoreCase(prefix) || DEFAULT_PREFIX.equalsIgnoreCase(prefix);
    }
    
    /**
     * Check if a command is the built-in ask command
     */
    public static boolean isInlineAskCommand(GenerativeAICommand command) {
        return command instanceof InlineAskCommand;
    }
}
