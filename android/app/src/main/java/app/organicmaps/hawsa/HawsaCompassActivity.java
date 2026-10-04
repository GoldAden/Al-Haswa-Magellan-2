package app.organicmaps.hawsa;

import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

public class HawsaCompassActivity extends AppCompatActivity {

    private CompassView compassView;
    private TextView tvQiblaDirection;
    private TextView tvQiblaDegrees;
    private LinearLayout prayerTimesContainer;

    private static final double QIBLA_BEARING = 248.0;

    private static final String[] PRAYER_NAMES_AR = {
        "الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"
    };

    private static final String[] PRAYER_NAMES_EN = {
        "Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"
    };

    private static final int[] PRAYER_COLORS = {
        0xFF4FC3F7,  // Fajr - light blue
        0xFFFFB74D,  // Sunrise - orange
        0xFFFFD54F,  // Dhuhr - yellow
        0xFFFF8A65,  // Asr - deep orange
        0xFFFF5722,  // Maghrib - red-orange
        0xFF7C4DFF   // Isha - purple
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
        setContentView(R.layout.activity_hawsa_compass);

        // Title
        TextView tvTitle = findViewById(R.id.tv_compass_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_compass_title);

        // Compass view
        compassView = findViewById(R.id.compass_view);

        // Qibla direction text
        tvQiblaDirection = findViewById(R.id.tv_qibla_direction);
        tvQiblaDegrees = findViewById(R.id.tv_qibla_degrees);

        updateQiblaInfo();

        // Prayer times container
        prayerTimesContainer = findViewById(R.id.prayer_times_container);
        buildPrayerTimes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (compassView != null) {
            compassView.startSensors();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (compassView != null) {
            compassView.stopSensors();
        }
    }

    private void updateQiblaInfo() {
        if (tvQiblaDirection != null) {
            tvQiblaDirection.setText("اتجاه القبلة: جنوب غرب");
        }
        if (tvQiblaDegrees != null) {
            tvQiblaDegrees.setText(String.format(Locale.US, "%.0f° من الشمال", QIBLA_BEARING));
        }
    }

    private void buildPrayerTimes() {
        if (prayerTimesContainer == null) return;
        prayerTimesContainer.removeAllViews();

        PrayerTimesCalculator.PrayerTimes times = PrayerTimesCalculator.getTodayPrayerTimes();

        // Find next prayer to highlight
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        double currentDecimalHour = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;
        int nextPrayerIdx = -1;
        double[] prayerHours = {times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha};

        for (int i = 0; i < prayerHours.length; i++) {
            if (prayerHours[i] > currentDecimalHour) {
                nextPrayerIdx = i;
                break;
            }
        }
        if (nextPrayerIdx == -1) nextPrayerIdx = 0; // After Isha, next is Fajr

        String[] prayerTimeStrs = {
            times.getFajrStr(),
            times.getSunriseStr(),
            times.getDhuhrStr(),
            times.getAsrStr(),
            times.getMaghribStr(),
            times.getIshaStr()
        };

        for (int i = 0; i < 6; i++) {
            View row = createPrayerRow(i, prayerTimeStrs[i], i == nextPrayerIdx);
            prayerTimesContainer.addView(row);
        }
    }

    private View createPrayerRow(int index, String timeStr, boolean isNext) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 12, 8, 12);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        if (isNext) {
            row.setBackgroundColor(0x204FC3F7); // Highlight next prayer
        }

        // Color indicator dot
        View dot = new View(this);
        dot.setBackgroundColor(PRAYER_COLORS[index]);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(12, 12);
        dotParams.setMargins(0, 0, 12, 0);
        dot.setLayoutParams(dotParams);
        row.addView(dot);

        // Prayer name (Arabic)
        TextView tvNameAr = new TextView(this);
        tvNameAr.setText(PRAYER_NAMES_AR[index]);
        tvNameAr.setTextSize(18);
        tvNameAr.setTextColor(isNext ? 0xFF4FC3F7 : 0xFFE0E1DD);
        tvNameAr.getPaint().setFakeBoldText(isNext);
        tvNameAr.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvNameAr);

        // Prayer name (English transliteration)
        TextView tvNameEn = new TextView(this);
        tvNameEn.setText(PRAYER_NAMES_EN[index]);
        tvNameEn.setTextSize(11);
        tvNameEn.setTextColor(0xFF778DA9);
        row.addView(tvNameEn);

        // Time (AM/PM)
        TextView tvTime = new TextView(this);
        tvTime.setText(timeStr);
        tvTime.setTextSize(18);
        tvTime.setTextColor(isNext ? 0xFF4FC3F7 : 0xFFE0E1DD);
        tvTime.getPaint().setFakeBoldText(true);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.setMargins(20, 0, 0, 0);
        tvTime.setLayoutParams(timeParams);
        row.addView(tvTime);

        // Separator
        if (index < 5) {
            View separator = new View(this);
            separator.setBackgroundColor(0x40415A77);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            sepParams.setMargins(24, 12, 0, 0);
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