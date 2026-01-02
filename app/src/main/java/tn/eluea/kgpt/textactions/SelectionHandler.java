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
package tn.eluea.kgpt.textactions;

import android.content.BroadcastReceiver;
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

import tn.eluea.kgpt.MainHook;
import tn.eluea.kgpt.external.TextActionsMenuActivity;
import tn.eluea.kgpt.provider.XposedConfigReader;

/**
 * Handles text selection detection and floating menu display.
 * Uses a transparent Activity to show the floating menu (works without SYSTEM_ALERT_WINDOW permission).
 */
public class SelectionHandler {
    
    public static final String ACTION_COMMIT_TEXT = "tn.eluea.kgpt.ACTION_COMMIT_TEXT";
    public static final String EXTRA_TEXT_TO_COMMIT = "commit_text";
    
    private static final String PREF_TEXT_ACTIONS_ENABLED = "text_actions_enabled";
    private static final long SELECTION_DEBOUNCE_MS = 400;
    private static final long MENU_COOLDOWN_MS = 2000; // Prevent spam
    
    private final Context context;
    private final OnTextActionListener actionListener;
    
    private String lastSelectedText = null;
    private int lastSelStart = -1;
    private int lastSelEnd = -1;
    private long lastMenuShowTime = 0;
    private boolean isMenuShowing = false;
    
    // Track current IMS to commit text
    private InputMethodService currentIms; 
    
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingShowMenu;
    
    private BroadcastReceiver resultReceiver;
    private boolean receiverRegistered = false;
    
    public interface OnTextActionListener {
        void onTextActionRequested(TextAction action, String selectedText);
    }
    
    public SelectionHandler(Context context, OnTextActionListener listener) {
        this.context = context;
        this.actionListener = listener;
        
        // Register broadcast receiver for action results
        registerResultReceiver();
    }
    
    /**
     * Register broadcast receiver to get results from TextActionsMenuActivity.
     */
    private void registerResultReceiver() {
        if (receiverRegistered) return;
        
        resultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_COMMIT_TEXT.equals(intent.getAction())) {
                    String text = intent.getStringExtra(EXTRA_TEXT_TO_COMMIT);
                    int start = intent.getIntExtra("selection_start", -1);
                    int end = intent.getIntExtra("selection_end", -1);
                    
                    if (text != null && currentIms != null) {
                        try {
                            InputConnection ic = currentIms.getCurrentInputConnection();
                            if (ic != null) {
                                ic.beginBatchEdit();
                                if (start >= 0 && end >= 0) {
                                    // Ensure selection is set to the original range
                                    ic.setSelection(Math.min(start, end), Math.max(start, end));
                                }
                                ic.commitText(text, 1);
                                ic.endBatchEdit();
                                MainHook.log("Committed text from AI action to [" + start + ", " + end + "]");
                            }
                        } catch (Exception e) {
                            MainHook.log("Failed to commit text: " + e.getMessage());
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
        return XposedConfigReader.getBoolean(PREF_TEXT_ACTIONS_ENABLED, false);
    }
    
    /**
     * Called when selection changes in the input field.
     */
    public void onSelectionChanged(InputMethodService ims, int oldSelStart, int oldSelEnd,
                                   int newSelStart, int newSelEnd) {
        this.currentIms = ims;
        
        if (!isEnabled()) {
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
        if (ims == null) return null;
        
        InputConnection ic = ims.getCurrentInputConnection();
        if (ic == null) return null;
        
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
            MainHook.log("Error getting selected text: " + e.getMessage());
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
            MainHook.log("Menu on cooldown, skipping");
            return;
        }
        
        if (isMenuShowing) {
            MainHook.log("Menu already showing, skipping");
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
            
            MainHook.log("Showing text actions menu for: " + 
                selectedText.substring(0, Math.min(20, selectedText.length())) + "...");
        } catch (Exception e) {
            MainHook.log("Failed to show text actions menu: " + e.getMessage());
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
        }
        
        if (receiverRegistered && resultReceiver != null) {
            try {
                context.unregisterReceiver(resultReceiver);
                receiverRegistered = false;
            } catch (Exception e) {
                MainHook.log("Error unregistering receiver: " + e.getMessage());
            }
        }
    }
}
