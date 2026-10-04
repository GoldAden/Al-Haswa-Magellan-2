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
 * Custom view that draws an animated tide chart
 * Shows tide height curve with time axis, high/low markers showing TIME instead of height,
 * and current time indicator
 */
public class TideChartView extends View {

    private Paint curvePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint labelPaint;
    private Paint markerPaint;
    private Paint currentTimePaint;
    private Paint highLowPaint;
    private Paint goldPaint;

    private List<TideCalculator.TidePoint> tidePoints;
    private List<TideCalculator.TideEvent> tideEvents;
    private float minH = 0;
    private float maxH = 2;
    private int currentHour;
    private int currentMinute;

    private float animProgress = 0f;
    private boolean animating = false;

    // Colors matching the dark navy + gold theme
    private static final int COLOR_CURVE = 0xFF4FC3F7;
    private static final int COLOR_FILL_TOP = 0x404FC3F7;
    private static final int COLOR_FILL_BOT = 0x100D1B2A;
    private static final int COLOR_GRID = 0x30778DA9;
    private static final int COLOR_LABEL = 0xFF778DA9;
    private static final int COLOR_MARKER = 0xFFE0E1DD;
    private static final int COLOR_CURRENT = 0xFFD4AF37;   // Gold for current time
    private static final int COLOR_HIGH = 0xFF4CAF50;
    private static final int COLOR_LOW = 0xFFFF5722;
    private static final int COLOR_BG = 0xFF0D1B2A;        // Dark navy
    private static final int COLOR_GOLD = 0xFFD4AF37;

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
        highLowPaint.setTextSize(16f);
        highLowPaint.setFakeBoldText(true);

        goldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        goldPaint.setStyle(Paint.Style.STROKE);
        goldPaint.setStrokeWidth(1.5f);
        goldPaint.setColor(COLOR_GOLD);

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
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
        float padL = 55f, padR = 20f, padT = 25f, padB = 40f;
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        // Background
        canvas.drawColor(COLOR_BG);

        // Draw grid lines
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

        // Build the tide curve path with animation
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
                if (i > 0 && i < maxIdx) {
                    TideCalculator.TidePoint prev = tidePoints.get(i - 1);
                    float prevX = padL + (chartW * (float) prev.hour / 24.0f);
                    float prevY = padT + chartH * (1.0f - ((float) prev.height - minH) / hRange);
                    curvePath.quadTo(prevX + (x - prevX) * 0.5f, (prevY + y) / 2f, x, y);
                } else {
                    curvePath.lineTo(x, y);
                }
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

        // Draw high/low tide markers with TIME labels instead of height
        if (tideEvents != null && animProgress >= 0.8f) {
            int markerAlpha = (int)(255 * Math.min(1f, (animProgress - 0.8f) / 0.2f));
            for (TideCalculator.TideEvent event : tideEvents) {
                Calendar evtCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
                evtCal.setTimeInMillis(event.timeMillis);
                float evtHour = evtCal.get(Calendar.HOUR_OF_DAY) + evtCal.get(Calendar.MINUTE) / 60.0f;
                float x = padL + (chartW * evtHour / 24.0f);
                float y = padT + chartH * (1.0f - ((float) event.height - minH) / hRange);

                // Gold vertical line from point to label
                goldPaint.setAlpha(markerAlpha);
                canvas.drawLine(x, y, x, y - 30, goldPaint);

                // Draw dot
                Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                dotPaint.setColor(event.isHigh ? COLOR_HIGH : COLOR_LOW);
                dotPaint.setAlpha(markerAlpha);
                canvas.drawCircle(x, y, 5f, dotPaint);

                // White circle outline
                Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                outlinePaint.setStyle(Paint.Style.STROKE);
                outlinePaint.setStrokeWidth(1.5f);
                outlinePaint.setColor(0xFFE0E1DD);
                outlinePaint.setAlpha(markerAlpha);
                canvas.drawCircle(x, y, 5f, outlinePaint);

                // Time label (above the point) - THIS IS THE KEY CHANGE: show time, not height
                String timeLabel = formatEventTime(evtCal);
                Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                timePaint.setTextSize(13f);
                timePaint.setColor(event.isHigh ? COLOR_HIGH : COLOR_LOW);
                timePaint.setAlpha(markerAlpha);
                timePaint.setFakeBoldText(true);
                timePaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(timeLabel, x, y - 32, timePaint);

                // Phase label (مد or جزر) below the point
                highLowPaint.setColor(event.isHigh ? COLOR_HIGH : COLOR_LOW);
                highLowPaint.setAlpha(markerAlpha);
                highLowPaint.setTextAlign(Paint.Align.CENTER);
                String phaseLabel = event.isHigh ? "مد" : "جزر";
                canvas.drawText(phaseLabel, x, y + 20, highLowPaint);
            }
        }

        // Draw current time indicator (gold)
        if (animProgress >= 0.5f) {
            float currentTimeHour = currentHour + currentMinute / 60.0f;
            float cx = padL + (chartW * currentTimeHour / 24.0f);
            currentTimePaint.setAlpha((int)(255 * Math.min(1f, (animProgress - 0.5f) / 0.3f)));
            canvas.drawLine(cx, padT, cx, padT + chartH, currentTimePaint);

            // Current height dot on curve
            double currentHeight = TideCalculator.getTideHeight(
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh")).getTimeInMillis());
            float cy = padT + chartH * (1.0f - ((float) currentHeight - minH) / hRange);
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(COLOR_GOLD);
            canvas.drawCircle(cx, cy, 6f, dotPaint);

            // Current time label at top
            String timeLabel = String.format(Locale.US, "%d:%02d",
                currentHour > 12 ? currentHour - 12 : (currentHour == 0 ? 12 : currentHour),
                currentMinute) + (currentHour >= 12 ? "PM" : "AM");
            Paint timeLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            timeLabelPaint.setTextSize(14f);
            timeLabelPaint.setColor(COLOR_GOLD);
            canvas.drawText(timeLabel, cx - 20, padT - 6, timeLabelPaint);
        }
    }

    private String formatEventTime(Calendar cal) {
        int h = cal.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = cal.get(Calendar.MINUTE);
        String ap = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        return String.format(Locale.US, "%d:%02d%s", h, m, ap);
    }
}