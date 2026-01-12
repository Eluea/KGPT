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

import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import java.util.ArrayList;
import java.util.List;

import tn.eluea.kgpt.listener.InputEventListener;

public class IMSController {
    private static final long INPUT_LOCK_TIMEOUT_MS = 15000; // 15 seconds timeout (reduced from 60s)

    private InputMethodService ims = null;
    private String typedText = "";
    private int cursor = 0;
    private volatile boolean inputNotify = false;
    private volatile boolean inputLock = false;
    private volatile long inputLockStartTime = 0;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable lockTimeoutRunnable = () -> {
        if (inputLock) {
            // Force unlock after timeout
            tn.eluea.kgpt.util.Logger.log("Input lock timeout - forcing unlock");
            inputLock = false;
            inputNotify = false;
            inputLockStartTime = 0;
        }
    };

    private List<InputEventListener> mListeners = new ArrayList<>();

    public IMSController() {
    }

    public static IMSController getInstance() {
        return UiInteractor.getInstance().getIMSController();
    }

    public void onUpdateSelection(int oldSelStart,
            int oldSelEnd,
            int newSelStart,
            int newSelEnd,
            int candidatesStart,
            int candidatesEnd) {
        if (inputNotify) {
            return;
        }
        if (ims == null)
            return;
        InputConnection ic = ims.getCurrentInputConnection();
        if (ic != null) {
            ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (extractedText != null && extractedText.text != null) {
                typedText = extractedText.text.toString();
                cursor = newSelEnd;
                notifyTextUpdate();
            }
        }
    }

    public void addListener(InputEventListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(InputEventListener listener) {
        mListeners.remove(listener);
    }

    private void notifyTextUpdate() {
        for (InputEventListener listener : mListeners) {
            listener.onTextUpdate(typedText, cursor);
        }
    }

    public void registerService(InputMethodService ims) {
        this.ims = ims;
    }

    public void unregisterService(InputMethodService ims) {
        this.ims = null;
    }

    public void delete(int count) {
        if (ims == null)
            return;
        InputConnection ic = ims.getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(count, 0);
        }
    }

    public void commit(String text) {
        if (ims == null)
            return;
        InputConnection ic = ims.getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    public void stopNotifyInput() {
        inputNotify = true;
    }

    public void startNotifyInput() {
        inputNotify = false;
    }

    public void flush() {
        if (ims == null)
            return;
        InputConnection ic = ims.getCurrentInputConnection();
        if (ic != null) {
            ic.finishComposingText();
        }
    }

    public boolean isInputLocked() {
        // Auto-unlock if timeout exceeded
        if (inputLock && inputLockStartTime > 0) {
            long elapsed = System.currentTimeMillis() - inputLockStartTime;
            if (elapsed > INPUT_LOCK_TIMEOUT_MS) {
                inputLock = false;
                inputNotify = false;
                inputLockStartTime = 0;
                timeoutHandler.removeCallbacks(lockTimeoutRunnable);
            }
        }
        return inputLock;
    }

    public void startInputLock() {
        inputLock = true;
        inputLockStartTime = System.currentTimeMillis();
        // Schedule timeout
        timeoutHandler.removeCallbacks(lockTimeoutRunnable);
        timeoutHandler.postDelayed(lockTimeoutRunnable, INPUT_LOCK_TIMEOUT_MS);
    }

    public void endInputLock() {
        inputLock = false;
        inputLockStartTime = 0;
        timeoutHandler.removeCallbacks(lockTimeoutRunnable);
    }

    /**
     * Force reset the input lock state. Use this to recover from stuck states.
     */
    public void forceResetLock() {
        inputLock = false;
        inputNotify = false;
        inputLockStartTime = 0;
        timeoutHandler.removeCallbacks(lockTimeoutRunnable);
    }
}
