/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
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

    /**
     * Max characters read from the field per selection update. All KGPT
     * triggers are suffix-anchored and short, so a bounded window is
     * functionally equivalent while avoiding the O(field-length) copy of a
     * full getExtractedText() binder round-trip on every keystroke.
     */
    private static final int TEXT_WINDOW_CHARS = 4000;

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
            // Bounded read first (cheap); some ICs (e.g. the Google search
            // box) return null here — fall back to a bounded extracted-text
            // read so the trigger pipeline never silently dies.
            CharSequence beforeCursor = ic.getTextBeforeCursor(TEXT_WINDOW_CHARS, 0);
            if (beforeCursor == null) {
                ExtractedTextRequest req = new ExtractedTextRequest();
                req.hintMaxChars = TEXT_WINDOW_CHARS;
                ExtractedText et = ic.getExtractedText(req, 0);
                if (et != null && et.text != null) {
                    int selEnd = Math.min(newSelEnd, et.text.length());
                    int from = Math.max(0, selEnd - TEXT_WINDOW_CHARS);
                    beforeCursor = et.text.subSequence(from, selEnd);
                }
            }
            if (beforeCursor != null) {
                typedText = beforeCursor.toString();
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

    public void hideKeyboard() {
        if (ims != null) {
            try {
                ims.requestHideSelf(0);
            } catch (Exception e) {
                tn.eluea.kgpt.util.Logger.log("Failed to hide keyboard: " + e.getMessage());
            }
        }
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
