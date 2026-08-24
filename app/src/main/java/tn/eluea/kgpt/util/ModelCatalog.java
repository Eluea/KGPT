/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.util;

import java.util.HashMap;
import java.util.Map;

import tn.eluea.kgpt.llm.LanguageModel;

/**
 * SINGLE SOURCE OF TRUTH for per-provider sub-model presets and valid model
 * names. Used by BOTH the in-app ModelsFragment and the floating
 * ConfigureModelDialogBox so validation/presets can never drift apart again
 * (H1). Data authored from the in-app catalog (the newer of the two).
 */
public final class ModelCatalog {
    private ModelCatalog() {}

    public static final Map<LanguageModel, String[]> PRESETS = new HashMap<>();
    public static final Map<LanguageModel, java.util.Set<String>> VALID = new HashMap<>();

        static {
        // Gemini models - August 2026 latest (Free via Google AI Studio)
        PRESETS.put(LanguageModel.Gemini, new String[] {
                "gemini-3.7-flash",
                "gemini-3.6-flash",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite",
                "gemini-2.5-flash",
                "gemini-2.5-pro"
        });
        VALID.put(LanguageModel.Gemini, new java.util.HashSet<>(java.util.Arrays.asList(
                "gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite",
                "gemini-3.1-pro-preview", "gemini-3.1-flash-lite",
                "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite",
                "gemini-2.0-flash", "gemini-2.0-flash-lite")));

        // ChatGPT models - August 2026 latest (GPT-5.6 generation)
        PRESETS.put(LanguageModel.ChatGPT, new String[] {
                "gpt-5.6-sol",
                "gpt-5.6-terra",
                "gpt-5.6-luna",
                "gpt-5.6-cyber",
                "gpt-5",
                "gpt-oss-120b",
                "gpt-oss-20b"
        });
        VALID.put(LanguageModel.ChatGPT, new java.util.HashSet<>(java.util.Arrays.asList(
                "gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna", "gpt-5.6-cyber",
                "gpt-5", "gpt-4o", "gpt-4o-mini",
                "gpt-oss-120b", "gpt-oss-20b")));

        // Groq models - August 2026 latest (Free LPU inference)
        PRESETS.put(LanguageModel.Groq, new String[] {
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "qwen/qwen3-vl-32b-instruct",
                "minimaxai/minimax-m2.5",
                "groq/compound"
        });
        VALID.put(LanguageModel.Groq, null); // Allow any - Groq catalog changes frequently

        // OpenRouter models - August 2026 (Free tier available)
        PRESETS.put(LanguageModel.OpenRouter, new String[] {
                "openrouter/free",
                "nvidia/nemotron-3-ultra:free",
                "openai/gpt-oss-120b:free",
                "qwen/qwen3-coder:free",
                "google/gemini-2.5-flash:free",
                "openai/gpt-4o-mini"
        });
        VALID.put(LanguageModel.OpenRouter, null); // Allow any for OpenRouter

        // Claude models - August 2026 latest (Claude 5 generation)
        PRESETS.put(LanguageModel.Claude, new String[] {
                "claude-opus-5",
                "claude-fable-5",
                "claude-sonnet-5",
                "claude-haiku-4-5-20251001"
        });
        VALID.put(LanguageModel.Claude, new java.util.HashSet<>(java.util.Arrays.asList(
                "claude-opus-5", "claude-fable-5", "claude-sonnet-5",
                "claude-haiku-4-5-20251001", "claude-haiku-4-5",
                "claude-sonnet-4-20250514", "claude-opus-4-5-20250630",
                "claude-sonnet-4-5-20250630")));

        // Mistral models - August 2026 latest
        PRESETS.put(LanguageModel.Mistral, new String[] {
                "mistral-large-latest",
                "mistral-medium-3.5",
                "mistral-small-latest",
                "codestral-latest",
                "ministral-3-latest"
        });
        VALID.put(LanguageModel.Mistral, new java.util.HashSet<>(java.util.Arrays.asList(
                "mistral-large-latest", "mistral-medium-3.5", "mistral-small-latest",
                "codestral-latest", "ministral-3-latest",
                "mistral-small-2506", "codestral-2501")));

        // Chutes models - August 2026 latest
        PRESETS.put(LanguageModel.Chutes, new String[] {
                "deepseek-ai/DeepSeek-V3.2",
                "deepseek-ai/DeepSeek-V4-Flash-0731",
                "deepseek-ai/DeepSeek-R1",
                "meta-llama/Llama-3.3-70B-Instruct",
                "Qwen/Qwen2.5-72B-Instruct"
        });
        VALID.put(LanguageModel.Chutes, null); // Allow any model

        // Perplexity models - August 2026 latest (Search-grounded)
        PRESETS.put(LanguageModel.Perplexity, new String[] {
                "sonar-pro",
                "sonar",
                "sonar-reasoning-pro",
                "sonar-deep-research"
        });
        VALID.put(LanguageModel.Perplexity, new java.util.HashSet<>(java.util.Arrays.asList(
                "sonar-pro", "sonar", "sonar-reasoning-pro", "sonar-deep-research")));

        // GLM (ZhipuAI / Z.ai) models - August 2026 latest (GLM-5 generation)
        PRESETS.put(LanguageModel.GLM, new String[] {
                "glm-5.3",
                "glm-5.2",
                "glm-5.1",
                "glm-5",
                "glm-4-plus",
                "glm-4-flash"
        });
        VALID.put(LanguageModel.GLM, new java.util.HashSet<>(java.util.Arrays.asList(
                "glm-5.3", "glm-5.2", "glm-5.1", "glm-5",
                "glm-4-plus", "glm-4-flash", "glm-4-air")));

        // Grok (xAI) models - August 2026 latest
        PRESETS.put(LanguageModel.Grok, new String[] {
                "grok-4.6",
                "grok-4.5",
                "grok-4.3",
                "grok-4",
                "grok-3",
                "grok-2-latest",
                "grok-2",
                "grok-beta"
        });
        VALID.put(LanguageModel.Grok, new java.util.HashSet<>(java.util.Arrays.asList(
                "grok-4.6", "grok-4.5", "grok-4.3", "grok-4", "grok-3",
                "grok-2-latest", "grok-2", "grok-2-vision-1212", "grok-beta")));

        // DeepSeek models - August 2026 latest
        PRESETS.put(LanguageModel.DeepSeek, new String[] {
                "deepseek-chat",
                "deepseek-reasoner",
                "deepseek-v4-pro",
                "deepseek-v4-flash",
                "deepseek-coder"
        });
        VALID.put(LanguageModel.DeepSeek, new java.util.HashSet<>(java.util.Arrays.asList(
                "deepseek-chat", "deepseek-reasoner", "deepseek-v4-pro",
                "deepseek-v4-flash", "deepseek-coder",
                "deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-R1")));

        // Kimi (Moonshot AI) models - August 2026 latest
        PRESETS.put(LanguageModel.Kimi, new String[] {
                "kimi-k3",
                "kimi-k2.6",
                "kimi-k2.7-code",
                "kimi-k2.5",
                "moonshot-v1-auto",
                "moonshot-v1-8k",
                "moonshot-v1-32k",
                "moonshot-v1-128k"
        });
        VALID.put(LanguageModel.Kimi, new java.util.HashSet<>(java.util.Arrays.asList(
                "kimi-k3", "kimi-k2.6", "kimi-k2.7-code", "kimi-k2.5", "kimi-k2-instruct",
                "moonshot-v1-auto", "moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")));
    }
}
