package app.organicmaps.hawsa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Offline harmonic tide prediction for the Gulf of Aden and Arabian Sea region.
 * Uses verified Harmonic Constituents from TICON-4
 * (Hart-Davis, Dettmering & Seitz 2025, DGFI-TUM, CC BY 4.0)
 * for Aden, Djibouti, Salalah, Masirah, and Muscat reference stations.
 *
 * Predictions are computed entirely on-device using the standard
 * harmonic method:  h(t) = Z0 + Sum[ f_i * A_i * cos(V_i + u_i - kappa_i) ]
 * where V_i = speed_i * t_hours + V0_i  (Greenwich equilibrium argument)
 * and f_i, u_i are the nodal modulation corrections.
 *
 * No internet connection is required after installation.
 * GPS selects the nearest reference station; Aden is the default fallback.
 */
public class TideCalculator {

    // ===== Station selection =====
    private static int currentStationIndex = 0;
    private static double userLat = 12.788;
    private static double userLon = 44.975;

    // ===== Reference epoch: J2000.0 = 2000-01-01 12:00 UTC =====
    private static final long J2000_MS = 946728000000L;

    // ===== Astronomical longitudes at J2000.0 (degrees) =====
    // From Meeus "Astronomical Algorithms" and IERS conventions.
    //   s = mean longitude of Moon
    //   h = mean longitude of Sun
    //   p = mean longitude of lunar perigee
    //   N = mean longitude of ascending node (with negative sign convention)
    private static final double s0 = 218.3165;
    private static final double h0 = 280.4606;
    private static final double p0 = 83.3585;
    private static final double N0 = 125.0445;

    // ===== Angular speeds (deg/hour) from Schureman =====
    private static final double s_speed = 0.549016530;
    private static final double h_speed = 0.041068640;
    private static final double p_speed = 0.004106840;
    private static final double N_speed = -0.002206413;

    // Greenwich hour angle rate of mean Moon: 14.49205211 deg/hr
    private static final double tau_speed = 14.49205211;

    // ===== Published constituent angular speeds (deg/hour) =====
    // From Schureman "Manual of Harmonic Analysis and Prediction of Tides"
    private static final double SPD_M2      = 28.98410424;
    private static final double SPD_S2      = 30.00000000;
    private static final double SPD_N2      = 28.43972954;
    private static final double SPD_K1      = 15.04106864;
    private static final double SPD_O1      = 13.94303559;
    private static final double SPD_P1      = 14.95893136;
    private static final double SPD_Q1      = 13.39866194;
    private static final double SPD_K2      = 30.08213728;
    private static final double SPD_M4      = 57.96820848;
    private static final double SPD_MS4     = 58.98410424;
    private static final double SPD_MN4     = 57.42383378;
    private static final double SPD_2N2     = 27.89535484;
    private static final double SPD_S4      = 60.00000000;
    private static final double SPD_SA      =  0.04106864;
    private static final double SPD_SSA     =  0.08213728;
    private static final double SPD_MM      =  0.54437470;
    private static final double SPD_MF      =  1.09803303;
    private static final double SPD_M6      = 86.95231272;
    private static final double SPD_M1      = 14.49205211;
    private static final double SPD_J1      = 15.58544335;
    private static final double SPD_OO1     = 16.13510170;
    private static final double SPD_S1      = 15.00000000;
    private static final double SPD_RHO1     = 13.47151450;
    private static final double SPD_2Q1     = 12.85428829;
    private static final double SPD_NU2     = 28.51258316;
    private static final double SPD_MU2     = 27.96820848;
    private static final double SPD_L2      = 29.52847891;
    private static final double SPD_T2      = 29.95893330;
    private static final double SPD_LAMBDA2 = 29.45562529;
    private static final double SPD_2SM2    = 31.01589576;
    private static final double SPD_R2      = 30.04106670;
    private static final double SPD_M3      = 43.47615636;

    // ===== V0 (Greenwich equilibrium argument at J2000.0) in degrees =====
    private static final double tau_J2000 = 242.1441;

    private static final double V0_M2      = mod360(2 * tau_J2000 - 2 * s0 + 2 * h0);
    private static final double V0_S2      = mod360(2 * tau_J2000);
    private static final double V0_N2      = mod360(2 * tau_J2000 - 3 * s0 + h0 + p0);
    private static final double V0_K1      = mod360(tau_J2000 + s0 + h0 - 90);
    private static final double V0_O1      = mod360(tau_J2000 - 2 * s0 + h0 + 90);
    private static final double V0_P1      = mod360(tau_J2000 - h0 + 90);
    private static final double V0_Q1      = mod360(tau_J2000 - 3 * s0 + h0 + p0 + 90);
    private static final double V0_K2      = mod360(2 * tau_J2000 + 2 * s0 + 2 * h0);
    private static final double V0_M4      = mod360(4 * tau_J2000 - 4 * s0 + 4 * h0);
    private static final double V0_MS4     = mod360(4 * tau_J2000 - 2 * s0 + 2 * h0);
    private static final double V0_MN4     = mod360(4 * tau_J2000 - 5 * s0 + 3 * h0 + p0);
    private static final double V0_2N2     = mod360(2 * tau_J2000 - 4 * s0 + 2 * h0 + 2 * p0);
    private static final double V0_S4      = mod360(4 * tau_J2000);
    private static final double V0_SA      = mod360(h0);
    private static final double V0_SSA     = mod360(2 * h0);
    private static final double V0_MM      = mod360(s0 - p0);
    private static final double V0_MF      = mod360(2 * s0);
    private static final double V0_M6      = mod360(6 * tau_J2000 - 6 * s0 + 6 * h0);
    private static final double V0_M1      = mod360(tau_J2000 - s0 + h0 + N0);
    private static final double V0_J1      = mod360(tau_J2000 + s0 + h0 + 90);
    private static final double V0_OO1     = mod360(tau_J2000 + 2 * s0 + h0 - 90);
    private static final double V0_S1      = mod360(tau_J2000 + h0);
    private static final double V0_RHO1    = mod360(tau_J2000 - 2 * s0 + h0 + p0 + 90);
    private static final double V0_2Q1     = mod360(tau_J2000 - 4 * s0 + h0 + 2 * p0 + 90);
    private static final double V0_NU2     = mod360(2 * tau_J2000 - 3 * s0 + 4 * h0 - p0);
    private static final double V0_MU2     = mod360(2 * tau_J2000 - 4 * s0 + 4 * h0);
    private static final double V0_L2      = mod360(2 * tau_J2000 - s0 + h0 + p0);
    private static final double V0_T2      = mod360(2 * tau_J2000 - s0 + p0);
    private static final double V0_LAMBDA2 = mod360(2 * tau_J2000 - s0 + p0 + N0);
    private static final double V0_2SM2    = mod360(2 * tau_J2000 + 2 * s0 - 2 * h0);
    private static final double V0_R2      = mod360(2 * tau_J2000 + s0 - p0);
    private static final double V0_M3      = mod360(3 * tau_J2000 - 3 * s0 + 3 * h0);

