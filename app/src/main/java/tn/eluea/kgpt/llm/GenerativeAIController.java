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
package tn.eluea.kgpt.llm;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.listener.GenerativeAIListener;
import tn.eluea.kgpt.llm.client.LanguageModelClient;
import tn.eluea.kgpt.listener.ConfigChangeListener;
import tn.eluea.kgpt.llm.internet.InternetProvider;
import tn.eluea.kgpt.llm.internet.SimpleInternetProvider;
import tn.eluea.kgpt.llm.publisher.SimpleStringPublisher;
import tn.eluea.kgpt.llm.service.ExternalInternetProvider;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.ui.UiInteractor;

public class GenerativeAIController implements ConfigChangeListener {
    private LanguageModelClient mModelClient = null;

    private final SPManager mSPManager;
    private final UiInteractor mInteractor;
    private ExternalInternetProvider mExternalClient = null;

    private List<GenerativeAIListener> mListeners = new ArrayList<>();
    private InternetProvider mInternetProvider = new SimpleInternetProvider();

    public GenerativeAIController() {
        mSPManager = SPManager.getInstance();
        mInteractor = UiInteractor.getInstance();

        mInteractor.registerConfigChangeListener(this);
        if (mSPManager.hasLanguageModel()) {
            setModel(mSPManager.getLanguageModel());
        } else {
            mModelClient = LanguageModelClient.forModel(LanguageModel.Gemini);
        }

        updateInternetProvider();
    }

    private void updateInternetProvider() {
        updateInternetProvider(null);
    }

    private void updateInternetProvider(Boolean enableExternalInternet) {
        // Always use SimpleInternetProvider for now
        // ExternalInternetProvider has issues on Android 12+
        tn.eluea.kgpt.util.Logger.log("Using SimpleInternetProvider");
        mInternetProvider = new SimpleInternetProvider();

        if (mModelClient != null) {
            mModelClient.setInternetProvider(mInternetProvider);
        }
    }

    public boolean needModelClient() {
        return mModelClient == null;
    }

    public boolean needApiKey() {
        return mModelClient.getApiKey() == null || mModelClient.getApiKey().isEmpty();
    }

    private void setModel(LanguageModel model) {
        tn.eluea.kgpt.util.Logger.log("setModel " + model.label);
        mModelClient = LanguageModelClient.forModel(model);
        for (LanguageModelField field : LanguageModelField.values()) {
            mModelClient.setField(field, mSPManager.getLanguageModelField(model, field));
        }
        mModelClient.setInternetProvider(mInternetProvider);
    }

    @Override
    public void onLanguageModelChange(LanguageModel model) {
        if (mModelClient == null || mModelClient.getLanguageModel() != model) {
            setModel(model);
        }
    }

    @Override
    public void onLanguageModelFieldChange(LanguageModel model, LanguageModelField field, String value) {
        if (mModelClient != null && mModelClient.getLanguageModel() == model) {
            mModelClient.setField(field, value);
        }
    }

    @Override
    public void onCommandsChange(String commandsRaw) {
    }

    @Override
    public void onPatternsChange(String patternsRaw) {

    }

    @Override
    public void onOtherSettingsChange(Bundle otherSettings) {
        String enableInternetKey = OtherSettingsType.EnableExternalInternet.name();
        if (otherSettings.containsKey(enableInternetKey)) {
            boolean enableExternalInternet = otherSettings.getBoolean(enableInternetKey);
            updateInternetProvider(enableExternalInternet);
        }
    }

    public void addListener(GenerativeAIListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(GenerativeAIListener listener) {
        mListeners.remove(listener);
    }

    public void generateResponse(String prompt) {
        generateResponse(prompt, null);
    }

    public void generateResponse(String prompt, String systemMessage) {
        tn.eluea.kgpt.util.Logger.log("Getting response for text \"" + prompt + "\"");

        if (prompt.isEmpty()) {
            return;
        }

        mInteractor.post(() -> mListeners.forEach(GenerativeAIListener::onAIPrepare));

        Publisher<String> publisher;
        if (needModelClient()) {
            publisher = new SimpleStringPublisher("Missing API Key");
        } else {
            publisher = mModelClient.submitPrompt(prompt, systemMessage);
        }

        publisher.subscribe(new Subscriber<String>() {
            boolean completed = false;
            boolean hasError = false;

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String s) {
                if (s == null || s.isEmpty()) {
                    return;
                }

                tn.eluea.kgpt.util.Logger.log("onNext: string with length " + s.length());

                mInteractor.post(() -> mListeners.forEach(
                        l -> l.onAINext(s)));
            }

            @Override
            public void onError(Throwable t) {
                if (completed || hasError) {
                    tn.eluea.kgpt.util.Logger.log("Skipping duplicate onError");
                    return;
                }
                hasError = true;
                completed = true;

                tn.eluea.kgpt.util.Logger.error(t.getMessage());

                // Notify listeners about the error
                mInteractor.post(() -> {
                    mListeners.forEach(l -> l.onAIError(t));
                });
                tn.eluea.kgpt.util.Logger.log("Error handled");
            }

            @Override
            public void onComplete() {
                if (completed) {
                    tn.eluea.kgpt.util.Logger.log("Skipping duplicate onComplete");
                    return;
                }
                completed = true;

                mInteractor.post(() -> mListeners.forEach(GenerativeAIListener::onAIComplete));
                tn.eluea.kgpt.util.Logger.log("Done");
            }
        });
    }

    public LanguageModel getLanguageModel() {
        return mModelClient.getLanguageModel();
    }

    public LanguageModelClient getModelClient() {
        return mModelClient;
    }
}
