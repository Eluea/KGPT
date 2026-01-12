package tn.eluea.kgpt.ui.settings.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SaturationValueView extends View {

    private Paint paint;
    private Paint borderPaint;
    private Paint selectorPaint;
    private Paint selectorStrokePaint;

    private float hue = 0f;
    private float saturation = 0f;
    private float value = 1f;

    private OnColorChangeListener listener;

    public interface OnColorChangeListener {
        void onColorChanged(float saturation, float value);
    }

    public SaturationValueView(Context context) {
        super(context);
        init();
    }

    public SaturationValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SaturationValueView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(Color.LTGRAY);

        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(4f);
        selectorPaint.setColor(Color.WHITE);
        selectorPaint.setShadowLayer(4f, 0, 2f, Color.argb(80, 0, 0, 0));

        selectorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorStrokePaint.setStyle(Paint.Style.STROKE);
        selectorStrokePaint.setStrokeWidth(4f);
        selectorStrokePaint.setColor(Color.WHITE);

        setLayerType(LAYER_TYPE_SOFTWARE, selectorPaint);
    }

    public void setHue(float hue) {
        this.hue = hue;
        invalidate();
    }

    public void setSaturationValue(float sat, float val) {
        this.saturation = sat;
        this.value = val;
        invalidate();
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 1. Draw Saturation Gradient (Left: White -> Right: HueColor)
        Shader satShader = new LinearGradient(0, 0, width, 0,
                Color.WHITE, Color.HSVToColor(new float[] { hue, 1f, 1f }),
                Shader.TileMode.CLAMP);

        // 2. Draw Value Gradient (Top: Transparent -> Bottom: Black)
        Shader valShader = new LinearGradient(0, 0, 0, height,
                Color.TRANSPARENT, Color.BLACK,
                Shader.TileMode.CLAMP);

        // Combine
        ComposeShader shader = new ComposeShader(satShader, valShader, PorterDuff.Mode.SRC_OVER); // Actually we need
                                                                                                  // both applied.
        // A better way for SV Box:
        // Layer 1: Saturation (White to Hue)
        // Layer 2: Value (Transparent to Black) over it.

        paint.setShader(satShader);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setShader(valShader);
        canvas.drawRect(0, 0, width, height, paint);

        // Draw Border
        // Draw Border
        // canvas.drawRect(0, 0, width, height, borderPaint);

        // Draw Selector
        float x = saturation * width;
        float y = (1f - value) * height;

        // Ensure selector stays within bounds
        x = Math.max(0, Math.min(x, width));
        y = Math.max(0, Math.min(y, height));

        canvas.drawCircle(x, y, 16f, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = Math.max(0, Math.min(event.getX(), getWidth()));
                float y = Math.max(0, Math.min(event.getY(), getHeight()));

                saturation = x / getWidth();
                value = 1f - (y / getHeight());

                if (listener != null) {
                    listener.onColorChanged(saturation, value);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
