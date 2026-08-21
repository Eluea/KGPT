package tn.eluea.kgpt.features.downloader.core;

import android.content.Context;
import android.util.Log;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloaderEngine {
    private static final String TAG = "KGPT_DownloaderEngine";
    private static DownloaderEngine instance;

    private boolean initialized = false;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public interface InfoCallback {
        void onSuccess(VideoInfo info);
        void onError(Exception e);
    }

    public interface ProgressListener {
        void onProgressUpdate(float progress, long etaInSeconds, String line);
        void onComplete(File downloadedFile);
        void onError(Exception e);
    }

    public static synchronized DownloaderEngine getInstance() {
        if (instance == null) {
            instance = new DownloaderEngine();
        }
        return instance;
    }

    private DownloaderEngine() {}

    /**
     * Initialize the yt-dlp and FFmpeg native engines.
     */
    public synchronized void init(Context context) {
        if (initialized) {
            return;
        }
        try {
            YoutubeDL.getInstance().init(context.getApplicationContext());
            FFmpeg.getInstance().init(context.getApplicationContext());
            initialized = true;
            DownloaderPrefs.setCoreInstalled(context, true);
            Log.d(TAG, "DownloaderEngine & FFmpeg initialized successfully");

            // Auto-check for yt-dlp core updates in background
            executor.execute(() -> {
                try {
                    long lastCheck = DownloaderPrefs.getLastCoreCheckTimestamp(context);
                    long now = System.currentTimeMillis();
                    if (now - lastCheck > 24 * 60 * 60 * 1000L) { // Check once a day
                        DownloaderPrefs.setLastCoreCheckTimestamp(context, now);
                        YoutubeDL.getInstance().updateYoutubeDL(context.getApplicationContext(), YoutubeDL.UpdateChannel._STABLE);
                        Log.d(TAG, "yt-dlp auto-updated to latest stable release");
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Auto-update yt-dlp check skipped: " + t.getMessage());
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize DownloaderEngine", t);
        }
    }

    public boolean isCoreInstalled(Context context) {
        if (initialized) {
            return true;
        }
        if (DownloaderPrefs.isCoreInstalled(context)) {
            return true;
        }
        try {
            File filesDir = context.getApplicationContext().getFilesDir();
            File noBackupDir = context.getApplicationContext().getNoBackupFilesDir();
            File ytdl1 = new File(filesDir, "youtubedl-android");
            File ytdl2 = new File(noBackupDir, "youtubedl-android");
            File python1 = new File(filesDir, "packages/python");
            File python2 = new File(noBackupDir, "packages/python");
            if (ytdl1.exists() || ytdl2.exists() || python1.exists() || python2.exists()) {
                DownloaderPrefs.setCoreInstalled(context, true);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Asynchronously fetch media metadata and available formats.
     */
    public void fetchVideoInfo(Context context, String url, InfoCallback callback) {
        executor.execute(() -> {
            try {
                init(context);
                String cleanUrl = MediaUtils.extractUrl(url);
                if (cleanUrl == null) {
                    throw new IllegalArgumentException("Invalid media URL");
                }
                VideoInfo info = YoutubeDL.getInstance().getInfo(cleanUrl);
                if (callback != null) {
                    callback.onSuccess(info);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching video info: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Download the media according to user-selected options.
     */
    public void executeDownload(Context context, DownloadOptions options, ProgressListener listener) {
        executor.execute(() -> {
            try {
                init(context);
                String cleanUrl = MediaUtils.extractUrl(options.getUrl());
                if (cleanUrl == null) {
                    throw new IllegalArgumentException("Invalid media URL");
                }

                File outputDir = options.getCustomDownloadDir() != null
                        ? options.getCustomDownloadDir()
                        : DownloaderPrefs.getTargetDownloadDirectory(context, options.isAudio(), options.getUploader());

                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                YoutubeDLRequest request = new YoutubeDLRequest(cleanUrl);
                
                // Core reliability & YouTube 403 / SABR bypass options
                request.addOption("--no-mtime");
                request.addOption("--no-playlist");
                request.addOption("--trim-filenames", "160");
                request.addOption("--retries", "10");
                request.addOption("--fragment-retries", "10");
                request.addOption("--http-chunk-size", "10M");
                request.addOption("--no-check-certificates");
                request.addOption("--geo-bypass");
                request.addOption("--extractor-args", "youtube:player_client=android,web");
                request.addOption("--user-agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
                request.addOption("--add-header", "Accept-Language:en-US,en;q=0.9");

                // Set output template
                String fileNamePattern = options.getCustomFileName() != null && !options.getCustomFileName().trim().isEmpty()
                        ? options.getCustomFileName().trim() + ".%(ext)s"
                        : "%(title)s.%(ext)s";
                String template = outputDir.getAbsolutePath() + "/" + fileNamePattern;
                request.addOption("-o", template);

                // Configure Video vs Audio
                if (options.isAudio()) {
                    request.addOption("-x");
                    request.addOption("--audio-format", options.getAudioFormat() != null ? options.getAudioFormat() : "mp3");
                    if (options.getAudioQuality() != null && !options.getAudioQuality().isEmpty()) {
                        request.addOption("--audio-quality", options.getAudioQuality().contains("k") ? options.getAudioQuality() : options.getAudioQuality() + "k");
                    }
                    request.addOption("-f", "ba/b");
                } else {
                    // Video Quality selector with resilient fallback
                    String quality = options.getVideoQuality();
                    if ("best".equalsIgnoreCase(quality)) {
                        request.addOption("-f", "bv*+ba/b");
                        request.addOption("-S", "res,ext:mp4:m4a");
                    } else {
                        request.addOption("-f", "bv*[height<=" + quality + "]+ba/b[height<=" + quality + "]/bv*+ba/b");
                        request.addOption("-S", "res:" + quality + ",ext:mp4:m4a");
                    }
                    String format = options.getVideoFormat() != null ? options.getVideoFormat().toLowerCase() : "mp4";
                    request.addOption("--merge-output-format", format);
                }

                if (options.isEmbedThumbnail()) {
                    request.addOption("--embed-thumbnail");
                }
                if (options.isEmbedMetadata()) {
                    request.addOption("--add-metadata");
                }
                if (options.isEmbedSubtitles()) {
                    request.addOption("--embed-subs");
                    request.addOption("--sub-langs", "all,-live_chat");
                }
                if (options.isSplitChapters()) {
                    request.addOption("--split-chapters");
                }

                // Add Cut / Trim section if specified
                if (options.getDownloadSections() != null && !options.getDownloadSections().trim().isEmpty()) {
                    request.addOption("--download-sections", options.getDownloadSections().trim());
                    request.addOption("--force-keyframes-at-cuts");
                }

                // Multi-threaded Downloading
                int fragments = options.getConcurrentFragments() > 0 ? options.getConcurrentFragments() : 8;
                request.addOption("-N", String.valueOf(fragments));
                request.addOption("--concurrent-fragments", String.valueOf(fragments));

                String processId = options.getProcessId() != null ? options.getProcessId() : "dl_" + System.currentTimeMillis();
                options.setProcessId(processId);

                // Execute download with live progress
                YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                        request,
                        processId,
                        (progress, etaInSeconds, line) -> {
                            if (listener != null) {
                                listener.onProgressUpdate(
                                        progress != null ? progress : 0f,
                                        etaInSeconds != null ? etaInSeconds : 0L,
                                        line != null ? line : ""
                                );
                            }
                            return kotlin.Unit.INSTANCE;
                        }
                );

                Log.d(TAG, "Download finished successfully: " + response.getOut());

                // Scan the output directory for actual media files (strictly ignoring thumbnails, part files, etc.)
                File[] files = outputDir.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    if (lower.endsWith(".part") || lower.endsWith(".temp") || lower.endsWith(".ytdl") || lower.endsWith(".webp") || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".json")) {
                        return false;
                    }
                    if (options.isAudio()) {
                        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".flac") || lower.endsWith(".opus") || lower.endsWith(".wav") || lower.endsWith(".aac");
                    } else {
                        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".avi");
                    }
                });

                File latestFile = null;
                long latestTime = 0;
                if (files != null) {
                    for (File f : files) {
                        if (f.lastModified() > latestTime) {
                            latestTime = f.lastModified();
                            latestFile = f;
                        }
                    }
                }

                if (latestFile != null) {
                    MediaUtils.scanMediaFile(context, latestFile, options.isAudio() ? "audio/*" : "video/*");
                }

                if (listener != null) {
                    listener.onComplete(latestFile);
                }

            } catch (Exception e) {
                Log.e(TAG, "Download execution failed", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * Cancel an active download and delete any partial/temporary files.
     */
    public void cancelDownload(String processId, File targetDir) {
        executor.execute(() -> {
            try {
                if (processId != null) {
                    YoutubeDL.getInstance().destroyProcessById(processId);
                    Log.d(TAG, "Destroyed process: " + processId);
                }

                // Delete partial downloaded files
                if (targetDir != null && targetDir.exists()) {
                    File[] partials = targetDir.listFiles((dir, name) ->
                            name.endsWith(".part") || name.endsWith(".temp") || name.endsWith(".ytdl") || name.endsWith(".part-Frag")
                    );
                    if (partials != null) {
                        for (File p : partials) {
                            boolean deleted = p.delete();
                            Log.d(TAG, "Deleted partial file on cancel: " + p.getName() + " -> " + deleted);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error cancelling download: " + t.getMessage());
            }
        });
    }

    /**
     * Update yt-dlp core extractor scripts from internet.
     */
    public void updateCore(Context context, YoutubeDL.UpdateChannel channel) {
        init(context);
        executor.execute(() -> {
            try {
                YoutubeDL.getInstance().updateYoutubeDL(context, channel);
                Log.d(TAG, "yt-dlp core updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to update yt-dlp core", e);
            }
        });
    }
}
