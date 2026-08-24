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

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.SPManager;

/**
 * Manager class for managing dynamic custom AI providers.
 */
public class CustomProviderManager {

    public static final String PREF_CUSTOM_PROVIDERS = "custom_providers_json_v1";
    public static final String PREF_SELECTED_PROVIDER_TYPE = "selected_provider_type"; // "BUILTIN" or "CUSTOM"
    public static final String PREF_SELECTED_CUSTOM_PROVIDER_ID = "selected_custom_provider_id";

    public static final String TYPE_BUILTIN = "BUILTIN";
    public static final String TYPE_CUSTOM = "CUSTOM";

    private static CustomProviderManager sInstance;

    public static synchronized CustomProviderManager getInstance() {
        if (sInstance == null) {
            sInstance = new CustomProviderManager();
        }
        return sInstance;
    }

    private CustomProviderManager() {
    }

    /**
     * Get all saved custom providers
     */
    // E2: parse cache — getCustomProviders() ran on the main thread per call
    private String cachedRaw = null;
    private List<CustomProvider> cachedList = null;

    public List<CustomProvider> getCustomProviders() {
        List<CustomProvider> list = new ArrayList<>();
        if (!SPManager.isReady()) {
            return list;
        }

        String jsonStr = SPManager.getInstance().getClient().getString(PREF_CUSTOM_PROVIDERS, "[]");
        synchronized (this) {
            if (jsonStr != null && jsonStr.equals(cachedRaw) && cachedList != null) {
                return new ArrayList<>(cachedList);
            }
        }
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                CustomProvider p = CustomProvider.fromJson(obj);
                if (p != null) {
                    list.add(p);
                }
            }
        } catch (JSONException e) {
            tn.eluea.kgpt.util.Logger.error("Failed to parse custom providers JSON: " + e.getMessage());
        }

        synchronized (this) {
            cachedRaw = jsonStr;
            cachedList = new ArrayList<>(list);
        }

        return list;
    }

    /**
     * Get a specific custom provider by its ID
     */
    public CustomProvider getCustomProvider(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (CustomProvider p : getCustomProviders()) {
            if (id.equals(p.getId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Save or update a custom provider
     */
    public void saveCustomProvider(CustomProvider provider) {
        if (provider == null || !SPManager.isReady()) {
            return;
        }

        List<CustomProvider> providers = getCustomProviders();
        boolean found = false;
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getId().equals(provider.getId())) {
                providers.set(i, provider);
                found = true;
                break;
            }
        }
        if (!found) {
            providers.add(provider);
        }

        saveAll(providers);
    }

    /**
     * Delete a custom provider by ID
     */
    public void deleteCustomProvider(String id) {
        if (id == null || !SPManager.isReady()) {
            return;
        }

        List<CustomProvider> providers = getCustomProviders();
        providers.removeIf(p -> id.equals(p.getId()));
        saveAll(providers);

        // If deleted provider was currently active, fallback to Gemini
        if (isCustomProviderSelected() && id.equals(getSelectedCustomProviderId())) {
            setSelectedProviderType(TYPE_BUILTIN);
        }
    }

    private void saveAll(List<CustomProvider> providers) {
        JSONArray arr = new JSONArray();
        for (CustomProvider p : providers) {
            arr.put(p.toJson());
        }
        SPManager.getInstance().getClient().putString(PREF_CUSTOM_PROVIDERS, arr.toString());
    }

    /**
     * Check if currently active provider is a custom provider
     */
    public boolean isCustomProviderSelected() {
        if (!SPManager.isReady()) {
            return false;
        }
        String type = SPManager.getInstance().getClient().getString(PREF_SELECTED_PROVIDER_TYPE, TYPE_BUILTIN);
        return TYPE_CUSTOM.equals(type);
    }

    public void setSelectedProviderType(String type) {
        if (SPManager.isReady()) {
            SPManager.getInstance().getClient().putString(PREF_SELECTED_PROVIDER_TYPE, type);
        }
    }

    public String getSelectedCustomProviderId() {
        if (!SPManager.isReady()) {
            return null;
        }
        return SPManager.getInstance().getClient().getString(PREF_SELECTED_CUSTOM_PROVIDER_ID, null);
    }

    public void setSelectedCustomProviderId(String id) {
        if (SPManager.isReady()) {
            SPManager.getInstance().getClient().putString(PREF_SELECTED_CUSTOM_PROVIDER_ID, id);
            setSelectedProviderType(TYPE_CUSTOM);
        }
    }

    public CustomProvider getSelectedCustomProvider() {
        String id = getSelectedCustomProviderId();
        return getCustomProvider(id);
    }

    /**
     * Get API key for a custom provider
     */
    public String getCustomProviderApiKey(String customProviderId) {
        if (customProviderId == null || !SPManager.isReady()) {
            return "";
        }
        return SPManager.getInstance().getClient().getString("custom_api_key_" + customProviderId, "");
    }

    /**
     * Set API key for a custom provider
     */
    public void setCustomProviderApiKey(String customProviderId, String apiKey) {
        if (customProviderId != null && SPManager.isReady()) {
            SPManager.getInstance().getClient().putString("custom_api_key_" + customProviderId, apiKey);
        }
    }

    /**
     * Get selected submodel for a custom provider
     */
    public String getCustomProviderSubModel(String customProviderId) {
        if (customProviderId == null || !SPManager.isReady()) {
            return "";
        }
        CustomProvider p = getCustomProvider(customProviderId);
        String defaultModel = p != null ? p.getDefaultModel() : "";
        return SPManager.getInstance().getClient().getString("custom_submodel_" + customProviderId, defaultModel);
    }

    /**
     * Set selected submodel for a custom provider
     */
    public void setCustomProviderSubModel(String customProviderId, String subModel) {
        if (customProviderId != null && SPManager.isReady()) {
            SPManager.getInstance().getClient().putString("custom_submodel_" + customProviderId, subModel);
        }
    }
}
