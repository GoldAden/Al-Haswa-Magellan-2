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
 * Fullscreen Compass & Qibla activity with 5 Azan times.
 * Modern immersive design. GPS-based location with saved fallback.
 * Fixed: status bar color matches compass bg, GPS prompt single-dismiss, auto-dismiss on GPS enable.
 */
public class HawsaCompassActivity extends AppCompatActivity {

    private CompassView compassView;
    private TextView tvQiblaDirection;
    private TextView tvQiblaDegrees;
    private TextView tvQiblaArrow;
    private LinearLayout prayerTimesContainer;
    private ImageView btnBack;

    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS_NAME = "hawsa_compass_prefs";
    private static final String KEY_LAT = "last_lat";
    private static final String KEY_LON = "last_lon";
    private static final String KEY_TZ = "last_tz";

    private boolean locationAcquired = false;

    // GPS dialog reference — prevents stacking and allows single-dismiss
    private AlertDialog gpsDialog = null;
    private boolean isCheckingGpsOnResume = false;

    // Only 5 Azan prayers (NO Sunrise)
    private static final String[] PRAYER_NAMES_AR = {
        "\u0627\u0644\u0641\u062C\u0631", "\u0627\u0644\u0638\u0647\u0631", "\u0627\u0644\u0639\u0635\u0631", "\u0627\u0644\u0645\u063A\u0631\u0628", "\u0627\u0644\u0639\u0634\u0627\u0621"
    };

    private static final int[] PRAYER_COLORS = {
        0xFF4FC3F7,  // Fajr - light blue
        0xFFFFD54F,  // Dhuhr - gold
        0xFFFF8A65,  // Asr - deep orange
        0xFFFF5722,  // Maghrib - red-orange
        0xFF7C4DFF   // Isha - purple
    };

    private static final int[] PRAYER_ICON_RES = {
        R.drawable.ic_hawsa_prayer_fajr,
        R.drawable.ic_hawsa_prayer_dhuhr,
        R.drawable.ic_hawsa_prayer_asr,
        R.drawable.ic_hawsa_prayer_maghrib,
        R.drawable.ic_hawsa_prayer_isha
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
            // Auto-dismiss GPS dialog if it was showing
            dismissGpsDialog();
        }

        @Override
        public void onProviderDisabled(String provider) {
            showGPSPrompt();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // === FIX: Status bar color matches compass background ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0D1B2A);  // Match COLOR_BG exactly
            getWindow().setNavigationBarColor(0xFF0D1B2A);
        }

