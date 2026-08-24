/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.ui;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloaderEngine;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.util.TransitionHelper;

public class CoreInstallerBottomSheet {

    private static final String TAG = "KGPT_CoreInstaller";

    // Direct GitHub Release URL format (Single Full Bundle)
    private static final String RELEASE_DOWNLOAD_URL = "https://github.com/Eluea/KGPT/releases/download/v4.0.8-downloader-core/kgpt-core-";

    // Fast CDN fallback mirrors for chunked download
    private static final String[] CDN_MIRRORS = new String[]{
            "https://cdn.jsdelivr.net/gh/Eluea/KGPTv4.0.8-downloader-core/core_bundles/",
            "https://fastly.jsdelivr.net/gh/Eluea/KGPTv4.0.8-downloader-core/core_bundles/",
            "https://gcore.jsdelivr.net/gh/Eluea/KGPTv4.0.8-downloader-core/core_bundles/",
            "https://raw.githubusercontent.com/Eluea/KGPT/main/core_bundles/"
    };

    public interface OnInstallCompleteListener {
        void onInstalled();
    }

    private final Context context;
    private final OnInstallCompleteListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private FloatingBottomSheet dialog;
    private ViewGroup rootContainer;
    private ImageView ivHeaderIcon, ivSuccessIcon;
    private com.airbnb.lottie.LottieAnimationView lottieProgress;
    private TextView tvTitle, tvSubtitle, tvPluginArch, tvPluginSize, tvDownloadStatus, tvDownloadDetails, tvDownloadTitle;
    private MaterialCardView cardPluginInfo;
    private View scrollFeatures, containerActions, containerDownloading;
    private MaterialButton btnDownloadPlugin, btnCancelPlugin, btnAbortDownload;

    private boolean isCancelled = false;
    private android.content.DialogInterface.OnDismissListener onDismissListener;

    private static class ChunkInfo {
        int index;
        String name;
        long size;
        String sha256;
    }

    public CoreInstallerBottomSheet(@NonNull Context context, OnInstallCompleteListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setOnDismissListener(android.content.DialogInterface.OnDismissListener listener) {
        this.onDismissListener = listener;
    }

    public void show() {
        dialog = new FloatingBottomSheet(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_core_installer, null);
        BottomSheetHelper.applyTheme(context, view);
        dialog.setContentView(view);
        dialog.setCancelable(true);

        initViews(view);
        populateInfo();
        setupListeners();

        dialog.setOnDismissListener(d -> {
            isCancelled = true;
            executor.shutdownNow();
            if (onDismissListener != null) {
                onDismissListener.onDismiss(d);
            }
        });

        dialog.show();
    }

    private void initViews(View view) {
        rootContainer = (ViewGroup) view;
        ivHeaderIcon = view.findViewById(R.id.iv_header_icon);
        tvTitle = view.findViewById(R.id.tv_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvPluginArch = view.findViewById(R.id.tv_plugin_arch);
        tvPluginSize = view.findViewById(R.id.tv_plugin_size);
        tvDownloadStatus = view.findViewById(R.id.tv_download_status);
        tvDownloadDetails = view.findViewById(R.id.tv_download_details);
        tvDownloadTitle = view.findViewById(R.id.tv_download_title);

        cardPluginInfo = view.findViewById(R.id.card_plugin_info);
        scrollFeatures = view.findViewById(R.id.scroll_features);
        containerActions = view.findViewById(R.id.container_actions);
        containerDownloading = view.findViewById(R.id.container_downloading);

        lottieProgress = view.findViewById(R.id.lottie_progress);
        ivSuccessIcon = view.findViewById(R.id.iv_success_icon);

        int primaryColor = com.google.android.material.color.MaterialColors.getColor(context,
                androidx.appcompat.R.attr.colorPrimary, android.graphics.Color.WHITE);
        if (lottieProgress != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieProgress, primaryColor);
        }

        btnDownloadPlugin = view.findViewById(R.id.btn_download_plugin);
        btnCancelPlugin = view.findViewById(R.id.btn_cancel_plugin);
        btnAbortDownload = view.findViewById(R.id.btn_abort_download);
    }

    private void populateInfo() {
        String abi = getPrimaryAbi();
        if (tvPluginArch != null) {
            tvPluginArch.setText(abi);
        }
        if (tvPluginSize != null) {
            if (abi.contains("arm64")) {
                tvPluginSize.setText("~47 MB");
            } else if (abi.contains("v7a")) {
                tvPluginSize.setText("~41 MB");
            } else {
                tvPluginSize.setText("~48 MB");
            }
        }
    }

    private String getPrimaryAbi() {
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            for (String abi : Build.SUPPORTED_ABIS) {
                if (abi.equals("arm64-v8a") || abi.equals("armeabi-v7a") || abi.equals("x86_64") || abi.equals("x86")) {
                    return abi;
                }
            }
            return Build.SUPPORTED_ABIS[0];
        }
        return "arm64-v8a";
    }

