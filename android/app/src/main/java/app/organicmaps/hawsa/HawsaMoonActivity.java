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
import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import app.organicmaps.R;

/**
 * Moon phase screen with beautiful animated phases, current phase info,
 * rise/set times, and Hijri date.
 * Pressing the moon card from HawsaMainActivity opens this.
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

    // Arabic day abbreviations
    private static final String[] DAY_NAMES_AR = {
        "\u0623\u062D", "\u0625\u062B", "\u062B\u0644", "\u0623\u0631",
        "\u062E\u0645", "\u062C\u0645", "\u0633\u0628"
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(COLOR_BG);
            getWindow().setNavigationBarColor(COLOR_BG);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Use Riyadh timezone for all calculations
        TimeZone riyadhTz = TimeZone.getTimeZone("Asia/Riyadh");
        Calendar now = Calendar.getInstance(riyadhTz);
        long timeMillis = now.getTimeInMillis();

        // Pre-compute moon data using the actual MoonCalculator API
        double age = MoonCalculator.getLunarAge(timeMillis);
        double illum = MoonCalculator.getIllumination(timeMillis);
        float phase = (float) (age / MoonCalculator.SYNODIC_MONTH); // 0.0–1.0
        float illumF = (float) illum;
        String phaseName = MoonCalculator.getPhaseNameArabic(age);
        String moonrise = MoonCalculator.getMoonriseTime(timeMillis);
        String moonset = MoonCalculator.getMoonsetTime(timeMillis);

        // Hijri date using the actual HijriDateCalculator API
        HijriDateCalculator.HijriDate hijriDate = HijriDateCalculator.calculateHijri(now);
        String hijriStr = HijriDateCalculator.formatHijriArabic(hijriDate);

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
        moonPhaseView.setPhase(phase, illumF);
        rootLayout.addView(moonPhaseView);

        // === Current Phase Info Card ===
        LinearLayout infoCard = createCard();

        // Phase name
        TextView tvPhaseName = new TextView(this);
        tvPhaseName.setText(phaseName);
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

        // Rise / Set times
        LinearLayout timesGrid = new LinearLayout(this);
        timesGrid.setOrientation(LinearLayout.HORIZONTAL);
        timesGrid.setGravity(Gravity.CENTER);
        timesGrid.setPadding(0, dp(12), 0, dp(8));

        timesGrid.addView(createTimeColumn("\u0627\u0644\u0634\u0631\u0648\u0642", moonrise));
        timesGrid.addView(createTimeColumn("\u0627\u0644\u063A\u0631\u0648\u0628", moonset));

        infoCard.addView(timesGrid);

        // Separator
        infoCard.addView(createSeparator());

        // Hijri date
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

        // Use getWeeklyMoonInfo() which returns List<MoonDayInfo>
        List<MoonCalculator.MoonDayInfo> weeklyInfo = MoonCalculator.getWeeklyMoonInfo();

        LinearLayout weeklyStrip = new LinearLayout(this);
        weeklyStrip.setOrientation(LinearLayout.HORIZONTAL);
        weeklyStrip.setGravity(Gravity.CENTER);
        weeklyStrip.setPadding(0, 0, 0, dp(4));

        for (int i = 0; i < weeklyInfo.size(); i++) {
            MoonCalculator.MoonDayInfo dayInfo = weeklyInfo.get(i);
            float dayPhase = (float) (dayInfo.age / MoonCalculator.SYNODIC_MONTH);
            float dayIllum = (float) dayInfo.illumination;
            weeklyStrip.addView(createDayMoonIcon(dayPhase, dayIllum, i == 0));
        }

        weeklyCard.addView(weeklyStrip);

        // Day labels under strip
        LinearLayout dayLabels = new LinearLayout(this);
        dayLabels.setOrientation(LinearLayout.HORIZONTAL);
        dayLabels.setGravity(Gravity.CENTER);

        for (int i = 0; i < weeklyInfo.size(); i++) {
            MoonCalculator.MoonDayInfo dayInfo = weeklyInfo.get(i);
            int dow = dayInfo.date.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
            if (dow < 0) dow = 0;
            if (dow > 6) dow = 6;
            TextView lbl = new TextView(this);
            lbl.setText(DAY_NAMES_AR[dow]);
            lbl.setTextSize(11);
            lbl.setTextColor(i == 0 ? COLOR_GOLD : COLOR_TEXT_DIM);
            lbl.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lbl.setLayoutParams(lp);
            dayLabels.addView(lbl);
        }

        weeklyCard.addView(dayLabels);
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
     * Create a small realistic moon phase icon for the weekly strip.
     * Uses proper terminator geometry (elliptical approximation) to show
     * crescents, quarter moons, gibbous, and full/new phases realistically.
     */
    private View createDayMoonIcon(final float phaseFraction, final float illumination, final boolean isToday) {
        View icon = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                float cx = w / 2f;
                float cy = h / 2f;
                float r = Math.min(w, h) / 2f - dp(4);

                Paint litPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                litPaint.setColor(isToday ? 0xFFF1F5F9 : 0xFF94A3B8);
                litPaint.setStyle(Paint.Style.FILL);

                Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                darkPaint.setColor(0xFF1E293B);
                darkPaint.setStyle(Paint.Style.FILL);

                // Draw full dark circle first (shadow side)
                canvas.drawCircle(cx, cy, r, darkPaint);

                // Draw the lit portion using proper terminator geometry
                // phaseFraction: 0 = new moon, 0.5 = full moon, 1 = back to new
                double terminatorX;
                boolean isWaxing = phaseFraction <= 0.5;

                if (isWaxing) {
                    // terminatorX goes from -r (new) to +r (full)
                    terminatorX = r * (2.0 * phaseFraction - 1.0);
                } else {
                    // waning: terminatorX goes from +r (full) to -r (new)
                    double wanePhase = phaseFraction - 0.5;
                    terminatorX = r * (1.0 - 2.0 * wanePhase);
                }

                if (illumination >= 0.99) {
                    // Full moon — draw full lit circle
                    canvas.drawCircle(cx, cy, r, litPaint);
                } else if (illumination <= 0.01) {
                    // New moon — already dark, nothing more to draw
                } else {
                    // Draw lit crescent/gibbous using terminator arc + limb arc
                    Path litPath = new Path();
                    litPath.moveTo(cx, cy - r);  // top of moon

                    // Walk along the terminator (elliptical curve from top to bottom)
                    int steps = 40;
                    for (int i = 0; i <= steps; i++) {
                        float angle = 90f - (180f * i / steps);  // 90 to -90
                        float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                        float xTerminator = cx + (float) (terminatorX * Math.cos(Math.toRadians(angle)));
                        litPath.lineTo(xTerminator, y);
                    }

                    // Walk along the limb (outer edge) back from bottom to top
                    for (int i = 0; i <= steps; i++) {
                        float angle = -90f + (180f * i / steps);  // -90 to 90
                        float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                        float xLimb;
                        if (isWaxing) {
                            xLimb = cx + r * (float) Math.cos(Math.toRadians(angle));  // right limb
                        } else {
                            xLimb = cx - r * (float) Math.cos(Math.toRadians(angle));  // left limb
                        }
                        litPath.lineTo(xLimb, y);
                    }

                    litPath.close();
                    canvas.drawPath(litPath, litPaint);
                }

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
