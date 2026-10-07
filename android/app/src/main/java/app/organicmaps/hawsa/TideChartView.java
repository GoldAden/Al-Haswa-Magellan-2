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
 * Modern tide chart with maritime styling.
 * ENLARGED tide time labels, height labels, hour axis labels,
 * and now-indicator label for readability.
 */
public class TideChartView extends View {

    private Paint curvePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint labelPaint;
    private Paint markerDotPaint;
    private Paint currentTimePaint;
    private Paint highLowPaint;
    private Paint badgePaint;
    private Paint badgeTextPaint;
    private Paint nowDotPaint;
    private Paint nowLabelPaint;
    private Paint heightLabelPaint;

    private List<TideCalculator.TidePoint> tidePoints;
    private List<TideCalculator.TideEvent> tideEvents;
    private float minH = 0;
    private float maxH = 2;
    private int currentHour;
    private int currentMinute;
    private boolean isRising = true;

    private float animProgress = 0f;
    private boolean animating = false;

    private static final int COLOR_BG = 0xFF0F172A;
    private static final int COLOR_CURVE = 0xFFD4AF37;
    private static final int COLOR_FILL_TOP = 0x40D4AF37;
    private static final int COLOR_FILL_BOT = 0x100F172A;
    private static final int COLOR_GRID = 0x25334155;
    private static final int COLOR_LABEL = 0xFF64748B;
    private static final int COLOR_HIGH = 0xFF10B981;
    private static final int COLOR_LOW = 0xFFEF4444;
    private static final int COLOR_CURRENT = 0xFFD4AF37;
    private static final int COLOR_BADGE_RISING = 0xFF10B981;
    private static final int COLOR_BADGE_FALLING = 0xFFEF4444;

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

        // Height axis labels — ENLARGED to 24f
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(20f);
        labelPaint.setColor(COLOR_LABEL);

        markerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerDotPaint.setStyle(Paint.Style.FILL);

        currentTimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentTimePaint.setStyle(Paint.Style.STROKE);
        currentTimePaint.setStrokeWidth(2f);
        currentTimePaint.setColor(COLOR_CURRENT);
        currentTimePaint.setPathEffect(new DashPathEffect(new float[]{6f, 3f}, 0));

        // High/Low tide TIME labels — ENLARGED to 22f
        highLowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highLowPaint.setTextSize(22f);
        highLowPaint.setFakeBoldText(true);

        // High/Low tide HEIGHT labels — ENLARGED to 18f
        heightLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heightLabelPaint.setTextSize(18f);
        heightLabelPaint.setFakeBoldText(true);

        badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setStyle(Paint.Style.FILL);

        badgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgeTextPaint.setTextSize(12f);
        badgeTextPaint.setFakeBoldText(true);
        badgeTextPaint.setColor(0xFFFFFFFF);

        nowDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nowDotPaint.setStyle(Paint.Style.FILL);
        nowDotPaint.setColor(COLOR_CURRENT);

        // Now indicator time label — ENLARGED to 18f
        nowLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nowLabelPaint.setTextSize(16f);
        nowLabelPaint.setColor(COLOR_CURRENT);
        nowLabelPaint.setFakeBoldText(true);

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

        if (events != null) {
            Calendar now = Calendar.getInstance(TimeZone.getDefault());
            long nowMs = now.getTimeInMillis();
            TideCalculator.TideEvent lastEvent = null;
            for (TideCalculator.TideEvent e : events) {
                if (e.timeMillis <= nowMs) {
                    lastEvent = e;
                }
            }
            if (lastEvent != null) {
                isRising = !lastEvent.isHigh;
            }
        }

        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        currentHour = now.get(Calendar.HOUR_OF_DAY);
        currentMinute = now.get(Calendar.MINUTE);

