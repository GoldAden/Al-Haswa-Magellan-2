package app.organicmaps.hawsa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

/**
 * Fullscreen Redmi-style compass activity with:
 * - Modern dark theme compass with gold accents
 * - Qibla direction indicator with Kaaba icon
 * - 5 Azan (Adhan) prayer times: Fajr, Dhuhr, Asr, Maghrib, Isha
 * - GPS-based location for Qibla and prayer calculations
 * - Saves last known location in SharedPreferences
 * - Prompts user to enable GPS if disabled
 */
public class HawsaCompassActivity extends AppCompatActivity {

    private CompassView compassView;
    private TextView tvQiblaDirection;
    private TextView tvQiblaDegrees;
    private LinearLayout prayerTimesContainer;
    private ImageView btnBack;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS_NAME = "hawsa_compass_prefs";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LON = "last_lon";

    private boolean locationAcquired = false;

    // 5 Azan (Adhan) prayer names in Arabic (NO Sunrise)
    private static final String[] PRAYER_NAMES_AR = {
        "\u0627\u0644\u0641\u062C\u0631",     // Fajr
        "\u0627\u0644\u0638\u0647\u0631",     // Dhuhr
        "\u0627\u0644\u0639\u0635\u0631",     // Asr
        "\u0627\u0644\u0645\u063A\u0631\u0628",   // Maghrib
        "\u0627\u0644\u0639\u0634\u0627\u0621"     // Isha
    };

    // Prayer color indicators (gold theme)
    private static final int[] PRAYER_COLORS = {
        0xFF4FC3F7,  // Fajr - light blue
        0xFFD4AF37,  // Dhuhr - gold
        0xFFFF8A65,  // Asr - deep orange
        0xFFFF5722,  // Maghrib - red-orange
        0xFF7C4DFF   // Isha - purple
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            saveLocation(location);
            locationAcquired = true;
            updateFromLocation(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {
            requestLocationUpdates();
        }

        @Override
        public void onProviderDisabled(String provider) {
            showGPSPrompt();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0D1B2A);
            getWindow().setNavigationBarColor(0xFF0D1B2A);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        }

        // Fullscreen flags
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Force RTL for Arabic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_compass);

        // Back button
        btnBack = findViewById(R.id.btn_compass_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Title
        TextView tvTitle = findViewById(R.id.tv_compass_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_compass_title);

        compassView = findViewById(R.id.compass_view);
        tvQiblaDirection = findViewById(R.id.tv_qibla_direction);
        tvQiblaDegrees = findViewById(R.id.tv_qibla_degrees);
        prayerTimesContainer = findViewById(R.id.prayer_times_container);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Try saved location first
        loadSavedLocation();

        // Check and request GPS
        checkLocationAndRequest();

        buildPrayerTimes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply fullscreen
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        if (compassView != null) compassView.startSensors();
        if (locationAcquired) requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (compassView != null) compassView.stopSensors();
        stopLocationUpdates();
    }

