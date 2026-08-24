/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
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
        availableCommands.addAll(tn.eluea.kgpt.instruction.command.CommandManager.getStaticCommandPrefixes());
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

    // Cached ask-shield pattern: compiled once per prefix instead of on every
    // keystroke (this parse path runs on the keyboard's main thread).
    private static volatile String cachedAskPrefix = null;
    private static volatile java.util.regex.Pattern cachedAskPattern = null;

    private static java.util.regex.Pattern getAskPattern(String prefix) {
        java.util.regex.Pattern p = cachedAskPattern;
        if (p != null && prefix.equals(cachedAskPrefix)) {
            return p;
        }
        String regex = "(?:/|\\s+|^)" + java.util.regex.Pattern.quote(prefix) + "\\s+";
        p = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        cachedAskPrefix = prefix;
        cachedAskPattern = p;
        return p;
    }

    private void updatePatterns(List<ParsePattern> parsePatterns) {
        directives.clear();
        aiTriggerEnabled = false;
        android.util.Log.d("KGPT_ParseDiag", "updatePatterns: in=" + parsePatterns.size());

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

    /**
     * Re-read patterns from config and rebuild directives.
     * Used by the periodic config refresh so hooked processes recover from
     * stale initial reads without requiring a manual edit+save.
     */
    public void reloadPatterns() {
        try {
            updatePatterns(SPManager.getInstance().getParsePatterns());
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.log(e);
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
        AppTriggerParseResult appTriggerResult = checkAppTrigger(textBeforeCursor);
        if (appTriggerResult != null) {
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
            // Regex: Whitespace OR start of line, optional slash, then 'ask', then
            // whitespace
            // This is safer than previous slash-enforced logic
            // Compiled once per prefix — this runs on every keystroke.
            java.util.regex.Matcher askMatcher = getAskPattern(prefix).matcher(textBeforeCursor);

            int lastAskIndex = -1;
            int lastContentStart = -1;

            // Find the *last* occurrence of /ask followed by whitespace
            while (askMatcher.find()) {
                lastAskIndex = askMatcher.start();
                lastContentStart = askMatcher.end();
            }

            if (lastAskIndex >= 0) {
                // Check if the match starts with whitespace and preserve it
                String matchText = textBeforeCursor.substring(lastAskIndex, lastContentStart);
                int leadingSpaceCount = 0;
                while (leadingSpaceCount < matchText.length()
                        && Character.isWhitespace(matchText.charAt(leadingSpaceCount))) {
                    leadingSpaceCount++;
                }

                if (leadingSpaceCount > 0) {
                    lastAskIndex += leadingSpaceCount;
                }

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
        if (appTriggerManager == null || !appTriggerManager.isFeatureEnabled()) {
            return null;
        }

        List<AppTrigger> triggers = appTriggerManager.getAppTriggers();

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

        for (AppTrigger trigger : triggers) {
            if (!trigger.isEnabled()) {
                continue;
            }

            String triggerText = trigger.getTrigger().toLowerCase(java.util.Locale.ROOT);

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
