/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt;

import android.content.Context;

import tn.eluea.kgpt.core.ai.AiResponseManager;
import tn.eluea.kgpt.core.dispatcher.BrainDispatcher;
import tn.eluea.kgpt.instruction.command.CommandManager;
import tn.eluea.kgpt.listener.DialogDismissListener;
import tn.eluea.kgpt.listener.InputEventListener;
import tn.eluea.kgpt.llm.GenerativeAIController;
import tn.eluea.kgpt.provider.XposedConfigReader;
import tn.eluea.kgpt.text.TextParser;
import tn.eluea.kgpt.text.parse.result.ParseResult;
import tn.eluea.kgpt.features.textactions.SelectionHandler;
import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.features.textactions.TextActionPrompts;
import tn.eluea.kgpt.ui.IMSController;
import tn.eluea.kgpt.ui.UiInteractor;
import tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerManager;

public class KGPTBrain implements InputEventListener, DialogDismissListener {

    private long lastConfigReload = 0;
    private static final long CONFIG_RELOAD_INTERVAL = 5000; // Reload config every 5 seconds

    // Track raw config payloads so periodic refresh only rebuilds on real changes
    private String lastCommandsRaw = null;
    private String lastPatternsRaw = null;

    private final GenerativeAIController mAIController; // Kept for reference if needed
    private final CommandManager mCommandManager;
    private final TextParser mTextParser;
    private final SPUpdater mSPUpdater;
    private final AppTriggerManager mAppTriggerManager;
    private final SelectionHandler mSelectionHandler;

    private final android.os.Handler mConfigHandler;
    private final android.os.HandlerThread mConfigHandlerThread;

    // Decomposed Components
    private final AiResponseManager aiResponseManager;
    private final BrainDispatcher brainDispatcher;

    public KGPTBrain(Context context) {
        IMSController.getInstance().addListener(this);
        UiInteractor.getInstance().registerOnDismissListener(this);

        // Initialize Background Config Handler
        mConfigHandlerThread = new android.os.HandlerThread("KGPT_ConfigLoader");
        mConfigHandlerThread.start();
        mConfigHandler = new android.os.Handler(mConfigHandlerThread.getLooper());

        // Schedule the config refresh on a fixed cadence instead of posting a
        // check from every single keystroke (onTextUpdate).
        mConfigHandler.post(() -> {
            android.os.Looper looper = android.os.Looper.myLooper();
            Runnable periodic = new Runnable() {
                @Override
                public void run() {
                    try {
                        reloadConfigIfNeeded();
                    } finally {
                        if (looper != null && !mConfigHandlerThread.isInterrupted()) {
                            mConfigHandler.postDelayed(this, CONFIG_RELOAD_INTERVAL);
                        }
                    }
                }
            };
            mConfigHandler.postDelayed(periodic, CONFIG_RELOAD_INTERVAL);
        });

        // Initialize Dependency Injection
        tn.eluea.kgpt.core.di.ServiceLocator locator = tn.eluea.kgpt.core.di.ServiceLocator.getInstance();

        mAIController = locator.getGenerativeAIController();
        mCommandManager = locator.getCommandManager();
        mTextParser = locator.getTextParser();
        mSPUpdater = locator.getSpUpdater();

        // Use singletons from locator
        aiResponseManager = locator.getAiResponseManager();
        brainDispatcher = locator.getBrainDispatcher();

        // Initialize App Trigger Manager via Factory
        mAppTriggerManager = locator.createAppTriggerManager(context);
        mTextParser.setAppTriggerManager(mAppTriggerManager);

        // Initialize Selection Handler for Text Actions
        mSelectionHandler = new SelectionHandler(context, this::onTextActionRequested);

        // Load inline ask prefix from config
        loadInlineAskPrefix();

        tn.eluea.kgpt.util.Logger.log("KGPTBrain initialized (Refactored)");
        tn.eluea.kgpt.util.Logger.log("XSharedPreferences available: " + XposedConfigReader.isAvailable());
    }

    /**
     * Load inline ask prefix from XSharedPreferences
     */
    private void loadInlineAskPrefix() {
        String prefix = XposedConfigReader.getString("inline_ask_prefix",
                tn.eluea.kgpt.instruction.command.InlineAskCommand.DEFAULT_PREFIX);
        tn.eluea.kgpt.instruction.command.InlineAskCommand.setPrefix(prefix);
        tn.eluea.kgpt.util.Logger.log("Loaded inline_ask_prefix: " + prefix);
    }

