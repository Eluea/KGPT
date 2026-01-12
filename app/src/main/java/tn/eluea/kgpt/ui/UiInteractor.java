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
package tn.eluea.kgpt.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.llm.LanguageModel;
import tn.eluea.kgpt.listener.ConfigChangeListener;
import tn.eluea.kgpt.listener.ConfigInfoProvider;
import tn.eluea.kgpt.listener.DialogDismissListener;
import tn.eluea.kgpt.core.ui.dialog.DialogType;
import tn.eluea.kgpt.llm.LanguageModelField;

public class UiInteractor {
    public static final String ACTION_DIALOG_RESULT = "tn.eluea.kgpt.DIALOG_RESULT";

    public static final String EXTRA_DIALOG_TYPE = "tn.eluea.kgpt.overlay.DIALOG_TYPE";

    public static final String EXTRA_CONFIG_SELECTED_MODEL = "tn.eluea.kgpt.config.SELECTED_MODEL";

    public static final String EXTRA_CONFIG_LANGUAGE_MODEL = "tn.eluea.kgpt.config.model";

    public static final String EXTRA_CONFIG_LANGUAGE_MODEL_FIELD = "tn.eluea.kgpt.config.model.%s";

    public static final String EXTRA_WEBVIEW_TITLE = "tn.eluea.kgpt.webview.TITLE";

    public static final String EXTRA_WEBVIEW_URL = "tn.eluea.kgpt.webview.URL";

    public static final String EXTRA_SEARCH_ENGINE = "tn.eluea.kgpt.webview.SEARCH_ENGINE";

    public static final String EXTRA_COMMAND_LIST = "tn.eluea.kgpt.command.LIST";

    public static final String EXTRA_COMMAND_INDEX = "tn.eluea.kgpt.command.INDEX";

    public static final String EXTRA_PATTERN_LIST = "tn.eluea.kgpt.pattern.LIST";

    public static final String EXTRA_OTHER_SETTINGS = "tn.eluea.kgpt.other_settings";

    private Context mContext = null;
    private ConfigInfoProvider mConfigInfoProvider = null;
    private final ArrayList<ConfigChangeListener> mConfigChangeListeners = new ArrayList<>();
    private final ArrayList<DialogDismissListener> mOnDismissListeners = new ArrayList<>();

    private long mLastDialogLaunch = 0L;

    private InputMethodService mInputMethodService;

    private IMSController mIMSController;

    private static UiInteractor instance = null;

    public static UiInteractor getInstance() {
        if (instance == null) {
            throw new RuntimeException("Missing call to UiInteracter.init(Context)");
        }
        return instance;
    }

    private UiInteractor(Context context, ConfigInfoProvider configInfoProvider) {
        mContext = context;
        mConfigInfoProvider = configInfoProvider;
        mIMSController = new IMSController();
    }

    public static void init(Context context) {
        instance = new UiInteractor(context, SPManager.getInstance());
    }

    public Context getContext() {
        return mContext;
    }

    public void onInputMethodCreate(InputMethodService inputMethodService) {
        registerService(inputMethodService);
        mIMSController.registerService(inputMethodService);
    }

    public void onInputMethodDestroy(InputMethodService inputMethodService) {
        unregisterService(inputMethodService);
        mIMSController.unregisterService(inputMethodService);
    }

