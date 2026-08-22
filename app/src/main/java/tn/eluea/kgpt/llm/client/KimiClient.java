/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.llm.client;

import tn.eluea.kgpt.llm.LanguageModel;

public class KimiClient extends ChatGPTClient {
    @Override
    public LanguageModel getLanguageModel() {
        return LanguageModel.Kimi;
    }
}
