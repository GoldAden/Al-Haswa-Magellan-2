package app.organicmaps.hawsa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Locale;

/**
 * Modern phone-style compass with dense tick marks, full 0-360 degree labels,
 * N/E/S/W Latin + Arabic cardinal labels, prominent fixed top indicator,
 * clear Qibla pointer, and very smooth sensor smoothing.
 */
public class CompassView extends View implements SensorEventListener {

    private double qiblaBearing = 0;

    // Theme colors — match app dark navy background
    private static final int COLOR_BG = 0xFF0D1B2A;
    private static final int COLOR_DIAL_FACE = 0xFFF5F5F0;
    private static final int COLOR_DIAL_RING = 0xFFD0D0D0;
    private static final int COLOR_DIAL_BORDER = 0xFFB0B0B0;
    private static final int COLOR_TICK_2DEG = 0xFFCCCCCC;
    private static final int COLOR_TICK_10DEG = 0xFF888888;
    private static final int COLOR_TICK_30DEG = 0xFF424242;
    private static final int COLOR_DEGREE_TEXT = 0xFF757575;
    private static final int COLOR_DEGREE_TEXT_MAJOR = 0xFF424242;
    private static final int COLOR_NORTH = 0xFFD32F2F;
    private static final int COLOR_EAST = 0xFF1976D2;
    private static final int COLOR_SOUTH = 0xFFF57C00;
    private static final int COLOR_WEST = 0xFF388E3C;
    private static final int COLOR_CARDINAL_LATIN = 0xFFFFFFFF;
    private static final int COLOR_CARDINAL_AR = 0xFFFFFFFF;
    private static final int COLOR_QIBLA_ARROW = 0xFFD4AF37;
    private static final int COLOR_QIBLA_GLOW = 0x40D4AF37;
    private static final int COLOR_QIBLA_LABEL = 0xCCD4AF37;
    private static final int COLOR_FIXED_NORTH = 0xFFD32F2F;
    private static final int COLOR_FIXED_NORTH_GLOW = 0x30D32F2F;
    private static final int COLOR_CENTER_DOT = 0xFFD32F2F;
    private static final int COLOR_HEADING_TEXT = 0xFF212121;
    private static final int COLOR_DIRECTION_LABEL = 0xFF757575;
    private static final int COLOR_CROSSHAIR = 0xFFBDBDBD;

    // Paints
    private Paint dialFacePaint;
    private Paint dialRingPaint;
    private Paint dialBorderPaint;
    private Paint tick2Paint;
    private Paint tick10Paint;
    private Paint tick30Paint;
    private Paint degreeTextPaint;
    private Paint degreeTextMajorPaint;
    private Paint cardinalBgN, cardinalBgE, cardinalBgS, cardinalBgW;
    private Paint cardinalLatinPaint;
    private Paint cardinalArN, cardinalArE, cardinalArS, cardinalArW;
    private Paint qiblaArrowPaint;
    private Paint qiblaGlowPaint;
    private Paint qiblaLabelPaint;
    private Paint fixedNorthPaint;
    private Paint northGlowPaint;
    private Paint headingPaint;
    private Paint directionPaint;
    private Paint crosshairPaint;
    private Paint centerDotPaint;
    private Paint centerRingPaint;
    private Paint kaabaPaint;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravityValues = null;
    private float[] geomagneticValues = null;
    private float currentAzimuth = 0f;
    private float targetAzimuth = 0f;
    private float smoothAzimuth = 0f;

    // Smoothing parameters
    private static final float EMA_FACTOR = 0.08f;  // Low-pass EMA — very smooth
    private static final float NOISE_THRESHOLD = 0.5f; // Ignore changes < 0.5°
    private static final long INVALIDATE_INTERVAL_MS = 33; // ~30fps max

    private long lastInvalidateTime = 0;
    // Ring buffer for averaging (5-sample window)
    private static final int SMOOTH_WINDOW = 5;
    private float[] smoothBuffer = new float[SMOOTH_WINDOW];
    private int smoothBufferIdx = 0;
    private int smoothBufferCount = 0;

