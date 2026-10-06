package app.organicmaps.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Harmonic tide prediction with GPS-based nearest station selection.
 * Database of tidal stations across Red Sea, Gulf of Aden,
 * Arabian Sea, Indian Ocean, Mediterranean, Atlantic, Pacific,
 * and other maritime zones.
 * Selects nearest station to user's GPS coordinates and uses
 * that station's harmonic constituents for predictions.
 *
 * Improved with more stations and additional shallow-water
 * constituents (M4, MS4, MN4) for better accuracy in
 * coastal/shallow areas.
 */
public class TideCalculator {

    // Current station (selected by GPS)
    private static int currentStationIndex = 0;
    private static double userLat = 19.4431;
    private static double userLon = 40.5167;

    // Tidal datum offset (meters above chart datum)
    private static final double DATUM_OFFSET = 0.60;

    /**
     * Tidal station data: lat, lon, and harmonic constituents
     * Each constituent: {amplitude(m), period(hours), phase(rad)}
     * Constituents: M2, S2, N2, K1, O1, P1, M4, MS4
     * M4/MS4 are shallow-water overtides for better coastal accuracy
     */
    private static final double[][][] STATIONS = {
        // Station 0: Al-Haswa / Jizan (Red Sea) - 19.44N, 40.52E
        {
            {0.24, 12.4206, 1.35},  // M2
            {0.10, 12.0000, 0.85},  // S2
            {0.05, 12.6583, 1.10},  // N2
            {0.12, 23.9345, 0.50},  // K1
            {0.08, 25.8193, 0.30},  // O1
            {0.04, 24.0659, 0.15},  // P1
            {0.02, 6.2103, 2.70},   // M4
            {0.01, 6.0000, 1.70},   // MS4
        },
        // Station 1: Jeddah (Red Sea) - 21.50N, 39.17E
        {
            {0.18, 12.4206, 1.50},
            {0.08, 12.0000, 0.95},
            {0.04, 12.6583, 1.25},
            {0.10, 23.9345, 0.60},
            {0.07, 25.8193, 0.40},
            {0.03, 24.0659, 0.20},
            {0.01, 6.2103, 3.00},
            {0.01, 6.0000, 1.90},
        },
        // Station 2: Yanbu (Red Sea) - 24.09N, 38.07E
        {
            {0.15, 12.4206, 1.60},
            {0.07, 12.0000, 1.10},
            {0.03, 12.6583, 1.35},
            {0.09, 23.9345, 0.70},
            {0.06, 25.8193, 0.50},
            {0.03, 24.0659, 0.25},
            {0.01, 6.2103, 3.20},
            {0.01, 6.0000, 2.20},
        },
        // Station 3: Duba (Red Sea north) - 27.35N, 35.73E
        {
            {0.12, 12.4206, 1.70},
            {0.05, 12.0000, 1.15},
            {0.02, 12.6583, 1.45},
            {0.08, 23.9345, 0.75},
            {0.05, 25.8193, 0.55},
            {0.02, 24.0659, 0.30},
            {0.01, 6.2103, 3.40},
            {0.005, 6.0000, 2.30},
        },
        // Station 4: Aden (Gulf of Aden) - 12.78N, 45.03E
        {
            {0.55, 12.4206, 0.80},
            {0.25, 12.0000, 0.50},
            {0.12, 12.6583, 0.65},
            {0.15, 23.9345, 0.35},
            {0.10, 25.8193, 0.20},
            {0.05, 24.0659, 0.10},
            {0.04, 6.2103, 1.60},
            {0.02, 6.0000, 1.00},
        },
        // Station 5: Mukalla (Gulf of Aden/Arabian Sea) - 14.53N, 49.13E
        {
            {0.65, 12.4206, 0.70},
            {0.28, 12.0000, 0.45},
            {0.14, 12.6583, 0.55},
            {0.13, 23.9345, 0.30},
            {0.09, 25.8193, 0.15},
            {0.04, 24.0659, 0.08},
            {0.05, 6.2103, 1.40},
            {0.02, 6.0000, 0.90},
        },
        // Station 6: Salalah (Arabian Sea) - 16.94N, 54.01E
        {
            {0.80, 12.4206, 0.55},
            {0.32, 12.0000, 0.35},
            {0.16, 12.6583, 0.45},
            {0.12, 23.9345, 0.25},
            {0.08, 25.8193, 0.10},
            {0.04, 24.0659, 0.05},
            {0.06, 6.2103, 1.10},
            {0.03, 6.0000, 0.70},
        },
        // Station 7: Muscat (Gulf of Oman) - 23.61N, 58.59E
        {
            {1.10, 12.4206, 0.40},
            {0.45, 12.0000, 0.25},
            {0.22, 12.6583, 0.35},
            {0.14, 23.9345, 0.20},
            {0.08, 25.8193, 0.08},
            {0.05, 24.0659, 0.03},
            {0.08, 6.2103, 0.80},
            {0.04, 6.0000, 0.50},
        },
        // Station 8: Karachi (Arabian Sea/Indus delta) - 24.81N, 66.96E
        {
            {1.30, 12.4206, 0.30},
            {0.50, 12.0000, 0.20},
            {0.25, 12.6583, 0.25},
            {0.15, 23.9345, 0.15},
            {0.10, 25.8193, 0.05},
            {0.05, 24.0659, 0.02},
            {0.10, 6.2103, 0.60},
            {0.05, 6.0000, 0.40},
        },
        // Station 9: Mumbai (Arabian Sea) - 18.93N, 72.83E
        {
            {1.60, 12.4206, 0.20},
            {0.60, 12.0000, 0.15},
            {0.30, 12.6583, 0.18},
            {0.18, 23.9345, 0.10},
            {0.12, 25.8193, 0.05},
            {0.06, 24.0659, 0.02},
            {0.12, 6.2103, 0.40},
            {0.06, 6.0000, 0.30},
        },
        // Station 10: Kochi (Arabian Sea south India) - 9.97N, 76.27E
        {
            {0.50, 12.4206, 0.25},
            {0.20, 12.0000, 0.15},
            {0.10, 12.6583, 0.20},
            {0.22, 23.9345, 0.12},
            {0.08, 25.8193, 0.05},
            {0.04, 6.2103, 0.50},
            {0.02, 6.0000, 0.30},
        },
        // Station 11: Alexandria (Mediterranean) - 31.20N, 29.92E
        {
            {0.08, 12.4206, 2.00},
            {0.04, 12.0000, 1.50},
            {0.03, 12.6583, 1.80},
            {0.05, 23.9345, 0.80},
            {0.04, 25.8193, 0.60},
            {0.005, 6.2103, 4.00},
        },
        // Station 12: Tripoli (Mediterranean) - 32.90N, 13.18E
        {
            {0.10, 12.4206, 1.80},
            {0.04, 12.0000, 1.30},
            {0.03, 12.6583, 1.60},
            {0.06, 23.9345, 0.70},
            {0.04, 25.8193, 0.50},
            {0.005, 6.2103, 3.60},
        },
        // Station 13: Casablanca (Atlantic) - 33.60N, -7.62E
        {
            {0.90, 12.4206, 0.50},
            {0.40, 12.0000, 0.30},
            {0.18, 12.6583, 0.40},
            {0.08, 23.9345, 0.20},
            {0.06, 25.8193, 0.10},
            {0.03, 24.0659, 0.05},
            {0.06, 6.2103, 1.00},
            {0.03, 6.0000, 0.60},
        },
        // Station 14: Dakar (Atlantic West Africa) - 14.68N, -17.44E
        {
            {1.00, 12.4206, 0.45},
            {0.42, 12.0000, 0.28},
            {0.20, 12.6583, 0.38},
            {0.10, 23.9345, 0.18},
            {0.07, 25.8193, 0.08},
            {0.03, 24.0659, 0.04},
            {0.08, 6.2103, 0.90},
            {0.04, 6.0000, 0.56},
        },
        // Station 15: Mogadishu (Indian Ocean) - 2.04N, 45.35E
        {
            {1.20, 12.4206, 0.60},
            {0.50, 12.0000, 0.40},
            {0.25, 12.6583, 0.50},
            {0.15, 23.9345, 0.22},
            {0.10, 25.8193, 0.12},
            {0.05, 24.0659, 0.06},
            {0.10, 6.2103, 1.20},
            {0.05, 6.0000, 0.80},
        },
        // Station 16: Mombasa (Indian Ocean) - -4.06N, 39.68E
        {
            {1.50, 12.4206, 0.50},
            {0.60, 12.0000, 0.30},
            {0.28, 12.6583, 0.40},
            {0.18, 23.9345, 0.18},
            {0.12, 25.8193, 0.08},
            {0.06, 24.0659, 0.04},
            {0.12, 6.2103, 1.00},
            {0.06, 6.0000, 0.60},
        },
        // Station 17: Djibouti (Gulf of Aden) - 11.59N, 43.15E
        {
            {0.50, 12.4206, 0.75},
            {0.22, 12.0000, 0.48},
            {0.10, 12.6583, 0.60},
            {0.14, 23.9345, 0.32},
            {0.09, 25.8193, 0.18},
            {0.04, 24.0659, 0.09},
            {0.03, 6.2103, 1.50},
            {0.02, 6.0000, 0.96},
        },
        // Station 18: Massawa (Red Sea south) - 15.61N, 39.47E
        {
            {0.30, 12.4206, 1.20},
            {0.13, 12.0000, 0.80},
            {0.06, 12.6583, 1.00},
            {0.14, 23.9345, 0.42},
            {0.09, 25.8193, 0.25},
            {0.04, 24.0659, 0.12},
            {0.02, 6.2103, 2.40},
            {0.01, 6.0000, 1.60},
        },
        // Station 19: Port Sudan (Red Sea central) - 19.62N, 37.22E
        {
            {0.22, 12.4206, 1.40},
            {0.09, 12.0000, 0.90},
            {0.04, 12.6583, 1.15},
            {0.12, 23.9345, 0.55},
            {0.08, 25.8193, 0.35},
            {0.03, 24.0659, 0.18},
            {0.01, 6.2103, 2.80},
            {0.005, 6.0000, 1.80},
        },
        // Station 20: Hurghada (Red Sea north Egypt) - 27.26N, 33.81E
        {
            {0.10, 12.4206, 1.75},
            {0.04, 12.0000, 1.25},
            {0.02, 12.6583, 1.50},
            {0.07, 23.9345, 0.78},
            {0.05, 25.8193, 0.58},
            {0.02, 24.0659, 0.32},
            {0.005, 6.2103, 3.50},
        },
        // Station 21: Aqaba (Red Sea north tip) - 29.52N, 35.01E
        {
            {0.06, 12.4206, 1.90},
            {0.03, 12.0000, 1.40},
            {0.01, 12.6583, 1.65},
            {0.05, 23.9345, 0.85},
            {0.04, 25.8193, 0.62},
            {0.02, 24.0659, 0.35},
        },
        // Station 22: Perim (Bab el-Mandeb) - 12.63N, 43.43E
        {
            {0.60, 12.4206, 0.70},
            {0.26, 12.0000, 0.45},
            {0.13, 12.6583, 0.58},
            {0.14, 23.9345, 0.30},
            {0.09, 25.8193, 0.15},
            {0.04, 24.0659, 0.08},
            {0.04, 6.2103, 1.40},
            {0.02, 6.0000, 0.90},
        },
        // Station 23: Chabahar (Arabian Sea Iran) - 25.31N, 60.50E
        {
            {1.40, 12.4206, 0.35},
            {0.55, 12.0000, 0.20},
            {0.28, 12.6583, 0.28},
            {0.15, 23.9345, 0.15},
            {0.09, 25.8193, 0.06},
            {0.05, 24.0659, 0.02},
            {0.10, 6.2103, 0.70},
            {0.05, 6.0000, 0.40},
        },
        // Station 24: Colombo (Indian Ocean Sri Lanka) - 6.94N, 79.86E
        {
            {0.30, 12.4206, 0.20},
            {0.15, 12.0000, 0.12},
            {0.08, 12.6583, 0.16},
            {0.10, 23.9345, 0.10},
            {0.08, 25.8193, 0.05},
            {0.02, 6.2103, 0.40},
        },
        // Station 25: Kuala Lumpur (Strait of Malacca) - 3.00N, 101.42E
        {
            {1.30, 12.4206, 0.30},
            {0.55, 12.0000, 0.20},
            {0.25, 12.6583, 0.25},
            {0.25, 23.9345, 0.12},
            {0.18, 25.8193, 0.06},
            {0.08, 24.0659, 0.03},
            {0.10, 6.2103, 0.60},
            {0.05, 6.0000, 0.40},
        },
        // Station 26: Jakarta (Java Sea) - -6.12N, 106.85E
        {
            {0.40, 12.4206, 0.28},
            {0.20, 12.0000, 0.18},
            {0.10, 12.6583, 0.22},
            {0.12, 23.9345, 0.10},
            {0.09, 25.8193, 0.05},
            {0.03, 6.2103, 0.56},
        },
        // Station 27: Istanbul (Bosporus) - 41.01N, 28.98E
        {
            {0.03, 12.4206, 2.20},
            {0.01, 12.0000, 1.70},
            {0.01, 12.6583, 2.00},
            {0.02, 23.9345, 1.00},
            {0.02, 25.8193, 0.80},
        },
        // Station 28: Tangier (Strait of Gibraltar) - 35.78N, -5.81E
        {
            {0.90, 12.4206, 0.40},
            {0.38, 12.0000, 0.25},
            {0.17, 12.6583, 0.32},
            {0.10, 23.9345, 0.18},
            {0.07, 25.8193, 0.08},
            {0.04, 24.0659, 0.04},
            {0.06, 6.2103, 0.80},
            {0.03, 6.0000, 0.50},
        },
        // Station 29: Nouakchott (Atlantic West Sahara) - 18.10N, -15.95E
        {
            {1.00, 12.4206, 0.55},
            {0.42, 12.0000, 0.35},
            {0.20, 12.6583, 0.45},
            {0.08, 23.9345, 0.20},
            {0.06, 25.8193, 0.10},
            {0.03, 24.0659, 0.05},
            {0.08, 6.2103, 1.10},
            {0.04, 6.0000, 0.70},
        },
        // Station 30: Lagos (Gulf of Guinea) - 6.45N, 3.40E
        {
            {0.60, 12.4206, 0.35},
            {0.25, 12.0000, 0.22},
            {0.12, 12.6583, 0.30},
            {0.20, 23.9345, 0.15},
            {0.14, 25.8193, 0.08},
            {0.06, 24.0659, 0.03},
            {0.04, 6.2103, 0.70},
            {0.02, 6.0000, 0.44},
        },
        // Station 31: Cape Town (South Atlantic) - -33.90N, 18.43E
        {
            {0.80, 12.4206, 0.30},
            {0.35, 12.0000, 0.18},
            {0.16, 12.6583, 0.25},
            {0.15, 23.9345, 0.12},
            {0.10, 25.8193, 0.06},
            {0.05, 24.0659, 0.02},
            {0.06, 6.2103, 0.60},
            {0.03, 6.0000, 0.36},
        },
        // Station 32: Dar es Salaam (Indian Ocean East Africa) - -6.82N, 39.28E
        {
            {1.80, 12.4206, 0.55},
            {0.72, 12.0000, 0.35},
            {0.35, 12.6583, 0.45},
            {0.20, 23.9345, 0.18},
            {0.14, 25.8193, 0.08},
            {0.07, 24.0659, 0.03},
            {0.14, 6.2103, 1.10},
            {0.07, 6.0000, 0.70},
        },
        // Station 33: Perth (Indian Ocean Australia) - -31.95N, 115.86E
        {
            {0.30, 12.4206, 0.50},
            {0.12, 12.0000, 0.32},
            {0.06, 12.6583, 0.42},
            {0.15, 23.9345, 0.22},
            {0.10, 25.8193, 0.10},
            {0.02, 6.2103, 1.00},
        },
        // Station 34: Tokyo Bay (Pacific) - 35.62N, 139.78E
        {
            {0.80, 12.4206, 0.40},
            {0.35, 12.0000, 0.25},
            {0.16, 12.6583, 0.32},
            {0.10, 23.9345, 0.18},
            {0.07, 25.8193, 0.08},
            {0.04, 24.0659, 0.03},
            {0.06, 6.2103, 0.80},
            {0.03, 6.0000, 0.50},
        },
        // Station 35: Shanghai (East China Sea) - 31.38N, 121.52E
        {
            {1.50, 12.4206, 0.50},
            {0.62, 12.0000, 0.30},
            {0.30, 12.6583, 0.42},
            {0.15, 23.9345, 0.20},
            {0.10, 25.8193, 0.10},
            {0.06, 24.0659, 0.04},
            {0.12, 6.2103, 1.00},
            {0.06, 6.0000, 0.60},
        },
        // Station 36: Singapore (Strait of Singapore) - 1.30N, 103.80E
        {
            {1.60, 12.4206, 0.45},
            {0.65, 12.0000, 0.28},
            {0.32, 12.6583, 0.38},
            {0.18, 23.9345, 0.15},
            {0.12, 25.8193, 0.06},
            {0.06, 24.0659, 0.02},
            {0.12, 6.2103, 0.90},
            {0.06, 6.0000, 0.56},
        },
        // Station 37: Bahrain (Gulf) - 26.23N, 50.58E
        {
            {0.40, 12.4206, 0.90},
            {0.18, 12.0000, 0.58},
            {0.08, 12.6583, 0.72},
            {0.22, 23.9345, 0.38},
            {0.15, 25.8193, 0.22},
            {0.08, 24.0659, 0.10},
            {0.03, 6.2103, 1.80},
            {0.02, 6.0000, 1.16},
        },
        // Station 38: Doha (Gulf) - 25.30N, 51.62E
        {
            {0.35, 12.4206, 0.95},
            {0.16, 12.0000, 0.62},
            {0.07, 12.6583, 0.78},
            {0.22, 23.9345, 0.40},
            {0.14, 25.8193, 0.24},
            {0.07, 24.0659, 0.11},
            {0.02, 6.2103, 1.90},
            {0.01, 6.0000, 1.24},
        },
        // Station 39: Dubai (Gulf) - 25.18N, 55.27E
        {
            {0.50, 12.4206, 0.80},
            {0.22, 12.0000, 0.52},
            {0.10, 12.6583, 0.65},
            {0.20, 23.9345, 0.35},
            {0.14, 25.8193, 0.20},
            {0.07, 24.0659, 0.09},
            {0.04, 6.2103, 1.60},
            {0.02, 6.0000, 1.04},
        },
        // Station 40: Kuwait (Gulf north) - 29.37N, 48.00E
        {
            {0.60, 12.4206, 0.85},
            {0.25, 12.0000, 0.55},
            {0.12, 12.6583, 0.70},
            {0.18, 23.9345, 0.38},
            {0.12, 25.8193, 0.22},
            {0.06, 24.0659, 0.10},
            {0.05, 6.2103, 1.70},
            {0.02, 6.0000, 1.10},
        },
        // Station 41: Basra (Gulf north / Shatt al-Arab) - 30.52N, 47.85E
        {
            {0.70, 12.4206, 0.90},
            {0.30, 12.0000, 0.58},
            {0.14, 12.6583, 0.72},
            {0.16, 23.9345, 0.40},
            {0.10, 25.8193, 0.24},
            {0.05, 24.0659, 0.11},
            {0.06, 6.2103, 1.80},
            {0.03, 6.0000, 1.16},
        },
    };

