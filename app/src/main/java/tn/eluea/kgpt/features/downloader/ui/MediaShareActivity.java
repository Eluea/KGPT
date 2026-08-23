/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.features.downloader.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import tn.eluea.kgpt.R;
import tn.eluea.kgpt.features.downloader.core.DownloaderEngine;
import tn.eluea.kgpt.features.downloader.core.MediaSearchItem;
import tn.eluea.kgpt.features.downloader.core.MediaUtils;
import tn.eluea.kgpt.features.downloader.core.YouTubeSearchEngine;

public class MediaShareActivity extends AppCompatActivity {
    private static final String TAG = "KGPT_MediaShareActivity";

    private MediaDownloaderBottomSheet currentSheet = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (currentSheet != null) {
            try {
                currentSheet.dismiss();
            } catch (Throwable ignored) {}
            currentSheet = null;
        }

        if (intent == null) {
            finish();
            return;
        }

        String rawText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (rawText == null && intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
            CharSequence text = intent.getClipData().getItemAt(0).getText();
            if (text != null) {
                rawText = text.toString();
            }
        }
        if (rawText == null && intent.getStringExtra("url") != null) {
            rawText = intent.getStringExtra("url");
        }
        if (rawText == null && intent.getData() != null) {
            rawText = intent.getData().toString();
        }

        List<String> extractedUrls = MediaUtils.extractAllUrls(rawText);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        if (extractedUrls.isEmpty()) {
            if (rawText != null && !rawText.trim().isEmpty() && (rawText.startsWith("http://") || rawText.startsWith("https://"))) {
                extractedUrls.add(rawText.trim());
            }
        }

        if (extractedUrls.isEmpty()) {
            if (rawText != null && !rawText.trim().isEmpty()) {
                final String query = rawText.trim();
                // Auto-resolve top video directly instead of showing search picker sheet
                YouTubeSearchEngine.getInstance().search(this, query, 1, null, new YouTubeSearchEngine.SearchCallback() {
                    @Override
                    public void onSuccess(YouTubeSearchEngine.SearchResult result) {
                        if (isFinishing() || isDestroyed()) return;
                        if (result != null && !result.getItems().isEmpty()) {
                            MediaSearchItem item = result.getItems().get(0);
                            String videoUrl = item.getUrl();
                            if (videoUrl != null && !videoUrl.isEmpty()) {
                                openDirectDownloadSheet(videoUrl);
                                return;
                            }
                        }
                        // Fallback to search bottom sheet if no direct item resolved
                        openSearchSheet(query);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isFinishing() || isDestroyed()) return;
                        openSearchSheet(query);
                    }
                });
                return;
            }
            Toast.makeText(this, getString(R.string.toast_invalid_url), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final List<String> finalUrls = extractedUrls;

        // Check if on-demand downloader core is installed
        if (!DownloaderEngine.getInstance().isCoreInstalled(this)) {
            CoreInstallerBottomSheet installer = new CoreInstallerBottomSheet(this, () -> {
                openTargetFlow(finalUrls);
            });
            installer.setOnDismissListener(dialog -> finish());
            installer.show();
            return;
        }

        openTargetFlow(finalUrls);
    }

    private void openDirectDownloadSheet(String url) {
        if (!DownloaderEngine.getInstance().isCoreInstalled(this)) {
            CoreInstallerBottomSheet installer = new CoreInstallerBottomSheet(this, () -> {
                openDirectDownloadSheet(url);
            });
            installer.setOnDismissListener(dialog -> finish());
            installer.show();
            return;
        }

        currentSheet = new MediaDownloaderBottomSheet(this, url);
        currentSheet.setOnDismissListener(this::finish);
        currentSheet.show();
    }

    private void openSearchSheet(String query) {
        YouTubeSearchBottomSheet searchSheet = new YouTubeSearchBottomSheet(this, query);
        searchSheet.setOnDismissListener(dialog -> finish());
        searchSheet.show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private void openTargetFlow(List<String> extractedUrls) {
        if (extractedUrls.size() == 1) {
            currentSheet = new MediaDownloaderBottomSheet(this, extractedUrls.get(0));
            currentSheet.setOnDismissListener(this::finish);
            currentSheet.show();
        } else {
            LinkSelectionBottomSheet linkPicker = new LinkSelectionBottomSheet(this, extractedUrls, (LinkSelectionBottomSheet.OnLinkSelectedListener) selectedUrl -> {
                currentSheet = new MediaDownloaderBottomSheet(this, selectedUrl);
                currentSheet.setOnDismissListener(this::finish);
                currentSheet.show();
            });
            linkPicker.setOnDismissListener(dialog -> finish());
            linkPicker.show();
        }
    }
}
