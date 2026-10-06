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

import java.util.Locale;

/**
 * Redmi-style compass with light/white dial face.
 * Features: clean thin tick marks, Arabic cardinal labels on colored circles,
 * fixed red north triangle at top, smooth dial rotation, heading in center.
 */
public class CompassView extends View implements SensorEventListener {

    private double qiblaBearing = 0;

    // Redmi light-dial theme
    private static final int COLOR_BG = 0xFF0A0E1A;             // outer background (dark navy)
    private static final int COLOR_DIAL_FACE = 0xFFF0F0F0;    // white/light dial face
    private static final int COLOR_DIAL_RING = 0xFFD0D0D0;    // thin ring around dial
    private static final int COLOR_DIAL_BORDER = 0xFFB0B0B0;  // outer edge of dial
    private static final int COLOR_TICK_THIN = 0xFF9E9E9E;    // 5° ticks - thin grey
    private static final int COLOR_TICK_MAJOR = 0xFF424242;   // 30° ticks - darker
    private static final int COLOR_TICK_MID = 0xFF757575;      // 10° ticks - medium
    private static final int COLOR_DEGREE_TEXT = 0xFF616161;   // degree number text
    private static final int COLOR_NORTH = 0xFFD32F2F;         // red for N
    private static final int COLOR_EAST = 0xFF1976D2;          // blue for E
    private static final int COLOR_SOUTH = 0xFFF57C00;         // amber for S
    private static final int COLOR_WEST = 0xFF388E3C;          // green for W
    private static final int COLOR_QIBLA_ARROW = 0xFFD4AF37;  // gold
    private static final int COLOR_QIBLA_GLOW = 0x30D4AF37;
    private static final int COLOR_QIBLA_LABEL = 0xCCD4AF37;
    private static final int COLOR_FIXED_NORTH = 0xFFD32F2F;   // fixed red triangle at top
    private static final int COLOR_FIXED_NORTH_GLOW = 0x20D32F2F;
    private static final int COLOR_CENTER_DOT = 0xFFD32F2F;
    private static final int COLOR_HEADING_TEXT = 0xFF212121;   // dark text on light face
    private static final int COLOR_DIRECTION_LABEL = 0xFF757575;
    private static final int COLOR_CROSSHAIR = 0xFFBDBDBD;

    // Paints
    private Paint dialFacePaint;
    private Paint dialRingPaint;
    private Paint dialBorderPaint;
    private Paint thinTickPaint;
    private Paint majorTickPaint;
    private Paint midTickPaint;
    private Paint degreeTextPaint;
    private Paint cardinalPaintN, cardinalPaintE, cardinalPaintS, cardinalPaintW;
    private Paint cardinalBgN, cardinalBgE, cardinalBgS, cardinalBgW;
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

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravityValues = null;
    private float[] geomagneticValues = null;
    private float currentAzimuth = 0f;
    private float targetAzimuth = 0f;
    private float smoothAzimuth = 0f;

    private long lastInvalidateTime = 0;

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
        // Dial face (white/light)
        dialFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialFacePaint.setStyle(Paint.Style.FILL);
        dialFacePaint.setColor(COLOR_DIAL_FACE);

        // Ring between face and edge
        dialRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialRingPaint.setStyle(Paint.Style.STROKE);
        dialRingPaint.setStrokeWidth(3f);
        dialRingPaint.setColor(COLOR_DIAL_RING);

        // Outer border
        dialBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialBorderPaint.setStyle(Paint.Style.STROKE);
        dialBorderPaint.setStrokeWidth(4f);
        dialBorderPaint.setColor(COLOR_DIAL_BORDER);

        // Tick paints - Redmi uses very thin, clean ticks
        thinTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinTickPaint.setStyle(Paint.Style.STROKE);
        thinTickPaint.setStrokeWidth(1f);
        thinTickPaint.setColor(COLOR_TICK_THIN);

        midTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        midTickPaint.setStyle(Paint.Style.STROKE);
        midTickPaint.setStrokeWidth(1.5f);
        midTickPaint.setColor(COLOR_TICK_MID);

        majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(2.5f);
        majorTickPaint.setColor(COLOR_TICK_MAJOR);

        // Degree text
        degreeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreeTextPaint.setTextSize(18f);
        degreeTextPaint.setColor(COLOR_DEGREE_TEXT);
        degreeTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        // Cardinal labels with Arabic text
        cardinalPaintN = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalPaintN.setTextSize(28f);
        cardinalPaintN.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        cardinalPaintN.setColor(Color.WHITE);