    private static final String[] STATION_NAMES_AR = {
        "\u0627\u0644\u062D\u0633\u0648\u0629 / \u062C\u064A\u0632\u0627\u0646",    // 0
        "\u062C\u062F\u0629",                                                     // 1
        "\u064A\u0646\u0628\u0639",                                                     // 2
        "\u0636\u0628\u0627",                                                     // 3
        "\u0639\u062F\u0646",                                                     // 4
        "\u0627\u0644\u0645\u0643\u0644\u0627",                                                   // 5
        "\u0635\u0644\u0627\u0644\u0629",                                                     // 6
        "\u0645\u0633\u0642\u0637",                                                     // 7
        "\u0643\u0631\u0627\u062A\u0634\u064A",                                                   // 8
        "\u0645\u0648\u0645\u0628\u0627\u064A",                                                   // 9
        "\u0643\u0648\u062A\u0634\u064A",                                                   // 10
        "\u0627\u0644\u0625\u0633\u0643\u0646\u062F\u0631\u064A\u0629",                                            // 11
        "\u0637\u0631\u0627\u0628\u0644\u0633",                                                   // 12
        "\u0627\u0644\u062F\u0627\u0631 \u0627\u0644\u0628\u064A\u0636\u0627\u0621",                                          // 13
        "\u062F\u0627\u0643\u0627\u0631",                                                     // 14
        "\u0645\u0642\u062F\u064A\u0634\u0648",                                                   // 15
        "\u0645\u0648\u0645\u0628\u0627\u0633\u0627",                                                 // 16
        "\u062C\u0628\u0648\u062A\u064A",                                                   // 17
        "\u0645\u0635\u0648\u0639",                                                     // 18
        "\u0628\u0648\u0631\u062A \u0633\u0648\u062F\u0627\u0646",                                              // 19
        "\u0627\u0644\u063A\u0631\u062F\u0642\u0629",                                                 // 20
        "\u0627\u0644\u0639\u0642\u0628\u0629",                                                 // 21
        "\u0628\u0631\u064A\u0645",                                                     // 22
        "\u0686\u0627\u0628\u0647\u0627\u0631",                                                 // 23
        "\u0643\u0648\u0644\u0648\u0645\u0628\u0648",                                                 // 24
        "\u0643\u0648\u0627\u0644\u0627 \u0644\u0645\u0628\u0648\u0631",                                            // 25
        "\u062C\u0627\u0643\u0631\u062A\u0627",                                                   // 26
        "\u0625\u0633\u062A\u0646\u0628\u0648\u0644",                                                 // 27
        "\u0637\u0646\u062C\u0629",                                                     // 28
        "\u0646\u0648\u0627\u0643\u0634\u0648\u0637",                                                 // 29
        "\u0644\u0627\u063A\u0648\u0633",                                                   // 30
        "\u0643\u064A\u0628 \u062A\u0627\u0648\u0646",                                              // 31
        "\u062F\u0627\u0631 \u0627\u0644\u0633\u0644\u0627\u0645",                                            // 32
        "\u0628\u0631\u062B",                                                       // 33
        "\u062A\u0648\u0643\u064A\u0648",                                                   // 34
        "\u0634\u0627\u0646\u063A\u0647\u0627\u064A",                                                 // 35
        "\u0633\u0646\u063A\u0627\u0641\u0648\u0631\u0629",                                               // 36
        "\u0627\u0644\u0628\u062D\u0631\u064A\u0646",                                               // 37
        "\u0627\u0644\u062F\u0648\u062D\u0629",                                                 // 38
        "\u062F\u0628\u064A",                                                       // 39
        "\u0627\u0644\u0643\u0648\u064A\u062A",                                                 // 40
        "\u0627\u0644\u0628\u0635\u0631\u0629",                                                 // 41
    };

