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

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.util.TransitionHelper;

/**
 * Bottom sheet dialog for displaying update information and download progress.
 */
public class UpdateBottomSheet {

    private static final String TAG = "KGPT_UpdateBottomSheet";

    private final Context context;
    private final UpdateInfo updateInfo;
    private final Handler mainHandler;
    private final ExecutorService executor;

    private FloatingBottomSheet dialog;
    private UpdateDownloader downloader;

    // UI Elements
    private ImageView ivHeaderIcon;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvCurrentVersion;
    private TextView tvNewVersion;
    private TextView tvChangelog;
    private TextView tvDownloadStatus;
    private TextView tvDownloadDetails;
    private TextView tvDownloadTitle;
    private CircularProgressIndicator progressCircular;
    private ImageView ivHourglassIcon;
    private ImageView ivSuccessIcon;
    private MaterialButton btnDownload;
    private MaterialButton btnLater;
    private MaterialButton btnInstall;
    private MaterialButton btnCancel;
    private View downloadingContainer;
    private View actionsContainer;
    private View installContainer;
    private MaterialCardView cardVersionComparison;
    private View scrollChangelog;

    private File downloadedApk;
    private ObjectAnimator hourglassRotationAnimator;
    private long lastTotalBytes = 0;
    private ViewGroup rootContainer;

    public UpdateBottomSheet(Context context, UpdateInfo updateInfo) {
        this.context = context;
        this.updateInfo = updateInfo;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Show the update bottom sheet
     */
    public void show() {
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_update, null);

        // Apply theme
        BottomSheetHelper.applyTheme(context, sheetView);

        dialog = new FloatingBottomSheet(context);
        dialog.setContentView(sheetView);
        dialog.setCancelable(true);

        initViews(sheetView);
        populateData();
        setupListeners();

        dialog.show();
    }

    private void initViews(View view) {
        rootContainer = (ViewGroup) view;
        ivHeaderIcon = view.findViewById(R.id.iv_header_icon);
        tvTitle = view.findViewById(R.id.tv_title);
        tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tvCurrentVersion = view.findViewById(R.id.tv_current_version);
        tvNewVersion = view.findViewById(R.id.tv_new_version);
        tvChangelog = view.findViewById(R.id.tv_changelog);
        tvDownloadStatus = view.findViewById(R.id.tv_download_status);
        tvDownloadDetails = view.findViewById(R.id.tv_download_details);
        tvDownloadTitle = view.findViewById(R.id.tv_download_title);
        progressCircular = view.findViewById(R.id.progress_circular);
        ivHourglassIcon = view.findViewById(R.id.iv_hourglass_icon);
        ivSuccessIcon = view.findViewById(R.id.iv_success_icon);
        btnDownload = view.findViewById(R.id.btn_download);
        btnLater = view.findViewById(R.id.btn_later);
        btnInstall = view.findViewById(R.id.btn_install);
        btnCancel = view.findViewById(R.id.btn_cancel);
        downloadingContainer = view.findViewById(R.id.container_downloading);
        actionsContainer = view.findViewById(R.id.container_actions);
        installContainer = view.findViewById(R.id.container_install);
        cardVersionComparison = view.findViewById(R.id.card_version_comparison);
        scrollChangelog = view.findViewById(R.id.scroll_changelog);

        // Make changelog scrollable
        if (tvChangelog != null) {
            tvChangelog.setMovementMethod(new ScrollingMovementMethod());
        }
    }

    private void populateData() {
        if (tvCurrentVersion != null) {
            tvCurrentVersion.setText("v" + BuildConfig.VERSION_NAME);
        }

        if (tvNewVersion != null) {
            tvNewVersion.setText("v" + updateInfo.getVersionName());
        }

        if (tvChangelog != null) {
            String changelog = updateInfo.getChangelog();
            if (changelog != null && !changelog.isEmpty()) {
                // Clean up markdown for display
                changelog = changelog
                        .replaceAll("#+\\s*", "") // Remove markdown headers
                        .replaceAll("\\*\\*", "") // Remove bold markers
                        .replaceAll("\\*", "•") // Convert list items
                        .trim();
                tvChangelog.setText(changelog);
            } else {
                tvChangelog.setText("No changelog available.");
            }
        }

        // Initial state: show download button
        showState(State.READY);
    }

