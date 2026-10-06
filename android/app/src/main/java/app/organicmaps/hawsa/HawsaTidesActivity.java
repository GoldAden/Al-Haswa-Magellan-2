package app.organicmaps.hawsa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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
 * Tides activity with GPS-based tide predictions.
 * Uses TideCalculator with 28 stations, finding the nearest via haversine.
 * Saves last known location in SharedPreferences.
 * Prompts user to enable GPS if disabled.
 * Arabic UI with ص/م time format.
 */
public class HawsaTidesActivity extends AppCompatActivity {

    private TideChartView tideChartView;
    private LinearLayout daysContainer;
    private TextView tvTidesTitle;
    private TextView tvStationName;
    private TextView tvWeeklySummary;
    private View btnBack;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final String PREFS_NAME = "hawsa_tides_prefs";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LON = "last_lon";

    private boolean locationAcquired = false;
    private int selectedDayOffset = 0; // 0 = today

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

        // Dark status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0D1B2A);
            getWindow().setNavigationBarColor(0xFF0D1B2A);
        }

        // Force RTL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        setContentView(R.layout.activity_hawsa_tides);

        btnBack = findViewById(R.id.btn_tides_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTidesTitle = findViewById(R.id.tv_tides_title);
        if (tvTidesTitle != null) tvTidesTitle.setText(R.string.hawsa_tides_title);

        tideChartView = findViewById(R.id.tide_chart_view);
        tvStationName = findViewById(R.id.tv_station_name);
        tvWeeklySummary = findViewById(R.id.tv_weekly_summary);
        daysContainer = findViewById(R.id.days_container);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Load saved location first
        loadSavedLocation();

        // Check GPS and request
        checkLocationAndRequest();

        // Build day selector buttons
        buildDayButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationAcquired) requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    // --- The rest is GPS + SharedPreferences + tide logic ---

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
                    "\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u0625\u0630\u0646 \u0627\u0644\u0645\u0648\u0642\u0639 \u0644\u062D\u0633\u0627\u0628 \u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631",
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
                    10000, 10, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    10000, 10, locationListener);
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
            updateFromLocation(savedLat, savedLon);
            locationAcquired = true;
        }
    }

    private void updateFromLocation(double latitude, double longitude) {
        TideCalculator.setUserLocation(latitude, longitude);
        TideCalculator.TideStation station = TideCalculator.findNearestStation();

        if (tvStationName != null) {
            tvStationName.setText("\u0645\u062D\u0637\u0629: " + station.name + " (" + String.format(Locale.US, "%.0f", station.distanceKm) + " \u0643\u0645)");
        }

        updateChartForDay(selectedDayOffset);
        updateWeeklySummary();
    }

    private void updateChartForDay(int dayOffset) {
        selectedDayOffset = dayOffset;
        if (tideChartView != null) {
            TideCalculator.TideData data = TideCalculator.predictTidesForDay(dayOffset);
            tideChartView.setTideData(data);
        }
        highlightSelectedDay(dayOffset);
    }

    private void updateWeeklySummary() {
        if (tvWeeklySummary == null) return;

        StringBuilder sb = new StringBuilder();
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());

        for (int d = 0; d < 7; d++) {
            TideCalculator.TideData data = TideCalculator.predictTidesForDay(d);
            cal.add(Calendar.DATE, d == 0 ? 0 : 1);

            String dayName = getArabicDayName(cal.get(Calendar.DAY_OF_WEEK));
            String highTime = formatDecimalHourArabic(data.timeOfHigh);
            String lowTime = formatDecimalHourArabic(data.timeOfLow);

            sb.append(String.format(Locale.US, "%s: \u0645\u0646\u062E\u0641\u0636 %.1f\u0645 (%s) | \u0645\u0631\u062A\u0641\u0639 %.1f\u0645 (%s)\n",
                dayName, data.heightOfLow, lowTime, data.heightOfHigh, highTime));
        }

        tvWeeklySummary.setText(sb.toString().trim());
    }

    private void buildDayButtons() {
        if (daysContainer == null) return;
        daysContainer.removeAllViews();

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        String[] dayLabels = new String[7];
        dayLabels[0] = "\u0627\u0644\u064A\u0648\u0645";  // اليوم

        for (int d = 1; d < 7; d++) {
            cal.add(Calendar.DATE, 1);
            dayLabels[d] = getArabicDayName(cal.get(Calendar.DAY_OF_WEEK));
        }
        // Reset calendar
        cal = Calendar.getInstance(TimeZone.getDefault());

        for (int d = 0; d < 7; d++) {
            final int dayOffset = d;
            Button btn = new Button(this);
            btn.setText(dayLabels[d]);
            btn.setTextSize(12);
            btn.setTextColor(d == 0 ? 0xFFD4AF37 : 0xFF778DA9);
            btn.setBackgroundColor(d == 0 ? 0x20D4AF37 : 0x00);
            btn.setAllCaps(false);
            btn.setPadding(12, 8, 12, 8);
            btn.setOnClickListener(v -> updateChartForDay(dayOffset));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            btn.setLayoutParams(params);
            daysContainer.addView(btn);
        }
    }

    private void highlightSelectedDay(int selected) {
        if (daysContainer == null) return;
        for (int i = 0; i < daysContainer.getChildCount(); i++) {
            Button btn = (Button) daysContainer.getChildAt(i);
            btn.setTextColor(i == selected ? 0xFFD4AF37 : 0xFF778DA9);
            btn.setBackgroundColor(i == selected ? 0x20D4AF37 : 0x00);
        }
    }

    private String getArabicDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:    return "\u0627\u0644\u0623\u062D\u062F";
            case Calendar.MONDAY:    return "\u0627\u0644\u0625\u062B\u0646\u064A\u0646";
            case Calendar.TUESDAY:   return "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621";
            case Calendar.WEDNESDAY: return "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621";
            case Calendar.THURSDAY:  return "\u0627\u0644\u062E\u0645\u064A\u0633";
            case Calendar.FRIDAY:    return "\u0627\u0644\u062C\u0645\u0639\u0629";
            case Calendar.SATURDAY:  return "\u0627\u0644\u0633\u0628\u062A";
            default: return "";
        }
    }

    private String formatDecimalHourArabic(double decimalHour) {
        if (decimalHour < 0 || decimalHour >= 24) return "--:--";
        int hour24 = (int) decimalHour;
        int minute = (int) Math.round((decimalHour - hour24) * 60);
        if (minute == 60) { minute = 0; hour24++; }
        if (hour24 == 24) hour24 = 0;

        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;

        String ampm = hour24 < 12 ? "\u0635" : "\u0645";  // ص / م
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, ampm);
    }

    private void showGPSPrompt() {
        new AlertDialog.Builder(this)
            .setTitle("\u062A\u0641\u0639\u064A\u0644 \u0627\u0644\u0645\u0648\u0642\u0639")
            .setMessage("\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 (GPS) \u0644\u062D\u0633\u0627\u0628 \u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631 \u0628\u062F\u0642\u0629. \u0647\u0644 \u062A\u0631\u064A\u062F \u0627\u0644\u0630\u0647\u0627\u0628 \u0625\u0644\u0649 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0645\u0648\u0642\u0639\u061F")
            .setPositiveButton("\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A", (dialog, which) -> {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } catch (Exception ignored) {}
            })
            .setNegativeButton("\u0625\u0644\u063A\u0627\u0621", null)
            .setCancelable(false)
            .show();
    }
}
