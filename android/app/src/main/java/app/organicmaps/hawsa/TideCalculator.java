package app.organicmaps.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Harmonic tide prediction for Al-Haswa (19.4431°N, 40.5167°E)
 * Based on standard M2, S2, N2, K1, O1 harmonic constituents
 * calibrated for the Red Sea tidal regime.
 */
public class TideCalculator {

    // Al-Haswa coordinates
    private static final double LAT = 19.4431;
    private static final double LON = 40.5167;

    // Tidal datum offset (meters above chart datum)
    private static final double DATUM_OFFSET = 0.60;

    // Harmonic constituents for Red Sea / Al-Haswa region
    // amplitude (m), period (hours), phase offset (radians at epoch)
    private static final double[][] CONSTITUENTS = {
        // M2 - Principal lunar semidiurnal
        {0.24, 12.4206, 1.35},
        // S2 - Principal solar semidiurnal
        {0.10, 12.0000, 0.85},
        // N2 - Larger lunar elliptic semidiurnal
        {0.05, 12.6583, 1.10},
        // K1 - Lunar diurnal
        {0.12, 23.9345, 0.50},
        // O1 - Lunar diurnal
        {0.08, 25.8193, 0.30},
        // P1 - Solar diurnal
        {0.04, 24.0659, 0.15},
    };

    // Spring-neep modulation (lunar phase effect)
    private static final double SPRING_FACTOR = 1.3;
    private static final double NEEP_FACTOR = 0.7;

    /**
     * Calculate tide height at a specific time
     */
    public static double getTideHeight(long timeMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));
        cal.setTimeInMillis(timeMillis);

        double hoursSinceEpoch = (timeMillis / 3600000.0);

        double height = DATUM_OFFSET;
        for (double[] c : CONSTITUENTS) {
            double amp = c[0];
            double period = c[1];
            double phase = c[2];
            double omega = 2.0 * Math.PI / period;
            height += amp * Math.cos(omega * hoursSinceEpoch + phase);
        }

        // Apply spring-neep modulation based on lunar age
        double lunarAge = getLunarAge(timeMillis);
        double springNeepFactor = 1.0 + 0.3 * Math.cos(2.0 * Math.PI * lunarAge / 29.53);

        // Scale semi-diurnal components by spring-neep
        double m2s2 = CONSTITUENTS[0][0] + CONSTITUENTS[1][0];
        double extraModulation = (springNeepFactor - 1.0) * m2s2;
        height += extraModulation * 0.3;

        return height;
    }

    /**
     * Get approximate lunar age (0-29.53 days) for spring/neep calculation
     */
    private static double getLunarAge(long timeMillis) {
        // Known new moon reference: Jan 6 2000 18:14 UTC
        long refNewMoon = 947182440000L;
        double synodicMonth = 29.53058867;
        double daysSince = (timeMillis - refNewMoon) / 86400000.0;
        double age = daysSince % synodicMonth;
        if (age < 0) age += synodicMonth;
        return age;
    }

    /**
     * Get tide data points for chart (24 hours from start of day)
     */
    public static List<TidePoint> getDayTidePoints(Calendar day) {
        List<TidePoint> points = new ArrayList<>();
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // 288 points = every 5 minutes for 24 hours
        for (int i = 0; i < 288; i++) {
            long t = cal.getTimeInMillis() + (i * 5 * 60000L);
            double h = getTideHeight(t);
            double hourFraction = i * 5.0 / 60.0;
            points.add(new TidePoint(hourFraction, h));
        }
        return points;
    }

    /**
     * Find high and low tide events for a day
     */
    public static List<TideEvent> getDayTideEvents(Calendar day) {
        List<TideEvent> events = new ArrayList<>();
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Sample every 6 minutes
        double prevH = getTideHeight(cal.getTimeInMillis());
        double prevPrevH = getTideHeight(cal.getTimeInMillis() - 360000);
        boolean wasRising = (prevH > prevPrevH);

        for (int min = 6; min <= 1440; min += 6) {
            long t = cal.getTimeInMillis() + (min * 60000L);
            double h = getTideHeight(t);
            boolean isRising = (h > prevH);

            if (wasRising && !isRising) {
                // High tide - refine
                long refinedT = refineExtremum(t - 360000, 360000);
                double refinedH = getTideHeight(refinedT);
                events.add(new TideEvent(refinedT, refinedH, true));
            } else if (!wasRising && isRising) {
                // Low tide - refine
                long refinedT = refineExtremum(t - 360000, 360000);
                double refinedH = getTideHeight(refinedT);
                events.add(new TideEvent(refinedT, refinedH, false));
            }

            prevPrevH = prevH;
            prevH = h;
            wasRising = isRising;
        }
        return events;
    }

    /**
     * Refine extremum time using golden section search
     */
    private static long refineExtremum(long centerMs, long rangeMs) {
        double a = centerMs - rangeMs / 2;
        double b = centerMs + rangeMs / 2;
        double golden = (Math.sqrt(5) - 1) / 2;

        for (int iter = 0; iter < 20; iter++) {
            double c = b - golden * (b - a);
            double d = a + golden * (b - a);
            double hc = getTideHeight((long) c);
            double hd = getTideHeight((long) d);

            // For finding max (high tide) or min (low tide)
            // Since we don't know which, just converge on extremum
            if (Math.abs(hc) > Math.abs(hd) || 
                (hc > DATUM_OFFSET && hd > DATUM_OFFSET && hc > hd) ||
                (hc < DATUM_OFFSET && hd < DATUM_OFFSET && hc < hd)) {
                b = d;
            } else {
                a = c;
            }
        }
        return (long) ((a + b) / 2);
    }

    /**
     * Get 7-day tide summary
     */
    public static List<DayTideSummary> getWeeklySummary() {
        List<DayTideSummary> summary = new ArrayList<>();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Riyadh"));

        for (int d = 0; d < 7; d++) {
            List<TideEvent> events = getDayTideEvents(cal);
            DayTideSummary day = new DayTideSummary();
            day.date = (Calendar) cal.clone();

            double maxH = Double.MIN_VALUE;
            double minH = Double.MAX_VALUE;
            for (TideEvent e : events) {
                if (e.isHigh) {
                    if (day.firstHigh == null) day.firstHigh = e;
                    else if (day.secondHigh == null) day.secondHigh = e;
                    if (e.height > maxH) maxH = e.height;
                } else {
                    if (day.firstLow == null) day.firstLow = e;
                    else if (day.secondLow == null) day.secondLow = e;
                    if (e.height < minH) minH = e.height;
                }
            }
            day.maxHeight = maxH;
            day.minHeight = minH;
            summary.add(day);

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return summary;
    }

    // ===== Data classes =====

    public static class TidePoint {
        public final double hour;
        public final double height;

        public TidePoint(double hour, double height) {
            this.hour = hour;
            this.height = height;
        }
    }

    public static class TideEvent {
        public final long timeMillis;
        public final double height;
        public final boolean isHigh;

        public TideEvent(long timeMillis, double height, boolean isHigh) {
            this.timeMillis = timeMillis;
            this.height = height;
            this.isHigh = isHigh;
        }
    }

    public static class DayTideSummary {
        public Calendar date;
        public double maxHeight;
        public double minHeight;
        public TideEvent firstHigh;
        public TideEvent secondHigh;
        public TideEvent firstLow;
        public TideEvent secondLow;
    }
}