    private static double mod360(double x) {
        x = x % 360;
        if (x < 0) x += 360;
        return x;
    }

    // ===== Constituent index constants =====
    private static final int IDX_M2 = 0, IDX_S2 = 1, IDX_N2 = 2, IDX_K1 = 3,
            IDX_O1 = 4, IDX_P1 = 5, IDX_Q1 = 6, IDX_K2 = 7,
            IDX_M4 = 8, IDX_MS4 = 9, IDX_MN4 = 10, IDX_2N2 = 11,
            IDX_S4 = 12, IDX_SA = 13, IDX_SSA = 14, IDX_MM = 15,
            IDX_MF = 16, IDX_M6 = 17, IDX_M1 = 18, IDX_J1 = 19,
            IDX_OO1 = 20, IDX_S1 = 21, IDX_RHO1 = 22, IDX_2Q1 = 23,
            IDX_NU2 = 24, IDX_MU2 = 25, IDX_L2 = 26, IDX_T2 = 27,
            IDX_LAMBDA2 = 28, IDX_2SM2 = 29, IDX_R2 = 30, IDX_M3 = 31;
    private static final int NUM_CONSTITUENTS = 32;

    // Speed and V0 arrays for indexed lookup
    private static final double[] SPEEDS = {
        SPD_M2, SPD_S2, SPD_N2, SPD_K1, SPD_O1, SPD_P1, SPD_Q1, SPD_K2,
        SPD_M4, SPD_MS4, SPD_MN4, SPD_2N2, SPD_S4, SPD_SA, SPD_SSA, SPD_MM,
        SPD_MF, SPD_M6, SPD_M1, SPD_J1, SPD_OO1, SPD_S1, SPD_RHO1, SPD_2Q1,
        SPD_NU2, SPD_MU2, SPD_L2, SPD_T2, SPD_LAMBDA2, SPD_2SM2, SPD_R2, SPD_M3
    };

    private static final double[] V0S = {
        V0_M2, V0_S2, V0_N2, V0_K1, V0_O1, V0_P1, V0_Q1, V0_K2,
        V0_M4, V0_MS4, V0_MN4, V0_2N2, V0_S4, V0_SA, V0_SSA, V0_MM,
        V0_MF, V0_M6, V0_M1, V0_J1, V0_OO1, V0_S1, V0_RHO1, V0_2Q1,
        V0_NU2, V0_MU2, V0_L2, V0_T2, V0_LAMBDA2, V0_2SM2, V0_R2, V0_M3
    };

    private static final String[] CONSTITUENT_NAMES = {
        "M2","S2","N2","K1","O1","P1","Q1","K2",
        "M4","MS4","MN4","2N2","S4","SA","SSA","MM",
        "MF","M6","M1","J1","OO1","S1","RHO1","2Q1",
        "NU2","MU2","L2","T2","LAMBDA2","2SM2","R2","M3"
    };

    // ===== TICON-4 Aden Station (aden-172a-yem-uhslc_rq) =====
    // Source: Hart-Davis, M., Dettmering, D., Seitz, F. (2025).
    //   TICON-4: TIdal CONstants based on GESLA-4 sea-level records.
    //   SEANOE. https://doi.org/10.17882/109129
    // License: CC BY 4.0
    // Latitude: 12.788, Longitude: 44.975
    // Z0 (MSL above LAT) = 1.682m  (MSL=1.451, LAT=-0.231)
    private static final double Z0_ADEN = 1.682;
    private static final double[][] ADEN_CONSTITUENTS = new double[NUM_CONSTITUENTS][2];
    static {
        ADEN_CONSTITUENTS[IDX_M2]      = new double[]{0.48448993, 136.0078};
        ADEN_CONSTITUENTS[IDX_S2]      = new double[]{0.21433894, 156.4982};
        ADEN_CONSTITUENTS[IDX_N2]      = new double[]{0.13429483, 130.8815};
        ADEN_CONSTITUENTS[IDX_K1]      = new double[]{0.39995211, 349.5122};
        ADEN_CONSTITUENTS[IDX_O1]      = new double[]{0.20219290, 352.3122};
        ADEN_CONSTITUENTS[IDX_P1]      = new double[]{0.12155874, 347.0704};
        ADEN_CONSTITUENTS[IDX_Q1]      = new double[]{0.04225454, 351.5283};
        ADEN_CONSTITUENTS[IDX_K2]      = new double[]{0.05956720, 150.8643};
        ADEN_CONSTITUENTS[IDX_M4]      = new double[]{0.00319951, 137.4264};
        ADEN_CONSTITUENTS[IDX_MS4]     = new double[]{0.00206110, 320.7299};
        ADEN_CONSTITUENTS[IDX_MN4]     = new double[]{0.00254921, 64.0897};
        ADEN_CONSTITUENTS[IDX_2N2]     = new double[]{0.02713412, 100.0769};
        ADEN_CONSTITUENTS[IDX_S4]      = new double[]{0.00144889, 118.6723};
        ADEN_CONSTITUENTS[IDX_SA]      = new double[]{0.09950719, 354.6151};
        ADEN_CONSTITUENTS[IDX_SSA]     = new double[]{0.03356098, 127.1987};
        ADEN_CONSTITUENTS[IDX_MM]      = new double[]{0.00656045, 339.9066};
        ADEN_CONSTITUENTS[IDX_MF]      = new double[]{0.01305509, 10.1359};
        ADEN_CONSTITUENTS[IDX_M6]      = new double[]{0.00123798, 64.4937};
        ADEN_CONSTITUENTS[IDX_M1]      = new double[]{0.01116658, 289.7950};
        ADEN_CONSTITUENTS[IDX_J1]      = new double[]{0.02485542, 358.6453};
        ADEN_CONSTITUENTS[IDX_OO1]     = new double[]{0.01906526, 354.1064};
        ADEN_CONSTITUENTS[IDX_S1]      = new double[]{0.02647597, 117.2699};
        ADEN_CONSTITUENTS[IDX_RHO1]    = new double[]{0.00807182, 359.5682};
        ADEN_CONSTITUENTS[IDX_2Q1]     = new double[]{0.00680457, 343.8724};
        ADEN_CONSTITUENTS[IDX_NU2]     = new double[]{0.02544553, 137.1001};
        ADEN_CONSTITUENTS[IDX_MU2]     = new double[]{0.02267258, 100.0100};
        ADEN_CONSTITUENTS[IDX_L2]      = new double[]{0.01146017, 133.9947};
        ADEN_CONSTITUENTS[IDX_T2]      = new double[]{0.01365397, 163.0097};
        ADEN_CONSTITUENTS[IDX_LAMBDA2] = new double[]{0.00411358, 102.0431};
        ADEN_CONSTITUENTS[IDX_2SM2]    = new double[]{0.00468107, 8.7235};
        ADEN_CONSTITUENTS[IDX_R2]      = new double[]{0.00329532, 188.8792};
        ADEN_CONSTITUENTS[IDX_M3]      = new double[]{0.00526565, 77.4024};
    }
    private static final double ADEN_LAT = 12.788;
    private static final double ADEN_LON = 44.975;

