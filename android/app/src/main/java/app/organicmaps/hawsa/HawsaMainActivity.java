package app.organicmaps.hawsa;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import app.organicmaps.MwmActivity;
import app.organicmaps.R;

public class HawsaMainActivity extends AppCompatActivity {

    private static final double DEFAULT_LAT = 19.4431;
    private static final double DEFAULT_LON = 40.5167;
    private static final int DEFAULT_ZOOM = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Window setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.hawsa_status_bar_color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = getWindow().getDecorView();
                int flags = decor.getSystemUiVisibility();
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decor.setSystemUiVisibility(flags);
            }
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_hawsa_main);

        // ===== البطاقات الأربع =====

        // 1) الخريطة — يفتح MwmActivity (Organic Maps) مع إحداثيات الحسوة
        findViewById(R.id.card_map).setOnClickListener(v -> openMap());

        // 2) المد والجزر
        findViewById(R.id.card_tides).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, HawsaTidesActivity.class));
            } catch (Exception e) { /* TODO: handle */ }
        });

        // 3) معلومات القمر
        findViewById(R.id.card_moon).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, HawsaMoonActivity.class));
            } catch (Exception e) { /* TODO: handle */ }
        });

        // 4) البوصلة واتجاه القبلة
        findViewById(R.id.card_compass).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, HawsaCompassActivity.class));
            } catch (Exception e) { /* TODO: handle */ }
        });
    }

    /**
     * فتح خريطة الحسوة عبر MwmActivity مع تمرير الإحداثيات
     */
    private void openMap() {
        Intent intent = new Intent(this, MwmActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(
            "geo:" + DEFAULT_LAT + "," + DEFAULT_LON + "?z=" + DEFAULT_ZOOM));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}