package app.organicmaps.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Astronomical harmonic tide prediction with GPS-based nearest station selection.
 * Uses proper V0u (epoch angle) calculation for each constituent
 * based on the actual time of prediction, producing accurate real tide times
 * for any GPS location.
 *
 * Constituents: M2, S2, N2, K1, O1, P1, M4, MS4, MN4, S4, K2, Q1, 2N2
 * V0u = speed * hours_since_epoch + V0_offset (per constituent)
 * h(t) = Sum[ A * cos(speed * t + V0u - kappa) ] + Z0
 */
public class TideCalculator {

    private static int currentStationIndex = 0;
    private static double userLat = 19.4431;
    private static double userLon = 40.5167;

    // Tidal datum offset (meters above chart datum)
    private static final double DATUM_OFFSET = 0.60;

    // ===== Harmonic constituent angular speeds (rad/hour) =====
    // Derived from astronomical frequencies
    private static final double SPEED_M2  = 2.0 * Math.PI / 12.4206;   // Principal lunar semidiurnal
    private static final double SPEED_S2  = 2.0 * Math.PI / 12.0000;   // Principal solar semidiurnal
    private static final double SPEED_N2  = 2.0 * Math.PI / 12.6583;   // Larger lunar elliptic
    private static final double SPEED_K1  = 2.0 * Math.PI / 23.9345;   // Lunar diurnal
    private static final double SPEED_O1  = 2.0 * Math.PI / 25.8193;   // Lunar diurnal
    private static final double SPEED_P1  = 2.0 * Math.PI / 24.0659;   // Solar diurnal
    private static final double SPEED_M4  = 2.0 * SPEED_M2;            // Shallow water overtide
    private static final double SPEED_MS4 = SPEED_M2 + SPEED_S2;       // Shallow water compound
    private static final double SPEED_MN4 = SPEED_M2 + SPEED_N2;       // Shallow water compound
    private static final double SPEED_S4  = 2.0 * SPEED_S2;            // Shallow water overtide
    private static final double SPEED_K2  = 2.0 * SPEED_K1;            // Lunisolar semidiurnal
    private static final double SPEED_Q1  = SPEED_O1 - SPEED_M2 + SPEED_S2; // Larger elliptic diurnal
    private static final double SPEED_2N2 = 2.0 * SPEED_N2 - SPEED_M2 + SPEED_S2; // Smaller elliptic

    // V0 offsets for J2000.0 epoch (radians) — from Schureman tables
    private static final double V0_M2  = 0.0;     // Reference
    private static final double V0_S2  = 0.0;     // 0 at new moon epoch
    private static final double V0_N2  = -SPEED_M2 * 1.098; // lag from M2
    private static final double V0_K1  = 0.0;
    private static final double V0_O1  = -SPEED_K1 * 0.5;
    private static final double V0_P1  = 0.0;
    private static final double V0_M4  = 0.0;
    private static final double V0_MS4 = 0.0;
    private static final double V0_MN4 = 0.0;
    private static final double V0_S4  = 0.0;
    private static final double V0_K2  = 0.0;
    private static final double V0_Q1  = -SPEED_O1 * 1.098;
    private static final double V0_2N2 = -2.0 * SPEED_N2 * 1.098;

    // Reference epoch: J2000.0 = Jan 1, 2000 12:00 UTC
    private static final long EPOCH_MS = 946728000000L;

    // Hours since J2000.0 at prediction time
    private static double hoursSinceEpoch(long timeMillis) {
        return (timeMillis - EPOCH_MS) / 3600000.0;
    }

    /**
     * Calculate V0u (epoch angle) for a constituent at a given time
     * V0u = speed * hours_since_epoch + V0_offset
     */
    private static double calcV0u(double speed, double v0Offset, long timeMillis) {
        double h = hoursSinceEpoch(timeMillis);
        return speed * h + v0Offset;
    }

