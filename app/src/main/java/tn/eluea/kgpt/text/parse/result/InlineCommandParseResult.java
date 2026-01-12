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

/**
 * Parse result for inline commands (any /command).
 * This allows applying any command to a specific part of text while preserving the rest.
 * 
 * Example: "Important text. /translate كيف حالك$"
 * - preservedText = "Important text. "
 * - command = "translate"
 * - prompt = "كيف حالك"
 * - Only the "/translate كيف حالك$" part is deleted
 */
public class InlineCommandParseResult extends ParseResult {
    public final String command;
    public final String prompt;
    public final String preservedText;
    public final int commandStart;

    public InlineCommandParseResult(List<String> groups, int indexStart, int indexEnd,
                                    String command, String prompt, String preservedText, int commandStart) {
        super(groups, commandStart, indexEnd); // indexStart is where /command begins
        this.command = command;
        this.prompt = prompt;
        this.preservedText = preservedText;
        this.commandStart = commandStart;
    }
}
