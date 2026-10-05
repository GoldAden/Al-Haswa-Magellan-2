package app.organicmaps.hawsa;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
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
    private LinearLayout weeklyMoonContainer;

    private static final String[] ARABIC_DAYS = {
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
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
        setContentView(R.layout.activity_hawsa_moon);

        // Title
        TextView tvTitle = findViewById(R.id.tv_moon_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_moon_title);

        // Moon info section
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        long timeMillis = now.getTimeInMillis();

        // Moon phase view
        moonPhaseView = findViewById(R.id.moon_phase_view);
        double age = MoonCalculator.getLunarAge(timeMillis);
        double illum = MoonCalculator.getIllumination(timeMillis);
        if (moonPhaseView != null) {
            moonPhaseView.setPhase(illum, age);
        }

        // Phase name
        TextView tvPhase = findViewById(R.id.tv_moon_phase_name);
        if (tvPhase != null) {
            tvPhase.setText(MoonCalculator.getPhaseNameArabic(age));
        }

        // Illumination
        TextView tvIllumination = findViewById(R.id.tv_moon_illumination);
        if (tvIllumination != null) {
            tvIllumination.setText(String.format(Locale.US, "الإضاءة: %.1f%%", illum * 100));
        }

        // Moon age
        TextView tvAge = findViewById(R.id.tv_moon_age);
        if (tvAge != null) {
            tvAge.setText(String.format(Locale.US, "عمر القمر: %.1f يوم", age));
        }

        // Moon rise
        TextView tvRise = findViewById(R.id.tv_moon_rise);
        if (tvRise != null) {
            tvRise.setText(String.format(Locale.US, "الطلوع: %s", MoonCalculator.getMoonriseTime(timeMillis)));
        }

        // Moon set
        TextView tvSet = findViewById(R.id.tv_moon_set);
        if (tvSet != null) {
            tvSet.setText(String.format(Locale.US, "الغروب: %s", MoonCalculator.getMoonsetTime(timeMillis)));
        }

        // ===== Hijri Date Section =====
        LinearLayout hijriContainer = findViewById(R.id.hijri_date_container);
        if (hijriContainer != null) {
            buildHijriSection(hijriContainer, now);
        }

        // ===== Weekly Moon Section =====
        weeklyMoonContainer = findViewById(R.id.weekly_moon_container);
        if (weeklyMoonContainer != null) {
            buildWeeklySection();
        }

        // Designer credit
        TextView tvCredit = findViewById(R.id.tv_designer_credit);
        if (tvCredit != null) {
            tvCredit.setText("تم تصميم هذا التطبيق من قبل أصيل صادق");
        }
    }

    private void buildHijriSection(LinearLayout container, Calendar date) {
        container.removeAllViews();

        // Calculate Hijri date using HijriDateCalculator
        HijriDateCalculator.HijriDate hijriDate = HijriDateCalculator.calculateHijri(date);

        // Hijri date main text
        TextView tvHijriDate = new TextView(this);
        tvHijriDate.setText(HijriDateCalculator.formatHijriArabic(hijriDate));
        tvHijriDate.setTextSize(26);
        tvHijriDate.setTextColor(0xFFE0E1DD); // Light text
        tvHijriDate.getPaint().setFakeBoldText(true);
        tvHijriDate.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tvHijriDate.setLayoutParams(dateParams);
        container.addView(tvHijriDate);

        // Hijri month name (subtext)
        TextView tvHijriMonth = new TextView(this);
        tvHijriMonth.setText(String.format(Locale.US, "شهر %s - العام %d هـ", hijriDate.monthName, hijriDate.year));
        tvHijriMonth.setTextSize(14);
        tvHijriMonth.setTextColor(0xFF778DA9); // Muted text
        tvHijriMonth.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams monthParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        monthParams.setMargins(0, 8, 0, 0);
        tvHijriMonth.setLayoutParams(monthParams);
        container.addView(tvHijriMonth);

        // Lunar month progress indicator
        double moonAge = MoonCalculator.getLunarAge(date.getTimeInMillis());
        double lunarMonthProgress = moonAge / 29.53058867 * 100;

        TextView tvLunarProgress = new TextView(this);
        tvLunarProgress.setText(String.format(Locale.US, "تقدم الشهر القمري: %.0f%%", lunarMonthProgress));
        tvLunarProgress.setTextSize(13);
        tvLunarProgress.setTextColor(0xFF778DA9);
        tvLunarProgress.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.setMargins(0, 10, 0, 0);
        tvLunarProgress.setLayoutParams(progressParams);
        container.addView(tvLunarProgress);
    }

    private void buildWeeklySection() {
        if (weeklyMoonContainer == null) return;
        weeklyMoonContainer.removeAllViews();

        List<MoonCalculator.MoonDayInfo> weekly = MoonCalculator.getWeeklyMoonInfo();

        for (int i = 0; i < weekly.size(); i++) {
            MoonCalculator.MoonDayInfo day = weekly.get(i);
            View row = createWeeklyRow(day, i);
            weeklyMoonContainer.addView(row);
        }
    }

    private View createWeeklyRow(MoonCalculator.MoonDayInfo day, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Day name + date
        int dow = day.date.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        String dayName = ARABIC_DAYS[dow];

        LinearLayout leftInfo = new LinearLayout(this);
        leftInfo.setOrientation(LinearLayout.VERTICAL);
        leftInfo.setPadding(0, 0, 24, 0);

        TextView tvDay = new TextView(this);
        tvDay.setText(dayName);
        tvDay.setTextSize(16);
        tvDay.setTextColor(0xFFE0E1DD);
        tvDay.getPaint().setFakeBoldText(true);
        leftInfo.addView(tvDay);

        TextView tvDetails = new TextView(this);
        tvDetails.setText(String.format(Locale.US, "%s · %.0f%%", day.phaseName, day.illumination * 100));
        tvDetails.setTextSize(12);
        tvDetails.setTextColor(0xFF778DA9);
        leftInfo.addView(tvDetails);

        row.addView(leftInfo);

        // Rise/Set times
        LinearLayout rightInfo = new LinearLayout(this);
        rightInfo.setOrientation(LinearLayout.VERTICAL);
        rightInfo.setGravity(Gravity.END);

        TextView tvRise = new TextView(this);
        tvRise.setText(String.format(Locale.US, "طلوع: %s", day.moonrise));
        tvRise.setTextSize(13);
        tvRise.setTextColor(0xFFCE93D8); // Moon accent
        rightInfo.addView(tvRise);

        TextView tvSet = new TextView(this);
        tvSet.setText(String.format(Locale.US, "غروب: %s", day.moonset));
        tvSet.setTextSize(13);
        tvSet.setTextColor(0xFF778DA9);
        rightInfo.addView(tvSet);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        rightInfo.setLayoutParams(rightParams);
        row.addView(rightInfo);

        // Separator
        if (index < 6) {
            View separator = new View(this);
            separator.setBackgroundColor(0xFF415A77);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            sepParams.setMargins(0, 12, 0, 0);
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
}
