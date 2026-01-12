/*
 * KGPT - AI in your keyboard
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated view with sparkles and flowing lines for decorative backgrounds.
 * Enhanced for smoother, slower, and more premium animation.
 */
public class AnimatedSparklesView extends View {

    private final List<Sparkle> sparkles = new ArrayList<>();
    private final List<FlowingLine> flowingLines = new ArrayList<>();
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;

    // Configuration for speed and aesthetics
    private static final float SPEED_MULTIPLIER = 0.3f; // Global speed control (lower is slower)

    public AnimatedSparklesView(Context context) {
        super(context);
        init();
    }

    public AnimatedSparklesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedSparklesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        sparklePaint.setStyle(Paint.Style.FILL);
        sparklePaint.setColor(0xFFFFFFFF);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(0x80FFFFFF); // Semi-transparent lines
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            initParticles(w, h);
            startAnimation();
        }
    }

    private void initParticles(int width, int height) {
        sparkles.clear();
        flowingLines.clear();

        // Create sparkles spread across the card
        // Increased count slightly for better visuals but they fade in/out
        for (int i = 0; i < 20; i++) {
            float x = random.nextFloat() * width;
            float y = random.nextFloat() * height;
            float targetSize = 4f + random.nextFloat() * 6f; // Larger, more visible
            float speed = 0.2f + random.nextFloat() * 0.4f;
            float phase = random.nextFloat() * (float) Math.PI * 2;
            sparkles.add(new Sparkle(x, y, targetSize, speed, phase));
        }

        // Create flowing curved lines
        for (int i = 0; i < 4; i++) {
            float startY = random.nextFloat() * height;
            float length = 50f + random.nextFloat() * 100f;
            float speed = 0.4f + random.nextFloat() * 0.4f;
            flowingLines.add(new FlowingLine(startY, length, speed));
        }
    }

    private void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(10000); // Very long duration for continuous loop feeling
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            updateParticles();
            invalidate();
        });
        animator.start();
    }

    private void updateParticles() {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0)
            return;

        // Update sparkles
        for (Sparkle sparkle : sparkles) {
            // Slower pulsing opacity
            sparkle.phase += sparkle.speed * 0.05f * SPEED_MULTIPLIER;
            sparkle.alpha = (float) (0.3f + 0.7f * Math.pow(Math.sin(sparkle.phase), 2));

            // Gentle floating motion (Multi-axis)
            sparkle.x -= sparkle.speed * 1.5f * SPEED_MULTIPLIER;
            sparkle.y += Math.sin(sparkle.phase * 0.5f) * 0.5f * SPEED_MULTIPLIER;

            // Rotation for diamond effect
            sparkle.rotation += sparkle.speed * 0.5f * SPEED_MULTIPLIER;

            // Reset when off screen or fully faded out for a long time
            if (sparkle.x < -20) {
                sparkle.x = width + 20;
                sparkle.y = random.nextFloat() * height;
            }
        }

        // Update flowing lines
        for (FlowingLine line : flowingLines) {
            line.progress += line.speed * 0.005f * SPEED_MULTIPLIER;

            // Fade in and out based on progress
            if (line.progress < 0.2f) {
                line.alpha = line.progress / 0.2f;
            } else if (line.progress > 0.8f) {
                line.alpha = (1f - line.progress) / 0.2f;
            } else {
                line.alpha = 1f;
            }

            if (line.progress > 1f) {
                line.progress = 0f;
                line.startX = width * 0.8f + random.nextFloat() * width * 0.2f;
                line.startY = random.nextFloat() * height;
                line.angle = 160f + random.nextFloat() * 40f; // Flows left-ish
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw flowing lines first
        for (FlowingLine line : flowingLines) {
            drawFlowingLine(canvas, line);
        }

        // Draw sparkles
        for (Sparkle sparkle : sparkles) {
            drawSparkle(canvas, sparkle);
        }
    }

    private void drawSparkle(Canvas canvas, Sparkle sparkle) {
        if (sparkle.alpha < 0.01f)
            return;

        int alpha = (int) (sparkle.alpha * 255);
        sparklePaint.setAlpha(alpha);

        float size = sparkle.size * sparkle.alpha; // Scale with alpha for "pop" effect
        float x = sparkle.x;
        float y = sparkle.y;

        canvas.save();
        canvas.rotate(sparkle.rotation, x, y);

        // Draw Diamond Shape (Rhombus)
        Path path = new Path();
        path.moveTo(x, y - size); // Top
        path.lineTo(x + size * 0.6f, y); // Right
        path.lineTo(x, y + size); // Bottom
        path.lineTo(x - size * 0.6f, y); // Left
        path.close();

        canvas.drawPath(path, sparklePaint);

        // Draw centered glow
        sparklePaint.setAlpha(alpha / 4);
        canvas.drawCircle(x, y, size * 1.5f, sparklePaint);

        canvas.restore();
    }

    private void drawFlowingLine(Canvas canvas, FlowingLine line) {
        if (line.alpha < 0.05f)
            return;

        int alpha = (int) (line.alpha * 60); // Very subtle lines
        linePaint.setAlpha(alpha);
        linePaint.setStrokeWidth(2f);

        Path path = new Path();
        float startX = line.startX;
        float startY = line.startY;

        // Calculate end point based on angle and progress
        float currentLength = line.length * line.progress * 2f;

        // Simple straight-ish line with curve
        path.moveTo(startX, startY);
        path.cubicTo(
                startX - currentLength * 0.3f, startY + 10,
                startX - currentLength * 0.6f, startY - 10,
                startX - currentLength, startY);

        canvas.drawPath(path, linePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getWidth() > 0 && getHeight() > 0) {
            startAnimation();
        }
    }

    private static class Sparkle {
        float x, y, size, speed, phase, alpha, rotation;

        Sparkle(float x, float y, float size, float speed, float phase) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.phase = phase; // Used for alpha cycle
            this.alpha = 0f;
            this.rotation = (float) (Math.random() * 360);
        }
    }

    private static class FlowingLine {
        float startX, startY, length, speed, angle, progress, alpha;

        FlowingLine(float startY, float length, float speed) {
            this.startY = startY;
            this.length = length;
            this.speed = speed;
            this.progress = random(-0.5f, 0.5f); // Start at random progress
            this.startX = 0; // Set dynamically
        }

        private float random(float min, float max) {
            return min + (float) Math.random() * (max - min);
        }
    }
}