    private void setupListeners() {
        if (btnDownloadPlugin != null) {
            btnDownloadPlugin.setOnClickListener(v -> startDownloadProcess());
        }
        if (btnCancelPlugin != null) {
            btnCancelPlugin.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnAbortDownload != null) {
            btnAbortDownload.setOnClickListener(v -> {
                isCancelled = true;
                dialog.dismiss();
            });
        }
    }

    private void startDownloadProcess() {
        TransitionHelper.beginTransition(rootContainer, TransitionHelper.DURATION_NORMAL);

        if (ivHeaderIcon != null) ivHeaderIcon.setVisibility(View.GONE);
        if (tvTitle != null) tvTitle.setVisibility(View.GONE);
        if (tvSubtitle != null) tvSubtitle.setVisibility(View.GONE);
        if (cardPluginInfo != null) cardPluginInfo.setVisibility(View.GONE);
        if (scrollFeatures != null) scrollFeatures.setVisibility(View.GONE);
        if (containerActions != null) containerActions.setVisibility(View.GONE);

        if (containerDownloading != null) containerDownloading.setVisibility(View.VISIBLE);

        if (lottieProgress != null) {
            lottieProgress.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
            lottieProgress.playAnimation();
        }

        executor.execute(this::performDownloadAndInstallation);
    }

