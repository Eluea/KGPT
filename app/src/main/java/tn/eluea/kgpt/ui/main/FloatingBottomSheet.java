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

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;

import tn.eluea.kgpt.R;

import android.view.ContextThemeWrapper;
import tn.eluea.kgpt.util.MaterialYouManager;

/**
 * A floating card-style bottom sheet dialog
 * Shows content as a floating card with rounded corners on all sides
 * Note: Blur is handled by the parent Activity (DialogActivity), not by this
 * dialog
 */
public class FloatingBottomSheet extends Dialog {

    private View contentView;
    private android.widget.FrameLayout wrapper;
    private boolean isDismissing = false;

    // Flag to indicate if blur should be managed by this dialog (for standalone
    // use)
    private boolean manageOwnBlur = true;

    public FloatingBottomSheet(@NonNull Context context) {
        this(context, R.style.FloatingBottomSheetTheme);
    }

    public FloatingBottomSheet(@NonNull Context context, int themeResId) {
        super(createThemedContext(context, themeResId), 0);
        // Check if we're inside DialogActivity - if so, don't manage blur
        if (context instanceof tn.eluea.kgpt.core.ui.DialogActivity) {
            manageOwnBlur = false;
        } else if (context instanceof ContextThemeWrapper) {
            Context baseContext = ((ContextThemeWrapper) context).getBaseContext();
            if (baseContext instanceof tn.eluea.kgpt.core.ui.DialogActivity) {
                manageOwnBlur = false;
            }
        }
    }

    private static Context createThemedContext(Context context, int themeResId) {
        Context baseContext = new ContextThemeWrapper(context, themeResId);
        return MaterialYouManager.getInstance(context).wrapContext(baseContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        if (window != null) {
            // Make window transparent
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set window attributes
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.CENTER;

            // Only apply blur if managing own blur (standalone dialogs, not inside
            // DialogActivity)
            if (manageOwnBlur) {
                SharedPreferences prefs = getContext().getSharedPreferences("keyboard_gpt_ui", Context.MODE_PRIVATE);
                boolean isBlurEnabled = prefs.getBoolean("blur_enabled", true);
                int blurIntensity = prefs.getInt("blur_intensity", 25);
                int blurTintColor = prefs.getInt("blur_tint_color", Color.TRANSPARENT);

                if (isBlurEnabled && blurIntensity > 0) {
                    // Blur is enabled
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        int blurRadius = (blurIntensity * 50) / 100;
                        params.setBlurBehindRadius(blurRadius);
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    }

                    // Apply Tint or Dim
                    if (blurTintColor != Color.TRANSPARENT) {
                        int alpha = 120;
                        int colorWithAlpha = Color.argb(alpha, Color.red(blurTintColor), Color.green(blurTintColor),
                                Color.blue(blurTintColor));
                        window.setBackgroundDrawable(new ColorDrawable(colorWithAlpha));
                        params.dimAmount = 0f;
                    } else {
                        params.dimAmount = 0.5f;
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                } else {
                    // Blur is disabled - use standard dim effect
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        params.setBlurBehindRadius(0);
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    }
                    params.dimAmount = 0.5f;
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
            } else {
                // Inside DialogActivity - no blur, no dim (Activity handles it)
                params.dimAmount = 0f;
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            window.setAttributes(params);
            // Edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, true);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(false);
            }

            // Set navigation bar icons color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                if (!BottomSheetHelper.isDarkMode(getContext())) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                decorView.setSystemUiVisibility(flags);
            }
        }
    }

    @Override
    public void setContentView(@NonNull View view) {
        this.contentView = view;

        // Apply theme to the content
        BottomSheetHelper.applyTheme(getContext(), view);

        // Create wrapper with margins
        Context context = getContext();
        int margin = (int) (16 * context.getResources().getDisplayMetrics().density);

        wrapper = new android.widget.FrameLayout(context);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        wrapper.setOnClickListener(v -> dismiss());
        view.setClickable(true);

        android.widget.FrameLayout.LayoutParams contentParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.BOTTOM;
        contentParams.setMargins(margin, 0, margin, margin);

        wrapper.addView(view, contentParams);

        super.setContentView(wrapper);
    }

    /**
     * Smoothly transition to new content with crossfade animation
     */
    public void transitionToContent(@NonNull View newView, Runnable onComplete) {
        if (wrapper == null || contentView == null) {
            setContentView(newView);
            if (onComplete != null)
                onComplete.run();
            return;
        }

        // Apply theme to new content
        BottomSheetHelper.applyTheme(getContext(), newView);
        newView.setClickable(true);

        Context context = getContext();
        int margin = (int) (16 * context.getResources().getDisplayMetrics().density);

        android.widget.FrameLayout.LayoutParams contentParams = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.BOTTOM;
        contentParams.setMargins(margin, 0, margin, margin);

        // Prepare new view
        newView.setAlpha(0f);
        wrapper.addView(newView, contentParams);

        final View oldView = contentView;
        contentView = newView;

        // Crossfade animation
        oldView.animate()
                .alpha(0f)
                .setDuration(150)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .start();

        newView.animate()
                .alpha(1f)
                .setDuration(150)
                .setStartDelay(50)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    wrapper.removeView(oldView);
                    if (onComplete != null)
                        onComplete.run();
                })
                .start();
    }

    @Override
    public void show() {
        super.show();

        // Animate in - card slides up
        if (contentView != null) {
            Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
            contentView.startAnimation(slideUp);
        }
    }

    @Override
    public void dismiss() {
        if (isDismissing)
            return;
        isDismissing = true;

        if (contentView != null) {
            Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    FloatingBottomSheet.super.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            contentView.startAnimation(slideDown);
        } else {
            super.dismiss();
        }
    }

    /**
     * Dismiss instantly without any animation - used when switching between dialogs
     */
    public void dismissInstant() {
        if (isDismissing)
            return;
        isDismissing = true;
        super.dismiss();
    }

    /**
     * Get the current content view
     */
    public View getContentView() {
        return contentView;
    }
}
