/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.tabs.TabLayout;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloadOptions;
import tn.eluea.kgpt.features.downloader.core.DownloaderEngine;
import tn.eluea.kgpt.features.downloader.core.DownloaderPrefs;
import tn.eluea.kgpt.features.downloader.core.MediaUtils;
import tn.eluea.kgpt.features.downloader.core.ThumbnailLoader;
import tn.eluea.kgpt.features.downloader.service.MediaDownloadService;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.util.LottieHelper;
import tn.eluea.kgpt.util.TransitionHelper;

/**
 * Modern Bottom Sheet for YouTube / Media Downloading.
 * Designed with Material 3, rich Lottie animations, and smooth dynamic height transitions.
 */
public class MediaDownloaderBottomSheet {

    private final Context context;
    private final String mediaUrl;
    private FloatingBottomSheet dialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    // Main views
    private View rootView;
    private ViewGroup rootLayout;
    private TabLayout tabLayoutMode;
    private ImageView ivThumbnail;
    private View scrimOverlay;
    private LinearLayout layoutThumbnailOverlay;
    private LottieAnimationView lottieSuccess;

    private TextView tvTitle, tvUploader, tvDurationBadge, tvContainer, tvQualitySectionLabel, tvQualityTitle;
    private TextView tvEstimatedSize, tvAdjustSectionLabel, tvCommandPreview;
    private LinearLayout layoutLoading, layoutContent, layoutCommandView, layoutOptionsSection, layoutDownloadingState;
    private LottieAnimationView lottieLoading;
    private LinearProgressIndicator progressDownload;
    private TextView tvDownloadProgressText;
    private MaterialButton btnDownloadCancel, btnDownloadBackground;

    private MaterialCardView cardThumbnailContainer, cardContainer, cardQuality, cardFileSize;
    private MaterialButton btnDownloadNow;
    private LinearLayout btnTurboSpeed;
    private LottieAnimationView lottieTurboSpeed;
    private TextView tvTurboBadge;
    private FrameLayout btnClose;
    private MaterialButton btnChipCut, btnChipThumbnail, btnChipChapters, btnChipSubtitles, btnChipRecode, btnChipFilename, btnChipExtraCommand;

    // State
    private VideoInfo cachedVideoInfo;
    private int currentTab = 0; // 0 = Video, 1 = Audio, 2 = CMD
    private String selectedContainer = "MP4";
    private String selectedVideoQuality = "1080";
    private String selectedAudioFormat = "mp3";
    private String selectedAudioBitrate = "320";
    private int concurrentFragments = 16;
    private String customTitle = null;
    private String customFilenameTemplate = "%(uploader)s - %(title)s";
    private String extraCommands = "";
    private String cutStartTime = "";
    private String cutEndTime = "";
    private boolean embedThumbnail = true;
    private boolean saveSeparateThumbnail = false;
    private boolean splitChapters = false;
    private boolean embedChapters = true;
    private boolean embedSubtitles = false;
    private boolean burnSubtitles = false;
    private boolean recodeVideo = false;
    private boolean compatibleH264 = false;

    private Runnable onDismissCallback;

    public MediaDownloaderBottomSheet(@NonNull Context context, @NonNull String mediaUrl) {
        this.context = context;
        this.mediaUrl = mediaUrl;
    }

    public void setOnDismissListener(Runnable callback) {
        this.onDismissCallback = callback;
    }

    public void setOnDismissListener(android.content.DialogInterface.OnDismissListener listener) {
        if (listener != null) {
            this.onDismissCallback = () -> listener.onDismiss(dialog);
        }
    }

    public void show() {
        if (!DownloaderEngine.getInstance().isCoreInstalled(context)) {
            CoreInstallerBottomSheet installer = new CoreInstallerBottomSheet(context, () -> {
                MediaDownloaderBottomSheet newSheet = new MediaDownloaderBottomSheet(context, mediaUrl);
                if (onDismissCallback != null) {
                    newSheet.setOnDismissListener(d -> onDismissCallback.run());
                }
                newSheet.show();
            });
            installer.setOnDismissListener(d -> {
                if (onDismissCallback != null) {
                    onDismissCallback.run();
                }
            });
            installer.show();
            return;
        }

        dialog = new FloatingBottomSheet(context);
        rootView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_media_downloader, null);
        BottomSheetHelper.applyTheme(context, rootView);
        dialog.setContentView(rootView);

        initViews(rootView);
        setupTabs();
        loadDefaultSettings();
        setupListeners();

        dialog.setOnDismissListener(d -> {
            MediaDownloadService.setDownloadEventListener(null);
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        });

