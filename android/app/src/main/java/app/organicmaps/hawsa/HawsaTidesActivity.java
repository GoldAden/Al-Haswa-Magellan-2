package app.organicmaps.hawsa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

/**
 * Redesigned Tides activity with GPS-based nearest marine station,
 * current tide status, modern day selector pills, and weekly summary.
 */
public class HawsaTidesActivity extends AppCompatActivity {

    private TideChartView tideChartView;
    private LinearLayout daysContainer;
    private TextView tvTidesTitle;
    private TextView tvStationName;
    private TextView tvWeeklySummary;
    private ImageView btnBack;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    private static final String PREFS_NAME = "hawsa_tides_prefs";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LON = "last_lon";

    private boolean locationAcquired = false;
    private int selectedDayOffset = 0;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            saveLocation(location);
            locationAcquired = true;
            TideCalculator.setUserLocation(location.getLatitude(), location.getLongitude());
            updateStationDisplay();
            refreshTideData();
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

        // Match existing layout IDs: btn_tides_back, tv_tides_title, tv_station_name,
        // tide_chart_view, days_container, tv_weekly_summary
        btnBack = findViewById(R.id.btn_tides_back);
        tvTidesTitle = findViewById(R.id.tv_tides_title);
        tvStationName = findViewById(R.id.tv_station_name);
        tideChartView = findViewById(R.id.tide_chart_view);
        daysContainer = findViewById(R.id.days_container);
        tvWeeklySummary = findViewById(R.id.tv_weekly_summary);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvTidesTitle != null) tvTidesTitle.setText("\u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Try to load saved location first
        loadSavedLocation();

        // Check location permission and request GPS
        checkLocationAndRequest();

        // Build initial day buttons and tide data
        buildDayPills();
        refreshTideData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationAcquired) {
            requestLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
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
                Toast.makeText(this, "\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u0625\u0630\u0646 \u0627\u0644\u0645\u0648\u0642\u0639 \u0644\u062D\u0633\u0627\u0628 \u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631 \u0628\u062F\u0642\u0629",
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
                // Get last known for faster initial fix
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown != null) {
                    saveLocation(lastKnown);
                    locationAcquired = true;
                    TideCalculator.setUserLocation(lastKnown.getLatitude(), lastKnown.getLongitude());
                    updateStationDisplay();
                    refreshTideData();
                }
            } catch (Exception ignored) {}
        }
    }

    @SuppressLint("MissingPermission")
    private void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ignored) {}
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
            TideCalculator.setUserLocation(savedLat, savedLon);
            locationAcquired = true;
            updateStationDisplay();
        }
    }

    private void updateStationDisplay() {
        if (tvStationName != null) {
            String stationName = TideCalculator.getStationNameAr();
            double dist = TideCalculator.getDistanceToNearestStationKm();
            String distStr = dist < 1 ? String.format(Locale.US, "%.0f\u0645", dist * 1000) :
                    String.format(Locale.US, "%.1f\u0643\u0645", dist);
            tvStationName.setText("\u0645\u062D\u0637\u0629 " + stationName + " (" + distStr + ")");
        }
    }

    private void showGPSPrompt() {
        new AlertDialog.Builder(this, R.style.HawsaAlertDialog)
                .setTitle("\u062A\u0641\u0639\u064A\u0644 \u0627\u0644\u0645\u0648\u0642\u0639")
                .setMessage("\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 (GPS) \u0644\u062D\u0633\u0627\u0628 \u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631 \u0628\u062F\u0642\u0629 \u0628\u0646\u0627\u0621 \u0639\u0644\u0649 \u0623\u0642\u0631\u0628 \u0645\u062D\u0637\u0629 \u0628\u062D\u0631\u064A\u0629. \u0647\u0644 \u062A\u0631\u064A\u062F \u0627\u0644\u0630\u0647\u0627\u0628 \u0625\u0644\u0649 \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A\u061F")
                .setPositiveButton("\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A", (dialog, which) -> {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("\u0625\u0644\u063A\u0627\u0621", null)
                .setCancelable(false)
                .show();
    }

    /**
     * Modern pill-style day selector buttons
     */
    private void buildDayPills() {
        if (daysContainer == null) return;
        daysContainer.removeAllViews();

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

            // Pill background
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
            daysContainer.addView(pill);
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
        if (tvWeeklySummary == null) return;

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

        tvWeeklySummary.setText(sb.toString().trim());
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