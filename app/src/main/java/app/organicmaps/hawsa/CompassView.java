package app.organicmaps.hawsa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/**
 * Modern compass view with rotating dial
 * North needle: Red
 * Qibla needle: Gold (#D4AF37)
 * Other cardinal needles: Silver
 */
public class CompassView extends View implements SensorEventListener {

    private static final double QIBLA_BEARING = 248.0;

    // Dark navy + gold theme
    private static final int COLOR_BG = 0xFF0D1B2A;
    private static final int COLOR_OUTER_RING = 0xFFD4AF37;       // Gold outer ring
    private static final int COLOR_INNER_RING = 0xFF1B263B;
    private static final int COLOR_TICK = 0xFF778DA9;
    private static final int COLOR_MAJOR_TICK = 0xFFE0E1DD;
    private static final int COLOR_NORTH = 0xFFE53935;             // Red for North
    private static final int COLOR_QIBLA = 0xFFD4AF37;             // Gold for Qibla
    private static final int COLOR_SILVER = 0xFFC0C0C0;            // Silver for other directions
    private static final int COLOR_CENTER = 0xFFD4AF37;            // Gold center
    private static final int COLOR_DEGREE_TEXT = 0xFF778DA9;
    private static final int COLOR_CARDINAL_BG = 0x400D1B2A;       // Semi-transparent bg for labels

    private Paint outerRingPaint;
    private Paint innerRingPaint;
    private Paint tickPaint;
    private Paint majorTickPaint;
    private Paint northPaint;
    private Paint qiblaPaint;
    private Paint silverPaint;
    private Paint centerPaint;
    private Paint degreePaint;
    private Paint bgPaint;
    private Paint labelBgPaint;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravityValues = null;
    private float[] geomagneticValues = null;
    private float currentAzimuth = 0f;
    private float targetAzimuth = 0f;
    private float smoothAzimuth = 0f;

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
        outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRingPaint.setStyle(Paint.Style.STROKE);
        outerRingPaint.setStrokeWidth(3f);
        outerRingPaint.setColor(COLOR_OUTER_RING);

        innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(1.5f);
        innerRingPaint.setColor(COLOR_INNER_RING);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1f);
        tickPaint.setColor(COLOR_TICK);

        majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(2f);
        majorTickPaint.setColor(COLOR_MAJOR_TICK);

        northPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northPaint.setStyle(Paint.Style.FILL);
        northPaint.setColor(COLOR_NORTH);

        qiblaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaPaint.setStyle(Paint.Style.FILL);
        qiblaPaint.setColor(COLOR_QIBLA);

        silverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        silverPaint.setStyle(Paint.Style.FILL);
        silverPaint.setColor(COLOR_SILVER);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(COLOR_CENTER);

        degreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreePaint.setTextSize(28f);
        degreePaint.setColor(COLOR_DEGREE_TEXT);
        degreePaint.setFakeBoldText(true);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(COLOR_BG);

        labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelBgPaint.setStyle(Paint.Style.FILL);
        labelBgPaint.setColor(COLOR_CARDINAL_BG);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
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

                float diff = targetAzimuth - smoothAzimuth;
                if (diff > 180) diff -= 360;
                if (diff < -180) diff += 360;
                smoothAzimuth += diff * 0.15f;
                if (smoothAzimuth < 0) smoothAzimuth += 360;
                if (smoothAzimuth >= 360) smoothAzimuth -= 360;

                currentAzimuth = smoothAzimuth;
                invalidate();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - 20f;

        // Background
        canvas.drawColor(COLOR_BG);

        // Outer gold ring with glow
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(6f);
        glowPaint.setColor(0x40D4AF37);
        canvas.drawCircle(cx, cy, radius + 4, glowPaint);
        canvas.drawCircle(cx, cy, radius, outerRingPaint);

        // Second decorative ring
        canvas.drawCircle(cx, cy, radius * 0.92f, innerRingPaint);

        // Draw ticks (rotate with compass)
        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        for (int deg = 0; deg < 360; deg += 5) {
            float rad = (float) Math.toRadians(deg);
            float innerR, outerR;
            Paint usePaint;

            if (deg % 30 == 0) {
                innerR = radius * 0.82f;
                outerR = radius * 0.92f;
                usePaint = majorTickPaint;
            } else if (deg % 10 == 0) {
                innerR = radius * 0.85f;
                outerR = radius * 0.92f;
                usePaint = tickPaint;
            } else {
                innerR = radius * 0.88f;
                outerR = radius * 0.92f;
                usePaint = tickPaint;
            }

            canvas.drawLine(
                cx + innerR * (float) Math.sin(rad),
                cy - innerR * (float) Math.cos(rad),
                cx + outerR * (float) Math.sin(rad),
                cy - outerR * (float) Math.cos(rad),
                usePaint
            );

            // Degree numbers every 30°
            if (deg % 30 == 0 && deg % 90 != 0) {
                Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                numPaint.setTextSize(12f);
                numPaint.setColor(COLOR_TICK);
                numPaint.setTextAlign(Paint.Align.CENTER);
                float labelR = radius * 0.74f;
                float lx = cx + labelR * (float) Math.sin(rad);
                float ly = cy - labelR * (float) Math.cos(rad);
                canvas.save();
                canvas.rotate(deg, lx, ly);
                canvas.drawText(String.valueOf(deg), lx, ly + 4, numPaint);
                canvas.restore();
            }
        }

        // ===== Cardinal Direction Needles =====
        float needleLength = radius * 0.55f;
        float needleBase = 10f;

        // NORTH needle (RED) - triangle pointing up
        Path northPath = new Path();
        northPath.moveTo(cx, cy - needleLength);           // tip
        northPath.lineTo(cx - needleBase, cy);              // base left
        northPath.lineTo(cx + needleBase, cy);              // base right
        northPath.close();
        canvas.drawPath(northPath, northPaint);

        // Small "N" label above north needle
        Paint northLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        northLabel.setTextSize(14f);
        northLabel.setColor(COLOR_NORTH);
        northLabel.setFakeBoldText(true);
        northLabel.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ش", cx, cy - needleLength - 8, northLabel);

        // SOUTH needle (SILVER) - triangle pointing down
        Path southPath = new Path();
        southPath.moveTo(cx, cy + needleLength);            // tip
        southPath.lineTo(cx - needleBase, cy);               // base left
        southPath.lineTo(cx + needleBase, cy);               // base right
        southPath.close();
        canvas.drawPath(southPath, silverPaint);

        // EAST needle (SILVER) - small triangle pointing right
        Path eastPath = new Path();
        float eastLen = radius * 0.35f;
        eastPath.moveTo(cx + eastLen, cy);
        eastPath.lineTo(cx, cy - 6);
        eastPath.lineTo(cx, cy + 6);
        eastPath.close();
        canvas.drawPath(eastPath, silverPaint);

        // WEST needle (SILVER) - small triangle pointing left
        Path westPath = new Path();
        westPath.moveTo(cx - eastLen, cy);
        westPath.lineTo(cx, cy - 6);
        westPath.lineTo(cx, cy + 6);
        westPath.close();
        canvas.drawPath(westPath, silverPaint);

        // Center decorative ring
        canvas.drawCircle(cx, cy, 10f, centerPaint);
        canvas.drawCircle(cx, cy, 10f, outerRingPaint);  // Gold outline on center

        canvas.restore(); // End rotating section

        // ===== QIBLA INDICATOR (Gold, rotates opposite to compass) =====
        float qiblaScreenAngle = (float) (QIBLA_BEARING - currentAzimuth);
        float qiblaRad = (float) Math.toRadians(qiblaScreenAngle);

        // Qibla arrow (gold, prominent)
        Path qiblaArrow = new Path();
        float qTip = radius * 0.52f;
        float qBase = radius * 0.25f;
        float qArrowW = 12f;

        float tipX = cx + qTip * (float) Math.sin(qiblaRad);
        float tipY = cy - qTip * (float) Math.cos(qiblaRad);
        float baseLX = cx + qBase * (float) Math.sin(qiblaRad) - qArrowW * (float) Math.cos(qiblaRad);
        float baseLY = cy - qBase * (float) Math.cos(qiblaRad) - qArrowW * (float) Math.sin(qiblaRad);
        float baseRX = cx + qBase * (float) Math.sin(qiblaRad) + qArrowW * (float) Math.cos(qiblaRad);
        float baseRY = cy - qBase * (float) Math.cos(qiblaRad) + qArrowW * (float) Math.sin(qiblaRad);

        qiblaArrow.moveTo(tipX, tipY);
        qiblaArrow.lineTo(baseLX, baseLY);
        qiblaArrow.lineTo(baseRX, baseRY);
        qiblaArrow.close();
        canvas.drawPath(qiblaArrow, qiblaPaint);

        // Qibla glow effect
        Paint qiblaGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaGlow.setStyle(Paint.Style.STROKE);
        qiblaGlow.setStrokeWidth(3f);
        qiblaGlow.setColor(0x40D4AF37);
        canvas.drawPath(qiblaArrow, qiblaGlow);

        // Kaaba emoji label along qibla direction
        Paint qiblaLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaLabel.setTextSize(16f);
        qiblaLabel.setColor(COLOR_QIBLA);
        qiblaLabel.setFakeBoldText(true);
        qiblaLabel.setTextAlign(Paint.Align.CENTER);
        float qlR = radius * 0.20f;
        float qlX = cx + qlR * (float) Math.sin(qiblaRad);
        float qlY = cy - qlR * (float) Math.cos(qiblaRad);
        canvas.drawText("🕋", qlX, qlY + 6, qiblaLabel);

        // Current heading at bottom (gold text)
        String headingText = String.format(Locale.US, "%.0f°", currentAzimuth);
        Paint headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingPaint.setTextSize(30f);
        headingPaint.setColor(COLOR_QIBLA);
        headingPaint.setFakeBoldText(true);
        headingPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(headingText, cx, cy + radius + 28, headingPaint);

        // "N" indicator at top of view (fixed, not rotating)
        Paint fixedNorth = new Paint(Paint.ANTI_ALIAS_FLAG);
        fixedNorth.setTextSize(12f);
        fixedNorth.setColor(COLOR_NORTH);
        fixedNorth.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("شمال", cx, padTop(cx, cy, radius) - 6, fixedNorth);
    }

    private float padTop(float cx, float cy, float radius) {
        return cy - radius - 4;
    }
}