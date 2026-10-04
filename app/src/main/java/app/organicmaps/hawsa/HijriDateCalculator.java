package app.organicmaps.hawsa;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Islamic (Hijri) lunar calendar date calculator
 * Uses the tabular Islamic calendar algorithm for approximate Hijri dates.
 * Based on the civil Hijri calendar algorithm by Kasgar/Al-Faruqi.
 * Accuracy: ±1 day compared to actual lunar observation.
 */
public class HijriDateCalculator {

    // Days in each Hijri month in a 30-year cycle
    private static final int[] MONTH_DAYS_30Y = {
        355, 354, 355, 354, 355, 354, 355, 354, 355, 354, // years 1-10
        355, 354, 354, 355, 354, 355, 354, 355, 354, 355, // years 11-20
        354, 355, 354, 354, 355, 354, 355, 354, 355, 354  // years 21-30
    };

    // Hijri month names in Arabic
    private static final String[] HIJRI_MONTHS_AR = {
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    };

    // Reference: July 16, 622 CE (1 Muharram 1 AH) = Julian Day 1948439
    private static final long HIJRI_EPOCH_JD = 1948439;

    /**
     * Convert Gregorian calendar to Hijri date
     */
    public static HijriDate toHijri(Calendar gregorian) {
        // Convert Gregorian to Julian Day Number
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
     * Julian Day Number to Hijri date
     */
    private static HijriDate jdToHijri(long jd) {
        // Adjust for epoch
        long adjustedJD = jd - HIJRI_EPOCH_JD;

        // Calculate Hijri year and day
        int hijriYear = 1;
        long remainingDays = adjustedJD;

        // Calculate year
        for (int cycle = 0; cycle < 100; cycle++) {
            int baseYear = cycle * 30;
            for (int y = 0; y < 30; y++) {
                int yearDays = getHijriYearDays(y);
                if (remainingDays < yearDays) {
                    hijriYear = baseYear + y + 1;
                    return computeMonthAndDay(hijriYear, (int)remainingDays);
                }
                remainingDays -= yearDays;
            }
        }

        // Fallback: use simplified calculation
        hijriYear = (int)(adjustedJD / 354.36667) + 1;
        int dayOfYear = (int)(adjustedDaysInYear(hijriYear, adjustedJD));
        return computeMonthAndDay(hijriYear, dayOfYear);
    }

    private static int getHijriYearDays(int yearInCycle) {
        // 30-year cycle: 11 leap years (years 2,5,7,10,13,16,18,21,24,26,29)
        int[] leapYears = {2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29};
        for (int ly : leapYears) {
            if (yearInCycle == ly - 1) return 355;
        }
        return 354;
    }

    private static int adjustedDaysInYear(int hijriYear, long totalDays) {
        long accumulated = 0;
        for (int y = 1; y < hijriYear; y++) {
            int cycle = (y - 1) / 30;
            int pos = (y - 1) % 30;
            accumulated += getHijriYearDays(pos);
        }
        return (int)(totalDays - accumulated);
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
        int pos = ((year - 1) % 30);
        for (int ly : leapYears) {
            if (pos == ly - 1) return true;
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