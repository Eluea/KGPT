/*
 * Copyright (c) 2025-2026 Amr Aldeeb @Eluea
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 *
 * Licensed under the GPLv3.
 */
package tn.eluea.kgpt.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import tn.eluea.kgpt.KGPTApplication;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.backup.BackupManager;
import tn.eluea.kgpt.backup.BackupOptions;
import tn.eluea.kgpt.backup.BackupOptionsBottomSheet;
import tn.eluea.kgpt.backup.BackupWarningDialog;

/**
 * Dedicated Activity for managing Local Backups and Restores.
 * Safe, 100% offline, and private.
 */
public class BackupSettingsActivity extends AppCompatActivity {

    private LinearLayout btnLocalExport, btnLocalImport;
    private ImageView btnBack;

    private BackupManager backupManager;

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    private BackupOptions pendingBackupOptions;
    private String pendingRestoreJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply dynamic Material You & AMOLED theme
        tn.eluea.kgpt.util.MaterialYouManager.getInstance(this).applyTheme(this);

        setContentView(R.layout.activity_backup_settings);

        backupManager = new BackupManager(this);

        setupLaunchers();
        initViews();
        setupListeners();
        applyAmoledIfNeeded();
    }

    private void setupLaunchers() {
        // Local Backup File Creation Launcher
        backupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performLocalBackup(uri);
                        }
                    }
                });

        // Local Restore File Selection Launcher
        restoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performLocalRestore(uri);
                        }
                    }
                });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnLocalExport = findViewById(R.id.btn_local_export);
        btnLocalImport = findViewById(R.id.btn_local_import);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Local Export
        if (btnLocalExport != null) {
            btnLocalExport.setOnClickListener(v -> startLocalBackup());
        }

        // Local Import
        if (btnLocalImport != null) {
            btnLocalImport.setOnClickListener(v -> startLocalRestore());
        }
    }

    private void startLocalBackup() {
        BackupOptionsBottomSheet.forBackup(this, options -> {
            if (options.isSelected(BackupOptions.Option.SENSITIVE_DATA)) {
                new BackupWarningDialog(this, () -> proceedToBackupFilePicker(options)).show();
            } else {
                proceedToBackupFilePicker(options);
            }
        }).show();
    }

    private void proceedToBackupFilePicker(BackupOptions options) {
        pendingBackupOptions = options;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, BackupManager.generateBackupFilename());
        backupLauncher.launch(intent);
    }

    private void performLocalBackup(Uri uri) {
        if (pendingBackupOptions == null) return;
        try {
            String json = backupManager.createBackup(pendingBackupOptions);
            boolean success = backupManager.saveToFile(uri, json);
            if (success) {
                Toast.makeText(this, getString(R.string.backup_exported), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pendingBackupOptions = null;
        }
    }

    private void startLocalRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        restoreLauncher.launch(intent);
    }

    private void performLocalRestore(Uri uri) {
        String json = backupManager.readFromFile(uri);
        if (json == null || json.isEmpty()) {
            Toast.makeText(this, getString(R.string.import_failed), Toast.LENGTH_LONG).show();
            return;
        }

        BackupManager.BackupAnalysis analysis = backupManager.analyzeBackup(json);
        if (!analysis.valid) {
            Toast.makeText(this, "Invalid backup: " + analysis.errorMessage, Toast.LENGTH_LONG).show();
            return;
        }

        pendingRestoreJson = json;
        BackupOptionsBottomSheet.forRestore(this, analysis.availableOptions, selectedOptions -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.title_settings_restore)
                    .setMessage(R.string.desc_settings_restore)
                    .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                        BackupManager.RestoreResult result = backupManager.restoreBackup(pendingRestoreJson, selectedOptions);
                        if (result.success) {
                            showRestartDialog(result.itemsRestored);
                        } else {
                            Toast.makeText(this, "Restore failed: " + result.errorMessage, Toast.LENGTH_LONG).show();
                        }
                        pendingRestoreJson = null;
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }).show();
    }

    private void showRestartDialog(int itemsRestored) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Complete")
                .setMessage("Successfully restored " + itemsRestored + " items. Please restart the app to apply all changes.")
                .setPositiveButton("Restart Now", (dialog, which) -> {
                    KGPTApplication.restartApp(this);
                })
                .setCancelable(false)
                .show();
    }

    private void applyAmoledIfNeeded() {
        boolean isDarkMode = tn.eluea.kgpt.ui.main.BottomSheetHelper.isDarkMode(this);
        boolean isAmoled = tn.eluea.kgpt.ui.main.BottomSheetHelper.isAmoledMode(this);

        if (isDarkMode && isAmoled) {
            View root = findViewById(R.id.coordinator);
            if (root != null) {
                root.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.background_amoled));
            }
        }
    }
}