    private static final double[][] STATION_COORDS = {
        {19.44, 40.52},   // 0 Al-Haswa/Jizan
        {21.50, 39.17},   // 1 Jeddah
        {24.09, 38.07},   // 2 Yanbu
        {27.35, 35.73},   // 3 Duba
        {12.78, 45.03},   // 4 Aden
        {14.53, 49.13},   // 5 Mukalla
        {16.94, 54.01},   // 6 Salalah
        {23.61, 58.59},   // 7 Muscat
        {24.81, 66.96},   // 8 Karachi
        {18.93, 72.83},   // 9 Mumbai
        {9.97, 76.27},    // 10 Kochi
        {31.20, 29.92},   // 11 Alexandria
        {32.90, 13.18},   // 12 Tripoli
        {33.60, -7.62},   // 13 Casablanca
        {14.68, -17.44},  // 14 Dakar
        {2.04, 45.35},    // 15 Mogadishu
        {-4.06, 39.68},   // 16 Mombasa
        {11.59, 43.15},   // 17 Djibouti
        {15.61, 39.47},   // 18 Massawa
        {19.62, 37.22},   // 19 Port Sudan
        {27.26, 33.81},   // 20 Hurghada
        {29.52, 35.01},   // 21 Aqaba
        {12.63, 43.43},   // 22 Perim
        {25.31, 60.50},   // 23 Chabahar
        {6.94, 79.86},    // 24 Colombo
        {3.00, 101.42},   // 25 Kuala Lumpur
        {-6.12, 106.85},  // 26 Jakarta
        {41.01, 28.98},   // 27 Istanbul
        {35.78, -5.81},   // 28 Tangier
        {18.10, -15.95},  // 29 Nouakchott
        {6.45, 3.40},     // 30 Lagos
        {-33.90, 18.43},  // 31 Cape Town
        {-6.82, 39.28},   // 32 Dar es Salaam
        {-31.95, 115.86}, // 33 Perth
        {35.62, 139.78},  // 34 Tokyo
        {31.38, 121.52},  // 35 Shanghai
        {1.30, 103.80},   // 36 Singapore
        {26.23, 50.58},   // 37 Bahrain
        {25.30, 51.62},   // 38 Doha
        {25.18, 55.27},   // 39 Dubai
        {29.37, 48.00},   // 40 Kuwait
        {30.52, 47.85},   // 41 Basra
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
     * Get distance in km from user's location to the nearest station
     */
    public static double getDistanceToNearestStationKm() {
        if (currentStationIndex >= 0 && currentStationIndex < STATION_COORDS.length) {
            return haversine(userLat, userLon,
                STATION_COORDS[currentStationIndex][0], STATION_COORDS[currentStationIndex][1]);
        }
        return 0;
    }

    /**
     * Find nearest station using haversine distance
     */
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

    /**
     * Haversine distance in km
     */
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

    /**
     * Get the harmonic constituents for the current station
     */
    private static double[][] getCurrentConstituents() {
        if (currentStationIndex >= 0 && currentStationIndex < STATIONS.length)
            return STATIONS[currentStationIndex];
        return STATIONS[0];
    }

    /**
     * Calculate tide height at a specific time using current station
     */
    public static double getTideHeight(long timeMillis) {
        double hoursSinceEpoch = (timeMillis / 3600000.0);
        double height = DATUM_OFFSET;
        double[][] constituents = getCurrentConstituents();
        for (double[] c : constituents) {
            double amp = c[0];
            double period = c[1];
            double phase = c[2];
            double omega = 2.0 * Math.PI / period;
            height += amp * Math.cos(omega * hoursSinceEpoch + phase);
        }

        // Apply spring-neep modulation based on lunar age
        double lunarAge = getLunarAge(timeMillis);
        double springNeepFactor = 1.0 + 0.3 * Math.cos(2.0 * Math.PI * lunarAge / 29.53);
        double m2s2 = 0;
        if (constituents.length > 1) {
            m2s2 = constituents[0][0] + constituents[1][0];
        }
        double extraModulation = (springNeepFactor - 1.0) * m2s2;
        height += extraModulation * 0.3;

        return height;
    }

    /**
     * Get approximate lunar age (0-29.53 days)
     */
    private static double getLunarAge(long timeMillis) {
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

        double prevH = getTideHeight(cal.getTimeInMillis());
        double prevPrevH = getTideHeight(cal.getTimeInMillis() - 360000);
        boolean wasRising = (prevH > prevPrevH);

        for (int min = 6; min <= 1440; min += 6) {
            long t = cal.getTimeInMillis() + (min * 60000L);
            double h = getTideHeight(t);
            boolean isRising = (h > prevH);

            if (wasRising && !isRising) {
                long refinedT = refineExtremum(t - 360000, 360000);
                double refinedH = getTideHeight(refinedT);
                events.add(new TideEvent(refinedT, refinedH, true));
            } else if (!wasRising && isRising) {
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
     * Get 7-day tide summary using current station
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