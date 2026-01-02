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
package tn.eluea.kgpt.textactions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import tn.eluea.kgpt.MainHook;
import tn.eluea.kgpt.R;

/**
 * Floating menu that appears above selected text with AI action options.
 */
public class FloatingActionMenu {
    
    private static final int ANIMATION_DURATION = 200;
    private static final int AUTO_HIDE_DELAY = 8000; // 8 seconds
    
    private final Context context;
    private final TextActionManager actionManager;
    private final OnActionSelectedListener listener;
    
    private WindowManager windowManager;
    private View menuView;
    private boolean isShowing = false;
    private String currentSelectedText;
    
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = this::hide;
    
    public interface OnActionSelectedListener {
        void onActionSelected(TextAction action, String selectedText);
    }
    
    public FloatingActionMenu(Context context, TextActionManager actionManager, OnActionSelectedListener listener) {
        this.context = context;
        this.actionManager = actionManager;
        this.listener = listener;
    }
    
    /**
     * Show the floating menu at the specified position.
     */
    public void show(String selectedText, int x, int y) {
        if (isShowing) {
            hide();
        }
        
        if (selectedText == null || selectedText.trim().isEmpty()) {
            return;
        }
        
        currentSelectedText = selectedText;
        
        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            menuView = createMenuView();
            
            WindowManager.LayoutParams params = createLayoutParams(x, y);
            windowManager.addView(menuView, params);
            
            animateIn();
            isShowing = true;
            
            // Schedule auto-hide
            autoHideHandler.removeCallbacks(autoHideRunnable);
            autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
            
            MainHook.log("FloatingActionMenu shown");
        } catch (Exception e) {
            MainHook.log("Failed to show FloatingActionMenu: " + e.getMessage());
        }
    }
    
    /**
     * Hide the floating menu.
     */
    public void hide() {
        if (!isShowing || menuView == null) {
            return;
        }
        
        autoHideHandler.removeCallbacks(autoHideRunnable);
        
        animateOut(() -> {
            try {
                if (windowManager != null && menuView != null) {
                    windowManager.removeView(menuView);
                }
            } catch (Exception e) {
                MainHook.log("Error removing FloatingActionMenu: " + e.getMessage());
            }
            menuView = null;
            isShowing = false;
            currentSelectedText = null;
        });
    }
    
    /**
     * Check if the menu is currently showing.
     */
    public boolean isShowing() {
        return isShowing;
    }
    
    /**
     * Create the menu view with action buttons.
     */
    private View createMenuView() {
        // Main container
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        
        // Menu card
        LinearLayout menuCard = new LinearLayout(context);
        menuCard.setOrientation(LinearLayout.HORIZONTAL);
        menuCard.setGravity(Gravity.CENTER_VERTICAL);
        menuCard.setPadding(dp(8), dp(8), dp(8), dp(8));
        menuCard.setBackground(createMenuBackground());
        menuCard.setElevation(dp(8));
        
        // Scroll view for actions
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        // Actions container
        LinearLayout actionsContainer = new LinearLayout(context);
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionsContainer.setGravity(Gravity.CENTER_VERTICAL);
        
        // Add action buttons
        List<TextAction> actions = actionManager.getEnabledActions();
        boolean showLabels = actionManager.shouldShowLabels();
        
        for (int i = 0; i < actions.size(); i++) {
            TextAction action = actions.get(i);
            View actionView = createActionButton(action, showLabels);
            actionsContainer.addView(actionView);
            
            // Add divider between actions
            if (i < actions.size() - 1) {
                actionsContainer.addView(createDivider());
            }
        }
        
        // Add close button
        actionsContainer.addView(createDivider());
        actionsContainer.addView(createCloseButton());
        
        scrollView.addView(actionsContainer);
        menuCard.addView(scrollView);
        container.addView(menuCard);
        
        // Arrow pointing down
        container.addView(createArrow());
        
        return container;
    }
    
    /**
     * Create an action button.
     */
    private View createActionButton(TextAction action, boolean showLabel) {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setClickable(true);
        button.setFocusable(true);
        
        // Ripple effect
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        button.setBackgroundResource(outValue.resourceId);
        
        // Icon
        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(24), dp(24)));
        try {
            icon.setImageResource(action.iconRes);
            icon.setColorFilter(Color.parseColor(action.color));
        } catch (Exception e) {
            // Fallback if icon not found
            icon.setImageResource(android.R.drawable.ic_menu_edit);
            icon.setColorFilter(Color.WHITE);
        }
        button.addView(icon);
        
        // Label
        if (showLabel) {
            TextView label = new TextView(context);
            label.setText(action.labelEn);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            label.setTextColor(Color.parseColor("#B3FFFFFF")); // 70% white
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.topMargin = dp(2);
            label.setLayoutParams(labelParams);
            button.addView(label);
        }
        
        // Click listener
        button.setOnClickListener(v -> {
            if (listener != null && currentSelectedText != null) {
                listener.onActionSelected(action, currentSelectedText);
            }
            hide();
        });
        
        return button;
    }
    
    /**
     * Create close button.
     */
    private View createCloseButton() {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));
        button.setClickable(true);
        button.setFocusable(true);
        
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
        button.setBackgroundResource(outValue.resourceId);
        
        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        icon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        icon.setColorFilter(Color.parseColor("#80FFFFFF")); // 50% white
        button.addView(icon);
        
        button.setOnClickListener(v -> hide());
        
        return button;
    }
    
    /**
     * Create a vertical divider.
     */
    private View createDivider() {
        View divider = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(24));
        params.setMargins(dp(4), 0, dp(4), 0);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#33FFFFFF")); // 20% white
        return divider;
    }
    
    /**
     * Create the arrow pointing to the selected text.
     */
    private View createArrow() {
        View arrow = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(12), dp(8));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        arrow.setLayoutParams(params);
        
        GradientDrawable arrowDrawable = new GradientDrawable();
        arrowDrawable.setShape(GradientDrawable.RECTANGLE);
        arrowDrawable.setColor(Color.parseColor("#E6212121"));
        arrow.setBackground(arrowDrawable);
        arrow.setRotation(45);
        
        return arrow;
    }
    
    /**
     * Create the menu background drawable.
     */
    private GradientDrawable createMenuBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.parseColor("#E6212121")); // 90% dark
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), Color.parseColor("#33FFFFFF")); // 20% white border
        return background;
    }
    
    /**
     * Create window layout params.
     */
    private WindowManager.LayoutParams createLayoutParams(int x, int y) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.max(0, x - dp(100)); // Center roughly
        params.y = Math.max(0, y - dp(80)); // Above the selection
        
        return params;
    }
    
    /**
     * Animate the menu in.
     */
    private void animateIn() {
        if (menuView == null) return;
        
        menuView.setAlpha(0f);
        menuView.setScaleX(0.8f);
        menuView.setScaleY(0.8f);
        menuView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();
    }
    
    /**
     * Animate the menu out.
     */
    private void animateOut(Runnable onComplete) {
        if (menuView == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        menuView.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(ANIMATION_DURATION / 2)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) onComplete.run();
                }
            })
            .start();
    }
    
    /**
     * Convert dp to pixels.
     */
    private int dp(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.getResources().getDisplayMetrics()
        );
    }
}