    public CompassView(Context context) {
        super(context);
        init(context);
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Dial face
        dialFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialFacePaint.setStyle(Paint.Style.FILL);
        dialFacePaint.setColor(COLOR_DIAL_FACE);

        dialRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialRingPaint.setStyle(Paint.Style.STROKE);
        dialRingPaint.setStrokeWidth(2f);
        dialRingPaint.setColor(COLOR_DIAL_RING);

        dialBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialBorderPaint.setStyle(Paint.Style.STROKE);
        dialBorderPaint.setStrokeWidth(4f);
        dialBorderPaint.setColor(COLOR_DIAL_BORDER);

        // Dense tick paints — modern compass style
        tick2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tick2Paint.setStyle(Paint.Style.STROKE);
        tick2Paint.setStrokeWidth(0.8f);
        tick2Paint.setColor(COLOR_TICK_2DEG);

        tick10Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tick10Paint.setStyle(Paint.Style.STROKE);
        tick10Paint.setStrokeWidth(1.5f);
        tick10Paint.setColor(COLOR_TICK_10DEG);

        tick30Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tick30Paint.setStyle(Paint.Style.STROKE);
        tick30Paint.setStrokeWidth(2.5f);
        tick30Paint.setColor(COLOR_TICK_30DEG);

        // Degree text (every 10°)
        degreeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreeTextPaint.setTextSize(11f);
        degreeTextPaint.setColor(COLOR_DEGREE_TEXT);
        degreeTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        degreeTextMajorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreeTextMajorPaint.setTextSize(13f);
        degreeTextMajorPaint.setColor(COLOR_DEGREE_TEXT_MAJOR);
        degreeTextMajorPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        // Cardinal background circles
        cardinalBgN = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalBgN.setStyle(Paint.Style.FILL);
        cardinalBgN.setColor(COLOR_NORTH);

        cardinalBgE = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalBgE.setStyle(Paint.Style.FILL);
        cardinalBgE.setColor(COLOR_EAST);

        cardinalBgS = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalBgS.setStyle(Paint.Style.FILL);
        cardinalBgS.setColor(COLOR_SOUTH);

        cardinalBgW = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalBgW.setStyle(Paint.Style.FILL);
        cardinalBgW.setColor(COLOR_WEST);

        // Latin cardinal labels (N/E/S/W) — large and clear
        cardinalLatinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalLatinPaint.setTextSize(22f);
        cardinalLatinPaint.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        cardinalLatinPaint.setColor(COLOR_CARDINAL_LATIN);

        // Arabic cardinal labels
        cardinalArN = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalArN.setTextSize(11f);
        cardinalArN.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        cardinalArN.setColor(COLOR_CARDINAL_AR);

        cardinalArE = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalArE.setTextSize(11f);
        cardinalArE.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        cardinalArE.setColor(COLOR_CARDINAL_AR);

        cardinalArS = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalArS.setTextSize(11f);
        cardinalArS.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        cardinalArS.setColor(COLOR_CARDINAL_AR);

        cardinalArW = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalArW.setTextSize(11f);
        cardinalArW.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        cardinalArW.setColor(COLOR_CARDINAL_AR);

        // Qibla — larger and more visible
        qiblaArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaArrowPaint.setStyle(Paint.Style.FILL);
        qiblaArrowPaint.setColor(COLOR_QIBLA_ARROW);

        qiblaGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaGlowPaint.setStyle(Paint.Style.FILL);
        qiblaGlowPaint.setColor(COLOR_QIBLA_GLOW);

        qiblaLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaLabelPaint.setTextSize(12f);
        qiblaLabelPaint.setColor(COLOR_QIBLA_LABEL);
        qiblaLabelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        kaabaPaint.setStyle(Paint.Style.FILL);
        kaabaPaint.setColor(COLOR_QIBLA_ARROW);

        // Fixed red north triangle — larger and more prominent
        fixedNorthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fixedNorthPaint.setStyle(Paint.Style.FILL);
        fixedNorthPaint.setColor(COLOR_FIXED_NORTH);

        northGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northGlowPaint.setStyle(Paint.Style.FILL);
        northGlowPaint.setColor(COLOR_FIXED_NORTH_GLOW);

        // Heading in center
        headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingPaint.setTextSize(54f);
        headingPaint.setColor(COLOR_HEADING_TEXT);
        headingPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        headingPaint.setFakeBoldText(true);

        directionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionPaint.setTextSize(18f);
        directionPaint.setColor(COLOR_DIRECTION_LABEL);
        directionPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        // Crosshair
        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1f);
        crosshairPaint.setColor(COLOR_CROSSHAIR);

