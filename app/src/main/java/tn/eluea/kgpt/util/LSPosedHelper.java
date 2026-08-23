/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.util;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

/**
 * Central LSPosed Modern API Service & Scope Manager.
 * Handles dynamic scope request prompts and scope checks in LSPosed.
 */
public class LSPosedHelper {

    private static final String TAG = "LSPosedHelper";
    private static volatile XposedService sService = null;
    private static volatile boolean isBound = false;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public interface ScopeCallback {
        void onResult(boolean approved);
    }

    public interface ScopeListener {
        void onScopeStatusChanged(String packageName, boolean inScope);
    }

    private static final List<ScopeListener> sScopeListeners = new CopyOnWriteArrayList<>();

    public static void addScopeListener(ScopeListener listener) {
        if (listener != null && !sScopeListeners.contains(listener)) {
            sScopeListeners.add(listener);
        }
    }

    public static void removeScopeListener(ScopeListener listener) {
        sScopeListeners.remove(listener);
    }

    public static void init(Context context) {
        try {
            XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
                @Override
                public void onServiceBind(XposedService service) {
                    sService = service;
                    isBound = true;
                    Log.d(TAG, "LSPosed XposedService bound successfully, API: " + service.getApiVersion());
                }

                @Override
                public void onServiceDied(XposedService service) {
                    sService = null;
                    isBound = false;
                    Log.d(TAG, "LSPosed XposedService died");
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "Failed to register XposedService listener: " + t.getMessage());
        }
    }

    public static XposedService getService() {
        return sService;
    }

    public static boolean isLSPosedActive() {
        return isBound && sService != null && sService.getApiVersion() > 0;
    }

    /**
     * Check if a specific package is currently in LSPosed active scope.
     */
    public static boolean isPackageInScope(String packageName) {
        if (sService != null && packageName != null) {
            try {
                List<String> scope = sService.getScope();
                if (scope != null) {
                    return scope.contains(packageName);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed to get scope from service: " + t.getMessage());
            }
        }
        return false;
    }

    /**
     * Request scope for a target package (e.g., com.google.android.youtube).
     * Triggers LSPosed to display its native Scope Request notification to the user.
     */
    public static void requestScope(Context context, String packageName, ScopeCallback callback) {
        if (packageName == null) return;

        if (sService != null) {
            try {
                Log.d(TAG, "Requesting LSPosed scope for: " + packageName);
                sService.requestScope(Collections.singletonList(packageName), new XposedService.OnScopeEventListener() {
                    @Override
                    public void onScopeRequestApproved(List<String> packages) {
                        Log.d(TAG, "LSPosed Scope Approved: " + packages);
                        sHandler.post(() -> {
                            if (callback != null) callback.onResult(true);
                            for (ScopeListener l : sScopeListeners) {
                                l.onScopeStatusChanged(packageName, true);
                            }
                        });
                    }

                    @Override
                    public void onScopeRequestFailed(String reason) {
                        Log.w(TAG, "LSPosed Scope Request Failed/Denied: " + reason);
                        sHandler.post(() -> {
                            if (callback != null) callback.onResult(false);
                            for (ScopeListener l : sScopeListeners) {
                                l.onScopeStatusChanged(packageName, false);
                            }
                        });
                    }
                });
                return;
            } catch (Throwable t) {
                Log.e(TAG, "Error invoking LSPosed requestScope: " + t.getMessage(), t);
            }
        }

        if (callback != null) {
            sHandler.post(() -> callback.onResult(false));
        }
    }

    /**
     * Remove package from LSPosed scope when switch is turned off.
     */
    public static void removeScope(String packageName) {
        if (sService != null && packageName != null) {
            try {
                Log.d(TAG, "Removing package from LSPosed scope: " + packageName);
                sService.removeScope(Collections.singletonList(packageName));
                for (ScopeListener l : sScopeListeners) {
                    l.onScopeStatusChanged(packageName, false);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error removing scope: " + t.getMessage(), t);
            }
        }
    }
}
