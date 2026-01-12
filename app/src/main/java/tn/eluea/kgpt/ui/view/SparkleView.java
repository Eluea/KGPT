/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT.
 */
package tn.eluea.kgpt.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A view that displays sparkle particles around its bounds.
 * Used to create an attention-grabbing effect around buttons.
 */
public class SparkleView extends View {

    private static final int PARTICLE_COUNT = 12;
    private static final long ANIMATION_DURATION = 2000;

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final Path starPath = new Path();

    private ValueAnimator animator;
    private int sparkleColor;
    private boolean isAnimating = false;

    public SparkleView(Context context) {
        super(context);
        init();
    }

    public SparkleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SparkleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        sparkleColor = MaterialColors.getColor(this, 
            android.R.attr.colorPrimary, 0xFFFFD700);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setSparkleColor(int color) {
        this.sparkleColor = color;
    }

    public void startAnimation() {
        if (isAnimating) return;
        isAnimating = true;

        // Initialize particles
        initParticles();

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            updateParticles();
            invalidate();
        });
        animator.start();
    }

    public void stopAnimation() {
        isAnimating = false;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        particles.clear();
        invalidate();
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(createParticle());
        }
    }

    private Particle createParticle() {
        Particle p = new Particle();
        resetParticle(p);
        // Randomize initial progress so particles don't all start at once
        p.progress = random.nextFloat();
        return p;
    }

    private void resetParticle(Particle p) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            width = 200;
            height = 88;
        }

        // Calculate button bounds (centered within the sparkle view)
        int buttonPadding = 16; // dp converted roughly
        float density = getContext().getResources().getDisplayMetrics().density;
        int padding = (int) (buttonPadding * density);
        
        int buttonTop = padding;
        int buttonBottom = height - padding;
        int buttonLeft = 0;
        int buttonRight = width;

        // Position particles around the button edges
        int edge = random.nextInt(4);
        switch (edge) {
            case 0: // Top
                p.x = buttonLeft + random.nextFloat() * (buttonRight - buttonLeft);
                p.y = buttonTop - 5 + random.nextFloat() * 10;
                break;
            case 1: // Right
                p.x = buttonRight - 10 + random.nextFloat() * 20;
                p.y = buttonTop + random.nextFloat() * (buttonBottom - buttonTop);
                break;
            case 2: // Bottom
                p.x = buttonLeft + random.nextFloat() * (buttonRight - buttonLeft);
                p.y = buttonBottom - 5 + random.nextFloat() * 10;
                break;
            case 3: // Left
                p.x = buttonLeft - 10 + random.nextFloat() * 20;
                p.y = buttonTop + random.nextFloat() * (buttonBottom - buttonTop);
                break;
        }

        p.size = 4 + random.nextFloat() * 8;
        p.alpha = 0f;
        p.progress = 0f;
        p.speed = 0.01f + random.nextFloat() * 0.02f;
        p.rotation = random.nextFloat() * 360;
        p.rotationSpeed = (random.nextFloat() - 0.5f) * 10;
    }

    private void updateParticles() {
        for (Particle p : particles) {
            p.progress += p.speed;
            p.rotation += p.rotationSpeed;

            // Fade in and out
            if (p.progress < 0.3f) {
                p.alpha = p.progress / 0.3f;
            } else if (p.progress > 0.7f) {
                p.alpha = (1f - p.progress) / 0.3f;
            } else {
                p.alpha = 1f;
            }

            // Reset particle when animation completes
            if (p.progress >= 1f) {
                resetParticle(p);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Particle p : particles) {
            if (p.alpha > 0) {
                paint.setColor(sparkleColor);
                paint.setAlpha((int) (p.alpha * 255));

                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.rotation);
                drawStar(canvas, p.size);
                canvas.restore();
            }
        }
    }

    private void drawStar(Canvas canvas, float size) {
        starPath.reset();
        float innerRadius = size * 0.4f;
        float outerRadius = size;

        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4);
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            float x = (float) (Math.cos(angle) * radius);
            float y = (float) (Math.sin(angle) * radius);

            if (i == 0) {
                starPath.moveTo(x, y);
            } else {
                starPath.lineTo(x, y);
            }
        }
        starPath.close();
        canvas.drawPath(starPath, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    private static class Particle {
        float x, y;
        float size;
        float alpha;
        float progress;
        float speed;
        float rotation;
        float rotationSpeed;
    }
}
