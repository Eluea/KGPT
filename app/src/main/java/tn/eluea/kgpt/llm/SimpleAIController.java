/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.llm;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.listener.GenerativeAIListener;
import tn.eluea.kgpt.llm.client.LanguageModelClient;
import tn.eluea.kgpt.llm.internet.SimpleInternetProvider;
import tn.eluea.kgpt.llm.publisher.SimpleStringPublisher;

/**
 * Simplified AI Controller for use in app context (not Xposed context).
 * Does not depend on MainHook or UiInteractor.
 */
public class SimpleAIController {
    private static final String TAG = "KGPT_SimpleAI";
    
    private LanguageModelClient mModelClient = null;
    private final SPManager mSPManager;
    private final Handler mMainHandler;
    private final List<GenerativeAIListener> mListeners = new ArrayList<>();

    public SimpleAIController() {
        mSPManager = SPManager.getInstance();
        mMainHandler = new Handler(Looper.getMainLooper());
        
        if (mSPManager.hasLanguageModel()) {
            setModel(mSPManager.getLanguageModel());
        } else {
            mModelClient = LanguageModelClient.forModel(LanguageModel.Gemini);
        }
        
        // Set internet provider
        if (mModelClient != null) {
            mModelClient.setInternetProvider(new SimpleInternetProvider());
        }
    }

    private void setModel(LanguageModel model) {
        Log.d(TAG, "setModel " + model.label);
        mModelClient = LanguageModelClient.forModel(model);
        for (LanguageModelField field : LanguageModelField.values()) {
            mModelClient.setField(field, mSPManager.getLanguageModelField(model, field));
        }
        mModelClient.setInternetProvider(new SimpleInternetProvider());
    }

    public boolean needModelClient() {
        return mModelClient == null;
    }

    public boolean needApiKey() {
        return mModelClient == null || 
               mModelClient.getApiKey() == null || 
               mModelClient.getApiKey().isEmpty();
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
        Log.d(TAG, "Getting response for text length: " + prompt.length());

        if (prompt.isEmpty()) {
            return;
        }

        // Notify prepare on main thread
        mMainHandler.post(() -> {
            for (GenerativeAIListener l : mListeners) {
                l.onAIPrepare();
            }
        });

        Publisher<String> publisher;
        if (needModelClient() || needApiKey()) {
            publisher = new SimpleStringPublisher("Missing API Key. Please configure your API key in KGPT settings.");
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

                Log.d(TAG, "onNext: string with length " + s.length());

                mMainHandler.post(() -> {
                    for (GenerativeAIListener l : mListeners) {
                        l.onAINext(s);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                if (completed || hasError) {
                    Log.d(TAG, "Skipping duplicate onError");
                    return;
                }
                hasError = true;
                completed = true;
                
                Log.e(TAG, "AI Error", t);
                
                // Notify listeners about the error on main thread
                mMainHandler.post(() -> {
                    for (GenerativeAIListener l : mListeners) {
                        l.onAIError(t);
                    }
                });
            }

            @Override
            public void onComplete() {
                if (completed) {
                    Log.d(TAG, "Skipping duplicate onComplete");
                    return;
                }
                completed = true;

                Log.d(TAG, "Done");
                
                mMainHandler.post(() -> {
                    for (GenerativeAIListener l : mListeners) {
                        l.onAIComplete();
                    }
                });
            }
        });
    }

    public LanguageModel getLanguageModel() {
        return mModelClient != null ? mModelClient.getLanguageModel() : LanguageModel.Gemini;
    }
}
