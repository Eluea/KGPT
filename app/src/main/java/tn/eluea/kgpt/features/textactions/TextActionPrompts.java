/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.textactions;

import tn.eluea.kgpt.features.textactions.domain.TextAction;

/**
 * Provides system prompts for each text action.
 * Follows Clean Code principles by separating data from logic.
 */
public class TextActionPrompts {

    /**
     * Get the system message for a specific action.
     */
    public static String getSystemMessage(TextAction action, android.content.Context context) {
        return getSystemMessage(action, null, context);
    }

    /**
     * Get the system message for a specific action, with optional target info.
     */
    public static String getSystemMessage(TextAction action, String targetInfo, android.content.Context context) {
        if (context == null) {
            return "Process the following text:";
        }

        try {
            switch (action) {
                case REPHRASE:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_rephrase);
                case FIX_ERRORS:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_fix_errors);
                case IMPROVE:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_improve);
                case EXPAND:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_expand);
                case SHORTEN:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_shorten);
                case FORMAL:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_formal);
                case CASUAL:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_casual);
                case TRANSLATE:
                    if (targetInfo != null && !targetInfo.isEmpty()) {
                        return context.getString(tn.eluea.kgpt.R.string.prompt_translate_target, targetInfo);
                    }
                    return context.getString(tn.eluea.kgpt.R.string.prompt_translate_auto);
                case DOWNLOAD:
                    return context.getString(tn.eluea.kgpt.R.string.action_download_media);
                default:
                    return context.getString(tn.eluea.kgpt.R.string.prompt_default);
            }
        } catch (Exception e) {
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
