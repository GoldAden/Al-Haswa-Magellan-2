package app.organicmaps.hawsa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.SweepGradient;
import android.graphics.RadialGradient;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/**
 * Redesigned Redmi-style modern compass view - fullscreen immersive.
 * Features: smooth rotation, gradient ring, glow effects, qibla indicator,
 * heading display with direction label.
 */
public class CompassView extends View implements SensorEventListener {

    private double qiblaBearing = 0;

    // Redmi-style dark theme colors
    private static final int COLOR_BG = 0xFF0A0E1A;
    private static final int COLOR_DIAL_BG = 0xFF111827;
    private static final int COLOR_OUTER_RING = 0xFF1E293B;
    private static final int COLOR_INNER_GLOW = 0xFF1E293B;
    private static final int COLOR_TICK = 0xFF334155;
    private static final int COLOR_MAJOR_TICK = 0xFF94A3B8;
    private static final int COLOR_DEGREE_TEXT = 0xFF64748B;
    private static final int COLOR_NORTH = 0xFFEF4444;
    private static final int COLOR_EAST = 0xFF3B82F6;
    private static final int COLOR_SOUTH = 0xFFF59E0B;
    private static final int COLOR_WEST = 0xFF10B981;
    private static final int COLOR_QIBLA_ARROW = 0xFFD4AF37;
    private static final int COLOR_QIBLA_GLOW = 0x30D4AF37;
    private static final int COLOR_QIBLA_LABEL = 0xCCD4AF37;
    private static final int COLOR_CROSSHAIR = 0xFF475569;
    private static final int COLOR_CENTER_DOT = 0xFFEF4444;
    private static final int COLOR_HEADING_TEXT = 0xFFF1F5F9;
    private static final int COLOR_DIRECTION_TEXT = 0xFF94A3B8;
    private static final int COLOR_HEADING_BG = 0xD0102030;
    private static final int COLOR_N_GLOW = 0x20EF4444;

    private Paint dialBgPaint;
    private Paint outerRingPaint;
    private Paint outerRingFillPaint;
    private Paint tickPaint;
    private Paint majorTickPaint;
    private Paint degreePaint;
    private Paint cardinalPaint;
    private Paint qiblaArrowPaint;
    private Paint qiblaGlowPaint;
    private Paint qiblaLabelPaint;
    private Paint crosshairPaint;
    private Paint headingPaint;
    private Paint directionPaint;
    private Paint headingBgPaint;
    private Paint fixedNorthPaint;
    private Paint northGlowPaint;

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
        dialBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialBgPaint.setStyle(Paint.Style.FILL);
        dialBgPaint.setColor(COLOR_DIAL_BG);

        outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRingPaint.setStyle(Paint.Style.STROKE);
        outerRingPaint.setStrokeWidth(4f);
        outerRingPaint.setColor(COLOR_OUTER_RING);

        outerRingFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRingFillPaint.setStyle(Paint.Style.FILL);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1f);
        tickPaint.setColor(COLOR_TICK);

        majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(2.5f);
        majorTickPaint.setColor(COLOR_MAJOR_TICK);

        degreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreePaint.setTextSize(20f);
        degreePaint.setColor(COLOR_DEGREE_TEXT);
        degreePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalPaint.setTextSize(42f);
        cardinalPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        qiblaArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaArrowPaint.setStyle(Paint.Style.FILL);
        qiblaArrowPaint.setColor(COLOR_QIBLA_ARROW);

        qiblaGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaGlowPaint.setStyle(Paint.Style.FILL);
        qiblaGlowPaint.setColor(COLOR_QIBLA_GLOW);

        qiblaLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaLabelPaint.setTextSize(14f);
        qiblaLabelPaint.setColor(COLOR_QIBLA_LABEL);
        qiblaLabelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1.2f);
        crosshairPaint.setColor(COLOR_CROSSHAIR);

        headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingPaint.setTextSize(64f);
        headingPaint.setColor(COLOR_HEADING_TEXT);
        headingPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        headingPaint.setFakeBoldText(true);

        directionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionPaint.setTextSize(22f);
        directionPaint.setColor(COLOR_DIRECTION_TEXT);
        directionPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        headingBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingBgPaint.setStyle(Paint.Style.FILL);
        headingBgPaint.setColor(COLOR_HEADING_BG);

        fixedNorthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fixedNorthPaint.setStyle(Paint.Style.FILL);
        fixedNorthPaint.setColor(COLOR_NORTH);

        northGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northGlowPaint.setStyle(Paint.Style.FILL);
        northGlowPaint.setColor(COLOR_N_GLOW);

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

                // Smoother Redmi-style interpolation
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

    private String getDirectionLabel(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "N";
        if (azimuth >= 22.5 && azimuth < 67.5) return "NE";
        if (azimuth >= 67.5 && azimuth < 112.5) return "E";
        if (azimuth >= 112.5 && azimuth < 157.5) return "SE";
        if (azimuth >= 157.5 && azimuth < 202.5) return "S";
        if (azimuth >= 202.5 && azimuth < 247.5) return "SW";
        if (azimuth >= 247.5 && azimuth < 292.5) return "W";
        if (azimuth >= 292.5 && azimuth < 337.5) return "NW";
        return "N";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float outerRadius = Math.min(cx, cy) - 16f;
        float dialRadius = outerRadius - 28f;

        canvas.drawColor(COLOR_BG);

        // Outer ring with sweep gradient (Redmi glow effect)
        RectF outerRect = new RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius);
        SweepGradient sweepGrad = new SweepGradient(cx, cy,
                new int[]{0xFF1E293B, 0xFF334155, 0xFF1E293B, 0xFF0F172A, 0xFF1E293B},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1f});
        outerRingFillPaint.setShader(sweepGrad);
        canvas.drawOval(outerRect, outerRingFillPaint);
        canvas.drawCircle(cx, cy, outerRadius, outerRingPaint);

        // Inner glow ring
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setColor(COLOR_INNER_GLOW);
        canvas.drawCircle(cx, cy, dialRadius + 6f, glowPaint);

        // Inner dial background with subtle radial gradient
        RadialGradient dialGrad = new RadialGradient(cx, cy, dialRadius,
                0xFF1A2332, COLOR_DIAL_BG, Shader.TileMode.CLAMP);
        Paint dialGradPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dialGradPaint.setStyle(Paint.Style.FILL);
        dialGradPaint.setShader(dialGrad);
        canvas.drawCircle(cx, cy, dialRadius, dialGradPaint);

        // Thin inner ring
        Paint thinRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thinRingPaint.setStyle(Paint.Style.STROKE);
        thinRingPaint.setStrokeWidth(1f);
        thinRingPaint.setColor(0xFF1E293B);
        canvas.drawCircle(cx, cy, dialRadius, thinRingPaint);

        // === Rotating dial elements ===
        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        float tickOuter = dialRadius - 4f;
        float tickInner = dialRadius - 14f;
        float majorTickInner = dialRadius - 22f;
        float midTickInner = dialRadius - 18f;
        float cardinalY = dialRadius - 48f;
        float degreeY = dialRadius - 44f;

        // Tick marks
        for (int deg = 0; deg < 360; deg += 2) {
            boolean isMajor = (deg % 30 == 0);
            boolean isMid = (deg % 10 == 0) && !isMajor;

            canvas.save();
            canvas.rotate(deg, cx, cy);

            if (isMajor) {
                canvas.drawLine(cx, cy - majorTickInner, cx, cy - tickOuter, majorTickPaint);
            } else if (isMid) {
                canvas.drawLine(cx, cy - midTickInner, cx, cy - tickOuter, majorTickPaint);
            } else {
                canvas.drawLine(cx, cy - tickInner, cx, cy - tickOuter, tickPaint);
            }

            canvas.restore();
        }

        // Degree labels (every 30, skip cardinals)
        for (int deg = 0; deg < 360; deg += 30) {
            if (deg % 90 != 0) {
                canvas.save();
                canvas.rotate(deg, cx, cy);
                String label = String.valueOf(deg);
                float textWidth = degreePaint.measureText(label);
                canvas.drawText(label, cx - textWidth / 2, cy - degreeY, degreePaint);
                canvas.restore();
            }
        }

        // Cardinal directions with glow
        String[] cardinals = {"N", "E", "S", "W"};
        int[] cardinalColors = {COLOR_NORTH, COLOR_EAST, COLOR_SOUTH, COLOR_WEST};

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(i * 90, cx, cy);

            // Glow behind cardinal
            Paint cGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            cGlow.setTextSize(42f);
            cGlow.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            cGlow.setColor(cardinalColors[i]);
            cGlow.setAlpha(40);
            float glowWidth = cGlow.measureText(cardinals[i]);
            canvas.drawText(cardinals[i], cx - glowWidth / 2, cy - cardinalY, cGlow);

            // Main cardinal text
            cardinalPaint.setColor(cardinalColors[i]);
            cardinalPaint.setAlpha(255);
            float textWidth = cardinalPaint.measureText(cardinals[i]);
            canvas.drawText(cardinals[i], cx - textWidth / 2, cy - cardinalY, cardinalPaint);

            canvas.restore();
        }

        // Qibla indicator with glow
        if (qiblaBearing > 0) {
            canvas.save();
            canvas.rotate((float) qiblaBearing, cx, cy);

            // Qibla glow
            float glowRadius = 22f;
            canvas.drawCircle(cx, cy - outerRadius + 6f, glowRadius, qiblaGlowPaint);

            // Qibla arrow
            Path qiblaPath = new Path();
            float qBase = dialRadius - 2f;
            float qTip = outerRadius + 2f;
            qiblaPath.moveTo(cx - 10f, cy - qBase);
            qiblaPath.lineTo(cx, cy - qTip);
            qiblaPath.lineTo(cx + 10f, cy - qBase);
            qiblaPath.close();
            canvas.drawPath(qiblaPath, qiblaArrowPaint);

            // Kaaba icon
            Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            kaabaPaint.setStyle(Paint.Style.FILL);
            kaabaPaint.setColor(COLOR_QIBLA_ARROW);
            float kaabaSize = 5f;
            RectF kaabaRect = new RectF(cx - kaabaSize, cy - qTip - kaabaSize - 1f,
                    cx + kaabaSize, cy - qTip + kaabaSize - 1f);
            canvas.drawRect(kaabaRect, kaabaPaint);

            // Qibla label rotated upright
            canvas.save();
            canvas.rotate(-(float) qiblaBearing, cx, cy - qBase + 18f);
            String qLabel = "\u0627\u0644\u0642\u0628\u0644\u0629";
            float qLabelWidth = qiblaLabelPaint.measureText(qLabel);
            canvas.drawText(qLabel, cx - qLabelWidth / 2, cy - qBase + 24f, qiblaLabelPaint);
            canvas.restore();

            canvas.restore();
        }

        canvas.restore(); // End dial rotation

        // === Fixed elements ===

        // Fixed north indicator at top
        Path northPath = new Path();
        float nY = cy - dialRadius - 6f;
        northPath.moveTo(cx - 11f, nY + 6f);
        northPath.lineTo(cx, nY - 10f);
        northPath.lineTo(cx + 11f, nY + 6f);
        northPath.close();
        canvas.drawPath(northPath, northGlowPaint);
        canvas.drawPath(northPath, fixedNorthPaint);

        // Crosshair lines
        float crossSize = 32f;
        crosshairPaint.setAlpha(80);
        canvas.drawLine(cx - crossSize, cy, cx + crossSize, cy, crosshairPaint);
        canvas.drawLine(cx, cy - crossSize, cx, cy + crossSize, crosshairPaint);

        // Center circle
        Paint centerOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerOuter.setStyle(Paint.Style.FILL);
        centerOuter.setColor(0xFF1E293B);
        canvas.drawCircle(cx, cy, 10f, centerOuter);

        Paint centerInner = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerInner.setStyle(Paint.Style.FILL);
        centerInner.setColor(COLOR_CENTER_DOT);
        canvas.drawCircle(cx, cy, 5f, centerInner);

        Paint centerDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDot.setStyle(Paint.Style.FILL);
        centerDot.setColor(0xFFFEF2F2);
        canvas.drawCircle(cx, cy, 2f, centerDot);

        // Heading display at top
        String headingStr = String.format(Locale.US, "%.0f\u00B0", currentAzimuth);
        String dirLabel = getDirectionLabel(currentAzimuth);
        float headingWidth = headingPaint.measureText(headingStr);
        float dirWidth = directionPaint.measureText(dirLabel);
        float totalWidth = headingWidth + 12f + dirWidth;

        RectF headingRect = new RectF(
                cx - totalWidth / 2 - 24f,
                cy - outerRadius + 6f,
                cx + totalWidth / 2 + 24f,
                cy - outerRadius + 72f
        );
        canvas.drawRoundRect(headingRect, 14f, 14f, headingBgPaint);
        canvas.drawText(headingStr, cx - totalWidth / 2, cy - outerRadius + 56f, headingPaint);
        directionPaint.setAlpha(180);
        canvas.drawText(dirLabel, cx - totalWidth / 2 + headingWidth + 12f, cy - outerRadius + 56f, directionPaint);
    }
}