    /**
     * Periodically reload config from XSharedPreferences to pick up changes
     */
    private void reloadConfigIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastConfigReload > CONFIG_RELOAD_INTERVAL) {
            lastConfigReload = now;
            // Force XSharedPreferences to reload
            XposedConfigReader.forceReload();
            // Reload app triggers
            if (mAppTriggerManager != null) {
                mAppTriggerManager.reloadTriggers();
            }
            // Reload inline ask prefix
            loadInlineAskPrefix();

            // Rebuild parser/command state when the stored payloads change.
            // Without this, a hooked process that started with a stale or
            // legacy-format config stays dead until the user manually edits
            // and saves an item.
            try {
                tn.eluea.kgpt.SPManager spManager = tn.eluea.kgpt.SPManager.getInstance();
                String commandsRaw = spManager.getGenerativeAICommandsRaw();
                if (lastCommandsRaw == null || !lastCommandsRaw.equals(commandsRaw)) {
                    lastCommandsRaw = commandsRaw;
                    mCommandManager.reloadCommands();
                    tn.eluea.kgpt.util.Logger.log("Commands reloaded from periodic refresh");
                }

                String patternsRaw = spManager.getParsePatternsRaw();
                if (patternsRaw == null) {
                    patternsRaw = "";
                }
                String normalizedPatternsRaw = patternsRaw;
                if (lastPatternsRaw == null || !lastPatternsRaw.equals(normalizedPatternsRaw)) {
                    lastPatternsRaw = normalizedPatternsRaw;
                    mTextParser.reloadPatterns();
                    tn.eluea.kgpt.util.Logger.log("Patterns reloaded from periodic refresh");
                }
            } catch (Exception e) {
                tn.eluea.kgpt.util.Logger.log(e);
            }
        }
    }

    @Override
    public void onTextUpdate(String text, int cursor) {
        // Config refresh runs on its own periodic schedule (see constructor);
        // the per-keystroke hot path stays allocation-light.

        IMSController imsController = UiInteractor.getInstance().getIMSController();
        ParseResult result = mTextParser.parse(text, cursor);
        if (result != null) {
            if (result.indexEnd == cursor) {
                int deleteCount = result.indexEnd - result.indexStart;

                imsController.stopNotifyInput();
                imsController.delete(deleteCount);
                imsController.startNotifyInput();

                processParsedText(text, result);
            }
        }
    }

    public void processParsedText(String text, ParseResult parseResult) {
        brainDispatcher.dispatch(parseResult);
    }

    @Override
    public void onDismiss(boolean isPrompt, boolean isCommand, boolean isPattern) {
        if (isPrompt) {
            UiInteractor.getInstance().post(() -> {
                UiInteractor.getInstance().toastShort("Selected " + mAIController.getLanguageModel()
                        + " (" + mAIController.getModelClient().getSubModel() + ")");
            });
        } else if (isCommand) {
            UiInteractor.getInstance().post(() -> {
                UiInteractor.getInstance().toastShort("New Commands Saved");
            });
        } else if (isPattern) {
            UiInteractor.getInstance().post(() -> {
                UiInteractor.getInstance().toastShort("New Pattern Saved");
            });
        }
    }

    /**
     * Called when a text action is requested from the floating menu.
     */
    private void onTextActionRequested(TextAction action, String selectedText) {
        tn.eluea.kgpt.util.Logger.log("Text action requested: " + action.name());

        // Set text action mode to replace selected text with result
        aiResponseManager.setTextActionMode(true, selectedText);

        // Get the system message for this action
        String systemMessage = TextActionPrompts.getSystemMessage(action, UiInteractor.getInstance().getContext());
        String prompt = TextActionPrompts.buildPrompt(action, selectedText);

        // Generate response
        aiResponseManager.generateResponse(prompt, systemMessage);
    }

    /**
     * Get the selection handler for external access.
     */
    public SelectionHandler getSelectionHandler() {
        return mSelectionHandler;
    }

    /**
     * Clean up resources when the brain is no longer needed.
     * Call this when the InputMethodService is destroyed.
     */
    public void destroy() {
        // Remove listeners
        IMSController.getInstance().removeListener(this);
        UiInteractor.getInstance().unregisterOnDismissListener(this);

        // Clean up selection handler
        if (mSelectionHandler != null) {
            mSelectionHandler.destroy();
        }

        // Stop the config handler thread
        if (mConfigHandlerThread != null) {
            mConfigHandlerThread.quitSafely();
        }

        tn.eluea.kgpt.util.Logger.log("KGPTBrain destroyed");
    }
}