    // ===== TICON-4 Djibouti Station (djibouti-119a-dji-uhslc_rq) =====
    // Source: Same TICON-4 publication
    // License: CC BY 4.0
    // Latitude: 11.608, Longitude: 43.140
    // Z0 (MSL above LAT) = 1.671m (MSL=1.914, LAT=0.243)
    private static final double Z0_DJIBOUTI = 1.671;
    private static final double[][] DJIBOUTI_CONSTITUENTS = new double[NUM_CONSTITUENTS][2];
    static {
        DJIBOUTI_CONSTITUENTS[IDX_M2]      = new double[]{0.48491307, 135.6280};
        DJIBOUTI_CONSTITUENTS[IDX_S2]      = new double[]{0.21880642, 156.3779};
        DJIBOUTI_CONSTITUENTS[IDX_N2]      = new double[]{0.13460899, 131.5981};
        DJIBOUTI_CONSTITUENTS[IDX_K1]      = new double[]{0.39447070, 350.5537};
        DJIBOUTI_CONSTITUENTS[IDX_O1]      = new double[]{0.19702111, 353.5253};
        DJIBOUTI_CONSTITUENTS[IDX_P1]      = new double[]{0.12103235, 348.4939};
        DJIBOUTI_CONSTITUENTS[IDX_Q1]      = new double[]{0.04077954, 354.0582};
        DJIBOUTI_CONSTITUENTS[IDX_K2]      = new double[]{0.06018929, 151.0135};
        DJIBOUTI_CONSTITUENTS[IDX_M4]      = new double[]{0.00477736, 135.9315};
        DJIBOUTI_CONSTITUENTS[IDX_MS4]     = new double[]{0.00190891, 318.0305};
        DJIBOUTI_CONSTITUENTS[IDX_MN4]     = new double[]{0.00370294, 72.0213};
        DJIBOUTI_CONSTITUENTS[IDX_2N2]     = new double[]{0.02639519, 102.0849};
        DJIBOUTI_CONSTITUENTS[IDX_S4]      = new double[]{0.00192516, 127.3653};
        DJIBOUTI_CONSTITUENTS[IDX_SA]      = new double[]{0.08590872, 357.4827};
        DJIBOUTI_CONSTITUENTS[IDX_SSA]     = new double[]{0.03217968, 130.0290};
        DJIBOUTI_CONSTITUENTS[IDX_MM]      = new double[]{0.00754987, 339.5485};
        DJIBOUTI_CONSTITUENTS[IDX_MF]      = new double[]{0.01326962, 6.1386};
        DJIBOUTI_CONSTITUENTS[IDX_M6]      = new double[]{0.00251111, 82.4926};
        DJIBOUTI_CONSTITUENTS[IDX_M1]      = new double[]{0.01445470, 276.1501};
        DJIBOUTI_CONSTITUENTS[IDX_J1]      = new double[]{0.02474366, 358.3496};
        DJIBOUTI_CONSTITUENTS[IDX_OO1]     = new double[]{0.01945163, 0.5316};
        DJIBOUTI_CONSTITUENTS[IDX_S1]      = new double[]{0.02594008, 114.2677};
        DJIBOUTI_CONSTITUENTS[IDX_RHO1]    = new double[]{0.00763365, 0.8470};
        DJIBOUTI_CONSTITUENTS[IDX_2Q1]     = new double[]{0.00664378, 347.0691};
        DJIBOUTI_CONSTITUENTS[IDX_NU2]     = new double[]{0.02545072, 137.3881};
        DJIBOUTI_CONSTITUENTS[IDX_MU2]     = new double[]{0.02309931, 99.9774};
        DJIBOUTI_CONSTITUENTS[IDX_L2]      = new double[]{0.01185536, 133.3496};
        DJIBOUTI_CONSTITUENTS[IDX_T2]      = new double[]{0.01265552, 161.7203};
        DJIBOUTI_CONSTITUENTS[IDX_LAMBDA2] = new double[]{0.00391659, 95.5985};
        DJIBOUTI_CONSTITUENTS[IDX_2SM2]    = new double[]{0.00570949, 8.6845};
        DJIBOUTI_CONSTITUENTS[IDX_R2]      = new double[]{0.00363682, 189.5172};
        DJIBOUTI_CONSTITUENTS[IDX_M3]      = new double[]{0.00640507, 81.0819};
    }
    private static final double DJIBOUTI_LAT = 11.608;
    private static final double DJIBOUTI_LON = 43.140;

