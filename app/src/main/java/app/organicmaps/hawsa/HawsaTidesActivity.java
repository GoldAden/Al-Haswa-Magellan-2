package app.organicmaps.hawsa;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

public class HawsaTidesActivity extends AppCompatActivity {

    private TideChartView tideChart;
    private LinearLayout weeklyContainer;
    private LinearLayout dayButtonsContainer;
    private Calendar selectedDay;

    private static final String[] ARABIC_DAYS = {
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    };

    private static final String[] ARABIC_MONTHS = {
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.hawsa_status_bar_color));
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_tides);

        TextView tvTitle = findViewById(R.id.tv_tides_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_tides_title);

        TextView tvLocation = findViewById(R.id.tv_tides_location);
        if (tvLocation != null) tvLocation.setText("الحسوة، البحر الأحمر");

        tideChart = findViewById(R.id.tide_chart);

        dayButtonsContainer = findViewById(R.id.day_button_container);

        weeklyContainer = findViewById(R.id.weekly_tides_container);

        selectedDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        updateChart();
        createDayButtons();
        buildWeeklySummary();
    }

    private void createDayButtons() {
        if (dayButtonsContainer == null) return;
        dayButtonsContainer.removeAllViews();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));

        int todayDow = cal.get(Calendar.DAY_OF_WEEK) - 1;

        for (int i = 0; i < 7; i++) {
            TextView btn = new TextView(this);
            String dayName = ARABIC_DAYS[(todayDow + i) % 7];
            btn.setText(dayName);
            btn.setTextSize(14);
            btn.setPadding(24, 12, 24, 12);
            btn.setAllCaps(false);

            if (i == 0) {
                btn.setTextColor(0xFFD4AF37);
                btn.setBackgroundResource(R.drawable.hawsa_day_button_selected);
            } else {
                btn.setTextColor(0xFF778DA9);
                btn.setBackgroundResource(R.drawable.hawsa_day_button);
            }

            final int dayOffset = i;
            btn.setOnClickListener(v -> {
                selectedDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
                selectedDay.add(Calendar.DAY_OF_MONTH, dayOffset);
                updateChart();
                updateButtonSelection(dayOffset);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dayButtonsContainer.addView(btn, params);
        }
    }

    private void updateButtonSelection(int selectedOffset) {
        for (int i = 0; i < dayButtonsContainer.getChildCount(); i++) {
            TextView btn = (TextView) dayButtonsContainer.getChildAt(i);
            if (i == selectedOffset) {
                btn.setTextColor(0xFFD4AF37);
                btn.setBackgroundResource(R.drawable.hawsa_day_button_selected);
            } else {
                btn.setTextColor(0xFF778DA9);
                btn.setBackgroundResource(R.drawable.hawsa_day_button);
            }
        }
    }

    private void updateChart() {
        if (tideChart == null) return;
        Calendar day = (Calendar) selectedDay.clone();
        List<TideCalculator.TidePoint> points = TideCalculator.getDayTidePoints(day);
        List<TideCalculator.TideEvent> events = TideCalculator.getDayTideEvents(day);
        tideChart.setDayData(day, points, events);
    }

    private void buildWeeklySummary() {
        if (weeklyContainer == null) return;
        weeklyContainer.removeAllViews();

        List<TideCalculator.DayTideSummary> weekly = TideCalculator.getWeeklySummary();

        for (int i = 0; i < weekly.size(); i++) {
            TideCalculator.DayTideSummary day = weekly.get(i);
            View row = createDayRow(day, i);
            weeklyContainer.addView(row);
        }
    }

    private View createDayRow(TideCalculator.DayTideSummary day, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 10, 0, 10);

        // Day header: Day name + date
        int dow = day.date.get(Calendar.DAY_OF_WEEK) - 1;
        String dayName = ARABIC_DAYS[dow];
        String dateStr = day.date.get(Calendar.DAY_OF_MONTH) + " " + ARABIC_MONTHS[day.date.get(Calendar.MONTH)];

        TextView tvDayHeader = new TextView(this);
        tvDayHeader.setText(dayName + "  " + dateStr);
        tvDayHeader.setTextSize(16);
        tvDayHeader.setTextColor(0xFFE0E1DD);
        tvDayHeader.getPaint().setFakeBoldText(true);
        row.addView(tvDayHeader);

        // Build 4 event lines in chronological order
        java.util.ArrayList<TideCalculator.TideEvent> sortedEvents = new java.util.ArrayList<>();
        if (day.firstLow != null) sortedEvents.add(day.firstLow);
        if (day.firstHigh != null) sortedEvents.add(day.firstHigh);
        if (day.secondLow != null) sortedEvents.add(day.secondLow);
        if (day.secondHigh != null) sortedEvents.add(day.secondHigh);

        // Sort by time
        java.util.Collections.sort(sortedEvents, (a, b) -> Long.compare(a.timeMillis, b.timeMillis));

        for (int e = 0; e < sortedEvents.size(); e++) {
            TideCalculator.TideEvent event = sortedEvents.get(e);
            String timeStr = formatEventTime(event);

            TextView tvEvent = new TextView(this);
            if (event.isHigh) {
                // آخر المد وبداية الجزر
                tvEvent.setText("آخر المد وبداية الجزر  " + timeStr + "  نسبة ارتفاع الماء " + String.format(Locale.US, "%.1f", event.height) + " متر");
            } else {
                // آخر الجزر وبداية المد
                tvEvent.setText("آخر الجزر وبداية المد  " + timeStr + "  نسبة ارتفاع الماء " + String.format(Locale.US, "%.1f", event.height) + " متر");
            }
            tvEvent.setTextSize(13);
            tvEvent.setTextColor(event.isHigh ? 0xFF4CAF50 : 0xFFFF5722);
            tvEvent.setPadding(8, 4, 8, 4);
            row.addView(tvEvent);
        }

        // Separator line
        if (index < 6) {
            View separator = new View(this);
            separator.setBackgroundColor(0xFF415A77);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            sepParams.setMargins(0, 10, 0, 0);
            separator.setLayoutParams(sepParams);
            row.addView(separator);
        }

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);
        return row;
    }

    private String formatEventTime(TideCalculator.TideEvent event) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        cal.setTimeInMillis(event.timeMillis);
        return MoonCalculator.formatTimeAmPm(cal);
    }
}