    private void setupListeners() {
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> startDownload());
        }

        if (btnLater != null) {
            btnLater.setOnClickListener(v -> {
                dialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (downloader != null) {
                    downloader.cancelDownload();
                }
                stopHourglassAnimation();
                showState(State.READY);
            });
        }

        if (btnInstall != null) {
            btnInstall.setOnClickListener(v -> {
                if (downloadedApk != null && downloader != null) {
                    // Clear cached update first
                    UpdateWorker.clearCachedUpdate(context);

                    // Launch installation - don't dismiss dialog immediately
                    // The system install dialog will appear on top
                    downloader.installApk(downloadedApk);

                    // Dismiss after a short delay to ensure install intent is launched
                    mainHandler.postDelayed(() -> {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }, 500);
                }
            });
        }
    }

    private void startDownload() {
        showState(State.DOWNLOADING);

        downloader = new UpdateDownloader(context);
        downloader.startDownload(updateInfo, new UpdateDownloader.DownloadProgressListener() {
            @Override
            public void onProgressUpdate(int progress, long bytesDownloaded, long totalBytes) {
                mainHandler.post(() -> {
                    if (progressCircular != null) {
                        // Use higher precision (0-1000 instead of 0-100)
                        int preciseProgress = (int) ((bytesDownloaded * 1000L) / Math.max(totalBytes, 1));
                        progressCircular.setProgress(preciseProgress);
                    }
                    if (tvDownloadStatus != null) {
                        tvDownloadStatus.setText("Downloading... " + progress + "%");
                    }
                    if (tvDownloadDetails != null) {
                        String downloaded = formatBytes(bytesDownloaded);
                        String total = formatBytes(totalBytes);
                        tvDownloadDetails.setText(downloaded + " / " + total);
                    }
                    // Store total bytes for final display
                    lastTotalBytes = totalBytes;
                });
            }

            @Override
            public void onDownloadComplete(File apkFile) {
                downloadedApk = apkFile;
                mainHandler.post(() -> {
                    stopHourglassAnimation();
                    showState(State.COMPLETE);
                });
            }

            @Override
            public void onDownloadFailed(String error) {
                mainHandler.post(() -> {
                    stopHourglassAnimation();
                    showState(State.READY);
                    if (tvDownloadStatus != null) {
                        tvDownloadStatus.setText("Error: " + error);
                        tvDownloadStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private enum State {
        READY, // Show download button
        DOWNLOADING, // Show progress circle
        COMPLETE // Show install button
    }

    private void showState(State state) {
        switch (state) {
            case READY:
                // Show all info containers
                if (cardVersionComparison != null) {
                    cardVersionComparison.setVisibility(View.VISIBLE);
                    cardVersionComparison.setAlpha(1f);
                }
                if (scrollChangelog != null) {
                    scrollChangelog.setVisibility(View.VISIBLE);
                    scrollChangelog.setAlpha(1f);
                }
                if (ivHeaderIcon != null) {
                    ivHeaderIcon.setImageResource(R.drawable.ic_update_rocket_filled);
                }
                if (tvTitle != null)
                    tvTitle.setText("Update Available");
                if (tvSubtitle != null) {
                    tvSubtitle.setText("A new version of KGPT is available.");
                    tvSubtitle.setVisibility(View.VISIBLE);
                }
                if (actionsContainer != null)
                    actionsContainer.setVisibility(View.VISIBLE);
                if (downloadingContainer != null)
                    downloadingContainer.setVisibility(View.GONE);
                if (installContainer != null)
                    installContainer.setVisibility(View.GONE);
                break;

            case DOWNLOADING:
                // Begin smooth transition
                TransitionHelper.beginTransition(rootContainer, TransitionHelper.DURATION_NORMAL);

                // Hide header icon and title during download
                if (ivHeaderIcon != null)
                    ivHeaderIcon.setVisibility(View.GONE);
                if (tvTitle != null)
                    tvTitle.setVisibility(View.GONE);
                if (tvSubtitle != null)
                    tvSubtitle.setVisibility(View.GONE);

                // Hide version and changelog containers
                if (cardVersionComparison != null)
                    cardVersionComparison.setVisibility(View.GONE);
                if (scrollChangelog != null)
                    scrollChangelog.setVisibility(View.GONE);

                // Set download title
                if (tvDownloadTitle != null)
                    tvDownloadTitle.setText("Downloading Update");
                if (actionsContainer != null)
                    actionsContainer.setVisibility(View.GONE);
                if (downloadingContainer != null) {
                    downloadingContainer.setVisibility(View.VISIBLE);
                }
                if (installContainer != null)
                    installContainer.setVisibility(View.GONE);

                // Reset progress UI
                if (progressCircular != null) {
                    progressCircular.setProgress(0);
                    progressCircular.setMax(1000); // Higher precision
                    progressCircular.setVisibility(View.VISIBLE);

                    // Enable wavy effect (Material 1.13.0+)
                    // Adjusted for 16dp track thickness
                    try {
                        progressCircular.setWavelength(120); // Longer wavelength for smoother waves
                        progressCircular.setWaveAmplitude(10); // Higher amplitude for visible waves
                        progressCircular.setWaveSpeed(60); // Moderate speed for smooth animation
                    } catch (NoSuchMethodError e) {
                        // Wavy effect not supported in this version
                    }
                }
                if (ivHourglassIcon != null) {
                    ivHourglassIcon.setVisibility(View.VISIBLE);
                }
                if (ivSuccessIcon != null)
                    ivSuccessIcon.setVisibility(View.GONE);
                if (tvDownloadStatus != null)
                    tvDownloadStatus.setText("Starting download...");
                if (tvDownloadDetails != null)
                    tvDownloadDetails.setText("");
                if (btnCancel != null)
                    btnCancel.setVisibility(View.VISIBLE);

                // Start hourglass rotation animation
                startHourglassAnimation();
                break;

            case COMPLETE:
                // Hide header icon and title
                if (ivHeaderIcon != null)
                    ivHeaderIcon.setVisibility(View.GONE);
                if (tvTitle != null)
                    tvTitle.setVisibility(View.GONE);
                if (tvSubtitle != null)
                    tvSubtitle.setVisibility(View.GONE);

                // Set complete title
                if (tvDownloadTitle != null)
                    tvDownloadTitle.setText("Download Complete");

                // Show success state with animation
                showDownloadCompleteAnimation();

                if (tvDownloadStatus != null)
                    tvDownloadStatus.setText("Ready to install!");
                // Show accurate final size
                if (tvDownloadDetails != null && lastTotalBytes > 0) {
                    String totalSize = formatBytes(lastTotalBytes);
                    tvDownloadDetails.setText(totalSize + " / " + totalSize);
                }
                if (btnCancel != null)
                    btnCancel.setVisibility(View.GONE);

                // Show install button with smooth transition
                if (installContainer != null) {
                    installContainer.setVisibility(View.VISIBLE);
                    TransitionHelper.slideInFromBottom(installContainer, TransitionHelper.DURATION_NORMAL);
                }
                break;
        }
    }

    private void startHourglassAnimation() {
        if (ivHourglassIcon == null)
            return;

        // Create rotation animation - 360 degrees every 1.4 seconds
        hourglassRotationAnimator = ObjectAnimator.ofFloat(ivHourglassIcon, "rotation", 0f, 360f);
        hourglassRotationAnimator.setDuration(1400);
        hourglassRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        hourglassRotationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        hourglassRotationAnimator.start();
    }

    private void stopHourglassAnimation() {
        if (hourglassRotationAnimator != null) {
            hourglassRotationAnimator.cancel();
            hourglassRotationAnimator = null;
        }
        // Reset rotation
        if (ivHourglassIcon != null) {
            ivHourglassIcon.setRotation(0f);
        }
    }

    private void showDownloadCompleteAnimation() {
        if (progressCircular == null || ivSuccessIcon == null || ivHourglassIcon == null)
            return;

        // Animate progress to 100% (1000 for high precision)
        progressCircular.setProgress(1000);

        // Hide hourglass icon with fade out
        ivHourglassIcon.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> ivHourglassIcon.setVisibility(View.GONE))
                .start();

        // Show success icon with pop animation after hourglass fades
        mainHandler.postDelayed(() -> {
            TransitionHelper.popIn(ivSuccessIcon, TransitionHelper.DURATION_NORMAL);
        }, 250);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Dismiss the dialog
     */
    public void dismiss() {
        stopHourglassAnimation();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Check for updates and show dialog if available
     */
    public static void checkAndShow(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                UpdateChecker checker = new UpdateChecker(context);
                UpdateInfo updateInfo = checker.checkForUpdate();

                if (updateInfo != null) {
                    mainHandler.post(() -> {
                        new UpdateBottomSheet(context, updateInfo).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
            }
        });
    }

    /**
     * Show dialog for cached update (from background check)
     */
    public static void showCachedUpdate(Context context) {
        UpdateInfo cached = UpdateWorker.getCachedUpdate(context);
        if (cached != null) {
            new UpdateBottomSheet(context, cached).show();
        }
    }
}