    // ===== TICON-4 Salalah Station (salalah-114a-omn-uhslc_rq) =====
    // Source: Same TICON-4 publication. CC BY 4.0
    // Latitude: 16.935, Longitude: 54.007
    // Z0 (MSL above LAT) = 1.308m (MSL=2.008, LAT=0.700)
    private static final double Z0_SALALAH = 1.308;
    private static final double[][] SALALAH_CONSTITUENTS = new double[NUM_CONSTITUENTS][2];
    static {
        SALALAH_CONSTITUENTS[IDX_M2]      = new double[]{0.31363983, 144.5635};
        SALALAH_CONSTITUENTS[IDX_S2]      = new double[]{0.12302050, 167.9992};
        SALALAH_CONSTITUENTS[IDX_N2]      = new double[]{0.08346450, 133.0392};
        SALALAH_CONSTITUENTS[IDX_K1]      = new double[]{0.36328745, 344.4034};
        SALALAH_CONSTITUENTS[IDX_O1]      = new double[]{0.18573503, 347.2097};
        SALALAH_CONSTITUENTS[IDX_P1]      = new double[]{0.11086832, 341.9865};
        SALALAH_CONSTITUENTS[IDX_Q1]      = new double[]{0.03964764, 348.5366};
        SALALAH_CONSTITUENTS[IDX_K2]      = new double[]{0.03400899, 161.8978};
        SALALAH_CONSTITUENTS[IDX_M4]      = new double[]{0.00178349, 326.4855};
        SALALAH_CONSTITUENTS[IDX_MS4]     = new double[]{0.00090165, 144.9706};
        SALALAH_CONSTITUENTS[IDX_MN4]     = new double[]{0.00139809, 254.4372};
        SALALAH_CONSTITUENTS[IDX_2N2]     = new double[]{0.01457482, 99.9571};
        SALALAH_CONSTITUENTS[IDX_S4]      = new double[]{0.00080562, 315.3151};
        SALALAH_CONSTITUENTS[IDX_SA]      = new double[]{0.08282259, 325.9338};
        SALALAH_CONSTITUENTS[IDX_SSA]     = new double[]{0.05153992, 121.7945};
        SALALAH_CONSTITUENTS[IDX_MM]      = new double[]{0.00566807, 34.4934};
        SALALAH_CONSTITUENTS[IDX_MF]      = new double[]{0.01088996, 6.3237};
        SALALAH_CONSTITUENTS[IDX_M6]      = new double[]{0.00097412, 254.8187};
        SALALAH_CONSTITUENTS[IDX_M1]      = new double[]{0.01301888, 253.2547};
        SALALAH_CONSTITUENTS[IDX_J1]      = new double[]{0.02252522, 355.8095};
        SALALAH_CONSTITUENTS[IDX_OO1]     = new double[]{0.01501756, 13.5962};
        SALALAH_CONSTITUENTS[IDX_S1]      = new double[]{0.01992449, 118.1521};
        SALALAH_CONSTITUENTS[IDX_RHO1]    = new double[]{0.00709823, 351.8937};
        SALALAH_CONSTITUENTS[IDX_2Q1]     = new double[]{0.00614700, 337.1251};
        SALALAH_CONSTITUENTS[IDX_NU2]     = new double[]{0.01626288, 139.5773};
        SALALAH_CONSTITUENTS[IDX_MU2]     = new double[]{0.01257419, 101.0658};
        SALALAH_CONSTITUENTS[IDX_L2]      = new double[]{0.00733504, 149.1080};
        SALALAH_CONSTITUENTS[IDX_T2]      = new double[]{0.00526696, 178.6782};
        SALALAH_CONSTITUENTS[IDX_LAMBDA2] = new double[]{0.00135707, 135.1148};
        SALALAH_CONSTITUENTS[IDX_2SM2]    = new double[]{0.00186254, 356.8735};
        SALALAH_CONSTITUENTS[IDX_R2]      = new double[]{0.00102177, 188.4466};
        SALALAH_CONSTITUENTS[IDX_M3]      = new double[]{0.00040531, 152.4095};
    }
    private static final double SALALAH_LAT = 16.935;
    private static final double SALALAH_LON = 54.007;

