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
package tn.eluea.kgpt.llm;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public enum LanguageModel {
    Gemini("Gemini", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta"),
    ChatGPT("ChatGPT", "gpt-4o", "https://api.openai.com/v1"),
    Groq("Groq", "llama-3.3-70b-versatile", "https://api.groq.com/openai/v1"),
    OpenRouter("OpenRouter", "google/gemini-2.0-flash-exp:free", "https://openrouter.ai/api/v1"),
    Claude("Claude", "claude-sonnet-4-5-20250630", "https://api.anthropic.com/v1"),
    Mistral("Mistral", "mistral-small-latest", "https://api.mistral.ai/v1"),
    ;

    public final String label;

    public final Map<LanguageModelField, String> defaults;

    LanguageModel(String label, String defaultSubModel, String defaultBaseUrl) {
        this.label = label;

        defaults = ImmutableMap.of(
                LanguageModelField.SubModel, defaultSubModel,
                LanguageModelField.BaseUrl, defaultBaseUrl,
                LanguageModelField.MaxTokens, "4096",
                LanguageModelField.Temperature, "1.0",
                LanguageModelField.TopP, "1.0"
        );
    }

    public String getDefault(LanguageModelField field) {
        return defaults.getOrDefault(field, null);
    }
}
