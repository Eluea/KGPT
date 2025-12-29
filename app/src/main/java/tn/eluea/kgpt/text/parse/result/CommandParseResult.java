package tn.eluea.kgpt.text.parse.result;

import java.util.List;

public class CommandParseResult extends ParseResult {
    public final String command;
    public final String prompt;

    protected CommandParseResult(List<String> groups, int indexStart, int indexEnd) {
        super(groups, indexStart, indexEnd);

        // New logic: text first, then symbol at end
        // Group 1 = prompt (the text before the symbol)
        // Group 2 = command (optional, for custom commands like %translate%)
        this.prompt = groups.size() >= 2 && groups.get(1) != null ? groups.get(1).trim() : "";
        this.command = groups.size() >= 3 && groups.get(2) != null ? groups.get(2).trim() : "";
    }
}
