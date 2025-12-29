package tn.eluea.kgpt.llm.client;

import tn.eluea.kgpt.llm.LanguageModel;

public class OpenRouterClient extends ChatGPTClient {
    @Override
    public LanguageModel getLanguageModel() {
        return LanguageModel.OpenRouter;
    }
}
