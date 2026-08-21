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
    private View rowDownloadDir, rowCoreUpdate;
    private TextView tvCurrentDownloadDir, tvCoreVersion;
    private MaterialSwitch switchGroupCreator, switchPrefThumbnail, switchPrefSubtitles, switchPrefChapters;

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

                    String path = uri.getPath();
                    if (path != null) {
                        DownloaderPrefs.setCustomDownloadPath(this, path);
                        updateDirDisplay();
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_downloader);

        applyAmoledIfNeeded();
        initViews();
        loadSavedPreferences();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedPreferences();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etDirectUrl = findViewById(R.id.et_direct_url);
        btnPasteUrl = findViewById(R.id.btn_paste_url);
        btnStartDirectDownload = findViewById(R.id.btn_start_direct_download);
        rowDownloadDir = findViewById(R.id.row_download_dir);
        rowCoreUpdate = findViewById(R.id.row_core_update);
        tvCurrentDownloadDir = findViewById(R.id.tv_current_download_dir);
        tvCoreVersion = findViewById(R.id.tv_core_version);

        switchGroupCreator = findViewById(R.id.switch_group_creator);
        switchPrefThumbnail = findViewById(R.id.switch_pref_thumbnail);
        switchPrefSubtitles = findViewById(R.id.switch_pref_subtitles);
        switchPrefChapters = findViewById(R.id.switch_pref_chapters);
    }

    private void loadSavedPreferences() {
        updateDirDisplay();
        updateCoreStatusDisplay();

        if (switchGroupCreator != null) switchGroupCreator.setChecked(DownloaderPrefs.isGroupByUploader(this));
        if (switchPrefThumbnail != null) switchPrefThumbnail.setChecked(DownloaderPrefs.isEmbedThumbnail(this));
        if (switchPrefSubtitles != null) switchPrefSubtitles.setChecked(DownloaderPrefs.isEmbedSubtitles(this));
        if (switchPrefChapters != null) switchPrefChapters.setChecked(DownloaderPrefs.isSplitChapters(this));
    }

    private void updateDirDisplay() {
        String path = DownloaderPrefs.getDownloadRootPath(this);
        if (tvCurrentDownloadDir != null) {
            tvCurrentDownloadDir.setText(path);
        }
    }

    private void updateCoreStatusDisplay() {
        boolean installed = DownloaderEngine.getInstance().isCoreInstalled(this);
        if (tvCoreVersion != null) {
            if (installed) {
                tvCoreVersion.setText("v4.0.8 (" + getString(R.string.status_core_ready) + ")");
                int primaryColor = MaterialColors.getColor(tvCoreVersion, androidx.appcompat.R.attr.colorPrimary, Color.CYAN);
                tvCoreVersion.setTextColor(primaryColor);
            } else {
                tvCoreVersion.setText(R.string.status_core_not_installed);
                tvCoreVersion.setTextColor(Color.parseColor("#FF5252"));
            }
        }
    }

    private void setupListeners() {
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

        if (rowCoreUpdate != null) {
            rowCoreUpdate.setOnClickListener(v -> {
                if (!DownloaderEngine.getInstance().isCoreInstalled(this)) {
                    new CoreInstallerBottomSheet(this, this::loadSavedPreferences).show();
                } else {
                    new CoreUpdateBottomSheet(this).show();
                }
            });
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

        View btnInfoCredits = findViewById(R.id.btn_info_credits);
        if (btnInfoCredits != null) {
            btnInfoCredits.setOnClickListener(v -> showCreditsBottomSheet());
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

    private void showCreditsBottomSheet() {
        tn.eluea.kgpt.ui.main.FloatingBottomSheet bottomSheet = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_downloader_credits, null);
        tn.eluea.kgpt.ui.main.BottomSheetHelper.applyTheme(this, sheetView);
        bottomSheet.setContentView(sheetView);

        MaterialButton btnVisitGithub = sheetView.findViewById(R.id.btn_visit_github);
        if (btnVisitGithub != null) {
            btnVisitGithub.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Eluea/KGPT"));
                    startActivity(browserIntent);
                } catch (Throwable ignored) {}
            });
        }

        MaterialButton btnDismiss = sheetView.findViewById(R.id.btn_dismiss_credits);
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
