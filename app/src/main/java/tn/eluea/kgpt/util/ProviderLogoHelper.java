/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.util;

import androidx.annotation.DrawableRes;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.llm.LanguageModel;

public final class ProviderLogoHelper {
    private ProviderLogoHelper() {}

    @DrawableRes
    public static int getLogoRes(LanguageModel model) {
        if (model == null) return R.drawable.ic_provider_custom;
        switch (model) {
            case Gemini:
                return R.drawable.ic_provider_gemini;
            case ChatGPT:
                return R.drawable.ic_provider_chatgpt;
            case Groq:
                return R.drawable.ic_provider_groq;
            case OpenRouter:
                return R.drawable.ic_provider_openrouter;
            case Claude:
                return R.drawable.ic_provider_claude;
            case Mistral:
                return R.drawable.ic_provider_mistral;
            case Chutes:
                return R.drawable.ic_provider_chutes;
            case Perplexity:
                return R.drawable.ic_provider_perplexity;
            case GLM:
                return R.drawable.ic_provider_zhipu;
            case Grok:
                return R.drawable.ic_provider_grok;
            case DeepSeek:
                return R.drawable.ic_provider_deepseek;
            case Kimi:
                return R.drawable.ic_provider_kimi;
            default:
                return R.drawable.ic_provider_custom;
        }
    }

    @DrawableRes
    public static int getCustomProviderLogoRes() {
        return R.drawable.ic_provider_custom;
    }
}
