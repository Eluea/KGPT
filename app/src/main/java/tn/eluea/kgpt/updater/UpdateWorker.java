/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package tn.eluea.kgpt.updater;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

import tn.eluea.kgpt.SPManager;

/**
 * Background worker to periodically check for updates.
 * Uses WorkManager for reliable background scheduling.
 */
public class UpdateWorker extends Worker {

    private static final String TAG = "KGPT_UpdateWorker";
    private static final String WORK_NAME = "kgpt_update_check";
    private static final String PREFS_NAME = "kgpt_update_prefs";
    private static final String PREF_LAST_CHECK = "last_update_check";
    private static final String PREF_AVAILABLE_UPDATE = "available_update_version";
    private static final String PREF_AVAILABLE_UPDATE_URL = "available_update_url";
    private static final String PREF_AVAILABLE_UPDATE_CHANGELOG = "available_update_changelog";
    private static final String PREF_AVAILABLE_UPDATE_CHECKSUM = "available_update_checksum";

    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting update check work");

        try {
            // Check if updates are enabled
            if (!isUpdateCheckEnabled()) {
                Log.d(TAG, "Update checking is disabled");
                return Result.success();
            }

            // Perform update check
            UpdateChecker checker = new UpdateChecker(getApplicationContext());
            UpdateInfo updateInfo = checker.checkForUpdate();

            if (updateInfo != null) {
                Log.i(TAG, "New update found: " + updateInfo.getVersionName());
                saveAvailableUpdate(updateInfo);
            } else {
                Log.d(TAG, "No updates available");
                clearAvailableUpdate();
            }

            // Save last check time
            saveLastCheckTime();

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Update check failed", e);
            return Result.retry();
        }
    }

    /**
     * Check if update checking is enabled in settings
     */
    private boolean isUpdateCheckEnabled() {
        if (SPManager.isReady()) {
            return SPManager.getInstance().getUpdateCheckEnabled();
        }
        return true; // Default enabled
    }

    /**
     * Save available update info to SharedPreferences
     */
    private void saveAvailableUpdate(UpdateInfo info) {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putString(PREF_AVAILABLE_UPDATE, info.getVersionName())
                .putString(PREF_AVAILABLE_UPDATE_URL, info.getDownloadUrl())
                .putString(PREF_AVAILABLE_UPDATE_CHANGELOG, info.getChangelog())
                .putString(PREF_AVAILABLE_UPDATE_CHECKSUM, info.getChecksum())
                .apply();
    }

    /**
     * Clear available update info
     */
    private void clearAvailableUpdate() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .remove(PREF_AVAILABLE_UPDATE)
                .remove(PREF_AVAILABLE_UPDATE_URL)
                .remove(PREF_AVAILABLE_UPDATE_CHANGELOG)
                .remove(PREF_AVAILABLE_UPDATE_CHECKSUM)
                .apply();
    }

    /**
     * Save last check time
     */
    private void saveLastCheckTime() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        prefs.edit()
                .putLong(PREF_LAST_CHECK, System.currentTimeMillis())
                .apply();
    }

    // ============ Static Methods for External Access ============

    /**
     * Schedule periodic update checks
     * 
     * @param context       Application context
     * @param intervalHours Interval in hours (e.g., 12, 24, 48)
     */
    public static void scheduleUpdateCheck(Context context, int intervalHours) {
        Log.i(TAG, "Scheduling update check every " + intervalHours + " hours");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                UpdateWorker.class,
                intervalHours,
                TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS) // First check after 1 hour
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest);
    }

    /**
     * Cancel scheduled update checks
     */
    public static void cancelUpdateCheck(Context context) {
        Log.i(TAG, "Cancelling scheduled update checks");
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    /**
     * Check if there's a cached available update
     */
    public static UpdateInfo getCachedUpdate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String version = prefs.getString(PREF_AVAILABLE_UPDATE, null);
        if (version == null) {
            return null;
        }

        String url = prefs.getString(PREF_AVAILABLE_UPDATE_URL, null);
        String changelog = prefs.getString(PREF_AVAILABLE_UPDATE_CHANGELOG, "");
        String checksum = prefs.getString(PREF_AVAILABLE_UPDATE_CHECKSUM, null);

        return new UpdateInfo(version, 0, changelog, url, checksum, "", 0);
    }

    /**
     * Get last update check time
     */
    public static long getLastCheckTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(PREF_LAST_CHECK, 0);
    }

    /**
     * Format last check time for display
     */
    public static String getLastCheckTimeFormatted(Context context) {
        long lastCheck = getLastCheckTime(context);
        if (lastCheck == 0) {
            return "Never";
        }

        long diff = System.currentTimeMillis() - lastCheck;
        long hours = diff / (1000 * 60 * 60);
        long minutes = (diff / (1000 * 60)) % 60;

        if (hours > 24) {
            long days = hours / 24;
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    /**
     * Clear cached update (after user dismisses or installs)
     */
    public static void clearCachedUpdate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(PREF_AVAILABLE_UPDATE)
                .remove(PREF_AVAILABLE_UPDATE_URL)
                .remove(PREF_AVAILABLE_UPDATE_CHANGELOG)
                .remove(PREF_AVAILABLE_UPDATE_CHECKSUM)
                .apply();
    }
}
