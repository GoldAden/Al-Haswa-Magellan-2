package app.organicmaps.hawsa;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Accurate Islamic prayer times calculator using astronomical solar position.
 * Calculation method: Muslim World League (MWL) angles.
 *   Fajr: 18°, Isha: 17° (standard for Saudi Arabia / Arabian Peninsula)
 * Asr: Standard (Shafi'i/Hanbali/Maliki) — shadow length = object length + shadow at noon.
 * Coordinates: Al-Haswa (19.4431°N, 40.5167°E), Timezone: UTC+3 (Asia/Riyadh).
 */
public class PrayerTimesCalculator {

    // Al-Haswa coordinates
    private static final double LATITUDE = 19.4431;
    private static final double LONGITUDE = 40.5167;
    private static final double TIMEZONE_OFFSET = 3.0; // UTC+3

    // MWL angles
    private static final double FAJR_ANGLE = 18.0;
    private static final double ISHA_ANGLE = 17.0;

    // Sun parameters for a given day
    private static class SunPosition {
        double declination;
        double eqOfTime;
    }

    public static class PrayerTimes {
        public double fajr;
        public double sunrise;
        public double dhuhr;
        public double asr;
        public double maghrib;
        public double isha;

        public String getFajrStr() { return decimalToTime(fajr); }
        public String getSunriseStr() { return decimalToTime(sunrise); }
        public String getDhuhrStr() { return decimalToTime(dhuhr); }
        public String getAsrStr() { return decimalToTime(asr); }
        public String getMaghribStr() { return decimalToTime(maghrib); }
        public String getIshaStr() { return decimalToTime(isha); }
    }

    /**
     * Get today's prayer times for Al-Haswa
     */
    public static PrayerTimes getTodayPrayerTimes() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        return getPrayerTimes(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Calculate prayer times for a given Gregorian date
     */
    public static PrayerTimes getPrayerTimes(int year, int month, int day) {
        SunPosition sun = computeSunPosition(year, month, day);
        PrayerTimes times = new PrayerTimes();

        // Solar noon (dhuhr)
        double noon = 12 + TIMEZONE_OFFSET - LONGITUDE / 15.0 - sun.eqOfTime / 60.0;
        if (noon < 0) noon += 24;
        if (noon >= 24) noon -= 24;
        times.dhuhr = noon;

        // Sunrise / Sunset (sun center at 0.833° below horizon for refraction)
        double cosRiseSet = (Math.sin(Math.toRadians(-0.833))
                - Math.sin(Math.toRadians(LATITUDE)) * Math.sin(Math.toRadians(sun.declination)))
                / (Math.cos(Math.toRadians(LATITUDE)) * Math.cos(Math.toRadians(sun.declination)));
        cosRiseSet = Math.max(-1, Math.min(1, cosRiseSet));
        double riseSetAngle = Math.toDegrees(Math.acos(cosRiseSet));
        times.sunrise = noon - riseSetAngle / 15.0;
        double sunset = noon + riseSetAngle / 15.0;
        times.maghrib = sunset;

        // Fajr (sun at -18°)
        double cosFajr = (Math.sin(Math.toRadians(-FAJR_ANGLE))
                - Math.sin(Math.toRadians(LATITUDE)) * Math.sin(Math.toRadians(sun.declination)))
                / (Math.cos(Math.toRadians(LATITUDE)) * Math.cos(Math.toRadians(sun.declination)));
        cosFajr = Math.max(-1, Math.min(1, cosFajr));
        double fajrAngle = Math.toDegrees(Math.acos(cosFajr));
        times.fajr = noon - fajrAngle / 15.0;

        // Asr (standard Shafi'i: shadow = object + shadow-at-noon)
        // The standard formula works regardless of hemisphere or season:
        //   asrAlt = atan(1 / (1 + tan(|lat - decl|)))
        //   then solve for hour angle from altitude
        double asrAlt = Math.toDegrees(Math.atan(
            1.0 / (1.0 + Math.tan(Math.toRadians(Math.abs(LATITUDE - sun.declination))))
        ));
        double cosAsr = (Math.sin(Math.toRadians(asrAlt))
                - Math.sin(Math.toRadians(LATITUDE)) * Math.sin(Math.toRadians(sun.declination)))
                / (Math.cos(Math.toRadians(LATITUDE)) * Math.cos(Math.toRadians(sun.declination)));
        cosAsr = Math.max(-1, Math.min(1, cosAsr));
        double asrAngle = Math.toDegrees(Math.acos(cosAsr));
        times.asr = noon + asrAngle / 15.0;

        // Isha (sun at -17°)
        double cosIsha = (Math.sin(Math.toRadians(-ISHA_ANGLE))
                - Math.sin(Math.toRadians(LATITUDE)) * Math.sin(Math.toRadians(sun.declination)))
                / (Math.cos(Math.toRadians(LATITUDE)) * Math.cos(Math.toRadians(sun.declination)));
        cosIsha = Math.max(-1, Math.min(1, cosIsha));
        double ishaAngle = Math.toDegrees(Math.acos(cosIsha));
        times.isha = sunset + ishaAngle / 15.0;

        // Handle extreme latitudes / polar night/day
        // If fajr < 0, isha > 24, or values are invalid, use 1/7 rule
        if (times.fajr < 0 || times.sunrise < times.fajr + 0.1) {
            double nightPortion = (sunset - times.sunrise) / 7.0;
            times.fajr = times.sunrise - nightPortion;
            if (times.fajr < 0) times.fajr += 24;
        }
        if (times.isha >= 24) {
            double nightPortion = (sunset - times.sunrise) / 7.0;
            times.isha = times.maghrib + nightPortion;
            if (times.isha >= 24) times.isha -= 24;
        }

        return times;
    }

    /**
     * Compute solar declination and equation of time for a given date.
     * Uses the NOAA solar calculator algorithm.
     */
    private static SunPosition computeSunPosition(int year, int month, int day) {
        // Julian Day Number
        if (month <= 2) { year--; month += 12; }
        long A = year / 100;
        long B = 2 - A + A / 4;
        long JD = (long)(365.25 * (year + 4716)) + (long)(30.6001 * (month + 1)) + day + B - 1524;

        // Centuries from J2000.0
        double T = (JD - 2451545.0) / 36525.0;

        // Geometric mean longitude of the Sun (degrees)
        double L0 = 280.46646 + 36000.76983 * T;
        L0 = normalizeAngle(L0);

        // Mean anomaly of the Sun (degrees)
        double M = 357.52911 + 35999.05029 * T;
        M = normalizeAngle(M);

        // Eccentricity of Earth's orbit
        double e = 0.016708634 - 0.000042037 * T;

        // Sun's equation of the center (degrees)
        double Mrad = Math.toRadians(M);
        double C = Math.sin(Mrad) * (1.914602 - 0.004817 * T)
                  + Math.sin(2 * Mrad) * (0.019993 - 0.000101 * T)
                  + Math.sin(3 * Mrad) * 0.000289;

        // Sun's true longitude
        double sunTrueLong = L0 + C;

        // Sun's apparent longitude
        double omega = 125.04 - 1934.136 * T;
        double lambda = sunTrueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        // Mean obliquity of the ecliptic
        double epsilon0 = 23.0 + (26.0 + (21.448 - 46.815 * T) / 60.0) / 60.0;
        double epsilon = epsilon0 + 0.00256 * Math.cos(Math.toRadians(omega));

        // Solar declination
        double sinDec = Math.sin(Math.toRadians(epsilon)) * Math.sin(Math.toRadians(lambda));
        double declination = Math.toDegrees(Math.asin(sinDec));

        // Equation of time
        double y = Math.tan(Math.toRadians(epsilon / 2.0));
        y *= y;
        double L0rad = Math.toRadians(L0);
        double eqTime = y * Math.sin(2 * L0rad)
                      - 2 * e * Math.sin(Mrad)
                      + 4 * e * y * Math.sin(Mrad) * Math.cos(2 * L0rad)
                      - 0.5 * y * y * Math.sin(4 * L0rad)
                      - 1.25 * e * e * Math.sin(2 * Mrad);
        eqTime = Math.toDegrees(eqTime) * 4.0; // Convert to minutes

        SunPosition sp = new SunPosition();
        sp.declination = declination;
        sp.eqOfTime = eqTime;
        return sp;
    }

    private static double normalizeAngle(double angle) {
        double a = angle % 360;
        if (a < 0) a += 360;
        return a;
    }

    /**
     * Convert decimal hours (e.g. 5.5 = 5:30 AM) to time string
     */
    private static String decimalToTime(double decimal) {
        if (decimal < 0) decimal += 24;
        if (decimal >= 24) decimal -= 24;

        int hours = (int) Math.floor(decimal);
        int minutes = (int) Math.round((decimal - hours) * 60);
        if (minutes >= 60) {
            hours++;
            minutes -= 60;
        }
        if (hours >= 24) hours -= 24;

        // Use Arabic-formatted 12-hour clock
        String ampm = hours < 12 ? "ص" : "م";
        int h12 = hours % 12;
        if (h12 == 0) h12 = 12;

        return String.format(Locale.US, "%d:%02d %s", h12, minutes, ampm);
    }
}