        // Center
        centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(COLOR_CENTER_DOT);

        centerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerRingPaint.setStyle(Paint.Style.STROKE);
        centerRingPaint.setStrokeWidth(2f);
        centerRingPaint.setColor(0xFFBDBDBD);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }

    public void setQiblaBearing(double bearing) {
        this.qiblaBearing = bearing;
        invalidate();
    }

    public double getQiblaBearing() {
        return this.qiblaBearing;
    }

    public float getCurrentAzimuth() {
        return currentAzimuth;
    }

    public void startSensors() {
        if (sensorManager != null) {
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            if (magnetometer != null)
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stopSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravityValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagneticValues = event.values.clone();
        }

        if (gravityValues != null && geomagneticValues != null) {
            float[] rotationMatrix = new float[9];
            float[] orientationValues = new float[3];

            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues);
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                float azimuth = (float) Math.toDegrees(orientationValues[0]);
                if (azimuth < 0) azimuth += 360;

                // Noise gate: ignore changes smaller than threshold
                float diff = Math.abs(azimuth - targetAzimuth);
                if (diff > 180) diff = 360 - diff;
                if (diff < NOISE_THRESHOLD && smoothBufferCount >= SMOOTH_WINDOW) {
                    return; // Ignore sensor noise
                }

                targetAzimuth = azimuth;

                // EMA (Exponential Moving Average) for smooth interpolation
                float emaDiff = targetAzimuth - smoothAzimuth;
                if (emaDiff > 180) emaDiff -= 360;
                if (emaDiff < -180) emaDiff += 360;
                smoothAzimuth += emaDiff * EMA_FACTOR;
                if (smoothAzimuth < 0) smoothAzimuth += 360;
                if (smoothAzimuth >= 360) smoothAzimuth -= 360;

                // Additional ring-buffer averaging for extra smoothness
                smoothBuffer[smoothBufferIdx] = smoothAzimuth;
                smoothBufferIdx = (smoothBufferIdx + 1) % SMOOTH_WINDOW;
                smoothBufferCount = Math.min(smoothBufferCount + 1, SMOOTH_WINDOW);

                float sum = 0;
                float sumSin = 0, sumCos = 0;
                for (int i = 0; i < smoothBufferCount; i++) {
                    double rad = Math.toRadians(smoothBuffer[i]);
                    sumSin += Math.sin(rad);
                    sumCos += Math.cos(rad);
                }
                currentAzimuth = (float) Math.toDegrees(Math.atan2(sumSin / smoothBufferCount, sumCos / smoothBufferCount));
                if (currentAzimuth < 0) currentAzimuth += 360;

                // Throttle invalidate to ~30fps
                long now = System.currentTimeMillis();
                if (now - lastInvalidateTime > INVALIDATE_INTERVAL_MS) {
                    lastInvalidateTime = now;
                    invalidate();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private String getDirectionLabelArabic(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "\u0634";
        if (azimuth >= 22.5 && azimuth < 67.5) return "\u0634\u0634";
        if (azimuth >= 67.5 && azimuth < 112.5) return "\u0634\u0631";
        if (azimuth >= 112.5 && azimuth < 157.5) return "\u062C\u0634";
        if (azimuth >= 157.5 && azimuth < 202.5) return "\u062C";
        if (azimuth >= 202.5 && azimuth < 247.5) return "\u062C\u063A";
        if (azimuth >= 247.5 && azimuth < 292.5) return "\u063A";
        if (azimuth >= 292.5 && azimuth < 337.5) return "\u0634\u063A";
        return "\u0634";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float outerRadius = Math.min(cx, cy) - 28f; // space for prominent fixed indicator
        float dialRadius = outerRadius - 4f;

        // Dark background matching app theme
        canvas.drawColor(COLOR_BG);

        // === Light/white dial face ===
        canvas.drawCircle(cx, cy, dialRadius, dialFacePaint);
        // Thin ring
        canvas.drawCircle(cx, cy, dialRadius, dialRingPaint);

        // === Rotating dial elements ===
        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        float tickOuter = dialRadius - 2f;
        float tick2Inner = dialRadius - 8f;    // 2° ticks — very short
        float tick10Inner = dialRadius - 14f;   // 10° ticks — medium
        float tick30Inner = dialRadius - 20f;   // 30° ticks — long
        float cardinalCenter = dialRadius - 38f;  // center of cardinal circles
        float degreeTextY = dialRadius - 16f;     // degree number baseline
        float degreeTextMajorY = dialRadius - 20f; // major degree number baseline

        // === Dense tick marks: every 2° ===
        for (int deg = 0; deg < 360; deg += 2) {
            boolean is30 = (deg % 30 == 0);
            boolean is10 = (deg % 10 == 0) && !is30;
            float inner;
            Paint p;
            if (is30) {
                inner = tick30Inner;
                p = tick30Paint;
            } else if (is10) {
                inner = tick10Inner;
                p = tick10Paint;
            } else {
                inner = tick2Inner;
                p = tick2Paint;
            }

            canvas.save();
            canvas.rotate(deg, cx, cy);
            canvas.drawLine(cx, cy - inner, cx, cy - tickOuter, p);
            canvas.restore();
        }

        // === Degree labels every 10° (skip cardinals at 0/90/180/270) ===
        for (int deg = 0; deg < 360; deg += 10) {
            if (deg % 90 == 0) continue; // cardinals go here

            canvas.save();
            canvas.rotate(deg, cx, cy);

            boolean is30 = (deg % 30 == 0);
            Paint tp = is30 ? degreeTextMajorPaint : degreeTextPaint;
            String label = String.valueOf(deg);

            // Rotate text back upright for readability
            canvas.save();
            canvas.rotate(-deg, cx, cy - (is30 ? degreeTextMajorY : degreeTextY));
            float tw = tp.measureText(label);
            canvas.drawText(label, cx - tw / 2, cy - (is30 ? degreeTextMajorY : degreeTextY) + tp.descent(), tp);
            canvas.restore();

            canvas.restore();
        }

        // === Cardinal direction labels: N/E/S/W + Arabic ===
        String[] latinLabels = {"N", "E", "S", "W"};
        String[] arabicLabels = {
            "\u0634\u0645\u0627\u0644",  // شمال
            "\u0634\u0631\u0642",       // شرق
            "\u062C\u0646\u0648\u0628",  // جنوب
            "\u063A\u0631\u0628"        // غرب
        };
        Paint[] bgPaints = {cardinalBgN, cardinalBgE, cardinalBgS, cardinalBgW};
        float circleRadius = 24f;

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(i * 90, cx, cy);

            // Colored background circle
            canvas.drawCircle(cx, cy - cardinalCenter, circleRadius, bgPaints[i]);

            // Latin letter (large, white, bold) — rotated back upright for readability
            canvas.save();
            canvas.rotate(-(i * 90 - currentAzimuth), cx, cy - cardinalCenter - 3f);
            float lw = cardinalLatinPaint.measureText(latinLabels[i]);
            canvas.drawText(latinLabels[i], cx - lw / 2, cy - cardinalCenter + cardinalLatinPaint.descent() - 3f, cardinalLatinPaint);
            canvas.restore();

            // Arabic label (small, below latin) — rotated back upright
            canvas.save();
            canvas.rotate(-(i * 90 - currentAzimuth), cx, cy - cardinalCenter + 13f);
            Paint arPaint = (i == 0) ? cardinalArN : (i == 1) ? cardinalArE : (i == 2) ? cardinalArS : cardinalArW;
            float aw = arPaint.measureText(arabicLabels[i]);
            canvas.drawText(arabicLabels[i], cx - aw / 2, cy - cardinalCenter + 13f + arPaint.descent(), arPaint);
            canvas.restore();

            canvas.restore();
        }

        // === Qibla indicator — larger and more visible ===
        if (qiblaBearing > 0) {
            canvas.save();
            canvas.rotate((float) qiblaBearing, cx, cy);

            // Glow circle
            float glowR = 20f;
            canvas.drawCircle(cx, cy - dialRadius + 6f, glowR, qiblaGlowPaint);

            // Larger Qibla arrow pointing outward
            Path qPath = new Path();
            float qBase = dialRadius - 12f;
            float qTip = dialRadius + 6f;
            qPath.moveTo(cx - 12f, cy - qBase);
            qPath.lineTo(cx, cy - qTip);
            qPath.lineTo(cx + 12f, cy - qBase);
            qPath.close();
            canvas.drawPath(qPath, qiblaArrowPaint);

            // Kaaba cube icon at tip
            float ks = 5f;
            RectF kaabaRect = new RectF(cx - ks, cy - qTip - ks - 2f, cx + ks, cy - qTip + ks - 2f);
            canvas.drawRect(kaabaRect, kaabaPaint);
            // Kaaba stripe
            Paint kaabaStripe = new Paint(Paint.ANTI_ALIAS_FLAG);
            kaabaStripe.setStyle(Paint.Style.FILL);
            kaabaStripe.setColor(0xFFB8860B);
            canvas.drawRect(new RectF(cx - ks, cy - qTip - 1f, cx + ks, cy - qTip + 1f), kaabaStripe);

            // "القبلة" label — rotated back upright for readability
            canvas.save();
            canvas.rotate(-(float) qiblaBearing, cx, cy - qBase + 18f);
            String qLabel = "\u0627\u0644\u0642\u0628\u0644\u0629";
            float qlw = qiblaLabelPaint.measureText(qLabel);
            canvas.drawText(qLabel, cx - qlw / 2, cy - qBase + 24f, qiblaLabelPaint);
            canvas.restore();

            canvas.restore();
        }

        canvas.restore(); // End dial rotation

        // === Fixed elements ===

        // Prominent fixed red north indicator at top — larger triangle
        float nBaseY = cy - outerRadius - 2f;
        float nTipY = nBaseY - 4f;
        float nTopY = nBaseY - 18f;
        Path nPath = new Path();
        nPath.moveTo(cx - 14f, nBaseY + 4f);
        nPath.lineTo(cx, nTopY);
        nPath.lineTo(cx + 14f, nBaseY + 4f);
        nPath.close();
        // Glow
        Path nGlowPath = new Path();
        nGlowPath.moveTo(cx - 18f, nBaseY + 8f);
        nGlowPath.lineTo(cx, nTopY - 4f);
        nGlowPath.lineTo(cx + 18f, nBaseY + 8f);
        nGlowPath.close();
        canvas.drawPath(nGlowPath, northGlowPaint);
        canvas.drawPath(nPath, fixedNorthPaint);

        // Thin fixed line at top (modern compass reference line)
        Paint fixedLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fixedLinePaint.setStyle(Paint.Style.STROKE);
        fixedLinePaint.setStrokeWidth(3f);
        fixedLinePaint.setColor(COLOR_FIXED_NORTH);
        canvas.drawLine(cx, cy - outerRadius, cx, nBaseY + 4f, fixedLinePaint);

        // Crosshair lines in center
        float crossLen = 28f;
        crosshairPaint.setAlpha(80);
        canvas.drawLine(cx - crossLen, cy, cx + crossLen, cy, crosshairPaint);
        canvas.drawLine(cx, cy - crossLen, cx, cy + crossLen, crosshairPaint);

        // Center circle + dot
        canvas.drawCircle(cx, cy, 8f, centerRingPaint);
        canvas.drawCircle(cx, cy, 4f, centerDotPaint);

        // Heading readout in center
        String headingStr = String.format(Locale.US, "%.0f\u00B0", currentAzimuth);
        float headingW = headingPaint.measureText(headingStr);
        canvas.drawText(headingStr, cx - headingW / 2, cy + 22f, headingPaint);

        // Direction abbreviation below heading
        String dirStr = getDirectionLabelArabic(currentAzimuth);
        float dirW = directionPaint.measureText(dirStr);
        canvas.drawText(dirStr, cx - dirW / 2, cy + 42f, directionPaint);
    }
}
