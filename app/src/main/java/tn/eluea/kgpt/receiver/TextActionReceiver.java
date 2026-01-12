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
package tn.eluea.kgpt.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.features.textactions.TextActionPrompts;
import tn.eluea.kgpt.llm.GenerativeAIController;
import tn.eluea.kgpt.listener.GenerativeAIListener;

/**
 * Receives text action requests from hooked apps and processes them with AI.
 */
public class TextActionReceiver extends BroadcastReceiver implements GenerativeAIListener {

    private static final String TAG = "KGPT_TextActionReceiver";
    public static final String ACTION_REQUEST = "tn.eluea.kgpt.TEXT_ACTION_REQUEST";
    public static final String ACTION_RESPONSE = "tn.eluea.kgpt.TEXT_ACTION_RESPONSE";

    private Context context;
    private StringBuilder responseBuilder;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_REQUEST.equals(intent.getAction()))
            return;

        this.context = context;

        String actionName = intent.getStringExtra("action");
        String text = intent.getStringExtra("text");

        if (actionName == null || text == null || text.isEmpty()) {
            Log.w(TAG, "Invalid text action request");
            return;
        }

        Log.d(TAG, "Received text action request: " + actionName);

        try {
            TextAction action = TextAction.valueOf(actionName);
            processTextAction(action, text);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown action: " + actionName);
        }
    }

    private void processTextAction(TextAction action, String text) {
        responseBuilder = new StringBuilder();

        // Get prompts for this action
        String systemMessage = TextActionPrompts.getSystemMessage(action);
        String prompt = TextActionPrompts.buildPrompt(action, text);

        // Create AI controller and generate response
        GenerativeAIController aiController = new GenerativeAIController();
        aiController.addListener(this);

        // Run in background thread
        new Thread(() -> {
            aiController.generateResponse(prompt, systemMessage);
        }).start();
    }

    @Override
    public void onAIPrepare() {
        Log.d(TAG, "AI preparing...");
    }

    @Override
    public void onAINext(String chunk) {
        responseBuilder.append(chunk);
    }

    @Override
    public void onAIError(Throwable t) {
        Log.e(TAG, "AI error: " + t.getMessage());
        sendResponse("[Error: " + t.getMessage() + "]");
    }

    @Override
    public void onAIComplete() {
        String result = responseBuilder.toString();
        Log.d(TAG, "AI complete, result length: " + result.length());
        sendResponse(result);
    }

    private void sendResponse(String result) {
        if (context == null)
            return;

        Intent responseIntent = new Intent(ACTION_RESPONSE);
        responseIntent.putExtra("result", result);
        context.sendBroadcast(responseIntent);

        Log.d(TAG, "Sent response broadcast");
    }
}
