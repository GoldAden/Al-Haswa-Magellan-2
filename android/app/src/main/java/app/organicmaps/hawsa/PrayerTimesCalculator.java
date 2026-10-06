package app.organicmaps.hawsa;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Islamic prayer times calculator with dynamic GPS-based coordinates
 * Using Muslim World League calculation method (Fajr 18°, Isha 17°)
 * All times in AM/PM format
 */
public class PrayerTimesCalculator {

    // Dynamic coordinates (set from GPS)
    private static double lat = 19.4431;
    private static double lon = 40.5167;
    private static double timezoneOffset = 3.0; // AST (UTC+3)

    // Muslim World League angles
    private static final double FAJR_ANGLE = 18.0;
    private static final double ISHA_ANGLE = 17.0;

    private static double toRad(double deg) { return deg * Math.PI / 180.0; }
    private static double toDeg(double rad) { return rad * 180.0 / Math.PI; }

    /**
     * Set location coordinates for prayer calculations
     */
    public static void setLocation(double latitude, double longitude, double tzOffset) {
        lat = latitude;
        lon = longitude;
        timezoneOffset = tzOffset;
    }

    public static double getLatitude() { return lat; }
    public static double getLongitude() { return lon; }
    public static double getTimezoneOffset() { return timezoneOffset; }

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
        return 12.0 - eqt / 60.0 - lon / 15.0 + timezoneOffset;
    }

    /**
     * Calculate hour angle for a given sun altitude
     */
    private static double hourAngle(double latitude, double decl, double altitude) {
        double latRad = toRad(latitude);
        double declRad = toRad(decl);
        double altRad = toRad(altitude);
        double cosH = (Math.sin(altRad) - Math.sin(latRad) * Math.sin(declRad))
                      / (Math.cos(latRad) * Math.cos(declRad));
        if (cosH > 1) return -1;
        if (cosH < -1) return -1;
        return toDeg(Math.acos(cosH));
    }

    /**
     * Calculate Qibla bearing from current location to Mecca
     * Mecca coordinates: 21.4225°N, 39.8262°E
     */
    public static double calculateQiblaBearing() {
        double meccaLat = 21.4225;
        double meccaLon = 39.8262;
        double latRad = toRad(lat);
        double meccaLatRad = toRad(meccaLat);
        double deltaLon = toRad(meccaLon - lon);
        double bearing = toDeg(Math.atan2(
            Math.sin(deltaLon),
            Math.cos(latRad) * Math.tan(meccaLatRad) - Math.sin(latRad) * Math.cos(deltaLon)
        ));
        if (bearing < 0) bearing += 360;
        return bearing;
    }

    /**
     * Calculate 5 prayer times (no Sunrise) for a given calendar
     */
    public static PrayerTimes getPrayerTimes(Calendar cal) {
        PrayerTimes pt = new PrayerTimes();
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        double decl = solarDeclination(dayOfYear);
        double noon = solarNoon(dayOfYear);

        // Fajr: sun at -18° below horizon
        double fajrHA = hourAngle(lat, decl, -FAJR_ANGLE);
        pt.fajr = noon - fajrHA / 15.0;

        // Dhuhr: solar noon + small adjustment
        pt.dhuhr = noon + 0.0167;

        // Asr (Shafi'i: shadow = object + shadow at noon)
        double asrAlt = toDeg(Math.atan(1.0 / (1.0 + Math.abs(Math.tan(toRad(lat - decl))))));
        double asrHA = hourAngle(lat, decl, asrAlt);
        pt.asr = noon + asrHA / 15.0;

        // Maghrib: sunset (sun at -0.833°)
        double sunsetHA = hourAngle(lat, decl, -0.833);
        pt.maghrib = noon + sunsetHA / 15.0;

        // Isha: sun at -17° below horizon
        double ishaHA = hourAngle(lat, decl, -ISHA_ANGLE);
        pt.isha = noon + ishaHA / 15.0;

        return pt;
    }

    /**
     * Get today's prayer times using current dynamic location
     */
    public static PrayerTimes getTodayPrayerTimes() {
        String tzId = "GMT";
        if (timezoneOffset == 3.0) tzId = "Asia/Riyadh";
        else if (timezoneOffset == 2.0) tzId = "Africa/Cairo";
        else if (timezoneOffset == 1.0) tzId = "Europe/Paris";
        else if (timezoneOffset == 0.0) tzId = "UTC";
        else if (timezoneOffset == -5.0) tzId = "America/New_York";
        else {
            // Generic offset
            tzId = TimeZone.getTimeZone("GMT" + (timezoneOffset >= 0 ? "+" : "") + (int)timezoneOffset).getID();
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(tzId));
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
        public double dhuhr;
        public double asr;
        public double maghrib;
        public double isha;

        public String getFajrStr() { return formatToAmPm(fajr); }
        public String getDhuhrStr() { return formatToAmPm(dhuhr); }
        public String getAsrStr() { return formatToAmPm(asr); }
        public String getMaghribStr() { return formatToAmPm(maghrib); }
        public String getIshaStr() { return formatToAmPm(isha); }
    }
}