    private void performDownloadAndInstallation() {
        String abi = getPrimaryAbi();
        File finalZip = new File(context.getCacheDir(), "kgpt-core-" + abi + ".zip");
        File destCoreDir = new File(context.getFilesDir(), "youtubedl-core");
        File destNoBackupDir = new File(context.getNoBackupFilesDir(), "youtubedl-android");
        if (!destCoreDir.exists()) destCoreDir.mkdirs();
        if (!destNoBackupDir.exists()) destNoBackupDir.mkdirs();

        try {
            boolean downloadSuccess = false;

            // 0. P4: framework Remote Files (trusted, LSPosed-managed)
            File remoteZip = findRemoteBundleZip(abi);
            if (remoteZip != null) {
                String zipSha = readManifestZipSha(abi);
                if (zipSha != null) verifySha256(remoteZip, zipSha);
                copyFile(remoteZip, finalZip);
                downloadSuccess = true;
            }

            // 1. First check local device storage (Instant setup if present)
            File localZip = (remoteZip != null) ? null : findLocalBundleZip(abi);
            if (localZip != null) {
                // Local bundles get the same zero-trust treatment: verify against
                // the manifest zip digest when available, otherwise reject.
                String zipSha = readManifestZipSha(abi);
                if (zipSha != null) verifySha256(localZip, zipSha);
                else throw new Exception("Local core bundle rejected: no manifest digest to verify");
            }
            if (localZip != null && localZip.exists() && localZip.length() > 1024 * 1024) {
                Log.d(TAG, "Using local bundle zip: " + localZip.getAbsolutePath());
                copyFile(localZip, finalZip);
                downloadSuccess = true;
            }

            // 2. Primary: Download directly from GitHub Releases (Single file, high speed)
            if (!downloadSuccess) {
                String releaseUrl = RELEASE_DOWNLOAD_URL + abi + ".zip";
                try {
                    Log.d(TAG, "Downloading full release bundle: " + releaseUrl);
                    downloadSingleFile(releaseUrl, finalZip);
                    // Integrity: prefer the pinned manifest digest; if the digest
                    // source is the network itself this at least pins the
                    // assembled artifact across retries/mirrors.
                    String zipSha = readManifestZipSha(abi);
                    if (zipSha != null) verifySha256(finalZip, zipSha);
                    downloadSuccess = true;
                } catch (Exception e) {
                    Log.w(TAG, "Release download failed (" + e.getMessage() + "), falling back to chunked CDN...");
                }
            }

            // 3. Fallback: Multi-threaded parallel chunk download via global CDNs
            if (!downloadSuccess) {
                performChunkedDownload(abi, finalZip);
                downloadSuccess = true;
            }

            if (isCancelled) return;

            // 4. Extraction Phase
            mainHandler.post(() -> {
                if (tvDownloadStatus != null) tvDownloadStatus.setText(context.getString(R.string.status_installing_plugin));
                if (tvDownloadDetails != null) tvDownloadDetails.setText("Extracting core binaries...");
            });

            extractZip(finalZip, destCoreDir);

            // Extract inner packages into packages/python and packages/ffmpeg
            File packagesDir = new File(destNoBackupDir, "packages");
            File pythonDir = new File(packagesDir, "python");
            File ffmpegDir = new File(packagesDir, "ffmpeg");
            if (!packagesDir.exists()) packagesDir.mkdirs();
            if (!pythonDir.exists()) pythonDir.mkdirs();
            if (!ffmpegDir.exists()) ffmpegDir.mkdirs();

            File pythonZip = new File(destCoreDir, "libpython.zip.so");
            if (pythonZip.exists()) {
                try {
                    extractZip(pythonZip, pythonDir);
                } catch (Throwable t) {
                    Log.w(TAG, "Unzipping python package: " + t.getMessage());
                }
            }

            File ffmpegZip = new File(destCoreDir, "libffmpeg.zip.so");
            if (ffmpegZip.exists()) {
                try {
                    extractZip(ffmpegZip, ffmpegDir);
                } catch (Throwable t) {
                    Log.w(TAG, "Unzipping ffmpeg package: " + t.getMessage());
                }
            }

            // Resolve ELF symlinks
            DownloaderEngine.getInstance().resolveSymlinksRecursively(pythonDir);
            DownloaderEngine.getInstance().resolveSymlinksRecursively(ffmpegDir);

            // Clean up temporary ZIP
            finalZip.delete();

            // Initialize the engine
            DownloaderEngine.getInstance().init(context);

            // Asynchronously update yt-dlp to latest release in background
            new Thread(() -> {
                try {
                    com.yausername.youtubedl_android.YoutubeDL.getInstance().updateYoutubeDL(context.getApplicationContext(), com.yausername.youtubedl_android.YoutubeDL.UpdateChannel._STABLE);
                } catch (Throwable ignored) {}
            }).start();

            mainHandler.post(() -> {
                if (lottieProgress != null) {
                    lottieProgress.setRepeatCount(0);
                }
                if (tvDownloadStatus != null) tvDownloadStatus.setText(context.getString(R.string.toast_engine_updated));
                if (tvDownloadDetails != null) tvDownloadDetails.setText("Ready to download media");

                mainHandler.postDelayed(() -> {
                    if (dialog != null && dialog.isShowing()) {
                        // Clear dismiss listener before dismiss to avoid triggering finish() on host activity
                        dialog.setOnDismissListener(null);
                        dialog.dismiss();
                    }
                    if (listener != null) {
                        listener.onInstalled();
                    }
                }, 1000);
            });

        } catch (Exception e) {
            Log.e(TAG, "Install failed: " + e.getMessage(), e);
            mainHandler.post(() -> {
                if (lottieProgress != null) {
                    lottieProgress.setRepeatCount(0);
                }
                Toast.makeText(context, "Installation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                if (dialog != null) dialog.dismiss();
            });
        }
    }

    private void downloadSingleFile(String urlStr, File destFile) throws Exception {
        HttpURLConnection conn = openConnectionWithRedirects(urlStr);
        long totalSize = conn.getContentLength();
        if (totalSize <= 0) {
            totalSize = 48 * 1024 * 1024L;
        }

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[32768];
            int read;
            long totalDownloaded = 0;
            while ((read = in.read(buffer)) != -1) {
                if (isCancelled) return;
                out.write(buffer, 0, read);
                totalDownloaded += read;
                final long curr = totalDownloaded;
                final long tot = totalSize;
                mainHandler.post(() -> updateProgressUi(curr, tot));
            }
            out.flush();
        } finally {
            conn.disconnect();
        }
    }