    // ===== TICON-4 Masirah Station (masirah-113a-omn-uhslc_rq) =====
    // Source: Same TICON-4 publication. CC BY 4.0
    // Latitude: 20.687, Longitude: 58.872
    // Z0 (MSL above LAT) = 1.795m (MSL=1.468, LAT=-0.327)
    private static final double Z0_MASIRAH = 1.795;
    private static final double[][] MASIRAH_CONSTITUENTS = new double[NUM_CONSTITUENTS][2];
    static {
        MASIRAH_CONSTITUENTS[IDX_M2]      = new double[]{0.60432201, 166.7580};
        MASIRAH_CONSTITUENTS[IDX_S2]      = new double[]{0.23244261, 198.2638};
        MASIRAH_CONSTITUENTS[IDX_N2]      = new double[]{0.14902426, 151.1544};
        MASIRAH_CONSTITUENTS[IDX_K1]      = new double[]{0.40225975, 346.1305};
        MASIRAH_CONSTITUENTS[IDX_O1]      = new double[]{0.21015346, 348.9465};
        MASIRAH_CONSTITUENTS[IDX_P1]      = new double[]{0.12276425, 346.7567};
        MASIRAH_CONSTITUENTS[IDX_Q1]      = new double[]{0.04492036, 351.2166};
        MASIRAH_CONSTITUENTS[IDX_K2]      = new double[]{0.06301705, 190.4369};
        MASIRAH_CONSTITUENTS[IDX_M4]      = new double[]{0.02481191, 287.0855};
        MASIRAH_CONSTITUENTS[IDX_MS4]     = new double[]{0.01957121, 330.3619};
        MASIRAH_CONSTITUENTS[IDX_MN4]     = new double[]{0.01157612, 268.3971};
        MASIRAH_CONSTITUENTS[IDX_2N2]     = new double[]{0.02224816, 116.2897};
        MASIRAH_CONSTITUENTS[IDX_S4]      = new double[]{0.00373421, 18.6880};
        MASIRAH_CONSTITUENTS[IDX_SA]      = new double[]{0.02522256, 21.0020};
        MASIRAH_CONSTITUENTS[IDX_SSA]     = new double[]{0.03299635, 139.9675};
        MASIRAH_CONSTITUENTS[IDX_MM]      = new double[]{0.00350334, 133.7922};
        MASIRAH_CONSTITUENTS[IDX_MF]      = new double[]{0.00694892, 14.8071};
        MASIRAH_CONSTITUENTS[IDX_M6]      = new double[]{0.00174055, 82.5024};
        MASIRAH_CONSTITUENTS[IDX_M1]      = new double[]{0.01726535, 257.3425};
        MASIRAH_CONSTITUENTS[IDX_J1]      = new double[]{0.02428377, 0.1564};
        MASIRAH_CONSTITUENTS[IDX_OO1]     = new double[]{0.01597166, 26.6319};
        MASIRAH_CONSTITUENTS[IDX_S1]      = new double[]{0.02607897, 118.9441};
        MASIRAH_CONSTITUENTS[IDX_RHO1]    = new double[]{0.00791915, 352.9461};
        MASIRAH_CONSTITUENTS[IDX_2Q1]     = new double[]{0.00675428, 339.8422};
        MASIRAH_CONSTITUENTS[IDX_NU2]     = new double[]{0.02945032, 154.0415};
        MASIRAH_CONSTITUENTS[IDX_MU2]     = new double[]{0.01714332, 145.4059};
        MASIRAH_CONSTITUENTS[IDX_L2]      = new double[]{0.01658332, 160.7903};
        MASIRAH_CONSTITUENTS[IDX_T2]      = new double[]{0.01650002, 202.4852};
        MASIRAH_CONSTITUENTS[IDX_LAMBDA2] = new double[]{0.00573131, 132.4206};
        MASIRAH_CONSTITUENTS[IDX_2SM2]    = new double[]{0.00489158, 336.3531};
        MASIRAH_CONSTITUENTS[IDX_R2]      = new double[]{0.00174294, 287.8046};
        MASIRAH_CONSTITUENTS[IDX_M3]      = new double[]{0.00727712, 152.8783};
    }
    private static final double MASIRAH_LAT = 20.687;
    private static final double MASIRAH_LON = 58.872;

    // ===== TICON-4 Muscat Station (muscat-110a-omn-uhslc_rq) =====
    // Source: Same TICON-4 publication. CC BY 4.0
    // Latitude: 23.628, Longitude: 58.565
    // Z0 (MSL above LAT) = 1.900m (MSL=2.324, LAT=0.424)
    private static final double Z0_MUSCAT = 1.900;
    private static final double[][] MUSCAT_CONSTITUENTS = new double[NUM_CONSTITUENTS][2];
    static {
        MUSCAT_CONSTITUENTS[IDX_M2]      = new double[]{0.63529783, 159.8115};
        MUSCAT_CONSTITUENTS[IDX_S2]      = new double[]{0.24135936, 190.2711};
        MUSCAT_CONSTITUENTS[IDX_N2]      = new double[]{0.15782833, 140.2610};
        MUSCAT_CONSTITUENTS[IDX_K1]      = new double[]{0.39269852, 341.4488};
        MUSCAT_CONSTITUENTS[IDX_O1]      = new double[]{0.20542370, 343.1163};
        MUSCAT_CONSTITUENTS[IDX_P1]      = new double[]{0.11938481, 340.2778};
        MUSCAT_CONSTITUENTS[IDX_Q1]      = new double[]{0.04347749, 343.5320};
        MUSCAT_CONSTITUENTS[IDX_K2]      = new double[]{0.06237048, 184.1380};
        MUSCAT_CONSTITUENTS[IDX_M4]      = new double[]{0.00648579, 198.4794};
        MUSCAT_CONSTITUENTS[IDX_MS4]     = new double[]{0.00329725, 293.7129};
        MUSCAT_CONSTITUENTS[IDX_MN4]     = new double[]{0.00301708, 147.4678};
        MUSCAT_CONSTITUENTS[IDX_2N2]     = new double[]{0.02545040, 100.9444};
        MUSCAT_CONSTITUENTS[IDX_S4]      = new double[]{0.00053473, 24.8893};
        MUSCAT_CONSTITUENTS[IDX_SA]      = new double[]{0.05669365, 109.1931};
        MUSCAT_CONSTITUENTS[IDX_SSA]     = new double[]{0.03708361, 182.7650};
        MUSCAT_CONSTITUENTS[IDX_MM]      = new double[]{0.01200993, 61.8669};
        MUSCAT_CONSTITUENTS[IDX_MF]      = new double[]{0.00823966, 12.4982};
        MUSCAT_CONSTITUENTS[IDX_M6]      = new double[]{0.00075340, 133.4663};
        MUSCAT_CONSTITUENTS[IDX_M1]      = new double[]{0.02162165, 277.2883};
        MUSCAT_CONSTITUENTS[IDX_J1]      = new double[]{0.02548050, 346.2107};
        MUSCAT_CONSTITUENTS[IDX_OO1]     = new double[]{0.02553843, 355.8150};
        MUSCAT_CONSTITUENTS[IDX_S1]      = new double[]{0.02472754, 124.3401};
        MUSCAT_CONSTITUENTS[IDX_RHO1]    = new double[]{0.00873157, 346.9967};
        MUSCAT_CONSTITUENTS[IDX_2Q1]     = new double[]{0.00712748, 331.7141};
        MUSCAT_CONSTITUENTS[IDX_NU2]     = new double[]{0.03109243, 145.2458};
        MUSCAT_CONSTITUENTS[IDX_MU2]     = new double[]{0.02192178, 115.5017};
        MUSCAT_CONSTITUENTS[IDX_L2]      = new double[]{0.01201048, 184.0851};
        MUSCAT_CONSTITUENTS[IDX_T2]      = new double[]{0.01343106, 192.8740};
        MUSCAT_CONSTITUENTS[IDX_LAMBDA2] = new double[]{0.00319159, 211.4949};
        MUSCAT_CONSTITUENTS[IDX_2SM2]    = new double[]{0.00113602, 11.2381};
        MUSCAT_CONSTITUENTS[IDX_R2]      = new double[]{0.00193718, 130.8130};
        MUSCAT_CONSTITUENTS[IDX_M3]      = new double[]{0.00708118, 132.6580};
    }
    private static final double MUSCAT_LAT = 23.628;
    private static final double MUSCAT_LON = 58.565;

