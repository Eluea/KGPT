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
import tn.eluea.kgpt.features.textactions.TextActionCommands;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResult;
import tn.eluea.kgpt.text.parse.result.InlineAskParseResultFactory;
import tn.eluea.kgpt.text.parse.result.InlineCommandParseResult;
import tn.eluea.kgpt.text.parse.result.InlineCommandParseResultFactory;
import tn.eluea.kgpt.text.parse.result.ParseResultFactory;
import tn.eluea.kgpt.text.parse.ParseDirective;
import tn.eluea.kgpt.text.parse.result.ParseResult;
import tn.eluea.kgpt.text.parse.result.AppTriggerParseResult;
import tn.eluea.kgpt.text.parse.result.TextActionParseResult;
import tn.eluea.kgpt.features.textactions.TextActionCommands;
import tn.eluea.kgpt.ui.UiInteractor;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTrigger;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerManager;
import tn.eluea.kgpt.instruction.command.GenerativeAICommand;

public class TextParser implements ConfigChangeListener {
    private final List<ParseDirective> directives = new ArrayList<>();
    private String currentTriggerSymbol = "$";
    private boolean aiTriggerEnabled = false;
    private boolean textActionsEnabled = false;
    private AppTriggerManager appTriggerManager = null;
    private java.util.Set<String> availableCommands = new java.util.HashSet<>();

    public TextParser() {
        UiInteractor.getInstance().registerConfigChangeListener(this);
        List<ParsePattern> parsePatterns = SPManager.getInstance().getParsePatterns();
        updatePatterns(parsePatterns);
        loadAvailableCommands();
    }

    private void loadAvailableCommands() {
        availableCommands.clear();
        List<GenerativeAICommand> commands = SPManager.getInstance().getGenerativeAICommands();
        for (GenerativeAICommand cmd : commands) {
            availableCommands.add(cmd.getCommandPrefix());
        }
    }

    public void setAppTriggerManager(AppTriggerManager manager) {
        this.appTriggerManager = manager;
    }

    /**
     * Set whether text actions are enabled.
     */
    public void setTextActionsEnabled(boolean enabled) {
        this.textActionsEnabled = enabled;
    }

    private void updatePatterns(List<ParsePattern> parsePatterns) {
        directives.clear();
        aiTriggerEnabled = false;

        for (ParsePattern parsePattern : parsePatterns) {
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
            android.util.Log.d("KGPT_AppTrigger",
                    "Found trigger: " + appTriggerResult.trigger + " -> " + appTriggerResult.packageName);
            return appTriggerResult;
        }

        // Check for text action commands (e.g., "text $rephrase")
        TextActionParseResult textActionResult = checkTextAction(textBeforeCursor);
        if (textActionResult != null) {
            return textActionResult;
        }

        // Only check inline ask if AI trigger is enabled
        if (aiTriggerEnabled) {
            // Check for inline commands first (any /command with preserved text)
            // These handle their own text preservation
            InlineCommandParseResult inlineCommandResult = InlineCommandParseResultFactory.parse(
                    textBeforeCursor, currentTriggerSymbol, availableCommands);
            if (inlineCommandResult != null) {
                return inlineCommandResult;
            }

            // Check for /ask usage as a shield for ANY directive
            // This fixes the issue where valid triggers (like @ for bold) would apply to
            // the entire text
            // because they matched the whole string pattern. /ask now properly delimits the
            // scope.
            String prefix = tn.eluea.kgpt.instruction.command.InlineAskCommand.getPrefix();
            String askPatternStr = "/" + java.util.regex.Pattern.quote(prefix) + "\\s+";
            java.util.regex.Pattern askPattern = java.util.regex.Pattern.compile(askPatternStr);
            java.util.regex.Matcher askMatcher = askPattern.matcher(textBeforeCursor);

            int lastAskIndex = -1;
            int lastContentStart = -1;

            // Find the *last* occurrence of /ask followed by whitespace
            while (askMatcher.find()) {
                lastAskIndex = askMatcher.start();
                lastContentStart = askMatcher.end();
            }

            if (lastAskIndex >= 0) {
                String scopedText = textBeforeCursor.substring(lastContentStart);

                // Check if this scoped text matches any directive
                for (ParseDirective directive : directives) {
                    // Pass 'lastAskIndex' as startOverride so the Result consumes the "/ask ..."
                    // part
                    // Pass 'lastContentStart' as offset for the scoped text
                    ParseResult result = directive.parseWithStartOverride(scopedText, lastContentStart, lastAskIndex);
                    if (result != null) {
                        return result;
                    }
                }
            }

            // Fallback to strict InlineAskParseResultFactory if generic shielding didn't
            // match anything
            // This handles cases specific to the Factory implementation if any
            InlineAskParseResult inlineAskResult = InlineAskParseResultFactory.parse(
                    textBeforeCursor, currentTriggerSymbol);
            if (inlineAskResult != null) {
                return inlineAskResult;
            }
        }

        for (ParseDirective directive : directives) {
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
        String lowerTrimmed = trimmedText.toLowerCase(java.util.Locale.ROOT);
        android.util.Log.d("KGPT_AppTrigger", "Checking text: '" + lowerTrimmed + "'");

        for (AppTrigger trigger : triggers) {
            if (!trigger.isEnabled()) {
                continue;
            }

            String triggerText = trigger.getTrigger().toLowerCase(java.util.Locale.ROOT);
            android.util.Log.d("KGPT_AppTrigger", "Comparing with trigger: '" + triggerText + "'");

            boolean match = false;
            if (lowerTrimmed.equals(triggerText)) {
                match = true;
            } else if (lowerTrimmed.endsWith(triggerText)) {
                // Check word boundary
                char charBefore = lowerTrimmed.charAt(lowerTrimmed.length() - triggerText.length() - 1);
                if (!Character.isLetterOrDigit(charBefore)) {
                    match = true;
                }
            }

            if (match) {
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
                        trigger.getAppName());
            }
        }

        android.util.Log.d("KGPT_AppTrigger", "No match found");
        return null;
    }

    /**
     * Check if the text ends with a text action command
     */
    private TextActionParseResult checkTextAction(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Parse the text for action commands
        TextActionCommands.ParseResult result = TextActionCommands.parse(text);
        if (result != null) {
            android.util.Log.d("KGPT_TextAction",
                    "Found action: " + result.action.name() + " for text: " + result.text);
            return new TextActionParseResult(
                    Collections.singletonList(result.text),
                    result.startIndex,
                    result.endIndex,
                    result.text,
                    result.action);
        }

        return null;
    }

    @Override
    public void onLanguageModelChange(LanguageModel model) {
    }

    @Override
    public void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value) {
    }

    @Override
    public void onCommandsChange(String commandsRaw) {
        loadAvailableCommands();
    }

    @Override
    public void onPatternsChange(String patternsRaw) {
        updatePatterns(ParsePattern.decode(patternsRaw));
    }

    @Override
    public void onOtherSettingsChange(Bundle otherSettings) {
    }
}
