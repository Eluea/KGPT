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

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

import tn.eluea.kgpt.SPManager;

/**
 * Downloads APK updates using Android's DownloadManager.
 * Provides progress tracking and handles installation.
 */
public class UpdateDownloader {

    private static final String TAG = "KGPT_UpdateDownloader";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private final Context context;
    private final DownloadManager downloadManager;
    private final Handler mainHandler;

    private long currentDownloadId = -1;
    private DownloadProgressListener progressListener;
    private BroadcastReceiver downloadReceiver;
    private boolean isMonitoring = false;

    public UpdateDownloader(Context context) {
        this.context = context.getApplicationContext();
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Start downloading an update.
     * 
     * @param updateInfo The update information containing download URL
     * @param listener   Callback for download progress and completion
     * @return download ID or -1 if failed
     */
    public long startDownload(UpdateInfo updateInfo, DownloadProgressListener listener) {
        this.progressListener = listener;

        // Security check: HTTPS only
        if (!updateInfo.isSecureDownload()) {
            Log.e(TAG, "Security: Refusing to download from non-HTTPS URL");
            if (listener != null) {
                listener.onDownloadFailed("Security error: Download URL must be HTTPS");
            }
            return -1;
        }

        try {
            Uri downloadUri = Uri.parse(updateInfo.getDownloadUrl());

            // Create download request
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);

            // Configure request
            request.setTitle("KGPT Update " + updateInfo.getVersionName());
            request.setDescription("Downloading update...");
            request.setMimeType(APK_MIME_TYPE);

            // Allow download over mobile and wifi
            request.setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI |
                            DownloadManager.Request.NETWORK_MOBILE);

            // Show in notification bar
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

            // Set destination
            String fileName = "KGPT-" + updateInfo.getCleanVersionName() + ".apk";
            
            // Check for custom download path
            String customPath = "";
            if (SPManager.isReady()) {
                customPath = SPManager.getInstance().getUpdateDownloadPath();
            }
            
            if (customPath != null && !customPath.isEmpty()) {
                // Use custom path
                File customDir = new File(customPath);
                if (!customDir.exists()) {
                    customDir.mkdirs();
                }
                File destFile = new File(customDir, fileName);
                request.setDestinationUri(Uri.fromFile(destFile));
            } else {
                // Use default Downloads folder
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            }

            // Enqueue download
            currentDownloadId = downloadManager.enqueue(request);
            Log.i(TAG, "Download started with ID: " + currentDownloadId);

            // Register completion receiver
            registerDownloadReceiver(updateInfo);

            // Start progress monitoring
            startProgressMonitoring();

            return currentDownloadId;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start download", e);
            if (listener != null) {
                listener.onDownloadFailed("Failed to start download: " + e.getMessage());
            }
            return -1;
        }
    }

    /**
     * Cancel ongoing download
     */
    public void cancelDownload() {
        if (currentDownloadId != -1) {
            downloadManager.remove(currentDownloadId);
            Log.i(TAG, "Download cancelled: " + currentDownloadId);
            currentDownloadId = -1;
        }
        stopProgressMonitoring();
        unregisterDownloadReceiver();
    }

    /**
     * Register receiver for download completion
     */
    private void registerDownloadReceiver(UpdateInfo updateInfo) {
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == currentDownloadId) {
                    handleDownloadComplete(updateInfo);
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(downloadReceiver, filter);
        }
    }

    /**
     * Unregister download completion receiver
     */
    private void unregisterDownloadReceiver() {
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {
            }
            downloadReceiver = null;
        }
    }

    /**
     * Handle download completion
     */
    private void handleDownloadComplete(UpdateInfo updateInfo) {
        stopProgressMonitoring();

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(currentDownloadId);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(statusIndex);

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    String localUri = cursor.getString(uriIndex);

                    Log.i(TAG, "Download completed: " + localUri);

                    // Verify the downloaded file
                    File apkFile = new File(Uri.parse(localUri).getPath());
                    verifyAndNotify(apkFile, updateInfo);

                } else {
                    int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                    int reason = cursor.getInt(reasonIndex);
                    String errorMessage = getDownloadErrorMessage(reason);

                    Log.e(TAG, "Download failed: " + errorMessage);
                    if (progressListener != null) {
                        mainHandler.post(() -> progressListener.onDownloadFailed(errorMessage));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling download completion", e);
            if (progressListener != null) {
                mainHandler.post(() -> progressListener.onDownloadFailed("Error: " + e.getMessage()));
            }
        }

        unregisterDownloadReceiver();
    }

    /**
     * Verify downloaded APK and notify listener
     */
    private void verifyAndNotify(File apkFile, UpdateInfo updateInfo) {
        // Verify security
        UpdateSecurityVerifier verifier = new UpdateSecurityVerifier(context);
        UpdateSecurityVerifier.VerificationResult result = verifier.verifyApk(
                apkFile,
                updateInfo.getChecksum());

        if (result.success) {
            Log.i(TAG, "APK verification successful");
            if (progressListener != null) {
                mainHandler.post(() -> progressListener.onDownloadComplete(apkFile));
            }
        } else {
            Log.e(TAG, "APK verification failed: " + result.message);
            // Delete the suspicious file
            apkFile.delete();
            if (progressListener != null) {
                mainHandler.post(() -> progressListener.onDownloadFailed(
                        "Security verification failed: " + result.message));
            }
        }
    }

    /**
     * Start monitoring download progress
     */
    private void startProgressMonitoring() {
        isMonitoring = true;

        new Thread(() -> {
            while (isMonitoring && currentDownloadId != -1) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(currentDownloadId);

                try (Cursor cursor = downloadManager.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int bytesDownloadedIndex = cursor.getColumnIndex(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalIndex = cursor.getColumnIndex(
                                DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        int statusIndex = cursor.getColumnIndex(
                                DownloadManager.COLUMN_STATUS);

                        long bytesDownloaded = cursor.getLong(bytesDownloadedIndex);
                        long bytesTotal = cursor.getLong(bytesTotalIndex);
                        int status = cursor.getInt(statusIndex);

                        if (status == DownloadManager.STATUS_RUNNING && bytesTotal > 0) {
                            int progress = (int) ((bytesDownloaded * 100) / bytesTotal);

                            if (progressListener != null) {
                                mainHandler.post(() -> progressListener.onProgressUpdate(
                                        progress, bytesDownloaded, bytesTotal));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error monitoring progress", e);
                }

                try {
                    Thread.sleep(500); // Update every 500ms
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }).start();
    }

    /**
     * Stop monitoring download progress
     */
    private void stopProgressMonitoring() {
        isMonitoring = false;
    }

    /**
     * Trigger APK installation (shows system install dialog)
     */
    public void installApk(File apkFile) {
        try {
            Log.i(TAG, "Installing APK: " + apkFile.getAbsolutePath());
            Log.i(TAG, "File exists: " + apkFile.exists() + ", size: " + apkFile.length());
            
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist!");
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    apkUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            apkFile);
                    Log.i(TAG, "FileProvider URI: " + apkUri);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "FileProvider failed, trying direct URI", e);
                    // Fallback: try using the file URI directly with proper permissions
                    apkUri = Uri.fromFile(apkFile);
                }
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, APK_MIME_TYPE);
            
            // Verify there's an app to handle this intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                Log.i(TAG, "Installation intent launched successfully");
            } else {
                Log.e(TAG, "No app found to handle APK installation");
                // Try alternative approach using ACTION_INSTALL_PACKAGE
                Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installIntent.setData(apkUri);
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(installIntent);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch installation", e);
            e.printStackTrace();
        }
    }

    /**
     * Get human-readable error message for download failure
     */
    private String getDownloadErrorMessage(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "Cannot resume download";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "Storage not found";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "File already exists";
            case DownloadManager.ERROR_FILE_ERROR:
                return "Storage error";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "Network data error";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "Insufficient storage space";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "Too many redirects";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "Server error";
            case DownloadManager.ERROR_UNKNOWN:
            default:
                return "Unknown error";
        }
    }

    /**
     * Listener interface for download progress
     */
    public interface DownloadProgressListener {
        void onProgressUpdate(int progress, long bytesDownloaded, long totalBytes);

        void onDownloadComplete(File apkFile);

        void onDownloadFailed(String error);
    }
}
