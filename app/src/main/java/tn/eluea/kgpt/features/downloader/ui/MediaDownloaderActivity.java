/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloaderEngine;
import tn.eluea.kgpt.features.downloader.core.DownloaderPrefs;
import tn.eluea.kgpt.features.downloader.core.MediaUtils;

public class MediaDownloaderActivity extends AppCompatActivity {

    private EditText etDirectUrl;
    private MaterialButton btnPasteUrl, btnStartDirectDownload;
    private View rowDownloadDir;
    private TextView tvCurrentDownloadDir, tvCoreVersion, tvCoreBadge;
    private MaterialSwitch switchGroupCreator, switchGroupByApp, switchPrefThumbnail, switchPrefSubtitles, switchPrefChapters;

    // Accordion Direct Download
    private View headerDirectDownload, layoutAccordionContent;
    private android.widget.ImageView ivAccordionArrow;

    // Engine Core Card
    private com.airbnb.lottie.LottieAnimationView lottieCoreStatus, lottieCoreCheckingSpinner;
    private FrameLayout containerCoreAction;
    private View layoutCoreCheckingProgress, layoutCoreDownloadingProgress;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressCoreDownload;
    private MaterialButton btnCoreAction;
    private final java.util.concurrent.ExecutorService coreExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    // Native In-App Download Hooks
    private View sectionNativeHooks, rowHookYoutube, rowHookYtmusic, dividerHooks;
    private android.widget.ImageView ivIconYoutube, ivIconYtmusic;
    private MaterialSwitch switchHookYoutube, switchHookYtmusic;