    private final BroadcastReceiver mDialogResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_DIALOG_RESULT.equals(intent.getAction())) {
                tn.eluea.kgpt.util.Logger.log("Got result");
                boolean isPrompt = false;
                boolean isCommand = false;
                boolean isPattern = false;

                try {
                    if (!mConfigChangeListeners.isEmpty() && intent.getExtras() != null) {
                        for (String key : intent.getExtras().keySet()) {
                            if (key == null)
                                continue;

                            switch (key) {
                                case EXTRA_CONFIG_SELECTED_MODEL:
                                    String selectedModelStr = intent.getStringExtra(EXTRA_CONFIG_SELECTED_MODEL);
                                    if (selectedModelStr != null) {
                                        try {
                                            LanguageModel selectedLanguageModel = LanguageModel
                                                    .valueOf(selectedModelStr);
                                            mConfigChangeListeners
                                                    .forEach((l) -> l.onLanguageModelChange(selectedLanguageModel));
                                            isPrompt = true;
                                        } catch (IllegalArgumentException e) {
                                            tn.eluea.kgpt.util.Logger
                                                    .log("Invalid language model: " + selectedModelStr);
                                        }
                                    }
                                    break;
                                case EXTRA_CONFIG_LANGUAGE_MODEL:
                                    Bundle bundle = intent.getBundleExtra(EXTRA_CONFIG_LANGUAGE_MODEL);
                                    if (bundle != null) {
                                        for (String modelName : bundle.keySet()) {
                                            if (modelName == null)
                                                continue;
                                            try {
                                                LanguageModel configuredlanguageModel = LanguageModel
                                                        .valueOf(modelName);
                                                Bundle languageModelBundle = bundle.getBundle(modelName);
                                                if (languageModelBundle == null)
                                                    continue;

                                                for (LanguageModelField field : LanguageModelField.values()) {
                                                    if (languageModelBundle.containsKey(field.name)) {
                                                        String fieldValue = languageModelBundle.getString(field.name);
                                                        mConfigChangeListeners
                                                                .forEach(l -> l.onLanguageModelFieldChange(
                                                                        configuredlanguageModel,
                                                                        field, fieldValue));
                                                    }
                                                }
                                            } catch (IllegalArgumentException e) {
                                                tn.eluea.kgpt.util.Logger.log("Invalid model name: " + modelName);
                                            }
                                        }
                                        isPrompt = true;
                                    }
                                    break;
                                case EXTRA_COMMAND_LIST:
                                    String commandsRaw = intent.getStringExtra(EXTRA_COMMAND_LIST);
                                    if (commandsRaw != null) {
                                        mConfigChangeListeners.forEach((l) -> l.onCommandsChange(commandsRaw));
                                        isCommand = true;
                                    }
                                    break;
                                case EXTRA_PATTERN_LIST:
                                    String patternsRaw = intent.getStringExtra(EXTRA_PATTERN_LIST);
                                    if (patternsRaw != null) {
                                        mConfigChangeListeners.forEach((l) -> l.onPatternsChange(patternsRaw));
                                        isPattern = true;
                                    }
                                    break;
                                case EXTRA_OTHER_SETTINGS:
                                    tn.eluea.kgpt.util.Logger.log("Got other result");
                                    Bundle otherSettings = intent.getBundleExtra(EXTRA_OTHER_SETTINGS);
                                    if (otherSettings != null) {
                                        mConfigChangeListeners.forEach((l) -> l.onOtherSettingsChange(otherSettings));
                                    }
                                    break;
                            }
                        }
                    }
                } catch (Exception e) {
                    tn.eluea.kgpt.util.Logger.log("Error processing dialog result: " + e.getMessage());
                }

                final boolean finalIsPrompt = isPrompt;
                final boolean finalIsCommand = isCommand;
                final boolean finalIsPattern = isPattern;
                mOnDismissListeners.forEach((l) -> l.onDismiss(finalIsPrompt, finalIsCommand, finalIsPattern));
            }
        }
    };

    public boolean showChoseModelDialog() {
        if (isDialogOnCooldown()) {
            return false;
        }

        tn.eluea.kgpt.util.Logger.log("Launching configure dialog");
        mContext.startActivity(getOverlayIntent(DialogType.ChoseModel));
        return true;
    }

    public boolean showWebSearchDialog(String title, String url) {
        if (isDialogOnCooldown()) {
            return false;
        }

        Intent intent = getOverlayIntent(DialogType.WebSearch, false);
        intent.putExtra(EXTRA_WEBVIEW_TITLE, title);
        intent.putExtra(EXTRA_WEBVIEW_URL, url);
        // Send search engine preference so WebSearchActivity doesn't need SPManager
        intent.putExtra(EXTRA_SEARCH_ENGINE, SPManager.getInstance().getSearchEngine());

        tn.eluea.kgpt.util.Logger.log("Launching web search");
        mContext.startActivity(intent);

        return true;
    }

    public boolean showEditCommandsDialog() {
        if (isDialogOnCooldown()) {
            return false;
        }

        tn.eluea.kgpt.util.Logger.log("Launching commands edit");
        mContext.startActivity(getOverlayIntent(DialogType.EditCommandsList));

        return true;
    }

    public boolean showSettingsDialog() {
        if (isDialogOnCooldown()) {
            return false;
        }

        tn.eluea.kgpt.util.Logger.log("Launching settings");
        mContext.startActivity(getOverlayIntent(DialogType.Settings));

        return true;
    }

    private Intent getOverlayIntent(DialogType dialogType) {
        return getOverlayIntent(dialogType, true);
    }

    private Intent getOverlayIntent(DialogType dialogType, boolean includeSp) {
        Intent intent = new Intent("tn.eluea.kgpt.OVERLAY");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_DIALOG_TYPE, dialogType.name());
        if (includeSp) {
            String rawCommands = SPManager.getInstance().getGenerativeAICommandsRaw();
            String rawPatterns = SPManager.getInstance().getParsePatternsRaw();

            intent.putExtra(EXTRA_COMMAND_LIST, rawCommands);
            intent.putExtra(EXTRA_PATTERN_LIST, rawPatterns);
            intent.putExtra(EXTRA_CONFIG_LANGUAGE_MODEL, mConfigInfoProvider.getConfigBundle());
            intent.putExtra(EXTRA_CONFIG_SELECTED_MODEL,
                    mConfigInfoProvider.getLanguageModel().name());
            intent.putExtra(EXTRA_OTHER_SETTINGS,
                    mConfigInfoProvider.getOtherSettings());
        }

        return intent;
    }

    public void registerConfigChangeListener(ConfigChangeListener listener) {
        mConfigChangeListeners.add(listener);
    }

    public void unregisterConfigChangeListener(ConfigChangeListener listener) {
        mConfigChangeListeners.remove(listener);
    }

    public void registerOnDismissListener(DialogDismissListener listener) {
        mOnDismissListeners.add(listener);
    }

    public void unregisterOnDismissListener(DialogDismissListener listener) {
        mOnDismissListeners.remove(listener);
    }

    public void registerService(InputMethodService inputMethodService) {
        mInputMethodService = inputMethodService;
        IntentFilter filter = new IntentFilter(ACTION_DIALOG_RESULT);
        ContextCompat.registerReceiver(inputMethodService.getApplicationContext(), mDialogResultReceiver,
                filter, ContextCompat.RECEIVER_EXPORTED);
    }

    public void unregisterService(InputMethodService inputMethodService) {
        inputMethodService.getApplicationContext().unregisterReceiver(mDialogResultReceiver);
        if (inputMethodService != mInputMethodService) {
            tn.eluea.kgpt.util.Logger.log("[W] inputMethodService do not correspond in unregisterService");
            mInputMethodService = null;
        }
    }

    private boolean isDialogOnCooldown() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastDialogLaunch < 3000) {
            tn.eluea.kgpt.util.Logger
                    .log("Preventing spam dialog launch. (" + currentTime + " ~ " + mLastDialogLaunch + ")");
            return true;
        }
        mLastDialogLaunch = currentTime;
        return false;
    }

    public void toastShort(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void toastLong(String message) {
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

    public void post(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public InputConnection getInputConnection() {
        return mInputMethodService.getCurrentInputConnection();
    }

    public IMSController getIMSController() {
        return mIMSController;
    }

    public InputMethodService getIMS() {
        return mInputMethodService;
    }

    /**
     * Launch an app using ComponentName for reliable launching.
     * Uses the same approach as ActivityLauncher for maximum compatibility.
     * 
     * @param packageName  The package name of the app
     * @param activityName The activity class name (can be null for fallback)
     */
    public boolean launchApp(String packageName, String activityName) {
        android.util.Log.d("KGPT_AppTrigger", "launchApp() called with: " + packageName + "/" + activityName);

        if (packageName == null || packageName.isEmpty()) {
            android.util.Log.e("KGPT_AppTrigger", "launchApp: packageName is null or empty");
            tn.eluea.kgpt.util.Logger.log("launchApp: packageName is null or empty");
            return false;
        }

        android.util.Log.d("KGPT_AppTrigger", "launchApp: Attempting to launch " + packageName);
        tn.eluea.kgpt.util.Logger.log("launchApp: Attempting to launch " + packageName + "/" + activityName);

        try {
            // Method 1: Use ComponentName directly (ActivityLauncher approach)
            if (activityName != null && !activityName.isEmpty()) {
                android.util.Log.d("KGPT_AppTrigger", "Trying ComponentName: " + packageName + "/" + activityName);
                Intent intent = new Intent();
                intent.setComponent(new android.content.ComponentName(packageName, activityName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mContext.startActivity(intent);
                android.util.Log.d("KGPT_AppTrigger", "SUCCESS via ComponentName");
                tn.eluea.kgpt.util.Logger.log("launchApp: Success via ComponentName");
                return true;
            }
        } catch (Exception e1) {
            android.util.Log.d("KGPT_AppTrigger", "ComponentName failed: " + e1.getMessage());
            tn.eluea.kgpt.util.Logger.log("launchApp: ComponentName failed: " + e1.getMessage());
        }

        try {
            // Method 2: Try getLaunchIntentForPackage
            android.util.Log.d("KGPT_AppTrigger", "Trying getLaunchIntentForPackage");
            PackageManager pm = mContext.getPackageManager();
            Intent defaultIntent = pm.getLaunchIntentForPackage(packageName);

            if (defaultIntent != null) {
                defaultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mContext.startActivity(defaultIntent);
                android.util.Log.d("KGPT_AppTrigger", "SUCCESS via getLaunchIntentForPackage");
                tn.eluea.kgpt.util.Logger.log("launchApp: Success via getLaunchIntentForPackage");
                return true;
            }
        } catch (Exception e2) {
            android.util.Log.e("KGPT_AppTrigger", "getLaunchIntentForPackage failed: " + e2.getMessage());
        }

        try {
            // Method 3: Try direct intent with ACTION_MAIN and CATEGORY_LAUNCHER
            android.util.Log.d("KGPT_AppTrigger", "Trying ACTION_MAIN intent");
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mContext.startActivity(intent);
            android.util.Log.d("KGPT_AppTrigger", "SUCCESS via ACTION_MAIN");
            tn.eluea.kgpt.util.Logger.log("launchApp: Success via ACTION_MAIN");
            return true;
        } catch (Exception e3) {
            android.util.Log.e("KGPT_AppTrigger", "ACTION_MAIN failed: " + e3.getMessage());
        }

        android.util.Log.e("KGPT_AppTrigger", "All methods failed for " + packageName);
        return false;
    }

    /**
     * Launch an app by package name only (legacy method for backward compatibility)
     */
    public boolean launchApp(String packageName) {
        return launchApp(packageName, null);
    }
}
