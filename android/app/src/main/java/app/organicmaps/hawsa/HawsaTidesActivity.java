package app.organicmaps.hawsa;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.appcompat.app.AppCompatActivity;

import app.organicmaps.R;

/**
 * Redesigned Tides activity for Aden station only.
 * No location permission required — Aden data is hardcoded from TICON-4.
 * Features: current tide status, modern day selector pills, and weekly summary.
 */
public class HawsaTidesActivity extends AppCompatActivity {

    private TideChartView tideChartView;
    private LinearLayout dayButtonContainer;
    private LinearLayout weeklyTidesContainer;
    private TextView tvTidesTitle;
    private TextView tvTidesLocation;
    private TextView tvCurrentStatus;
    private ImageView btnBack;

    private int selectedDayOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark status bar matching app theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0D1B2A);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        }

        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_tides);

        btnBack = findViewById(R.id.btn_tides_back);
        tvTidesTitle = findViewById(R.id.tv_tides_title);
        tvTidesLocation = findViewById(R.id.tv_tides_location);
        tvCurrentStatus = findViewById(R.id.tv_current_status);
        tideChartView = findViewById(R.id.tide_chart);
        dayButtonContainer = findViewById(R.id.day_button_container);
        weeklyTidesContainer = findViewById(R.id.weekly_tides_container);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvTidesTitle != null) tvTidesTitle.setText("\u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631");

        // Fixed Aden station — no location permission needed
        updateStationDisplay();

        // Build day buttons and tide data
        buildDayPills();
        refreshTideData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No location updates needed — Aden is fixed
        updateCurrentStatus();
    }

    private void updateStationDisplay() {
        if (tvTidesLocation != null) {
            String stationName = TideCalculator.getStationNameAr();
            String source = TideCalculator.getStationSourceAr();
            tvTidesLocation.setText("\u0645\u062D\u0637\u0629 " + stationName + " \u2014 " + source);
        }
        updateCurrentStatus();
    }

    private void updateCurrentStatus() {
        if (tvCurrentStatus == null) return;
        List<TideCalculator.TideEvent> todayEvents = TideCalculator.getDayTideEvents(Calendar.getInstance());
        if (todayEvents != null && !todayEvents.isEmpty()) {
            long now = System.currentTimeMillis();
            TideCalculator.TideEvent lastEvent = null;
            for (TideCalculator.TideEvent e : todayEvents) {
                if (e.timeMillis <= now) {
                    lastEvent = e;
                } else {
                    break;
                }
            }
            if (lastEvent != null) {
                boolean isRising = !lastEvent.isHigh;
                String statusText = isRising ? "\u0645\u062F \u0635\u0627\u0639\u062F \u2191" : "\u062C\u0632\u0631 \u0645\u0627\u062D\u0628 \u2193";
                tvCurrentStatus.setText(statusText);
                tvCurrentStatus.setTextColor(isRising ? 0xFF10B981 : 0xFFEF4444);
            } else {
                tvCurrentStatus.setText("");
            }
        }
    }

    /**
     * Modern pill-style day selector buttons
     */
    private void buildDayPills() {
        if (dayButtonContainer == null) return;
        dayButtonContainer.removeAllViews();

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            TextView pill = new TextView(this);

            if (i == 0) {
                pill.setText("\u0627\u0644\u064A\u0648\u0645");
            } else {
                Calendar offset = (Calendar) cal.clone();
                offset.add(Calendar.DAY_OF_MONTH, i);
                String[] arabicDays = {"\u0627\u0644\u0623\u062D\u062F", "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", "\u0627\u0644\u062E\u0645\u064A\u0633", "\u0627\u0644\u062C\u0645\u0639\u0629", "\u0627\u0644\u0633\u0628\u062A"};
                int dayOfWeek = offset.get(Calendar.DAY_OF_WEEK) - 1;
                pill.setText(arabicDays[dayOfWeek]);
            }

            pill.setTextSize(13);
            pill.setGravity(Gravity.CENTER);
            pill.setAllCaps(false);
            pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            boolean selected = (i == selectedDayOffset);
            pill.setTextColor(selected ? 0xFF0A0E1A : 0xFF64748B);

            int pillPaddingH = 20;
            int pillPaddingV = 10;
            pill.setPadding(pillPaddingH, pillPaddingV, pillPaddingH, pillPaddingV);

            android.graphics.drawable.GradientDrawable pillBg = new android.graphics.drawable.GradientDrawable();
            pillBg.setCornerRadius(20f);
            pillBg.setColor(selected ? 0xFFD4AF37 : 0xFF1E293B);
            pillBg.setStroke(selected ? 0 : 1, selected ? 0 : 0xFF334155);
            pill.setBackground(pillBg);

            final int dayOffset = i;
            pill.setOnClickListener(v -> {
                selectedDayOffset = dayOffset;
                buildDayPills();
                refreshTideData();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(6, 0, 6, 0);
            pill.setLayoutParams(params);
            dayButtonContainer.addView(pill);
        }
    }

    private void refreshTideData() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.add(Calendar.DAY_OF_MONTH, selectedDayOffset);

        List<TideCalculator.TidePoint> points = TideCalculator.getDayTidePoints(cal);
        List<TideCalculator.TideEvent> events = TideCalculator.getDayTideEvents(cal);

        if (tideChartView != null) {
            tideChartView.setDayData(cal, points, events);
        }

        updateWeeklySummaryText();
    }

    private void updateWeeklySummaryText() {
        if (weeklyTidesContainer == null) return;

        List<TideCalculator.DayTideSummary> summary = TideCalculator.getWeeklySummary();

        String[] arabicDays = {"\u0627\u0644\u0623\u062D\u062F", "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", "\u0627\u0644\u062E\u0645\u064A\u0633", "\u0627\u0644\u062C\u0645\u0639\u0629", "\u0627\u0644\u0633\u0628\u062A"};
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < summary.size(); i++) {
            TideCalculator.DayTideSummary day = summary.get(i);
            int dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK) - 1;

            sb.append(arabicDays[dayOfWeek]).append(" ").append(dateFmt.format(day.date.getTime()));
            sb.append("\n");

            if (day.firstHigh != null) {
                sb.append("  \u0645\u062F \u0639\u0627\u0644\u064A: ").append(formatEventTime(day.firstHigh));
                sb.append(" (").append(String.format(Locale.US, "%.1f\u0645", day.firstHigh.height)).append(")");
                if (day.secondHigh != null) {
                    sb.append(" | ").append(formatEventTime(day.secondHigh));
                    sb.append(" (").append(String.format(Locale.US, "%.1f\u0645", day.secondHigh.height)).append(")");
                }
                sb.append("\n");
            }

            if (day.firstLow != null) {
                sb.append("  \u062C\u0632\u0631 \u0645\u0646\u062E\u0641\u0636: ").append(formatEventTime(day.firstLow));
                sb.append(" (").append(String.format(Locale.US, "%.1f\u0645", day.firstLow.height)).append(")");
                if (day.secondLow != null) {
                    sb.append(" | ").append(formatEventTime(day.secondLow));
                    sb.append(" (").append(String.format(Locale.US, "%.1f\u0645", day.secondLow.height)).append(")");
                }
                sb.append("\n");
            }

            if (day.maxHeight > Double.MIN_VALUE) {
                sb.append("  \u2191").append(String.format(Locale.US, "%.1f", day.maxHeight)).append("\u0645 ");
            }
            if (day.minHeight < Double.MAX_VALUE) {
                sb.append("\u2193").append(String.format(Locale.US, "%.1f", day.minHeight)).append("\u0645");
            }
            sb.append("\n\n");
        }

        TextView summaryText = null;
        if (weeklyTidesContainer.getChildCount() > 0) {
            summaryText = (TextView) weeklyTidesContainer.getChildAt(0);
        }
        if (summaryText == null) {
            summaryText = new TextView(this);
            summaryText.setTextSize(12);
            summaryText.setTextColor(0xFFE0E1DD);
            weeklyTidesContainer.addView(summaryText);
        }
        summaryText.setText(sb.toString().trim());
    }

    private String formatEventTime(TideCalculator.TideEvent event) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        cal.setTimeInMillis(event.timeMillis);
        int h = cal.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = cal.get(Calendar.MINUTE);
        String ap = cal.get(Calendar.AM_PM) == Calendar.AM ? "\u0635" : "\u0645";
        return String.format(Locale.US, "%d:%02d%s", h, m, ap);
    }
}
