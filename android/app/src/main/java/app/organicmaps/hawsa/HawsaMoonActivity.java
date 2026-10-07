package app.organicmaps.hawsa;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

/**
 * Moon phase screen with beautiful animated phases, current phase info,
 * accurate rise/set times (Meeus/NOAA astronomical algorithms),
 * Hijri date, and realistic weekly moon icons.
 */
public class HawsaMoonActivity extends AppCompatActivity {

    private MoonPhaseView moonPhaseView;
    private ImageView btnBack;

    // Theme colors
    private static final int COLOR_BG = 0xFF0D1B2A;
    private static final int COLOR_CARD = 0xFF111827;
    private static final int COLOR_GOLD = 0xFFD4AF37;
    private static final int COLOR_TEXT = 0xFFE0E1DD;
    private static final int COLOR_TEXT_DIM = 0xFF778DA9;
    private static final int COLOR_SEPARATOR = 0xFF1E293B;

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

        // === FIX: Status bar color matches background ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BG);
            getWindow().setNavigationBarColor(COLOR_BG);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        }

        // Immersive sticky
        hideSystemUI();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Build the entire layout programmatically for full control
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(COLOR_BG);
        scrollView.setFillViewport(true);
        scrollView.setPadding(0, 0, 0, 0);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(COLOR_BG);
        rootLayout.setPadding(dp(20), dp(50), dp(20), dp(30));
        rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // === Back button + Title ===
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(16));

        btnBack = new ImageView(this);
        btnBack.setImageResource(R.drawable.ic_hawsa_back);
        btnBack.setColorFilter(COLOR_GOLD);
        btnBack.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        btnBack.setLayoutParams(backParams);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        TextView tvTitle = new TextView(this);
        tvTitle.setText("\u0627\u0644\u0642\u0645\u0631");
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(COLOR_GOLD);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(titleParams);
        tvTitle.setGravity(Gravity.CENTER);

        // Invisible spacer to balance the back button
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

        topBar.addView(btnBack);
        topBar.addView(tvTitle);
        topBar.addView(spacer);
        rootLayout.addView(topBar);

        // === Animated Moon Phase View ===
        moonPhaseView = new MoonPhaseView(this);
        LinearLayout.LayoutParams moonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(280));
        moonPhaseView.setLayoutParams(moonParams);
        rootLayout.addView(moonPhaseView);

        // === Current Phase Info Card ===
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        long timeMillis = now.getTimeInMillis();

        // FIXED: Use correct MoonCalculator API
        double age = MoonCalculator.getLunarAge(timeMillis);
        double illum = MoonCalculator.getIllumination(timeMillis);
        // Phase fraction: 0=new, 0.5=full, 1.0=new again
        float phaseFraction = (float)(age / MoonCalculator.SYNODIC_MONTH);

        // Set phase into animated view
        if (moonPhaseView != null) {
            moonPhaseView.setPhase(phaseFraction, (float)illum);
        }

        LinearLayout infoCard = createCard();

        // Phase name — use MoonCalculator.getPhaseNameArabic()
        TextView tvPhaseName = new TextView(this);
        tvPhaseName.setText(MoonCalculator.getPhaseNameArabic(age));
        tvPhaseName.setTextSize(20);
        tvPhaseName.setTextColor(COLOR_GOLD);
        tvPhaseName.setTypeface(null, Typeface.BOLD);
        tvPhaseName.setGravity(Gravity.CENTER);
        tvPhaseName.setPadding(0, dp(16), 0, dp(4));
        infoCard.addView(tvPhaseName);

        // Illumination
        TextView tvIllumination = new TextView(this);
        tvIllumination.setText(String.format(Locale.US, "\u0627\u0644\u0625\u0636\u0627\u0621\u0629: %.0f%%", illum * 100));
        tvIllumination.setTextSize(16);
        tvIllumination.setTextColor(COLOR_TEXT);
        tvIllumination.setGravity(Gravity.CENTER);
        tvIllumination.setPadding(0, dp(4), 0, dp(8));
        infoCard.addView(tvIllumination);

        // Moon age
        TextView tvAge = new TextView(this);
        tvAge.setText(String.format(Locale.US, "\u0639\u0645\u0631 \u0627\u0644\u0642\u0645\u0631: %.1f \u064A\u0648\u0645", age));
        tvAge.setTextSize(14);
        tvAge.setTextColor(COLOR_TEXT_DIM);
        tvAge.setGravity(Gravity.CENTER);
        tvAge.setPadding(0, 0, 0, dp(12));
        infoCard.addView(tvAge);

        // Separator
        infoCard.addView(createSeparator());

        // Rise / Set times — FIXED: Use MoonCalculator.getMoonriseTime/getMoonsetTime
        LinearLayout timesGrid = new LinearLayout(this);
        timesGrid.setOrientation(LinearLayout.HORIZONTAL);
        timesGrid.setGravity(Gravity.CENTER);
        timesGrid.setPadding(0, dp(12), 0, dp(8));

        String riseTime = MoonCalculator.getMoonriseTime(timeMillis);
        String setTime = MoonCalculator.getMoonsetTime(timeMillis);

        timesGrid.addView(createTimeColumn("\u0627\u0644\u0637\u0644\u0648\u0639", riseTime));
        timesGrid.addView(createTimeColumn("\u0627\u0644\u063A\u0631\u0648\u0628", setTime));

        infoCard.addView(timesGrid);

        // Separator
        infoCard.addView(createSeparator());

        // Hijri date — FIXED: Use HijriDateCalculator.calculateHijri() + formatHijriArabic()
        HijriDateCalculator.HijriDate hijriDate = HijriDateCalculator.calculateHijri(now);
        String hijriStr = HijriDateCalculator.formatHijriArabic(hijriDate);

        TextView tvHijri = new TextView(this);
        tvHijri.setText("\u0627\u0644\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u0647\u062C\u0631\u064A: " + hijriStr);
        tvHijri.setTextSize(16);
        tvHijri.setTextColor(COLOR_TEXT);
        tvHijri.setGravity(Gravity.CENTER);
        tvHijri.setTypeface(null, Typeface.BOLD);
        tvHijri.setPadding(0, dp(12), 0, dp(8));
        infoCard.addView(tvHijri);

        // Gregorian date
        SimpleDateFormat gregFmt = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("ar"));
        TextView tvGregorian = new TextView(this);
        tvGregorian.setText("\u0627\u0644\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u0645\u064A\u0644\u0627\u062F\u064A: " + gregFmt.format(now.getTime()));
        tvGregorian.setTextSize(13);
        tvGregorian.setTextColor(COLOR_TEXT_DIM);
        tvGregorian.setGravity(Gravity.CENTER);
        tvGregorian.setPadding(0, dp(2), 0, dp(12));
        infoCard.addView(tvGregorian);

        rootLayout.addView(infoCard);

        // === Weekly Phase Strip ===
        LinearLayout weeklyCard = createCard();
        weeklyCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView tvWeeklyTitle = new TextView(this);
        tvWeeklyTitle.setText("\u0623\u0637\u0648\u0627\u0631 \u0627\u0644\u0642\u0645\u0631 \u0647\u0630\u0627 \u0627\u0644\u0623\u0633\u0628\u0648\u0639");
        tvWeeklyTitle.setTextSize(15);
        tvWeeklyTitle.setTextColor(COLOR_GOLD);
        tvWeeklyTitle.setTypeface(null, Typeface.BOLD);
        tvWeeklyTitle.setPadding(0, 0, 0, dp(12));
        weeklyCard.addView(tvWeeklyTitle);

        // FIXED: Use MoonCalculator.getWeeklyMoonInfo() returning List<MoonDayInfo>
        List<MoonCalculator.MoonDayInfo> weekly = MoonCalculator.getWeeklyMoonInfo();

        LinearLayout weeklyStrip = new LinearLayout(this);
        weeklyStrip.setOrientation(LinearLayout.HORIZONTAL);
        weeklyStrip.setGravity(Gravity.CENTER);
        weeklyStrip.setPadding(0, 0, 0, dp(4));

        for (int i = 0; i < weekly.size() && i < 7; i++) {
            MoonCalculator.MoonDayInfo dayInfo = weekly.get(i);
            float dayPhase = (float)(dayInfo.age / MoonCalculator.SYNODIC_MONTH);
            float dayIllum = (float)dayInfo.illumination;
            boolean isToday = (i == 0);
            weeklyStrip.addView(createRealisticDayMoonIcon(dayPhase, dayIllum, isToday));
        }

        weeklyCard.addView(weeklyStrip);

        // Day labels under strip
        LinearLayout dayLabels = new LinearLayout(this);
        dayLabels.setOrientation(LinearLayout.HORIZONTAL);
        dayLabels.setGravity(Gravity.CENTER);

        String[] shortDayNames = {"\u0623\u062D", "\u0625\u062B", "\u062B\u0644", "\u0623\u0631", "\u062E\u0645", "\u062C\u0645", "\u0633\u0628"};

        for (int i = 0; i < weekly.size() && i < 7; i++) {
            MoonCalculator.MoonDayInfo dayInfo = weekly.get(i);
            int dow = dayInfo.date.get(Calendar.DAY_OF_WEEK) - 1;
            TextView lbl = new TextView(this);
            lbl.setText(shortDayNames[dow]);
            lbl.setTextSize(11);
            lbl.setTextColor(i == 0 ? COLOR_GOLD : COLOR_TEXT_DIM);
            lbl.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lbl.setLayoutParams(lp);
            dayLabels.addView(lbl);
        }

        weeklyCard.addView(dayLabels);

        // Weekly details with rise/set times
        LinearLayout weeklyDetails = new LinearLayout(this);
        weeklyDetails.setOrientation(LinearLayout.VERTICAL);
        weeklyDetails.setPadding(0, dp(12), 0, 0);

        String[] arabicDays = {"\u0627\u0644\u0623\u062D\u062F", "\u0627\u0644\u0627\u062B\u0646\u064A\u0646", "\u0627\u0644\u062B\u0644\u0627\u062B\u0627\u0621", "\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621", "\u0627\u0644\u062E\u0645\u064A\u0633", "\u0627\u0644\u062C\u0645\u0639\u0629", "\u0627\u0644\u0633\u0628\u062A"};

        for (int i = 0; i < weekly.size() && i < 7; i++) {
            MoonCalculator.MoonDayInfo dayInfo = weekly.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            if (i == 0) {
                row.setBackgroundColor(0x15D4AF37);
            }

            // Day name
            int dow = dayInfo.date.get(Calendar.DAY_OF_WEEK) - 1;
            TextView tvDayName = new TextView(this);
            tvDayName.setText(arabicDays[dow]);
            tvDayName.setTextSize(14);
            tvDayName.setTextColor(i == 0 ? COLOR_GOLD : COLOR_TEXT);
            tvDayName.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvDayName.setLayoutParams(nameParams);
            row.addView(tvDayName);

            // Phase name + illumination
            TextView tvPhaseInfo = new TextView(this);
            tvPhaseInfo.setText(String.format(Locale.US, "%s \u00B7 %.0f%%", dayInfo.phaseName, dayInfo.illumination * 100));
            tvPhaseInfo.setTextSize(11);
            tvPhaseInfo.setTextColor(COLOR_TEXT_DIM);
            LinearLayout.LayoutParams phaseParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvPhaseInfo.setLayoutParams(phaseParams);
            tvPhaseInfo.setGravity(Gravity.CENTER);
            row.addView(tvPhaseInfo);

            // Rise/Set
            TextView tvTimes = new TextView(this);
            String timesStr = String.format(Locale.US, "\u0637 %s | \u063A %s", dayInfo.moonrise, dayInfo.moonset);
            tvTimes.setText(timesStr);
            tvTimes.setTextSize(11);
            tvTimes.setTextColor(i == 0 ? 0xFFD4AF37 : 0xFFCE93D8);
            LinearLayout.LayoutParams timesParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvTimes.setLayoutParams(timesParams);
            tvTimes.setGravity(Gravity.CENTER);
            row.addView(tvTimes);

            weeklyDetails.addView(row);

            if (i < 6) {
                View sep = new View(this);
                sep.setBackgroundColor(COLOR_SEPARATOR);
                LinearLayout.LayoutParams sepP = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                sep.setLayoutParams(sepP);
                weeklyDetails.addView(sep);
            }
        }

        weeklyCard.addView(weeklyDetails);
        rootLayout.addView(weeklyCard);

        // === Designer credit ===
        TextView tvCredit = new TextView(this);
        tvCredit.setText("\u062A\u0645 \u062A\u0635\u0645\u064A\u0645 \u0647\u0630\u0627 \u0627\u0644\u062A\u0637\u0628\u064A\u0642 \u0645\u0646 \u0642\u0628\u0644 \u0623\u0635\u064A\u0644 \u0635\u0627\u062F\u0642");
        tvCredit.setTextSize(10);
        tvCredit.setTextColor(0xFF415A77);
        tvCredit.setGravity(Gravity.CENTER);
        tvCredit.setPadding(0, dp(20), 0, 0);
        rootLayout.addView(tvCredit);

        scrollView.addView(rootLayout);
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (moonPhaseView != null) {
            moonPhaseView.startAllAnimations();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (moonPhaseView != null) {
            moonPhaseView.stopAllAnimations();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    // ===== Helpers =====

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_CARD);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFF1E293B);
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, 0);
        card.setLayoutParams(params);

        return card;
    }

    private View createSeparator() {
        View sep = new View(this);
        sep.setBackgroundColor(COLOR_SEPARATOR);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        sepParams.setMargins(dp(32), 0, dp(32), 0);
        sep.setLayoutParams(sepParams);
        return sep;
    }

    private LinearLayout createTimeColumn(String label, String timeStr) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(8), 0, dp(8), 0);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(colParams);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12);
        tvLabel.setTextColor(COLOR_TEXT_DIM);
        tvLabel.setGravity(Gravity.CENTER);
        col.addView(tvLabel);

        TextView tvTime = new TextView(this);
        tvTime.setText(timeStr);
        tvTime.setTextSize(18);
        tvTime.setTextColor(COLOR_GOLD);
        tvTime.setTypeface(null, Typeface.BOLD);
        tvTime.setGravity(Gravity.CENTER);
        col.addView(tvTime);

        return col;
    }

    /**
     * Create a realistic moon icon for the weekly strip using the same
     * terminator algorithm as MoonPhaseView (not half-and-half).
     *
     * Phase mapping (same as MoonPhaseView.drawTerminator):
     *   0.00 = new moon (all dark)
     *   0.25 = first quarter (right half lit)
     *   0.50 = full moon (all lit)
     *   0.75 = last quarter (left half lit)
     *   1.00 = new moon (all dark)
     *
     * Waxing (0-0.5): right side lit, dark moves from right to left
     * Waning (0.5-1): left side lit, dark moves from left to right
     */
    @SuppressLint("ViewConstructor")
    private View createRealisticDayMoonIcon(float phase, float illumination, boolean isToday) {
        View icon = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                float cx = w / 2f;
                float cy = h / 2f;
                float r = Math.min(w, h) / 2f - dp(4);

                // Moon lit base
                Paint litPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                litPaint.setColor(isToday ? 0xFFF1F5F9 : 0xFF94A3B8);
                litPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, r, litPaint);

                // Dark overlay using realistic terminator
                Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                darkPaint.setColor(0xFF1E293B);
                darkPaint.setStyle(Paint.Style.FILL);

                Path darkPath = new Path();
                float p = phase;  // 0=new, 0.5=full, 1=new

                if (p <= 0.5f) {
                    // Waxing: right side lit, dark moves from right to left
                    darkPath.moveTo(cx, cy - r);
                    // Right arc (lit edge of moon)
                    darkPath.arcTo(cx - r, cy - r, cx + r, cy + r, -90, -180, true);
                    // Terminator curve
                    if (p < 0.25f) {
                        // Crescent: terminator on right side of center (concave right)
                        float curveFactor = (0.25f - p) / 0.25f;
                        for (float angle = 90; angle >= -90; angle -= 5) {
                            float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                            float x = cx + r * curveFactor * (float) Math.cos(Math.toRadians(angle));
                            darkPath.lineTo(x, y);
                        }
                    } else {
                        // Gibbous: terminator on left side of center (concave left)
                        float curveFactor = (p - 0.25f) / 0.25f;
                        for (float angle = 90; angle >= -90; angle -= 5) {
                            float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                            float x = cx - r * curveFactor * (float) Math.cos(Math.toRadians(angle));
                            darkPath.lineTo(x, y);
                        }
                    }
                    darkPath.close();
                } else {
                    // Waning: left side lit, dark moves from left to right
                    float wp = p - 0.5f; // 0 at full, 0.5 at new
                    darkPath.moveTo(cx, cy - r);
                    // Left arc (lit edge)
                    darkPath.arcTo(cx - r, cy - r, cx + r, cy + r, -90, 180, false);
                    // Terminator curve
                    if (wp < 0.25f) {
                        // Waning gibbous: terminator on right of center (concave right)
                        float curveFactor = 1 - wp / 0.25f;
                        for (float angle = 90; angle >= -90; angle -= 5) {
                            float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                            float x = cx + r * (1 - curveFactor) * (float) Math.cos(Math.toRadians(angle));
                            darkPath.lineTo(x, y);
                        }
                    } else {
                        // Waning crescent: terminator on left of center (concave left)
                        float curveFactor = 1 - (wp - 0.25f) / 0.25f;
                        for (float angle = 90; angle >= -90; angle -= 5) {
                            float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                            float x = cx - r * curveFactor * (float) Math.cos(Math.toRadians(angle));
                            darkPath.lineTo(x, y);
                        }
                    }
                    darkPath.close();
                }

                canvas.drawPath(darkPath, darkPaint);

                // Highlight ring for today
                if (isToday) {
                    Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    ringPaint.setColor(COLOR_GOLD);
                    ringPaint.setStyle(Paint.Style.STROKE);
                    ringPaint.setStrokeWidth(2f);
                    canvas.drawCircle(cx, cy, r + dp(1), ringPaint);
                }
            }
        };

        int iconSize = dp(40);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                0, iconSize, 1f);
        icon.setLayoutParams(iconParams);
        return icon;
    }
}