package tn.eluea.kgpt.features.downloader.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloadOptions;
import tn.eluea.kgpt.features.downloader.core.DownloaderEngine;
import tn.eluea.kgpt.features.downloader.core.DownloaderPrefs;
import tn.eluea.kgpt.features.downloader.core.MediaUtils;

public class MediaDownloadService extends Service {
    private static final String TAG = "KGPT_DownloadService";
    private static final String CHANNEL_ID = "kgpt_media_downloader_channel";
    private static final int NOTIFICATION_ID = 4001;

    public static final String ACTION_START_DOWNLOAD = "tn.eluea.kgpt.downloader.START_DOWNLOAD";
    public static final String ACTION_CANCEL_DOWNLOAD = "tn.eluea.kgpt.downloader.CANCEL_DOWNLOAD";
    public static final String EXTRA_OPTIONS = "extra_download_options";
    public static final String EXTRA_MEDIA_TITLE = "extra_media_title";

    private NotificationManager notificationManager;
    private String currentProcessId = null;
    private File currentOutputDir = null;

    public static void startDownload(Context context, DownloadOptions options, String mediaTitle) {
        Intent intent = new Intent(context, MediaDownloadService.class);
        intent.setAction(ACTION_START_DOWNLOAD);
        intent.putExtra(EXTRA_OPTIONS, options);
        intent.putExtra(EXTRA_MEDIA_TITLE, mediaTitle != null ? mediaTitle : "جاري تنزيل الوسائط...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "تنزيلات الوسائط (Media Downloads)",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("إشعارات تقدم تنزيل الفيديوهات والصوتيات");
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setShowBadge(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public interface DownloadEventListener {
        void onProgress(String processId, float progress, long etaInSeconds, String line);
        void onComplete(String processId, File downloadedFile);
        void onError(String processId, Exception e);
    }

    private static DownloadEventListener activeEventListener;

    public static void setDownloadEventListener(DownloadEventListener listener) {
        activeEventListener = listener;
    }

    public static void cancelDownload(Context context) {
        Intent intent = new Intent(context, MediaDownloadService.class);
        intent.setAction(ACTION_CANCEL_DOWNLOAD);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_CANCEL_DOWNLOAD.equals(action)) {
            cancelActiveDownload();
            return START_NOT_STICKY;
        }

        if (ACTION_START_DOWNLOAD.equals(action)) {
            DownloadOptions options = (DownloadOptions) intent.getSerializableExtra(EXTRA_OPTIONS);
            String title = intent.getStringExtra(EXTRA_MEDIA_TITLE);
            if (title == null) title = "وسائط KGPT";

            if (options != null) {
                currentProcessId = options.getProcessId() != null ? options.getProcessId() : "dl_" + System.currentTimeMillis();
                options.setProcessId(currentProcessId);
                currentOutputDir = options.getCustomDownloadDir() != null
                        ? options.getCustomDownloadDir()
                        : DownloaderPrefs.getTargetDownloadDirectory(this, options.isAudio(), options.getUploader());

                startForeground(NOTIFICATION_ID, buildProgressNotification(title, 0, "جاري بدء التنزيل..."));
                executeDownloadTask(options, title);
            }
        }
        return START_NOT_STICKY;
    }

    private void cancelActiveDownload() {
        Log.d(TAG, "Cancelling active download process: " + currentProcessId);
        DownloaderEngine.getInstance().cancelDownload(currentProcessId, currentOutputDir);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
        if (activeEventListener != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (activeEventListener != null) {
                    activeEventListener.onError(currentProcessId, new Exception("Download cancelled"));
                }
            });
        }
        stopForeground(true);
        stopSelf();
    }

    private void executeDownloadTask(DownloadOptions options, String mediaTitle) {
        DownloaderEngine.getInstance().executeDownload(
                this,
                options,
                new DownloaderEngine.ProgressListener() {
                    private long lastUpdate = 0;

                    @Override
                    public void onProgressUpdate(float progress, long etaInSeconds, String line) {
                        long now = System.currentTimeMillis();
                        if (now - lastUpdate > 300) { // Fast smooth updates
                            lastUpdate = now;
                            String etaStr = etaInSeconds > 0 ? " • متبقي: " + MediaUtils.formatDuration(etaInSeconds) : "";
                            String stats = String.format(java.util.Locale.US, "%.1f%%%s", progress, etaStr);
                            notificationManager.notify(
                                    NOTIFICATION_ID,
                                    buildProgressNotification(mediaTitle, (int) progress, stats)
                            );
                        }

                        if (activeEventListener != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                if (activeEventListener != null) {
                                    activeEventListener.onProgress(currentProcessId, progress, etaInSeconds, line);
                                }
                            });
                        }
                    }

                    @Override
                    public void onComplete(File downloadedFile) {
                        showCompleteNotification(mediaTitle, downloadedFile);
                        if (activeEventListener != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                if (activeEventListener != null) {
                                    activeEventListener.onComplete(currentProcessId, downloadedFile);
                                }
                            });
                        }
                        stopForeground(false);
                        stopSelf();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Download error in service", e);
                        showErrorNotification(mediaTitle, e.getMessage());
                        if (activeEventListener != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                if (activeEventListener != null) {
                                    activeEventListener.onError(currentProcessId, e);
                                }
                            });
                        }
                        stopForeground(false);
                        stopSelf();
                    }
                }
        );
    }

    private String getShortTitle(String title) {
        if (title == null) return "KGPT Download";
        if (title.length() > 28) {
            return title.substring(0, 25) + "...";
        }
        return title;
    }

    private Notification buildProgressNotification(String title, int progress, String subText) {
        Intent cancelIntent = new Intent(this, MediaDownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL_DOWNLOAD);
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this,
                1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download_filled)
                .setContentTitle(getShortTitle(title))
                .setContentText(subText)
                .setProgress(100, progress, progress <= 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_close, getString(R.string.btn_dismiss), cancelPendingIntent);

        return builder.build();
    }

    private void showCompleteNotification(String title, File file) {
        PendingIntent contentIntent = null;
        if (file != null && file.exists()) {
            try {
                Uri fileUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        file
                );
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(fileUri, file.getName().endsWith(".mp3") || file.getName().endsWith(".m4a") || file.getName().endsWith(".flac") ? "audio/*" : "video/*");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                contentIntent = PendingIntent.getActivity(
                        this,
                        0,
                        viewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
            } catch (Throwable t) {
                Log.e(TAG, "Failed to create file open intent", t);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield_tick_filled)
                .setContentTitle(getString(R.string.notification_download_complete))
                .setContentText(getShortTitle(title))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent);
            builder.addAction(R.drawable.ic_check_circle_filled, getString(R.string.btn_open), contentIntent);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void showErrorNotification(String title, String errorMsg) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield_cross_filled)
                .setContentTitle(getString(R.string.notification_download_failed))
                .setContentText(getShortTitle(title) + ": " + (errorMsg != null ? errorMsg : "Error"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
