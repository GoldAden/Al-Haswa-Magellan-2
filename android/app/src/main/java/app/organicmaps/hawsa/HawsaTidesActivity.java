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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

public class HawsaTidesActivity extends AppCompatActivity {

    private TideChartView tideChartView;
    private LinearLayout dayButtonContainer;
    private LinearLayout weeklyTidesContainer;
    private TextView tvTidesTitle;
    private TextView tvTidesLocation;

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
            updateLocationDisplay();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_tides);

        tvTidesTitle = findViewById(R.id.tv_tides_title);
        tvTidesLocation = findViewById(R.id.tv_tides_location);
        tideChartView = findViewById(R.id.tide_chart);
        dayButtonContainer = findViewById(R.id.day_button_container);
        weeklyTidesContainer = findViewById(R.id.weekly_tides_container);

        if (tvTidesTitle != null) tvTidesTitle.setText(R.string.hawsa_tides_title);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Try to load saved location first
        loadSavedLocation();

        // Check location permission and request GPS
        checkLocationAndRequest();

        // Build initial day buttons and tide data
        buildDayButtons();
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
            updateLocationDisplay();
        }
    }

    private void updateLocationDisplay() {
        if (tvTidesLocation != null) {
            String stationName = TideCalculator.getStationNameAr();
            tvTidesLocation.setText("\u0645\u062D\u0637\u0629: " + stationName);
        }
    }

    private void showGPSPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("\u062A\u0641\u0639\u064A\u0644 \u0627\u0644\u0645\u0648\u0642\u0639")
                .setMessage("\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 (GPS) \u0644\u062D\u0633\u0627\u0628 \u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631 \u0628\u062F\u0642\u0629 \u0628\u0646\u0627\u0621 \u0639\u0644\u0649 \u0645\u0648\u0642\u0639\u0643. \u0647\u0644 \u062A\u0631\u064A\u062F \u0627\u0644\u0630\u0647\u0627\u0628 \u0625\u0644\u0649 \u0625\u0639\u062F\u0627\u062F\u0627\u062A \u0627\u0644\u0645\u0648\u0642\u0639\u061F")
                .setPositiveButton("\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A", (dialog, which) -> {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("\u0625\u0644\u063A\u0627\u0621", null)
                .setCancelable(false)
                .show();
    }

    private void buildDayButtons() {
        if (dayButtonContainer == null) return;
        dayButtonContainer.removeAllViews();

        // Day names not used - using arabicDays array below instead

        // Actually let's use proper day offsets
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("E dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            Button btn = new Button(this);
            if (i == 0) {
                btn.setText("\u0627\u0644\u064A\u0648\u0645");
            } else {
                Calendar offset = (Calendar) cal.clone();
                offset.add(Calendar.DAY_OF_MONTH, i);
                String[] arabicDays = {"\u0627\u0644\u0623\u062D\u062F", "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", "\u0627\u0644\u062E\u0645\u064A\u0633", "\u0627\u0644\u062C\u0645\u0639\u0629", "\u0627\u0644\u0633\u0628\u062A"};
                int dayOfWeek = offset.get(Calendar.DAY_OF_WEEK) - 1;
                btn.setText(arabicDays[dayOfWeek]);
            }
            btn.setTextSize(12);
            btn.setTextColor(i == selectedDayOffset ? 0xFF4FC3F7 : 0xFF778DA9);
            btn.setBackgroundColor(i == selectedDayOffset ? 0x404FC3F7 : 0x00000000);
            final int dayOffset = i;
            btn.setOnClickListener(v -> {
                selectedDayOffset = dayOffset;
                buildDayButtons();
                refreshTideData();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(params);
            dayButtonContainer.addView(btn);
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

        buildWeeklySummary();
    }

    private void buildWeeklySummary() {
        if (weeklyTidesContainer == null) return;
        weeklyTidesContainer.removeAllViews();

        List<TideCalculator.DayTideSummary> summary = TideCalculator.getWeeklySummary();

        for (int i = 0; i < summary.size(); i++) {
            TideCalculator.DayTideSummary day = summary.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(8, 8, 8, 8);

            if (i == selectedDayOffset) {
                row.setBackgroundColor(0x204FC3F7);
            }

            // Day header
            String[] arabicDays = {"\u0627\u0644\u0623\u062D\u062F", "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", "\u0627\u0644\u062E\u0645\u064A\u0633", "\u0627\u0644\u062C\u0645\u0639\u0629", "\u0627\u0644\u0633\u0628\u062A"};
            int dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK) - 1;
            SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());

            TextView tvDay = new TextView(this);
            tvDay.setText(arabicDays[dayOfWeek] + " " + dateFmt.format(day.date.getTime()));
            tvDay.setTextSize(14);
            tvDay.setTextColor(0xFFE0E1DD);
            tvDay.getPaint().setFakeBoldText(true);
            row.addView(tvDay);

            // High tides
            LinearLayout highsRow = new LinearLayout(this);
            highsRow.setOrientation(LinearLayout.HORIZONTAL);
            highsRow.setPadding(0, 4, 0, 0);

            TextView tvHighLabel = new TextView(this);
            tvHighLabel.setText("\u0645\u062F \u0639\u0627\u0644\u064A: ");
            tvHighLabel.setTextSize(12);
            tvHighLabel.setTextColor(0xFF4CAF50);
            highsRow.addView(tvHighLabel);

            if (day.firstHigh != null) {
                TextView tvH1 = new TextView(this);
                tvH1.setText(formatEventTime(day.firstHigh) + " (" + String.format(Locale.US, "%.1f\u0645", day.firstHigh.height) + ")");
                tvH1.setTextSize(12);
                tvH1.setTextColor(0xFF4CAF50);
                highsRow.addView(tvH1);
            }
            if (day.secondHigh != null) {
                TextView tvSep = new TextView(this);
                tvSep.setText(" | ");
                tvSep.setTextSize(12);
                tvSep.setTextColor(0xFF415A77);
                highsRow.addView(tvSep);
                TextView tvH2 = new TextView(this);
                tvH2.setText(formatEventTime(day.secondHigh) + " (" + String.format(Locale.US, "%.1f\u0645", day.secondHigh.height) + ")");
                tvH2.setTextSize(12);
                tvH2.setTextColor(0xFF4CAF50);
                highsRow.addView(tvH2);
            }
            row.addView(highsRow);

            // Low tides
            LinearLayout lowsRow = new LinearLayout(this);
            lowsRow.setOrientation(LinearLayout.HORIZONTAL);
            lowsRow.setPadding(0, 2, 0, 0);

            TextView tvLowLabel = new TextView(this);
            tvLowLabel.setText("\u0645\u062F \u0645\u0646\u062E\u0641\u0636: ");
            tvLowLabel.setTextSize(12);
            tvLowLabel.setTextColor(0xFFFF5722);
            lowsRow.addView(tvLowLabel);

            if (day.firstLow != null) {
                TextView tvL1 = new TextView(this);
                tvL1.setText(formatEventTime(day.firstLow) + " (" + String.format(Locale.US, "%.1f\u0645", day.firstLow.height) + ")");
                tvL1.setTextSize(12);
                tvL1.setTextColor(0xFFFF5722);
                lowsRow.addView(tvL1);
            }
            if (day.secondLow != null) {
                TextView tvSep2 = new TextView(this);
                tvSep2.setText(" | ");
                tvSep2.setTextSize(12);
                tvSep2.setTextColor(0xFF415A77);
                lowsRow.addView(tvSep2);
                TextView tvL2 = new TextView(this);
                tvL2.setText(formatEventTime(day.secondLow) + " (" + String.format(Locale.US, "%.1f\u0645", day.secondLow.height) + ")");
                tvL2.setTextSize(12);
                tvL2.setTextColor(0xFFFF5722);
                lowsRow.addView(tvL2);
            }
            row.addView(lowsRow);

            // Separator
            if (i < summary.size() - 1) {
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
            weeklyTidesContainer.addView(row);
        }
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
