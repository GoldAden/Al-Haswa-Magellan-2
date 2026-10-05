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

        // Window setup
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

        // Title
        TextView tvTitle = findViewById(R.id.tv_tides_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_tides_title);

        // Location subtitle - hidden
        TextView tvLocation = findViewById(R.id.tv_tides_location);
        if (tvLocation != null) tvLocation.setVisibility(View.GONE);

        // Tide chart
        tideChart = findViewById(R.id.tide_chart);

        // Day buttons
        dayButtonsContainer = findViewById(R.id.day_button_container);
        if (dayButtonsContainer == null) {
            dayButtonsContainer = new LinearLayout(this);
            dayButtonsContainer.setOrientation(LinearLayout.HORIZONTAL);
            dayButtonsContainer.setPadding(0, 16, 0, 0);
            LinearLayout chartParent = (LinearLayout) tideChart.getParent().getParent();
            chartParent.addView(dayButtonsContainer);
        }

        // Weekly container
        weeklyContainer = findViewById(R.id.weekly_tides_container);
        if (weeklyContainer == null) {
            weeklyContainer = new LinearLayout(this);
            weeklyContainer.setOrientation(LinearLayout.VERTICAL);
            ScrollView parent = (ScrollView) tideChart.getParent().getParent().getParent().getParent();
            LinearLayout root = (LinearLayout) parent.getChildAt(0);
            root.addView(weeklyContainer);
        }

        // Initialize with today
        selectedDay = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        updateChart();
        createDayButtons();
        buildWeeklySummary();
    }

    private void createDayButtons() {
        dayButtonsContainer.removeAllViews();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));

        int todayDow = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun

        for (int i = 0; i < 7; i++) {
            TextView btn = new TextView(this);
            String dayName = ARABIC_DAYS[(todayDow + i) % 7];
            btn.setText(dayName);
            btn.setTextSize(14);
            btn.setPadding(24, 12, 24, 12);
            btn.setAllCaps(false);

            if (i == 0) {
                btn.setTextColor(0xFF4FC3F7); // Selected color
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
                btn.setTextColor(0xFF4FC3F7);
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
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Day name
        int dow = day.date.get(Calendar.DAY_OF_WEEK) - 1;
        String dayName = ARABIC_DAYS[dow];
        String dateStr = day.date.get(Calendar.DAY_OF_MONTH) + " " + ARABIC_MONTHS[day.date.get(Calendar.MONTH)];

        // Left side: Day name + date
        LinearLayout leftInfo = new LinearLayout(this);
        leftInfo.setOrientation(LinearLayout.VERTICAL);
        leftInfo.setPadding(0, 0, 24, 0);

        TextView tvDay = new TextView(this);
        tvDay.setText(dayName);
        tvDay.setTextSize(16);
        tvDay.setTextColor(0xFFE0E1DD);
        tvDay.getPaint().setFakeBoldText(true);
        leftInfo.addView(tvDay);

        TextView tvDate = new TextView(this);
        tvDate.setText(dateStr);
        tvDate.setTextSize(12);
        tvDate.setTextColor(0xFF778DA9);
        leftInfo.addView(tvDate);

        row.addView(leftInfo);

        // Right side: High and Low times
        LinearLayout rightInfo = new LinearLayout(this);
        rightInfo.setOrientation(LinearLayout.VERTICAL);

        // High tides
        if (day.firstHigh != null) {
            TextView tvH1 = new TextView(this);
            tvH1.setText("مد " + formatEventTime(day.firstHigh) + "  " + String.format(Locale.US, "%.1fم", day.firstHigh.height));
            tvH1.setTextSize(13);
            tvH1.setTextColor(0xFF4CAF50);
            rightInfo.addView(tvH1);
        }
        if (day.secondHigh != null) {
            TextView tvH2 = new TextView(this);
            tvH2.setText("مد " + formatEventTime(day.secondHigh) + "  " + String.format(Locale.US, "%.1fم", day.secondHigh.height));
            tvH2.setTextSize(13);
            tvH2.setTextColor(0xFF4CAF50);
            rightInfo.addView(tvH2);
        }

        // Low tides
        if (day.firstLow != null) {
            TextView tvL1 = new TextView(this);
            tvL1.setText("جزر " + formatEventTime(day.firstLow) + "  " + String.format(Locale.US, "%.1fم", day.firstLow.height));
            tvL1.setTextSize(13);
            tvL1.setTextColor(0xFFFF5722);
            rightInfo.addView(tvL1);
        }
        if (day.secondLow != null) {
            TextView tvL2 = new TextView(this);
            tvL2.setText("جزر " + formatEventTime(day.secondLow) + "  " + String.format(Locale.US, "%.1fم", day.secondLow.height));
            tvL2.setTextSize(13);
            tvL2.setTextColor(0xFFFF5722);
            rightInfo.addView(tvL2);
        }

        row.addView(rightInfo);

        // Separator line
        if (index < 6) {
            View separator = new View(this);
            separator.setBackgroundColor(0xFF415A77);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            sepParams.setMargins(0, 12, 0, 0);
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