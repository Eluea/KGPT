/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.llm.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Publisher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;
import java.util.stream.Collectors;

import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.llm.LanguageModelField;
import tn.eluea.kgpt.llm.model.CustomProvider;
import tn.eluea.kgpt.llm.publisher.ExceptionPublisher;
import tn.eluea.kgpt.llm.publisher.InternetRequestPublisher;

public class CustomProviderClient extends LanguageModelClient {

    private final CustomProvider provider;

    public CustomProviderClient(CustomProvider provider) {
        this.provider = provider != null ? provider : new CustomProvider();
        setField(LanguageModelField.BaseUrl, this.provider.getBaseUrl());
        setField(LanguageModelField.SubModel, this.provider.getDefaultModel());
        setField(LanguageModelField.MaxTokens, this.provider.getMaxTokens());
        setField(LanguageModelField.Temperature, this.provider.getTemperature());
        setField(LanguageModelField.TopP, this.provider.getTopP());
    }

    public CustomProvider getProvider() {
        return provider;
    }

    @Override
    public LanguageModel getLanguageModel() {
        return LanguageModel.ChatGPT; // Fallback representation
    }

    @Override
    public String toString() {
        return provider.getName() + " (" + getSubModel() + ")";
    }

    @Override
    public Publisher<String> submitPrompt(String prompt, String systemMessage) {
        if (provider.getAuthType() != CustomProvider.AuthType.NO_AUTH) {
            if (getApiKey() == null || getApiKey().trim().isEmpty()) {
                return LanguageModelClient.MISSING_API_KEY_PUBLISHER;
            }
        }

        if (systemMessage == null) {
            systemMessage = getDefaultSystemMessage();
        }

        String baseUrl = getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = provider.getBaseUrl();
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String endpoint = provider.getChatEndpoint();
        String fullUrl = baseUrl + endpoint;

        if (provider.getAuthType() == CustomProvider.AuthType.QUERY_PARAM && getApiKey() != null) {
            fullUrl += (fullUrl.contains("?") ? "&" : "?") + "key=" + getApiKey();
        }

        HttpURLConnection con;
        try {
            con = (HttpURLConnection) URI.create(fullUrl).toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");

            // Apply Authentication Headers
            if (provider.getAuthType() == CustomProvider.AuthType.BEARER_TOKEN && getApiKey() != null) {
                con.setRequestProperty("Authorization", "Bearer " + getApiKey());
            } else if (provider.getAuthType() == CustomProvider.AuthType.CUSTOM_HEADER && getApiKey() != null) {
                String headerName = provider.getCustomHeaderName();
                String prefix = provider.getAuthPrefix() != null ? provider.getAuthPrefix() : "";
                con.setRequestProperty(headerName, prefix + getApiKey());
            }

            // Apply Custom Headers (JSON)
            String customHeaders = provider.getCustomHeadersJson();
            if (customHeaders != null && !customHeaders.trim().isEmpty()) {
                try {
                    JSONObject headersObj = new JSONObject(customHeaders);
                    Iterator<String> keys = headersObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        con.setRequestProperty(key, headersObj.getString(key));
                    }
                } catch (JSONException ignored) {
                }
            }

            // Construct Request Body
            JSONArray messagesJson = new JSONArray();
            if (provider.isSupportsSystemMessage()) {
                messagesJson.put(new JSONObject()
                        .accumulate("role", "system")
                        .accumulate("content", systemMessage));
                messagesJson.put(new JSONObject()
                        .accumulate("role", "user")
                        .accumulate("content", prompt));
            } else {
                // If system message is not supported separately, prepend to user prompt
                String combined = "[" + systemMessage + "]\n\n" + prompt;
                messagesJson.put(new JSONObject()
                        .accumulate("role", "user")
                        .accumulate("content", combined));
            }

            JSONObject rootJson = new JSONObject();
            String subModel = getSubModel();
            if (subModel == null || subModel.isEmpty()) {
                subModel = provider.getDefaultModel();
            }
            rootJson.put("model", subModel);
            rootJson.put("messages", messagesJson);
            rootJson.put("stream", false);

            try {
                rootJson.put("max_tokens", getIntField(LanguageModelField.MaxTokens));
            } catch (Exception e) {
                rootJson.put("max_tokens", 4096);
            }

            try {
                rootJson.put("temperature", getDoubleField(LanguageModelField.Temperature));
            } catch (Exception e) {
                rootJson.put("temperature", 1.0);
            }

            try {
                rootJson.put("top_p", getDoubleField(LanguageModelField.TopP));
            } catch (Exception e) {
                rootJson.put("top_p", 1.0);
            }

            InternetRequestPublisher publisher = new InternetRequestPublisher(
                    (s, reader) -> {
                        String response = reader.lines().collect(Collectors.joining(""));
                        JSONObject responseJson = new JSONObject(response);
                        if (responseJson.has("choices")) {
                            JSONArray choices = responseJson.getJSONArray("choices");
                            for (int i = 0; i < choices.length(); i++) {
                                JSONObject choice = choices.getJSONObject(i).getJSONObject("message");
                                if (choice.has("role") && "assistant".equals(choice.getString("role"))) {
                                    s.onNext(choice.getString("content"));
                                    return;
                                }
                            }
                            if (choices.length() > 0) {
                                s.onNext(choices.getJSONObject(0)
                                        .getJSONObject("message")
                                        .getString("content"));
                            } else {
                                throw new JSONException("choices has length 0");
                            }
                        } else if (responseJson.has("message") && responseJson.getJSONObject("message").has("content")) {
                            s.onNext(responseJson.getJSONObject("message").getString("content"));
                        } else if (responseJson.has("response")) {
                            s.onNext(responseJson.getString("response"));
                        } else {
                            throw new JSONException("No valid content found in API response");
                        }
                    },
                    (s, reader) -> {
                        String response = reader.lines().collect(Collectors.joining(""));
                        try {
                            JSONObject responseJson = new JSONObject(response);
                            if (responseJson.has("error")) {
                                JSONObject errorJson = responseJson.getJSONObject("error");
                                String message = errorJson.optString("message", response);
                                throw new RuntimeException(provider.getName() + " Error: " + message);
                            }
                        } catch (JSONException ignored) {
                        }
                        throw new RuntimeException(provider.getName() + " Error: " + response);
                    });

            InputStream inputStream = sendRequest(con, rootJson.toString(), publisher);
            publisher.setInputStream(inputStream);
            return publisher;
        } catch (Throwable t) {
            return new ExceptionPublisher(t);
        }
    }

    public interface TestCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    /**
     * Test connection to this custom provider
     */
    public static void testConnection(CustomProvider provider, String apiKey, TestCallback callback) {
        new Thread(() -> {
            try {
                String baseUrl = provider.getBaseUrl();
                while (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                String endpoint = provider.getChatEndpoint();
                String fullUrl = baseUrl + endpoint;

                if (provider.getAuthType() == CustomProvider.AuthType.QUERY_PARAM && apiKey != null && !apiKey.isEmpty()) {
                    fullUrl += (fullUrl.contains("?") ? "&" : "?") + "key=" + apiKey;
                }

                HttpURLConnection con = (HttpURLConnection) URI.create(fullUrl).toURL().openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(12000);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");

                if (provider.getAuthType() == CustomProvider.AuthType.BEARER_TOKEN && apiKey != null && !apiKey.isEmpty()) {
                    con.setRequestProperty("Authorization", "Bearer " + apiKey);
                } else if (provider.getAuthType() == CustomProvider.AuthType.CUSTOM_HEADER && apiKey != null && !apiKey.isEmpty()) {
                    String prefix = provider.getAuthPrefix() != null ? provider.getAuthPrefix() : "";
                    con.setRequestProperty(provider.getCustomHeaderName(), prefix + apiKey);
                }

                JSONObject root = new JSONObject();
                root.put("model", provider.getDefaultModel());
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "user").put("content", "Hello! Reply with 'OK' if you can read this."));
                root.put("messages", messages);
                root.put("max_tokens", 16);
                root.put("stream", false);

                con.setDoOutput(true);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                int code = con.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                        String resp = reader.lines().collect(Collectors.joining(""));
                        callback.onSuccess(resp);
                    }
                } else {
                    InputStream errStream = con.getErrorStream();
                    String err = errStream != null ? new BufferedReader(new InputStreamReader(errStream)).lines().collect(Collectors.joining("")) : "HTTP " + code;
                    callback.onError("HTTP " + code + ": " + err);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }).start();
    }
}
