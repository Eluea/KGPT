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
package tn.eluea.kgpt.features.textactions;

import android.content.BroadcastReceiver;
import tn.eluea.kgpt.features.textactions.domain.TextAction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import androidx.core.content.ContextCompat;

import tn.eluea.kgpt.features.textactions.ui.TextActionsMenuActivity;
import tn.eluea.kgpt.provider.XposedConfigReader;

/**
 * Handles text selection detection and floating menu display.
 * Uses a transparent Activity to show the floating menu (works without
 * SYSTEM_ALERT_WINDOW permission).
 */
public class SelectionHandler {

    public static final String ACTION_COMMIT_TEXT = "tn.eluea.kgpt.ACTION_COMMIT_TEXT";
    public static final String EXTRA_TEXT_TO_COMMIT = "commit_text";

    private static final String PREF_TEXT_ACTIONS_ENABLED = "text_actions_enabled";
    private static final long SELECTION_DEBOUNCE_MS = 600; // Increased from 400ms for better UX
    private static final long MENU_COOLDOWN_MS = 1500; // Reduced from 2000ms for better responsiveness

    private final java.lang.ref.WeakReference<Context> contextRef;
    private final OnTextActionListener actionListener;

    private String lastSelectedText = null;
    private int lastSelStart = -1;
    private int lastSelEnd = -1;
    private long lastMenuShowTime = 0;
    private volatile boolean isMenuShowing = false;

    // Track current IMS to commit text (use WeakReference to prevent leaks)
    private java.lang.ref.WeakReference<InputMethodService> currentImsRef;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingShowMenu;

    private BroadcastReceiver resultReceiver;
    private volatile boolean receiverRegistered = false;

    public interface OnTextActionListener {
        void onTextActionRequested(TextAction action, String selectedText);
    }

    public SelectionHandler(Context context, OnTextActionListener listener) {
        this.contextRef = new java.lang.ref.WeakReference<>(context);
        this.actionListener = listener;

        // Register broadcast receiver for action results
        registerResultReceiver();
    }

    /**
     * Register broadcast receiver to get results from TextActionsMenuActivity.
     */
    private void registerResultReceiver() {
        if (receiverRegistered)
            return;
        
        Context context = contextRef.get();
        if (context == null) {
            tn.eluea.kgpt.util.Logger.log("SelectionHandler: Context is null, cannot register receiver");
            return;
        }

        resultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_COMMIT_TEXT.equals(intent.getAction())) {
                    String text = intent.getStringExtra(EXTRA_TEXT_TO_COMMIT);
                    int start = intent.getIntExtra("selection_start", -1);
                    int end = intent.getIntExtra("selection_end", -1);

                    InputMethodService currentIms = currentImsRef != null ? currentImsRef.get() : null;
                    if (text != null && currentIms != null) {
                        try {
                            InputConnection ic = currentIms.getCurrentInputConnection();
                            if (ic != null) {
                                ic.beginBatchEdit();
                                
                                if (start >= 0 && end >= 0 && start != end) {
                                    int selStart = Math.min(start, end);
                                    int selEnd = Math.max(start, end);
                                    int selectedLength = selEnd - selStart;
                                    
                                    // Method 1: Set selection and delete, then commit
                                    // First, set cursor to the end of selection
                                    ic.setSelection(selEnd, selEnd);
                                    // Delete backwards (the selected text)
                                    ic.deleteSurroundingText(selectedLength, 0);
                                    // Now commit the new text at cursor position
                                    ic.commitText(text, 1);
                                    
                                    tn.eluea.kgpt.util.Logger
                                            .log("Replaced " + selectedLength + " chars at [" + selStart + ", " + selEnd + "] with " + text.length() + " chars");
                                } else {
                                    // No valid selection, just commit at cursor
                                    ic.commitText(text, 1);
                                    tn.eluea.kgpt.util.Logger.log("Committed text at cursor (no selection)");
                                }
                                
                                ic.endBatchEdit();
                            }
                        } catch (Exception e) {
                            tn.eluea.kgpt.util.Logger.log("Failed to commit text: " + e.getMessage());
                        }
                    }
                    isMenuShowing = false;
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_COMMIT_TEXT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(resultReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(resultReceiver, filter);
        }
        receiverRegistered = true;
    }

    /**
     * Check if the text actions feature is enabled.
     */
    public boolean isEnabled() {
        return XposedConfigReader.getBoolean(PREF_TEXT_ACTIONS_ENABLED, true);
    }