    private final ActivityResultLauncher<Uri> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );
                    } catch (Throwable ignored) {}

                    // Store the tree URI (not uri.getPath() — a /tree/... path is
                    // not a filesystem location and mkdirs silently failed)
                    DownloaderPrefs.setCustomTreeUri(this, uri.toString());
                    updateDirDisplay();
                }
            }
    );

    private void applyToggleVisualState(View iconView, View labelView, boolean enabled, boolean animate) {
        float targetAlpha = enabled ? 1.0f : 0.38f;
        float targetScale = enabled ? 1.0f : 0.92f;
        float labelAlpha = enabled ? 1.0f : 0.55f;

        if (animate) {
            if (iconView != null) {
                iconView.animate()
                        .alpha(targetAlpha)
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(260)
                        .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                        .start();
            }
            if (labelView != null) {
                labelView.animate()
                        .alpha(labelAlpha)
                        .setDuration(260)
                        .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                        .start();
            }
        } else {
            if (iconView != null) {
                iconView.setAlpha(targetAlpha);
                iconView.setScaleX(targetScale);
                iconView.setScaleY(targetScale);
            }
            if (labelView != null) {
                labelView.setAlpha(labelAlpha);
            }
        }
    }

    private final tn.eluea.kgpt.util.LSPosedHelper.ScopeListener scopeListener = (packageName, inScope) -> {
        runOnUiThread(() -> {
            if ("com.google.android.youtube".equals(packageName)) {
                if (switchHookYoutube != null && switchHookYoutube.isChecked() != inScope) {
                    switchHookYoutube.setChecked(inScope);
                }
                DownloaderPrefs.setYouTubeHookEnabled(this, inScope);
                View labelYoutube = findViewById(R.id.layout_text_youtube);
                applyToggleVisualState(ivIconYoutube, labelYoutube, inScope, true);
            } else if ("com.google.android.apps.youtube.music".equals(packageName)) {
                if (switchHookYtmusic != null && switchHookYtmusic.isChecked() != inScope) {
                    switchHookYtmusic.setChecked(inScope);
                }
                DownloaderPrefs.setYTMusicHookEnabled(this, inScope);
                View labelYtmusic = findViewById(R.id.layout_text_ytmusic);
                applyToggleVisualState(ivIconYtmusic, labelYtmusic, inScope, true);
            }
        });
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_downloader);

        applyAmoledIfNeeded();
        initViews();
        loadSavedPreferences();
        setupListeners();

        // Play Lottie once on page entry
        if (lottieCoreStatus != null) {
            lottieCoreStatus.setRepeatCount(0);
            lottieCoreStatus.playAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tn.eluea.kgpt.util.LSPosedHelper.addScopeListener(scopeListener);
        loadSavedPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tn.eluea.kgpt.util.LSPosedHelper.removeScopeListener(scopeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        coreExecutor.shutdown();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnInfoGuide = findViewById(R.id.btn_info_guide);
        if (btnInfoGuide != null) {
            btnInfoGuide.setOnClickListener(v -> showGuideBottomSheet());
        }

        // Accordion Direct Download
        headerDirectDownload = findViewById(R.id.header_direct_download);
        layoutAccordionContent = findViewById(R.id.layout_accordion_content);
        ivAccordionArrow = findViewById(R.id.iv_accordion_arrow);
        etDirectUrl = findViewById(R.id.et_direct_url);
        btnPasteUrl = findViewById(R.id.btn_paste_url);
        btnStartDirectDownload = findViewById(R.id.btn_start_direct_download);

        // Core Card
        tvCoreVersion = findViewById(R.id.tv_core_version);
        tvCoreBadge = findViewById(R.id.tv_core_badge);
        lottieCoreStatus = findViewById(R.id.lottie_core_status);
        containerCoreAction = findViewById(R.id.container_core_action);
        btnCoreAction = findViewById(R.id.btn_core_action);
        layoutCoreCheckingProgress = findViewById(R.id.layout_core_checking_progress);
        lottieCoreCheckingSpinner = findViewById(R.id.lottie_core_checking_spinner);
        layoutCoreDownloadingProgress = findViewById(R.id.layout_core_downloading_progress);
        progressCoreDownload = findViewById(R.id.progress_core_download);

        int colorPrimary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, Color.CYAN);
        if (lottieCoreCheckingSpinner != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieCoreCheckingSpinner, colorPrimary);
        }
        if (lottieCoreStatus != null) {
            tn.eluea.kgpt.util.LottieHelper.tint(lottieCoreStatus, colorPrimary);
        }

        // Storage & Preferences
        rowDownloadDir = findViewById(R.id.row_download_dir);
        tvCurrentDownloadDir = findViewById(R.id.tv_current_download_dir);
        switchGroupByApp = findViewById(R.id.switch_group_by_app);
        switchGroupCreator = findViewById(R.id.switch_group_creator);
        switchPrefThumbnail = findViewById(R.id.switch_pref_thumbnail);
        switchPrefSubtitles = findViewById(R.id.switch_pref_subtitles);
        switchPrefChapters = findViewById(R.id.switch_pref_chapters);

        sectionNativeHooks = findViewById(R.id.section_native_hooks);
        rowHookYoutube = findViewById(R.id.row_hook_youtube);
        rowHookYtmusic = findViewById(R.id.row_hook_ytmusic);
        dividerHooks = findViewById(R.id.divider_hooks);
        ivIconYoutube = findViewById(R.id.iv_icon_youtube);
        ivIconYtmusic = findViewById(R.id.iv_icon_ytmusic);
        switchHookYoutube = findViewById(R.id.switch_hook_youtube);
        switchHookYtmusic = findViewById(R.id.switch_hook_ytmusic);
    }

    private void loadSavedPreferences() {
        updateDirDisplay();
        updateCoreStatusDisplay();
        setupNativeAppHooksDisplay();

        if (switchGroupByApp != null) switchGroupByApp.setChecked(DownloaderPrefs.isGroupByApp(this));
        if (switchGroupCreator != null) switchGroupCreator.setChecked(DownloaderPrefs.isGroupByUploader(this));
        if (switchPrefThumbnail != null) switchPrefThumbnail.setChecked(DownloaderPrefs.isEmbedThumbnail(this));
        if (switchPrefSubtitles != null) switchPrefSubtitles.setChecked(DownloaderPrefs.isEmbedSubtitles(this));
        if (switchPrefChapters != null) switchPrefChapters.setChecked(DownloaderPrefs.isSplitChapters(this));
    }

    private boolean isLSPosedActiveOnDevice() {
        // XposedService binding is the only reliable activation signal under
        // the Modern API (world-readable prefs heuristics produce false
        // positives on devices without LSPosed).
        return tn.eluea.kgpt.util.LSPosedHelper.isLSPosedActive();
    }

    private void setupNativeAppHooksDisplay() {
        if (!isLSPosedActiveOnDevice()) {
            if (sectionNativeHooks != null) {
                sectionNativeHooks.setVisibility(View.GONE);
            }
            return;
        }

        android.content.pm.PackageManager pm = getPackageManager();
        boolean isYtInstalled = false;
        boolean isYtMusicInstalled = false;

        try {
            android.content.pm.ApplicationInfo ytInfo = pm.getApplicationInfo("com.google.android.youtube", 0);
            if (ytInfo != null) {
                isYtInstalled = true;
                android.graphics.drawable.Drawable icon = pm.getApplicationIcon(ytInfo);
                if (ivIconYoutube != null) ivIconYoutube.setImageDrawable(icon);
            }
        } catch (Throwable ignored) {}

        try {
            android.content.pm.ApplicationInfo ytmInfo = pm.getApplicationInfo("com.google.android.apps.youtube.music", 0);
            if (ytmInfo != null) {
                isYtMusicInstalled = true;
                android.graphics.drawable.Drawable icon = pm.getApplicationIcon(ytmInfo);
                if (ivIconYtmusic != null) ivIconYtmusic.setImageDrawable(icon);
            }
        } catch (Throwable ignored) {}

        if (sectionNativeHooks != null) {
            if (isYtInstalled || isYtMusicInstalled) {
                sectionNativeHooks.setVisibility(View.VISIBLE);
                if (rowHookYoutube != null) rowHookYoutube.setVisibility(isYtInstalled ? View.VISIBLE : View.GONE);
                if (rowHookYtmusic != null) rowHookYtmusic.setVisibility(isYtMusicInstalled ? View.VISIBLE : View.GONE);
                if (dividerHooks != null) dividerHooks.setVisibility((isYtInstalled && isYtMusicInstalled) ? View.VISIBLE : View.GONE);
            } else {
                sectionNativeHooks.setVisibility(View.GONE);
            }
        }

        boolean ytInScope = tn.eluea.kgpt.util.LSPosedHelper.isPackageInScope("com.google.android.youtube");
        boolean ytmInScope = tn.eluea.kgpt.util.LSPosedHelper.isPackageInScope("com.google.android.apps.youtube.music");

        boolean ytActive = ytInScope && DownloaderPrefs.isYouTubeHookEnabled(this);
        boolean ytmActive = ytmInScope && DownloaderPrefs.isYTMusicHookEnabled(this);

        if (switchHookYoutube != null) {
            switchHookYoutube.setChecked(ytActive);
        }
        if (switchHookYtmusic != null) {
            switchHookYtmusic.setChecked(ytmActive);
        }

        View labelYoutube = findViewById(R.id.layout_text_youtube);
        View labelYtmusic = findViewById(R.id.layout_text_ytmusic);
        applyToggleVisualState(ivIconYoutube, labelYoutube, ytActive, false);
        applyToggleVisualState(ivIconYtmusic, labelYtmusic, ytmActive, false);
    }

    private void updateDirDisplay() {
        String tree = DownloaderPrefs.getCustomTreeUri(this);
        if (tree != null && tvCurrentDownloadDir != null) {
            // Show a friendly summary of the SAF tree (last segment)
            String seg = android.net.Uri.parse(tree).getLastPathSegment();
            if (seg != null && seg.contains(":")) seg = seg.substring(seg.indexOf(':') + 1);
            tvCurrentDownloadDir.setText("SAF: " + (seg != null ? seg : tree));
            return;
        }
        String path = DownloaderPrefs.getDownloadRootPath(this);
        if (tvCurrentDownloadDir != null) {
            tvCurrentDownloadDir.setText(path);
        }
    }

    private void updateCoreStatusDisplay() {
        boolean installed = DownloaderEngine.getInstance().isCoreInstalled(this);
        if (tvCoreVersion != null) {
            if (installed) {
                tvCoreVersion.setText(getString(R.string.core_status_ready));
                int primaryColor = MaterialColors.getColor(tvCoreVersion, androidx.appcompat.R.attr.colorPrimary, Color.CYAN);
                tvCoreVersion.setTextColor(primaryColor);
            } else {
                tvCoreVersion.setText(R.string.status_core_not_installed);
                tvCoreVersion.setTextColor(Color.parseColor("#FF5252"));
            }
        }
        if (tvCoreBadge != null) {
            tvCoreBadge.setText(installed ? "v4.0.8" : getString(R.string.badge_ytdlp));
        }
        if (btnCoreAction != null) {
            btnCoreAction.setText(installed ? R.string.btn_core_check_updates : R.string.btn_core_install_update);
        }
    }

    private void performCoreCheckOrUpdate() {
        if (isFinishing() || isDestroyed()) return;

        boolean isInstalled = DownloaderEngine.getInstance().isCoreInstalled(this);
        if (!isInstalled) {
            new CoreInstallerBottomSheet(this, () -> {
                loadSavedPreferences();
                updateCoreStatusDisplay();
            }).show();
            return;
        }

        // Transition button to loading / loop animation
        if (containerCoreAction != null) {
            tn.eluea.kgpt.util.TransitionHelper.beginTransition(containerCoreAction, tn.eluea.kgpt.util.TransitionHelper.DURATION_FAST);
        }

        if (btnCoreAction != null) btnCoreAction.setVisibility(View.GONE);
        if (layoutCoreCheckingProgress != null) layoutCoreCheckingProgress.setVisibility(View.VISIBLE);
        if (layoutCoreDownloadingProgress != null) layoutCoreDownloadingProgress.setVisibility(View.GONE);

        if (lottieCoreStatus != null) {
            lottieCoreStatus.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
            if (!lottieCoreStatus.isAnimating()) {
                lottieCoreStatus.playAnimation();
            }
        }

        coreExecutor.execute(() -> {
            DownloaderEngine.getInstance().init(this);
            try {
                com.yausername.youtubedl_android.YoutubeDL.UpdateStatus status =
                        com.yausername.youtubedl_android.YoutubeDL.getInstance().updateYoutubeDL(this, com.yausername.youtubedl_android.YoutubeDL.UpdateChannel._STABLE);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (containerCoreAction != null) {
                        tn.eluea.kgpt.util.TransitionHelper.beginTransition(containerCoreAction, tn.eluea.kgpt.util.TransitionHelper.DURATION_FAST);
                    }
                    if (btnCoreAction != null) btnCoreAction.setVisibility(View.VISIBLE);
                    if (layoutCoreCheckingProgress != null) layoutCoreCheckingProgress.setVisibility(View.GONE);
                    if (layoutCoreDownloadingProgress != null) layoutCoreDownloadingProgress.setVisibility(View.GONE);

                    // Smoothly let Lottie finish its current loop iteration without abrupt snapping
                    if (lottieCoreStatus != null) {
                        lottieCoreStatus.setRepeatCount(0);
                    }

                    updateCoreStatusDisplay();
                    Toast.makeText(this, getString(R.string.core_status_ready), Toast.LENGTH_SHORT).show();
                });
            } catch (Throwable t) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (containerCoreAction != null) {
                        tn.eluea.kgpt.util.TransitionHelper.beginTransition(containerCoreAction, tn.eluea.kgpt.util.TransitionHelper.DURATION_FAST);
                    }
                    if (btnCoreAction != null) btnCoreAction.setVisibility(View.VISIBLE);
                    if (layoutCoreCheckingProgress != null) layoutCoreCheckingProgress.setVisibility(View.GONE);
                    if (layoutCoreDownloadingProgress != null) layoutCoreDownloadingProgress.setVisibility(View.GONE);

                    // Smoothly let Lottie finish its current loop iteration
                    if (lottieCoreStatus != null) {
                        lottieCoreStatus.setRepeatCount(0);
                    }

                    updateCoreStatusDisplay();
                    Toast.makeText(this, "Core check: " + (t.getMessage() != null ? t.getMessage() : "Error"), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupListeners() {
        if (headerDirectDownload != null && layoutAccordionContent != null) {
            headerDirectDownload.setOnClickListener(v -> {
                boolean isExpanded = layoutAccordionContent.getVisibility() == View.VISIBLE;
                View container = findViewById(R.id.main_content_container);
                if (container instanceof android.view.ViewGroup) {
                    tn.eluea.kgpt.util.TransitionHelper.beginTransition((android.view.ViewGroup) container, tn.eluea.kgpt.util.TransitionHelper.DURATION_NORMAL);
                }
                if (isExpanded) {
                    layoutAccordionContent.setVisibility(View.GONE);
                    if (ivAccordionArrow != null) {
                        ivAccordionArrow.animate().rotation(0f).setDuration(200).start();
                    }
                } else {
                    layoutAccordionContent.setVisibility(View.VISIBLE);
                    if (ivAccordionArrow != null) {
                        ivAccordionArrow.animate().rotation(90f).setDuration(200).start();
                    }
                }
            });
        }

        if (btnPasteUrl != null) {
            btnPasteUrl.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    if (item != null && item.getText() != null) {
                        etDirectUrl.setText(item.getText().toString().trim());
                        etDirectUrl.setSelection(etDirectUrl.getText().length());
                        return;
                    }
                }
                Toast.makeText(this, getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show();
            });
        }

        if (btnStartDirectDownload != null) {
            btnStartDirectDownload.setOnClickListener(v -> {
                String input = etDirectUrl.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(this, getString(R.string.toast_invalid_url), Toast.LENGTH_SHORT).show();
                    return;
                }
                List<String> urls = MediaUtils.extractAllUrls(input);
                if (!DownloaderEngine.getInstance().isCoreInstalled(this)) {
                    new CoreInstallerBottomSheet(this, () -> {
                        loadSavedPreferences();
                        proceedWithUrls(urls, input);
                    }).show();
                    return;
                }
                proceedWithUrls(urls, input);
            });
        }

        if (rowDownloadDir != null) {
            rowDownloadDir.setOnClickListener(v -> folderPickerLauncher.launch(null));
        }

        if (btnCoreAction != null) {
            btnCoreAction.setOnClickListener(v -> performCoreCheckOrUpdate());
        }

        if (switchGroupByApp != null) {
            switchGroupByApp.setOnCheckedChangeListener((btn, isChecked) ->
                    DownloaderPrefs.setGroupByApp(this, isChecked));
        }

        if (switchGroupCreator != null) {
            switchGroupCreator.setOnCheckedChangeListener((btn, isChecked) ->
                    DownloaderPrefs.setGroupByUploader(this, isChecked));
        }

        if (switchPrefThumbnail != null) {
            switchPrefThumbnail.setOnCheckedChangeListener((btn, isChecked) ->
                    DownloaderPrefs.setEmbedThumbnail(this, isChecked));
        }

        if (switchPrefSubtitles != null) {
            switchPrefSubtitles.setOnCheckedChangeListener((btn, isChecked) ->
                    DownloaderPrefs.setEmbedSubtitles(this, isChecked));
        }

        if (switchPrefChapters != null) {
            switchPrefChapters.setOnCheckedChangeListener((btn, isChecked) ->
                    DownloaderPrefs.setSplitChapters(this, isChecked));
        }

        if (switchHookYoutube != null) {
            switchHookYoutube.setOnClickListener(v -> {
                boolean isChecked = switchHookYoutube.isChecked();
                View labelYoutube = findViewById(R.id.layout_text_youtube);
                if (isChecked) {
                    switchHookYoutube.setChecked(false);
                    applyToggleVisualState(ivIconYoutube, labelYoutube, false, true);
                    Toast.makeText(this, getString(R.string.toast_requesting_lsposed_scope), Toast.LENGTH_SHORT).show();
                    tn.eluea.kgpt.util.LSPosedHelper.requestScope(this, "com.google.android.youtube", approved -> {
                        if (approved) {
                            DownloaderPrefs.setYouTubeHookEnabled(this, true);
                            switchHookYoutube.setChecked(true);
                            applyToggleVisualState(ivIconYoutube, labelYoutube, true, true);
                            Toast.makeText(this, getString(R.string.toast_hook_enabled, getString(R.string.app_youtube_name)), Toast.LENGTH_SHORT).show();
                        } else {
                            DownloaderPrefs.setYouTubeHookEnabled(this, false);
                            switchHookYoutube.setChecked(false);
                            applyToggleVisualState(ivIconYoutube, labelYoutube, false, true);
                            Toast.makeText(this, getString(R.string.toast_lsposed_scope_denied), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    DownloaderPrefs.setYouTubeHookEnabled(this, false);
                    applyToggleVisualState(ivIconYoutube, labelYoutube, false, true);
                    tn.eluea.kgpt.util.LSPosedHelper.removeScope("com.google.android.youtube");
                    Toast.makeText(this, getString(R.string.toast_hook_disabled, getString(R.string.app_youtube_name)), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchHookYtmusic != null) {
            switchHookYtmusic.setOnClickListener(v -> {
                boolean isChecked = switchHookYtmusic.isChecked();
                View labelYtmusic = findViewById(R.id.layout_text_ytmusic);
                if (isChecked) {
                    switchHookYtmusic.setChecked(false);
                    applyToggleVisualState(ivIconYtmusic, labelYtmusic, false, true);
                    Toast.makeText(this, getString(R.string.toast_requesting_lsposed_scope), Toast.LENGTH_SHORT).show();
                    tn.eluea.kgpt.util.LSPosedHelper.requestScope(this, "com.google.android.apps.youtube.music", approved -> {
                        if (approved) {
                            DownloaderPrefs.setYTMusicHookEnabled(this, true);
                            switchHookYtmusic.setChecked(true);
                            applyToggleVisualState(ivIconYtmusic, labelYtmusic, true, true);
                            Toast.makeText(this, getString(R.string.toast_hook_enabled, getString(R.string.app_ytmusic_name)), Toast.LENGTH_SHORT).show();
                        } else {
                            DownloaderPrefs.setYTMusicHookEnabled(this, false);
                            switchHookYtmusic.setChecked(false);
                            applyToggleVisualState(ivIconYtmusic, labelYtmusic, false, true);
                            Toast.makeText(this, getString(R.string.toast_lsposed_scope_denied), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    DownloaderPrefs.setYTMusicHookEnabled(this, false);
                    applyToggleVisualState(ivIconYtmusic, labelYtmusic, false, true);
                    tn.eluea.kgpt.util.LSPosedHelper.removeScope("com.google.android.apps.youtube.music");
                    Toast.makeText(this, getString(R.string.toast_hook_disabled, getString(R.string.app_ytmusic_name)), Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnInfoGuide = findViewById(R.id.btn_info_guide);
        if (btnInfoGuide != null) {
            btnInfoGuide.setOnClickListener(v -> showGuideBottomSheet());
        }
    }

    private void proceedWithUrls(List<String> urls, String fallbackInput) {
        if (urls.size() > 1) {
            new LinkSelectionBottomSheet(this, urls, (LinkSelectionBottomSheet.OnLinkSelectedListener) selectedUrl -> {
                new MediaDownloaderBottomSheet(this, selectedUrl).show();
            }).show();
        } else if (urls.size() == 1) {
            new MediaDownloaderBottomSheet(this, urls.get(0)).show();
        } else {
            new YouTubeSearchBottomSheet(this, fallbackInput).show();
        }
    }

    private void showGuideBottomSheet() {
        tn.eluea.kgpt.ui.main.FloatingBottomSheet bottomSheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_downloader_guide, null);
        tn.eluea.kgpt.ui.main.BottomSheetHelper.applyTheme(this, sheetView);
        bottomSheet.setContentView(sheetView);

        MaterialButton btnDismiss = sheetView.findViewById(R.id.btn_dismiss_guide);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> bottomSheet.dismiss());
        }

        bottomSheet.show();
    }

    private void applyAmoledIfNeeded() {
        boolean isDarkMode = tn.eluea.kgpt.ui.main.BottomSheetHelper.isDarkMode(this);
        boolean isAmoled = tn.eluea.kgpt.ui.main.BottomSheetHelper.isAmoledMode(this);

        if (isDarkMode && isAmoled) {
            View root = findViewById(R.id.root_layout);
            if (root != null) {
                root.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.background_amoled));
            }
        }
    }
}
