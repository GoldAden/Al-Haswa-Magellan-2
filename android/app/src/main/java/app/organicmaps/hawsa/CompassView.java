package app.organicmaps.hawsa;

import android.content.Context;
import android.graphics.Canvas;
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
 * Redmi-style modern compass view with smooth dial rotation,
 * clean tick marks, cardinal directions, Qibla indicator,
 * and heading display.
 */
public class CompassView extends View implements SensorEventListener {

    private double qiblaBearing = 0;

    // Redmi-style colors
    private static final int COLOR_BG = 0xFF0D1B2A;
    private static final int COLOR_DIAL_BG = 0xFF1B2838;
    private static final int COLOR_OUTER_RING = 0xFF2C3E50;
    private static final int COLOR_TICK = 0xFF4A5568;
    private static final int COLOR_MAJOR_TICK = 0xFFA0AEC0;
    private static final int COLOR_DEGREE_TEXT = 0xFF718096;
    private static final int COLOR_NORTH = 0xFFE53E3E;
    private static final int COLOR_EAST = 0xFF3182CE;
    private static final int COLOR_SOUTH = 0xFFD69E2E;
    private static final int COLOR_WEST = 0xFF38A169;
    private static final int COLOR_QIBLA_ARROW = 0xFFFFD700;
    private static final int COLOR_QIBLA_LABEL = 0xB3FFD700;
    private static final int COLOR_CROSSHAIR = 0xFFCBD5E0;
    private static final int COLOR_CENTER_DOT = 0xFFE53E3E;
    private static final int COLOR_HEADING_TEXT = 0xFFE2E8F0;
    private static final int COLOR_HEADING_BG = 0xE0102030;

    private Paint dialBgPaint;
    private Paint outerRingPaint;
    private Paint tickPaint;
    private Paint majorTickPaint;
    private Paint degreePaint;
    private Paint cardinalPaint;
    private Paint qiblaArrowPaint;
    private Paint qiblaLabelPaint;
    private Paint crosshairPaint;
    private Paint centerDotPaint;
    private Paint headingPaint;
    private Paint headingBgPaint;
    private Paint innerRingPaint;

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
        outerRingPaint.setStrokeWidth(6f);
        outerRingPaint.setColor(COLOR_OUTER_RING);

        innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setStrokeWidth(2f);
        innerRingPaint.setColor(COLOR_OUTER_RING);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1.2f);
        tickPaint.setColor(COLOR_TICK);

        majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(2.5f);
        majorTickPaint.setColor(COLOR_MAJOR_TICK);

        degreePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        degreePaint.setTextSize(22f);
        degreePaint.setColor(COLOR_DEGREE_TEXT);
        degreePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalPaint.setTextSize(38f);
        cardinalPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        qiblaArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaArrowPaint.setStyle(Paint.Style.FILL);
        qiblaArrowPaint.setColor(COLOR_QIBLA_ARROW);

        qiblaLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qiblaLabelPaint.setTextSize(16f);
        qiblaLabelPaint.setColor(COLOR_QIBLA_LABEL);
        qiblaLabelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(1.5f);
        crosshairPaint.setColor(COLOR_CROSSHAIR);

        centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDotPaint.setStyle(Paint.Style.FILL);
        centerDotPaint.setColor(COLOR_CENTER_DOT);

        headingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingPaint.setTextSize(52f);
        headingPaint.setColor(COLOR_HEADING_TEXT);
        headingPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        headingPaint.setFakeBoldText(true);

        headingBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headingBgPaint.setStyle(Paint.Style.FILL);
        headingBgPaint.setColor(COLOR_HEADING_BG);

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

                float diff = targetAzimuth - smoothAzimuth;
                if (diff > 180) diff -= 360;
                if (diff < -180) diff += 360;
                smoothAzimuth += diff * 0.18f;
                if (smoothAzimuth < 0) smoothAzimuth += 360;
                if (smoothAzimuth >= 360) smoothAzimuth -= 360;

                currentAzimuth = smoothAzimuth;

                long now = System.currentTimeMillis();
                if (now - lastInvalidateTime > 30) {
                    lastInvalidateTime = now;
                    invalidate();
                }
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
        float outerRadius = Math.min(cx, cy) - 20f;
        float dialRadius = outerRadius - 30f;

        canvas.drawColor(COLOR_BG);

        // Outer ring with gradient
        RectF outerRect = new RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius);
        LinearGradient ringGrad = new LinearGradient(cx, cy - outerRadius, cx, cy + outerRadius,
                0xFF2C3E50, 0xFF1B2838, Shader.TileMode.CLAMP);
        Paint ringFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringFillPaint.setStyle(Paint.Style.FILL);
        ringFillPaint.setShader(ringGrad);
        canvas.drawOval(outerRect, ringFillPaint);
        canvas.drawCircle(cx, cy, outerRadius, outerRingPaint);

        // Inner dial background
        canvas.drawCircle(cx, cy, dialRadius, dialBgPaint);
        canvas.drawCircle(cx, cy, dialRadius, innerRingPaint);

        // Rotating elements - dial rotates opposite to azimuth (Redmi style)
        canvas.save();
        canvas.rotate(-currentAzimuth, cx, cy);

        float tickOuter = dialRadius - 6f;
        float tickInner = dialRadius - 16f;
        float majorTickInner = dialRadius - 26f;
        float cardinalY = dialRadius - 50f;
        float degreeY = dialRadius - 48f;

        // Tick marks
        for (int deg = 0; deg < 360; deg += 2) {
            boolean isMajor = (deg % 30 == 0);
            boolean isMid = (deg % 10 == 0) && !isMajor;

            if (isMajor) {
                canvas.drawLine(cx, cy - majorTickInner, cx, cy - tickOuter, majorTickPaint);
            } else if (isMid) {
                canvas.drawLine(cx, cy - tickInner, cx, cy - tickOuter, majorTickPaint);
            } else {
                canvas.drawLine(cx, cy - tickInner, cx, cy - tickOuter, tickPaint);
            }
        }

        // Degree labels (every 30°, skip cardinals)
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

        // Cardinal directions
        String[] cardinals = {"N", "E", "S", "W"};
        String[] arabicLabels = {"شمال", "شرق", "جنوب", "غرب"};
        int[] cardinalColors = {COLOR_NORTH, COLOR_EAST, COLOR_SOUTH, COLOR_WEST};

        for (int i = 0; i < 4; i++) {
            canvas.save();
            canvas.rotate(i * 90, cx, cy);
            cardinalPaint.setColor(cardinalColors[i]);
            float textWidth = cardinalPaint.measureText(cardinals[i]);
            canvas.drawText(cardinals[i], cx - textWidth / 2, cy - cardinalY, cardinalPaint);
            canvas.restore();
        }

        // Qibla indicator on rotating dial
        if (qiblaBearing > 0) {
            canvas.save();
            canvas.rotate((float) qiblaBearing, cx, cy);

            // Qibla arrow pointing outward
            Path qiblaPath = new Path();
            float qBase = dialRadius - 2f;
            float qTip = outerRadius - 2f;
            qiblaPath.moveTo(cx - 9f, cy - qBase);
            qiblaPath.lineTo(cx, cy - qTip);
            qiblaPath.lineTo(cx + 9f, cy - qBase);
            qiblaPath.close();
            canvas.drawPath(qiblaPath, qiblaArrowPaint);

            // Kaaba symbol (small filled square) at tip
            Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            kaabaPaint.setStyle(Paint.Style.FILL);
            kaabaPaint.setColor(COLOR_QIBLA_ARROW);
            float kaabaSize = 7f;
            RectF kaabaRect = new RectF(cx - kaabaSize, cy - qTip - kaabaSize - 2f,
                    cx + kaabaSize, cy - qTip + kaabaSize - 2f);
            canvas.drawRect(kaabaRect, kaabaPaint);

            canvas.restore();
        }

        canvas.restore(); // End rotation

        // Fixed elements
        // Fixed north indicator triangle at top of dial
        Paint fixedNorthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fixedNorthPaint.setStyle(Paint.Style.FILL);
        fixedNorthPaint.setColor(COLOR_NORTH);
        Path northPath = new Path();
        float nY = cy - dialRadius - 8f;
        northPath.moveTo(cx - 10f, nY);
        northPath.lineTo(cx, nY - 14f);
        northPath.lineTo(cx + 10f, nY);
        northPath.close();
        canvas.drawPath(northPath, fixedNorthPaint);

        // Crosshair at center
        float crossSize = 28f;
        crosshairPaint.setAlpha(120);
        canvas.drawLine(cx - crossSize, cy, cx + crossSize, cy, crosshairPaint);
        canvas.drawLine(cx, cy - crossSize, cx, cy + crossSize, crosshairPaint);
        crosshairPaint.setAlpha(255);

        // Center dot
        canvas.drawCircle(cx, cy, 5f, centerDotPaint);

        // Heading display (fixed at top center, above dial)
        String headingStr = String.format(Locale.US, "%.0f\u00B0", currentAzimuth);
        float headingWidth = headingPaint.measureText(headingStr);
        RectF headingRect = new RectF(
                cx - headingWidth / 2 - 18f,
                cy - outerRadius + 12f,
                cx + headingWidth / 2 + 18f,
                cy - outerRadius + 68f
        );
        canvas.drawRoundRect(headingRect, 10f, 10f, headingBgPaint);
        canvas.drawText(headingStr, cx - headingWidth / 2, cy - outerRadius + 58f, headingPaint);
    }
}