        startAnimation();
    }

    private void startAnimation() {
        animProgress = 0f;
        animating = true;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                animProgress += 0.04f;
                if (animProgress >= 1f) {
                    animProgress = 1f;
                    animating = false;
                }
                invalidate();
                if (animating) {
                    postDelayed(this, 25);
                }
            }
        }, 25);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float padL = 56f, padR = 18f, padT = 30f, padB = 44f;
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;

        canvas.drawColor(COLOR_BG);

        float hRange = maxH - minH;
        if (hRange <= 0) hRange = 1f;

        // Horizontal grid + height axis labels (ENLARGED)
        int numHLines = 4;
        for (int i = 0; i <= numHLines; i++) {
            float y = padT + chartH * i / numHLines;
            canvas.drawLine(padL, y, padL + chartW, y, gridPaint);
            float heightVal = maxH - (hRange * i / numHLines);
            String label = String.format(Locale.US, "%.1f", heightVal);
            canvas.drawText(label, 4, y + 7, labelPaint);
        }

        // Vertical grid + hour axis labels (ENLARGED to 24f via labelPaint)
        String[] hourLabels = {"0", "3", "6", "9", "12", "15", "18", "21", "24"};
        for (int i = 0; i < hourLabels.length; i++) {
            float x = padL + chartW * i / (hourLabels.length - 1);
            canvas.drawLine(x, padT, x, padT + chartH, gridPaint);
            canvas.drawText(hourLabels[i], x - 8, h - 8, labelPaint);
        }

        if (tidePoints == null || tidePoints.isEmpty()) return;

        // Build tide curve
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
                curvePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        float lastX = padL + (chartW * (float) tidePoints.get(maxIdx).hour / 24.0f);
        fillPath.lineTo(lastX, padT + chartH);
        fillPath.close();

        LinearGradient gradient = new LinearGradient(0, padT, 0, padT + chartH,
                COLOR_FILL_TOP, COLOR_FILL_BOT, Shader.TileMode.CLAMP);
        fillPaint.setShader(gradient);
        fillPaint.setAlpha((int)(200 * animProgress));
        canvas.drawPath(fillPath, fillPaint);

        curvePaint.setAlpha((int)(255 * animProgress));
        canvas.drawPath(curvePath, curvePaint);

        // High/Low tide markers with ENLARGED labels
        if (tideEvents != null && animProgress >= 0.7f) {
            int markerAlpha = (int)(255 * Math.min(1f, (animProgress - 0.7f) / 0.3f));
            for (TideCalculator.TideEvent event : tideEvents) {
                Calendar evtCal = Calendar.getInstance(TimeZone.getDefault());
                evtCal.setTimeInMillis(event.timeMillis);
                float evtHour = evtCal.get(Calendar.HOUR_OF_DAY) + evtCal.get(Calendar.MINUTE) / 60.0f;
                float x = padL + (chartW * evtHour / 24.0f);
                float y = padT + chartH * (1.0f - ((float) event.height - minH) / hRange);

                int color = event.isHigh ? COLOR_HIGH : COLOR_LOW;

                // Glow dot
                markerDotPaint.setColor(color);
                markerDotPaint.setAlpha(markerAlpha);
                canvas.drawCircle(x, y, 9f, markerDotPaint);

                // White center
                Paint whiteCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
                whiteCenter.setStyle(Paint.Style.FILL);
                whiteCenter.setColor(0xFFFFFFFF);
                whiteCenter.setAlpha(markerAlpha);
                canvas.drawCircle(x, y, 4f, whiteCenter);

                // TIME label — ENLARGED highLowPaint 22f
                highLowPaint.setColor(color);
                highLowPaint.setAlpha(markerAlpha);
                String timeLabel = formatEventTime(evtCal);
                float timeWidth = highLowPaint.measureText(timeLabel);
                canvas.drawText(timeLabel, x - timeWidth / 2, y - 22f, highLowPaint);

                // HEIGHT label — ENLARGED heightLabelPaint 18f
                heightLabelPaint.setColor(color);
                heightLabelPaint.setAlpha(markerAlpha);
                String heightStr = String.format(Locale.US, "%.2f\u0645", event.height);
                float hWidth = heightLabelPaint.measureText(heightStr);
                canvas.drawText(heightStr, x - hWidth / 2, y + 26f, heightLabelPaint);
            }
        }

        // Current time indicator
        if (animProgress >= 0.4f) {
            float currentTimeHour = currentHour + currentMinute / 60.0f;
            float cx = padL + (chartW * currentTimeHour / 24.0f);
            currentTimePaint.setAlpha((int)(255 * Math.min(1f, (animProgress - 0.4f) / 0.3f)));
            canvas.drawLine(cx, padT, cx, padT + chartH, currentTimePaint);

            double currentHeight = TideCalculator.getCurrentTideHeight();
            float cy = padT + chartH * (1.0f - ((float) currentHeight - minH) / hRange);

            // Glow
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setColor(COLOR_CURRENT);
            glow.setAlpha(60);
            canvas.drawCircle(cx, cy, 14f, glow);

            nowDotPaint.setAlpha(255);
            canvas.drawCircle(cx, cy, 7f, nowDotPaint);

            // Now label — ENLARGED nowLabelPaint 16f
            String nowLabel = String.format(Locale.US, "%d:%02d",
                currentHour > 12 ? currentHour - 12 : (currentHour == 0 ? 12 : currentHour),
                currentMinute) + (currentHour >= 12 ? "\u0645" : "\u0635");
            nowLabelPaint.setAlpha(220);
            canvas.drawText(nowLabel, cx - 24, padT - 6, nowLabelPaint);
        }

        // Rising/Falling badge
        if (animProgress >= 0.9f) {
            int badgeAlpha = (int)(255 * Math.min(1f, (animProgress - 0.9f) / 0.1f));
            String badgeText = isRising ? "\u0645\u062F \u2191" : "\u062C\u0632\u0631 \u2193";
            int badgeColor = isRising ? COLOR_BADGE_RISING : COLOR_BADGE_FALLING;

            float badgeX = w - padR - 80f;
            float badgeY = padT + 6f;
            float badgeW = 76f;
            float badgeH = 24f;

            badgePaint.setColor(badgeColor);
            badgePaint.setAlpha((int)(badgeAlpha * 0.85f));
            RectF badgeRect = new RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH);
            canvas.drawRoundRect(badgeRect, 12f, 12f, badgePaint);

            badgeTextPaint.setAlpha(badgeAlpha);
            float textWidth = badgeTextPaint.measureText(badgeText);
            canvas.drawText(badgeText, badgeX + (badgeW - textWidth) / 2, badgeY + 17f, badgeTextPaint);
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