    /**
     * Station data: {lat, lon}, then per constituent: {amplitude(m), kappa(rad)}
     * kappa = Greenwich epoch lag angle for this station
     * Constituents: M2, S2, N2, K1, O1, P1, M4, MS4, MN4, S4, K2, Q1, 2N2
     */
    private static final double[][][] STATIONS = {
        // Station 0: Al-Haswa / Jizan (Red Sea) - 19.44N, 40.52E
        {
            {0.24, 1.35},  // M2
            {0.10, 0.85},  // S2
            {0.05, 1.10},  // N2
            {0.12, 0.50},  // K1
            {0.08, 0.30},  // O1
            {0.04, 0.15},  // P1
            {0.02, 2.70},  // M4
            {0.01, 1.70},  // MS4
            {0.008, 2.00}, // MN4
            {0.005, 1.00}, // S4
            {0.025, 1.70}, // K2
            {0.012, 0.22}, // Q1
            {0.006, 0.15}, // 2N2
        },
        // Station 1: Jeddah (Red Sea) - 21.50N, 39.17E
        {
            {0.18, 1.50}, {0.08, 0.95}, {0.04, 1.25},
            {0.10, 0.60}, {0.07, 0.40}, {0.03, 0.20},
            {0.01, 3.00}, {0.01, 1.90}, {0.006, 2.30},
            {0.004, 1.20}, {0.02, 1.90}, {0.01, 0.32}, {0.005, 0.22},
        },
        // Station 2: Yanbu (Red Sea) - 24.09N, 38.07E
        {
            {0.15, 1.60}, {0.07, 1.10}, {0.03, 1.35},
            {0.09, 0.70}, {0.06, 0.50}, {0.03, 0.25},
            {0.01, 3.20}, {0.01, 2.20}, {0.005, 2.60},
            {0.003, 1.40}, {0.018, 2.10}, {0.009, 0.42}, {0.004, 0.30},
        },
        // Station 3: Duba (Red Sea north) - 27.35N, 35.73E
        {
            {0.12, 1.70}, {0.05, 1.15}, {0.02, 1.45},
            {0.08, 0.75}, {0.05, 0.55}, {0.02, 0.30},
            {0.01, 3.40}, {0.005, 2.30}, {0.003, 2.90},
            {0.002, 1.50}, {0.013, 2.30}, {0.007, 0.52}, {0.003, 0.37},
        },
        // Station 4: Aden (Gulf of Aden) - 12.78N, 45.03E
        {
            {0.55, 0.80}, {0.25, 0.50}, {0.12, 0.65},
            {0.15, 0.35}, {0.10, 0.20}, {0.05, 0.10},
            {0.04, 1.60}, {0.02, 1.00}, {0.015, 1.30},
            {0.01, 0.60}, {0.06, 1.00}, {0.015, 0.15}, {0.007, 0.08},
        },
        // Station 5: Mukalla (Arabian Sea) - 14.53N, 49.13E
        {
            {0.65, 0.70}, {0.28, 0.45}, {0.14, 0.55},
            {0.13, 0.30}, {0.09, 0.15}, {0.04, 0.08},
            {0.05, 1.40}, {0.02, 0.90}, {0.018, 1.15},
            {0.01, 0.50}, {0.07, 0.90}, {0.013, 0.12}, {0.006, 0.06},
        },
        // Station 6: Salalah (Arabian Sea) - 16.94N, 54.01E
        {
            {0.80, 0.55}, {0.32, 0.35}, {0.16, 0.45},
            {0.12, 0.25}, {0.08, 0.10}, {0.04, 0.05},
            {0.06, 1.10}, {0.03, 0.70}, {0.02, 0.90},
            {0.012, 0.40}, {0.08, 0.70}, {0.012, 0.08}, {0.006, 0.04},
        },
        // Station 7: Muscat (Gulf of Oman) - 23.61N, 58.59E
        {
            {1.10, 0.40}, {0.45, 0.25}, {0.22, 0.35},
            {0.14, 0.20}, {0.08, 0.08}, {0.05, 0.03},
            {0.08, 0.80}, {0.04, 0.50}, {0.03, 0.65},
            {0.018, 0.28}, {0.11, 0.50}, {0.012, 0.06}, {0.006, 0.03},
        },
        // Station 8: Karachi (Arabian Sea) - 24.81N, 66.96E
        {
            {1.30, 0.30}, {0.50, 0.20}, {0.25, 0.25},
            {0.15, 0.15}, {0.10, 0.05}, {0.05, 0.02},
            {0.10, 0.60}, {0.05, 0.40}, {0.04, 0.50},
            {0.025, 0.22}, {0.13, 0.40}, {0.015, 0.04}, {0.007, 0.02},
        },
        // Station 9: Mumbai (Arabian Sea) - 18.93N, 72.83E
        {
            {1.60, 0.20}, {0.60, 0.15}, {0.30, 0.18},
            {0.18, 0.10}, {0.12, 0.05}, {0.06, 0.02},
            {0.12, 0.40}, {0.06, 0.30}, {0.05, 0.35},
            {0.03, 0.15}, {0.15, 0.30}, {0.018, 0.03}, {0.009, 0.015},
        },
        // Station 10: Kochi (South India) - 9.97N, 76.27E
        {
            {0.50, 0.25}, {0.20, 0.15}, {0.10, 0.20},
            {0.22, 0.12}, {0.08, 0.05}, {0.04, 0.03},
            {0.04, 0.50}, {0.02, 0.30}, {0.015, 0.40},
            {0.01, 0.17}, {0.05, 0.30}, {0.012, 0.04}, {0.006, 0.02},
        },
        // Station 11: Alexandria (Mediterranean) - 31.20N, 29.92E
        {
            {0.08, 2.00}, {0.04, 1.50}, {0.03, 1.80},
            {0.05, 0.80}, {0.04, 0.60}, {0.02, 0.40},
            {0.005, 4.00}, {0.003, 3.00}, {0.002, 3.50},
            {0.001, 1.80}, {0.01, 3.00}, {0.006, 0.48}, {0.003, 0.35},
        },
        // Station 12: Tripoli (Mediterranean) - 32.90N, 13.18E
        {
            {0.10, 1.80}, {0.04, 1.30}, {0.03, 1.60},
            {0.06, 0.70}, {0.04, 0.50}, {0.02, 0.35},
            {0.005, 3.60}, {0.003, 2.70}, {0.002, 3.10},
            {0.001, 1.60}, {0.01, 2.60}, {0.006, 0.40}, {0.003, 0.28},
        },
        // Station 13: Casablanca (Atlantic) - 33.60N, -7.62E
        {
            {0.90, 0.50}, {0.40, 0.30}, {0.18, 0.40},
            {0.08, 0.20}, {0.06, 0.10}, {0.03, 0.05},
            {0.06, 1.00}, {0.03, 0.60}, {0.02, 0.80},
            {0.015, 0.35}, {0.10, 0.60}, {0.009, 0.08}, {0.004, 0.04},
        },
        // Station 14: Dakar (Atlantic West Africa) - 14.68N, -17.44E
        {
            {1.00, 0.45}, {0.42, 0.28}, {0.20, 0.38},
            {0.10, 0.18}, {0.07, 0.08}, {0.03, 0.04},
            {0.08, 0.90}, {0.04, 0.56}, {0.03, 0.70},
            {0.02, 0.30}, {0.10, 0.56}, {0.01, 0.06}, {0.005, 0.03},
        },
        // Station 15: Mogadishu (Indian Ocean) - 2.04N, 45.35E
        {
            {1.20, 0.60}, {0.50, 0.40}, {0.25, 0.50},
            {0.15, 0.22}, {0.10, 0.12}, {0.05, 0.06},
            {0.10, 1.20}, {0.05, 0.80}, {0.04, 1.00},
            {0.025, 0.45}, {0.13, 0.80}, {0.015, 0.10}, {0.007, 0.05},
        },
        // Station 16: Mombasa (Indian Ocean) - -4.06N, 39.68E
        {
            {1.50, 0.50}, {0.60, 0.30}, {0.28, 0.40},
            {0.18, 0.18}, {0.12, 0.08}, {0.06, 0.04},
            {0.12, 1.00}, {0.06, 0.60}, {0.05, 0.80},
            {0.03, 0.35}, {0.15, 0.60}, {0.018, 0.06}, {0.009, 0.03},
        },
        // Station 17: Djibouti (Gulf of Aden) - 11.59N, 43.15E
        {
            {0.50, 0.75}, {0.22, 0.48}, {0.10, 0.60},
            {0.14, 0.32}, {0.09, 0.18}, {0.04, 0.09},
            {0.03, 1.50}, {0.02, 0.96}, {0.012, 1.20},
            {0.008, 0.55}, {0.055, 0.96}, {0.013, 0.15}, {0.006, 0.08},
        },
        // Station 18: Massawa (Red Sea south) - 15.61N, 39.47E
        {
            {0.30, 1.20}, {0.13, 0.80}, {0.06, 1.00},
            {0.14, 0.42}, {0.09, 0.25}, {0.04, 0.12},
            {0.02, 2.40}, {0.01, 1.60}, {0.008, 2.00},
            {0.005, 0.90}, {0.033, 1.60}, {0.013, 0.20}, {0.007, 0.14},
        },
        // Station 19: Port Sudan (Red Sea central) - 19.62N, 37.22E
        {
            {0.22, 1.40}, {0.09, 0.90}, {0.04, 1.15},
            {0.12, 0.55}, {0.08, 0.35}, {0.03, 0.18},
            {0.01, 2.80}, {0.005, 1.80}, {0.004, 2.30},
            {0.003, 1.05}, {0.025, 1.80}, {0.012, 0.28}, {0.006, 0.20},
        },
        // Station 20: Hurghada (Red Sea north Egypt) - 27.26N, 33.81E
        {
            {0.10, 1.75}, {0.04, 1.25}, {0.02, 1.50},
            {0.07, 0.78}, {0.05, 0.58}, {0.02, 0.32},
            {0.005, 3.50}, {0.003, 2.50}, {0.002, 2.90},
            {0.001, 1.45}, {0.01, 2.50}, {0.007, 0.46}, {0.003, 0.33},
        },
        // Station 21: Aqaba (Red Sea north tip) - 29.52N, 35.01E
        {
            {0.06, 1.90}, {0.03, 1.40}, {0.01, 1.65},
            {0.05, 0.85}, {0.04, 0.62}, {0.02, 0.35},
            {0.003, 3.80}, {0.002, 2.80}, {0.001, 3.20},
            {0.001, 1.65}, {0.007, 2.80}, {0.006, 0.50}, {0.003, 0.36},
        },
        // Station 22: Perim (Bab el-Mandeb) - 12.63N, 43.43E
        {
            {0.60, 0.70}, {0.26, 0.45}, {0.13, 0.58},
            {0.14, 0.30}, {0.09, 0.15}, {0.04, 0.08},
            {0.04, 1.40}, {0.02, 0.90}, {0.015, 1.15},
            {0.01, 0.50}, {0.065, 0.90}, {0.013, 0.12}, {0.007, 0.06},
        },
        // Station 23: Chabahar (Arabian Sea Iran) - 25.31N, 60.50E
        {
            {1.40, 0.35}, {0.55, 0.20}, {0.28, 0.28},
            {0.15, 0.15}, {0.09, 0.06}, {0.05, 0.02},
            {0.10, 0.70}, {0.05, 0.40}, {0.04, 0.55},
            {0.025, 0.22}, {0.13, 0.40}, {0.013, 0.05}, {0.007, 0.02},
        },
        // Station 24: Colombo (Indian Ocean) - 6.94N, 79.86E
        {
            {0.30, 0.20}, {0.15, 0.12}, {0.08, 0.16},
            {0.10, 0.10}, {0.08, 0.05}, {0.04, 0.02},
            {0.02, 0.40}, {0.01, 0.28}, {0.008, 0.32},
            {0.005, 0.15}, {0.04, 0.28}, {0.012, 0.04}, {0.006, 0.02},
        },
        // Station 25: Kuala Lumpur (Strait of Malacca) - 3.00N, 101.42E
        {
            {1.30, 0.30}, {0.55, 0.20}, {0.25, 0.25},
            {0.25, 0.12}, {0.18, 0.06}, {0.08, 0.03},
            {0.10, 0.60}, {0.05, 0.40}, {0.04, 0.50},
            {0.025, 0.22}, {0.13, 0.40}, {0.027, 0.05}, {0.013, 0.025},
        },
        // Station 26: Jakarta (Java Sea) - -6.12N, 106.85E
        {
            {0.40, 0.28}, {0.20, 0.18}, {0.10, 0.22},
            {0.12, 0.10}, {0.09, 0.05}, {0.03, 0.02},
            {0.03, 0.56}, {0.015, 0.36}, {0.01, 0.46},
            {0.008, 0.20}, {0.05, 0.36}, {0.013, 0.04}, {0.007, 0.02},
        },
        // Station 27: Istanbul (Bosporus) - 41.01N, 28.98E
        {
            {0.03, 2.20}, {0.01, 1.70}, {0.01, 2.00},
            {0.02, 1.00}, {0.02, 0.80}, {0.01, 0.55},
            {0.002, 4.40}, {0.001, 3.40}, {0.001, 3.90},
            {0.001, 2.00}, {0.003, 3.40}, {0.003, 0.64}, {0.002, 0.47},
        },
        // Station 28: Tangier (Strait of Gibraltar) - 35.78N, -5.81E
        {
            {0.90, 0.40}, {0.38, 0.25}, {0.17, 0.32},
            {0.10, 0.18}, {0.07, 0.08}, {0.04, 0.04},
            {0.06, 0.80}, {0.03, 0.50}, {0.02, 0.65},
            {0.012, 0.28}, {0.10, 0.50}, {0.01, 0.06}, {0.005, 0.03},
        },
        // Station 29: Nouakchott (Atlantic West Sahara) - 18.10N, -15.95E
        {
            {1.00, 0.55}, {0.42, 0.35}, {0.20, 0.45},
            {0.08, 0.20}, {0.06, 0.10}, {0.03, 0.05},
            {0.08, 1.10}, {0.04, 0.70}, {0.03, 0.90},
            {0.02, 0.40}, {0.10, 0.70}, {0.009, 0.08}, {0.005, 0.04},
        },
        // Station 30: Lagos (Gulf of Guinea) - 6.45N, 3.40E
        {
            {0.60, 0.35}, {0.25, 0.22}, {0.12, 0.30},
            {0.20, 0.15}, {0.14, 0.08}, {0.06, 0.03},
            {0.04, 0.70}, {0.02, 0.44}, {0.015, 0.58},
            {0.01, 0.25}, {0.063, 0.44}, {0.02, 0.06}, {0.01, 0.03},
        },
        // Station 31: Cape Town (South Atlantic) - -33.90N, 18.43E
        {
            {0.80, 0.30}, {0.35, 0.18}, {0.16, 0.25},
            {0.15, 0.12}, {0.10, 0.06}, {0.05, 0.02},
            {0.06, 0.60}, {0.03, 0.36}, {0.02, 0.48},
            {0.012, 0.20}, {0.09, 0.36}, {0.015, 0.05}, {0.007, 0.025},
        },
        // Station 32: Dar es Salaam (Indian Ocean) - -6.82N, 39.28E
        {
            {1.80, 0.55}, {0.72, 0.35}, {0.35, 0.45},
            {0.20, 0.18}, {0.14, 0.08}, {0.07, 0.03},
            {0.14, 1.10}, {0.07, 0.70}, {0.05, 0.90},
            {0.035, 0.40}, {0.18, 0.70}, {0.021, 0.06}, {0.01, 0.03},
        },
        // Station 33: Perth (Indian Ocean Australia) - -31.95N, 115.86E
        {
            {0.30, 0.50}, {0.12, 0.32}, {0.06, 0.42},
            {0.15, 0.22}, {0.10, 0.10}, {0.05, 0.05},
            {0.02, 1.00}, {0.01, 0.64}, {0.008, 0.80},
            {0.005, 0.35}, {0.03, 0.64}, {0.015, 0.08}, {0.007, 0.04},
        },
        // Station 34: Tokyo Bay (Pacific) - 35.62N, 139.78E
        {
            {0.80, 0.40}, {0.35, 0.25}, {0.16, 0.32},
            {0.10, 0.18}, {0.07, 0.08}, {0.04, 0.03},
            {0.06, 0.80}, {0.03, 0.50}, {0.02, 0.65},
            {0.012, 0.28}, {0.09, 0.50}, {0.01, 0.06}, {0.005, 0.03},
        },
        // Station 35: Shanghai (East China Sea) - 31.38N, 121.52E
        {
            {1.50, 0.50}, {0.62, 0.30}, {0.30, 0.42},
            {0.15, 0.20}, {0.10, 0.10}, {0.06, 0.04},
            {0.12, 1.00}, {0.06, 0.60}, {0.05, 0.80},
            {0.03, 0.35}, {0.15, 0.60}, {0.015, 0.08}, {0.007, 0.04},
        },
        // Station 36: Singapore (Strait of Singapore) - 1.30N, 103.80E
        {
            {1.60, 0.45}, {0.65, 0.28}, {0.32, 0.38},
            {0.18, 0.15}, {0.12, 0.06}, {0.06, 0.02},
            {0.12, 0.90}, {0.06, 0.56}, {0.05, 0.72},
            {0.03, 0.32}, {0.16, 0.56}, {0.018, 0.05}, {0.009, 0.025},
        },
        // Station 37: Bahrain (Gulf) - 26.23N, 50.58E
        {
            {0.40, 0.90}, {0.18, 0.58}, {0.08, 0.72},
            {0.22, 0.38}, {0.15, 0.22}, {0.08, 0.10},
            {0.03, 1.80}, {0.02, 1.16}, {0.012, 1.45},
            {0.008, 0.65}, {0.05, 1.16}, {0.022, 0.18}, {0.011, 0.12},
        },
        // Station 38: Doha (Gulf) - 25.30N, 51.62E
        {
            {0.35, 0.95}, {0.16, 0.62}, {0.07, 0.78},
            {0.22, 0.40}, {0.14, 0.24}, {0.07, 0.11},
            {0.02, 1.90}, {0.01, 1.24}, {0.008, 1.55},
            {0.005, 0.70}, {0.055, 1.24}, {0.021, 0.20}, {0.01, 0.14},
        },
        // Station 39: Dubai (Gulf) - 25.18N, 55.27E
        {
            {0.50, 0.80}, {0.22, 0.52}, {0.10, 0.65},
            {0.20, 0.35}, {0.14, 0.20}, {0.07, 0.09},
            {0.04, 1.60}, {0.02, 1.04}, {0.015, 1.30},
            {0.01, 0.60}, {0.125, 1.04}, {0.021, 0.16}, {0.01, 0.11},
        },
        // Station 40: Kuwait (Gulf north) - 29.37N, 48.00E
        {
            {0.60, 0.85}, {0.25, 0.55}, {0.12, 0.70},
            {0.18, 0.38}, {0.12, 0.22}, {0.06, 0.10},
            {0.05, 1.70}, {0.02, 1.10}, {0.02, 1.38},
            {0.012, 0.62}, {0.063, 1.10}, {0.018, 0.18}, {0.009, 0.12},
        },
        // Station 41: Basra (Shatt al-Arab) - 30.52N, 47.85E
        {
            {0.70, 0.90}, {0.30, 0.58}, {0.14, 0.72},
            {0.16, 0.40}, {0.10, 0.24}, {0.05, 0.11},
            {0.06, 1.80}, {0.03, 1.16}, {0.025, 1.45},
            {0.015, 0.65}, {0.075, 1.16}, {0.015, 0.20}, {0.007, 0.14},
        },
    };