    private void checkLocationAndRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST);
        } else {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                requestLocationUpdates();
            } else {
                showGPSPrompt();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    requestLocationUpdates();
                } else {
                    showGPSPrompt();
                }
            } else {
                Toast.makeText(this,
                    "\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u0625\u0630\u0646 \u0627\u0644\u0645\u0648\u0642\u0639 \u0644\u062D\u0633\u0627\u0628 \u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629 \u0648\u0623\u0648\u0642\u0627\u062A \u0627\u0644\u0623\u0630\u0627\u0646",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestLocationUpdates() {
        if (locationManager == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    5000, 1, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    5000, 1, locationListener);
            } catch (Exception ignored) {}
        }
    }

    @SuppressLint("MissingPermission")
    private void stopLocationUpdates() {
        if (locationManager != null) {
            try { locationManager.removeUpdates(locationListener); }
            catch (Exception ignored) {}
        }
    }

    private void saveLocation(Location location) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putFloat(KEY_LAT, (float) location.getLatitude())
            .putFloat(KEY_LON, (float) location.getLongitude())
            .apply();
    }

    private void loadSavedLocation() {
        float savedLat = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getFloat(KEY_LAT, Float.MIN_VALUE);
        float savedLon = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getFloat(KEY_LON, Float.MIN_VALUE);

        if (savedLat != Float.MIN_VALUE && savedLon != Float.MIN_VALUE) {
            double tzOffset = getTimezoneOffset();
            PrayerTimesCalculator.setLocation(savedLat, savedLon, tzOffset);
            double qibla = PrayerTimesCalculator.calculateQiblaBearing();
            if (compassView != null) compassView.setQiblaBearing(qibla);
            updateQiblaInfo(qibla);
            buildPrayerTimes();
            locationAcquired = true;
        }
    }

    private void updateFromLocation(double latitude, double longitude) {
        double tzOffset = getTimezoneOffset();
        PrayerTimesCalculator.setLocation(latitude, longitude, tzOffset);

        double qibla = PrayerTimesCalculator.calculateQiblaBearing();
        if (compassView != null) compassView.setQiblaBearing(qibla);
        updateQiblaInfo(qibla);
        buildPrayerTimes();
    }

    private double getTimezoneOffset() {
        TimeZone tz = TimeZone.getDefault();
        int offsetMs = tz.getOffset(System.currentTimeMillis());
        return offsetMs / 3600000.0;
    }

    private void updateQiblaInfo(double qibla) {
        String directionAr = getQiblaDirectionArabic(qibla);
        if (tvQiblaDirection != null) {
            tvQiblaDirection.setText("\u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629: " + directionAr);
        }
        if (tvQiblaDegrees != null) {
            tvQiblaDegrees.setText(String.format(Locale.US, "%.1f\u00B0 \u0645\u0646 \u0627\u0644\u0634\u0645\u0627\u0644", qibla));
        }
    }

    private String getQiblaDirectionArabic(double bearing) {
        if (bearing >= 337.5 || bearing < 22.5) return "\u0634\u0645\u0627\u0644";
        if (bearing >= 22.5 && bearing < 67.5) return "\u0634\u0645\u0627\u0644 \u0634\u0631\u0642";
        if (bearing >= 67.5 && bearing < 112.5) return "\u0634\u0631\u0642";
        if (bearing >= 112.5 && bearing < 157.5) return "\u062C\u0646\u0648\u0628 \u0634\u0631\u0642";
        if (bearing >= 157.5 && bearing < 202.5) return "\u062C\u0646\u0648\u0628";
        if (bearing >= 202.5 && bearing < 247.5) return "\u062C\u0646\u0648\u0628 \u063A\u0631\u0628";
        if (bearing >= 247.5 && bearing < 292.5) return "\u063A\u0631\u0628";
        return "\u0634\u0645\u0627\u0644 \u063A\u0631\u0628";
    }

    private void showGPSPrompt() {
        new AlertDialog.Builder(this)
            .setTitle("\u062A\u0641\u0639\u064A\u0644 \u0627\u0644\u0645\u0648\u0642\u0639")
            .setMessage("\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 (GPS) \u0644\u062D\u0633\u0627\u0628 \u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629 \u0648\u0623\u0648\u0642\u0627\u062A \u0627\u0644\u0623\u0630\u0627\u0646 \u0628\u062F\u0642\u0629. \u0647\u0644 \u062A\u0631\u064A\u062F \u0627\u0644\u0630\u0647\u0627\u0628 \u0625\u0644\u0649 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0645\u0648\u0642\u0639\u061F")
            .setPositiveButton("\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A", (dialog, which) -> {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } catch (Exception ignored) {}
            })
            .setNegativeButton("\u0625\u0644\u063A\u0627\u0621", null)
            .setCancelable(false)
            .show();
    }

    private void buildPrayerTimes() {
        if (prayerTimesContainer == null) return;
        prayerTimesContainer.removeAllViews();

        PrayerTimesCalculator.PrayerTimes times = PrayerTimesCalculator.getTodayPrayerTimes();

        // Find next prayer to highlight
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        double currentDecimalHour = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;
        int nextPrayerIdx = -1;
        double[] prayerHours = {times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha};

        for (int i = 0; i < prayerHours.length; i++) {
            if (prayerHours[i] > currentDecimalHour) {
                nextPrayerIdx = i;
                break;
            }
        }
        if (nextPrayerIdx == -1) nextPrayerIdx = 0;

        String[] prayerTimeStrs = {
            times.getFajrStr(),
            times.getDhuhrStr(),
            times.getAsrStr(),
            times.getMaghribStr(),
            times.getIshaStr()
        };

        for (int i = 0; i < 5; i++) {
            View row = createPrayerRow(i, prayerTimeStrs[i], i == nextPrayerIdx);
            prayerTimesContainer.addView(row);
        }
    }

    private View createPrayerRow(int index, String timeStr, boolean isNext) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(12, 10, 12, 10);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Highlight next prayer with subtle gold background
        if (isNext) {
            row.setBackgroundColor(0x20D4AF37);
        }

        // Color indicator dot
        View dot = new View(this);
        dot.setBackgroundColor(PRAYER_COLORS[index]);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(10, 10);
        dotParams.setMargins(0, 0, 10, 0);
        dot.setLayoutParams(dotParams);
        row.addView(dot);

        // Prayer name (Arabic)
        TextView tvNameAr = new TextView(this);
        tvNameAr.setText(PRAYER_NAMES_AR[index]);
        tvNameAr.setTextSize(18);
        tvNameAr.setTextColor(isNext ? 0xFFD4AF37 : 0xFFE0E1DD);
        tvNameAr.getPaint().setFakeBoldText(isNext);
        tvNameAr.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvNameAr);

        // "أذان" label before time
        if (isNext) {
            TextView tvAzan = new TextView(this);
            tvAzan.setText("\u0623\u0630\u0627\u0646 ");  // أذان
            tvAzan.setTextSize(14);
            tvAzan.setTextColor(0xFFD4AF37);
            tvAzan.getPaint().setFakeBoldText(true);
            LinearLayout.LayoutParams azanParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tvAzan.setLayoutParams(azanParams);
            row.addView(tvAzan);
        }

        // Time display
        TextView tvTime = new TextView(this);
        tvTime.setText(timeStr);
        tvTime.setTextSize(18);
        tvTime.setTextColor(isNext ? 0xFFD4AF37 : 0xFFE0E1DD);
        tvTime.getPaint().setFakeBoldText(true);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.setMargins(16, 0, 0, 0);
        tvTime.setLayoutParams(timeParams);
        row.addView(tvTime);

        // Separator line
        if (index < 4) {
            View separator = new View(this);
            separator.setBackgroundColor(0x40D4AF37);
            LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            );
            sepParams.setMargins(16, 10, 0, 0);
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
