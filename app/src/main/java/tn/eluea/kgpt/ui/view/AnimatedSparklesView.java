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
 */
public class AnimatedSparklesView extends View {

    private final List<Sparkle> sparkles = new ArrayList<>();
    private final List<FlowingLine> flowingLines = new ArrayList<>();
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;

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
        linePaint.setColor(0xFFFFFFFF);
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

        // Create sparkles spread across the card (more on right side)
        for (int i = 0; i < 15; i++) {
            float x = width * 0.35f + random.nextFloat() * width * 0.6f;
            float y = random.nextFloat() * height;
            float size = 3f + random.nextFloat() * 5f;
            float speed = 0.5f + random.nextFloat() * 1f;
            float phase = random.nextFloat() * (float) Math.PI * 2;
            sparkles.add(new Sparkle(x, y, size, speed, phase));
        }

        // Create flowing curved lines
        for (int i = 0; i < 5; i++) {
            float startX = width * 0.4f + random.nextFloat() * width * 0.5f;
            float startY = random.nextFloat() * height;
            float length = 30f + random.nextFloat() * 50f;
            float speed = 0.8f + random.nextFloat() * 0.6f;
            float angle = -0.5f + random.nextFloat() * 1f; // Mostly horizontal
            flowingLines.add(new FlowingLine(startX, startY, length, speed, angle));
        }
    }

    private void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000);
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
        if (width == 0 || height == 0) return;

        // Update sparkles
        for (Sparkle sparkle : sparkles) {
            sparkle.phase += sparkle.speed * 0.08f;
            sparkle.alpha = (float) (0.2f + 0.8f * Math.abs(Math.sin(sparkle.phase)));
            
            // Gentle floating motion
            sparkle.y += Math.sin(sparkle.phase * 1.5f) * 0.5f;
            sparkle.x -= 0.3f; // Move left slowly

            // Reset when off screen
            if (sparkle.x < width * 0.3f) {
                sparkle.x = width + 10;
                sparkle.y = random.nextFloat() * height;
            }
        }

        // Update flowing lines
        for (FlowingLine line : flowingLines) {
            line.progress += line.speed * 0.015f;
            line.alpha = (float) Math.sin(line.progress * Math.PI);
            
            if (line.progress > 1f) {
                line.progress = 0f;
                line.startX = width * 0.5f + random.nextFloat() * width * 0.4f;
                line.startY = random.nextFloat() * height;
                line.angle = -0.3f + random.nextFloat() * 0.6f;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw flowing lines first (behind sparkles)
        for (FlowingLine line : flowingLines) {
            drawFlowingLine(canvas, line);
        }

        // Draw sparkles
        for (Sparkle sparkle : sparkles) {
            drawSparkle(canvas, sparkle);
        }
    }

    private void drawSparkle(Canvas canvas, Sparkle sparkle) {
        int alpha = (int) (sparkle.alpha * 200);
        sparklePaint.setAlpha(alpha);

        float size = sparkle.size;
        float x = sparkle.x;
        float y = sparkle.y;

        // Draw 4-point star sparkle
        Path path = new Path();
        
        // Vertical points
        path.moveTo(x, y - size);
        path.lineTo(x - size * 0.15f, y);
        path.lineTo(x, y + size);
        path.lineTo(x + size * 0.15f, y);
        path.close();
        
        // Horizontal points
        path.moveTo(x - size, y);
        path.lineTo(x, y - size * 0.15f);
        path.lineTo(x + size, y);
        path.lineTo(x, y + size * 0.15f);
        path.close();

        sparklePaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, sparklePaint);

        // Draw glow circle
        sparklePaint.setAlpha(alpha / 3);
        canvas.drawCircle(x, y, size * 0.8f, sparklePaint);
    }

    private void drawFlowingLine(Canvas canvas, FlowingLine line) {
        if (line.alpha < 0.05f) return;
        
        int alpha = (int) (line.alpha * 100);
        linePaint.setAlpha(alpha);
        linePaint.setStrokeWidth(2f + line.alpha * 2f);

        Path path = new Path();
        float startX = line.startX;
        float startY = line.startY;
        
        float length = line.length * line.progress;
        float endX = startX - length * (float) Math.cos(line.angle);
        float endY = startY + length * (float) Math.sin(line.angle);
        
        // Curved line with wave
        float midX = (startX + endX) / 2;
        float midY = (startY + endY) / 2 + (float) Math.sin(line.progress * Math.PI * 2) * 8;

        path.moveTo(startX, startY);
        path.quadTo(midX, midY, endX, endY);

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
        float x, y, size, speed, phase, alpha;

        Sparkle(float x, float y, float size, float speed, float phase) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.phase = phase;
            this.alpha = 1f;
        }
    }

    private static class FlowingLine {
        float startX, startY, length, speed, angle, progress, alpha;

        FlowingLine(float startX, float startY, float length, float speed, float angle) {
            this.startX = startX;
            this.startY = startY;
            this.length = length;
            this.speed = speed;
            this.angle = angle;
            this.progress = 0f;
            this.alpha = 0f;
        }
    }
}
