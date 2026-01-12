package tn.eluea.kgpt.util;

import android.util.Log;
import tn.eluea.kgpt.SPManager;
import de.robv.android.xposed.XposedBridge;

public class Logger {
    private static final String TAG = "KGPT";
    private static boolean isXposedContext = false;

    static {
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            isXposedContext = true;
        } catch (ClassNotFoundException e) {
            isXposedContext = false;
        }
    }

    public static void log(String message) {
        if (!isXposedContext) {
            Log.d(TAG, message);
            return;
        }

        // In Xposed context
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                XposedBridge.log("(" + TAG + ") " + message);
            }
        } catch (NoClassDefFoundError | Exception e) {
            // Fallback for safety
            Log.d(TAG, message);
        }
    }

    public static void log(String tag, String message) {
        if (!isXposedContext) {
            Log.d(tag, message);
            return;
        }

        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                XposedBridge.log("(" + tag + ") " + message);
            }
        } catch (NoClassDefFoundError | Exception e) {
            Log.d(tag, message);
        }
    }

    public static void error(String message) {
        if (!isXposedContext) {
            Log.e(TAG, message);
            return;
        }
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                XposedBridge.log("(" + TAG + ") [ERROR] " + message);
            }
        } catch (Throwable t) {
            Log.e(TAG, message);
        }
    }

    public static void log(Throwable t) {
        if (!isXposedContext) {
            Log.e(TAG, "Exception", t);
            return;
        }
        try {
            if (!SPManager.isReady() || SPManager.getInstance().getEnableLogs()) {
                XposedBridge.log(t);
            }
        } catch (Throwable th) {
            Log.e(TAG, "Exception", t);
        }
    }
}
