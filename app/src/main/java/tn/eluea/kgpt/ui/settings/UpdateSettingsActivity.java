/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 */
package tn.eluea.kgpt.ui.settings;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.materialswitch.MaterialSwitch;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.SPManager;
import tn.eluea.kgpt.updater.UpdateChecker;
import tn.eluea.kgpt.updater.UpdateInfo;
import tn.eluea.kgpt.updater.UpdateStatusBottomSheet;
import tn.eluea.kgpt.updater.UpdateWorker;

/**
 * Activity for managing update settings.
 */
public class UpdateSettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchUpdates;
    private LinearLayout btnUpdateInterval, btnCheckUpdate, btnDownloadPath;
    private TextView tvUpdateInterval, tvLastCheck, tvCurrentVersion, tvDownloadPath;
    private ImageView btnBack;
    private ImageView ivDockIcon;
    private ObjectAnimator rotationAnimator;
    
    private ActivityResultLauncher<Uri> folderPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply theme
        tn.eluea.kgpt.util.MaterialYouManager.getInstance(this).applyTheme(this);

        setContentView(R.layout.activity_update_settings);

        setupFolderPicker();
        initViews();
        loadSettings();
        setupListeners();
    }

    private void setupFolderPicker() {
        folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    // Take persistent permission
                    getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    
                    // Convert to file path
                    String path = getPathFromUri(uri);
                    if (path != null && SPManager.isReady()) {
                        SPManager.getInstance().setUpdateDownloadPath(path);
                        updateDownloadPathDisplay();
                    }
                }
            }
        );
    }

    private String getPathFromUri(Uri uri) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
        if (documentFile != null) {
            // Try to get the actual path
            String docId = uri.getLastPathSegment();
            if (docId != null && docId.contains(":")) {
                String[] split = docId.split(":");
                String type = split[0];
                String relativePath = split.length > 1 ? split[1] : "";
                
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory().getPath() + "/" + relativePath;
                }
            }
        }
        return null;
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        switchUpdates = findViewById(R.id.switch_updates);
        btnUpdateInterval = findViewById(R.id.btn_update_interval);
        btnDownloadPath = findViewById(R.id.btn_download_path);

        // Dock button
        btnCheckUpdate = findViewById(R.id.dock_check_updates);

        tvUpdateInterval = findViewById(R.id.tv_update_interval);
        tvCurrentVersion = findViewById(R.id.tv_current_version);
        tvDownloadPath = findViewById(R.id.tv_download_path);

        // Setup Dock UI
        if (btnCheckUpdate != null) {
            TextView tvDockText = btnCheckUpdate.findViewById(R.id.dock_action_text);
            ivDockIcon = btnCheckUpdate.findViewById(R.id.dock_action_icon);

            if (tvDockText != null)
                tvDockText.setText("Check for Updates");
            if (ivDockIcon != null)
                ivDockIcon.setImageResource(R.drawable.ic_refresh_circle_filled);
        }
    }

    private void loadSettings() {
        if (SPManager.isReady()) {
            boolean updatesEnabled = SPManager.getInstance().getUpdateCheckEnabled();
            int interval = SPManager.getInstance().getUpdateCheckInterval();

            switchUpdates.setChecked(updatesEnabled);
            tvUpdateInterval.setText(formatIntervalText(interval));

            // Update interval button state
            btnUpdateInterval.setAlpha(updatesEnabled ? 1.0f : 0.5f);
            btnUpdateInterval.setEnabled(updatesEnabled);
            
            // Update download path display
            updateDownloadPathDisplay();
        }

        // Current version
        if (tvCurrentVersion != null) {
            tvCurrentVersion.setText("v" + BuildConfig.VERSION_NAME);
        }
    }

    private void updateDownloadPathDisplay() {
        if (tvDownloadPath == null || !SPManager.isReady()) return;
        
        String customPath = SPManager.getInstance().getUpdateDownloadPath();
        if (customPath == null || customPath.isEmpty()) {
            tvDownloadPath.setText("Default (Downloads)");
        } else {
            // Show shortened path
            String displayPath = customPath;
            String externalStorage = Environment.getExternalStorageDirectory().getPath();
            if (displayPath.startsWith(externalStorage)) {
                displayPath = displayPath.replace(externalStorage, "Internal Storage");
            }
            tvDownloadPath.setText(displayPath);
        }
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Updates toggle
        switchUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (SPManager.isReady()) {
                SPManager.getInstance().setUpdateCheckEnabled(isChecked);

                // Update interval button state
                btnUpdateInterval.setAlpha(isChecked ? 1.0f : 0.5f);
                btnUpdateInterval.setEnabled(isChecked);

                // Reschedule or cancel WorkManager
                if (isChecked) {
                    int interval = SPManager.getInstance().getUpdateCheckInterval();
                    UpdateWorker.scheduleUpdateCheck(this, interval);
                } else {
                    UpdateWorker.cancelUpdateCheck(this);
                }
            }
        });

        // Interval picker
        btnUpdateInterval.setOnClickListener(v -> showIntervalPicker());

        // Download path picker
        if (btnDownloadPath != null) {
            btnDownloadPath.setOnClickListener(v -> showDownloadPathPicker());
        }

        // Check now
        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(v -> performManualUpdateCheck());
        }
    }

    private void showDownloadPathPicker() {
        String[] options = { "Default (Downloads)", "Choose Custom Folder" };

        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_update_interval, null);
        tn.eluea.kgpt.ui.main.BottomSheetHelper.applyTheme(this, sheetView);

        LinearLayout optionsContainer = sheetView.findViewById(R.id.options_container);
        optionsContainer.removeAllViews();

        tn.eluea.kgpt.ui.main.FloatingBottomSheet dialog = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(this);

        String currentPath = SPManager.isReady() ? SPManager.getInstance().getUpdateDownloadPath() : "";
        boolean isDefault = (currentPath == null || currentPath.isEmpty());

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            View itemView = getLayoutInflater().inflate(R.layout.item_search_engine_option, optionsContainer, false);

            TextView tvName = itemView.findViewById(R.id.tv_option_name);
            ImageView checkMark = itemView.findViewById(R.id.check_mark);

            tvName.setText(options[i]);

            boolean isSelected = (index == 0 && isDefault) || (index == 1 && !isDefault);

            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_option_selected_rounded);
                checkMark.setVisibility(View.VISIBLE);
            } else {
                itemView.setBackgroundResource(R.drawable.bg_selectable_rounded);
                checkMark.setVisibility(View.INVISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (index == 0) {
                    // Reset to default
                    if (SPManager.isReady()) {
                        SPManager.getInstance().setUpdateDownloadPath("");
                        updateDownloadPathDisplay();
                    }
                    dialog.dismiss();
                } else {
                    // Open folder picker
                    dialog.dismiss();
                    folderPickerLauncher.launch(null);
                }
            });

            optionsContainer.addView(itemView);
        }

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void showIntervalPicker() {
        String[] options = { "Every hour", "Every 6 hours", "Every 12 hours", "Every day", "Every 2 days",
                "Every week" };
        int[] values = { 1, 6, 12, 24, 48, 168 };

        int currentInterval = SPManager.isReady() ? SPManager.getInstance().getUpdateCheckInterval() : 24;

        // Inflate view
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_update_interval, null);
        tn.eluea.kgpt.ui.main.BottomSheetHelper.applyTheme(this, sheetView);

        // Find container
        LinearLayout optionsContainer = sheetView.findViewById(R.id.options_container);
        optionsContainer.removeAllViews();

        // Setup Bottom Sheet
        tn.eluea.kgpt.ui.main.FloatingBottomSheet dialog = new tn.eluea.kgpt.ui.main.FloatingBottomSheet(this);

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            View itemView = getLayoutInflater().inflate(R.layout.item_search_engine_option, optionsContainer, false);

            TextView tvName = itemView.findViewById(R.id.tv_option_name);
            ImageView checkMark = itemView.findViewById(R.id.check_mark);

            tvName.setText(options[i]);

            boolean isSelected = (values[i] == currentInterval);

            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.bg_option_selected_rounded);
                checkMark.setVisibility(View.VISIBLE);
            } else {
                itemView.setBackgroundResource(R.drawable.bg_selectable_rounded);
                checkMark.setVisibility(View.INVISIBLE);
            }

            itemView.setOnClickListener(v -> {
                int newInterval = values[index];
                if (SPManager.isReady()) {
                    SPManager.getInstance().setUpdateCheckInterval(newInterval);
                    tvUpdateInterval.setText(formatIntervalText(newInterval));

                    if (SPManager.getInstance().getUpdateCheckEnabled()) {
                        UpdateWorker.scheduleUpdateCheck(this, newInterval);
                    }
                }
                dialog.dismiss();
            });

            optionsContainer.addView(itemView);
        }

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void performManualUpdateCheck() {
        if (btnCheckUpdate == null)
            return;

        // Check internet connectivity first
        if (!isNetworkAvailable()) {
            UpdateStatusBottomSheet.forNoInternet(this).show();
            return;
        }

        // Show loading state on button
        btnCheckUpdate.setEnabled(false);
        btnCheckUpdate.setAlpha(0.7f);

        TextView tvDockText = btnCheckUpdate.findViewById(R.id.dock_action_text);
        if (tvDockText != null) {
            tvDockText.setText("Checking...");
        }

        // Start rotation animation on icon
        startIconRotation();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            UpdateInfo updateInfo = null;
            Exception error = null;

            try {
                UpdateChecker checker = new UpdateChecker(this);
                updateInfo = checker.checkForUpdate();
            } catch (Exception e) {
                error = e;
            }

            final UpdateInfo finalUpdateInfo = updateInfo;
            final Exception finalError = error;

            mainHandler.post(() -> {
                // Stop rotation animation
                stopIconRotation();

                // Restore button
                btnCheckUpdate.setEnabled(true);
                btnCheckUpdate.setAlpha(1.0f);

                if (tvDockText != null) {
                    tvDockText.setText("Check for Updates");
                }

                if (finalError != null) {
                    UpdateStatusBottomSheet.forError(this, finalError.getMessage()).show();
                } else if (finalUpdateInfo != null) {
                    UpdateStatusBottomSheet.forUpdateAvailable(this, finalUpdateInfo).show();
                } else {
                    UpdateStatusBottomSheet.forUpToDate(this).show();
                }
            });
        });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = 
            (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        
        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        
        android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && 
               (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void startIconRotation() {
        if (ivDockIcon == null) return;
        
        rotationAnimator = ObjectAnimator.ofFloat(ivDockIcon, "rotation", 0f, 360f);
        rotationAnimator.setDuration(1000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.start();
    }

    private void stopIconRotation() {
        if (rotationAnimator != null) {
            rotationAnimator.cancel();
            rotationAnimator = null;
        }
        if (ivDockIcon != null) {
            ivDockIcon.setRotation(0f);
        }
    }

    private String formatIntervalText(int hours) {
        if (hours <= 0)
            return "Disabled";
        if (hours == 1)
            return "Every hour";
        if (hours < 24)
            return "Every " + hours + " hours";
        if (hours == 24)
            return "Every day";
        if (hours == 48)
            return "Every 2 days";
        if (hours == 168)
            return "Every week";
        return "Every " + hours + " hours";
    }
}
