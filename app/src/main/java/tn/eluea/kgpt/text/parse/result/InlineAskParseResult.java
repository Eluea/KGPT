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
 * Parse result for inline ask command (/ask).
 * This command allows asking AI while preserving text before the command.
 * 
 * Example: "Important text. /ask What is the weather?$"
 * - preservedText = "Important text. "
 * - prompt = "What is the weather?"
 * - Only the "/ask What is the weather?$" part is deleted
 */
public class InlineAskParseResult extends ParseResult {
    public final String prompt;
    public final String preservedText;
    public final int askCommandStart;

    protected InlineAskParseResult(List<String> groups, int indexStart, int indexEnd, 
                                   String preservedText, int askCommandStart) {
        super(groups, askCommandStart, indexEnd); // indexStart is where /ask begins
        
        // Group 1 = the prompt after /ask
        this.prompt = groups.size() >= 2 && groups.get(1) != null ? groups.get(1).trim() : "";
        this.preservedText = preservedText;
        this.askCommandStart = askCommandStart;
    }
}
