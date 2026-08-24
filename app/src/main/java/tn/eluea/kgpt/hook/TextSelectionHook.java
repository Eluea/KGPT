/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.hook;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import tn.eluea.kgpt.features.textactions.domain.TextAction;
import tn.eluea.kgpt.provider.XposedConfigReader;

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

    public static void hook(HookManager hookManager, ClassLoader classLoader) {
        log("TextSelectionHook initializing...");

        hookActionModeCallback(hookManager);
        hookTextViewSelection(hookManager);
    }

    private static void hookActionModeCallback(HookManager hookManager) {
        try {
            hookManager.hook(
                    Activity.class,
                    "onActionModeStarted",
                    new Class<?>[]{ActionMode.class},
                    MethodHook.after(param -> {
                        if (!isEnabled()) return;

                        Activity activity = (Activity) param.getThisObject();
                        ActionMode mode = (ActionMode) param.getArgs()[0];
                        if (mode == null) return;

                        appContextRef = new WeakReference<>(activity.getApplicationContext());

                        // Inject for BOTH floating and primary action modes —
                        // many editors use the primary type, which previously
                        // never received the items.
                        if (mode.getType() == ActionMode.TYPE_FLOATING
                                || mode.getType() == ActionMode.TYPE_PRIMARY) {
                            log("ActionMode started (type=" + mode.getType() + "), adding KGPT actions");
                            addKGPTMenuItems(mode, activity);
                        }
                    })
            );
            log("Hooked Activity.onActionModeStarted");
        } catch (Throwable t) {
            log("Failed to hook Activity.onActionModeStarted: " + t.getMessage());
        }
    }

    private static void hookTextViewSelection(HookManager hookManager) {
        try {
            hookManager.hook(
                    TextView.class,
                    "onTextContextMenuItem",
                    new Class<?>[]{int.class},
                    MethodHook.before(param -> {
                        int id = (int) param.getArgs()[0];
                        TextView textView = (TextView) param.getThisObject();

                        if (id >= MENU_ID_KGPT_BASE && id <= MENU_ID_TRANSLATE) {
                            handleKGPTAction(textView, id);
                            param.setResult(true);
                        }
                    })
            );
            log("Hooked TextView.onTextContextMenuItem");
        } catch (Throwable t) {
            log("Failed to hook TextView.onTextContextMenuItem: " + t.getMessage());
        }
    }

    private static void addKGPTMenuItems(ActionMode mode, Activity activity) {
        try {
            Menu menu = mode.getMenu();
            if (menu == null) return;

            menu.add(Menu.NONE, MENU_ID_REPHRASE, 100, "✨ Rephrase");
            menu.add(Menu.NONE, MENU_ID_FIX, 101, "🔧 Fix Errors");
            menu.add(Menu.NONE, MENU_ID_IMPROVE, 102, "📝 Improve");
            menu.add(Menu.NONE, MENU_ID_EXPAND, 103, "📖 Expand");
            menu.add(Menu.NONE, MENU_ID_SHORTEN, 104, "✂️ Shorten");
            menu.add(Menu.NONE, MENU_ID_FORMAL, 105, "👔 Formal");
            menu.add(Menu.NONE, MENU_ID_CASUAL, 106, "👟 Casual");
            menu.add(Menu.NONE, MENU_ID_TRANSLATE, 107, "🌐 Translate");

            // Force the mode to re-render its menu; without this, items added
            // after the floating popup was measured may not show up (the
            // random "first time works, then never" behavior).
            try {
                mode.invalidate();
            } catch (Throwable ignored) {}

            log("Added KGPT menu items");
        } catch (Throwable t) {
            log("Failed to add menu items: " + t.getMessage());
        }
    }

    private static void handleKGPTAction(TextView textView, int menuId) {
        currentTextViewRef = new WeakReference<>(textView);

        int start = textView.getSelectionStart();
        int end = textView.getSelectionEnd();

        if (start < 0 || end < 0 || start == end) {
            log("No text selected");
            return;
        }

        CharSequence text = textView.getText();
        if (text == null) return;

        String selectedText = text.subSequence(Math.min(start, end), Math.max(start, end)).toString();
        if (selectedText.isEmpty()) return;

        capturedSelection[0] = Math.min(start, end);
        capturedSelection[1] = Math.max(start, end);

        TextAction action = menuIdToAction(menuId);
        if (action == null) return;

        Context context = textView.getContext();
        // Target echo: the response carries the requesting package so every
        // hooked process can ignore responses not meant for it (all hooked
        // processes receive the implicit broadcast).
        targetPackage = context.getPackageName();
        registerResultReceiver(context);
        launchTextAction(context, action, selectedText, targetPackage);
    }

    private static TextAction menuIdToAction(int menuId) {
        switch (menuId) {
            case MENU_ID_REPHRASE: return TextAction.REPHRASE;
            case MENU_ID_FIX: return TextAction.FIX_ERRORS;
            case MENU_ID_IMPROVE: return TextAction.IMPROVE;
            case MENU_ID_EXPAND: return TextAction.EXPAND;
            case MENU_ID_SHORTEN: return TextAction.SHORTEN;
            case MENU_ID_FORMAL: return TextAction.FORMAL;
            case MENU_ID_CASUAL: return TextAction.CASUAL;
            case MENU_ID_TRANSLATE: return TextAction.TRANSLATE;
            default: return null;
        }
    }

    private static volatile String targetPackage = null;
    // Offsets captured at REQUEST time — re-reading at response time broke
    // replacement when the caret moved during the AI round trip.
    private static final int[] capturedSelection = {-1, -1};

    private static void launchTextAction(Context context, TextAction action, String selectedText, String targetPackage) {
        try {
            Intent intent = new Intent("tn.eluea.kgpt.TEXT_ACTION_REQUEST");
            intent.putExtra("action", action.name());
            intent.putExtra("text", selectedText);
            intent.putExtra("target_package", targetPackage);
            intent.setPackage("tn.eluea.kgpt");
            context.sendBroadcast(intent);

            log("Sent text action request: " + action.name());
        } catch (Throwable t) {
            log("Failed to send text action: " + t.getMessage());
        }
    }

    private static void registerResultReceiver(Context context) {
        if (receiverRegistered || context == null) return;

        try {
            resultReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if ("tn.eluea.kgpt.TEXT_ACTION_RESPONSE".equals(intent.getAction())) {
                        // Ignore responses addressed to other hooked processes
                        String tgt = intent.getStringExtra("target_package");
                        if (tgt != null && !tgt.equals(ctx.getPackageName())) return;
                        String result = intent.getStringExtra("result");
                        if (result != null && currentTextViewRef.get() != null) {
                            replaceSelectedText(result);
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter("tn.eluea.kgpt.TEXT_ACTION_RESPONSE");
            androidx.core.content.ContextCompat.registerReceiver(
                    context, resultReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
            receiverRegistered = true;
            log("Result receiver registered");
        } catch (Throwable t) {
            log("Failed to register receiver: " + t.getMessage());
        }
    }

    private static void replaceSelectedText(String newText) {
        TextView textView = currentTextViewRef.get();
        if (textView == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                TextView currentTv = currentTextViewRef.get();
                if (currentTv == null) return;

                // Prefer offsets captured when the action was requested
                int start = capturedSelection[0];
                int end = capturedSelection[1];
                if (start < 0 || end < 0) {
                    start = currentTv.getSelectionStart();
                    end = currentTv.getSelectionEnd();
                }

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

    private static boolean isEnabled() {
        // Default TRUE to match the in-app default; a false default made the
        // injected menu items appear only when the prefs key happened to be
        // readable/seeded — the "works once, then never" report.
        return XposedConfigReader.getBoolean(PREF_TEXT_ACTIONS_ENABLED, true);
    }

    private static void log(String message) {
        Log.d(TAG, message);
    }
}
