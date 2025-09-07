package com.example.harvestrush;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Slow animated violet gradient background.
 */
public class AnimatedGradientBackgroundView extends View {
    private final Paint paint = new Paint();
    private LinearGradient gradient;
    private final Matrix matrix = new Matrix();
    private float shift = 0f;
    private long lastTime = 0L;
    private boolean running = true;

    // Animation config
    private static final float CYCLE_SECONDS = 25f; // full loop duration

    public AnimatedGradientBackgroundView(Context ctx) { super(ctx); }
    public AnimatedGradientBackgroundView(Context ctx, AttributeSet a) { super(ctx, a); }
    public AnimatedGradientBackgroundView(Context ctx, AttributeSet a, int s) { super(ctx, a, s); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        lastTime = System.nanoTime();
        post(frame);
    }
    @Override protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            if (lastTime > 0) {
                float dt = (now - lastTime) / 1_000_000_000f;
                shift += dt / CYCLE_SECONDS; // normalized 0..1
                if (shift > 1f) shift -= 1f;
            }
            lastTime = now;
            invalidate();
            postOnAnimation(this);
        }
    };

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildGradient();
    }

    private void buildGradient() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        // Base violet palette
        int[] colors = new int[]{
                0xFF3C0F60,
                0xFF55248A,
                0xFF6E3FB1,
                0xFF8A5ED8,
                0xFF6E3FB1,
                0xFF55248A
        };
        float[] positions = new float[]{0f, 0.20f, 0.45f, 0.70f, 0.85f, 1f};
        gradient = new LinearGradient(0,0,w,h, colors, positions, Shader.TileMode.MIRROR);
        paint.setShader(gradient);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gradient == null) buildGradient();
        if (gradient == null) return;
        // Apply translation & slight scale to create drifting effect around diagonal
        matrix.reset();
        float w = getWidth();
        float h = getHeight();
        float travel = w * 0.6f; // travel distance
        float dx = (shift * travel);
        float dy = (shift * travel * 0.7f);
        matrix.postTranslate(-travel/2f + dx, -travel/2f + dy);
        gradient.setLocalMatrix(matrix);
        canvas.drawRect(new Rect(0,0,(int)w,(int)h), paint);
        // Optional subtle overlay vignette can be added later
    }
}

