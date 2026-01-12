package tn.eluea.kgpt.ui.settings.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HueSliderView extends View {

    private Paint paint;
    private Paint selectorPaint;
    private float currentHue = 0f;
    private OnHueChangeListener listener;

    public interface OnHueChangeListener {
        void onHueChanged(float hue);
    }

    public HueSliderView(Context context) {
        super(context);
        init();
    }

    public HueSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HueSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(6f);
        selectorPaint.setColor(Color.WHITE);
        selectorPaint.setShadowLayer(4f, 0, 2f, Color.argb(80, 0, 0, 0));
        setLayerType(LAYER_TYPE_SOFTWARE, selectorPaint);
    }

    public void setHue(float hue) {
        this.currentHue = hue;
        invalidate();
    }

    public void setOnHueChangeListener(OnHueChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Rainbow Gradient
        int[] thumbColors = new int[361];
        for (int i = 0; i <= 360; i++) {
            thumbColors[i] = Color.HSVToColor(new float[] { i, 1f, 1f });
        }

        // Optimizing the gradient: standard rainbow colors
        int[] colors = new int[] {
                0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
        };

        Shader shader = new LinearGradient(0, 0, width, 0, colors, null, Shader.TileMode.CLAMP);
        paint.setShader(shader);

        // Round corners for track
        canvas.drawRoundRect(0, 0, width, height, height / 2f, height / 2f, paint);

        // Selector
        float radius = height * 0.35f; // Smaller radius
        float x = (currentHue / 360f) * width;
        x = Math.max(height / 2f, Math.min(x, width - height / 2f)); // Clamp center to rounded ends area

        // Draw selector circle
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);

        // Use a clearer selector style
        canvas.drawCircle(x, height / 2f, radius, fillPaint);
        canvas.drawCircle(x, height / 2f, radius, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = Math.max(0, Math.min(event.getX(), getWidth()));
                currentHue = (x / getWidth()) * 360f;

                if (listener != null) {
                    listener.onHueChanged(currentHue);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
