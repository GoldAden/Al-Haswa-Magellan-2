package app.organicmaps.hawsa;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Islamic prayer times calculator for Al-Haswa (19.4431°N, 40.5167°E)
 * Using standard calculation method (Muslim World League)
 * All times in AM/PM format
 */
public class PrayerTimesCalculator {

    private static final double LAT = 19.4431;
    private static final double LON = 40.5167;
    private static final double TIMEZONE_OFFSET = 3.0; // AST (UTC+3)

    // Muslim World League angles
    private static final double FAJR_ANGLE = 18.0;
    private static final double ISHA_ANGLE = 17.0;

    // Sun's position calculations
    private static double toRad(double deg) { return deg * Math.PI / 180.0; }
    private static double toDeg(double rad) { return rad * 180.0 / Math.PI; }

    /**
     * Calculate solar declination for a given day of year
     */
    private static double solarDeclination(int dayOfYear) {
        return -23.45 * Math.cos(toRad(360.0 / 365.0 * (dayOfYear + 10)));
    }

    /**
     * Calculate equation of time (minutes)
     */
    private static double equationOfTime(int dayOfYear) {
        double B = toRad(360.0 / 365.0 * (dayOfYear - 81));
        return 9.87 * Math.sin(2 * B) - 7.53 * Math.cos(B) - 1.5 * Math.sin(B);
    }

    /**
     * Calculate solar noon (hours UTC)
     */
    private static double solarNoon(int dayOfYear) {
        double eqt = equationOfTime(dayOfYear);
        return 12.0 - eqt / 60.0 - LON / 15.0 + TIMEZONE_OFFSET;
    }

    /**
     * Calculate hour angle for a given sun altitude
     */
    private static double hourAngle(double lat, double decl, double altitude) {
        double latRad = toRad(lat);
        double declRad = toRad(decl);
        double altRad = toRad(altitude);
        double cosH = (Math.sin(altRad) - Math.sin(latRad) * Math.sin(declRad)) 
                      / (Math.cos(latRad) * Math.cos(declRad));
        if (cosH > 1) return -1; // Sun never reaches this altitude
        if (cosH < -1) return -1; // Sun never sets below this altitude
        return toDeg(Math.acos(cosH));
    }

    /**
     * Calculate all 5 prayer times for today
     */
    public static PrayerTimes getPrayerTimes(Calendar cal) {
        PrayerTimes pt = new PrayerTimes();
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        double decl = solarDeclination(dayOfYear);
        double noon = solarNoon(dayOfYear);

        // Fajr: sun at -18° below horizon
        double fajrHA = hourAngle(LAT, decl, -FAJR_ANGLE);
        pt.fajr = noon - fajrHA / 15.0;

        // Sunrise: sun at 0° + refraction correction (-0.833°)
        double sunriseHA = hourAngle(LAT, decl, -0.833);
        pt.sunrise = noon - sunriseHA / 15.0;

        // Dhuhr: solar noon + small adjustment
        pt.dhuhr = noon + 0.0167; // 1 min after noon

        // Asr (Shafi'i: shadow = object + shadow at noon)
        double asrAlt = toDeg(Math.atan(1.0 / (1.0 + Math.abs(Math.tan(toRad(LAT - decl))))));
        double asrHA = hourAngle(LAT, decl, asrAlt);
        pt.asr = noon + asrHA / 15.0;

        // Maghrib: sunset
        pt.maghrib = noon + sunriseHA / 15.0;

        // Isha: sun at -17° below horizon
        double ishaHA = hourAngle(LAT, decl, -ISHA_ANGLE);
        pt.isha = noon + ishaHA / 15.0;

        return pt;
    }

    /**
     * Get today's prayer times for Al-Haswa
     */
    public static PrayerTimes getTodayPrayerTimes() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        return getPrayerTimes(cal);
    }

    /**
     * Format decimal hours to hh:mm AM/PM
     */
    public static String formatToAmPm(double decimalHours) {
        if (decimalHours < 0 || decimalHours > 24) return "--:--";
        if (decimalHours >= 24) decimalHours -= 24;
        
        int totalMinutes = (int) Math.round(decimalHours * 60.0);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        
        boolean isPM = hours >= 12;
        if (hours > 12) hours -= 12;
        if (hours == 0) hours = 12;
        
        return String.format("%d:%02d %s", hours, minutes, isPM ? "PM" : "AM");
    }

    public static class PrayerTimes {
        public double fajr;
        public double sunrise;
        public double dhuhr;
        public double asr;
        public double maghrib;
        public double isha;

        public String getFajrStr() { return formatToAmPm(fajr); }
        public String getSunriseStr() { return formatToAmPm(sunrise); }
        public String getDhuhrStr() { return formatToAmPm(dhuhr); }
        public String getAsrStr() { return formatToAmPm(asr); }
        public String getMaghribStr() { return formatToAmPm(maghrib); }
        public String getIshaStr() { return formatToAmPm(isha); }
    }
}