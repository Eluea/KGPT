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
package tn.eluea.kgpt.textactions;

/**
 * Provides system prompts for each text action.
 */
public class TextActionPrompts {

    /**
     * Get the system message for a specific action.
     */
    public static String getSystemMessage(TextAction action) {
        return getSystemMessage(action, null);
    }

    /**
     * Get the system message for a specific action, with optional target info.
     */
    public static String getSystemMessage(TextAction action, String targetInfo) {
        switch (action) {
            case REPHRASE:
                return "You are a text rephrasing assistant. Rephrase the given text while keeping the exact same meaning. " +
                       "Maintain the same language as the input. Only output the rephrased text, nothing else.";
            
            case FIX_ERRORS:
                return "You are a grammar and spelling correction assistant. Fix all grammar, spelling, and punctuation errors in the given text. " +
                       "Maintain the same language and meaning. Only output the corrected text, nothing else.";
            
            case IMPROVE:
                return "You are a writing improvement assistant. Improve the style, clarity, and flow of the given text while keeping the same meaning. " +
                       "Maintain the same language. Only output the improved text, nothing else.";
            
            case EXPAND:
                return "You are a text expansion assistant. Expand the given text by adding more details, examples, or explanations while keeping the core meaning. " +
                       "Maintain the same language. Only output the expanded text, nothing else.";
            
            case SHORTEN:
                return "You are a text summarization assistant. Shorten the given text while keeping the essential meaning and key points. " +
                       "Maintain the same language. Only output the shortened text, nothing else.";
            
            case FORMAL:
                return "You are a tone adjustment assistant. Convert the given text to a formal, professional tone. " +
                       "Maintain the same language and meaning. Only output the formal version, nothing else.";
            
            case CASUAL:
                return "You are a tone adjustment assistant. Convert the given text to a casual, friendly tone. " +
                       "Maintain the same language and meaning. Only output the casual version, nothing else.";
            
            case TRANSLATE:
                if (targetInfo != null && !targetInfo.isEmpty()) {
                    return "You are a translation assistant. Translate the given text to " + targetInfo + ". " +
                           "Only output the translated text, nothing else.";
                }
                return "You are a translation assistant. Detect the language of the input text and translate it to the opposite language " +
                       "(if Arabic, translate to English; if English, translate to Arabic; for other languages, translate to English). " +
                       "Only output the translated text, nothing else.";
            
            default:
                return "Process the following text:";
        }
    }

    /**
     * Build the full prompt for the AI.
     */
    public static String buildPrompt(TextAction action, String selectedText) {
        return selectedText;
    }
}
