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
package tn.eluea.kgpt.llm.client;

import tn.eluea.kgpt.llm.LanguageModel;

public class OpenRouterClient extends ChatGPTClient {
    @Override
    public LanguageModel getLanguageModel() {
        return LanguageModel.OpenRouter;
    }
}
