package app.organicmaps.hawsa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Custom view that draws an animated tide chart similar to Tides and Currents app.
 * Shows tide height curve with time axis, high/low markers, and current time indicator.
 * Now works with GPS-based station data from TideCalculator.
 */
public class TideChartView extends View {

    private Paint curvePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint labelPaint;
    private Paint markerPaint;
    private Paint currentTimePaint;
    private Paint highLowPaint;

    private List<TideCalculator.TidePoint> tidePoints;
    private List<TideCalculator.TideEvent> tideEvents;
    private float minH = 0;
    private float maxH = 2;
    private int currentHour;
    private int currentMinute;

    private float animProgress = 0f;
    private boolean animating = false;

    // Maritime dark theme colors
    private static final int COLOR_CURVE = 0xFF4FC3F7;
    private static final int COLOR_FILL_TOP = 0x404FC3F7;
    private static final int COLOR_FILL_BOT = 0x100D1B2A;
    private static final int COLOR_GRID = 0x30778DA9;
    private static final int COLOR_LABEL = 0xFF778DA9;
    private static final int COLOR_MARKER = 0xFFE0E1DD;
    private static final int COLOR_CURRENT = 0xFFFF6F61;
    private static final int COLOR_HIGH = 0xFF4CAF50;
    private static final int COLOR_LOW = 0xFFFF5722;
    private static final int COLOR_BG = 0xFF1B263B;

    public TideChartView(Context context) {
        super(context);
        init();
    }