        // Immersive sticky mode
        hideSystemUI();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_compass);

        // Back button
        btnBack = findViewById(R.id.btn_compass_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            });
        }

        compassView = findViewById(R.id.compass_view);
        tvQiblaDirection = findViewById(R.id.tv_qibla_direction);
        tvQiblaDegrees = findViewById(R.id.tv_qibla_degrees);
        tvQiblaArrow = findViewById(R.id.tv_qibla_arrow);
        prayerTimesContainer = findViewById(R.id.prayer_times_container);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Try saved location first
        loadSavedLocation();

        // Check location permission and request GPS
        checkLocationAndRequest();

        buildPrayerTimes();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (compassView != null) {
            compassView.startSensors();
        }
        if (locationAcquired) {
            requestLocationUpdates();
        }
        hideSystemUI();

        // === FIX: Auto-dismiss GPS dialog if GPS was enabled while away ===
        if (gpsDialog != null && gpsDialog.isShowing()) {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                dismissGpsDialog();
                requestLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (compassView != null) {
            compassView.stopSensors();
        }
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
                Toast.makeText(this, "\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u0625\u0630\u0646 \u0627\u0644\u0645\u0648\u0642\u0639 \u0644\u062F\u0642\u0629 \u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (locationManager == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        10000, 10, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        10000, 10, locationListener);
                // Also try to get last known location for faster initial fix
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown != null) {
                    saveLocation(lastKnown);
                    locationAcquired = true;
                    updateFromLocation(lastKnown.getLatitude(), lastKnown.getLongitude());
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
                .putFloat(KEY_TZ, (float) TimeZone.getDefault().getRawOffset() / 3600000f)
                .apply();
    }

    private void loadSavedLocation() {
        float savedLat = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_LAT, Float.MIN_VALUE);
        float savedLon = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_LON, Float.MIN_VALUE);
        float savedTz = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_TZ, 3.0f);

        if (savedLat != Float.MIN_VALUE && savedLon != Float.MIN_VALUE) {
            locationAcquired = true;
            updateFromLocation(savedLat, savedLon);
        }
    }

    private void updateFromLocation(double lat, double lon) {
        double tzOffset = TimeZone.getDefault().getRawOffset() / 3600000.0;
        PrayerTimesCalculator.setLocation(lat, lon, tzOffset);

        // Calculate and set Qibla bearing
        double qibla = PrayerTimesCalculator.calculateQiblaBearing();
        if (compassView != null) {
            compassView.setQiblaBearing(qibla);
        }

        // Update Qibla direction display
        String directionAr = getQiblaDirectionArabic(qibla);
        if (tvQiblaDirection != null) {
            tvQiblaDirection.setText("\u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629: " + directionAr);
        }
        if (tvQiblaDegrees != null) {
            tvQiblaDegrees.setText(String.format(Locale.US, "%.0f\u00B0 \u0645\u0646 \u0627\u0644\u0634\u0645\u0627\u0644", qibla));
        }
        if (tvQiblaArrow != null) {
            tvQiblaArrow.setText("\u2192 " + String.format(Locale.US, "%.0f\u00B0", qibla));
        }

        buildPrayerTimes();
    }

    private String getQiblaDirectionArabic(double bearing) {
        if (bearing >= 337.5 || bearing < 22.5) return "\u0634\u0645\u0627\u0644";
        if (bearing >= 22.5 && bearing < 67.5) return "\u0634\u0645\u0627\u0644 \u0634\u0631\u0642";
        if (bearing >= 67.5 && bearing < 112.5) return "\u0634\u0631\u0642";
        if (bearing >= 112.5 && bearing < 157.5) return "\u062C\u0646\u0648\u0628 \u0634\u0631\u0642";
        if (bearing >= 157.5 && bearing < 202.5) return "\u062C\u0646\u0648\u0628";
        if (bearing >= 202.5 && bearing < 247.5) return "\u062C\u0646\u0648\u0628 \u063A\u0631\u0628";
        if (bearing >= 247.5 && bearing < 292.5) return "\u063A\u0631\u0628";
        if (bearing >= 292.5 && bearing < 337.5) return "\u0634\u0645\u0627\u0644 \u063A\u0631\u0628";
        return "\u0634\u0645\u0627\u0644";
    }

    // === FIX: GPS prompt with single-dismiss and auto-dismiss ===
    private void showGPSPrompt() {
        // Don't stack dialogs
        if (gpsDialog != null && gpsDialog.isShowing()) {
            return;
        }

        gpsDialog = new AlertDialog.Builder(this, R.style.HawsaAlertDialog)
                .setTitle("\u062A\u0641\u0639\u064A\u0644 \u0627\u0644\u0645\u0648\u0642\u0639")
                .setMessage("\u064A\u062C\u0628 \u062A\u0641\u0639\u064A\u0644 \u062E\u062F\u0645\u0629 \u0627\u0644\u0645\u0648\u0642\u0639 (GPS) \u0644\u062F\u0642\u0629 \u0627\u062A\u062C\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629 \u0648\u0623\u0648\u0642\u0627\u062A \u0627\u0644\u0623\u0630\u0627\u0646. \u0647\u0644 \u062A\u0631\u064A\u062F \u0627\u0644\u0630\u0647\u0627\u0628 \u0625\u0644\u0649 \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A\u061F")
                .setPositiveButton("\u0641\u062A\u062D \u0627\u0644\u0625\u0639\u062F\u0627\u062F\u0627\u062A", (dialog, which) -> {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    } catch (Exception ignored) {}
                    gpsDialog = null;
                })
                .setNegativeButton("\u0625\u0644\u063A\u0627\u0621", (dialog, which) -> {
                    // Single press dismisses immediately
                    gpsDialog = null;
                })
                .setCancelable(true)  // === FIX: Allow back press to dismiss too ===
                .setOnCancelListener(dialog -> {
                    gpsDialog = null;
                })
                .show();
    }

    private void dismissGpsDialog() {
        if (gpsDialog != null) {
            try {
                gpsDialog.dismiss();
            } catch (Exception ignored) {}
            gpsDialog = null;
        }
    }

    private void buildPrayerTimes() {
        if (prayerTimesContainer == null) return;
        prayerTimesContainer.removeAllViews();

        PrayerTimesCalculator.PrayerTimes times = PrayerTimesCalculator.getTodayPrayerTimes();
        Calendar now = Calendar.getInstance(TimeZone.getDefault());
        double currentHour = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0;

        int nextPrayerIdx = -1;
        double[] prayerHours = {times.fajr, times.dhuhr, times.asr, times.maghrib, times.isha};

        for (int i = 0; i < 5; i++) {
            if (currentHour < prayerHours[i]) {
                nextPrayerIdx = i;
                break;
            }
        }
        if (nextPrayerIdx == -1) nextPrayerIdx = 0;

        String[] prayerTimes = {
            times.getFajrStr(), times.getDhuhrStr(), times.getAsrStr(),
            times.getMaghribStr(), times.getIshaStr()
        };

        for (int i = 0; i < 5; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(12, 10, 12, 10);

            boolean isNext = (i == nextPrayerIdx);
            if (isNext) {
                row.setBackgroundColor(0x20D4AF37);
            }

            // Prayer icon circle
            View iconCircle = new View(this);
            int iconSize = (int) (28 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.setMargins(0, 0, 12, 0);
            iconCircle.setLayoutParams(iconParams);
            iconCircle.setBackground(createCircleDrawable(PRAYER_COLORS[i]));

            // Prayer name
            TextView tvName = new TextView(this);
            tvName.setText(PRAYER_NAMES_AR[i]);
            tvName.setTextSize(16);
            tvName.setTextColor(isNext ? 0xFFD4AF37 : 0xFFE0E1DD);
            tvName.getPaint().setFakeBoldText(isNext);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameParams);

            // Time
            TextView tvTime = new TextView(this);
            tvTime.setText(prayerTimes[i]);
            tvTime.setTextSize(14);
            tvTime.setTextColor(isNext ? 0xFFD4AF37 : 0xFF778DA9);
            tvTime.getPaint().setFakeBoldText(isNext);

            // Next indicator
            if (isNext) {
                tvTime.append(" \u0627\u0644\u0642\u0627\u062F\u0645\u0629");
            }

            row.addView(iconCircle);
            row.addView(tvName);
            row.addView(tvTime);

            // Separator
            if (i < 4) {
                View separator = new View(this);
                separator.setBackgroundColor(0xFF1E293B);
                LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                );
                sepParams.setMargins(48, 0, 0, 0);
                separator.setLayoutParams(sepParams);
                prayerTimesContainer.addView(row);
                prayerTimesContainer.addView(separator);
            } else {
                prayerTimesContainer.addView(row);
            }
        }
    }

    private android.graphics.drawable.GradientDrawable createCircleDrawable(int color) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }
}