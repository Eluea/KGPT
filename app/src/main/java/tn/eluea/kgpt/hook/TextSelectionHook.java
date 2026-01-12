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
package tn.eluea.kgpt.hook;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import tn.eluea.kgpt.features.textactions.ui.TextActionsMenuActivity;
import tn.eluea.kgpt.provider.XposedConfigReader;
import tn.eluea.kgpt.features.textactions.domain.TextAction;

/**
 * Hook for intercepting text selection in any app.
 * Adds KGPT AI actions to the text selection menu.
 */
public class TextSelectionHook {

    private static final String TAG = "KGPT_TextSelection";
    private static final String PREF_TEXT_ACTIONS_ENABLED = "text_actions_enabled";

    // Menu item IDs for our actions
    private static final int MENU_ID_KGPT_BASE = 0x7F0F0000;
    private static final int MENU_ID_REPHRASE = MENU_ID_KGPT_BASE + 1;
    private static final int MENU_ID_FIX = MENU_ID_KGPT_BASE + 2;
    private static final int MENU_ID_IMPROVE = MENU_ID_KGPT_BASE + 3;
    private static final int MENU_ID_EXPAND = MENU_ID_KGPT_BASE + 4;
    private static final int MENU_ID_SHORTEN = MENU_ID_KGPT_BASE + 5;
    private static final int MENU_ID_FORMAL = MENU_ID_KGPT_BASE + 6;
    private static final int MENU_ID_CASUAL = MENU_ID_KGPT_BASE + 7;
    private static final int MENU_ID_TRANSLATE = MENU_ID_KGPT_BASE + 8;

    private static WeakReference<Context> appContextRef = new WeakReference<>(null);
    private static BroadcastReceiver resultReceiver;
    private static boolean receiverRegistered = false;
    private static WeakReference<TextView> currentTextViewRef = new WeakReference<>(null);

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        log("TextSelectionHook initializing for: " + lpparam.packageName);

        // Hook ActionMode.Callback to add our menu items
        hookActionModeCallback(lpparam);