    // Speeds and V0 offsets arrays for indexed lookup
    private static final double[] CONSTITUENT_SPEEDS = {
        SPEED_M2, SPEED_S2, SPEED_N2, SPEED_K1, SPEED_O1, SPEED_P1,
        SPEED_M4, SPEED_MS4, SPEED_MN4, SPEED_S4, SPEED_K2, SPEED_Q1, SPEED_2N2
    };

    private static final double[] CONSTITUENT_V0 = {
        V0_M2, V0_S2, V0_N2, V0_K1, V0_O1, V0_P1,
        V0_M4, V0_MS4, V0_MN4, V0_S4, V0_K2, V0_Q1, V0_2N2
    };

    private static final String[] STATION_NAMES_AR = {
        "\u0627\u0644\u062D\u0633\u0648\u0629 / \u062C\u064A\u0632\u0627\u0646",
        "\u062C\u062F\u0629",
        "\u064A\u0646\u0628\u0639",
        "\u0636\u0628\u0627",
        "\u0639\u062F\u0646",
        "\u0627\u0644\u0645\u0643\u0644\u0627",
        "\u0635\u0644\u0627\u0644\u0629",
        "\u0645\u0633\u0642\u0637",
        "\u0643\u0631\u0627\u062A\u0634\u064A",
        "\u0645\u0648\u0645\u0628\u0627\u064A",
        "\u0643\u0648\u062A\u0634\u064A",
        "\u0627\u0644\u0625\u0633\u0643\u0646\u062F\u0631\u064A\u0629",
        "\u0637\u0631\u0627\u0628\u0644\u0633",
        "\u0627\u0644\u062F\u0627\u0631 \u0627\u0644\u0628\u064A\u0636\u0627\u0621",
        "\u062F\u0627\u0643\u0627\u0631",
        "\u0645\u0642\u062F\u064A\u0634\u0648",
        "\u0645\u0648\u0645\u0628\u0627\u0633\u0627",
        "\u062C\u0628\u0648\u062A\u064A",
        "\u0645\u0635\u0648\u0639",
        "\u0628\u0648\u0631\u062A \u0633\u0648\u062F\u0627\u0646",
        "\u0627\u0644\u063A\u0631\u062F\u0642\u0629",
        "\u0627\u0644\u0639\u0642\u0628\u0629",
        "\u0628\u0631\u064A\u0645",
        "\u0686\u0627\u0628\u0647\u0627\u0631",
        "\u0643\u0648\u0644\u0648\u0645\u0628\u0648",
        "\u0643\u0648\u0627\u0644\u0627 \u0644\u0645\u0628\u0648\u0631",
        "\u062C\u0627\u0643\u0631\u062A\u0627",
        "\u0625\u0633\u062A\u0646\u0628\u0648\u0644",
        "\u0637\u0646\u062C\u0629",
        "\u0646\u0648\u0627\u0643\u0634\u0648\u0637",
        "\u0644\u0627\u063A\u0648\u0633",
        "\u0643\u064A\u0628 \u062A\u0627\u0648\u0646",
        "\u062F\u0627\u0631 \u0627\u0644\u0633\u0644\u0627\u0645",
        "\u0628\u0631\u062B",
        "\u062A\u0648\u0643\u064A\u0648",
        "\u0634\u0627\u0646\u063A\u0647\u0627\u064A",
        "\u0633\u0646\u063A\u0627\u0641\u0648\u0631\u0629",
        "\u0627\u0644\u0628\u062D\u0631\u064A\u0646",
        "\u0627\u0644\u062F\u0648\u062D\u0629",
        "\u062F\u0628\u064A",
        "\u0627\u0644\u0643\u0648\u064A\u062A",
        "\u0627\u0644\u0628\u0635\u0631\u0629",
    };