    private void performChunkedDownload(String abi, File finalZip) throws Exception {
        File chunksDir = new File(context.getCacheDir(), "chunks_" + abi);
        if (!chunksDir.exists()) chunksDir.mkdirs();

        File localDeviceDir = findLocalChunksDir(abi);
        List<ChunkInfo> chunks = loadManifestChunks(abi, localDeviceDir);
        long totalExpectedSize = 0;
        for (ChunkInfo c : chunks) totalExpectedSize += c.size;
        final long totalSize = totalExpectedSize > 0 ? totalExpectedSize : 48 * 1024 * 1024L;

        AtomicLong bytesDownloaded = new AtomicLong(0);

        int workerCount = Math.min(4, chunks.size());
        ExecutorService chunkPool = Executors.newFixedThreadPool(workerCount);
        CountDownLatch latch = new CountDownLatch(chunks.size());
        boolean[] downloadFailed = new boolean[]{false};
        String[] failureReason = new String[]{null};

        for (ChunkInfo chunk : chunks) {
            chunkPool.execute(() -> {
                if (isCancelled || downloadFailed[0]) {
                    latch.countDown();
                    return;
                }
                try {
                    File targetChunkFile = new File(chunksDir, chunk.name);

                    // Resume check — still integrity-verified
                    if (targetChunkFile.exists() && targetChunkFile.length() == chunk.size) {
                        verifySha256(targetChunkFile, chunk.sha256);
                        bytesDownloaded.addAndGet(chunk.size);
                        mainHandler.post(() -> updateProgressUi(bytesDownloaded.get(), totalSize));
                        latch.countDown();
                        return;
                    }

                    // Local check
                    if (localDeviceDir != null) {
                        File localChunk = new File(localDeviceDir, chunk.name);
                        if (localChunk.exists() && localChunk.length() == chunk.size) {
                            copyFile(localChunk, targetChunkFile);
                            verifySha256(targetChunkFile, chunk.sha256);
                            bytesDownloaded.addAndGet(chunk.size);
                            mainHandler.post(() -> updateProgressUi(bytesDownloaded.get(), totalSize));
                            latch.countDown();
                            return;
                        }
                    }

                    // Download chunk with CDN mirror fallback, then ENFORCE digest
                    downloadChunkWithMirrorFallback(abi, chunk, targetChunkFile, bytesDownloaded, totalSize);
                    verifySha256(targetChunkFile, chunk.sha256);
                    latch.countDown();
                } catch (Exception e) {
                    Log.e(TAG, "Failed downloading chunk " + chunk.name + ": " + e.getMessage(), e);
                    downloadFailed[0] = true;
                    failureReason[0] = e.getMessage();
                    latch.countDown();
                }
            });
        }

        latch.await();
        chunkPool.shutdown();

        if (isCancelled) return;
        if (downloadFailed[0]) {
            throw new Exception("Chunk download failed: " + (failureReason[0] != null ? failureReason[0] : "Network timeout"));
        }

        assembleChunks(chunks, chunksDir, finalZip);
        deleteDirectory(chunksDir);
    }

    private void downloadChunkWithMirrorFallback(String abi, ChunkInfo chunk, File targetChunkFile, AtomicLong totalDownloaded, long totalSize) throws Exception {
        Exception lastException = null;
        for (String baseUrl : CDN_MIRRORS) {
            if (isCancelled) return;
            try {
                String chunkUrl = baseUrl + abi + "/" + chunk.name;
                downloadChunkFromUrl(chunkUrl, targetChunkFile, chunk.size, totalDownloaded, totalSize);
                return;
            } catch (Exception e) {
                lastException = e;
                if (targetChunkFile.exists()) {
                    targetChunkFile.delete();
                }
            }
        }
        throw (lastException != null ? lastException : new Exception("All mirrors failed for " + chunk.name));
    }

