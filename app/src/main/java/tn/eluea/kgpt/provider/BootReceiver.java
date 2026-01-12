/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.provider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

/**
 * Receiver that starts on boot to ensure ConfigProvider is available
 * and config file is synced for the Xposed module running in keyboard process.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "KGPT_BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            
            // Query the provider to trigger its initialization and sync
            try {
                Cursor cursor = context.getContentResolver().query(
                        ConfigProvider.CONTENT_URI, null, null, null, null);
                if (cursor != null) {
                    int count = cursor.getCount();
                    cursor.close();
                    Log.d(TAG, "Provider initialized with " + count + " entries");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to query provider", e);
            }
            
            // Also try to read app triggers status
            try {
                ConfigClient client = new ConfigClient(context);
                boolean enabled = client.getBoolean("app_triggers_enabled", false);
                String triggers = client.getString("app_triggers", null);
                Log.d(TAG, "app_triggers_enabled = " + enabled);
                Log.d(TAG, "app_triggers = " + (triggers != null ? triggers.length() + " chars" : "null"));
            } catch (Exception e) {
                Log.e(TAG, "Failed to read config", e);
            }
        }
    }
}
