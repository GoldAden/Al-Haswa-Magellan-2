package app.organicmaps.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Moon phase calculator for Al-Haswa
 * Uses synodic month calculation for accurate phase prediction
 */
public class MoonCalculator {

    private static final double LAT = 19.4431;
    private static final double LON = 40.5167;

    // Known new moon: Jan 6 2000 18:14 UTC
    private static final long REF_NEW_MOON = 947182440000L;
    static final double SYNODIC_MONTH = 29.53058867;

    /**
     * Get lunar age (0 to ~29.53 days)
     */
    public static double getLunarAge(long timeMillis) {
        double daysSince = (timeMillis - REF_NEW_MOON) / 86400000.0;
        double age = daysSince % SYNODIC_MONTH;
        if (age < 0) age += SYNODIC_MONTH;
        return age;
    }

    /**
     * Get illumination fraction (0.0 to 1.0)
     */
    public static double getIllumination(long timeMillis) {
        double age = getLunarAge(timeMillis);
        // Illumination follows a cosine curve peaking at full moon (age ~14.76)
        return 0.5 * (1.0 - Math.cos(2.0 * Math.PI * age / SYNODIC_MONTH));
    }

    /**
     * Get phase name in Arabic
     */
    public static String getPhaseNameArabic(double age) {
        if (age < 1.85) return "محاق";           // New Moon
        if (age < 5.53) return "هلال متزايد";     // Waxing Crescent
        if (age < 9.22) return "تربيع أول";       // First Quarter
        if (age < 12.91) return "أحدب متزايد";     // Waxing Gibbous
        if (age < 16.61) return "بدر";            // Full Moon
        if (age < 20.30) return "أحدب متناقص";    // Waning Gibbous
        if (age < 23.99) return "تربيع أخير";      // Last Quarter
        if (age < 27.68) return "هلال متناقص";    // Waning Crescent
        return "محاق";                             // New Moon
    }

    /**
     * Get phase index (0-7) for drawing
     */
    public static int getPhaseIndex(double age) {
        if (age < 1.85) return 0;
        if (age < 5.53) return 1;
        if (age < 9.22) return 2;
        if (age < 12.91) return 3;
        if (age < 16.61) return 4;
        if (age < 20.30) return 5;
        if (age < 23.99) return 6;
        if (age < 27.68) return 7;
        return 0;
    }

    /**
     * Approximate moonrise time for Al-Haswa
     * Moonrise roughly = (lunar age / 29.53) * 24 + 6 hours (sunset reference)
     */
    public static String getMoonriseTime(long timeMillis) {
        double age = getLunarAge(timeMillis);
        // Moonrise shifts ~50 min later each day
        double baseHour = 6.0 + (age / SYNODIC_MONTH) * 24.0;
        // For new moon: rises with sun (~6:00), full moon: rises at sunset (~18:00)
        baseHour = baseHour % 24.0;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        cal.setTimeInMillis(timeMillis);
        cal.set(Calendar.HOUR_OF_DAY, (int) baseHour);
        cal.set(Calendar.MINUTE, (int) ((baseHour % 1.0) * 60));
        return formatTimeAmPm(cal);
    }

    /**
     * Approximate moonset time for Al-Haswa
     */
    public static String getMoonsetTime(long timeMillis) {
        double age = getLunarAge(timeMillis);
        double baseHour = 18.0 + (age / SYNODIC_MONTH) * 24.0;
        baseHour = baseHour % 24.0;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        cal.setTimeInMillis(timeMillis);
        cal.set(Calendar.HOUR_OF_DAY, (int) baseHour);
        cal.set(Calendar.MINUTE, (int) ((baseHour % 1.0) * 60));
        return formatTimeAmPm(cal);
    }

    /**
     * Format time as hh:mm AM/PM
     */
    public static String formatTimeAmPm(Calendar cal) {
        int hour = cal.get(Calendar.HOUR);
        if (hour == 0) hour = 12;
        int minute = cal.get(Calendar.MINUTE);
        String ampm = cal.get(Calendar.AM_PM) == Calendar.AM ? "ص" : "م";
        return String.format("%d:%02d %s", hour, minute, ampm);
    }

    /**
     * Get 7-day moon predictions
     */
    public static List<MoonDayInfo> getWeeklyMoonInfo() {
        List<MoonDayInfo> info = new ArrayList<>();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        for (int d = 0; d < 7; d++) {
            MoonDayInfo day = new MoonDayInfo();
            day.date = (Calendar) cal.clone();
            day.age = getLunarAge(cal.getTimeInMillis());
            day.illumination = getIllumination(cal.getTimeInMillis());
            day.phaseName = getPhaseNameArabic(day.age);
            day.phaseIndex = getPhaseIndex(day.age);
            day.moonrise = getMoonriseTime(cal.getTimeInMillis());
            day.moonset = getMoonsetTime(cal.getTimeInMillis());
            info.add(day);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return info;
    }

    public static class MoonDayInfo {
        public Calendar date;
        public double age;
        public double illumination;
        public String phaseName;
        public int phaseIndex;
        public String moonrise;
        public String moonset;
    }
}