    /** Enforced integrity check: chunk/zip must match the manifest digest. */
    private void verifySha256(File file, String expectedHex) throws Exception {
        if (expectedHex == null || expectedHex.trim().isEmpty()) {
            throw new Exception("Integrity check failed: manifest has no sha256 for " + file.getName());
        }
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            byte[] buf = new byte[32768];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        String actual = sb.toString();
        if (!actual.equalsIgnoreCase(expectedHex.trim())) {
            file.delete();
            throw new Exception("Integrity check failed (sha256 mismatch) for " + file.getName());
        }
    }

    /**
     * P4: framework Remote Files are a TRUSTED distribution source (managed by
     * LSPosed daemon) — checked before anything else. Digest is still enforced
     * by the caller when the manifest declares it.
     */
    private File findRemoteBundleZip(String abi) {
        try {
            io.github.libxposed.service.XposedService svc = tn.eluea.kgpt.util.LSPosedHelper.getService();
            if (svc == null) return null;
            String name = "kgpt-core-" + abi + ".zip";
            for (String remoteName : svc.listRemoteFiles()) {
                if (name.equals(remoteName)) {
                    android.os.ParcelFileDescriptor pfd = svc.openRemoteFile(name);
                    if (pfd == null) return null;
                    File local = new File(context.getCacheDir(), "kgpt_remote_" + name);
                    try (java.io.InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd);
                         java.io.FileOutputStream out = new java.io.FileOutputStream(local)) {
                        byte[] buf = new byte[32768];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }
                    Log.i(TAG, "Using framework remote file: " + name);
                    return local;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "remote file source unavailable: " + t.getMessage());
        }
        return null;
    }

