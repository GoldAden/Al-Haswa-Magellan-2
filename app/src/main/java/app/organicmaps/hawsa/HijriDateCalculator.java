package app.organicmaps.hawsa;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Islamic (Hijri) lunar calendar date calculator
 * Uses the standard tabular Islamic calendar algorithm.
 * Accuracy: ±1 day compared to actual lunar observation.
 */
public class HijriDateCalculator {

    // Hijri month names in Arabic
    private static final String[] HIJRI_MONTHS_AR = {
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    // Reference: July 16, 622 CE (1 Muharram 1 AH) = Julian Day 1948439
    private static final long HIJRI_EPOCH_JD = 1948439;

    // Mean synodic month length
    private static final double SYNODIC_MONTH = 29.53058867;

    // Mean lunar year length (12 synodic months)
    private static final double LUNAR_YEAR = 354.36667;

    /**
     * Convert Gregorian calendar to Hijri date
     */
    public static HijriDate toHijri(Calendar gregorian) {
        int year = gregorian.get(Calendar.YEAR);
        int month = gregorian.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
        int day = gregorian.get(Calendar.DAY_OF_MONTH);

        long jd = gregorianToJD(year, month, day);
        return jdToHijri(jd);
    }

    /**
     * Gregorian date to Julian Day Number
     */
    private static long gregorianToJD(int year, int month, int day) {
        if (month <= 2) {
            year--;
            month += 12;
        }
        long A = year / 100;
        long B = 2 - A + A / 4;
        return (long)(365.25 * (year + 4716)) + (long)(30.6001 * (month + 1)) + day + B - 1524;
    }

    /**
     * Julian Day Number to Hijri date using standard tabular Islamic calendar algorithm.
     * Uses synodic month division for robust year calculation
     * covering the full range of possible Hijri dates.
     */
    private static HijriDate jdToHijri(long jd) {
        // Adjust for epoch: days since 1 Muharram 1 AH
        long daysSinceEpoch = jd - HIJRI_EPOCH_JD;

        // Estimate the Hijri year using the mean lunar year length
        int hijriYear = (int) Math.floor(daysSinceEpoch / LUNAR_YEAR) + 1;

        // Adjust year: find the year where the remaining days fit
        // First try the estimated year, then adjust up or down if needed
        long dayOfYear = daysSinceEpoch - getDaysBeforeHijriYear(hijriYear - 1);
        while (dayOfYear < 0) {
            hijriYear--;
            dayOfYear = daysSinceEpoch - getDaysBeforeHijriYear(hijriYear - 1);
        }
        while (dayOfYear >= getHijriYearLength(hijriYear)) {
            dayOfYear -= getHijriYearLength(hijriYear);
            hijriYear++;
        }

        return computeMonthAndDay(hijriYear, (int) dayOfYear);
    }

    /**
     * Total days from 1 Muharram 1 AH up to the start of the given Hijri year.
     */
    private static long getDaysBeforeHijriYear(int year) {
        if (year <= 0) return 0;
        int fullCycles = year / 30;
        int remainingYears = year % 30;
        // Each 30-year cycle has 11 leap years of 355 days and 19 normal years of 354 days
        long cycleDays = fullCycles * (11L * 355 + 19L * 354);
        // Add days for remaining years in the current cycle
        int[] leapYearsInCycle = {2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29};
        for (int y = 1; y <= remainingYears; y++) {
            boolean isLeap = false;
            for (int ly : leapYearsInCycle) {
                if (y == ly) { isLeap = true; break; }
            }
            cycleDays += isLeap ? 355 : 354;
        }
        return cycleDays;
    }

    /**
     * Get the length of a specific Hijri year in days.
     */
    private static int getHijriYearLength(int year) {
        int pos = ((year - 1) % 30 + 30) % 30; // position in 30-year cycle (0-based)
        int[] leapYearsInCycle = {2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29};
        for (int ly : leapYearsInCycle) {
            if (pos + 1 == ly) return 355;
        }
        return 354;
    }

    /**
     * Compute Hijri month and day from year and day-of-year
     */
    private static HijriDate computeMonthAndDay(int year, int dayOfYear) {
        boolean isLeap = isHijriLeapYear(year);
        int[] monthLengths = getHijriMonthLengths(isLeap);

        int month = 0;
        int day = dayOfYear;

        for (int m = 0; m < 12; m++) {
            if (day < monthLengths[m]) {
                month = m + 1;
                break;
            }
            day -= monthLengths[m];
        }

        if (month == 0) month = 12;

        return new HijriDate(year, month, day + 1, HIJRI_MONTHS_AR[month - 1]);
    }

    private static boolean isHijriLeapYear(int year) {
        int[] leapYears = {2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29};
        int pos = ((year - 1) % 30 + 30) % 30;
        for (int ly : leapYears) {
            if (pos + 1 == ly) return true;
        }
        return false;
    }

    private static int[] getHijriMonthLengths(boolean isLeap) {
        // Odd months = 30 days, even months = 29 days (except Dhul Hijjah in leap year = 30)
        return new int[]{30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, isLeap ? 30 : 29};
    }

    /**
     * Get today's Hijri date for Al-Haswa timezone
     */
    public static HijriDate getTodayHijri() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        return toHijri(cal);
    }

    /**
     * Get Hijri date for a specific Gregorian calendar
     */
    public static HijriDate getHijriForDate(Calendar cal) {
        return toHijri(cal);
    }

    /**
     * Format Hijri date as Arabic string
     */
    public static String formatHijriArabic(HijriDate date) {
        return date.day + " " + date.monthName + " " + date.year + " هـ";
    }

    public static class HijriDate {
        public final int year;
        public final int month;
        public final int day;
        public final String monthName;

        public HijriDate(int year, int month, int day, String monthName) {
            this.year = year;
            this.month = month;
            this.day = day;
            this.monthName = monthName;
        }

        @Override
        public String toString() {
            return formatHijriArabic(this);
        }
    }
}