    public TideChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TideChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(3f);
        curvePaint.setColor(COLOR_CURVE);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.5f);
        gridPaint.setColor(COLOR_GRID);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{4f, 4f}, 0));

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(22f);
        labelPaint.setColor(COLOR_LABEL);

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setTextSize(20f);
        markerPaint.setColor(COLOR_MARKER);
        markerPaint.setFakeBoldText(true);

        currentTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentTimePaint.setStyle(Paint.Style.STROKE);
        currentTimePaint.setStrokeWidth(2f);
        currentTimePaint.setColor(COLOR_CURRENT);
        currentTimePaint.setPathEffect(new DashPathEffect(new float[]{6f, 3f}, 0));

        highLowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highLowPaint.setTextSize(18f);
        highLowPaint.setFakeBoldText(true);

        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        currentHour = now.get(Calendar.HOUR_OF_DAY);
        currentMinute = now.get(Calendar.MINUTE);

        startAnimation();
    }

    public void setDayData(Calendar day, List<TideCalculator.TidePoint> points, List<TideCalculator.TideEvent> events) {
        this.tidePoints = points;
        this.tideEvents = events;

        if (points != null && !points.isEmpty()) {
            minH = Float.MAX_VALUE;
            maxH = Float.MIN_VALUE;
            for (TideCalculator.TidePoint p : points) {
                if (p.height < minH) minH = (float) p.height;
                if (p.height > maxH) maxH = (float) p.height;
            }
            minH = (float) Math.floor(minH * 10) / 10 - 0.1f;
            maxH = (float) Math.ceil(maxH * 10) / 10 + 0.1f;
        }

        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        currentHour = now.get(Calendar.HOUR_OF_DAY);
        currentMinute = now.get(Calendar.MINUTE);

        invalidate();
    }

    private void startAnimation() {
        animProgress = 0f;
        animating = true;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                animProgress += 0.03f;
                if (animProgress >= 1f) {
                    animProgress = 1f;
                    animating = false;
                }
                invalidate();
                if (animating) {
                    postDelayed(this, 30);
                }
            }
        }, 30);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float padL = 50f, padR = 20f, padT = 20f, padB = 40f;
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        canvas.drawColor(COLOR_BG);

        float hRange = maxH - minH;
        if (hRange <= 0) hRange = 1f;

        // Horizontal grid + height labels
        int numHLines = 4;
        for (int i = 0; i <= numHLines; i++) {
            float y = padT + chartH * i / numHLines;
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint);
            float heightVal = maxH - (hRange * i / numHLines);
            String label = String.format(Locale.US, "%.1f", heightVal);
            canvas.drawText(label, 4, y + 6, labelPaint);
        }

        // Vertical grid + hour labels
        String[] hourLabels = {"0", "3", "6", "9", "12", "15", "18", "21", "24"};
        for (int i = 0; i < hourLabels.length; i++) {
            float x = padL + chartW * i / (hourLabels.length - 1);
            canvas.drawLine(x, padT, x, padT + chartH, gridPaint);
            canvas.drawText(hourLabels[i], x - 8, h - 8, labelPaint);
        }

        if (tidePoints == null || tidePoints.isEmpty()) return;

        // Build tide curve path with animation
        Path curvePath = new Path();
        Path fillPath = new Path();

        int maxIdx = (int) (tidePoints.size() * animProgress);
        if (maxIdx >= tidePoints.size()) maxIdx = tidePoints.size() - 1;

        for (int i = 0; i <= maxIdx; i++) {
            TideCalculator.TidePoint p = tidePoints.get(i);
            float x = padL + (chartW * (float) p.hour / 24.0f);
            float y = padT + chartH * (1.0f - ((float) p.height - minH) / hRange);

            if (i == 0) {
                curvePath.moveTo(x, y);
                fillPath.moveTo(x, padT + chartH);
                fillPath.lineTo(x, y);
            } else {
                TideCalculator.TidePoint prev = tidePoints.get(i - 1);
                float prevX = padL + (chartW * (float) prev.hour / 24.0f);
                float prevY = padT + chartH * (1.0f - ((float) prev.height - minH) / hRange);
                float midX = (prevX + x) / 2f;
                curvePath.quadTo(prevX + (x - prevX) * 0.5f, (prevY + y) / 2f, x, y);
                fillPath.lineTo(x, y);
            }
        }

        // Close fill path
        float lastX = padL + (chartW * (float) tidePoints.get(maxIdx).hour / 24.0f);
        fillPath.lineTo(lastX, padT + chartH);
        fillPath.close();

        // Gradient fill
        LinearGradient gradient = new LinearGradient(0, padT, 0, padT + chartH,
                COLOR_FILL_TOP, COLOR_FILL_BOT, Shader.TileMode.CLAMP);
        fillPaint.setShader(gradient);
        fillPaint.setAlpha((int)(180 * animProgress));
        canvas.drawPath(fillPath, fillPaint);

        // Draw curve
        curvePaint.setAlpha((int)(255 * animProgress));
        canvas.drawPath(curvePath, curvePaint);

        // Draw high/low tide markers
        if (tideEvents != null && animProgress >= 0.8f) {
            int markerAlpha = (int)(255 * Math.min(1f, (animProgress - 0.8f) / 0.2f));
            for (TideCalculator.TideEvent event : tideEvents) {
                Calendar evtCal = Calendar.getInstance(TimeZone.getDefault());
                evtCal.setTimeInMillis(event.timeMillis);
                float evtHour = evtCal.get(Calendar.HOUR_OF_DAY) + evtCal.get(Calendar.MINUTE) / 60.0f;
                float x = padL + (chartW * evtHour / 24.0f);
                float y = padT + chartH * (1.0f - ((float) event.height - minH) / hRange);

                Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                dotPaint.setColor(event.isHigh ? COLOR_HIGH : COLOR_LOW);
                dotPaint.setAlpha(markerAlpha);
                canvas.drawCircle(x, y, 5f, dotPaint);

                highLowPaint.setColor(event.isHigh ? COLOR_HIGH : COLOR_LOW);
                highLowPaint.setAlpha(markerAlpha);
                String label = formatEventTime(evtCal);
                canvas.drawText(label, x - 20, y - 14, highLowPaint);

                Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                smallPaint.setTextSize(14f);
                smallPaint.setColor(COLOR_LABEL);
                smallPaint.setAlpha(markerAlpha);
                canvas.drawText(String.format(Locale.US, "%.1f\u0645", event.height), x - 16, y + 18, smallPaint);
            }
        }

        // Draw current time indicator
        if (animProgress >= 0.5f) {
            float currentTimeHour = currentHour + currentMinute / 60.0f;
            float cx = padL + (chartW * currentTimeHour / 24.0f);
            currentTimePaint.setAlpha((int)(255 * Math.min(1f, (animProgress - 0.5f) / 0.3f)));
            canvas.drawLine(cx, padT, cx, padT + chartH, currentTimePaint);

            double currentHeight = TideCalculator.getTideHeight(
                Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
            float cy = padT + chartH * (1.0f - ((float) currentHeight - minH) / hRange);
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(COLOR_CURRENT);
            canvas.drawCircle(cx, cy, 6f, dotPaint);

            String timeLabel = String.format(Locale.US, "%d:%02d",
                currentHour > 12 ? currentHour - 12 : (currentHour == 0 ? 12 : currentHour),
                currentMinute) + (currentHour >= 12 ? "\u0645" : "\u0635");
            Paint timeLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            timeLabelPaint.setTextSize(16f);
            timeLabelPaint.setColor(COLOR_CURRENT);
            canvas.drawText(timeLabel, cx - 20, padT - 4, timeLabelPaint);
        }
    }

    private String formatEventTime(Calendar cal) {
        int h = cal.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = cal.get(Calendar.MINUTE);
        String ap = cal.get(Calendar.AM_PM) == Calendar.AM ? "\u0635" : "\u0645";
        return String.format(Locale.US, "%d:%02d%s", h, m, ap);
    }
}
