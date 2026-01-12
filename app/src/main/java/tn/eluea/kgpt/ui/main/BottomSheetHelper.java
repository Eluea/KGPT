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
package tn.eluea.kgpt.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import tn.eluea.kgpt.R;

/**
 * Helper class to apply consistent theming to Bottom Sheets
 * Handles Light, Dark, and AMOLED modes
 * Creates floating card-style bottom sheets
 */
public class BottomSheetHelper {

    private static final String PREF_NAME = "keyboard_gpt_ui";
    private static final String PREF_AMOLED = "amoled_mode";

    /**
     * Apply theme to bottom sheet view
     * This applies the correct background color based on theme
     * 
     * @param context   The context
     * @param sheetView The root view of the bottom sheet
     */
    public static void applyTheme(Context context, View sheetView) {
        boolean isDarkMode = isDarkMode(context);
        boolean isAmoled = isAmoledMode(context);

        // Apply to container with rounded corners
        View container = sheetView.findViewById(R.id.bottom_sheet_container);
        if (container == null && sheetView.getId() == R.id.bottom_sheet_container) {
            container = sheetView;
        }

        if (container != null) {
            if (isDarkMode && isAmoled) {
                container.setBackgroundResource(R.drawable.bg_bottom_sheet_floating_amoled);
            } else {
                // Use the standard drawable which relies on ?attr/colorSurface
                // This ensures Material You dynamic colors work in both Light and Dark modes
                container.setBackgroundResource(R.drawable.bg_bottom_sheet_floating);
            }
        }
    }

    /**
     * Show a floating bottom sheet dialog
     * 
     * @param context     The context
     * @param layoutResId The layout resource ID
     * @return The FloatingBottomSheet dialog
     */
    public static FloatingBottomSheet showFloating(Context context, int layoutResId) {
        FloatingBottomSheet dialog = new FloatingBottomSheet(context);
        View sheetView = android.view.LayoutInflater.from(context).inflate(layoutResId, null);
        dialog.setContentView(sheetView);
        return dialog;
    }

    /**
     * Show a floating bottom sheet dialog with a custom view
     * 
     * @param context The context
     * @param view    The content view
     * @return The FloatingBottomSheet dialog
     */
    public static FloatingBottomSheet showFloating(Context context, View view) {
        FloatingBottomSheet dialog = new FloatingBottomSheet(context);
        dialog.setContentView(view);
        return dialog;
    }

    /**
     * Make bottom sheet floating card style with margins
     * 
     * @param dialog    The BottomSheetDialog
     * @param sheetView The content view of the bottom sheet
     */
    public static void makeEdgeToEdge(BottomSheetDialog dialog, View sheetView) {
        Window window = dialog.getWindow();
        if (window == null)
            return;

        Context context = sheetView.getContext();

        // Make window edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Set navigation bar transparent
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Set dialog background to transparent
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        // Set light/dark navigation bar icons based on theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (!isDarkMode(context)) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }

        int margin = (int) (16 * context.getResources().getDisplayMetrics().density);
        int navBarHeight = getNavigationBarHeight(context);

        // Set content view
        dialog.setContentView(sheetView);

        // Apply blur
        applyBlur(dialog);

        // Set up the dialog after it's shown
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            // Find the bottom sheet frame
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Make background completely transparent
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                bottomSheet.setBackground(null);

                // Get the behavior and expand
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setPeekHeight(0);

                // Set margins on the bottom sheet container
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomSheet.getLayoutParams();
                params.leftMargin = margin;
                params.rightMargin = margin;
                params.bottomMargin = navBarHeight + margin;
                bottomSheet.setLayoutParams(params);
            }
        });
    }

    /**
     * Make bottom sheet floating card style (alternative method for custom dialogs)
     * 
     * @param dialog     The BottomSheetDialog
     * @param sheetView  The content view of the bottom sheet
     * @param isFloating Whether to use floating card style
     */
    public static void makeFloating(BottomSheetDialog dialog, View sheetView, boolean isFloating) {
        if (isFloating) {
            makeEdgeToEdge(dialog, sheetView);
        } else {
            // Original edge-to-edge behavior
            makeEdgeToEdgeOriginal(dialog, sheetView);
        }
    }

    /**
     * Original edge-to-edge implementation (for web search bottom sheet)
     */
    public static void makeEdgeToEdgeOriginal(BottomSheetDialog dialog, View sheetView) {
        Window window = dialog.getWindow();
        if (window == null)
            return;

        Context context = sheetView.getContext();

        // Make window edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Set navigation bar transparent so our content shows through
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        // Set light/dark navigation bar icons based on theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (!isDarkMode(context)) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }

        // Apply blur
        applyBlur(dialog);

        // Set up the dialog after it's shown
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            // Find the bottom sheet frame (Material's internal container)
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // Set the background with rounded top corners on the Material bottom sheet
                // container
                int backgroundRes = getBottomSheetBackgroundDrawable(context);
                bottomSheet.setBackgroundResource(backgroundRes);

                // Get the behavior and expand
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                // Add padding to the content for navigation bar
                int navBarHeight = getNavigationBarHeight(context);
                if (navBarHeight > 0) {
                    bottomSheet.setPadding(0, 0, 0, navBarHeight);
                    bottomSheet.setClipToPadding(false);
                }
            }
        });
    }

    /**
     * Get the appropriate background drawable for the bottom sheet based on theme
     */
    private static int getBottomSheetBackgroundDrawable(Context context) {
        boolean isDarkMode = isDarkMode(context);
        boolean isAmoled = isAmoledMode(context);

        if (isDarkMode && isAmoled) {
            return R.drawable.bg_bottom_sheet_material_amoled;
        } else {
            return R.drawable.bg_bottom_sheet_material;
        }
    }

    /**
     * Get navigation bar height
     */
    private static int getNavigationBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * Get the appropriate background color for the bottom sheet based on theme
     */
    private static int getBottomSheetBackgroundColor(Context context) {
        boolean isDarkMode = isDarkMode(context);
        boolean isAmoled = isAmoledMode(context);

        if (isDarkMode && isAmoled) {
            return context.getResources().getColor(R.color.surface_amoled, context.getTheme());
        } else if (isDarkMode) {
            return context.getResources().getColor(R.color.surface_dark, context.getTheme());
        } else {
            return context.getResources().getColor(R.color.surface_color, context.getTheme());
        }
    }

    /**
     * Check if dark mode is enabled using system configuration
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Check if AMOLED mode is enabled from preferences
     */
    public static boolean isAmoledMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AMOLED, false);
    }

    /**
     * Apply blur effect to dialog based on preferences
     */
    public static void applyBlur(android.app.Dialog dialog) {
        Context context = dialog.getContext();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isBlurEnabled = prefs.getBoolean("blur_enabled", true);
        int blurIntensity = prefs.getInt("blur_intensity", 25);

        Window window = dialog.getWindow();
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            if (isBlurEnabled && blurIntensity > 0) {
                int blurRadius = (blurIntensity * 50) / 100;
                params.setBlurBehindRadius(blurRadius);
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            } else {
                params.setBlurBehindRadius(0);
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            }
            window.setAttributes(params);
        }
    }
}
