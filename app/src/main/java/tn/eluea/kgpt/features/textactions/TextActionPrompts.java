package tn.eluea.kgpt.features.textactions;

import tn.eluea.kgpt.features.textactions.domain.TextAction;

/**
 * Provides system prompts for each text action.
 * Follows Clean Code principles by separating data from logic.
 */
public class TextActionPrompts {

    private static final String PROMPT_REPHRASE = "You are a text rephrasing assistant. Rephrase the given text while keeping the exact same meaning. "
            +
            "Maintain the same language as the input. Only output the rephrased text, nothing else.";

    private static final String PROMPT_FIX_ERRORS = "You are a grammar and spelling correction assistant. Fix all grammar, spelling, and punctuation errors in the given text. "
            +
            "Maintain the same language and meaning. Only output the corrected text, nothing else.";

    private static final String PROMPT_IMPROVE = "You are a writing improvement assistant. Improve the style, clarity, and flow of the given text while keeping the same meaning. "
            +
            "Maintain the same language. Only output the improved text, nothing else.";

    private static final String PROMPT_EXPAND = "You are a text expansion assistant. Expand the given text by adding more details, examples, or explanations while keeping the core meaning. "
            +
            "Maintain the same language. Only output the expanded text, nothing else.";

    private static final String PROMPT_SHORTEN = "You are a text summarization assistant. Shorten the given text while keeping the essential meaning and key points. "
            +
            "Maintain the same language. Only output the shortened text, nothing else.";

    private static final String PROMPT_FORMAL = "You are a tone adjustment assistant. Convert the given text to a formal, professional tone. "
            +
            "Maintain the same language and meaning. Only output the formal version, nothing else.";

    private static final String PROMPT_CASUAL = "You are a tone adjustment assistant. Convert the given text to a casual, friendly tone. "
            +
            "Maintain the same language and meaning. Only output the casual version, nothing else.";

    private static final String PROMPT_TRANSLATE_AUTO = "You are a translation assistant. Detect the language of the input text and translate it to the opposite language "
            +
            "(if Arabic, translate to English; if English, translate to Arabic; for other languages, translate to English). "
            +
            "Only output the translated text, nothing else.";

    private static final String PROMPT_DEFAULT = "Process the following text:";

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
                return PROMPT_REPHRASE;
            case FIX_ERRORS:
                return PROMPT_FIX_ERRORS;
            case IMPROVE:
                return PROMPT_IMPROVE;
            case EXPAND:
                return PROMPT_EXPAND;
            case SHORTEN:
                return PROMPT_SHORTEN;
            case FORMAL:
                return PROMPT_FORMAL;
            case CASUAL:
                return PROMPT_CASUAL;
            case TRANSLATE:
                if (targetInfo != null && !targetInfo.isEmpty()) {
                    return "You are a translation assistant. Translate the given text to " + targetInfo + ". " +
                            "Only output the translated text, nothing else.";
                }
                return PROMPT_TRANSLATE_AUTO;
            default:
                return PROMPT_DEFAULT;
        }
    }

    /**
     * Build the full prompt for the AI.
     */
    public static String buildPrompt(TextAction action, String selectedText) {
        return selectedText;
    }
}
