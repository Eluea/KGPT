/*
 * Copyright (c) 2025 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.util;

import android.util.Log;
import tn.eluea.kgpt.SPManager;

public class Logger {
    private static final String TAG = "KGPT";

    public static void log(String message) {
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                Log.d(TAG, message);
            }
        } catch (Throwable t) {
            Log.d(TAG, message);
        }
    }

    public static void log(String tag, String message) {
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                Log.d(tag, message);
            }
        } catch (Throwable t) {
            Log.d(tag, message);
        }
    }

    public static void error(String message) {
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                Log.e(TAG, "[ERROR] " + message);
            }
        } catch (Throwable t) {
            Log.e(TAG, message);
        }
    }

    public static void log(Throwable t) {
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                Log.e(TAG, "Exception: " + t.getMessage(), t);
            }
        } catch (Throwable th) {
            Log.e(TAG, "Exception", t);
        }
    }
}