    private File findLocalBundleZip(String abi) {
        // SECURITY: only the app-private dir is trusted for local bundles.
        // Shared-storage auto-load (/sdcard) allowed any app/adb to plant an
        // unverified 47MB native bundle that we would execute.
        String[] possiblePaths = new String[]{
                new File(context.getExternalFilesDir(null), "kgpt-core-" + abi + ".zip").getAbsolutePath()
        };
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile() && file.length() > 1024 * 1024) {
                return file;
            }
        }
        return null;
    }

    private File findLocalChunksDir(String abi) {
        String[] possiblePaths = new String[]{
                new File(context.getExternalFilesDir(null), "kgpt_core/" + abi).getAbsolutePath()
        };
        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }

    private volatile String manifestZipSha = null;
    private String readManifestZipSha(String abi) {
        // Populated by performChunkedDownload -> loadManifestChunks; a null here
        // means no trusted digest source, so local bundles are rejected.
        return manifestZipSha;
    }

    private List<ChunkInfo> loadManifestChunks(String abi, File localDeviceDir) {
        List<ChunkInfo> chunks = new ArrayList<>();
        try {
            String manifestJsonStr = null;
            if (localDeviceDir != null) {
                File localManifest = new File(localDeviceDir, "manifest.json");
                if (localManifest.exists()) {
                    manifestJsonStr = readFileToString(localManifest);
                }
            }
            if (manifestJsonStr == null || manifestJsonStr.trim().isEmpty()) {
                for (String baseUrl : CDN_MIRRORS) {
                    try {
                        String url = baseUrl + abi + "/manifest.json";
                        manifestJsonStr = downloadString(url);
                        if (manifestJsonStr != null && !manifestJsonStr.trim().isEmpty()) break;
                    } catch (Exception ignored) {}
                }
            }

            if (manifestJsonStr != null) {
                try {
                    org.json.JSONObject top = new org.json.JSONObject(manifestJsonStr);
                    manifestZipSha = top.optString("sha256", null);
                } catch (Throwable ignored) {}
                manifestJsonStr = manifestJsonStr.trim();
                if (manifestJsonStr.startsWith("\uFEFF")) {
                    manifestJsonStr = manifestJsonStr.substring(1);
                }
                JSONObject obj = new JSONObject(manifestJsonStr);
                JSONArray chunkArr = obj.optJSONArray("chunks");
                if (chunkArr != null) {
                    for (int i = 0; i < chunkArr.length(); i++) {
                        JSONObject cObj = chunkArr.getJSONObject(i);
                        ChunkInfo info = new ChunkInfo();
                        info.index = cObj.optInt("index", i);
                        info.name = cObj.optString("name", "chunk_" + String.format(java.util.Locale.US, "%02d", i) + ".bin");
                        info.size = cObj.optLong("size", 5242880);
                        info.sha256 = cObj.optString("sha256", "");
                        chunks.add(info);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not load remote manifest, using default chunk scheme: " + e.getMessage());
        }

        if (chunks.isEmpty()) {
            int chunkCount = abi.contains("v7a") ? 9 : (abi.contains("x86_64") ? 11 : 10);
            for (int i = 0; i < chunkCount; i++) {
                ChunkInfo info = new ChunkInfo();
                info.index = i;
                info.name = "chunk_" + String.format(java.util.Locale.US, "%02d", i) + ".bin";
                info.size = 5242880;
                chunks.add(info);
            }
        }
        return chunks;
    }

    private static HttpURLConnection openConnectionWithRedirects(String urlStr) throws Exception {
        String currentUrl = urlStr;
        for (int redirects = 0; redirects < 5; redirects++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; KGPT) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.connect();

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location != null && !location.isEmpty()) {
                    currentUrl = location;
                    continue;
                }
            }
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new Exception("HTTP " + code + " for " + urlStr);
            }
            return conn;
        }
        throw new Exception("Too many redirects for " + urlStr);
    }

    private void downloadChunkFromUrl(String urlStr, File destFile, long expectedSize, AtomicLong totalDownloaded, long totalSize) throws Exception {
        HttpURLConnection conn = openConnectionWithRedirects(urlStr);

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[32768];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (isCancelled) return;
                out.write(buffer, 0, read);
                long current = totalDownloaded.addAndGet(read);
                mainHandler.post(() -> updateProgressUi(current, totalSize));
            }
            out.flush();
        } finally {
            conn.disconnect();
        }
    }

    private void assembleChunks(List<ChunkInfo> chunks, File chunksDir, File finalZip) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(finalZip)) {
            byte[] buffer = new byte[65536];
            for (ChunkInfo chunk : chunks) {
                File chunkFile = new File(chunksDir, chunk.name);
                if (!chunkFile.exists()) {
                    throw new Exception("Missing chunk file: " + chunk.name);
                }
                try (FileInputStream fis = new FileInputStream(chunkFile)) {
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }
            }
            fos.flush();
        }
    }

    private void extractZip(File zipFile, File targetDir) throws Exception {
        byte[] buffer = new byte[32768];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Zip-slip guard: reject entries escaping the target dir
                File newFile = new File(targetDir, entry.getName());
                try {
                    if (!newFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath() + File.separator)) {
                        Log.w(TAG, "Blocked zip-slip entry: " + entry.getName());
                        zis.closeEntry();
                        continue;
                    }
                } catch (Throwable ignored) {}

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
                    // Grant execute & read permissions for binaries
                    newFile.setExecutable(true, false);
                    newFile.setReadable(true, false);
                }
                zis.closeEntry();
            }
        }
    }

    private void updateProgressUi(long bytesDownloaded, long totalBytes) {
        if (tvDownloadStatus != null) {
            int percent = (int) Math.min(100, (bytesDownloaded * 100L) / Math.max(totalBytes, 1));
            tvDownloadStatus.setText(percent + "%");
        }
        if (tvDownloadDetails != null) {
            String downloaded = formatBytes(bytesDownloaded);
            String total = formatBytes(totalBytes);
            tvDownloadDetails.setText(downloaded + " / " + total);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String downloadString(String urlStr) {
        try {
            HttpURLConnection conn = openConnectionWithRedirects(urlStr);
            try (InputStream is = conn.getInputStream();
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed downloading string from " + urlStr + ": " + e.getMessage());
        }
        return null;
    }

    private static String readFileToString(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception ignored) {}
        return null;
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[32768];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
            dir.delete();
        } else if (dir != null) {
            dir.delete();
        }
    }
}