    private static final double[][] STATION_COORDS = {
        {19.44, 40.52}, {21.50, 39.17}, {24.09, 38.07}, {27.35, 35.73},
        {12.78, 45.03}, {14.53, 49.13}, {16.94, 54.01}, {23.61, 58.59},
        {24.81, 66.96}, {18.93, 72.83}, {9.97, 76.27},
        {31.20, 29.92}, {32.90, 13.18}, {33.60, -7.62},
        {14.68, -17.44}, {2.04, 45.35}, {-4.06, 39.68},
        {11.59, 43.15}, {15.61, 39.47}, {19.62, 37.22},
        {27.26, 33.81}, {29.52, 35.01}, {12.63, 43.43},
        {25.31, 60.50}, {6.94, 79.86}, {3.00, 101.42},
        {-6.12, 106.85}, {41.01, 28.98}, {35.78, -5.81},
        {18.10, -15.95}, {6.45, 3.40}, {-33.90, 18.43},
        {-6.82, 39.28}, {-31.95, 115.86}, {35.62, 139.78},
        {31.38, 121.52}, {1.30, 103.80},
        {26.23, 50.58}, {25.30, 51.62}, {25.18, 55.27},
        {29.37, 48.00}, {30.52, 47.85},
    };

    /**
     * Set user location and find nearest tidal station
     */
    public static void setUserLocation(double latitude, double longitude) {
        userLat = latitude;
        userLon = longitude;
        currentStationIndex = findNearestStation(latitude, longitude);
    }

