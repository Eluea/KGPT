/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.text.parse;

import tn.eluea.kgpt.R;

public enum PatternType {
    Settings(R.string.title_settings_trigger, 0, "€$", true, "€", R.string.example_opens_settings),
    CommandAI(R.string.title_ai_trigger, 1, "(.+)\\$$", true, "$", R.string.example_ai_responds),
    CommandCustom(R.string.title_custom_command, 2, "([^%]+)%(?:([^ %]+))?%$", true, "%", R.string.example_translates),
    FormatItalic(R.string.title_italic, 1, "([^|]+)\\|$", true, "|", R.string.example_format_italic),
    FormatBold(R.string.title_bold, 1, "([^@]+)@$", true, "@", R.string.example_format_bold),
    FormatCrossout(R.string.title_crossout, 1, "([^~]+)~$", true, "~", R.string.example_format_crossout),
    FormatUnderline(R.string.title_underline, 1, "([^_]+)_$", true, "_", R.string.example_format_underline),
    WebSearch(R.string.title_web_search, 1, "(.+)\\?\\?$", true, "??", R.string.example_ai_responds),
    MediaDownloader(R.string.title_downloader_trigger, 1, "(.+)/down$", true, "/down", R.string.example_downloader_trigger);

    public final int titleResId;
    public final int groupCount;
    public final String defaultPattern;
    public final boolean editable;
    public final String defaultSymbol;
    public final int exampleResId;

    PatternType(int titleResId, int groupCount, String defaultPattern, boolean editable, String defaultSymbol,
            int exampleResId) {
        this.titleResId = titleResId;
        this.groupCount = groupCount;
        this.defaultPattern = defaultPattern;
        this.editable = editable;
        this.defaultSymbol = defaultSymbol;
        this.exampleResId = exampleResId;
    }

    /**
     * Convert a user-friendly symbol to regex pattern
     * The new logic: text is written normally, symbol at the END triggers AI
     * IMPORTANT: Requires at least one character before the trigger symbol
     */
    public static String symbolToRegex(String symbol, int groupCount) {
        if (symbol == null || symbol.isEmpty()) {
            return null;
        }

        String escapedSymbol = escapeRegex(symbol);

        if (groupCount == 0) {
            // For Settings-like patterns: exact match of the symbol
            return String.format("%s$", escapedSymbol);
        } else if (groupCount == 1) {
            // For multi-char symbols like "??", use (.+) to capture any text (at least 1
            // char)
            // For single-char symbols, use (.+) to allow symbol in text
            return String.format("(.+)%s$", escapedSymbol);
        } else if (groupCount == 2) {
            // Pattern for custom commands: text%command% or text%%
            // Changed from * to + to require at least one character before trigger
            String literalSymbol = escapeLiteralForCharClass(symbol);
            return String.format("([^%s]+)%s(?:([^ %s]+))?%s$", literalSymbol, escapedSymbol, literalSymbol,
                    escapedSymbol);
        }
        return null;
    }

    /**
     * Extract the trigger symbol from a regex pattern
     */
    public static String regexToSymbol(String regex) {
        if (regex == null || regex.isEmpty()) {
            return null;
        }

        // For CommandCustom pattern: ([^%]+)%(?:([^ %]+))?%$ - extract the % symbol
        // Look for pattern like ([^X]+)X where X is the symbol
        java.util.regex.Pattern charClassPattern = java.util.regex.Pattern.compile("\\[\\^([^\\]]+)\\]");
        java.util.regex.Matcher matcher = charClassPattern.matcher(regex);
        if (matcher.find()) {
            String charClass = matcher.group(1);
            if (charClass.length() > 0) {
                // Check if it starts with escaped char
                if (charClass.startsWith("\\") && charClass.length() > 1) {
                    return String.valueOf(charClass.charAt(1));
                } else {
                    // Get first character that's not a backslash or space
                    for (int j = 0; j < charClass.length(); j++) {
                        char c = charClass.charAt(j);
                        if (c != ' ' && c != '\\') {
                            return String.valueOf(c);
                        }
                    }
                }
            }
        }

        // Fallback: Try to find the symbol at the end (before $)
        String pattern = regex;
        if (pattern.endsWith("$")) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }

        // Build the symbol by reading escaped characters from the end
        StringBuilder symbol = new StringBuilder();
        int i = pattern.length() - 1;

        while (i >= 0) {
            char c = pattern.charAt(i);

            // Stop at regex special constructs (but not escaped ones)
            if (c == ')' || c == ']' || c == '+' || c == '*' || c == '?') {
                // Check if this is an escaped character
                if (i > 0 && pattern.charAt(i - 1) == '\\') {
                    // It's escaped, include it in symbol
                    symbol.insert(0, c);
                    i -= 2; // Skip the backslash
                    continue;
                }
                // Not escaped, stop here
                break;
            }

            // Check for escaped character
            if (i > 0 && pattern.charAt(i - 1) == '\\') {
                symbol.insert(0, c);
                i -= 2; // Skip the backslash
            } else if (c == '\\') {
                // Lone backslash, stop
                break;
            } else {
                symbol.insert(0, c);
                i--;
            }

            // Limit symbol length to prevent infinite loops
            if (symbol.length() > 20) {
                break;
            }
        }

        return symbol.length() > 0 ? symbol.toString() : null;
    }

    private static String escapeRegex(String symbol) {
        // Characters that need escaping in regex
        String specialChars = "\\^$.|?*+()[]{}";
        StringBuilder escaped = new StringBuilder();
        for (char c : symbol.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                escaped.append("\\");
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    private static String escapeLiteralForCharClass(String symbol) {
        // Characters that need escaping inside character class [...]
        String specialChars = "\\^-]";
        StringBuilder escaped = new StringBuilder();
        for (char c : symbol.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                escaped.append("\\");
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
