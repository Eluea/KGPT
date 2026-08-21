package tn.eluea.kgpt.util;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;

import java.io.DataOutputStream;

public class SystemBlurHelper {

    private static final String TAG = "KGPT_SystemBlurHelper";

    /**
     * Checks if cross-window blur is supported and enabled by the OS.
     */
    public static boolean isCrossWindowBlurSupported(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        try {
            WindowManager wm = context.getSystemService(WindowManager.class);
            if (wm != null) {
                return wm.isCrossWindowBlurEnabled();
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Checks if system disabled window blurs (e.g. disable_window_blurs = 1).
     */
    public static boolean isSystemBlurDisabled(Context context) {
        if (context == null) return false;
        try {
            int val = Settings.Global.getInt(context.getContentResolver(), "disable_window_blurs", 0);
            return val == 1;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Attempts to enable window blurs system-wide (via Settings.Global and Root shell).
     */
    public static void enableSystemWindowBlurs(Context context) {
        if (context == null) return;
        new Thread(() -> {
            try {
                // 1. Try Settings.Global directly
                try {
                    Settings.Global.putInt(context.getContentResolver(), "disable_window_blurs", 0);
                } catch (Throwable ignored) {}

                // 2. Try via Root shell if available
                try {
                    Process process = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    os.writeBytes("settings put global disable_window_blurs 0\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    process.waitFor();
                } catch (Throwable ignored) {}
            } catch (Throwable t) {
                Logger.log("Failed to enable system window blurs: " + t.getMessage());
            }
        }).start();
    }
}
