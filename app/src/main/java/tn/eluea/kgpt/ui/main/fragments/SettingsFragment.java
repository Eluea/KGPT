/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GitHub: https://github.com/Eluea
 * Telegram: https://t.me/Eluea
 */
package tn.eluea.kgpt.ui.main.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONException;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.KGPTApplication;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.backup.BackupManager;
import tn.eluea.kgpt.backup.BackupOptions;
import tn.eluea.kgpt.backup.BackupOptionsBottomSheet;
import tn.eluea.kgpt.backup.LogExporter;
import tn.eluea.kgpt.settings.OtherSettingsType;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;

public class SettingsFragment extends Fragment {

    private static final String PREF_THEME = "theme_mode";
    private static final String PREF_AMOLED = "amoled_mode";
    private static final String PREF_WINTER_MODE = "winter_mode";

    private MaterialSwitch switchDarkMode, switchAmoled, switchWinterMode, switchLogs, switchExternalInternet;

    private LinearLayout amoledContainer, btnBackup, btnRestore, btnExportLogs, btnBlurControl;
    private TextView tvAboutVersion;
    private FrameLayout btnInfo;
    private MaterialButton btnTelegramSupport, btnChangelog;
    private View rootView;

    // Icons for candy colors
    private android.widget.ImageView iconDarkMode, iconAmoled, iconBlur, iconMaterialYou;
    private android.widget.ImageView iconLogging, iconInternet, iconExport, iconBackup, iconRestore;

    // Update settings - single entry point
    private LinearLayout btnUpdateSettings;

    private SharedPreferences uiPrefs;
    private BackupManager backupManager;
    private LogExporter logExporter;

    // Activity result launchers for file picker
    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;
    private ActivityResultLauncher<Intent> exportLogsLauncher;

