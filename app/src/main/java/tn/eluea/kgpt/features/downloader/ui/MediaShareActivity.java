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
import tn.eluea.kgpt.features.downloader.core.MediaUtils;

public class MediaShareActivity extends AppCompatActivity {
    private static final String TAG = "KGPT_MediaShareActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
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
                // Smart Search: Non-URL text shared to KGPT Downloader
                YouTubeSearchBottomSheet searchSheet = new YouTubeSearchBottomSheet(this, rawText.trim());
                searchSheet.setOnDismissListener(dialog -> finish());
                searchSheet.show();
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

    private void openTargetFlow(List<String> extractedUrls) {
        if (extractedUrls.size() == 1) {
            MediaDownloaderBottomSheet sheet = new MediaDownloaderBottomSheet(this, extractedUrls.get(0));
            sheet.setOnDismissListener(this::finish);
            sheet.show();
        } else {
            LinkSelectionBottomSheet linkPicker = new LinkSelectionBottomSheet(this, extractedUrls, (LinkSelectionBottomSheet.OnLinkSelectedListener) selectedUrl -> {
                MediaDownloaderBottomSheet sheet = new MediaDownloaderBottomSheet(this, selectedUrl);
                sheet.setOnDismissListener(this::finish);
                sheet.show();
            });
            linkPicker.setOnDismissListener(dialog -> finish());
            linkPicker.show();
        }
    }
}