    /**
     * Called when selection changes in the input field.
     */
    public void onSelectionChanged(InputMethodService ims, int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd) {
        this.currentImsRef = new java.lang.ref.WeakReference<>(ims);

        boolean enabled = isEnabled();
        tn.eluea.kgpt.util.Logger.log("SelectionHandler: onSelectionChanged. Enabled=" + enabled);
        if (!enabled) {
            return;
        }

        // Check if there's a selection (not just cursor position)
        if (newSelStart != newSelEnd && newSelEnd > newSelStart) { // Initial check
            // Let's rely on getSelectedText for accurate check or just range check
            // Actually newSelEnd > newSelStart assumes order.
            // Let's normalize here.
            final int s = Math.min(newSelStart, newSelEnd);
            final int e = Math.max(newSelStart, newSelEnd);

            if (e > s) {
                // There's selected text
                String selectedText = getSelectedText(ims, s, e);

                if (selectedText != null && !selectedText.isEmpty() && selectedText.length() > 1) {
                    // Debug log
                    tn.eluea.kgpt.util.Logger.log("Selection detected: " + selectedText.length() + " chars");

                    // Check if this is a new selection
                    if (selectedText.equals(lastSelectedText) &&
                            s == lastSelStart && e == lastSelEnd) {
                        return; // Same selection, ignore
                    }

                    // Debounce to avoid flickering
                    if (pendingShowMenu != null) {
                        debounceHandler.removeCallbacks(pendingShowMenu);
                    }

                    final String finalText = selectedText;
                    final int finalStart = s;
                    final int finalEnd = e;

                    pendingShowMenu = () -> {
                        lastSelectedText = finalText;
                        lastSelStart = finalStart;
                        lastSelEnd = finalEnd;
                        showMenu(ims, finalStart, finalEnd, finalText);
                    };

                    debounceHandler.postDelayed(pendingShowMenu, SELECTION_DEBOUNCE_MS);
                }
            }
        } else {
            // No selection - cancel pending menu
            if (pendingShowMenu != null) {
                debounceHandler.removeCallbacks(pendingShowMenu);
                pendingShowMenu = null;
            }
            lastSelectedText = null;
            lastSelStart = -1;
            lastSelEnd = -1;
        }
    }

    /**
     * Get the selected text from the input connection.
     */
    private String getSelectedText(InputMethodService ims, int selStart, int selEnd) {
        if (ims == null)
            return null;

        InputConnection ic = ims.getCurrentInputConnection();
        if (ic == null)
            return null;

        try {
            // Try to get selected text directly
            CharSequence selected = ic.getSelectedText(0);
            if (selected != null && selected.length() > 0) {
                return selected.toString();
            }

            // Fallback: get from extracted text
            ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText != null && extractedText.text != null) {
                String fullText = extractedText.text.toString();
                int start = Math.max(0, Math.min(selStart, selEnd));
                int end = Math.min(fullText.length(), Math.max(selStart, selEnd));
                if (end > start) {
                    return fullText.substring(start, end);
                }
            }
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.log("Error getting selected text: " + e.getMessage());
        }

        return null;
    }

    /**
     * Show the floating action menu activity.
     */
    private void showMenu(InputMethodService ims, int start, int end, String selectedText) {
        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastMenuShowTime < MENU_COOLDOWN_MS) {
            tn.eluea.kgpt.util.Logger.log("Menu on cooldown, skipping");
            return;
        }

        if (isMenuShowing) {
            tn.eluea.kgpt.util.Logger.log("Menu already showing, skipping");
            return;
        }
        
        Context context = contextRef.get();
        if (context == null) {
            tn.eluea.kgpt.util.Logger.log("Context is null, cannot show menu");
            return;
        }

        lastMenuShowTime = now;
        isMenuShowing = true;

        try {
            // Calculate position (upper third of screen)
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            int positionY = screenHeight / 4;

            // Launch the menu activity
            Intent intent = new Intent(context, TextActionsMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(TextActionsMenuActivity.EXTRA_SELECTED_TEXT, selectedText);
            intent.putExtra("selection_start", start);
            intent.putExtra("selection_end", end);
            intent.putExtra(TextActionsMenuActivity.EXTRA_READONLY, false);

            context.startActivity(intent);

            tn.eluea.kgpt.util.Logger.log("Showing text actions menu for: " +
                    selectedText.substring(0, Math.min(20, selectedText.length())) + "...");
        } catch (Exception e) {
            tn.eluea.kgpt.util.Logger.log("Failed to show text actions menu: " + e.getMessage());
            isMenuShowing = false;
        }
    }

    /**
     * Hide the floating action menu (if showing).
     */
    public void hideMenu() {
        isMenuShowing = false;
    }

    /**
     * Clean up resources.
     */
    public void destroy() {
        if (pendingShowMenu != null) {
            debounceHandler.removeCallbacks(pendingShowMenu);
            pendingShowMenu = null;
        }
        
        // Clear references
        currentImsRef = null;

        if (receiverRegistered && resultReceiver != null) {
            try {
                Context context = contextRef.get();
                if (context != null) {
                    context.unregisterReceiver(resultReceiver);
                }
                receiverRegistered = false;
            } catch (Exception e) {
                tn.eluea.kgpt.util.Logger.log("Error unregistering receiver: " + e.getMessage());
            }
        }
    }
}
