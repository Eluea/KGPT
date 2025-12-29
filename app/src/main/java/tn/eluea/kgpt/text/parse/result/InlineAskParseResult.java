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
