package app.organicmaps.hawsa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Custom view that draws the moon phase visually
 * Shows illuminated and shadow portions like a real moon
 */
public class MoonPhaseView extends View {

    private static final int COLOR_MOON_LIT = 0xFFF5F5DC;     // Warm moonlight
    private static final int COLOR_MOON_DARK = 0xFF2C2C3A;    // Dark shadow
    private static final int COLOR_MOON_EDGE = 0xFFAAAAAA;    // Edge highlight
    private static final int COLOR_BG = 0xFF1B263B;          // Card bg

    private Paint litPaint;
    private Paint darkPaint;
    private Paint edgePaint;
    private Paint bgPaint;
    private Paint starPaint;

    private double illumination = 0.5;
    private boolean isWaxing = true;

    public MoonPhaseView(Context context) {
        super(context);
        init();
    }

    public MoonPhaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoonPhaseView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        litPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        litPaint.setStyle(Paint.Style.FILL);
        litPaint.setColor(COLOR_MOON_LIT);

        darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkPaint.setStyle(Paint.Style.FILL);
        darkPaint.setColor(COLOR_MOON_DARK);

        edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(2f);
        edgePaint.setColor(COLOR_MOON_EDGE);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(COLOR_BG);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(0x60778DA9);
    }

    public void setPhase(double illumination, double age) {
        this.illumination = illumination;
        this.isWaxing = age < 29.53058867 / 2.0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - 20f;

        // Draw background
        canvas.drawColor(COLOR_BG);

        // Draw some stars
        long seed = 42;
        for (int i = 0; i < 30; i++) {
            seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
            float sx = (seed % w);
            seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
            float sy = (seed % h);
            float sr = 1f + (seed % 3);
            canvas.drawCircle(sx, sy, sr, starPaint);
        }

        // Draw moon - dark base circle first
        canvas.drawCircle(cx, cy, radius, darkPaint);

        // Draw illuminated portion using terminator
        // The terminator divides the moon into lit and dark halves
        // For a given illumination, we draw the lit part

        float terminatorX = (float) (radius * 2 * illumination - radius);
        if (!isWaxing) {
            terminatorX = -terminatorX;
        }

        // Draw lit portion
        Path litPath = new Path();
        RectF moonRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        if (illumination >= 0.99) {
            // Full moon - draw full circle
            canvas.drawCircle(cx, cy, radius, litPaint);
        } else if (illumination <= 0.01) {
            // New moon - just the dark circle
            canvas.drawCircle(cx, cy, radius, darkPaint);
        } else {
            // Draw lit portion with terminator
            // Right half of moon (for waxing) or left half (for waning)
            if (isWaxing) {
                // Waxing: lit from right
                // Right semicircle
                litPath.arcTo(moonRect, -90, 180);
                // Terminator curve
                RectF termRect = new RectF(
                    cx + terminatorX - radius, cy - radius,
                    cx + terminatorX + radius, cy + radius
                );
                if (illumination < 0.5) {
                    litPath.arcTo(termRect, 90, -180);
                } else {
                    litPath.arcTo(termRect, 90, 180);
                }
                litPath.close();
            } else {
                // Waning: lit from left
                litPath.arcTo(moonRect, 90, 180);
                RectF termRect = new RectF(
                    cx - terminatorX - radius, cy - radius,
                    cx - terminatorX + radius, cy + radius
                );
                if (illumination < 0.5) {
                    litPath.arcTo(termRect, 270, 180);
                } else {
                    litPath.arcTo(termRect, 270, -180);
                }
                litPath.close();
            }

            // Gradient for the lit portion
            LinearGradient moonGradient = new LinearGradient(
                cx - radius, cy, cx + radius, cy,
                0xFFDDDDCC, COLOR_MOON_LIT, Shader.TileMode.CLAMP
            );
            litPaint.setShader(moonGradient);
            canvas.drawPath(litPath, litPaint);
        }

        // Draw edge circle
        canvas.drawCircle(cx, cy, radius, edgePaint);

        // Glow effect for bright moon
        if (illumination > 0.5) {
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(3f);
            int glowAlpha = (int) (60 * illumination);
            glowPaint.setColor(Color.argb(glowAlpha, 245, 245, 220));
            canvas.drawCircle(cx, cy, radius + 6, glowPaint);
            glowPaint.setStrokeWidth(2f);
            glowPaint.setColor(Color.argb(glowAlpha / 2, 245, 245, 220));
            canvas.drawCircle(cx, cy, radius + 12, glowPaint);
        }
    }
}