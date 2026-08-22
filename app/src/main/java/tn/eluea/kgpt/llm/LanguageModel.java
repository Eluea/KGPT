/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.llm;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public enum LanguageModel {
        Gemini("Gemini", "gemini-3.7-flash", "https://generativelanguage.googleapis.com/v1beta", true,
                        "https://aistudio.google.com/app/apikey"),
        ChatGPT("ChatGPT", "gpt-5", "https://api.openai.com/v1", false, "https://platform.openai.com/api-keys"),
        Groq("Groq", "openai/gpt-oss-120b", "https://api.groq.com/openai/v1", true,
                        "https://console.groq.com/keys"),
        OpenRouter("OpenRouter", "openrouter/free", "https://openrouter.ai/api/v1", true,
                        "https://openrouter.ai/keys"),
        Claude("Claude", "claude-sonnet-5", "https://api.anthropic.com/v1", false,
                        "https://console.anthropic.com/settings/keys"),
        Mistral("Mistral", "mistral-small-latest", "https://api.mistral.ai/v1", false,
                        "https://console.mistral.ai/api-keys"),
        Chutes("Chutes", "deepseek-ai/DeepSeek-V3.2", "https://api.chutes.ai/v1", false,
                        "https://chutes.ai"),
        Perplexity("Perplexity", "sonar-pro", "https://api.perplexity.ai", false,
                        "https://www.perplexity.ai/settings/api"),
        GLM("ZhipuAI", "glm-5.3", "https://open.bigmodel.cn/api/paas/v4", false,
                        "https://open.bigmodel.cn/usercenter/apikeys"),
        Grok("Grok", "grok-4.6", "https://api.x.ai/v1", false,
                        "https://console.x.ai"),
        DeepSeek("DeepSeek", "deepseek-chat", "https://api.deepseek.com", false,
                        "https://platform.deepseek.com/api_keys"),
        Kimi("Kimi", "kimi-k3", "https://api.moonshot.ai/v1", false,
                        "https://platform.kimi.ai/console/api-keys"),
        ;

        public final String label;
        public final boolean isFree;
        public final String getKeyUrl;

        public final Map<LanguageModelField, String> defaults;

        LanguageModel(String label, String defaultSubModel, String defaultBaseUrl, boolean isFree, String getKeyUrl) {
                this.label = label;
                this.isFree = isFree;
                this.getKeyUrl = getKeyUrl;

                defaults = ImmutableMap.of(
                                LanguageModelField.SubModel, defaultSubModel,
                                LanguageModelField.BaseUrl, defaultBaseUrl,
                                LanguageModelField.MaxTokens, "4096",
                                LanguageModelField.Temperature, "1.0",
                                LanguageModelField.TopP, "1.0");
        }

        public String getDefault(LanguageModelField field) {
                return defaults.getOrDefault(field, null);
        }
}
