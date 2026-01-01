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

/**
 * A floating card-style bottom sheet dialog
 * Shows content as a floating card with rounded corners on all sides
 */
public class FloatingBottomSheet extends Dialog {

    private View contentView;
    private boolean isDismissing = false;

    public FloatingBottomSheet(@NonNull Context context) {
        super(context, R.style.FloatingBottomSheetTheme);
    }

    public FloatingBottomSheet(@NonNull Context context, int themeResId) {
        super(context, themeResId);
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
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;
            params.dimAmount = 0.5f;
            window.setAttributes(params);
            
            // Edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false);
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
        int navBarHeight = getNavigationBarHeight(context);
        
        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(context);
        wrapper.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        android.widget.FrameLayout.LayoutParams contentParams = new android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        contentParams.setMargins(margin, 0, margin, navBarHeight + margin);
        
        wrapper.addView(view, contentParams);
        
        super.setContentView(wrapper);
    }

    @Override
    public void show() {
        super.show();
        
        // Animate in
        if (contentView != null) {
            Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up);
            contentView.startAnimation(slideUp);
        }
    }

    @Override
    public void dismiss() {
        if (isDismissing) return;
        isDismissing = true;
        
        if (contentView != null) {
            Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down);
            slideDown.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    FloatingBottomSheet.super.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            contentView.startAnimation(slideDown);
        } else {
            super.dismiss();
        }
    }

    private int getNavigationBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
