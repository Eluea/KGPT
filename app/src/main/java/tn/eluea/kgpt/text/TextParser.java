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
package tn.eluea.kgpt.text;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.listener.ConfigChangeListener;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.text.parse.ParsePattern;
import tn.eluea.kgpt.text.parse.PatternType;
import tn.eluea.kgpt.text.parse.result.AppTriggerParseResult;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResult;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResultFactory;
import tn.eluea.kgpt.text.parse.result.ParseResultFactory;
import tn.eluea.kgpt.text.parse.ParseDirective;
import tn.eluea.kgpt.text.parse.result.ParseResult;
import tn.eluea.kgpt.ui.UiInteractor;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTrigger;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerManager;

public class TextParser implements ConfigChangeListener {
    private final List<ParseDirective> directives = new ArrayList<>();
    private String currentTriggerSymbol = "$";
    private boolean aiTriggerEnabled = false;
    private AppTriggerManager appTriggerManager = null;

    public TextParser() {
        UiInteractor.getInstance().registerConfigChangeListener(this);
        List<ParsePattern> parsePatterns = SPManager.getInstance().getParsePatterns();
        updatePatterns(parsePatterns);
    }

    public void setAppTriggerManager(AppTriggerManager manager) {
        this.appTriggerManager = manager;
    }

    private void updatePatterns(List<ParsePattern> parsePatterns) {
        directives.clear();
        aiTriggerEnabled = false;
        
        for (ParsePattern parsePattern: parsePatterns) {
            // Only add enabled patterns
            if (parsePattern.isEnabled()) {
                directives.add(new ParseDirective(parsePattern.getPattern(),
                        ParseResultFactory.of(parsePattern.getType())));
            }
            
            // Track AI trigger symbol and enabled state
            if (parsePattern.getType() == PatternType.CommandAI) {
                String symbol = PatternType.regexToSymbol(parsePattern.getPattern().pattern());
                if (symbol != null && !symbol.isEmpty()) {
                    currentTriggerSymbol = symbol;
                }
                aiTriggerEnabled = parsePattern.isEnabled();
            }
        }
    }

    public ParseResult parse(String text, int cursor) {
        // Bounds check to prevent StringIndexOutOfBoundsException
        if (text == null || text.isEmpty()) {
            return null;
        }
        cursor = Math.max(0, Math.min(cursor, text.length()));
        
        String textBeforeCursor = text.substring(0, cursor);
        
        // Check for app triggers first (if enabled)
        android.util.Log.d("KGPT_AppTrigger", "parse() called with text: '" + textBeforeCursor + "'");
        AppTriggerParseResult appTriggerResult = checkAppTrigger(textBeforeCursor);
        if (appTriggerResult != null) {
            android.util.Log.d("KGPT_AppTrigger", "Found trigger: " + appTriggerResult.trigger + " -> " + appTriggerResult.packageName);
            return appTriggerResult;
        }
        
        // Only check inline ask if AI trigger is enabled
        if (aiTriggerEnabled) {
            InlineAskParseResult inlineAskResult = InlineAskParseResultFactory.parse(
                textBeforeCursor, currentTriggerSymbol);
            if (inlineAskResult != null) {
                return inlineAskResult;
            }
        }

        for (ParseDirective directive: directives) {
            ParseResult parseResult = directive.parse(textBeforeCursor);
            if (parseResult != null) {
                return parseResult;
            }
        }

        return null;
    }

    /**
     * Check if the text ends with an app trigger
     */
    private AppTriggerParseResult checkAppTrigger(String text) {
        android.util.Log.d("KGPT_AppTrigger", "checkAppTrigger() - appTriggerManager: " + (appTriggerManager != null));
        
        if (appTriggerManager == null || !appTriggerManager.isFeatureEnabled()) {
            android.util.Log.d("KGPT_AppTrigger", "Feature disabled or manager null. Enabled: " + 
                    (appTriggerManager != null ? appTriggerManager.isFeatureEnabled() : "null"));
            return null;
        }
        
        List<AppTrigger> triggers = appTriggerManager.getAppTriggers();
        android.util.Log.d("KGPT_AppTrigger", "Loaded " + triggers.size() + " triggers");
        for (AppTrigger t : triggers) {
            android.util.Log.d("KGPT_AppTrigger", "  - Trigger: '" + t.getTrigger() + "' enabled: " + t.isEnabled());
        }
        
        if (triggers.isEmpty()) {
            return null;
        }
        
        // Don't process empty text
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return null;
        }
        
        // Check if the trimmed text ends with any trigger
        // This handles both "trigger" and "trigger " cases
        String lowerTrimmed = trimmedText.toLowerCase();
        android.util.Log.d("KGPT_AppTrigger", "Checking text: '" + lowerTrimmed + "'");
        
        for (AppTrigger trigger : triggers) {
            if (!trigger.isEnabled()) {
                continue;
            }
            
            String triggerText = trigger.getTrigger().toLowerCase();
            android.util.Log.d("KGPT_AppTrigger", "Comparing with trigger: '" + triggerText + "'");
            
            // Check if text ends with the trigger (with word boundary)
            if (lowerTrimmed.equals(triggerText) || 
                (lowerTrimmed.endsWith(triggerText) && 
                 (lowerTrimmed.length() == triggerText.length() || 
                  Character.isWhitespace(lowerTrimmed.charAt(lowerTrimmed.length() - triggerText.length() - 1))))) {
                
                android.util.Log.d("KGPT_AppTrigger", "MATCH FOUND! trigger: " + triggerText);
                
                // Find the actual position in original text
                int triggerStartInTrimmed = trimmedText.length() - trigger.getTrigger().length();
                
                // Find where trimmed text starts in original
                int trimStart = 0;
                while (trimStart < text.length() && Character.isWhitespace(text.charAt(trimStart))) {
                    trimStart++;
                }
                
                int wordStart = trimStart + triggerStartInTrimmed;
                
                // Return result that removes from word start to end of text
                return new AppTriggerParseResult(
                        java.util.Collections.singletonList(trigger.getTrigger()),
                        wordStart,
                        text.length(),
                        trigger.getTrigger(),
                        trigger.getPackageName(),
                        trigger.getActivityName(),
                        trigger.getAppName()
                );
            }
        }
        
        android.util.Log.d("KGPT_AppTrigger", "No match found");
        return null;
    }

    @Override
    public void onLanguageModelChange(LanguageModel model) {}

    @Override
    public void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value) {}

    @Override
    public void onCommandsChange(String commandsRaw) {}

    @Override
    public void onPatternsChange(String patternsRaw) {
        updatePatterns(ParsePattern.decode(patternsRaw));
    }

    @Override
    public void onOtherSettingsChange(Bundle otherSettings) {}
}