    // ===== Station metadata (5 stations) =====
    private static final double[][] STATION_COORDS = {
        {ADEN_LAT, ADEN_LON},       // 0 - Aden
        {DJIBOUTI_LAT, DJIBOUTI_LON}, // 1 - Djibouti
        {SALALAH_LAT, SALALAH_LON},  // 2 - Salalah
        {MASIRAH_LAT, MASIRAH_LON},  // 3 - Masirah
        {MUSCAT_LAT, MUSCAT_LON},   // 4 - Muscat
    };

    private static final String[] STATION_NAMES_AR = {
        "\u0639\u062F\u0646",              // عدن
        "\u062C\u064A\u0628\u0648\u062A\u064A",        // جيبوتي
        "\u0635\u0644\u0627\u0644\u0629",            // صلالة
        "\u0645\u0635\u064A\u0631\u0629",            // مصيرة
        "\u0645\u0633\u0642\u0637",            // مسقط
    };

    private static final String[] STATION_NAMES_EN = {
        "Aden",
        "Djibouti",
        "Salalah",
        "Masirah",
        "Muscat",
    };

    private static final String[] STATION_SOURCES_AR = {
        "TICON-4 \u2013 \u0639\u062F\u0646",    // TICON-4 – عدن
        "TICON-4 \u2013 \u062C\u064A\u0628\u0648\u062A\u064A",  // TICON-4 – جيبوتي
        "TICON-4 \u2013 \u0635\u0644\u0627\u0644\u0629",  // TICON-4 – صلالة
        "TICON-4 \u2013 \u0645\u0635\u064A\u0631\u0629",  // TICON-4 – مصيرة
        "TICON-4 \u2013 \u0645\u0633\u0642\u0637",  // TICON-4 – مسقط
    };

    private static final double[] Z0_ARRAY = {
        Z0_ADEN, Z0_DJIBOUTI, Z0_SALALAH, Z0_MASIRAH, Z0_MUSCAT
    };

    private static final double[][][] ALL_CONSTITUENTS = {
        ADEN_CONSTITUENTS, DJIBOUTI_CONSTITUENTS, SALALAH_CONSTITUENTS,
        MASIRAH_CONSTITUENTS, MUSCAT_CONSTITUENTS
    };

    private static final double[] MSL_ARRAY = {
        1.451, 1.914, 2.008, 1.468, 2.324
    };

    // ===== Nodal modulation: f and u =====
    // The 18.6-year lunar nodal cycle modulates amplitudes and phases.
    // From Schureman Tables 14, 15:

    /**
     * Compute longitude of ascending node N (degrees) at time t.
     * N = N0 + N_speed * hours_since_J2000
     */
    private static double computeN(long timeMillisUTC) {
        double hoursSinceJ2000 = (timeMillisUTC - J2000_MS) / 3600000.0;
        double N = N0 + N_speed * hoursSinceJ2000;
        N = N % 360;
        if (N < 0) N += 360;
        return N;
    }

    /**
     * Nodal amplitude factor f for each constituent.
     * From Schureman Table 14.
     * FIX: Removed case IDX_MKS2 which referenced an undefined constant.
     */
    private static double nodalF(int idx, double N_deg) {
        double N_r = Math.toRadians(N_deg);
        switch (idx) {
            case IDX_M2: // M2
            case IDX_N2: // N2 (same as M2)
            case IDX_2N2: // 2N2 (same as M2)
            case IDX_NU2: // NU2 (same as M2)
            case IDX_MU2: // MU2 (same as M2)
            case IDX_LAMBDA2: // LAMBDA2 (same as M2)
            case IDX_2SM2: // 2SM2 (same as M2)
            case IDX_M3: // M3 (f_M2^1.5)
                double fM2 = 1.0 - 0.0373 * Math.cos(N_r) + 0.0005 * Math.cos(2 * N_r);
                if (idx == IDX_M3) return Math.pow(fM2, 1.5);
                return fM2;
            case IDX_S2: // S2
            case IDX_S4: // S4
            case IDX_P1: // P1
            case IDX_SA: // SA
            case IDX_SSA: // SSA
            case IDX_T2: // T2
            case IDX_R2: // R2
                return 1.0;
            case IDX_K1: // K1
            case IDX_RHO1: // RHO1 (same as K1)
                return 1.006 + 0.115 * Math.cos(N_r) - 0.009 * Math.cos(2 * N_r);
            case IDX_O1: // O1
            case IDX_Q1: // Q1 (same as O1)
            case IDX_2Q1: // 2Q1 (same as O1)
                return 1.009 + 0.187 * Math.cos(N_r) - 0.015 * Math.cos(2 * N_r);
            case IDX_K2: // K2
                return 1.024 + 0.286 * Math.cos(N_r) + 0.024 * Math.cos(2 * N_r);
            case IDX_M4: // M4 = f(M2)^2
            case IDX_MN4: // MN4 ≈ f(M2)^2
                double fM2b = 1.0 - 0.0373 * Math.cos(N_r) + 0.0005 * Math.cos(2 * N_r);
                return fM2b * fM2b;
            case IDX_MS4: // MS4 = f(M2) * f(S2)
                return 1.0 - 0.0373 * Math.cos(N_r) + 0.0005 * Math.cos(2 * N_r);
            case IDX_M6: // M6 = f(M2)^3
                double fM2c = 1.0 - 0.0373 * Math.cos(N_r) + 0.0005 * Math.cos(2 * N_r);
                return fM2c * fM2c * fM2c;
            case IDX_M1: // M1 (approximate)
                return 1.0 - 0.066 * Math.cos(N_r);
            case IDX_J1: // J1
            case IDX_OO1: // OO1 (similar to K1)
                return 1.006 + 0.115 * Math.cos(N_r) - 0.009 * Math.cos(2 * N_r);
            case IDX_S1: // S1
            case IDX_MM: // MM
            case IDX_MF: // MF
                return 1.0; // simplified for long-period
            case IDX_L2: // L2 (approximate)
                return 1.0 - 0.0373 * Math.cos(N_r) + 0.0005 * Math.cos(2 * N_r);
            default:
                return 1.0;
        }
    }

