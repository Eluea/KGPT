/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 */
package tn.eluea.kgpt.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated view that simulates snowfall overlay.
 * Designed to be placed on top of other views and pass touches through.
 */
public class SnowfallView extends View {

    private final List<SnowFlake> flakes = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;
    private int width, height;

    private static final int FLAKE_COUNT = 250;
    private static final float SPEED_MULTIPLIER = 1.5f;

    public SnowfallView(Context context) {
        this(context, null);
    }

    public SnowfallView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SnowfallView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        updateSnowColor();
        paint.setStyle(Paint.Style.FILL);
    }

    private void updateSnowColor() {
        int nightModeFlags = getContext().getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            paint.setColor(0xFFFFFFFF); // White in dark mode
        } else {
            paint.setColor(0xFF90A4AE); // Blue Grey in light mode for visibility
        }
    }

    @Override
    protected void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateSnowColor();
        invalidate();
    }

    public void setSpeedMultiplier(float multiplier) {
        // Not used currently as SPEED_MULTIPLIER is static final, but good for future
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        if (width > 0 && height > 0) {
            initFlakes();
            startAnimation();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private void initFlakes() {
        flakes.clear();
        for (int i = 0; i < FLAKE_COUNT; i++) {
            flakes.add(createFlake(true));
        }
    }

    private SnowFlake createFlake(boolean startRandomY) {
        float x = random.nextFloat() * width;
        float y = startRandomY ? random.nextFloat() * height : -20f;
        float size = 2f + random.nextFloat() * 6f; // Size between 2 and 8
        float speed = 2f + random.nextFloat() * 4f; // Falling speed
        float wind = -0.5f + random.nextFloat() * 1f; // Horizontal drift
        float alpha = 0.5f + random.nextFloat() * 0.5f; // Opacity
        return new SnowFlake(x, y, size, speed, wind, alpha);
    }

    private void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000); // Duration doesn't matter much for infinite loop
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            updateFlakes();
            invalidate();
        });
        animator.start();
    }

    private final List<android.graphics.Rect> obstacles = new ArrayList<>();
    private boolean isShakingOff = false;

    // ... existing init ...

    public void updateObstacles(List<android.graphics.Rect> newObstacles) {
        obstacles.clear();
        int[] location = new int[2];
        getLocationOnScreen(location);
        int dx = location[0];
        int dy = location[1];

        for (android.graphics.Rect r : newObstacles) {
            // Convert global screen coordinates to local view coordinates
            android.graphics.Rect localRect = new android.graphics.Rect(r);
            localRect.offset(-dx, -dy);
            obstacles.add(localRect);
        }
    }

    public void shakeOff() {
        isShakingOff = true;
        obstacles.clear();
        for (SnowFlake flake : flakes) {
            flake.landed = false;
        }
        // Reset flag after a short delay so new obstacles can be added
        postDelayed(() -> isShakingOff = false, 500);
    }

    private android.graphics.Rect fingerRect = null;

    public void updateFinger(float x, float y, boolean active) {
        if (active) {
            int size = 150; // Touch area size
            if (fingerRect == null)
                fingerRect = new android.graphics.Rect();
            fingerRect.set((int) x - size / 2, (int) y - size / 2, (int) x + size / 2, (int) y + size / 2);
        } else {
            fingerRect = null;
        }
    }

    private void updateFlakes() {
        if (width == 0 || height == 0)
            return;

        for (SnowFlake flake : flakes) {
            if (flake.landed) {
                if (isShakingOff) {
                    flake.landed = false;
                } else {
                    boolean supported = false;

                    // Check finger support
                    if (fingerRect != null && fingerRect.contains((int) flake.x, (int) flake.y + 5)) {
                        supported = true;
                    }

                    // Check static obstacles support
                    if (!supported) {
                        for (android.graphics.Rect rect : obstacles) {
                            if (rect.contains((int) flake.x, (int) flake.y + 5)) {
                                supported = true;
                                break;
                            }
                        }
                    }

                    if (!supported) {
                        flake.landed = false;
                    } else {
                        // Flake is stable. Check life.
                        flake.landedLife--;
                        if (flake.landedLife <= 0) {
                            // Melted: recycle to top
                            SnowFlake newFlake = createFlake(false);
                            flake.copyFrom(newFlake);
                        }
                        continue;
                    }
                }
            }

            flake.y += flake.speed * SPEED_MULTIPLIER;
            flake.x += flake.wind * SPEED_MULTIPLIER;

            // Wobble effect
            flake.x += Math.sin(flake.y / 50f) * 0.5f;

            // Collision Detection
            if (!isShakingOff) {
                // Check finger collision
                if (fingerRect != null && fingerRect.contains((int) flake.x, (int) flake.y)
                        && flake.y < fingerRect.top + 20) {
                    flake.y = fingerRect.top + random.nextFloat() * 5;
                    flake.landed = true;
                    flake.landedLife = 300 + random.nextInt(300);
                    continue; // Skip obstacle check
                }

                // Check obstacles collision
                if (!obstacles.isEmpty()) {
                    for (android.graphics.Rect rect : obstacles) {
                        if (rect.contains((int) flake.x, (int) flake.y) && flake.y < rect.top + 20) {
                            int cornerPadding = 40;
                            if (flake.x >= rect.left + cornerPadding && flake.x <= rect.right - cornerPadding) {
                                flake.y = rect.top + random.nextFloat() * 5;
                                flake.landed = true;
                                flake.landedLife = 300 + random.nextInt(300);
                                break;
                            }
                        }
                    }
                }
            }

            // Reset if out of bounds
            if (flake.y > height + 20) {
                SnowFlake newFlake = createFlake(false);
                flake.copyFrom(newFlake);
            }
            if (flake.x < -20) {
                flake.x = width + 20;
            } else if (flake.x > width + 20) {
                flake.x = -20;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (SnowFlake flake : flakes) {
            paint.setAlpha((int) (flake.alpha * 255));
            canvas.drawCircle(flake.x, flake.y, flake.size / 2, paint);
        }
    }

    private static class SnowFlake {
        float x, y, size, speed, wind, alpha;
        boolean landed = false;
        int landedLife = 0;

        SnowFlake(float x, float y, float size, float speed, float wind, float alpha) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.wind = wind;
            this.alpha = alpha;
            this.landed = false;
            this.landedLife = 0;
        }

        void copyFrom(SnowFlake other) {
            this.x = other.x;
            this.y = other.y;
            this.size = other.size;
            this.speed = other.speed;
            this.wind = other.wind;
            this.alpha = other.alpha;
            this.landed = false;
            this.landedLife = 0;
        }
    }
}
