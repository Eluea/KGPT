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

public class GeminiClient extends LanguageModelClient {
    @Override
    public Publisher<String> submitPrompt(String prompt, String systemMessage) {
        if (getApiKey() == null || getApiKey().isEmpty()) {
            return LanguageModelClient.MISSING_API_KEY_PUBLISHER;
        }

        if (systemMessage == null) {
            systemMessage = getDefaultSystemMessage();
        }

        String url = String.format("%s/models/%s:generateContent", getBaseUrl(), getSubModel());
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("x-goog-api-key", getApiKey());

            // Build contents array with proper format
            JSONArray contentsJson = new JSONArray();
            
            // Add system message as first user message
            JSONObject systemContent = new JSONObject();
            systemContent.put("role", "user");
            JSONArray systemParts = new JSONArray();
            systemParts.put(new JSONObject().put("text", systemMessage));
            systemContent.put("parts", systemParts);
            contentsJson.put(systemContent);
            
            // Add model acknowledgment
            JSONObject modelAck = new JSONObject();
            modelAck.put("role", "model");
            JSONArray modelParts = new JSONArray();
            modelParts.put(new JSONObject().put("text", "Understood. I will follow these instructions."));
            modelAck.put("parts", modelParts);
            contentsJson.put(modelAck);
            
            // Add user prompt
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            JSONArray userParts = new JSONArray();
            userParts.put(new JSONObject().put("text", prompt));
            userContent.put("parts", userParts);
            contentsJson.put(userContent);

            // Generation config
            JSONObject generationConfigJson = new JSONObject()
                    .put("maxOutputTokens", getIntField(LanguageModelField.MaxTokens))
                    .put("temperature", getDoubleField(LanguageModelField.Temperature))
                    .put("topP", getDoubleField(LanguageModelField.TopP));
            
            // Safety settings
            JSONArray safetySettings = new JSONArray()
                    .put(new JSONObject().put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                            .put("threshold", "BLOCK_NONE"))
                    .put(new JSONObject().put("category", "HARM_CATEGORY_HATE_SPEECH")
                            .put("threshold", "BLOCK_NONE"))
                    .put(new JSONObject().put("category", "HARM_CATEGORY_HARASSMENT")
                            .put("threshold", "BLOCK_NONE"))
                    .put(new JSONObject().put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                            .put("threshold", "BLOCK_NONE"));
            
            // Build root JSON
            JSONObject rootJson = new JSONObject();
            rootJson.put("contents", contentsJson);
            rootJson.put("generationConfig", generationConfigJson);
            rootJson.put("safetySettings", safetySettings);

            InternetRequestPublisher publisher = new InternetRequestPublisher(
                    (s, reader) -> {
                        String response = reader.lines().collect(Collectors.joining(""));
                        JSONObject responseJson = new JSONObject(response);
                        if (responseJson.has("candidates")) {
                            JSONArray candidates = responseJson.getJSONArray("candidates");
                            for (int i = 0; i < candidates.length(); i++) {
                                JSONObject candidate = candidates.getJSONObject(i);
                                if (candidate.has("content")) {
                                    JSONObject content = candidate.getJSONObject("content");
                                    if (content.has("parts")) {
                                        JSONArray parts = content.getJSONArray("parts");
                                        if (parts.length() > 0) {
                                            s.onNext(parts.getJSONObject(0).getString("text"));
                                            return;
                                        }
                                    }
                                }
                            }
                            throw new JSONException("No valid response found in candidates");
                        } else if (responseJson.has("error")) {
                            JSONObject error = responseJson.getJSONObject("error");
                            throw new RuntimeException("API Error: " + error.optString("message", "Unknown error"));
                        } else {
                            throw new JSONException("No \"candidates\" attribute found in response");
                        }
                    },
                    (s, reader) -> {
                        String response = reader.lines().collect(Collectors.joining(""));
                        try {
                            JSONObject errorJson = new JSONObject(response);
                            if (errorJson.has("error")) {
                                JSONObject error = errorJson.getJSONObject("error");
                                int code = error.optInt("code", 0);
                                String message = error.optString("message", response);
                                String status = error.optString("status", "");
                                
                                // Provide user-friendly error messages
                                String userMessage;
                                if (code == 429 || "RESOURCE_EXHAUSTED".equals(status)) {
                                    if (message.contains("limit: 0")) {
                                        userMessage = "This model requires a paid plan. Enable billing in Google Cloud Console or use a different model like gemini-2.5-flash";
                                    } else {
                                        userMessage = "Rate limit exceeded. Please wait a moment and try again";
                                    }
                                } else if (code == 404) {
                                    userMessage = "Model not found: " + getSubModel() + ". Please check the model name";
                                } else if (code == 403) {
                                    userMessage = "Access denied. Check your API key permissions";
                                } else if (code == 400) {
                                    userMessage = "Invalid request: " + message;
                                } else {
                                    userMessage = "API Error (" + code + "): " + message;
                                }
                                
                                throw new RuntimeException(userMessage);
                            }
                        } catch (JSONException e) {
                            // Not JSON, throw raw response
                        }
                        throw new RuntimeException(response);
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
        return LanguageModel.Gemini;
    }
}
