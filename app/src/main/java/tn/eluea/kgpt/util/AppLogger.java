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
package tn.eluea.kgpt.util;

import android.util.Log;

/**
 * Simple logger that works in both app context and Xposed context.
 * Uses Android Log API which is always available.
 */
public class AppLogger {
    private static final String TAG = "KGPT";
    
    public static void log(String message) {
        Log.d(TAG, message);
    }
    
    public static void log(String tag, String message) {
        Log.d(tag, message);
    }
    
    public static void log(Throwable t) {
        Log.e(TAG, "Error", t);
    }
    
    public static void logError(String message) {
        Log.e(TAG, message);
    }
    
    public static void logError(String message, Throwable t) {
        Log.e(TAG, message, t);
    }
    
    public static void logWarning(String message) {
        Log.w(TAG, message);
    }
    
    public static void logInfo(String message) {
        Log.i(TAG, message);
    }
    
    public static void logVerbose(String message) {
        Log.v(TAG, message);
    }
}
