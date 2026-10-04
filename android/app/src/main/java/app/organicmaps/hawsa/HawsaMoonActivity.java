package app.organicmaps.hawsa;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

public class HawsaMoonActivity extends AppCompatActivity {

    private MoonPhaseView moonPhaseView;
    private TextView tvPhaseName;
    private TextView tvIllumination;
    private TextView tvRiseSet;
    private LinearLayout weeklyContainer;

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
        setContentView(R.layout.activity_hawsa_moon);

        // Title
        TextView tvTitle = findViewById(R.id.tv_moon_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_moon_title);

        // Moon phase visual
        moonPhaseView = findViewById(R.id.moon_phase_view);

        // Phase name
        tvPhaseName = findViewById(R.id.tv_moon_phase_name);

        // Illumination
        tvIllumination = findViewById(R.id.tv_moon_illumination);

        // Rise/Set times
        tvRiseSet = findViewById(R.id.tv_moon_rise_set);

        // Weekly container
        weeklyContainer = findViewById(R.id.weekly_moon_container);

        // Load today's moon data
        updateMoonInfo();
        buildWeeklySummary();
    }

    private void updateMoonInfo() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        long timeMillis = now.getTimeInMillis();

        double age = MoonCalculator.getLunarAge(timeMillis);
        double illumination = MoonCalculator.getIllumination(timeMillis);
        String phaseName = MoonCalculator.getPhaseNameArabic(age);

        if (moonPhaseView != null) {
            moonPhaseView.setPhase(illumination, age);
        }

        if (tvPhaseName != null) {
            tvPhaseName.setText(phaseName);
        }

        if (tvIllumination != null) {
            String illumStr = String.format(Locale.US, "الإضاءة: %.0f%%", illumination * 100);
            tvIllumination.setText(illumStr);
        }

        if (tvRiseSet != null) {
            String moonrise = MoonCalculator.getMoonriseTime(timeMillis);
            String moonset = MoonCalculator.getMoonsetTime(timeMillis);
            tvRiseSet.setText("القمر ⬆ " + moonrise + "  |  ⬇ " + moonset);
        }
    }

    private void buildWeeklySummary() {
        if (weeklyContainer == null) return;
        weeklyContainer.removeAllViews();

        List<MoonCalculator.MoonDayInfo> weekly = MoonCalculator.getWeeklyMoonInfo();

        for (int i = 0; i < weekly.size(); i++) {
            MoonCalculator.MoonDayInfo day = weekly.get(i);
            View row = createDayRow(day, i);
            weeklyContainer.addView(row);
        }
    }

    private View createDayRow(MoonCalculator.MoonDayInfo day, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Left: Day name + date
        LinearLayout leftInfo = new LinearLayout(this);
        leftInfo.setOrientation(LinearLayout.VERTICAL);
        leftInfo.setPadding(0, 0, 24, 0);

        int dow = day.date.get(Calendar.DAY_OF_WEEK) - 1;
        String dayName = ARABIC_DAYS[dow];
        String dateStr = day.date.get(Calendar.DAY_OF_MONTH) + " " + ARABIC_MONTHS[day.date.get(Calendar.MONTH)];

        TextView tvDay = new TextView(this);
        tvDay.setText(dayName);
        tvDay.setTextSize(16);
        tvDay.setTextColor(0xFFE0E1DD);
        tvDay.setFakeBoldText(true);
        leftInfo.addView(tvDay);

        TextView tvDate = new TextView(this);
        tvDate.setText(dateStr);
        tvDate.setTextSize(12);
        tvDate.setTextColor(0xFF778DA9);
        leftInfo.addView(tvDate);

        row.addView(leftInfo);

        // Middle: Phase emoji
        TextView tvEmoji = new TextView(this);
        String[] moonEmojis = {"🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘"};
        tvEmoji.setText(moonEmojis[day.phaseIndex]);
        tvEmoji.setTextSize(24);
        row.addView(tvEmoji);

        // Right: Phase name + illumination
        LinearLayout rightInfo = new LinearLayout(this);
        rightInfo.setOrientation(LinearLayout.VERTICAL);
        rightInfo.setPadding(16, 0, 0, 0);

        TextView tvPhase = new TextView(this);
        tvPhase.setText(day.phaseName);
        tvPhase.setTextSize(14);
        tvPhase.setTextColor(0xFFE0E1DD);
        rightInfo.addView(tvPhase);

        TextView tvIllum = new TextView(this);
        tvIllum.setText(String.format(Locale.US, "%.0f%%", day.illumination * 100));
        tvIllum.setTextSize(12);
        tvIllum.setTextColor(0xFF778DA9);
        rightInfo.addView(tvIllum);

        TextView tvTimes = new TextView(this);
        tvTimes.setText("⬆" + day.moonrise + " ⬇" + day.moonset);
        tvTimes.setTextSize(11);
        tvTimes.setTextColor(0xFF778DA9);
        rightInfo.addView(tvTimes);

        row.addView(rightInfo);

        // Separator
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
}