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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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

/**
 * Redesigned Tides activity with GPS-based nearest marine station,
 * current tide status, modern day selector pills, and weekly summary.
 */
public class HawsaTidesActivity extends AppCompatActivity {

    private TideChartView tideChartView;
    private LinearLayout dayButtonContainer;
    private LinearLayout weeklyTidesContainer;
    private TextView tvTidesTitle;
    private TextView tvTidesLocation;
    private TextView tvCurrentStatus;
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
            updateLocationDisplay();
            updateCurrentStatus();
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

        // Dark status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.hawsa_bg_dark_navy));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        }

        // Force RTL for Arabic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_tides);

        btnBack = findViewById(R.id.btn_tides_back);
        tvTidesTitle = findViewById(R.id.tv_tides_title);
        tvTidesLocation = findViewById(R.id.tv_tides_location);
        tvCurrentStatus = findViewById(R.id.tv_current_status);
        tideChartView = findViewById(R.id.tide_chart);
        dayButtonContainer = findViewById(R.id.day_button_container);
        weeklyTidesContainer = findViewById(R.id.weekly_tides_container);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        if (tvTidesTitle != null) tvTidesTitle.setText(R.string.hawsa_tides_title);

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
        updateCurrentStatus();
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
                    updateLocationDisplay();
                    updateCurrentStatus();
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
            updateLocationDisplay();
            updateCurrentStatus();
        }
    }

    private void updateLocationDisplay() {
        if (tvTidesLocation != null) {
            String stationName = TideCalculator.getStationNameAr();
            double dist = TideCalculator.getStationDistanceKm();
            String distStr = dist < 1 ? String.format(Locale.US, "%.0f\u0645", dist * 1000) :
                    String.format(Locale.US, "%.1f\u0643\u0645", dist);
            tvTidesLocation.setText("\u0645\u062D\u0637\u0629 " + stationName + " (" + distStr + ")");
        }
    }

    private void updateCurrentStatus() {
        if (tvCurrentStatus != null) {
            double currentHeight = TideCalculator.getTideHeight(
                    Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
            // Determine if rising or falling by checking derivative
            long nextMs = Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis() + 600000; // +10min
            double nextHeight = TideCalculator.getTideHeight(nextMs);
            boolean rising = nextHeight > currentHeight;

            String status = rising ?
                    String.format(Locale.US, "\u0645\u062F \u2191 %.1f\u0645", currentHeight) :
                    String.format(Locale.US, "\u062C\u0632\u0631 \u2193 %.1f\u0645", currentHeight);
            int color = rising ? 0xFF10B981 : 0xFFEF4444;

            tvCurrentStatus.setText(status);
            tvCurrentStatus.setTextColor(color);
            tvCurrentStatus.getPaint().setFakeBoldText(true);
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
        if (dayButtonContainer == null) return;
        dayButtonContainer.removeAllViews();

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
                updateCurrentStatus();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(6, 0, 6, 0);
            pill.setLayoutParams(params);
            dayButtonContainer.addView(pill);
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

        String[] arabicDays = {"\u0627\u0644\u0623\u062D\u062F", "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", "\u0627\u0644\u062E\u0645\u064A\u0633", "\u0627\u0644\u062C\u0645\u0639\u0629", "\u0627\u0644\u0633\u0628\u062A"};
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < summary.size(); i++) {
            TideCalculator.DayTideSummary day = summary.get(i);
            int dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK) - 1;

            // Card-like row
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 14, 16, 14);

            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setCornerRadius(12f);
            cardBg.setColor(i == selectedDayOffset ? 0x20D4AF37 : 0xFF111827);
            cardBg.setStroke(1, i == selectedDayOffset ? 0xFFD4AF37 : 0xFF1E293B);
            card.setBackground(cardBg);

            // Day header row
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvDay = new TextView(this);
            tvDay.setText(arabicDays[dayOfWeek] + " " + dateFmt.format(day.date.getTime()));
            tvDay.setTextSize(15);
            tvDay.setTextColor(i == selectedDayOffset ? 0xFFD4AF37 : 0xFFE0E1DD);
            tvDay.getPaint().setFakeBoldText(true);

            // Max/Min badges
            LinearLayout badgeRow = new LinearLayout(this);
            badgeRow.setOrientation(LinearLayout.HORIZONTAL);
            badgeRow.setGravity(Gravity.END);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            badgeRow.setLayoutParams(badgeParams);

            if (day.maxHeight > Double.MIN_VALUE) {
                TextView tvMax = new TextView(this);
                tvMax.setText(String.format(Locale.US, "\u2191%.1f", day.maxHeight));
                tvMax.setTextSize(11);
                tvMax.setTextColor(0xFF10B981);

                android.graphics.drawable.GradientDrawable maxBg = new android.graphics.drawable.GradientDrawable();
                maxBg.setCornerRadius(8f);
                maxBg.setColor(0x2010B981);
                tvMax.setPadding(8, 4, 8, 4);
                tvMax.setBackground(maxBg);

                badgeRow.addView(tvMax);
            }

            if (day.minHeight < Double.MAX_VALUE) {
                TextView tvMin = new TextView(this);
                tvMin.setText(String.format(Locale.US, "\u2193%.1f", day.minHeight));
                tvMin.setTextSize(11);
                tvMin.setTextColor(0xFFEF4444);

                android.graphics.drawable.GradientDrawable minBg = new android.graphics.drawable.GradientDrawable();
                minBg.setCornerRadius(8f);
                minBg.setColor(0x20EF4444);
                tvMin.setPadding(8, 4, 8, 4);
                tvMin.setBackground(minBg);

                LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                minParams.setMargins(6, 0, 0, 0);
                tvMin.setLayoutParams(minParams);
                badgeRow.addView(tvMin);
            }

            headerRow.addView(tvDay);
            headerRow.addView(badgeRow);
            card.addView(headerRow);

            // High tides detail
            LinearLayout highsRow = new LinearLayout(this);
            highsRow.setOrientation(LinearLayout.HORIZONTAL);
            highsRow.setPadding(0, 6, 0, 0);

            TextView tvHighLabel = new TextView(this);
            tvHighLabel.setText("\u0645\u062F \u0639\u0627\u0644\u064A: ");
            tvHighLabel.setTextSize(12);
            tvHighLabel.setTextColor(0xFF10B981);
            highsRow.addView(tvHighLabel);

            if (day.firstHigh != null) {
                TextView tvH1 = new TextView(this);
                tvH1.setText(formatEventTime(day.firstHigh) + " (" + String.format(Locale.US, "%.1f\u0645", day.firstHigh.height) + ")");
                tvH1.setTextSize(12);
                tvH1.setTextColor(0xFF10B981);
                highsRow.addView(tvH1);
            }
            if (day.secondHigh != null) {
                TextView tvSep = new TextView(this);
                tvSep.setText(" | ");
                tvSep.setTextSize(12);
                tvSep.setTextColor(0xFF334155);
                highsRow.addView(tvSep);
                TextView tvH2 = new TextView(this);
                tvH2.setText(formatEventTime(day.secondHigh) + " (" + String.format(Locale.US, "%.1f\u0645", day.secondHigh.height) + ")");
                tvH2.setTextSize(12);
                tvH2.setTextColor(0xFF10B981);
                highsRow.addView(tvH2);
            }
            card.addView(highsRow);

            // Low tides detail
            LinearLayout lowsRow = new LinearLayout(this);
            lowsRow.setOrientation(LinearLayout.HORIZONTAL);
            lowsRow.setPadding(0, 3, 0, 0);

            TextView tvLowLabel = new TextView(this);
            tvLowLabel.setText("\u062C\u0632\u0631 \u0645\u0646\u062E\u0641\u0636: ");
            tvLowLabel.setTextSize(12);
            tvLowLabel.setTextColor(0xFFEF4444);
            lowsRow.addView(tvLowLabel);

            if (day.firstLow != null) {
                TextView tvL1 = new TextView(this);
                tvL1.setText(formatEventTime(day.firstLow) + " (" + String.format(Locale.US, "%.1f\u0645", day.firstLow.height) + ")");
                tvL1.setTextSize(12);
                tvL1.setTextColor(0xFFEF4444);
                lowsRow.addView(tvL1);
            }
            if (day.secondLow != null) {
                TextView tvSep2 = new TextView(this);
                tvSep2.setText(" | ");
                tvSep2.setTextSize(12);
                tvSep2.setTextColor(0xFF334155);
                lowsRow.addView(tvSep2);
                TextView tvL2 = new TextView(this);
                tvL2.setText(formatEventTime(day.secondLow) + " (" + String.format(Locale.US, "%.1f\u0645", day.secondLow.height) + ")");
                tvL2.setTextSize(12);
                tvL2.setTextColor(0xFFEF4444);
                lowsRow.addView(tvL2);
            }
            card.addView(lowsRow);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, i == 0 ? 0 : 8, 0, 0);
            card.setLayoutParams(cardParams);
            weeklyTidesContainer.addView(card);
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
