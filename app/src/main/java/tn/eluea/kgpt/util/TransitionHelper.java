/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package tn.eluea.kgpt.util;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.transition.AutoTransition;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

/**
 * Unified transition helper for smooth animations across the app.
 * Use this for bottom sheets, dialogs, cards, and any container transitions.
 */
public class TransitionHelper {

    // Default durations
    public static final int DURATION_FAST = 200;
    public static final int DURATION_NORMAL = 350;
    public static final int DURATION_SLOW = 500;

    /**
     * Apply smooth auto transition to a container.
     * Best for general layout changes (show/hide views, size changes).
     * 
     * @param container The ViewGroup to animate
     */
    public static void beginTransition(ViewGroup container) {
        beginTransition(container, DURATION_NORMAL);
    }

    /**
     * Apply smooth auto transition with custom duration.
     * 
     * @param container The ViewGroup to animate
     * @param duration  Animation duration in milliseconds
     */
    public static void beginTransition(ViewGroup container, int duration) {
        if (container == null) return;
        
        AutoTransition transition = new AutoTransition();
        transition.setDuration(duration);
        transition.setInterpolator(new DecelerateInterpolator());
        TransitionManager.beginDelayedTransition(container, transition);
    }

    /**
     * Apply fade transition only.
     * Best for simple show/hide without layout changes.
     * 
     * @param container The ViewGroup to animate
     */
    public static void beginFadeTransition(ViewGroup container) {
        beginFadeTransition(container, DURATION_NORMAL);
    }

    /**
     * Apply fade transition with custom duration.
     * 
     * @param container The ViewGroup to animate
     * @param duration  Animation duration in milliseconds
     */
    public static void beginFadeTransition(ViewGroup container, int duration) {
        if (container == null) return;
        
        Fade fade = new Fade();
        fade.setDuration(duration);
        fade.setInterpolator(new DecelerateInterpolator());
        TransitionManager.beginDelayedTransition(container, fade);
    }

    /**
     * Apply bounds change transition.
     * Best for size/position changes without fade.
     * 
     * @param container The ViewGroup to animate
     */
    public static void beginBoundsTransition(ViewGroup container) {
        beginBoundsTransition(container, DURATION_NORMAL);
    }

    /**
     * Apply bounds change transition with custom duration.
     * 
     * @param container The ViewGroup to animate
     * @param duration  Animation duration in milliseconds
     */
    public static void beginBoundsTransition(ViewGroup container, int duration) {
        if (container == null) return;
        
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(duration);
        changeBounds.setInterpolator(new DecelerateInterpolator());
        TransitionManager.beginDelayedTransition(container, changeBounds);
    }

    /**
     * Apply combined fade and bounds transition.
     * Best for complex layout changes with both visibility and size changes.
     * 
     * @param container The ViewGroup to animate
     */
    public static void beginCombinedTransition(ViewGroup container) {
        beginCombinedTransition(container, DURATION_NORMAL);
    }

    /**
     * Apply combined fade and bounds transition with custom duration.
     * 
     * @param container The ViewGroup to animate
     * @param duration  Animation duration in milliseconds
     */
    public static void beginCombinedTransition(ViewGroup container, int duration) {
        if (container == null) return;
        
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(new Fade());
        transitionSet.addTransition(new ChangeBounds());
        transitionSet.setDuration(duration);
        transitionSet.setInterpolator(new DecelerateInterpolator());
        transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
        TransitionManager.beginDelayedTransition(container, transitionSet);
    }

    /**
     * Apply bouncy/elastic transition.
     * Best for playful UI elements or success states.
     * 
     * @param container The ViewGroup to animate
     */
    public static void beginBouncyTransition(ViewGroup container) {
        beginBouncyTransition(container, DURATION_NORMAL);
    }

    /**
     * Apply bouncy/elastic transition with custom duration.
     * 
     * @param container The ViewGroup to animate
     * @param duration  Animation duration in milliseconds
     */
    public static void beginBouncyTransition(ViewGroup container, int duration) {
        if (container == null) return;
        
        AutoTransition transition = new AutoTransition();
        transition.setDuration(duration);
        transition.setInterpolator(new OvershootInterpolator(1.2f));
        TransitionManager.beginDelayedTransition(container, transition);
    }

    // ============ View Animation Helpers ============

    /**
     * Fade in a view with scale animation.
     * 
     * @param view The view to animate
     */
    public static void fadeInWithScale(View view) {
        fadeInWithScale(view, DURATION_NORMAL);
    }

    /**
     * Fade in a view with scale animation and custom duration.
     * 
     * @param view     The view to animate
     * @param duration Animation duration in milliseconds
     */
    public static void fadeInWithScale(View view, int duration) {
        if (view == null) return;
        
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Fade out a view with scale animation.
     * 
     * @param view The view to animate
     */
    public static void fadeOutWithScale(View view) {
        fadeOutWithScale(view, DURATION_NORMAL, null);
    }

    /**
     * Fade out a view with scale animation and callback.
     * 
     * @param view     The view to animate
     * @param duration Animation duration in milliseconds
     * @param onEnd    Callback when animation ends
     */
    public static void fadeOutWithScale(View view, int duration, Runnable onEnd) {
        if (view == null) return;
        
        view.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setAlpha(1f);
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    /**
     * Slide in a view from bottom.
     * 
     * @param view The view to animate
     */
    public static void slideInFromBottom(View view) {
        slideInFromBottom(view, DURATION_NORMAL);
    }

    /**
     * Slide in a view from bottom with custom duration.
     * 
     * @param view     The view to animate
     * @param duration Animation duration in milliseconds
     */
    public static void slideInFromBottom(View view, int duration) {
        if (view == null) return;
        
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(50f);
        
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Slide out a view to bottom.
     * 
     * @param view The view to animate
     */
    public static void slideOutToBottom(View view) {
        slideOutToBottom(view, DURATION_NORMAL, null);
    }

    /**
     * Slide out a view to bottom with callback.
     * 
     * @param view     The view to animate
     * @param duration Animation duration in milliseconds
     * @param onEnd    Callback when animation ends
     */
    public static void slideOutToBottom(View view, int duration, Runnable onEnd) {
        if (view == null) return;
        
        view.animate()
                .alpha(0f)
                .translationY(50f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setTranslationY(0f);
                    view.setAlpha(1f);
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    /**
     * Pop in animation (scale from 0 to 1 with overshoot).
     * Best for success icons, checkmarks, etc.
     * 
     * @param view The view to animate
     */
    public static void popIn(View view) {
        popIn(view, DURATION_NORMAL);
    }

    /**
     * Pop in animation with custom duration.
     * 
     * @param view     The view to animate
     * @param duration Animation duration in milliseconds
     */
    public static void popIn(View view, int duration) {
        if (view == null) return;
        
        view.setVisibility(View.VISIBLE);
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.setAlpha(0f);
        
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
    }
}