    /**
     * Nodal phase correction u (degrees) for each constituent.
     * From Schureman Table 15.
     */
    private static double nodalU(int idx, double N_deg) {
        double N_r = Math.toRadians(N_deg);
        switch (idx) {
            case IDX_M2: // M2
            case IDX_N2: // N2
            case IDX_2N2: // 2N2
            case IDX_NU2: // NU2
            case IDX_MU2: // MU2
            case IDX_LAMBDA2: // LAMBDA2
            case IDX_2SM2: // 2SM2
            case IDX_L2: // L2
                return -2.1 * Math.sin(N_r);
            case IDX_S2: // S2
            case IDX_S4: // S4
            case IDX_P1: // P1
            case IDX_SA: // SA
            case IDX_SSA: // SSA
            case IDX_T2: // T2
            case IDX_R2: // R2
            case IDX_S1: // S1
            case IDX_MM: // MM
            case IDX_MF: // MF
                return 0.0;
            case IDX_K1: // K1
            case IDX_RHO1: // RHO1 (same as K1)
            case IDX_J1: // J1
            case IDX_OO1: // OO1
                return -8.9 * Math.sin(N_r) + 0.6 * Math.sin(2 * N_r);
            case IDX_O1: // O1
            case IDX_Q1: // Q1
            case IDX_2Q1: // 2Q1
                return 10.8 * Math.sin(N_r) - 0.9 * Math.sin(2 * N_r);
            case IDX_K2: // K2
                return -12.6 * Math.sin(N_r) + 1.3 * Math.sin(2 * N_r);
            case IDX_M4: // M4 = 2*u(M2)
            case IDX_MN4: // MN4 ≈ 2*u(M2)
                return -4.2 * Math.sin(N_r);
            case IDX_MS4: // MS4 = u(M2)
                return -2.1 * Math.sin(N_r);
            case IDX_M6: // M6 = 3*u(M2)
                return -6.3 * Math.sin(N_r);
            case IDX_M1: // M1
                return 0.0; // approximate
            case IDX_M3: // M3 = 1.5*u(M2)
                return -3.15 * Math.sin(N_r);
            default:
                return 0.0;
        }
    }

    /**
     * Predict tide height at a given UTC epoch time in milliseconds.
     * h(t) = Z0 + Sum[ f_i * A_i * cos(V_i + u_i - kappa_i) ]
     */
    public static double getTideHeight(long timeMillisUTC) {
        double N_deg = computeN(timeMillisUTC);
        double[][] stationData = ALL_CONSTITUENTS[currentStationIndex];
        double Z0 = Z0_ARRAY[currentStationIndex];

        double hoursSinceJ2000 = (timeMillisUTC - J2000_MS) / 3600000.0;
        double h = Z0;
        for (int i = 0; i < NUM_CONSTITUENTS; i++) {
            double A = stationData[i][0];       // amplitude (m)
            if (A < 1e-7) continue;             // skip negligible
            double kappa = stationData[i][1];   // Greenwich phase lag (deg)
            double f = nodalF(i, N_deg);        // nodal amplitude factor
            double V = SPEEDS[i] * hoursSinceJ2000 + V0S[i]; // equilibrium argument (deg)
            double u = nodalU(i, N_deg);        // nodal phase correction (deg)
            double arg = Math.toRadians(V + u - kappa);
            h += f * A * Math.cos(arg);
        }
        return h;
    }

    /**
     * Predict tide height at a local Calendar time.
     */
    public static double getTideHeightLocal(Calendar localCal) {
        TimeZone tz = localCal.getTimeZone();
        int offset = tz.getOffset(localCal.getTimeInMillis());
        long utcMillis = localCal.getTimeInMillis() - offset;
        return getTideHeight(utcMillis);
    }

    // ===== High-level prediction API =====

    /**
     * A single predicted tide height at a specific hour.
     */
    public static class TidePoint {
        public final double hour;     // 0.0 - 24.0 local time
        public final double height;   // meters above LAT

        public TidePoint(double hour, double height) {
            this.hour = hour;
            this.height = height;
        }
    }

    /**
     * A predicted high/low tide event.
     */
    public static class TideEvent {
        public final long timeMillis;  // UTC epoch time
        public final boolean isHigh;
        public final double height;    // meters above LAT

        public TideEvent(long timeMillis, boolean isHigh, double height) {
            this.timeMillis = timeMillis;
            this.isHigh = isHigh;
            this.height = height;
        }
    }

    /**
     * Summary of a day's tides.
     */
    public static class DayTideSummary {
        public Calendar date;
        public double maxHeight = Double.MIN_VALUE;
        public double minHeight = Double.MAX_VALUE;
        public TideEvent firstHigh, secondHigh;
        public TideEvent firstLow, secondLow;
    }

    /**
     * Get 289 tide points for a day (every 5 minutes from 00:00 to 24:00 local)
     * for chart rendering.
     */
    public static List<TidePoint> getDayTidePoints(Calendar dayLocal) {
        List<TidePoint> points = new ArrayList<>();
        Calendar cal = (Calendar) dayLocal.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        TimeZone tz = dayLocal.getTimeZone();
        int offset = tz.getOffset(cal.getTimeInMillis());
        long utcBase = cal.getTimeInMillis() - offset;

        double stepHours = 5.0 / 60.0;
        for (double h = 0; h <= 24.001; h += stepHours) {
            long t = utcBase + (long)(h * 3600000);
            double height = getTideHeight(t);
            points.add(new TidePoint(h, height));
        }
        return points;
    }

