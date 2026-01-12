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
package tn.eluea.kgpt.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import tn.eluea.kgpt.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to apply Candy Color System when Material You is disabled.
 * Each UI element gets a unique vibrant color for a playful, engaging look.
 */
public class CandyColorHelper {

    // Color rotation for automatic coloring
    private static final int[] CANDY_COLORS = {
            R.color.candy_dark_mode, // Purple
            R.color.candy_blur, // Blue
            R.color.candy_internet, // Green
            R.color.candy_logging, // Orange
            R.color.candy_material_you, // Red
            R.color.candy_export, // Teal
            R.color.candy_backup, // Indigo
            R.color.candy_restore // Pink
    };

    // Icon ID to color mapping for consistent colors
    private static final Map<Integer, Integer> ICON_COLOR_MAP = new HashMap<>();

    static {
        // Settings icons
        ICON_COLOR_MAP.put(R.id.icon_dark_mode, R.color.candy_dark_mode);
        ICON_COLOR_MAP.put(R.id.icon_amoled, R.color.candy_amoled);
        ICON_COLOR_MAP.put(R.id.icon_blur, R.color.candy_blur);
        ICON_COLOR_MAP.put(R.id.icon_material_you, R.color.candy_material_you);
        ICON_COLOR_MAP.put(R.id.icon_logging, R.color.candy_logging);
        ICON_COLOR_MAP.put(R.id.icon_internet, R.color.candy_internet);
        ICON_COLOR_MAP.put(R.id.icon_export, R.color.candy_export);
        ICON_COLOR_MAP.put(R.id.icon_backup, R.color.candy_backup);
        ICON_COLOR_MAP.put(R.id.icon_restore, R.color.candy_restore);

        // Home page icons
        ICON_COLOR_MAP.put(R.id.icon_invocation, R.color.candy_invocation);
        ICON_COLOR_MAP.put(R.id.icon_web_search, R.color.candy_web_search);
    }

    /**
     * Apply candy colors to the entire view hierarchy.
     * Call this from any Activity's onCreate after setContentView,
     * or any Fragment's onViewCreated.
     * 
     * @param context  The context
     * @param rootView The root view of the layout
     */
    public static void applyToViewHierarchy(Context context, View rootView) {
        if (!shouldApplyCandyColors(context) || rootView == null) {
            return;
        }

        traverseAndColor(context, rootView, new CandyColorIterator());
    }

    /**
     * Traverse view hierarchy and apply candy colors to all ImageViews.
     */
    private static void traverseAndColor(Context context, View view, CandyColorIterator colorIterator) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            int viewId = imageView.getId();

            // Check if this icon has a specific mapped color
            if (viewId != View.NO_ID && ICON_COLOR_MAP.containsKey(viewId)) {
                int colorRes = ICON_COLOR_MAP.get(viewId);
                imageView.setColorFilter(ContextCompat.getColor(context, colorRes));
            } else {
                // Apply rotating candy color for unmapped icons
                int colorRes = colorIterator.next();
                imageView.setColorFilter(ContextCompat.getColor(context, colorRes));
            }
        }

        // Recurse into ViewGroups
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                traverseAndColor(context, viewGroup.getChildAt(i), colorIterator);
            }
        }
    }

    /**
     * Helper class to iterate through candy colors cyclically.
     */
    private static class CandyColorIterator {
        private int currentIndex = 0;

        public int next() {
            int color = CANDY_COLORS[currentIndex % CANDY_COLORS.length];
            currentIndex++;
            return color;
        }
    }

    /**
     * Apply candy colors to settings icons when Material You is disabled.
     * Call this method in SettingsFragment after views are initialized.
     */
    public static void applySettingsColors(Context context,
            ImageView darkModeIcon,
            ImageView amoledIcon,
            ImageView blurIcon,
            ImageView materialYouIcon,
            ImageView loggingIcon,
            ImageView internetIcon,
            ImageView exportIcon,
            ImageView backupIcon,
            ImageView restoreIcon) {

        if (!MaterialYouManager.getInstance(context).isMaterialYouEnabled()) {
            if (darkModeIcon != null) {
                darkModeIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_dark_mode));
            }
            if (amoledIcon != null) {
                amoledIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_amoled));
            }
            if (blurIcon != null) {
                blurIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_blur));
            }
            if (materialYouIcon != null) {
                materialYouIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_material_you));
            }
            if (loggingIcon != null) {
                loggingIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_logging));
            }
            if (internetIcon != null) {
                internetIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_internet));
            }
            if (exportIcon != null) {
                exportIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_export));
            }
            if (backupIcon != null) {
                backupIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_backup));
            }
            if (restoreIcon != null) {
                restoreIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_restore));
            }
        }
    }

    /**
     * Apply candy colors to home page quick action icons.
     */
    public static void applyQuickActionsColors(Context context,
            ImageView modelIcon,
            ImageView invocationIcon,
            ImageView webSearchIcon,
            ImageView labIcon) {

        if (!MaterialYouManager.getInstance(context).isMaterialYouEnabled()) {
            if (modelIcon != null) {
                modelIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_model));
            }
            if (invocationIcon != null) {
                invocationIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_invocation));
            }
            if (webSearchIcon != null) {
                webSearchIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_web_search));
            }
            if (labIcon != null) {
                labIcon.setColorFilter(ContextCompat.getColor(context, R.color.candy_lab));
            }
        }
    }

    /**
     * Get candy color for a specific setting by name.
     * Useful for dynamic color application.
     */
    public static int getCandyColor(Context context, String settingName) {
        switch (settingName.toLowerCase()) {
            case "dark_mode":
                return ContextCompat.getColor(context, R.color.candy_dark_mode);
            case "amoled":
                return ContextCompat.getColor(context, R.color.candy_amoled);
            case "blur":
                return ContextCompat.getColor(context, R.color.candy_blur);
            case "material_you":
                return ContextCompat.getColor(context, R.color.candy_material_you);
            case "logging":
                return ContextCompat.getColor(context, R.color.candy_logging);
            case "internet":
                return ContextCompat.getColor(context, R.color.candy_internet);
            case "export":
                return ContextCompat.getColor(context, R.color.candy_export);
            case "backup":
                return ContextCompat.getColor(context, R.color.candy_backup);
            case "restore":
                return ContextCompat.getColor(context, R.color.candy_restore);
            case "model":
                return ContextCompat.getColor(context, R.color.candy_model);
            case "invocation":
                return ContextCompat.getColor(context, R.color.candy_invocation);
            case "web_search":
                return ContextCompat.getColor(context, R.color.candy_web_search);
            case "lab":
                return ContextCompat.getColor(context, R.color.candy_lab);
            default:
                return ContextCompat.getColor(context, R.color.candy_blur); // Default blue
        }
    }

    /**
     * Check if candy colors should be applied (Material You is OFF).
     */
    public static boolean shouldApplyCandyColors(Context context) {
        return !MaterialYouManager.getInstance(context).isMaterialYouEnabled();
    }
}