    // Backup/Restore state
    private BackupOptions pendingBackupOptions;
    private BackupOptions pendingRestoreOptions;
    private String pendingRestoreJson;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize backup launcher
        backupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performBackup(uri);
                        }
                    }
                });

        // Initialize restore launcher
        restoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performRestore(uri);
                        }
                    }
                });

        // Initialize export logs launcher
        exportLogsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            performExportLogs(uri);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        uiPrefs = requireContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
        backupManager = new BackupManager(requireContext());
        logExporter = new LogExporter(requireContext());
        initViews(view);
        applyAmoledIfNeeded();
        applyThemeColors();
        // applyCandyColorsIfNeeded(); // Removed
        loadSettings();
        setupListeners();
        setupScrollListener();
    }

    private void setupScrollListener() {
        // We will check if rootView is NestedScrollView since it is often the root in
        // settings fragments.

        if (rootView instanceof androidx.core.widget.NestedScrollView) {

            androidx.core.widget.NestedScrollView nestedScrollView = (androidx.core.widget.NestedScrollView) rootView;

            nestedScrollView
                    .setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v,
                            scrollX, scrollY, oldScrollX, oldScrollY) -> {

                        if (getActivity() instanceof tn.eluea.kgpt.ui.main.MainActivity) {
                            tn.eluea.kgpt.ui.main.MainActivity activity = (tn.eluea.kgpt.ui.main.MainActivity) getActivity();
                            activity.onContentScrolled();

                            // Debounce obstacle update
                            v.removeCallbacks(updateObstaclesRunnable);
                            v.postDelayed(updateObstaclesRunnable, 200);
                        }
                    });
        }
    }

    private final Runnable updateObstaclesRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded() || getActivity() == null)
                return;

            // Find obstacles (Cards)
            java.util.List<android.graphics.Rect> obstacles = new java.util.ArrayList<>();
            findObstaclesRecursively(rootView, obstacles);

            if (getActivity() instanceof tn.eluea.kgpt.ui.main.MainActivity) {
                ((tn.eluea.kgpt.ui.main.MainActivity) getActivity()).updateSnowObstacles(obstacles);
            }
        }
    };

    private void findObstaclesRecursively(View view, java.util.List<android.graphics.Rect> obstacles) {
        if (view instanceof com.google.android.material.card.MaterialCardView) {
            if (view.getVisibility() == View.VISIBLE) {
                android.graphics.Rect rect = new android.graphics.Rect();
                view.getGlobalVisibleRect(rect);

                // Adjust for SnowfallView coordinate system (usually location on screen)
                // If SnowfallView covers the whole window, global rect is fine.
                // However, we might want to subtract the top inset/status bar if SnowfallView
                // starts below it.
                // But SnowfallView in ActivityMain is match_parent/match_parent with
                // fitsSystemWindows=true?
                // Let's assume Global Rect is close enough, we can fine tune.
                // Actually, SnowfallView might be offset.
                // Let's just pass global rects for now.

                obstacles.add(rect);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findObstaclesRecursively(group.getChildAt(i), obstacles);
            }
        }
    }

    private void initViews(View view) {
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchAmoled = view.findViewById(R.id.switch_amoled);
        switchWinterMode = view.findViewById(R.id.switch_winter_mode);
        btnBlurControl = view.findViewById(R.id.btn_blur_control);
        switchLogs = view.findViewById(R.id.switch_logs);
        switchExternalInternet = view.findViewById(R.id.switch_external_internet);
        amoledContainer = view.findViewById(R.id.amoled_container);
        tvAboutVersion = view.findViewById(R.id.tv_about_version);
        btnInfo = view.findViewById(R.id.btn_info);
        btnTelegramSupport = view.findViewById(R.id.btn_telegram_support);
        btnChangelog = view.findViewById(R.id.btn_changelog);
        btnBackup = view.findViewById(R.id.btn_backup);
        btnRestore = view.findViewById(R.id.btn_restore);
        btnExportLogs = view.findViewById(R.id.btn_export_logs);

        // Initialize icons for candy colors
        iconDarkMode = view.findViewById(R.id.icon_dark_mode);
        iconAmoled = view.findViewById(R.id.icon_amoled);
        iconBlur = view.findViewById(R.id.icon_blur);
        iconMaterialYou = view.findViewById(R.id.icon_material_you);
        iconLogging = view.findViewById(R.id.icon_logging);
        iconInternet = view.findViewById(R.id.icon_internet);
        iconExport = view.findViewById(R.id.icon_export);
        iconBackup = view.findViewById(R.id.icon_backup);
        iconRestore = view.findViewById(R.id.icon_restore);

        // Initialize update settings button
        btnUpdateSettings = view.findViewById(R.id.btn_update_settings);
    }

    private void applyThemeColors() {
        // Apply dynamic theme colors to switches
        Context context = requireContext();
        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(context,
                androidx.appcompat.R.attr.colorPrimary, 0);
        int colorOnPrimary = com.google.android.material.color.MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOnPrimary, 0);
        int colorOutline = com.google.android.material.color.MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutline, 0);
        int colorSurfaceContainerHighest = com.google.android.material.color.MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorSurfaceContainerHigh, 0);

        // Map to standard Material 3 Switch colors
        int thumbCheckedColor = colorOnPrimary;
        int trackCheckedColor = colorPrimary;
        int thumbUncheckedColor = colorOutline;
        int trackUncheckedColor = colorSurfaceContainerHighest;

        applySwitchColors(switchDarkMode, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor,
                trackUncheckedColor);
        applySwitchColors(switchAmoled, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor, trackUncheckedColor);
        applySwitchColors(switchWinterMode, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor,
                trackUncheckedColor);
        applySwitchColors(switchLogs, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor, trackUncheckedColor);
        applySwitchColors(switchExternalInternet, thumbCheckedColor, thumbUncheckedColor, trackCheckedColor,
                trackUncheckedColor);
    }

    /**
     * Apply candy colors to setting icons when Material You is disabled.
     */
    // CandyColors removed
    // Rely on XML attributes for icon tinting.

    private void applySwitchColors(MaterialSwitch switchView, int thumbChecked, int thumbUnchecked, int trackChecked,
            int trackUnchecked) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        int[] thumbColors = new int[] { thumbChecked, thumbUnchecked };
        int[] trackColors = new int[] { trackChecked, trackUnchecked };

        switchView.setThumbTintList(new ColorStateList(states, thumbColors));
        switchView.setTrackTintList(new ColorStateList(states, trackColors));
    }

    private ColorStateList createSwitchThumbColorStateList(int checkedColor) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
                checkedColor,
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
        };
        return new ColorStateList(states, colors);
    }

    private void applyAmoledIfNeeded() {
        boolean isAmoled = uiPrefs.getBoolean(PREF_AMOLED, false);
        boolean isDarkMode = uiPrefs.getBoolean(PREF_THEME, false);

        if (isDarkMode && isAmoled) {
            // Apply AMOLED colors
            rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_amoled));

            // Update all cards to AMOLED surface color
            applyAmoledToCards(rootView);
        }
    }

    private void applyAmoledToCards(View view) {
        if (view instanceof MaterialCardView) {
            ((MaterialCardView) view).setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.surface_amoled));
            int dynamicStrokeColor = com.google.android.material.color.MaterialColors.getColor(view,
                    com.google.android.material.R.attr.colorOutlineVariant,
                    ContextCompat.getColor(requireContext(), R.color.divider_dark));
            ((MaterialCardView) view).setStrokeColor(dynamicStrokeColor);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyAmoledToCards(group.getChildAt(i));
            }
        }
    }

    private void loadSettings() {
        // UI Settings
        boolean isDarkMode = uiPrefs.getBoolean(PREF_THEME, false);
        boolean isAmoled = uiPrefs.getBoolean(PREF_AMOLED, false);

        switchDarkMode.setChecked(isDarkMode);
        switchAmoled.setChecked(isAmoled);

        // Winter Mode
        boolean isWinterMode = uiPrefs.getBoolean(PREF_WINTER_MODE, false);
        if (switchWinterMode != null) {
            switchWinterMode.setChecked(isWinterMode);
        }

        // AMOLED option only available when dark mode is enabled
        amoledContainer.setAlpha(isDarkMode ? 1.0f : 0.5f);
        switchAmoled.setEnabled(isDarkMode);

        // App Settings
        if (SPManager.isReady()) {
            switchLogs.setChecked(SPManager.getInstance().getEnableLogs());
            switchExternalInternet.setChecked(SPManager.getInstance().getEnableExternalInternet());
        }

        // Version
        tvAboutVersion.setText("Version " + BuildConfig.VERSION_NAME);
    }

    private void setupListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            uiPrefs.edit().putBoolean(PREF_THEME, isChecked).apply();

            // Update AMOLED option availability
            amoledContainer.setAlpha(isChecked ? 1.0f : 0.5f);
            switchAmoled.setEnabled(isChecked);

            if (!isChecked && switchAmoled.isChecked()) {
                switchAmoled.setChecked(false);
                uiPrefs.edit().putBoolean(PREF_AMOLED, false).apply();
            }

            // Apply theme change globally
            KGPTApplication.applyTheme(isChecked);

            // Recreate to apply theme
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        if (switchWinterMode != null) {
            switchWinterMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                uiPrefs.edit().putBoolean(PREF_WINTER_MODE, isChecked).apply();

                // Notify MainActivity to update visibility immediately
                if (getActivity() instanceof tn.eluea.kgpt.ui.main.MainActivity) {
                    ((tn.eluea.kgpt.ui.main.MainActivity) getActivity()).applyWinterMode();
                }
            });
        }

        if (rootView != null) {
            View btnMaterialYou = rootView.findViewById(R.id.btn_material_you);
            if (btnMaterialYou != null) {
                btnMaterialYou.setOnClickListener(v -> {
                    startActivity(
                            new Intent(requireContext(), tn.eluea.kgpt.ui.settings.MaterialYouSettingsActivity.class));
                });
            }
        }

        switchAmoled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!switchAmoled.isEnabled())
                return;

            uiPrefs.edit().putBoolean(PREF_AMOLED, isChecked).apply();
            // Recreate activity to apply AMOLED theme
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        btnBlurControl.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), tn.eluea.kgpt.ui.settings.BlurControlActivity.class);
            startActivity(intent);
        });

        switchLogs.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (SPManager.isReady()) {
                SPManager.getInstance().setOtherSetting(OtherSettingsType.EnableLogs, isChecked);
            }
        });

        switchExternalInternet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (SPManager.isReady()) {
                SPManager.getInstance().setOtherSetting(OtherSettingsType.EnableExternalInternet, isChecked);
            }
        });

        btnInfo.setOnClickListener(v -> showInfoBottomSheet());

        btnTelegramSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SupKGPT"));
            startActivity(intent);
        });

        btnChangelog.setOnClickListener(v -> showChangelogBottomSheet());

        btnBackup.setOnClickListener(v -> startBackup());
        btnRestore.setOnClickListener(v -> startRestore());
        btnExportLogs.setOnClickListener(v -> startExportLogs());

        // Update settings - open dedicated activity
        if (btnUpdateSettings != null) {
            btnUpdateSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), tn.eluea.kgpt.ui.settings.UpdateSettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void startBackup() {
        // Show backup options bottom sheet
        BackupOptionsBottomSheet.forBackup(requireContext(), options -> {
            if (options.isSelected(BackupOptions.Option.SENSITIVE_DATA)) {
                // Show warning if sensitive data is included
                new tn.eluea.kgpt.backup.BackupWarningDialog(requireContext(), () -> {
                    proceedToBackupFilePicker(options);
                }).show();
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

    private void startRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        restoreLauncher.launch(intent);
    }

    private void startExportLogs() {
        // Check if logging is enabled
        boolean loggingEnabled = SPManager.isReady() && SPManager.getInstance().getEnableLogs();

        if (!loggingEnabled) {
            showLoggingWarningBottomSheet();
            return;
        }

        // Request root access first
        Toast.makeText(requireContext(), "Requesting root access...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean hasRoot = logExporter.requestRootAccess();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (hasRoot) {
                        Toast.makeText(requireContext(), "Root access granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Root access denied - some logs may be limited",
                                Toast.LENGTH_SHORT).show();
                    }

                    // Proceed to file picker
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    intent.putExtra(Intent.EXTRA_TITLE, LogExporter.generateExportFilename());
                    exportLogsLauncher.launch(intent);
                });
            }
        }).start();
    }

    private void showLoggingWarningBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_logging_warning, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        MaterialButton btnEnableLogging = sheetView.findViewById(R.id.btn_enable_logging);
        MaterialButton btnCancel = sheetView.findViewById(R.id.btn_cancel);

        btnEnableLogging.setOnClickListener(v -> {
            // Enable logging
            if (SPManager.isReady()) {
                SPManager.getInstance().setOtherSetting(OtherSettingsType.EnableLogs, true);
                switchLogs.setChecked(true);
            }
            dialog.dismiss();
            Toast.makeText(requireContext(), "Logging enabled. You can now export logs.", Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performExportLogs(Uri uri) {
        Toast.makeText(requireContext(), "Exporting logs...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            LogExporter.ExportResult result = logExporter.exportLogs(uri);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (result.success) {
                        showExportSuccessBottomSheet(result);
                    } else {
                        Toast.makeText(requireContext(), "Failed to export logs: " + result.errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void showExportSuccessBottomSheet(LogExporter.ExportResult result) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_export_success, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        // Update root status
        LinearLayout rootStatusContainer = sheetView.findViewById(R.id.root_status_container);
        TextView tvRootStatus = sheetView.findViewById(R.id.tv_root_status);
        TextView tvBootLogs = sheetView.findViewById(R.id.tv_boot_logs);
        TextView tvRootExplanation = sheetView.findViewById(R.id.tv_root_explanation);

        if (result.hasRootAccess) {
            rootStatusContainer.setBackgroundResource(R.drawable.bg_chip_success);
            tvRootStatus.setText("Root Access Granted");
            tvBootLogs.setText("â€¢ Boot/Kernel Logs (dmesg) âœ“");
            tvRootExplanation.setText(
                    "Root access was used to collect kernel logs (dmesg) and ANR traces which require elevated permissions on Android.");
        } else {
            rootStatusContainer.setBackgroundResource(R.drawable.bg_chip_warning);
            tvRootStatus.setText("No Root Access");
            tvBootLogs.setText("â€¢ Boot/Kernel Logs (limited)");
            tvRootExplanation.setText(
                    "Without root access, kernel logs (dmesg) and ANR traces could not be fully collected. Basic boot events from logcat are included.");
        }

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performBackup(Uri uri) {
        try {
            BackupOptions options = pendingBackupOptions != null ? pendingBackupOptions : new BackupOptions();
            String backupJson = backupManager.createBackup(options);
            boolean success = backupManager.saveToFile(uri, backupJson);

            if (success) {
                Toast.makeText(requireContext(),
                        "Backup saved successfully (" + options.getSelectedCount() + " sections)",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to save backup", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error creating backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            pendingBackupOptions = null;
        }
    }

    private void performRestore(Uri uri) {
        String backupJson = backupManager.readFromFile(uri);

        if (backupJson == null) {
            Toast.makeText(requireContext(), "Failed to read backup file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Analyze backup to get available options
        BackupManager.BackupAnalysis analysis = backupManager.analyzeBackup(backupJson);

        if (!analysis.valid) {
            Toast.makeText(requireContext(), analysis.errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        if (analysis.availableOptions.isEmpty()) {
            Toast.makeText(requireContext(), "No restorable data found in backup", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store backup JSON for later use
        pendingRestoreJson = backupJson;

        // Show restore options bottom sheet with available options
        BackupOptionsBottomSheet.forRestore(requireContext(), analysis.availableOptions, options -> {
            pendingRestoreOptions = options;
            executeRestore();
        }).show();
    }

    private void executeRestore() {
        if (pendingRestoreJson == null || pendingRestoreOptions == null) {
            return;
        }

        BackupManager.RestoreResult result = backupManager.restoreBackup(pendingRestoreJson, pendingRestoreOptions);

        if (result.success) {
            String restoredItemsStr = String.join(", ", result.restoredItems);
            Toast.makeText(requireContext(),
                    "Restored: " + restoredItemsStr + ". Restart app to apply theme changes.",
                    Toast.LENGTH_LONG).show();

            // Reload app triggers if they were restored
            if (pendingRestoreOptions.isSelected(BackupOptions.Option.APP_TRIGGERS)) {
                tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerManager triggerManager = tn.eluea.kgpt.ui.lab.apptrigger.AppTriggerManager
                        .getInstance();
                if (triggerManager != null) {
                    triggerManager.reloadTriggers();
                }
            }

            // Reload settings
            loadSettings();
        } else {
            Toast.makeText(requireContext(), "Restore failed: " + result.errorMessage, Toast.LENGTH_SHORT).show();
        }

        // Clear pending state
        pendingRestoreJson = null;
        pendingRestoreOptions = null;
    }

    private void showChangelogBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_changelog, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        // Resolve dynamic colors
        int colorPrimary = com.google.android.material.color.MaterialColors.getColor(sheetView,
                androidx.appcompat.R.attr.colorPrimary);
        int colorOnPrimary = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorOnPrimary);
        int colorSecondary = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorSecondary);
        int colorOnSecondary = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorOnSecondary);
        int colorTertiary = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorTertiary);
        int colorOnTertiary = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorOnTertiary);
        // Added PrimaryContainer
        int colorPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorPrimaryContainer);
        int colorOnPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(sheetView,
                com.google.android.material.R.attr.colorOnPrimaryContainer);

        // Set version with dynamic colors
        TextView tvVersion = sheetView.findViewById(R.id.tv_version);
        tvVersion.setText("v" + BuildConfig.VERSION_NAME);

        android.graphics.drawable.GradientDrawable versionBg = new android.graphics.drawable.GradientDrawable();
        versionBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        versionBg.setCornerRadius(100f); // Pill shape
        // Use PrimaryContainer for version badge (Degree of Seed)
        versionBg.setColor(colorPrimaryContainer);
        tvVersion.setBackground(versionBg);
        tvVersion.setTextColor(colorOnPrimaryContainer);

        // Add changelog entries
        LinearLayout changelogContent = sheetView.findViewById(R.id.changelog_content);

        // Version 4.0.4 Changes
        addChangelogEntry(changelogContent, "CHANGE",
                "Changed animation of the version card in the updates page",
                colorPrimary, colorOnPrimary);

        addChangelogEntry(changelogContent, "ADD",
                "Added Winter Mode",
                colorPrimary, colorOnPrimary);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void addChangelogEntry(LinearLayout container, String tag, String text, int bgColor, int textColor) {
        View entryView = LayoutInflater.from(requireContext()).inflate(R.layout.item_changelog_entry, container, false);

        TextView tvTag = entryView.findViewById(R.id.tv_tag);
        TextView tvText = entryView.findViewById(R.id.tv_text);

        tvTag.setText(tag);
        tvText.setText(text);

        // Set dynamic background
        android.graphics.drawable.GradientDrawable tagBg = new android.graphics.drawable.GradientDrawable();
        tagBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        tagBg.setCornerRadius(dpToPx(4)); // 4dp
        tagBg.setColor(bgColor);

        tvTag.setBackground(tagBg);
        tvTag.setTextColor(textColor);

        container.addView(entryView);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    // Existing helper if needed, but defining simple one above is safer to avoid
    // method not found
    private void _unused_placeholder() {
        // Just to ensure I replace the method signature correctly manually in the block
    }

    // Original method signature was (LinearLayout, String, String)
    // I am changing it to (LinearLayout, String, String, int, int)
    // But wait, the loop above was adding entries. I can OVERLOAD or just replace
    // usage.
    // I will replace usage in the block above.

    private void showInfoBottomSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_info, null);

        // Apply theme
        BottomSheetHelper.applyTheme(requireContext(), sheetView);

        FloatingBottomSheet dialog = new FloatingBottomSheet(requireContext());
        dialog.setContentView(sheetView);

        // Set content
        TextView tvTitle = sheetView.findViewById(R.id.tv_info_title);
        TextView tvDescription = sheetView.findViewById(R.id.tv_info_description);

        tvTitle.setText("About Settings");
        tvDescription.setText("Customize your KGPT experience.\n\n" +
                "â€¢ Dark Mode: Enable dark theme for comfortable viewing at night.\n\n" +
                "â€¢ AMOLED Dark: Pure black theme for OLED screens to save battery.\n\n" +
                "â€¢ Enable Logging: Turn on logs for debugging. Disable for better performance.\n\n" +
                "â€¢ External Internet Service: Recommended for chat completion. Enables external network requests for AI responses.");

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
