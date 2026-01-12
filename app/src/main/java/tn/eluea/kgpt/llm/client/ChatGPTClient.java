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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Publisher;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.llm.publisher.ExceptionPublisher;
import tn.eluea.kgpt.llm.publisher.InternetRequestPublisher;

public class ChatGPTClient extends LanguageModelClient {
    @Override
    public Publisher<String> submitPrompt(String prompt, String systemMessage) {
        if (getApiKey() == null || getApiKey().isEmpty()) {
            return LanguageModelClient.MISSING_API_KEY_PUBLISHER;
        }

        if (systemMessage == null) {
            systemMessage = getDefaultSystemMessage();
        }

        String url = getBaseUrl() + "/chat/completions";
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", "Bearer " + getApiKey());

            JSONArray messagesJson = new JSONArray();
            messagesJson.put(new JSONObject()
                    .accumulate("role", "system")
                    .accumulate("content", systemMessage));
            messagesJson.put(new JSONObject()
                    .accumulate("role", "user")
                    .accumulate("content", prompt));
            JSONObject rootJson = new JSONObject();
            rootJson.put("model", getSubModel());
            rootJson.put("messages", messagesJson);
            rootJson.put("stream", false);
            rootJson.put("max_completion_tokens", getIntField(LanguageModelField.MaxTokens));
            rootJson.put("temperature", getDoubleField(LanguageModelField.Temperature));
            rootJson.put("top_p", getDoubleField(LanguageModelField.TopP));

            InternetRequestPublisher publisher = new InternetRequestPublisher(
                    (s, reader) -> {
                        String response = reader.lines().collect(Collectors.joining(""));
                        JSONObject responseJson = new JSONObject(response);
                        if (responseJson.has("choices")) {
                            JSONArray choices = responseJson.getJSONArray("choices");
                            for (int i = 0; i < choices.length(); i++) {
                                JSONObject choice = choices.getJSONObject(i).getJSONObject("message");
                                if (choice.has("role") && "assistant".equals(choice.getString("role"))) {
                                    s.onNext(choice
                                            .getString("content"));
                                    return;
                                }
                            }
                            if (choices.length() > 0) {
                                s.onNext(choices.getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content"));
                            }
                            else {
                                throw new JSONException("choices has length 0");
                            }
                        } else {
                            throw new JSONException("no \"choices\" attribute found");
                        }
                    },
                    (s, reader) -> {
                        String response = reader.lines().collect(Collectors.joining(""));
                        JSONObject responseJson = new JSONObject(response);
                        if (responseJson.has("error")) {
                            JSONObject errorJson = responseJson.getJSONObject("error");
                            String message = errorJson.optString("message", response);
                            String type = errorJson.optString("type", "");
                            String code = errorJson.optString("code", "");
                            
                            // Provide user-friendly error messages
                            String userMessage;
                            if ("insufficient_quota".equals(code) || message.contains("quota")) {
                                userMessage = "API quota exceeded. Check your OpenAI billing or use a different model";
                            } else if ("invalid_api_key".equals(code) || message.contains("API key")) {
                                userMessage = "Invalid API key. Please check your OpenAI API key";
                            } else if ("model_not_found".equals(code) || message.contains("does not exist")) {
                                userMessage = "Model not found: " + getSubModel() + ". Please check the model name";
                            } else if ("rate_limit_exceeded".equals(type)) {
                                userMessage = "Rate limit exceeded. Please wait and try again";
                            } else {
                                userMessage = "OpenAI Error: " + message;
                            }
                            
                            throw new RuntimeException(userMessage);
                        }
                        else {
                            throw new RuntimeException(response);
                        }
                    });
            InputStream inputStream = sendRequest(con, rootJson.toString(), publisher);
            publisher.setInputStream(inputStream);
            return publisher;
        } catch (Throwable t) {
            return new ExceptionPublisher(t);
        }
    }

    @Override
    public LanguageModel getLanguageModel() {
        return LanguageModel.ChatGPT;
    }
}