    /**
     * Find high/low tide events for a day using derivative sign changes.
     */
    public static List<TideEvent> getDayTideEvents(Calendar dayLocal) {
        List<TideEvent> events = new ArrayList<>();
        Calendar cal = (Calendar) dayLocal.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        TimeZone tz = dayLocal.getTimeZone();
        int offset = tz.getOffset(cal.getTimeInMillis());
        long utcBase = cal.getTimeInMillis() - offset;

        double stepHours = 0.1;  // 6 minutes
        int steps = (int)(24.0 / stepHours) + 1;

        double prevH = getTideHeight(utcBase);
        double prevSlope = 0;
        long prevTime = utcBase;

        for (int i = 1; i <= steps; i++) {
            long t = utcBase + (long)(i * stepHours * 3600000);
            double h = getTideHeight(t);
            double slope = h - prevH;

            if (prevSlope > 0 && slope <= 0 && i > 1) {
                long exactTime = refineExtremum(prevTime, t, true);
                double exactH = getTideHeight(exactTime);
                events.add(new TideEvent(exactTime, true, exactH));
            } else if (prevSlope < 0 && slope >= 0 && i > 1) {
                long exactTime = refineExtremum(prevTime, t, false);
                double exactH = getTideHeight(exactTime);
                events.add(new TideEvent(exactTime, false, exactH));
            }

            prevSlope = slope;
            prevH = h;
            prevTime = t;
        }
        return events;
    }

    /**
     * Binary search refinement for exact time of a tide extremum.
     */
    private static long refineExtremum(long t1, long t2, boolean isHigh) {
        long lo = t1;
        long hi = t2;
        for (int iter = 0; iter < 40; iter++) {
            long mid = (lo + hi) / 2;
            double hMid = getTideHeight(mid);
            double hBefore = getTideHeight(mid - 120000); // 2 min before
            double slope = hMid - hBefore;
            if (isHigh) {
                if (slope > 0) lo = mid; else hi = mid;
            } else {
                if (slope < 0) lo = mid; else hi = mid;
            }
            if (hi - lo < 30000) break; // within 30 seconds
        }
        return (lo + hi) / 2;
    }

    /**
     * Get weekly summary of tide events for the next 7 days.
     */
    public static List<DayTideSummary> getWeeklySummary() {
        List<DayTideSummary> summary = new ArrayList<>();
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        for (int i = 0; i < 7; i++) {
            DayTideSummary day = new DayTideSummary();
            day.date = (Calendar) cal.clone();
            List<TideEvent> events = getDayTideEvents(cal);
            for (TideEvent e : events) {
                if (e.isHigh) {
                    if (day.firstHigh == null) day.firstHigh = e;
                    else if (day.secondHigh == null) day.secondHigh = e;
                } else {
                    if (day.firstLow == null) day.firstLow = e;
                    else if (day.secondLow == null) day.secondLow = e;
                }
                if (e.height > day.maxHeight) day.maxHeight = e.height;
                if (e.height < day.minHeight) day.minHeight = e.height;
            }
            // Fallback if no events found
            if (day.maxHeight == Double.MIN_VALUE) day.maxHeight = Z0_ARRAY[currentStationIndex];
            if (day.minHeight == Double.MAX_VALUE) day.minHeight = Z0_ARRAY[currentStationIndex];
            summary.add(day);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return summary;
    }

    // ===== User location and station selection =====

    public static void setUserLocation(double lat, double lon) {
        userLat = lat;
        userLon = lon;
        currentStationIndex = findNearestStation(lat, lon);
    }

    public static int getCurrentStationIndex() {
        return currentStationIndex;
    }

    public static String getStationNameAr() {
        return STATION_NAMES_AR[currentStationIndex];
    }

    public static String getStationNameEn() {
        return STATION_NAMES_EN[currentStationIndex];
    }

    public static String getStationSourceAr() {
        return STATION_SOURCES_AR[currentStationIndex];
    }

    /**
     * Haversine distance in km from user to current station.
     */
    public static double getDistanceToNearestStationKm() {
        double[] coords = STATION_COORDS[currentStationIndex];
        return haversineKm(userLat, userLon, coords[0], coords[1]);
    }

    public static double getUserLat() { return userLat; }
    public static double getUserLon() { return userLon; }

    private static int findNearestStation(double lat, double lon) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < STATION_COORDS.length; i++) {
            double d = haversineKm(lat, lon, STATION_COORDS[i][0], STATION_COORDS[i][1]);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Get current tide status: rising or falling.
     */
    public static boolean isTideRising() {
        long now = System.currentTimeMillis();
        double h1 = getTideHeight(now);
        double h2 = getTideHeight(now + 600000);  // 10 min later
        return h2 > h1;
    }

    /**
     * Get current tide height for right now.
     */
    public static double getCurrentTideHeight() {
        return getTideHeight(System.currentTimeMillis());
    }

    /**
     * Get the Z0 datum for current station.
     */
    public static double getZ0() {
        return Z0_ARRAY[currentStationIndex];
    }

    /**
     * Get datum info for current station (Arabic).
     */
    public static String getDatumInfoAr() {
        double msl = MSL_ARRAY[currentStationIndex];
        return "\u0645\u0633\u062A\u0648\u0649 \u0627\u0644\u0628\u062D\u0631 \u0627\u0644\u0645\u062A\u0648\u0633\u0637: "
            + String.format(java.util.Locale.US, "%.3f", msl)
            + "\u0645 \u0641\u0648\u0642 LAT";
    }

    /**
     * Get TICON-4 attribution text (Arabic).
     */
    public static String getAttributionAr() {
        return "\u0628\u064A\u0627\u0646\u0627\u062A \u0627\u0644\u0645\u062F \u0648\u0627\u0644\u062C\u0632\u0631 \u0645\u0646 TICON-4 (CC BY 4.0) \u2014 Hart-Davis, Dettmering & Seitz 2025";
    }

    /**
     * Get total number of stations.
     */
    public static int getStationCount() {
        return STATION_COORDS.length;
    }
}
