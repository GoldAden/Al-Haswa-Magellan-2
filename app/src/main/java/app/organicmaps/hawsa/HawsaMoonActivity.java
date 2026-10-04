package app.organicmaps.hawsa;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

public class HawsaMoonActivity extends AppCompatActivity {

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

        // Phase name
        TextView tvPhase = findViewById(R.id.tv_moon_phase);
        if (tvPhase != null) {
            tvPhase.setText(MoonCalculator.getPhaseNameAr(now));
        }

        // Illumination
        TextView tvIllumination = findViewById(R.id.tv_moon_illumination);
        if (tvIllumination != null) {
            double illum = MoonCalculator.getIllumination(now);
            tvIllumination.setText(String.format(Locale.US, "%.1f%%", illum));
        }

        // Moon age
        TextView tvAge = findViewById(R.id.tv_moon_age);
        if (tvAge != null) {
            double age = MoonCalculator.getMoonAge(now);
            tvAge.setText(String.format(Locale.US, "%.1f يوم", age));
        }

        // Rise / Set times
        String[] riseSet = MoonCalculator.getRiseSetTimes(now);
        TextView tvRise = findViewById(R.id.tv_moon_rise);
        if (tvRise != null) tvRise.setText(riseSet[0]);
        TextView tvSet = findViewById(R.id.tv_moon_set);
        if (tvSet != null) tvSet.setText(riseSet[1]);

        // ===== Hijri Date Section =====
        LinearLayout hijriContainer = findViewById(R.id.hijri_date_container);
        if (hijriContainer != null) {
            buildHijriSection(hijriContainer, now);
        } else {
            // Fallback: try to add to existing layout
            LinearLayout rootContainer = findViewById(R.id.moon_info_container);
            if (rootContainer != null) {
                buildHijriSection(rootContainer, now);
            }
        }

        // Designer credit
        TextView tvCredit = findViewById(R.id.tv_designer_credit);
        if (tvCredit != null) {
            tvCredit.setText("تم تصميم هذا التطبيق من قبل أصيل صادق");
        }
    }

    private void buildHijriSection(LinearLayout container, Calendar date) {
        // Section separator
        View separator = new View(this);
        separator.setBackgroundColor(0x40D4AF37); // Gold separator
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2
        );
        sepParams.setMargins(0, 24, 0, 24);
        separator.setLayoutParams(sepParams);
        container.addView(separator);

        // Section title: "التاريخ الهجري"
        TextView tvHijriTitle = new TextView(this);
        tvHijriTitle.setText("التاريخ الهجري");
        tvHijriTitle.setTextSize(20);
        tvHijriTitle.setTextColor(0xFFD4AF37); // Gold
        tvHijriTitle.getPaint().setFakeBoldText(true);
        tvHijriTitle.setTextDirection(View.TEXT_DIRECTION_RTL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(16, 0, 16, 8);
        tvHijriTitle.setLayoutParams(titleParams);
        container.addView(tvHijriTitle);

        // Hijri date card
        LinearLayout hijriCard = new LinearLayout(this);
        hijriCard.setOrientation(LinearLayout.VERTICAL);
        hijriCard.setPadding(24, 20, 24, 20);
        hijriCard.setBackgroundColor(0xFF142e50); // Card background
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 0, 16, 0);
        hijriCard.setLayoutParams(cardParams);

        // Get Hijri date
        HijriDateCalculator.HijriDate hijriDate = MoonCalculator.getHijriDate(date);

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
        hijriCard.addView(tvHijriDate);

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
        hijriCard.addView(tvHijriMonth);

        container.addView(hijriCard);

        // Lunar month progress indicator
        double moonAge = MoonCalculator.getMoonAge(date);
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
        progressParams.setMargins(20, 10, 20, 0);
        tvLunarProgress.setLayoutParams(progressParams);
        container.addView(tvLunarProgress);
    }
}