        dialog.show();
        loadMediaDetails();
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.root_layout);
        tabLayoutMode = view.findViewById(R.id.tab_layout_mode);
        ivThumbnail = view.findViewById(R.id.iv_thumbnail);
        scrimOverlay = view.findViewById(R.id.scrim_overlay);
        layoutThumbnailOverlay = view.findViewById(R.id.layout_thumbnail_overlay);
        lottieSuccess = view.findViewById(R.id.lottie_success);

        tvTitle = view.findViewById(R.id.tv_title);
        tvUploader = view.findViewById(R.id.tv_uploader);
        tvDurationBadge = view.findViewById(R.id.tv_duration_badge);
        tvContainer = view.findViewById(R.id.tv_container);
        tvQualitySectionLabel = view.findViewById(R.id.tv_quality_section_label);
        tvQualityTitle = view.findViewById(R.id.tv_quality_title);
        tvEstimatedSize = view.findViewById(R.id.tv_estimated_size);
        btnTurboSpeed = view.findViewById(R.id.btn_turbo_speed);
        lottieTurboSpeed = view.findViewById(R.id.lottie_turbo_speed);
        tvTurboBadge = view.findViewById(R.id.tv_turbo_badge);
        tvAdjustSectionLabel = view.findViewById(R.id.tv_adjust_section_label);
        tvCommandPreview = view.findViewById(R.id.tv_command_preview);

        layoutLoading = view.findViewById(R.id.layout_loading);
        lottieLoading = view.findViewById(R.id.lottie_loading);
        layoutContent = view.findViewById(R.id.layout_content);
        layoutOptionsSection = view.findViewById(R.id.layout_options_section);
        layoutCommandView = view.findViewById(R.id.layout_command_view);
        layoutDownloadingState = view.findViewById(R.id.layout_downloading_state);
        progressDownload = view.findViewById(R.id.progress_download);
        tvDownloadProgressText = view.findViewById(R.id.tv_download_progress_text);
        btnDownloadCancel = view.findViewById(R.id.btn_download_cancel);
        btnDownloadBackground = view.findViewById(R.id.btn_download_background);

        cardThumbnailContainer = view.findViewById(R.id.card_thumbnail_container);
        cardContainer = view.findViewById(R.id.card_container);
        cardQuality = view.findViewById(R.id.card_quality);
        cardFileSize = view.findViewById(R.id.card_file_size);

        btnClose = view.findViewById(R.id.btn_close);
        btnDownloadNow = view.findViewById(R.id.btn_download_now);

        btnChipCut = view.findViewById(R.id.btn_chip_cut);
        btnChipThumbnail = view.findViewById(R.id.btn_chip_thumbnail);
        btnChipChapters = view.findViewById(R.id.btn_chip_chapters);
        btnChipSubtitles = view.findViewById(R.id.btn_chip_subtitles);
        btnChipRecode = view.findViewById(R.id.btn_chip_recode);
        btnChipFilename = view.findViewById(R.id.btn_chip_filename);
        btnChipExtraCommand = view.findViewById(R.id.btn_chip_extra_command);

        if (lottieLoading != null) {
            LottieHelper.tint(lottieLoading, MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary, Color.CYAN));
        }
        if (lottieTurboSpeed != null) {
            LottieHelper.tint(lottieTurboSpeed, MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.YELLOW));
            lottieTurboSpeed.setRepeatCount(0);
        }
    }

    private void setupTabs() {
        if (tabLayoutMode == null) return;
        tabLayoutMode.removeAllTabs();

        String[] titles = {"Video", "Audio", "CMD"};
        String[] lottieFiles = {
                "lottie/system-solid-4239-video-camera-hover-videocam-2.json",
                "lottie/system-solid-464-headphones-hover-hearphones.json",
                "lottie/system-solid-2599-clipboard-code-hover-integration.json"
        };

        for (int i = 0; i < titles.length; i++) {
            TabLayout.Tab tab = tabLayoutMode.newTab();
            View tabView = LayoutInflater.from(context).inflate(R.layout.item_tab_media_mode, null);
            LottieAnimationView tabLottie = tabView.findViewById(R.id.tab_lottie);
            TextView tabText = tabView.findViewById(R.id.tab_text);

            tabLottie.setAnimation(lottieFiles[i]);
            tabText.setText(titles[i]);

            boolean isSelected = (i == currentTab);
            int color = isSelected
                    ? MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE)
                    : MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
            tabText.setTextColor(color);
            LottieHelper.tint(tabLottie, color);

            tab.setCustomView(tabView);
            tabLayoutMode.addTab(tab, isSelected);
        }

        tabLayoutMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateTabVisuals();
                TransitionHelper.beginTransition(rootLayout, TransitionHelper.DURATION_NORMAL);
                updateModeUi();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                updateTabVisuals();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                View cv = tab.getCustomView();
                if (cv != null) {
                    LottieAnimationView lottie = cv.findViewById(R.id.tab_lottie);
                    if (lottie != null) {
                        lottie.setFrame(0);
                        lottie.playAnimation();
                    }
                }
            }
        });
    }

    private void updateTabVisuals() {
        if (tabLayoutMode == null) return;
        int onPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        int onSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);

        for (int i = 0; i < tabLayoutMode.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayoutMode.getTabAt(i);
            if (tab != null && tab.getCustomView() != null) {
                View cv = tab.getCustomView();
                LottieAnimationView lottie = cv.findViewById(R.id.tab_lottie);
                TextView tv = cv.findViewById(R.id.tab_text);
                boolean isSelected = (i == tabLayoutMode.getSelectedTabPosition());

                int color = isSelected ? onPrimary : onSurfaceVariant;
                if (tv != null) tv.setTextColor(color);
                if (lottie != null) {
                    LottieHelper.tint(lottie, color);
                    if (isSelected) {
                        lottie.setFrame(0);
                        lottie.playAnimation();
                    }
                }
            }
        }
    }

    private void loadDefaultSettings() {
        boolean isAudioDefault = "audio".equalsIgnoreCase(DownloaderPrefs.getDefaultType(context));
        currentTab = isAudioDefault ? 1 : 0;
        if (tabLayoutMode != null) {
            TabLayout.Tab tab = tabLayoutMode.getTabAt(currentTab);
            if (tab != null) tab.select();
        }

        embedThumbnail = DownloaderPrefs.isEmbedThumbnail(context);
        embedSubtitles = DownloaderPrefs.isEmbedSubtitles(context);
        splitChapters = DownloaderPrefs.isSplitChapters(context);

        selectedContainer = isAudioDefault ? "MP3" : "MP4";
        selectedVideoQuality = DownloaderPrefs.getDefaultVideoQuality(context);
        selectedAudioFormat = DownloaderPrefs.getDefaultAudioFormat(context);

        updateModeUi();
    }

    private void setupListeners() {
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Sub-sheet trigger cards
        if (cardThumbnailContainer != null) {
            cardThumbnailContainer.setOnClickListener(v -> {
                if (layoutDownloadingState == null || layoutDownloadingState.getVisibility() != View.VISIBLE) {
                    showEditTitleDialog();
                }
            });
        }
        if (cardContainer != null) {
            cardContainer.setOnClickListener(v -> showContainerSelectorSheet());
        }
        if (cardQuality != null) {
            cardQuality.setOnClickListener(v -> showQualitySelectorSheet());
        }
        if (btnTurboSpeed != null) {
            btnTurboSpeed.setOnClickListener(v -> {
                if (lottieTurboSpeed != null) {
                    lottieTurboSpeed.setFrame(0);
                    lottieTurboSpeed.playAnimation();
                }
                showSpeedOptionsSheet();
            });
        }

        // Adjust chips
        if (btnChipCut != null) {
            btnChipCut.setOnClickListener(v -> showCutDialog());
        }
        if (btnChipThumbnail != null) {
            btnChipThumbnail.setOnClickListener(v -> showThumbnailOptionsSheet());
        }
        if (btnChipChapters != null) {
            btnChipChapters.setOnClickListener(v -> showChaptersOptionsSheet());
        }
        if (btnChipSubtitles != null) {
            btnChipSubtitles.setOnClickListener(v -> showSubtitlesOptionsSheet());
        }
        if (btnChipRecode != null) {
            btnChipRecode.setOnClickListener(v -> showRecodeOptionsSheet());
        }
        if (btnChipFilename != null) {
            btnChipFilename.setOnClickListener(v -> showFilenameTemplateSheet());
        }
        if (btnChipExtraCommand != null) {
            btnChipExtraCommand.setOnClickListener(v -> showExtraCommandSheet());
        }

        if (tvCommandPreview != null) {
            tvCommandPreview.setOnClickListener(v -> copyCommandToClipboard());
        }
        View tapToCopyBadge = rootView.findViewById(R.id.tv_tap_to_copy_badge);
        if (tapToCopyBadge != null) {
            tapToCopyBadge.setOnClickListener(v -> copyCommandToClipboard());
        }

        if (btnDownloadNow != null) {
            btnDownloadNow.setOnClickListener(v -> startDownload());
        }

        if (btnDownloadCancel != null) {
            btnDownloadCancel.setOnClickListener(v -> {
                MediaDownloadService.cancelDownload(context);
                Toast.makeText(context, context.getString(R.string.btn_cancel), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        if (btnDownloadBackground != null) {
            btnDownloadBackground.setOnClickListener(v -> {
                MediaDownloadService.setDownloadEventListener(null);
                dialog.dismiss();
            });
        }
    }

    private void updateModeUi() {
        boolean isAudio = (currentTab == 1);
        boolean isCommand = (currentTab == 2);

        if (isCommand) {
            layoutCommandView.setVisibility(View.VISIBLE);
            tvCommandPreview.setText(buildGeneratedCommand());
            if (btnDownloadNow != null) {
                btnDownloadNow.setVisibility(View.GONE);
            }
        } else {
            layoutCommandView.setVisibility(View.GONE);
            if (btnDownloadNow != null) {
                btnDownloadNow.setVisibility(View.VISIBLE);
            }
        }

        if (isAudio) {
            tvQualitySectionLabel.setText(context.getString(R.string.label_audio_quality));
            tvAdjustSectionLabel.setText(context.getString(R.string.label_adjust_audio));
            tvContainer.setText(selectedAudioFormat.toUpperCase());
            tvQualityTitle.setText("High Quality Audio (" + selectedAudioBitrate + " kbps)");
        } else {
            tvQualitySectionLabel.setText(context.getString(R.string.label_video_quality));
            tvAdjustSectionLabel.setText(context.getString(R.string.label_adjust_video));
            tvContainer.setText(selectedContainer);
            tvQualityTitle.setText(selectedVideoQuality.equals("best") ? "Highest Quality (Best)" : selectedVideoQuality + "p Full HD");
        }

        updateEstimatedSizeAndStorage();
        updateChipsVisualState();
    }

    private void updateChipsVisualState() {
        updateChipState(btnChipCut, !cutStartTime.isEmpty() && !cutEndTime.isEmpty());
        updateChipState(btnChipThumbnail, saveSeparateThumbnail || !embedThumbnail);
        updateChipState(btnChipChapters, splitChapters || !embedChapters);
        updateChipState(btnChipSubtitles, embedSubtitles || burnSubtitles);
        updateChipState(btnChipRecode, recodeVideo || compatibleH264);
        updateChipState(btnChipFilename, !customFilenameTemplate.equals("%(uploader)s - %(title)s"));
        updateChipState(btnChipExtraCommand, !extraCommands.isEmpty());
    }

    private void updateChipState(MaterialButton chip, boolean isActive) {
        if (chip == null) return;
        if (isActive) {
            int bgTint = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorPrimaryContainer, Color.DKGRAY);
            int fgColor = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.WHITE);
            chip.setBackgroundTintList(ColorStateList.valueOf(bgTint));
            chip.setTextColor(fgColor);
            chip.setIconTint(ColorStateList.valueOf(fgColor));
            chip.setStrokeWidth(0);
            chip.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
        } else {
            chip.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            int onSurface = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);
            int outline = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOutlineVariant, Color.GRAY);
            chip.setTextColor(onSurface);
            chip.setIconTint(ColorStateList.valueOf(MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.LTGRAY)));
            chip.setStrokeWidth(Math.round(1 * chip.getResources().getDisplayMetrics().density));
            chip.setStrokeColor(ColorStateList.valueOf(outline));
        }
    }

    private String estimateVideoSize(long duration, String quality) {
        int mbPerMinute;
        if ("2160".equals(quality)) mbPerMinute = 80;
        else if ("1440".equals(quality)) mbPerMinute = 40;
        else if ("1080".equals(quality) || "best".equals(quality)) mbPerMinute = 20;
        else if ("720".equals(quality)) mbPerMinute = 10;
        else if ("480".equals(quality)) mbPerMinute = 5;
        else mbPerMinute = 3;
        long mb = (duration * mbPerMinute) / 60;
        return mb < 1024 ? mb + " MB" : String.format(Locale.US, "%.1f GB", mb / 1024.0);
    }

    private String estimateAudioSize(long duration, String bitrate) {
        int kbps = 320;
        try {
            kbps = Integer.parseInt(bitrate);
            if (kbps <= 0) kbps = 320;
        } catch (Exception ignored) {}
        long mb = (duration * (kbps / 8)) / 1024;
        return mb + " MB";
    }

    private void updateEstimatedSizeAndStorage() {
        long duration = cachedVideoInfo != null ? cachedVideoInfo.getDuration() : 180;
        if (duration <= 0) duration = 180;

        String estimatedStr = (currentTab == 1)
                ? estimateAudioSize(duration, selectedAudioBitrate)
                : estimateVideoSize(duration, selectedVideoQuality);
        tvEstimatedSize.setText("~" + estimatedStr);

        if (tvTurboBadge != null) {
            tvTurboBadge.setText(concurrentFragments + "x");
        }
    }

    private void loadMediaDetails() {
        if (!DownloaderEngine.getInstance().isCoreInstalled(context)) {
            if (dialog != null) {
                dialog.setOnDismissListener(null);
                dialog.dismissInstant();
            }
            CoreInstallerBottomSheet installer = new CoreInstallerBottomSheet(context, () -> {
                MediaDownloaderBottomSheet newSheet = new MediaDownloaderBottomSheet(context, mediaUrl);
                if (onDismissCallback != null) {
                    newSheet.setOnDismissListener(d -> onDismissCallback.run());
                }
                newSheet.show();
            });
            installer.setOnDismissListener(d -> {
                if (onDismissCallback != null) {
                    onDismissCallback.run();
                }
            });
            installer.show();
            return;
        }

        layoutLoading.setVisibility(View.VISIBLE);
        layoutContent.setVisibility(View.GONE);

        DownloaderEngine.getInstance().fetchVideoInfo(context, mediaUrl, new DownloaderEngine.InfoCallback() {
            @Override
            public void onSuccess(VideoInfo info) {
                cachedVideoInfo = info;
                mainHandler.post(() -> {
                    TransitionHelper.beginTransition(rootLayout, TransitionHelper.DURATION_NORMAL);
                    layoutLoading.setVisibility(View.GONE);
                    layoutContent.setVisibility(View.VISIBLE);
                    populateDetails(info);
                });
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    String msg = cleanErrorMessage(e != null ? e.getMessage() : "Unknown error");
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    if (dialog != null) dialog.dismiss();
                });
            }
        });
    }

    private String cleanErrorMessage(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "فشل في تحليل الرابط، يرجى المحاولة لاحقاً";
        if (raw.contains("ERROR:")) {
            String after = raw.substring(raw.indexOf("ERROR:") + 6).trim();
            if (after.contains("\n")) {
                after = after.substring(0, after.indexOf("\n")).trim();
            }
            return after;
        }
        if (raw.contains("Traceback")) {
            String[] lines = raw.split("\n");
            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();
                if (!line.isEmpty() && !line.startsWith("File ") && !line.startsWith("Traceback")) {
                    return line;
                }
            }
        }
        return "فشل في قراءة الرابط: " + raw;
    }

    private void populateDetails(VideoInfo info) {
        if (info == null) return;
        tvTitle.setText(info.getTitle() != null ? info.getTitle() : "Media Content");
        tvUploader.setText(info.getUploader() != null ? info.getUploader() : "Unknown Author");

        if (info.getDuration() > 0) {
            tvDurationBadge.setText(MediaUtils.formatDuration(info.getDuration()));
            tvDurationBadge.setVisibility(View.VISIBLE);
        } else {
            tvDurationBadge.setVisibility(View.GONE);
        }

        updateModeUi();

        if (info.getThumbnail() != null && !info.getThumbnail().isEmpty()) {
            ThumbnailLoader.getInstance().load(info.getThumbnail(), ivThumbnail, R.drawable.ic_movie_outline);
        }
    }

    // ==========================================
    // Cut & Trim Dialog
    // ==========================================

    private void showCutDialog() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_cut_media, null);
        BottomSheetHelper.applyTheme(context, sheetView);

        int totalSeconds = 180;
        if (cachedVideoInfo != null && cachedVideoInfo.getDuration() > 0) {
            totalSeconds = (int) cachedVideoInfo.getDuration();
        } else if (tvDurationBadge != null && tvDurationBadge.getText() != null) {
            int parsed = parseTimeStringToSeconds(tvDurationBadge.getText().toString(), 0);
            if (parsed > 0) totalSeconds = parsed;
        }

        TextView tvCutStart = sheetView.findViewById(R.id.tv_cut_start);
        TextView tvCutEnd = sheetView.findViewById(R.id.tv_cut_end);
        TextView tvCutDuration = sheetView.findViewById(R.id.tv_cut_duration);
        RangeSlider sliderCutRange = sheetView.findViewById(R.id.slider_cut_range);
        View btnCloseCut = sheetView.findViewById(R.id.btn_close_cut);
        MaterialButton btnApplyCut = sheetView.findViewById(R.id.btn_apply_cut);
        MaterialButton btnResetCut = sheetView.findViewById(R.id.btn_reset_cut);
        MaterialButton chip30s = sheetView.findViewById(R.id.chip_preset_30s);
        MaterialButton chip60s = sheetView.findViewById(R.id.chip_preset_60s);
        MaterialButton chipFull = sheetView.findViewById(R.id.chip_preset_full);

        sliderCutRange.setValueFrom(0f);
        sliderCutRange.setValueTo((float) Math.max(totalSeconds, 10));
        sliderCutRange.setStepSize(1f);

        int initialStart = parseTimeStringToSeconds(cutStartTime, 0);
        int initialEnd = parseTimeStringToSeconds(cutEndTime, totalSeconds);
        if (initialStart < 0) initialStart = 0;
        if (initialEnd <= initialStart || initialEnd > totalSeconds) initialEnd = totalSeconds;

        sliderCutRange.setValues((float) initialStart, (float) initialEnd);
        tvCutStart.setText(formatDurationSeconds(initialStart));
        tvCutEnd.setText(formatDurationSeconds(initialEnd));
        tvCutDuration.setText(context.getString(R.string.label_clip_duration_val, formatDurationSeconds(initialEnd - initialStart)));

        final int finalTotalSeconds = totalSeconds;
        sliderCutRange.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int s = Math.round(values.get(0));
            int e = Math.round(values.get(1));
            tvCutStart.setText(formatDurationSeconds(s));
            tvCutEnd.setText(formatDurationSeconds(e));
            tvCutDuration.setText(context.getString(R.string.label_clip_duration_val, formatDurationSeconds(e - s)));
        });

        if (chip30s != null) {
            chip30s.setOnClickListener(v -> {
                int end30 = Math.min(30, finalTotalSeconds);
                sliderCutRange.setValues(0f, (float) end30);
            });
        }
        if (chip60s != null) {
            chip60s.setOnClickListener(v -> {
                int end60 = Math.min(60, finalTotalSeconds);
                sliderCutRange.setValues(0f, (float) end60);
            });
        }
        if (chipFull != null) {
            chipFull.setOnClickListener(v -> sliderCutRange.setValues(0f, (float) finalTotalSeconds));
        }

        if (btnCloseCut != null) {
            btnCloseCut.setOnClickListener(v -> subSheet.dismiss());
        }

        if (btnResetCut != null) {
            btnResetCut.setOnClickListener(v -> {
                cutStartTime = "";
                cutEndTime = "";
                if (btnChipCut != null) {
                    btnChipCut.setText(context.getString(R.string.chip_cut));
                }
                updateChipsVisualState();
                subSheet.dismiss();
            });
        }

        if (btnApplyCut != null) {
            btnApplyCut.setOnClickListener(v -> {
                List<Float> values = sliderCutRange.getValues();
                int s = Math.round(values.get(0));
                int e = Math.round(values.get(1));
                if (s == 0 && e >= finalTotalSeconds) {
                    cutStartTime = "";
                    cutEndTime = "";
                    if (btnChipCut != null) {
                        btnChipCut.setText(context.getString(R.string.chip_cut));
                    }
                } else {
                    cutStartTime = formatDurationSeconds(s);
                    cutEndTime = formatDurationSeconds(e);
                    if (btnChipCut != null) {
                        btnChipCut.setText(cutStartTime + " - " + cutEndTime);
                    }
                }
                updateChipsVisualState();
                subSheet.dismiss();
            });
        }

        subSheet.setContentView(sheetView);
        subSheet.show();
    }

    private String formatDurationSeconds(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private int parseTimeStringToSeconds(String timeStr, int defaultValue) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Integer.parseInt(parts[2]);
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 1) {
                return Integer.parseInt(parts[0]);
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    // ==========================================
    // Speed & Multi-Thread Selector
    // ==========================================

    private void showSpeedOptionsSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_single_select, null);
        BottomSheetHelper.applyTheme(context, sheetView);

        TextView tvSheetTitle = sheetView.findViewById(R.id.tv_sheet_title);
        if (tvSheetTitle != null) {
            tvSheetTitle.setText(context.getString(R.string.label_speed_mode));
        }
        TextView tvSheetDesc = sheetView.findViewById(R.id.tv_sheet_desc);
        if (tvSheetDesc != null) {
            tvSheetDesc.setText("Select concurrent download chunks");
        }

        // Header Lottie Bolt Play Once
        ImageView ivSheetIcon = sheetView.findViewById(R.id.iv_sheet_icon);
        LottieAnimationView lottieSheetIcon = sheetView.findViewById(R.id.lottie_sheet_icon);
        if (ivSheetIcon != null && lottieSheetIcon != null) {
            ivSheetIcon.setVisibility(View.GONE);
            lottieSheetIcon.setVisibility(View.VISIBLE);
            lottieSheetIcon.setRepeatCount(0);
            lottieSheetIcon.setAnimation("lottie/system-solid-451-bolt-hover-bolt.json");
            LottieHelper.tint(lottieSheetIcon, MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.WHITE));
            lottieSheetIcon.setFrame(0);
            lottieSheetIcon.playAnimation();
        }

        LinearLayout optionsContainer = sheetView.findViewById(R.id.options_container);

        String[][] speedOptions = {
                {"16", "Turbo 16x", "Balanced"},
                {"32", "Max Turbo 32x", "Ultra Fast"},
                {"8", "Standard 8x", "Normal"}
        };

        for (String[] opt : speedOptions) {
            View optView = LayoutInflater.from(context).inflate(R.layout.item_search_engine_option, optionsContainer, false);
            TextView tvName = optView.findViewById(R.id.tv_option_name);
            TextView tvBadge = optView.findViewById(R.id.tv_option_badge);
            View checkMark = optView.findViewById(R.id.check_mark);
            View container = optView.findViewById(R.id.option_container);

            tvName.setText(opt[1]);
            if (opt[2] != null && !opt[2].isEmpty() && tvBadge != null) {
                tvBadge.setText(opt[2]);
                tvBadge.setVisibility(View.VISIBLE);
            } else if (tvBadge != null) {
                tvBadge.setVisibility(View.GONE);
            }

            boolean isSelected = opt[0].equals(String.valueOf(concurrentFragments));
            checkMark.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_search_option_selected);
                int colorOnPrimaryContainer = MaterialColors.getColor(optView, com.google.android.material.R.attr.colorOnPrimaryContainer);
                tvName.setTextColor(colorOnPrimaryContainer);
            } else {
                container.setBackgroundResource(R.drawable.bg_selectable_rounded);
                int colorOnSurface = MaterialColors.getColor(optView, com.google.android.material.R.attr.colorOnSurface);
                tvName.setTextColor(colorOnSurface);
            }

            optView.setOnClickListener(v -> {
                concurrentFragments = Integer.parseInt(opt[0]);
                updateEstimatedSizeAndStorage();
                subSheet.dismiss();
            });

            optionsContainer.addView(optView);
        }

        subSheet.setContentView(sheetView);
        subSheet.show();
    }

    // ==========================================
    // Selection Sub-Sheets
    // ==========================================

    private void showQualitySelectorSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_single_select, null);
        BottomSheetHelper.applyTheme(context, sheetView);

        TextView tvSheetTitle = sheetView.findViewById(R.id.tv_sheet_title);
        if (tvSheetTitle != null) {
            tvSheetTitle.setText(context.getString(R.string.dialog_title_quality));
        }
        TextView tvSheetDesc = sheetView.findViewById(R.id.tv_sheet_desc);
        if (tvSheetDesc != null) {
            tvSheetDesc.setText(currentTab == 1 ? context.getString(R.string.label_audio_quality) : context.getString(R.string.label_video_quality));
        }

        LinearLayout optionsContainer = sheetView.findViewById(R.id.options_container);

        if (currentTab == 1) {
            String[][] audioOptions = {
                    {"320", "320 kbps (High Quality MP3)"},
                    {"256", "256 kbps (AAC M4A)"},
                    {"192", "192 kbps (Standard MP3)"},
                    {"0", "Lossless (FLAC)"}
            };
            for (String[] opt : audioOptions) {
                View optView = LayoutInflater.from(context).inflate(R.layout.item_search_engine_option, optionsContainer, false);
                TextView tvName = optView.findViewById(R.id.tv_option_name);
                View checkMark = optView.findViewById(R.id.check_mark);
                View container = optView.findViewById(R.id.option_container);

                tvName.setText(opt[1]);
                boolean isSelected = opt[0].equals(selectedAudioBitrate);
                checkMark.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

                if (isSelected) {
                    container.setBackgroundResource(R.drawable.bg_search_option_selected);
                } else {
                    container.setBackgroundResource(R.drawable.bg_selectable_rounded);
                }

                optView.setOnClickListener(v -> {
                    selectedAudioBitrate = opt[0];
                    updateModeUi();
                    subSheet.dismiss();
                });

                optionsContainer.addView(optView);
            }
        } else {
            String[][] videoOptions = {
                    {"best", "Highest Quality (Auto Best)"},
                    {"2160", "2160p 4K Ultra HD"},
                    {"1440", "1440p 2K QHD"},
                    {"1080", "1080p Full HD"},
                    {"720", "720p HD"},
                    {"480", "480p SD"},
                    {"360", "360p Data Saver"}
            };
            for (String[] opt : videoOptions) {
                View optView = LayoutInflater.from(context).inflate(R.layout.item_search_engine_option, optionsContainer, false);
                TextView tvName = optView.findViewById(R.id.tv_option_name);
                View checkMark = optView.findViewById(R.id.check_mark);
                View container = optView.findViewById(R.id.option_container);

                tvName.setText(opt[1]);
                boolean isSelected = opt[0].equals(selectedVideoQuality);
                checkMark.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

                if (isSelected) {
                    container.setBackgroundResource(R.drawable.bg_search_option_selected);
                } else {
                    container.setBackgroundResource(R.drawable.bg_selectable_rounded);
                }

                optView.setOnClickListener(v -> {
                    selectedVideoQuality = opt[0];
                    updateModeUi();
                    subSheet.dismiss();
                });

                optionsContainer.addView(optView);
            }
        }

        subSheet.setContentView(sheetView);
        subSheet.show();
    }

    private void showContainerSelectorSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_single_select, null);
        BottomSheetHelper.applyTheme(context, sheetView);

        TextView tvSheetTitle = sheetView.findViewById(R.id.tv_sheet_title);
        if (tvSheetTitle != null) {
            tvSheetTitle.setText(context.getString(R.string.label_container));
        }
        TextView tvSheetDesc = sheetView.findViewById(R.id.tv_sheet_desc);
        if (tvSheetDesc != null) {
            tvSheetDesc.setText(currentTab == 1 ? "Audio Output Formats" : "Video Output Formats");
        }

        LinearLayout optionsContainer = sheetView.findViewById(R.id.options_container);

        String[] formats = currentTab == 1
                ? new String[]{"MP3", "M4A", "FLAC", "OPUS", "WAV"}
                : new String[]{"MP4", "MKV", "WebM"};

        for (String fmt : formats) {
            View optView = LayoutInflater.from(context).inflate(R.layout.item_search_engine_option, optionsContainer, false);
            TextView tvName = optView.findViewById(R.id.tv_option_name);
            View checkMark = optView.findViewById(R.id.check_mark);
            View container = optView.findViewById(R.id.option_container);

            tvName.setText(fmt);
            boolean isSelected = currentTab == 1 ? fmt.equalsIgnoreCase(selectedAudioFormat) : fmt.equalsIgnoreCase(selectedContainer);
            checkMark.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);

            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_search_option_selected);
            } else {
                container.setBackgroundResource(R.drawable.bg_selectable_rounded);
            }

            optView.setOnClickListener(v -> {
                if (currentTab == 1) {
                    selectedAudioFormat = fmt.toLowerCase();
                } else {
                    selectedContainer = fmt;
                }
                updateModeUi();
                subSheet.dismiss();
            });

            optionsContainer.addView(optView);
        }

        subSheet.setContentView(sheetView);
        subSheet.show();
    }

    private void showThumbnailOptionsSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.dialog_title_thumbnail));

        createSwitchItem(content, context.getString(R.string.opt_embed_thumbnail_desc), embedThumbnail, (btn, checked) -> {
            embedThumbnail = checked;
            updateChipsVisualState();
        });
        createSwitchItem(content, context.getString(R.string.opt_save_thumbnail_image), saveSeparateThumbnail, (btn, checked) -> {
            saveSeparateThumbnail = checked;
            updateChipsVisualState();
        });

        addDismissButton(content, subSheet);
        subSheet.setContentView(content);
        subSheet.show();
    }

    private void showChaptersOptionsSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.dialog_title_chapters));

        createSwitchItem(content, context.getString(R.string.opt_split_chapters_desc), splitChapters, (btn, checked) -> {
            splitChapters = checked;
            updateChipsVisualState();
        });
        createSwitchItem(content, context.getString(R.string.opt_embed_chapters_desc), embedChapters, (btn, checked) -> {
            embedChapters = checked;
            updateChipsVisualState();
        });

        addDismissButton(content, subSheet);
        subSheet.setContentView(content);
        subSheet.show();
    }

    private void showSubtitlesOptionsSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.dialog_title_subtitles));

        createSwitchItem(content, context.getString(R.string.opt_embed_subtitles_desc), embedSubtitles, (btn, checked) -> {
            embedSubtitles = checked;
            updateChipsVisualState();
        });
        createSwitchItem(content, context.getString(R.string.opt_burn_subtitles_desc), burnSubtitles, (btn, checked) -> {
            burnSubtitles = checked;
            updateChipsVisualState();
        });

        addDismissButton(content, subSheet);
        subSheet.setContentView(content);
        subSheet.show();
    }

    private void showRecodeOptionsSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.dialog_title_recode));

        createSwitchItem(content, context.getString(R.string.chip_recode), recodeVideo, (btn, checked) -> {
            recodeVideo = checked;
            updateChipsVisualState();
        });
        createSwitchItem(content, context.getString(R.string.opt_recode_compatible_desc), compatibleH264, (btn, checked) -> {
            compatibleH264 = checked;
            updateChipsVisualState();
        });

        addDismissButton(content, subSheet);
        subSheet.setContentView(content);
        subSheet.show();
    }

    private void showFilenameTemplateSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.dialog_title_filename));

        EditText et = new EditText(context);
        et.setText(customFilenameTemplate);
        applyDialogEditTextStyle(et);
        content.addView(et);

        MaterialButton btnSave = new MaterialButton(context);
        btnSave.setText(context.getString(R.string.btn_ok));
        btnSave.setOnClickListener(v -> {
            customFilenameTemplate = et.getText().toString();
            updateChipsVisualState();
            subSheet.dismiss();
        });
        content.addView(btnSave);

        subSheet.setContentView(content);
        subSheet.show();
    }

    private void showExtraCommandSheet() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.dialog_title_extra_command));

        EditText et = new EditText(context);
        et.setHint("--sponsorblock-mark all");
        et.setText(extraCommands);
        applyDialogEditTextStyle(et);
        content.addView(et);

        MaterialButton btnSave = new MaterialButton(context);
        btnSave.setText(context.getString(R.string.btn_ok));
        btnSave.setOnClickListener(v -> {
            extraCommands = et.getText().toString();
            updateChipsVisualState();
            subSheet.dismiss();
        });
        content.addView(btnSave);

        subSheet.setContentView(content);
        subSheet.show();
    }

    private void showEditTitleDialog() {
        FloatingBottomSheet subSheet = new FloatingBottomSheet(context);
        LinearLayout content = createSubSheetContainer(context.getString(R.string.label_title));

        EditText et = new EditText(context);
        et.setText(tvTitle.getText());
        applyDialogEditTextStyle(et);
        content.addView(et);

        MaterialButton btnSave = new MaterialButton(context);
        btnSave.setText(context.getString(R.string.btn_ok));
        btnSave.setOnClickListener(v -> {
            customTitle = et.getText().toString();
            tvTitle.setText(customTitle);
            subSheet.dismiss();
        });
        content.addView(btnSave);

        subSheet.setContentView(content);
        subSheet.show();
    }

    // ==========================================
    // UI Helpers
    // ==========================================

    private void applyDialogEditTextStyle(EditText et) {
        int onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        int onSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);
        et.setTextColor(onSurface);
        et.setHintTextColor(onSurfaceVariant);
        et.setBackgroundResource(R.drawable.bg_input_field);
        et.setPadding(32, 24, 32, 24);
    }

    private LinearLayout createSubSheetContainer(String title) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.bg_bottom_sheet_floating);
        BottomSheetHelper.applyTheme(context, layout);
        layout.setPadding(48, 32, 48, 48);

        View handle = new View(context);
        LinearLayout.LayoutParams hParams = new LinearLayout.LayoutParams(120, 12);
        hParams.gravity = Gravity.CENTER_HORIZONTAL;
        hParams.bottomMargin = 32;
        handle.setLayoutParams(hParams);
        handle.setBackgroundResource(R.drawable.bg_drag_handle);
        layout.addView(handle);

        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(18);
        int onSurfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        tv.setTextColor(onSurfaceColor);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 0, 32);
        layout.addView(tv);

        return layout;
    }

    private MaterialSwitch createSwitchItem(LinearLayout container, String title, boolean isChecked, android.widget.CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);

        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(15);
        int onSurfaceColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        tv.setTextColor(onSurfaceColor);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        MaterialSwitch sw = new MaterialSwitch(context);
        sw.setChecked(isChecked);
        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);

        container.addView(row);
        return sw;
    }

    private void addDismissButton(LinearLayout container, Dialog subSheet) {
        MaterialButton btn = new MaterialButton(context);
        btn.setText(context.getString(R.string.btn_ok));
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setOnClickListener(v -> subSheet.dismiss());
        container.addView(btn);
    }

    private void copyCommandToClipboard() {
        if (tvCommandPreview == null) return;
        String cmd = tvCommandPreview.getText().toString();
        if (cmd.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Universal Downloader CMD", cmd);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Command copied to clipboard! 📋", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildGeneratedCommand() {
        boolean isAudio = (currentTab == 1);
        StringBuilder ytDlpArgs = new StringBuilder("yt-dlp ");
        if (isAudio) {
            ytDlpArgs.append("-x --audio-format ").append(selectedAudioFormat).append(" --audio-quality 0 ");
        } else {
            ytDlpArgs.append("-f \"bestvideo[height<=").append(selectedVideoQuality).append("]+bestaudio/best\" ");
        }
        ytDlpArgs.append("-N ").append(concurrentFragments).append(" --concurrent-fragments ").append(concurrentFragments).append(" ");
        if (!cutStartTime.isEmpty() && !cutEndTime.isEmpty()) {
            ytDlpArgs.append("--download-sections \"*").append(cutStartTime).append("-").append(cutEndTime).append("\" ");
        }
        if (embedThumbnail) ytDlpArgs.append("--embed-thumbnail ");
        if (embedSubtitles) ytDlpArgs.append("--embed-subs ");
        if (splitChapters) ytDlpArgs.append("--split-chapters ");
        if (!extraCommands.isEmpty()) ytDlpArgs.append(extraCommands).append(" ");
        ytDlpArgs.append("\"").append(mediaUrl).append("\"");

        // Universal Standalone Script / Command checking and installing requirements on any system
        StringBuilder sb = new StringBuilder();
        sb.append("command -v yt-dlp >/dev/null 2>&1 || pip install --upgrade yt-dlp 2>/dev/null; \\\n");
        sb.append("command -v ffmpeg >/dev/null 2>&1 || (sudo apt-get install -y ffmpeg 2>/dev/null || brew install ffmpeg 2>/dev/null || pkg install -y ffmpeg 2>/dev/null); \\\n");
        sb.append(ytDlpArgs);
        return sb.toString();
    }

    private void startDownload() {
        // Prepare download options
        DownloadOptions options = new DownloadOptions(mediaUrl);
        options.setType(currentTab == 1 ? DownloadOptions.Type.AUDIO : DownloadOptions.Type.VIDEO);
        if (cachedVideoInfo != null) {
            options.setUploader(cachedVideoInfo.getUploader());
        }

        options.setEmbedThumbnail(embedThumbnail);
        options.setEmbedSubtitles(embedSubtitles);
        options.setSplitChapters(splitChapters);
        options.setEmbedMetadata(true);
        options.setConcurrentFragments(concurrentFragments);

        if (!cutStartTime.isEmpty() && !cutEndTime.isEmpty()) {
            options.setDownloadSections("*" + cutStartTime + "-" + cutEndTime);
        }

        if (currentTab == 1) {
            options.setAudioFormat(selectedAudioFormat);
            options.setAudioQuality(selectedAudioBitrate);
        } else {
            options.setVideoQuality(selectedVideoQuality);
            if (selectedContainer.contains("MKV")) {
                options.setVideoFormat("mkv");
            } else if (selectedContainer.contains("WebM")) {
                options.setVideoFormat("webm");
            } else {
                options.setVideoFormat("mp4");
            }
        }

        String title = customTitle != null ? customTitle : (cachedVideoInfo != null && cachedVideoInfo.getTitle() != null ? cachedVideoInfo.getTitle() : context.getString(R.string.nav_media_download));

        // Smooth transition to Downloading State
        TransitionHelper.beginTransition(rootLayout, TransitionHelper.DURATION_NORMAL);
        if (tabLayoutMode != null) tabLayoutMode.setVisibility(View.GONE);
        if (layoutOptionsSection != null) layoutOptionsSection.setVisibility(View.GONE);
        if (layoutDownloadingState != null) layoutDownloadingState.setVisibility(View.VISIBLE);

        if (progressDownload != null) {
            progressDownload.setIndeterminate(true);
        }
        if (tvDownloadProgressText != null) {
            tvDownloadProgressText.setText("Connecting & preparing download...");
        }

        // Slightly shrink thumbnail card
        if (cardThumbnailContainer != null) {
            ViewGroup.LayoutParams lp = cardThumbnailContainer.getLayoutParams();
            if (lp != null) {
                lp.height = Math.round(135 * context.getResources().getDisplayMetrics().density);
                cardThumbnailContainer.setLayoutParams(lp);
            }
        }

        // Start Foreground Download Service
        MediaDownloadService.startDownload(context, options, title);

        // Attach listener
        MediaDownloadService.setDownloadEventListener(new MediaDownloadService.DownloadEventListener() {
            @Override
            public void onProgress(String processId, float progress, long etaInSeconds, String line) {
                if (progress <= 0) {
                    if (progressDownload != null) {
                        progressDownload.setIndeterminate(true);
                    }
                    if (tvDownloadProgressText != null) {
                        tvDownloadProgressText.setText("Connecting & preparing download...");
                    }
                } else {
                    if (progressDownload != null) {
                        progressDownload.setIndeterminate(false);
                        progressDownload.setProgress((int) progress);
                    }
                    if (tvDownloadProgressText != null) {
                        String etaStr = etaInSeconds > 0 ? " • " + MediaUtils.formatDuration(etaInSeconds) + " left" : "";
                        tvDownloadProgressText.setText(String.format(Locale.US, "Downloading... %.1f%%%s", progress, etaStr));
                    }
                }
            }

            @Override
            public void onComplete(String processId, File downloadedFile) {
                onDownloadCompleteAnimation();
            }

            @Override
            public void onError(String processId, Exception e) {
                if (tvDownloadProgressText != null) {
                    tvDownloadProgressText.setText("Download failed: " + e.getMessage());
                }
            }
        });
    }

    private void setupSuccessLottieColors() {
        if (lottieSuccess == null) return;
        int circleColor = MaterialColors.getColor(rootLayout, androidx.appcompat.R.attr.colorPrimary, Color.parseColor("#4CAF50"));
        int checkColor = Color.WHITE;

        lottieSuccess.addValueCallback(
                new com.airbnb.lottie.model.KeyPath("Accept", "Layer 4", "**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                frameInfo -> new android.graphics.PorterDuffColorFilter(circleColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
        );
        lottieSuccess.addValueCallback(
                new com.airbnb.lottie.model.KeyPath("Accept", "circle", "**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                frameInfo -> new android.graphics.PorterDuffColorFilter(circleColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
        );
        lottieSuccess.addValueCallback(
                new com.airbnb.lottie.model.KeyPath("Accept", "Layer 3", "**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                frameInfo -> new android.graphics.PorterDuffColorFilter(checkColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
        );
    }

    private void onDownloadCompleteAnimation() {
        if (rootLayout == null) return;
        TransitionHelper.beginTransition(rootLayout, TransitionHelper.DURATION_NORMAL);

        if (layoutDownloadingState != null) layoutDownloadingState.setVisibility(View.GONE);
        if (layoutThumbnailOverlay != null) layoutThumbnailOverlay.setVisibility(View.GONE);
        if (scrimOverlay != null) scrimOverlay.setVisibility(View.GONE);

        if (ivThumbnail != null) {
            ivThumbnail.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        ivThumbnail.setVisibility(View.GONE);
                        if (lottieSuccess != null) {
                            lottieSuccess.setVisibility(View.VISIBLE);
                            lottieSuccess.setScaleX(1f);
                            lottieSuccess.setScaleY(1f);
                            lottieSuccess.setAlpha(1f);
                            setupSuccessLottieColors();
                            lottieSuccess.setMinAndMaxFrame(60, 140);
                            lottieSuccess.setFrame(60);
                            lottieSuccess.playAnimation();
                        }
                    })
                    .start();
        } else if (lottieSuccess != null) {
            lottieSuccess.setVisibility(View.VISIBLE);
            setupSuccessLottieColors();
            lottieSuccess.setMinAndMaxFrame(60, 140);
            lottieSuccess.setFrame(60);
            lottieSuccess.playAnimation();
        }

        // Auto dismiss after 2.8 seconds
        mainHandler.postDelayed(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 2800);
    }
}
