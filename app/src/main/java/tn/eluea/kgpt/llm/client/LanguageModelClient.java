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

import androidx.annotation.NonNull;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.llm.internet.InternetProvider;
import tn.eluea.kgpt.llm.internet.SimpleInternetProvider;
import tn.eluea.kgpt.llm.service.InternetRequestListener;

public abstract class LanguageModelClient {
    private static final String TAG = "KGPT_LMClient";
    private Map<LanguageModelField, String> mFields = new HashMap<>();

    private InternetProvider mInternetProvider = new SimpleInternetProvider();

    abstract public Publisher<String> submitPrompt(String prompt, String systemMessage);

    abstract public LanguageModel getLanguageModel();

    public void setField(LanguageModelField field, String value) {
        mFields.put(field, value);
    }

    public String getField(LanguageModelField field) {
        return mFields.getOrDefault(field, getLanguageModel().getDefault(field));
    }

    public double getDoubleField(LanguageModelField field) {
        try {
            String doubleStr = mFields.getOrDefault(field, getLanguageModel().getDefault(field));
            if (doubleStr != null) {
                return Double.parseDouble(doubleStr);
            }
        } catch (NumberFormatException | NullPointerException e) {
            Log.e(TAG, "Error parsing double field", e);
        }
        return Double.parseDouble(getLanguageModel().getDefault(field));
    }

    public int getIntField(LanguageModelField field) {
        try {
            String intStr = mFields.getOrDefault(field, getLanguageModel().getDefault(field));
            if (intStr != null) {
                return Integer.parseInt(intStr);
            }
        } catch (NumberFormatException | NullPointerException e) {
            Log.e(TAG, "Error parsing int field", e);
        }
        return Integer.parseInt(getLanguageModel().getDefault(field));
    }

    public String getSubModel() {
        return getField(LanguageModelField.SubModel);
    }

    public String getApiKey() {
        String key = getField(LanguageModelField.ApiKey);
        return key != null ? key.trim() : null;
    }

    public String getBaseUrl() {
        return getField(LanguageModelField.BaseUrl);
    }

    public static LanguageModelClient forModel(LanguageModel model) {
        switch (model) {
            case Gemini:
                return new GeminiClient();
            case Groq:
                return new GroqClient();
            case OpenRouter:
                return new OpenRouterClient();
            case Claude:
                return new ClaudeClient();
            case Mistral:
                return new MistralClient();
            case ChatGPT:
            default:
                return new ChatGPTClient();
        }
    }

    static Publisher<String> MISSING_API_KEY_PUBLISHER = subscriber -> {
        subscriber.onNext("Missing API Key");
        subscriber.onComplete();
    };

    @NonNull
    @Override
    public String toString() {
        return getLanguageModel().label + " (" + getSubModel() + ")";
    }

    protected static String getDefaultSystemMessage() {
        return "You are a helpful assistant integrated inside a keyboard.";
    }

    public void setInternetProvider(InternetProvider internetProvider) {
        mInternetProvider = internetProvider;
    }

    protected InputStream sendRequest(HttpURLConnection con, String body, InternetRequestListener irl)
            throws IOException {
        return mInternetProvider.sendRequest(con, body, irl);
    }
}