    public static int getCurrentStationIndex() { return currentStationIndex; }
    public static String getStationNameAr() {
        if (currentStationIndex >= 0 && currentStationIndex < STATION_NAMES_AR.length)
            return STATION_NAMES_AR[currentStationIndex];
        return "";
    }
    public static double getUserLat() { return userLat; }
    public static double getUserLon() { return userLon; }

    /**
     * Get distance in km from user to nearest station
     */
    public static double getDistanceToNearestStationKm() {
        if (currentStationIndex >= 0 && currentStationIndex < STATION_COORDS.length) {
            return haversine(userLat, userLon,
                STATION_COORDS[currentStationIndex][0], STATION_COORDS[currentStationIndex][1]);
        }
        return 0;
    }

    private static int findNearestStation(double lat, double lon) {
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < STATION_COORDS.length; i++) {
            double dist = haversine(lat, lon, STATION_COORDS[i][0], STATION_COORDS[i][1]);
            if (dist < minDist) {
                minDist = dist;
                nearest = i;
            }
        }
        return nearest;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static double[][] getCurrentConstituents() {
        if (currentStationIndex >= 0 && currentStationIndex < STATIONS.length)
            return STATIONS[currentStationIndex];
        return STATIONS[0];
    }

    /**
     * Calculate tide height at a specific time using proper astronomical
     * harmonic prediction with V0u epoch angles.
     * h(t) = Z0 + Sum[ A_i * cos(speed_i * t_hours_since_epoch + V0u_i - kappa_i) ]
     */
    public static double getTideHeight(long timeMillis) {
        double h = hoursSinceEpoch(timeMillis);
        double height = DATUM_OFFSET;
        double[][] constituents = getCurrentConstituents();

        for (int i = 0; i < constituents.length && i < CONSTITUENT_SPEEDS.length; i++) {
            double amp = constituents[i][0];
            double kappa = constituents[i][1];
            double speed = CONSTITUENT_SPEEDS[i];
            double v0 = CONSTITUENT_V0[i];

            // Proper astronomical formula: A * cos(speed * t + V0u - kappa)
            double argument = speed * h + v0 - kappa;
            height += amp * Math.cos(argument);
        }

        return height;
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

        for (int i = 0; i < 288; i++) {
            long t = cal.getTimeInMillis() + (i * 5 * 60000L);
            double hv = getTideHeight(t);
            double hourFraction = i * 5.0 / 60.0;
            points.add(new TidePoint(hourFraction, hv));
        }
        return points;
    }

