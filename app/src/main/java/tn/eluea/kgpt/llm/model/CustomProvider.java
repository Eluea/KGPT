/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.llm.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a dynamic user-configured AI Provider.
 */
public class CustomProvider implements Serializable {

    public enum AuthType {
        BEARER_TOKEN("Bearer Token", "Authorization: Bearer <key>"),
        CUSTOM_HEADER("Custom Header", "e.g. x-api-key: <key>"),
        QUERY_PARAM("Query Parameter", "e.g. ?key=<key>"),
        NO_AUTH("No Authentication", "Local servers without keys");

        public final String label;
        public final String description;

        AuthType(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private String id;
    private String name;
    private String baseUrl;
    private String chatEndpoint; // default: "/chat/completions"
    private String defaultModel;
    private List<String> models = new ArrayList<>();
    private AuthType authType = AuthType.BEARER_TOKEN;
    private String customHeaderName = "x-api-key";
    private String authPrefix = ""; // e.g. "Bearer " or empty
    private String customHeadersJson = "";
    private boolean supportsSystemMessage = true;
    private boolean isFree = false;
    private String getKeyUrl = "";
    private String maxTokens = "4096";
    private String temperature = "1.0";
    private String topP = "1.0";
    private long createdAt = System.currentTimeMillis();

    public CustomProvider() {
        this.id = "custom_" + System.currentTimeMillis();
        this.chatEndpoint = "/chat/completions";
    }

    public CustomProvider(String id, String name, String baseUrl, String defaultModel) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.chatEndpoint = "/chat/completions";
        this.defaultModel = defaultModel;
        if (defaultModel != null && !defaultModel.isEmpty()) {
            this.models.add(defaultModel);
        }
    }

    // Getters and Setters
    public String getId() {
        return id != null ? id : "custom_unknown";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "Custom Provider";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl != null ? baseUrl : "";
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null) {
            // Trim trailing slashes
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
        }
        this.baseUrl = baseUrl;
    }

    public String getChatEndpoint() {
        if (chatEndpoint == null || chatEndpoint.trim().isEmpty()) {
            return "/chat/completions";
        }
        if (!chatEndpoint.startsWith("/")) {
            return "/" + chatEndpoint;
        }
        return chatEndpoint;
    }

    public void setChatEndpoint(String chatEndpoint) {
        this.chatEndpoint = chatEndpoint;
    }

    public String getDefaultModel() {
        if (defaultModel != null && !defaultModel.isEmpty()) {
            return defaultModel;
        }
        if (!models.isEmpty()) {
            return models.get(0);
        }
        return "default";
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public List<String> getModels() {
        return models != null ? models : Collections.emptyList();
    }

    public void setModels(List<String> models) {
        this.models = models != null ? new ArrayList<>(models) : new ArrayList<>();
    }

    public void addModel(String model) {
        if (model != null && !model.trim().isEmpty()) {
            String trimmed = model.trim();
            if (!this.models.contains(trimmed)) {
                this.models.add(trimmed);
            }
        }
    }

    public void removeModel(String model) {
        this.models.remove(model);
    }

    public AuthType getAuthType() {
        return authType != null ? authType : AuthType.BEARER_TOKEN;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType != null ? authType : AuthType.BEARER_TOKEN;
    }

    public String getCustomHeaderName() {
        return customHeaderName != null ? customHeaderName : "x-api-key";
    }

    public void setCustomHeaderName(String customHeaderName) {
        this.customHeaderName = customHeaderName;
    }

    public String getAuthPrefix() {
        return authPrefix != null ? authPrefix : "";
    }

    public void setAuthPrefix(String authPrefix) {
        this.authPrefix = authPrefix;
    }

    public String getCustomHeadersJson() {
        return customHeadersJson != null ? customHeadersJson : "";
    }

    public void setCustomHeadersJson(String customHeadersJson) {
        this.customHeadersJson = customHeadersJson;
    }

    public boolean isSupportsSystemMessage() {
        return supportsSystemMessage;
    }

    public void setSupportsSystemMessage(boolean supportsSystemMessage) {
        this.supportsSystemMessage = supportsSystemMessage;
    }

    public boolean isFree() {
        return isFree || authType == AuthType.NO_AUTH;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public String getGetKeyUrl() {
        return getKeyUrl != null ? getKeyUrl : "";
    }

    public void setGetKeyUrl(String getKeyUrl) {
        this.getKeyUrl = getKeyUrl;
    }

    public String getMaxTokens() {
        return maxTokens != null ? maxTokens : "4096";
    }

    public void setMaxTokens(String maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getTemperature() {
        return temperature != null ? temperature : "1.0";
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getTopP() {
        return topP != null ? topP : "1.0";
    }

    public void setTopP(String topP) {
        this.topP = topP;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // JSON Serialization
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", getId());
            json.put("name", getName());
            json.put("baseUrl", getBaseUrl());
            json.put("chatEndpoint", getChatEndpoint());
            json.put("defaultModel", getDefaultModel());
            
            JSONArray modelsArr = new JSONArray();
            for (String m : getModels()) {
                modelsArr.put(m);
            }
            json.put("models", modelsArr);

            json.put("authType", getAuthType().name());
            json.put("customHeaderName", getCustomHeaderName());
            json.put("authPrefix", getAuthPrefix());
            json.put("customHeadersJson", getCustomHeadersJson());
            json.put("supportsSystemMessage", isSupportsSystemMessage());
            json.put("isFree", isFree());
            json.put("getKeyUrl", getGetKeyUrl());
            json.put("maxTokens", getMaxTokens());
            json.put("temperature", getTemperature());
            json.put("topP", getTopP());
            json.put("createdAt", getCreatedAt());
        } catch (JSONException e) {
            tn.eluea.kgpt.util.Logger.error("Error serializing CustomProvider to JSON: " + e.getMessage());
        }
        return json;
    }

    public static CustomProvider fromJson(JSONObject json) {
        if (json == null) return null;
        CustomProvider provider = new CustomProvider();
        try {
            provider.setId(json.optString("id", "custom_" + System.currentTimeMillis()));
            provider.setName(json.optString("name", "Custom Provider"));
            provider.setBaseUrl(json.optString("baseUrl", ""));
            provider.setChatEndpoint(json.optString("chatEndpoint", "/chat/completions"));
            provider.setDefaultModel(json.optString("defaultModel", ""));

            if (json.has("models")) {
                JSONArray arr = json.getJSONArray("models");
                List<String> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getString(i));
                }
                provider.setModels(list);
            }

            String authTypeStr = json.optString("authType", AuthType.BEARER_TOKEN.name());
            try {
                provider.setAuthType(AuthType.valueOf(authTypeStr));
            } catch (IllegalArgumentException e) {
                provider.setAuthType(AuthType.BEARER_TOKEN);
            }

            provider.setCustomHeaderName(json.optString("customHeaderName", "x-api-key"));
            provider.setAuthPrefix(json.optString("authPrefix", ""));
            provider.setCustomHeadersJson(json.optString("customHeadersJson", ""));
            provider.setSupportsSystemMessage(json.optBoolean("supportsSystemMessage", true));
            provider.setFree(json.optBoolean("isFree", false));
            provider.setGetKeyUrl(json.optString("getKeyUrl", ""));
            provider.setMaxTokens(json.optString("maxTokens", "4096"));
            provider.setTemperature(json.optString("temperature", "1.0"));
            provider.setTopP(json.optString("topP", "1.0"));
            provider.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
        } catch (JSONException e) {
            tn.eluea.kgpt.util.Logger.error("Error parsing CustomProvider from JSON: " + e.getMessage());
        }
        return provider;
    }
}
