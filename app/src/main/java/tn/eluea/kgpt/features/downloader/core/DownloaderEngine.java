/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kotlin.Unit;

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
     * Initialize the yt-dlp and FFmpeg native engines with on-demand runtime support.
     */
    public synchronized void init(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        try {
            File nativeDir = new File(appContext.getApplicationInfo().nativeLibraryDir);
            File coreDir = new File(appContext.getFilesDir(), "youtubedl-core");
            File ytdlDir = new File(appContext.getNoBackupFilesDir(), "youtubedl-android");
            File packagesDir = new File(ytdlDir, "packages");
            File pythonDir = new File(packagesDir, "python");
            File ffmpegDir = new File(packagesDir, "ffmpeg");
            File ytdlpDir = new File(ytdlDir, "yt-dlp");

            if (!ytdlDir.exists()) ytdlDir.mkdirs();
            if (!packagesDir.exists()) packagesDir.mkdirs();
            if (!pythonDir.exists()) pythonDir.mkdirs();
            if (!ffmpegDir.exists()) ffmpegDir.mkdirs();
            if (!ytdlpDir.exists()) ytdlpDir.mkdirs();

            // Extract inner zip packages if not already extracted
            File pythonZip = new File(coreDir, "libpython.zip.so");
            if (pythonZip.exists() && (!pythonDir.exists() || pythonDir.list() == null || pythonDir.list().length == 0)) {
                try {
                    extractZip(pythonZip, pythonDir);
                } catch (Throwable t) {
                    Log.w(TAG, "Unzipping python package: " + t.getMessage());
                }
            }

            File ffmpegZip = new File(coreDir, "libffmpeg.zip.so");
            if (ffmpegZip.exists() && (!ffmpegDir.exists() || ffmpegDir.list() == null || ffmpegDir.list().length == 0)) {
                try {
                    extractZip(ffmpegZip, ffmpegDir);
                } catch (Throwable t) {
                    Log.w(TAG, "Unzipping ffmpeg package: " + t.getMessage());
                }
            }

            // Resolve ELF symlinks in python and ffmpeg packages (e.g. libz.so.1 -> libz.so.1.3.1)
            resolveSymlinksRecursively(pythonDir);
            resolveSymlinksRecursively(ffmpegDir);

            // Grant executable permissions
            setPermissionsRecursively(pythonDir);
            setPermissionsRecursively(ffmpegDir);

            // Use nativeLibraryDir as binDir if libpython.so exists there (OS executable), otherwise coreDir
            File binDir = (new File(nativeDir, "libpython.so").exists()) ? nativeDir : coreDir;

            try {
                Field binDirField = YoutubeDL.class.getDeclaredField("binDir");
                binDirField.setAccessible(true);
                binDirField.set(null, binDir);

                Field pythonPathField = YoutubeDL.class.getDeclaredField("pythonPath");
                pythonPathField.setAccessible(true);
                pythonPathField.set(null, new File(binDir, "libpython.so"));

                Field ffmpegPathField = YoutubeDL.class.getDeclaredField("ffmpegPath");
                ffmpegPathField.setAccessible(true);
                ffmpegPathField.set(null, new File(binDir, "libffmpeg.so"));

                Field qjsField = YoutubeDL.class.getDeclaredField("quickJsPath");
                qjsField.setAccessible(true);
                qjsField.set(null, new File(binDir, "libqjs.so"));

                Field ytdlpField = YoutubeDL.class.getDeclaredField("ytdlpPath");
                ytdlpField.setAccessible(true);
                ytdlpField.set(null, new File(ytdlpDir, "yt-dlp"));

                Field ldPathField = YoutubeDL.class.getDeclaredField("ENV_LD_LIBRARY_PATH");
                ldPathField.setAccessible(true);
                String ldPath = pythonDir.getAbsolutePath() + "/usr/lib:" + ffmpegDir.getAbsolutePath() + "/usr/lib:" + binDir.getAbsolutePath();
                ldPathField.set(null, ldPath);

                Field sslCertField = YoutubeDL.class.getDeclaredField("ENV_SSL_CERT_FILE");
                sslCertField.setAccessible(true);
                sslCertField.set(null, pythonDir.getAbsolutePath() + "/usr/etc/tls/cert.pem");

                Field pythonHomeField = YoutubeDL.class.getDeclaredField("ENV_PYTHONHOME");
                pythonHomeField.setAccessible(true);
                pythonHomeField.set(null, pythonDir.getAbsolutePath() + "/usr");

                Field tmpDirField = YoutubeDL.class.getDeclaredField("TMPDIR");
                tmpDirField.setAccessible(true);
                tmpDirField.set(null, appContext.getCacheDir().getAbsolutePath());

                // Ensure raw yt-dlp binary is extracted into ytdlpDir
                File ytdlpFile = new File(ytdlpDir, "yt-dlp");
                if (!ytdlpFile.exists()) {
                    try (InputStream in = appContext.getResources().openRawResource(com.yausername.youtubedl_android.R.raw.ytdlp);
                         FileOutputStream fos = new FileOutputStream(ytdlpFile)) {
                        byte[] buf = new byte[16384];
                        int len;
                        while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
                    } catch (Throwable ignored) {}
                }

                Field initField = YoutubeDL.class.getDeclaredField("initialized");
                initField.setAccessible(true);
                initField.set(null, true);
            } catch (Throwable t) {
                Log.w(TAG, "Configuring YoutubeDL reflection: " + t.getMessage());
                try {
                    YoutubeDL.getInstance().init(appContext);
                } catch (Throwable ignored) {}
            }

            try {
                Field binDirField = FFmpeg.class.getDeclaredField("binDir");
                binDirField.setAccessible(true);
                binDirField.set(null, binDir);

                Field initField = FFmpeg.class.getDeclaredField("initialized");
                initField.setAccessible(true);
                initField.set(null, true);
            } catch (Throwable t) {
                Log.w(TAG, "Configuring FFmpeg reflection: " + t.getMessage());
                try {
                    FFmpeg.getInstance().init(appContext);
                } catch (Throwable ignored) {}
            }

            initialized = true;
            DownloaderPrefs.setCoreInstalled(context, true);
            Log.d(TAG, "DownloaderEngine & FFmpeg initialized successfully with on-demand runtime");

        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize DownloaderEngine", t);
        }
    }

    public void resolveSymlinksRecursively(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                resolveSymlinksRecursively(file);
            } else if (file.isFile() && file.length() > 0 && file.length() < 256) {
                try {
                    byte[] bytes = new byte[(int) file.length()];
                    try (FileInputStream fis = new FileInputStream(file)) {
                        int read = fis.read(bytes);
                        if (read <= 0) continue;
                    }
                    String targetName = new String(bytes, "UTF-8").trim();
                    if (targetName.contains("\n") || targetName.contains("\r") || targetName.length() > 100) {
                        continue;
                    }
                    File targetFile = new File(file.getParentFile(), targetName);
                    if (targetFile.exists() && targetFile.isFile() && targetFile.length() > 256) {
                        Log.d(TAG, "Resolving symlink: " + file.getName() + " -> " + targetName);
                        file.delete();
                        try {
                            android.system.Os.symlink(targetName, file.getAbsolutePath());
                        } catch (Throwable t) {
                            try (FileInputStream in = new FileInputStream(targetFile);
                                 FileOutputStream out = new FileOutputStream(file)) {
                                byte[] buf = new byte[32768];
                                int len;
                                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                            }
                        }
                        file.setReadable(true, false);
                        file.setExecutable(true, false);
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void extractZip(File zipFile, File targetDir) throws Exception {
        byte[] buffer = new byte[32768];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    newFile.setExecutable(true, false);
                    newFile.setReadable(true, false);
                }
                zis.closeEntry();
            }
        }
        resolveSymlinksRecursively(targetDir);
    }

    private void setPermissionsRecursively(File file) {
        if (file == null || !file.exists()) return;
        file.setReadable(true, false);
        file.setExecutable(true, false);
        file.setWritable(true, true);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    setPermissionsRecursively(child);
                }
            }
        }
    }

    public boolean isCoreInstalled(Context context) {
        if (context == null) return false;
        try {
            File noBackupDir = context.getApplicationContext().getNoBackupFilesDir();
            File packagesDir = new File(noBackupDir, "youtubedl-android/packages");
            File pythonDir = new File(packagesDir, "python");
            File ffmpegDir = new File(packagesDir, "ffmpeg");

            boolean hasPython = pythonDir.exists() && pythonDir.isDirectory()
                    && pythonDir.list() != null && pythonDir.list().length > 0;
            boolean hasFfmpeg = ffmpegDir.exists() && ffmpegDir.isDirectory()
                    && ffmpegDir.list() != null && ffmpegDir.list().length > 0;

            if (hasPython && hasFfmpeg) {
                DownloaderPrefs.setCoreInstalled(context, true);
                return true;
            }
        } catch (Exception ignored) {}
        DownloaderPrefs.setCoreInstalled(context, false);
        return false;
    }

    public void resetInstallationState(Context context) {
        DownloaderPrefs.setCoreInstalled(context, false);
        initialized = false;
    }

    /**
     * Clear all thumbnail, temporary and cache files from downloader.
     */
    public void clearCache(Context context) {
        ThumbnailLoader.getInstance().clearCache();
        try {
            if (context != null) {
                File cacheDir = context.getCacheDir();
                if (cacheDir != null && cacheDir.exists()) {
                    File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("kgpt_") || name.startsWith("ytdl_") || name.endsWith(".tmp") || name.endsWith(".part"));
                    if (files != null) {
                        for (File f : files) {
                            try { f.delete(); } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Fetch media details asynchronously with metadata & available formats.
     */
    public void fetchVideoInfo(Context context, String url, InfoCallback callback) {
        init(context);
        clearCache(context);
        executor.execute(() -> {
            try {
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--no-playlist");
                request.addOption("--no-cache-dir");
                request.addOption("--no-check-certificates");
                request.addOption("--geo-bypass");
                request.addOption("--ignore-no-formats-error");
                request.addOption("--no-update");
                request.addOption("--extractor-args", "youtube:player_client=android_music,android,web,mweb");

                VideoInfo info = YoutubeDL.getInstance().getInfo(request);
                callback.onSuccess(info);
            } catch (Exception e) {
                Log.w(TAG, "Initial fetch failed, checking yt-dlp update: " + e.getMessage());
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(context.getApplicationContext(), YoutubeDL.UpdateChannel._STABLE);
                    YoutubeDLRequest retryReq = new YoutubeDLRequest(url);
                    retryReq.addOption("--no-playlist");
                    retryReq.addOption("--no-cache-dir");
                    retryReq.addOption("--no-check-certificates");
                    retryReq.addOption("--geo-bypass");
                    retryReq.addOption("--ignore-no-formats-error");
                    retryReq.addOption("--no-update");
                    retryReq.addOption("--extractor-args", "youtube:player_client=android_music,android,web,mweb");
                    VideoInfo info = YoutubeDL.getInstance().getInfo(retryReq);
                    callback.onSuccess(info);
                } catch (Throwable t) {
                    Log.e(TAG, "Error fetching video info after update: " + t.getMessage(), t);
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Execute download with user options and progress callbacks.
     */
    public void executeDownload(Context context, DownloadOptions options, ProgressListener listener) {
        init(context);
        executor.execute(() -> {
            String platform = MediaUtils.getPlatformName(options.getUrl());
            File outputDir = options.getCustomDownloadDir() != null
                    ? options.getCustomDownloadDir()
                    : DownloaderPrefs.getTargetDownloadDirectory(context, options.isAudio(), options.getUploader(), platform);

            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            YoutubeDLRequest request = buildRequest(context, options, outputDir);
            String processId = options.getProcessId();

            Log.d(TAG, "Executing download with command: " + request.buildCommand());

            try {
                YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                        request,
                        processId,
                        (progress, etaInSeconds, line) -> {
                            if (listener != null) {
                                listener.onProgressUpdate(progress, etaInSeconds, line);
                            }
                            return Unit.INSTANCE;
                        }
                );

                File downloadedFile = findDownloadedFile(outputDir, options.getUrl());
                if (listener != null) {
                    listener.onComplete(downloadedFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Download execution failed: " + e.getMessage(), e);
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                boolean isYouTubeRestriction = msg.contains("403") || msg.contains("sabr") || msg.contains("forbidden")
                        || msg.contains("older than 90 days") || msg.contains("sign in") || msg.contains("bot")
                        || msg.contains("unable to download video data");

                if (isYouTubeRestriction) {
                    Log.w(TAG, "Encountered YouTube restriction / 403 error. Auto-updating yt-dlp core and retrying download...");
                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(context.getApplicationContext(), YoutubeDL.UpdateChannel._STABLE);

                        YoutubeDLResponse retryResponse = YoutubeDL.getInstance().execute(
                                request,
                                processId,
                                (progress, etaInSeconds, line) -> {
                                    if (listener != null) {
                                        listener.onProgressUpdate(progress, etaInSeconds, line);
                                    }
                                    return Unit.INSTANCE;
                                }
                        );

                        File downloadedFile = findDownloadedFile(outputDir, options.getUrl());
                        if (listener != null) {
                            listener.onComplete(downloadedFile);
                        }
                        return;
                    } catch (Throwable retryEx) {
                        Log.e(TAG, "Retry download after auto-update failed: " + retryEx.getMessage(), retryEx);
                        if (listener != null) {
                            listener.onError(retryEx instanceof Exception ? (Exception) retryEx : new Exception(retryEx));
                        }
                        return;
                    }
                }

                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    public void cancelDownload(String processId, File outputDir) {
        if (processId != null) {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId);
            } catch (Exception e) {
                Log.w(TAG, "Error destroying download process: " + e.getMessage());
            }
        }
    }

    private YoutubeDLRequest buildRequest(Context context, DownloadOptions options, File outputDir) {
        YoutubeDLRequest request = new YoutubeDLRequest(options.getUrl());
        request.addOption("--no-update");
        request.addOption("--extractor-args", "youtube:player_client=android_music,android,web,mweb");

        String template = options.getCustomFileName() != null
                ? options.getCustomFileName()
                : "%(title)s";
        request.addOption("-o", outputDir.getAbsolutePath() + "/" + template + ".%(ext)s");

        int fragments = options.getConcurrentFragments();
        if (fragments > 1) {
            request.addOption("-N", String.valueOf(fragments));
        }

        if (options.isAudio()) {
            request.addOption("-x");
            String format = options.getAudioFormat() != null ? options.getAudioFormat() : DownloaderPrefs.getDefaultAudioFormat(context);
            request.addOption("--audio-format", format.toLowerCase());
            if (options.getAudioQuality() != null && !options.getAudioQuality().equals("0")) {
                request.addOption("--audio-quality", options.getAudioQuality() + "K");
            }
        } else {
            String quality = options.getVideoQuality() != null ? options.getVideoQuality() : DownloaderPrefs.getDefaultVideoQuality(context);
            String formatStr = buildVideoFormatSelector(quality);
            request.addOption("-f", formatStr);

            String container = options.getVideoFormat() != null ? options.getVideoFormat() : DownloaderPrefs.getDefaultVideoFormat(context);
            request.addOption("--merge-output-format", container.toLowerCase());
        }

        if (options.isEmbedThumbnail()) {
            request.addOption("--embed-thumbnail");
        }
        if (options.isEmbedMetadata()) {
            request.addOption("--add-metadata");
        }
        if (options.isEmbedSubtitles()) {
            request.addOption("--embed-subs");
            request.addOption("--all-subs");
        }
        if (options.isSplitChapters()) {
            request.addOption("--split-chapters");
        }

        if (options.getDownloadSections() != null && !options.getDownloadSections().isEmpty()) {
            request.addOption("--download-sections", options.getDownloadSections());
        }

        request.addOption("--no-mtime");
        request.addOption("--ignore-errors");
        return request;
    }

    private String buildVideoFormatSelector(String quality) {
        if (quality == null || "best".equalsIgnoreCase(quality)) {
            return "bestvideo+bestaudio/best";
        }
        try {
            int height = Integer.parseInt(quality.replaceAll("[^0-9]", ""));
            return "bestvideo[height<=" + height + "]+bestaudio/best[height<=" + height + "]/best";
        } catch (Exception e) {
            return "bestvideo+bestaudio/best";
        }
    }

    private File findDownloadedFile(File directory, String url) {
        if (directory == null || !directory.exists()) return null;
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) return null;

        File newest = null;
        long lastMod = 0;
        for (File f : files) {
            if (f.isFile() && f.lastModified() > lastMod && !f.getName().endsWith(".part") && !f.getName().endsWith(".ytdl")) {
                newest = f;
                lastMod = f.lastModified();
            }
        }
        return newest;
    }
}