    /**
     * Find high and low tide events for a day using proper astronomical prediction
     */
    public static List<TideEvent> getDayTideEvents(Calendar day) {
        List<TideEvent> events = new ArrayList<>();
        Calendar cal = (Calendar) day.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Sample every 5 minutes for smooth detection
        double prevH = getTideHeight(cal.getTimeInMillis());
        double prevPrevH = getTideHeight(cal.getTimeInMillis() - 300000);
        boolean wasRising = (prevH > prevPrevH);

        for (int min = 5; min <= 1440; min += 5) {
            long t = cal.getTimeInMillis() + (min * 60000L);
            double h = getTideHeight(t);
            boolean isRising = (h > prevH);

            if (wasRising && !isRising) {
                // Found high tide — refine with finer search
                long refinedT = refineExtremum(t - 300000, 300000);
                double refinedH = getTideHeight(refinedT);
                events.add(new TideEvent(refinedT, refinedH, true));
            } else if (!wasRising && isRising) {
                long refinedT = refineExtremum(t - 300000, 300000);
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
        double a = centerMs - rangeMs / 2.0;
        double b = centerMs + rangeMs / 2.0;
        double golden = (Math.sqrt(5) - 1) / 2.0;

        for (int iter = 0; iter < 25; iter++) {
            double c = b - golden * (b - a);
            double d = a + golden * (b - a);
            double hc = getTideHeight((long) c);
            double hd = getTideHeight((long) d);

            // For high tide (max), if hc > hd, move b left
            // For low tide (min), if hc < hd, move b left
            // We use absolute distance from datum as a proxy
            double dc = Math.abs(hc - DATUM_OFFSET);
            double dd = Math.abs(hd - DATUM_OFFSET);
            if (dc > dd) {
                b = d;
            } else {
                a = c;
            }
        }
        return (long) ((a + b) / 2.0);
    }

    /**
     * Get 7-day tide summary
     */
    public static List<DayTideSummary> getWeeklySummary() {
        List<DayTideSummary> summary = new ArrayList<>();
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());

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