        cardinalPaintE = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalPaintE.setTextSize(28f);
        cardinalPaintE.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        cardinalPaintE.setColor(Color.WHITE);

        cardinalPaintS = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalPaintS.setTextSize(28f);
        cardinalPaintS.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        cardinalPaintS.setColor(Color.WHITE);

        cardinalPaintW = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalPaintW.setTextSize(28f);
        cardinalPaintW.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        cardinalPaintW.setColor(Color.WHITE);

        // Background circles for cardinal labels
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

        // Qibla
        qiblaArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaArrowPaint.setStyle(Paint.Style.FILL);
        qiblaArrowPaint.setColor(COLOR_QIBLA_ARROW);

        qiblaGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaGlowPaint.setStyle(Paint.Style.FILL);
        qiblaGlowPaint.setColor(COLOR_QIBLA_GLOW);

        qiblaLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaLabelPaint.setTextSize(13f);
        qiblaLabelPaint.setColor(COLOR_QIBLA_LABEL);
        qiblaLabelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        // Fixed red north triangle at top
        fixedNorthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fixedNorthPaint.setStyle(Paint.Style.FILL);
        fixedNorthPaint.setColor(COLOR_FIXED_NORTH);

        northGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northGlowPaint.setStyle(Paint.Style.FILL);
        northGlowPaint.setColor(COLOR_FIXED_NORTH_GLOW);

        // Heading in center
        headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingPaint.setTextSize(58f);
        headingPaint.setColor(COLOR_HEADING_TEXT);
        headingPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        headingPaint.setFakeBoldText(true);

        directionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionPaint.setTextSize(20f);
        directionPaint.setColor(COLOR_DIRECTION_LABEL);
        directionPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        // Crosshair
        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1f);
        crosshairPaint.setColor(COLOR_CROSSHAIR);

        // Center dot
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
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null)
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
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

                targetAzimuth = azimuth;

                // Smooth Redmi-style interpolation (0.15 dampening)
                float diff = targetAzimuth - smoothAzimuth;
                if (diff > 180) diff -= 360;
                if (diff < -180) diff += 360;
                smoothAzimuth += diff * 0.15f;
                if (smoothAzimuth < 0) smoothAzimuth += 360;
                if (smoothAzimuth >= 360) smoothAzimuth -= 360;

                currentAzimuth = smoothAzimuth;

                long now = System.currentTimeMillis();
                if (now - lastInvalidateTime > 25) {
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
        float outerRadius = Math.min(cx, cy) - 24f;  // space for fixed north triangle
        float dialRadius = outerRadius - 6f;

        // Dark background
        canvas.drawColor(COLOR_BG);

        // === Light/white dial face ===
        canvas.drawCircle(cx, cy, dialRadius, dialFacePaint);

        // Thin ring around dial edge
        canvas.drawCircle(cx, cy, dialRadius, dialRingPaint);

        // === Rotating dial elements ===
        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        float tickOuter = dialRadius - 3f;
        float thinTickInner = dialRadius - 12f;   // 5° ticks - short
        float midTickInner = dialRadius - 18f;    // 10° ticks - medium
        float majorTickInner = dialRadius - 24f;  // 30° ticks - long
        float cardinalCenter = dialRadius - 46f;   // center of cardinal circles
        float degreeTextY = dialRadius - 40f;

        // Tick marks: every 5° thin, every 10° medium, every 30° major
        for (int deg = 0; deg < 360; deg += 5) {
            boolean isMajor = (deg % 30 == 0);
            boolean isMid = (deg % 10 == 0) && !isMajor;
            float inner = isMajor ? majorTickInner : (isMid ? midTickInner : thinTickInner);
            Paint p = isMajor ? majorTickPaint : (isMid ? midTickPaint : thinTickPaint);

            canvas.save();
            canvas.rotate(deg, cx, cy);
            canvas.drawLine(cx, cy - inner, cx, cy - tickOuter, p);
            canvas.restore();
        }

        // Degree labels (every 30°, skip where cardinals go)
        for (int deg = 0; deg < 360; deg += 30) {
            if (deg % 90 != 0) {
                canvas.save();
                canvas.rotate(deg, cx, cy);
                String label = String.valueOf(deg);
                float textWidth = degreeTextPaint.measureText(label);
                canvas.drawText(label, cx - textWidth / 2, cy - degreeTextY, degreeTextPaint);
                canvas.restore();
            }
        }

        // Arabic cardinal labels on colored circles: N=شمال, E=شرق, S=جنوب, W=غرب
        String[] cardinalLabels = {
            "\u0634\u0645\u0627\u0644",  // N
            "\u0634\u0631\u0642",       // E
            "\u062C\u0646\u0648\u0628",  // S
            "\u063A\u0631\u0628"        // W
        };
        Paint[] cardinalFgPaints = {cardinalPaintN, cardinalPaintE, cardinalPaintS, cardinalPaintW};
        Paint[] cardinalBgPaints = {cardinalBgN, cardinalBgE, cardinalBgS, cardinalBgW};
        float circleRadius = 22f;

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(i * 90, cx, cy);

            // Colored background circle
            canvas.drawCircle(cx, cy - cardinalCenter, circleRadius, cardinalBgPaints[i]);

            // Cardinal text (white on colored circle)
            float textWidth = cardinalFgPaints[i].measureText(cardinalLabels[i]);
            canvas.drawText(cardinalLabels[i], cx - textWidth / 2, cy - cardinalCenter + 9f, cardinalFgPaints[i]);

            canvas.restore();
        }

        // Qibla indicator on rotating dial
        if (qiblaBearing > 0) {
            canvas.save();
            canvas.rotate((float) qiblaBearing, cx, cy);

            // Qibla glow
            float glowR = 16f;
            canvas.drawCircle(cx, cy - dialRadius + 4f, glowR, qiblaGlowPaint);

            // Qibla arrow pointing outward
            Path qPath = new Path();
            float qBase = dialRadius - 8f;
            float qTip = dialRadius + 2f;
            qPath.moveTo(cx - 8f, cy - qBase);
            qPath.lineTo(cx, cy - qTip);
            qPath.lineTo(cx + 8f, cy - qBase);
            qPath.close();
            canvas.drawPath(qPath, qiblaArrowPaint);

            // Kaaba cube icon
            Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            kaabaPaint.setStyle(Paint.Style.FILL);
            kaabaPaint.setColor(COLOR_QIBLA_ARROW);
            float ks = 4f;
            RectF kaabaRect = new RectF(cx - ks, cy - qTip - ks - 1f, cx + ks, cy - qTip + ks - 1f);
            canvas.drawRect(kaabaRect, kaabaPaint);

            // "القبلة" label - rotated back upright for readability
            canvas.save();
            canvas.rotate(-(float) qiblaBearing, cx, cy - qBase + 16f);
            String qLabel = "\u0627\u0644\u0642\u0628\u0644\u0629";
            float qlw = qiblaLabelPaint.measureText(qLabel);
            canvas.drawText(qLabel, cx - qlw / 2, cy - qBase + 22f, qiblaLabelPaint);
            canvas.restore();

            canvas.restore();
        }

        canvas.restore(); // End dial rotation

        // === Fixed elements ===

        // Fixed red north triangle at top (Redmi hallmark)
        float nBaseY = cy - outerRadius - 2f;
        float nTipY = nBaseY + 14f;
        Path nPath = new Path();
        nPath.moveTo(cx - 10f, nTipY);
        nPath.lineTo(cx, nBaseY - 6f);
        nPath.lineTo(cx + 10f, nTipY);
        nPath.close();
        // Glow
        canvas.drawPath(nPath, northGlowPaint);
        canvas.drawPath(nPath, fixedNorthPaint);

        // Crosshair lines in center
        float crossLen = 28f;
        crosshairPaint.setAlpha(100);
        canvas.drawLine(cx - crossLen, cy, cx + crossLen, cy, crosshairPaint);
        canvas.drawLine(cx, cy - crossLen, cx, cy + crossLen, crosshairPaint);

        // Center circle + dot
        canvas.drawCircle(cx, cy, 8f, centerRingPaint);
        canvas.drawCircle(cx, cy, 4f, centerDotPaint);

        // Heading readout in center of dial (Redmi style)
        String headingStr = String.format(Locale.US, "%.0f\u00B0", currentAzimuth);
        float headingW = headingPaint.measureText(headingStr);
        canvas.drawText(headingStr, cx - headingW / 2, cy + 22f, headingPaint);

        // Direction abbreviation below heading
        String dirStr = getDirectionLabelArabic(currentAzimuth);
        float dirW = directionPaint.measureText(dirStr);
        canvas.drawText(dirStr, cx - dirW / 2, cy + 44f, directionPaint);
    }
}