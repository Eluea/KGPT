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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import tn.eluea.kgpt.BuildConfig;
import tn.eluea.kgpt.R;
import tn.eluea.kgpt.ui.main.BottomSheetHelper;
import tn.eluea.kgpt.ui.main.FloatingBottomSheet;
import tn.eluea.kgpt.util.TransitionHelper;

/**
 * Bottom sheet for showing update check status:
 * - Update available
 * - Already up to date
 * - No internet connection
 * - Check failed
 */
public class UpdateStatusBottomSheet {

    public enum Status {
        UPDATE_AVAILABLE,
        UP_TO_DATE,
        NO_INTERNET,
        ERROR
    }

    private final Context context;
    private final Status status;
    private final UpdateInfo updateInfo; // Only used for UPDATE_AVAILABLE
    private final String errorMessage; // Only used for ERROR
    private final Handler mainHandler;

    private FloatingBottomSheet dialog;
    private ViewGroup rootContainer;

    public UpdateStatusBottomSheet(Context context, Status status, UpdateInfo updateInfo, String errorMessage) {
        this.context = context;
        this.status = status;
        this.updateInfo = updateInfo;
        this.errorMessage = errorMessage;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static UpdateStatusBottomSheet forUpdateAvailable(Context context, UpdateInfo info) {
        return new UpdateStatusBottomSheet(context, Status.UPDATE_AVAILABLE, info, null);
    }

    public static UpdateStatusBottomSheet forUpToDate(Context context) {
        return new UpdateStatusBottomSheet(context, Status.UP_TO_DATE, null, null);
    }

    public static UpdateStatusBottomSheet forError(Context context, String errorMessage) {
        return new UpdateStatusBottomSheet(context, Status.ERROR, null, errorMessage);
    }

    public static UpdateStatusBottomSheet forNoInternet(Context context) {
        return new UpdateStatusBottomSheet(context, Status.NO_INTERNET, null, null);
    }

    public void show() {
        View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_update_status, null);

        BottomSheetHelper.applyTheme(context, sheetView);
        
        rootContainer = (ViewGroup) sheetView;

        dialog = new FloatingBottomSheet(context);
        dialog.setContentView(sheetView);
        dialog.setCancelable(true);

        setupView(sheetView);

        dialog.show();
    }

    private void setupView(View view) {
        ImageView ivStatus = view.findViewById(R.id.iv_status);
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvMessage = view.findViewById(R.id.tv_message);
        MaterialButton btnPrimary = view.findViewById(R.id.btn_primary);
        MaterialButton btnSecondary = view.findViewById(R.id.btn_secondary);

        View iconContainer = view.findViewById(R.id.icon_container);

        // Get theme colors
        int colorPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorPrimaryContainer, 0);
        int colorOnPrimaryContainer = com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnPrimaryContainer, 0);

        int colorErrorContainer = com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorErrorContainer, 0);
        int colorOnErrorContainer = com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnErrorContainer, 0);

        // Using Tertiary for success state as seen in other sheets
        int colorTertiaryContainer = com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorTertiaryContainer, 0);
        int colorOnTertiaryContainer = com.google.android.material.color.MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnTertiaryContainer, 0);

        switch (status) {
            case UPDATE_AVAILABLE:
                iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimaryContainer));

                ivStatus.setImageResource(R.drawable.ic_download_filled);
                ivStatus.setColorFilter(colorOnPrimaryContainer);

                tvTitle.setText("Update Available!");

                if (updateInfo != null) {
                    String message = "Version " + updateInfo.getVersionName() + " is now available.\n" +
                            "You are currently using v" + BuildConfig.VERSION_NAME + ".";
                    tvMessage.setText(message);
                }

                btnPrimary.setText("Download");
                btnPrimary.setIconResource(R.drawable.ic_download_filled);
                btnPrimary.setOnClickListener(v -> {
                    // Smooth transition: fade out current content, then show UpdateBottomSheet
                    TransitionHelper.fadeOutWithScale(rootContainer, TransitionHelper.DURATION_FAST, () -> {
                        dialog.dismissInstant();
                        // Small delay to ensure smooth transition
                        mainHandler.postDelayed(() -> {
                            new UpdateBottomSheet(context, updateInfo).show();
                        }, 50);
                    });
                });

                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText("Later");
                btnSecondary.setOnClickListener(v -> dialog.dismiss());
                break;

            case UP_TO_DATE:
                // Use Primary instead of Tertiary to match the blue theme
                iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimaryContainer));

                ivStatus.setImageResource(R.drawable.ic_shield_tick_filled);
                // Use primary color for the icon to make it pop inside the container
                ivStatus.setColorFilter(com.google.android.material.color.MaterialColors.getColor(
                        context, androidx.appcompat.R.attr.colorPrimary, 0));

                tvTitle.setText("You're Up to Date!");
                tvMessage.setText("You are running the latest version (v" + BuildConfig.VERSION_NAME + ").\n" +
                        "No updates are available at this time.");

                btnPrimary.setText("Great!");
                btnPrimary.setIconResource(R.drawable.ic_check);
                btnPrimary.setOnClickListener(v -> dialog.dismiss());

                btnSecondary.setVisibility(View.GONE);
                break;

            case ERROR:
                iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorErrorContainer));

                ivStatus.setImageResource(R.drawable.ic_close_circle_filled);
                ivStatus.setColorFilter(colorOnErrorContainer);

                tvTitle.setText("Check Failed");

                String msg = "Sorry, we couldn't check for updates right now.";
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    msg += "\n\n" + errorMessage;
                }
                msg += "\n\nPlease check your internet connection and try again.";
                tvMessage.setText(msg);

                btnPrimary.setText("Try Again");
                btnPrimary.setIconResource(R.drawable.ic_refresh_filled);
                btnPrimary.setOnClickListener(v -> {
                    dialog.dismiss();
                    // The caller should handle retry
                });

                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText("Close");
                btnSecondary.setOnClickListener(v -> dialog.dismiss());
                break;

            case NO_INTERNET:
                // Use a warning/secondary color for no internet
                int colorSecondaryContainer = com.google.android.material.color.MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorSecondaryContainer, 0);
                int colorOnSecondaryContainer = com.google.android.material.color.MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSecondaryContainer, 0);

                iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorSecondaryContainer));

                ivStatus.setImageResource(R.drawable.ic_wifi_off_filled);
                ivStatus.setColorFilter(colorOnSecondaryContainer);

                tvTitle.setText("No Internet Connection");
                tvMessage.setText("Unable to check for updates.\n\n" +
                        "Please connect to the internet and try again.");

                btnPrimary.setText("OK");
                btnPrimary.setIconResource(R.drawable.ic_check);
                btnPrimary.setOnClickListener(v -> dialog.dismiss());

                btnSecondary.setVisibility(View.GONE);
                break;
        }
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
