/*
 * Copyright (C) 2024-2025 Amr Aldeeb @Eluea
 * 
 * This file is part of KGPT - a fork of KeyboardGPT.
 * 
 * Custom Color Wheel View for Hue selection.
 * Shows color preview in center, selector on ring.
 */
package tn.eluea.kgpt.ui.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    private Paint wheelPaint;
    private Paint selectorPaint;
    private Paint centerPaint;
    private Paint centerBorderPaint;

    private float centerX, centerY;
    private float outerRadius, innerRadius;
    private float centerCircleRadius;

    private float currentHue = 0f;
    private float currentSat = 1f;
    private float currentVal = 1f;

    private int currentColor = Color.RED;

    private OnColorChangeListener listener;

    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wheelPaint.setStyle(Paint.Style.STROKE);

        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(4f);
        selectorPaint.setColor(Color.WHITE);
        selectorPaint.setShadowLayer(4f, 0, 2f, Color.argb(80, 0, 0, 0));
        setLayerType(LAYER_TYPE_SOFTWARE, selectorPaint);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);

        centerBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerBorderPaint.setStyle(Paint.Style.STROKE);
        centerBorderPaint.setStrokeWidth(4f);
        centerBorderPaint.setColor(Color.WHITE);
        centerBorderPaint.setShadowLayer(6f, 0, 3f, Color.argb(60, 0, 0, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        outerRadius = Math.min(w, h) / 2f - 24f;
        innerRadius = outerRadius * 0.60f; // Thicker ring
        centerCircleRadius = innerRadius * 0.70f;

        float strokeWidth = outerRadius - innerRadius;
        wheelPaint.setStrokeWidth(strokeWidth);

        createWheelShader();
    }

    private void createWheelShader() {
        if (outerRadius <= 0)
            return;

        // Create hue colors array for SweepGradient
        int[] hueColors = new int[361];
        for (int i = 0; i <= 360; i++) {
            hueColors[i] = Color.HSVToColor(new float[] { i, 1f, 1f });
        }

        // SweepGradient for the hue wheel - starts at 3 o'clock by default
        SweepGradient hueGradient = new SweepGradient(centerX, centerY, hueColors, null);

        // Rotate gradient to start at top (12 o'clock) - rotate by -90 degrees
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setRotate(-90, centerX, centerY);
        hueGradient.setLocalMatrix(matrix);

        wheelPaint.setShader(hueGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the hue ring
        float ringRadius = (outerRadius + innerRadius) / 2f;
        canvas.drawCircle(centerX, centerY, ringRadius, wheelPaint);

        // Draw center color preview circle
        centerPaint.setColor(currentColor);
        canvas.drawCircle(centerX, centerY, centerCircleRadius, centerPaint);
        canvas.drawCircle(centerX, centerY, centerCircleRadius, centerBorderPaint);

        // Draw selector on the ring at current hue position
        float angle = (float) Math.toRadians(currentHue - 90);
        float selectorX = centerX + ringRadius * (float) Math.cos(angle);
        float selectorY = centerY + ringRadius * (float) Math.sin(angle);

        // Draw selector - filled with hue color, white border
        Paint selectorFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorFillPaint.setStyle(Paint.Style.FILL);
        selectorFillPaint.setColor(Color.HSVToColor(new float[] { currentHue, 1f, 1f }));
        canvas.drawCircle(selectorX, selectorY, 16f, selectorFillPaint);
        canvas.drawCircle(selectorX, selectorY, 16f, selectorPaint);
    }

    private int activeControl = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float dx = x - centerX;
        float dy = y - centerY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeControl = 1;
                // Fall through
            case MotionEvent.ACTION_MOVE:
                if (activeControl == 1) {
                    // Calculate hue from angle
                    float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90;
                    if (angle < 0)
                        angle += 360;
                    if (angle >= 360)
                        angle -= 360;
                    currentHue = angle;

                    updateCurrentColor();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeControl = 0;
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateCurrentColor() {
        currentColor = Color.HSVToColor(new float[] { currentHue, currentSat, currentVal });
        if (listener != null) {
            listener.onColorChanged(currentColor);
        }
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    public int getColor() {
        return currentColor;
    }

    public float getHue() {
        return currentHue;
    }

    public void setSaturation(float sat) {
        this.currentSat = Math.max(0, Math.min(1, sat));
        updateCurrentColor();
        invalidate();
    }

    public void setValue(float val) {
        this.currentVal = Math.max(0, Math.min(1, val));
        updateCurrentColor();
        invalidate();
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        currentHue = hsv[0];
        currentSat = hsv[1];
        currentVal = hsv[2];
        currentColor = color;
        invalidate();
    }
}
