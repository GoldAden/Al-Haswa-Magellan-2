package app.organicmaps.hawsa;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Moon phase calculator with Hijri date support.
 * Uses Jean Meeus astronomical algorithms for moon phase calculation.
 */
public class MoonCalculator {

    // Moon phase names in Arabic
    private static final String[] PHASE_NAMES_AR = {
        "محاق",         // New Moon
        "هلال متزايد",   // Waxing Crescent
        "تربع أول",     // First Quarter
        "أحدب متزايد",  // Waxing Gibbous
        "بدر",          // Full Moon
        "أحدب متناقص",  // Waning Gibbous
        "تريع آخر",     // Last Quarter
        "هلال متناقص"   // Waning Crescent
    };

    /**
     * Get moon illumination percentage (0-100)
     */
    public static double getIllumination(Calendar date) {
        double phase = getPhaseValue(date);
        // Illumination based on phase angle
        return (1 - Math.cos(Math.toRadians(phase * 360))) / 2.0 * 100;
    }

    /**
     * Get moon phase as value 0.0 - 1.0
     * 0.0 = New Moon, 0.5 = Full Moon, 1.0 = next New Moon
     */
    public static double getPhaseValue(Calendar date) {
        // Convert to Julian Day
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);

        double jd = toJulianDay(year, month, day + 0.5);

        // Days since Jan 6 2000 (known new moon)
        double daysSinceRef = jd - 2451550.1;

        // Synodic month = 29.53058867
        double synodicMonth = 29.53058867;
        double phase = (daysSinceRef % synodicMonth) / synodicMonth;
        if (phase < 0) phase += 1.0;

        return phase;
    }

    /**
     * Get Arabic name of current moon phase
     */
    public static String getPhaseNameAr(Calendar date) {
        double phase = getPhaseValue(date);
        int phaseIndex;

        if (phase < 0.0625) phaseIndex = 0;         // New Moon
        else if (phase < 0.1875) phaseIndex = 1;    // Waxing Crescent
        else if (phase < 0.3125) phaseIndex = 2;    // First Quarter
        else if (phase < 0.4375) phaseIndex = 3;    // Waxing Gibbous
        else if (phase < 0.5625) phaseIndex = 4;    // Full Moon
        else if (phase < 0.6875) phaseIndex = 5;    // Waning Gibbous
        else if (phase < 0.8125) phaseIndex = 6;    // Last Quarter
        else if (phase < 0.9375) phaseIndex = 7;    // Waning Crescent
        else phaseIndex = 0;                        // New Moon

        return PHASE_NAMES_AR[phaseIndex];
    }

    /**
     * Get moon rise and set times approximation
     */
    public static String[] getRiseSetTimes(Calendar date) {
        double phase = getPhaseValue(date);

        // Approximate moonrise/moonset based on phase
        // New Moon: rises ~6am, sets ~6pm
        // Full Moon: rises ~6pm, sets ~6am
        double riseHour = 6.0 + phase * 24.0;
        double setHour = 18.0 + phase * 24.0;

        if (riseHour >= 24) riseHour -= 24;
        if (setHour >= 24) setHour -= 24;

        return new String[]{
            formatTime(riseHour),
            formatTime(setHour)
        };
    }

    /**
     * Get Hijri (Islamic) date for the given calendar date
     */
    public static HijriDateCalculator.HijriDate getHijriDate(Calendar date) {
        return HijriDateCalculator.getHijriForDate(date);
    }

    /**
     * Get today's Hijri date
     */
    public static HijriDateCalculator.HijriDate getTodayHijriDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        return HijriDateCalculator.getTodayHijri();
    }

    /**
     * Get formatted Hijri date string in Arabic
     */
    public static String getTodayHijriDateStr() {
        return HijriDateCalculator.formatHijriArabic(getTodayHijriDate());
    }

    /**
     * Get moon age in days (0 - 29.53)
     */
    public static double getMoonAge(Calendar date) {
        double phase = getPhaseValue(date);
        return phase * 29.53058867;
    }

    private static double toJulianDay(int year, int month, double day) {
        if (month <= 2) { year--; month += 12; }
        long A = year / 100;
        long B = 2 - A + A / 4;
        return (long)(365.25 * (year + 4716)) + (long)(30.6001 * (month + 1)) + day + B - 1524.5;
    }

    private static String formatTime(double decimalHour) {
        if (decimalHour < 0) decimalHour += 24;
        if (decimalHour >= 24) decimalHour -= 24;

        int h = (int) Math.floor(decimalHour);
        int m = (int) Math.round((decimalHour - h) * 60);
        if (m >= 60) { h++; m -= 60; }
        if (h >= 24) h -= 24;

        String ampm = h < 12 ? "ص" : "م";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;

        return String.format(java.util.Locale.US, "%d:%02d %s", h12, m, ampm);
    }
}