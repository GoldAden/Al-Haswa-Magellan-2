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
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

import java.util.Locale;

/**
 * Real compass view with rotating dial and Qibla needle
 * Shows magnetic north with a rotating compass rose
 * and a fixed Qibla indicator pointing to Mecca
 */
public class CompassView extends View implements SensorEventListener {

    // Qibla direction from Al-Haswa: ~248° from North
    private static final double QIBLA_BEARING = 248.0;

    // Colors - maritime dark theme
    private static final int COLOR_BG = 0xFF1B263B;
    private static final int COLOR_RING = 0xFF415A77;
    private static final int COLOR_TICK = 0xFF778DA9;
    private static final int COLOR_MAJOR_TICK = 0xFFE0E1DD;
    private static final int COLOR_NORTH = 0xFFFF6F61;        // Orange-red for North
    private static final int COLOR_SOUTH = 0xFF778DA9;
    private static final int COLOR_QIBLA = 0xFFFFD700;       // Gold for Qibla
    private static final int COLOR_CENTER = 0xFFE0E1DD;
    private static final int COLOR_DEGREE_TEXT = 0xFF778DA9;

    private Paint ringPaint;
    private Paint tickPaint;
    private Paint majorTickPaint;
    private Paint northPaint;
    private Paint southPaint;
    private Paint qiblaPaint;
    private Paint centerPaint;
    private Paint degreePaint;
    private Paint bgPaint;
    private Paint innerRingPaint;

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
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(4f);
        ringPaint.setColor(COLOR_RING);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1.5f);
        tickPaint.setColor(COLOR_TICK);

        majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(2.5f);
        majorTickPaint.setColor(COLOR_MAJOR_TICK);

        northPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northPaint.setStyle(Paint.Style.FILL);
        northPaint.setColor(COLOR_NORTH);

        southPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        southPaint.setStyle(Paint.Style.FILL);
        southPaint.setColor(COLOR_SOUTH);

        qiblaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaPaint.setStyle(Paint.Style.FILL);
        qiblaPaint.setColor(COLOR_QIBLA);

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

        innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(2f);
        innerRingPaint.setColor(COLOR_RING);

        // Setup sensors
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

                // Smooth the rotation
                float diff = targetAzimuth - smoothAzimuth;
                if (diff > 180) diff -= 360;
                if (diff < -180) diff += 360;
                smoothAzimuth += diff * 0.2f;
                if (smoothAzimuth < 0) smoothAzimuth += 360;
                if (smoothAzimuth >= 360) smoothAzimuth -= 360;

                currentAzimuth = smoothAzimuth;
                invalidate();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - 16f;

        canvas.drawColor(COLOR_BG);

        // Outer ring
        canvas.drawCircle(cx, cy, radius, ringPaint);
        // Inner ring
        canvas.drawCircle(cx, cy, radius * 0.85f, innerRingPaint);

        // Draw degree marks and ticks (rotate with compass)
        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        // Draw ticks every 10 degrees, major every 30
        for (int deg = 0; deg < 360; deg += 10) {
            float rad = (float) Math.toRadians(deg);
            float innerR = radius * 0.85f;
            float outerR = radius * 0.95f;

            if (deg % 30 == 0) {
                float labelR = radius * 0.73f;
                // Draw degree label
                Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                labelPaint.setTextSize(18f);
                labelPaint.setColor(COLOR_DEGREE_TEXT);
                labelPaint.setTextAlign(Paint.Align.CENTER);

                String label;
                switch (deg) {
                    case 0: label = "ش"; break;   // North
                    case 90: label = "ق"; break;  // East
                    case 180: label = "ج"; break; // South
                    case 270: label = "غ"; break; // West
                    default: label = String.valueOf(deg);
                        labelPaint.setTextSize(14f);
                        labelPaint.setColor(COLOR_TICK);
                        break;
                }
                float lx = cx + labelR * (float) Math.sin(rad);
                float ly = cy - labelR * (float) Math.cos(rad);
                canvas.save();
                canvas.rotate(deg, lx, ly);
                canvas.drawText(label, lx, ly + 6, labelPaint);
                canvas.restore();

                // Major tick
                canvas.drawLine(
                    cx + innerR * (float) Math.sin(rad),
                    cy - innerR * (float) Math.cos(rad),
                    cx + outerR * (float) Math.sin(rad),
                    cy - outerR * (float) Math.cos(rad),
                    majorTickPaint
                );
            } else {
                // Minor tick
                float minorInner = radius * 0.88f;
                canvas.drawLine(
                    cx + minorInner * (float) Math.sin(rad),
                    cy - minorInner * (float) Math.cos(rad),
                    cx + outerR * (float) Math.sin(rad),
                    cy - outerR * (float) Math.cos(rad),
                    tickPaint
                );
            }
        }

        // North indicator (triangle pointing up)
        Path northPath = new Path();
        float nTip = radius * 0.65f;
        northPath.moveTo(cx, cy - nTip);
        northPath.lineTo(cx - 10, cy - nTip + 24);
        northPath.lineTo(cx + 10, cy - nTip + 24);
        northPath.close();
        canvas.drawPath(northPath, northPaint);

        // South indicator (triangle pointing down)
        Path southPath = new Path();
        southPath.moveTo(cx, cy + nTip);
        southPath.lineTo(cx - 10, cy + nTip - 24);
        southPath.lineTo(cx + 10, cy + nTip - 24);
        southPath.close();
        canvas.drawPath(southPath, southPaint);

        // Center decorative ring
        canvas.drawCircle(cx, cy, 8f, centerPaint);

        canvas.restore(); // End rotating section

        // Draw Qibla indicator (fixed relative to device, moves opposite to compass)
        // Qibla is at 248° from North, so on screen it appears at (QIBLA_BEARING - currentAzimuth)
        float qiblaScreenAngle = (float) (QIBLA_BEARING - currentAzimuth);
        float qiblaRad = (float) Math.toRadians(qiblaScreenAngle);

        // Qibla needle (gold triangle)
        Path qiblaPath = new Path();
        float qTip = radius * 0.60f;
        float qBase = radius * 0.35f;
        float qiblaX = cx + qTip * (float) Math.sin(qiblaRad);
        float qiblaY = cy - qTip * (float) Math.cos(qiblaRad);
        float baseLX = cx + qBase * (float) Math.sin(qiblaRad - 0.08f);
        float baseLY = cy - qBase * (float) Math.cos(qiblaRad - 0.08f);
        float baseRX = cx + qBase * (float) Math.sin(qiblaRad + 0.08f);
        float baseRY = cy - qBase * (float) Math.cos(qiblaRad + 0.08f);

        qiblaPath.moveTo(qiblaX, qiblaY);
        qiblaPath.lineTo(baseLX, baseLY);
        qiblaPath.lineTo(baseRX, baseRY);
        qiblaPath.close();
        canvas.drawPath(qiblaPath, qiblaPaint);

        // Qibla label (ك)
        Paint qiblaLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaLabelPaint.setTextSize(16f);
        qiblaLabelPaint.setColor(COLOR_QIBLA);
        qiblaLabelPaint.setFakeBoldText(true);
        qiblaLabelPaint.setTextAlign(Paint.Align.CENTER);
        float qlR = radius * 0.30f;
        float qlX = cx + qlR * (float) Math.sin(qiblaRad);
        float qlY = cy - qlR * (float) Math.cos(qiblaRad);
        canvas.drawText("🕋", qlX, qlY + 6, qiblaLabelPaint);

        // Draw current heading text at bottom
        String headingText = String.format(Locale.US, "%.0f°", currentAzimuth);
        degreePaint.setTextSize(32f);
        degreePaint.setColor(COLOR_MAJOR_TICK);
        canvas.drawText(headingText, cx, cy + radius + 30, degreePaint);
    }
}