        // Alternative: Hook TextView's text selection
        hookTextViewSelection(lpparam);
    }

    /**
     * Hook ActionMode.Callback to add KGPT actions to selection menu.
     */
    private static void hookActionModeCallback(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Activity.onActionModeStarted to intercept text selection menus
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "onActionModeStarted",
                    ActionMode.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (!isEnabled())
                                return;

                            Activity activity = (Activity) param.thisObject;
                            ActionMode mode = (ActionMode) param.args[0];

                            if (mode == null)
                                return;

                            // Store context for later use
                            appContextRef = new WeakReference<>(activity.getApplicationContext());

                            // Check if this is a text selection action mode
                            if (mode.getType() == ActionMode.TYPE_FLOATING) {
                                log("Floating ActionMode started, adding KGPT actions");
                                addKGPTMenuItems(mode, activity);
                            }
                        }
                    });
            log("Hooked Activity.onActionModeStarted");
        } catch (Throwable t) {
            log("Failed to hook Activity.onActionModeStarted: " + t.getMessage());
        }
    }

    /**
     * Hook TextView to detect text selection.
     */
    private static void hookTextViewSelection(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook TextView.onTextContextMenuItem to handle our custom menu items
            XposedHelpers.findAndHookMethod(
                    TextView.class,
                    "onTextContextMenuItem",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            int id = (int) param.args[0];
                            TextView textView = (TextView) param.thisObject;

                            // Check if it's one of our menu items
                            if (id >= MENU_ID_KGPT_BASE && id <= MENU_ID_TRANSLATE) {
                                handleKGPTAction(textView, id);
                                param.setResult(true); // Consume the event
                            }
                        }
                    });
            log("Hooked TextView.onTextContextMenuItem");
        } catch (Throwable t) {
            log("Failed to hook TextView.onTextContextMenuItem: " + t.getMessage());
        }
    }

    /**
     * Add KGPT menu items to the ActionMode menu.
     */
    private static void addKGPTMenuItems(ActionMode mode, Activity activity) {
        try {
            Menu menu = mode.getMenu();
            if (menu == null)
                return;

            // Add a submenu for KGPT actions
            // Using order 100+ to place after system items
            menu.add(Menu.NONE, MENU_ID_REPHRASE, 100, "âœ¨ Rephrase");
            menu.add(Menu.NONE, MENU_ID_FIX, 101, "ًں”§ Fix Errors");
            menu.add(Menu.NONE, MENU_ID_IMPROVE, 102, "ًں“‌ Improve");
            menu.add(Menu.NONE, MENU_ID_EXPAND, 103, "ًں“– Expand");
            menu.add(Menu.NONE, MENU_ID_SHORTEN, 104, "âœ‚ï¸ڈ Shorten");
            menu.add(Menu.NONE, MENU_ID_FORMAL, 105, "ًں‘” Formal");
            menu.add(Menu.NONE, MENU_ID_CASUAL, 106, "ًںکٹ Casual");
            menu.add(Menu.NONE, MENU_ID_TRANSLATE, 107, "ًںŒگ Translate");

            log("Added KGPT menu items");
        } catch (Throwable t) {
            log("Failed to add menu items: " + t.getMessage());
        }
    }

    /**
     * Handle KGPT action selection.
     */
    private static void handleKGPTAction(TextView textView, int menuId) {
        currentTextViewRef = new WeakReference<>(textView);

        // Get selected text
        int start = textView.getSelectionStart();
        int end = textView.getSelectionEnd();

        if (start < 0 || end < 0 || start == end) {
            log("No text selected");
            return;
        }

        CharSequence text = textView.getText();
        if (text == null)
            return;

        String selectedText = text.subSequence(Math.min(start, end), Math.max(start, end)).toString();
        if (selectedText.isEmpty())
            return;

        log("Selected text: " + selectedText.substring(0, Math.min(20, selectedText.length())) + "...");

        // Map menu ID to action
        TextAction action = menuIdToAction(menuId);
        if (action == null)
            return;

        // Register result receiver if needed
        Context context = textView.getContext();
        registerResultReceiver(context);

        // Launch the text action
        launchTextAction(context, action, selectedText);
    }

    /**
     * Map menu ID to TextAction.
     */
    private static TextAction menuIdToAction(int menuId) {
        switch (menuId) {
            case MENU_ID_REPHRASE:
                return TextAction.REPHRASE;
            case MENU_ID_FIX:
                return TextAction.FIX_ERRORS;
            case MENU_ID_IMPROVE:
                return TextAction.IMPROVE;
            case MENU_ID_EXPAND:
                return TextAction.EXPAND;
            case MENU_ID_SHORTEN:
                return TextAction.SHORTEN;
            case MENU_ID_FORMAL:
                return TextAction.FORMAL;
            case MENU_ID_CASUAL:
                return TextAction.CASUAL;
            case MENU_ID_TRANSLATE:
                return TextAction.TRANSLATE;
            default:
                return null;
        }
    }

    /**
     * Launch text action via broadcast to KGPT.
     */
    private static void launchTextAction(Context context, TextAction action, String selectedText) {
        try {
            // Send broadcast to KGPT to process the action
            Intent intent = new Intent("tn.eluea.kgpt.TEXT_ACTION_REQUEST");
            intent.putExtra("action", action.name());
            intent.putExtra("text", selectedText);
            intent.setPackage("tn.eluea.kgpt");
            context.sendBroadcast(intent);

            log("Sent text action request: " + action.name());
        } catch (Throwable t) {
            log("Failed to send text action: " + t.getMessage());
        }
    }

    /**
     * Register broadcast receiver for results.
     */
    private static void registerResultReceiver(Context context) {
        if (receiverRegistered || context == null)
            return;

        try {
            resultReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if ("tn.eluea.kgpt.TEXT_ACTION_RESPONSE".equals(intent.getAction())) {
                        String result = intent.getStringExtra("result");
                        if (result != null && currentTextViewRef.get() != null) {
                            replaceSelectedText(result);
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter("tn.eluea.kgpt.TEXT_ACTION_RESPONSE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(resultReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(resultReceiver, filter);
            }
            receiverRegistered = true;
            log("Result receiver registered");
        } catch (Throwable t) {
            log("Failed to register receiver: " + t.getMessage());
        }
    }

    /**
     * Replace selected text with AI result.
     */
    private static void replaceSelectedText(String newText) {
        TextView textView = currentTextViewRef.get();
        if (textView == null)
            return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Re-check strong reference on UI thread
                TextView currentTv = currentTextViewRef.get();
                if (currentTv == null)
                    return;

                int start = currentTv.getSelectionStart();
                int end = currentTv.getSelectionEnd();

                if (start >= 0 && end >= 0 && start != end) {
                    CharSequence text = currentTv.getText();
                    if (text instanceof android.text.Editable) {
                        ((android.text.Editable) text).replace(
                                Math.min(start, end),
                                Math.max(start, end),
                                newText);
                        log("Replaced text successfully");
                    }
                }
            } catch (Throwable t) {
                log("Failed to replace text: " + t.getMessage());
            }
        });
    }

    /**
     * Check if text actions feature is enabled.
     */
    private static boolean isEnabled() {
        return XposedConfigReader.getBoolean(PREF_TEXT_ACTIONS_ENABLED, false);
    }

    private static void log(String message) {
        XposedBridge.log("(" + TAG + ") " + message);
    }
}
