package app.organicmaps.hawsa;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
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
        rootLayout.addView(moonParams == moonParams ? moonPhaseView : moonPhaseView);

        // === Current Phase Info Card ===
        LinearLayout infoCard = createCard();

        // Phase name
        TextView tvPhaseName = new TextView(this);
        tvPhaseName.setText(getCurrentPhaseNameArabic());
        tvPhaseName.setTextSize(20);
        tvPhaseName.setTextColor(COLOR_GOLD);
        tvPhaseName.setTypeface(null, Typeface.BOLD);
        tvPhaseName.setGravity(Gravity.CENTER);
        tvPhaseName.setPadding(0, dp(16), 0, dp(4));
        infoCard.addView(tvPhaseName);

        // Illumination
        double illum = MoonCalculator.getMoonIllumination();
        TextView tvIllumination = new TextView(this);
        tvIllumination.setText(String.format(Locale.US, "\u0627\u0644\u0625\u0636\u0627\u0621\u0629: %.0f%%", illum * 100));
        tvIllumination.setTextSize(16);
        tvIllumination.setTextColor(COLOR_TEXT);
        tvIllumination.setGravity(Gravity.CENTER);
        tvIllumination.setPadding(0, dp(4), 0, dp(8));
        infoCard.addView(tvIllumination);

        // Moon age
        double age = MoonCalculator.getMoonAge();
        TextView tvAge = new TextView(this);
        tvAge.setText(String.format(Locale.US, "\u0639\u0645\u0631 \u0627\u0644\u0642\u0645\u0631: %.1f \u064A\u0648\u0645", age));
        tvAge.setTextSize(14);
        tvAge.setTextColor(COLOR_TEXT_DIM);
        tvAge.setGravity(Gravity.CENTER);
        tvAge.setPadding(0, 0, 0, dp(12));
        infoCard.addView(tvAge);

        // Separator
        infoCard.addView(createSeparator());

        // Rise / Set / Transit times
        LinearLayout timesGrid = new LinearLayout(this);
        timesGrid.setOrientation(LinearLayout.HORIZONTAL);
        timesGrid.setGravity(Gravity.CENTER);
        timesGrid.setPadding(0, dp(12), 0, dp(8));

        timesGrid.addView(createTimeColumn("\u0627\u0644\u0634\u0631\u0648\u0642", MoonCalculator.getMoonRiseTimeStr()));
        timesGrid.addView(createTimeColumn("\u0627\u0644\u063A\u0631\u0648\u0628", MoonCalculator.getMoonSetTimeStr()));

        infoCard.addView(timesGrid);

        // Separator
        infoCard.addView(createSeparator());

        // Hijri date
        String hijriStr = HijriDateCalculator.getHijriDateStr();
        TextView tvHijri = new TextView(this);
        tvHijri.setText("\u0627\u0644\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u0647\u062C\u0631\u064A: " + hijriStr);
        tvHijri.setTextSize(16);
        tvHijri.setTextColor(COLOR_TEXT);
        tvHijri.setGravity(Gravity.CENTER);
        tvHijri.setTypeface(null, Typeface.BOLD);
        tvHijri.setPadding(0, dp(12), 0, dp(8));
        infoCard.addView(tvHijri);

        // Gregorian date
        Calendar today = Calendar.getInstance(TimeZone.getDefault());
        SimpleDateFormat gregFmt = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("ar"));
        TextView tvGregorian = new TextView(this);
        tvGregorian.setText("\u0627\u0644\u062A\u0627\u0631\u064A\u062E \u0627\u0644\u0645\u064A\u0644\u0627\u062F\u064A: " + gregFmt.format(today.getTime()));
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

        LinearLayout weeklyStrip = new LinearLayout(this);
        weeklyStrip.setOrientation(LinearLayout.HORIZONTAL);
        weeklyStrip.setGravity(Gravity.CENTER);
        weeklyStrip.setPadding(0, 0, 0, dp(4));

        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        for (int i = 0; i < 7; i++) {
            MoonCalculator.MoonDayData dayData = MoonCalculator.getDayData(cal);
            weeklyStrip.addView(createDayMoonIcon(dayData.phase, dayData.illumination, i == 0));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        weeklyCard.addView(weeklyStrip);

        // Day labels under strip
        LinearLayout dayLabels = new LinearLayout(this);
        dayLabels.setOrientation(LinearLayout.HORIZONTAL);
        dayLabels.setGravity(Gravity.CENTER);

        String[] dayNamesAr = {"\u0623\u062D", "\u0625\u062B", "\u062B\u0644", "\u0623\u0631", "\u062E\u0645", "\u062C\u0645", "\u0633\u0628"};
        Calendar calLabels = Calendar.getInstance(TimeZone.getDefault());
        int startDow = calLabels.get(Calendar.DAY_OF_WEEK);
        for (int i = 0; i < 7; i++) {
            int dow = (startDow - 1 + i) % 7;
            TextView lbl = new TextView(this);
            lbl.setText(dayNamesAr[dow]);
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

        // Set initial phase into the animated view
        float currentPhase = (float) MoonCalculator.getMoonPhase();
        float currentIllum = (float) MoonCalculator.getMoonIllumination();
        moonPhaseView.setPhase(currentPhase, currentIllum);
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

    private View createDayMoonIcon(float phase, float illumination, boolean isToday) {
        // Small moon icon — draw using a custom mini view
        View icon = new View(this) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
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

                // Full circle lit
                canvas.drawCircle(cx, cy, r, litPaint);

                // Dark overlay for phase
                Path darkPath = new Path();
                // Simplified terminator for small icon
                float p = phase;
                float curveFactor;
                if (p <= 0.5f) {
                    curveFactor = 1 - 2 * p;
                } else {
                    curveFactor = 2 * p - 1;
                }

                darkPath.moveTo(cx, cy - r);
                for (float angle = 90; angle >= -90; angle -= 10) {
                    float y = cy + r * (float) Math.sin(Math.toRadians(angle));
                    float xOffset;
                    if (p <= 0.5f) {
                        xOffset = r * curveFactor * (float) Math.cos(Math.toRadians(angle));
                        float x = cx + xOffset;
                        darkPath.lineTo(x, y);
                    } else {
                        xOffset = r * curveFactor * (float) Math.cos(Math.toRadians(angle));
                        float x = cx - xOffset;
                        darkPath.lineTo(x, y);
                    }
                }
                darkPath.close();
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

    private String getCurrentPhaseNameArabic() {
        double phase = MoonCalculator.getMoonPhase();
        double age = MoonCalculator.getMoonAge();

        if (age < 1.85) return "\u0645\u062D\u0627\u0642";       // New moon
        if (age < 5.55) return "\u0647\u0644\u0627\u0644 \u0623\u0648\u0644";  // Waxing crescent
        if (age < 9.25) return "\u062A\u0631\u0628\u064A\u0639 \u0623\u0648\u0644"; // First quarter
        if (age < 12.95) return "\u0623\u062C\u0628\u0632 \u0645\u062D\u062F\u0628"; // Waxing gibbous
        if (age < 16.65) return "\u0628\u062F\u0631";          // Full moon
        if (age < 20.35) return "\u0623\u062C\u0628\u0632 \u062A\u0646\u0627\u0642\u0635"; // Waning gibbous
        if (age < 24.05) return "\u062A\u0631\u0628\u064A\u0639 \u0622\u062E\u0631"; // Last quarter
        if (age < 27.75) return "\u0647\u0644\u0627\u0644 \u0622\u062E\u0631"; // Waning crescent
        return "\u0645\u062D\u0627\u0642"; // New moon
    }
}