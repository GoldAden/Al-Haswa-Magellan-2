package app.organicmaps.hawsa;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import app.organicmaps.R;

public class HawsaCompassActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hawsa_compass);

        TextView tvTitle = findViewById(R.id.tv_compass_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_compass_title);

        TextView tvQibla = findViewById(R.id.tv_qibla_info);
        if (tvQibla != null) tvQibla.setText(R.string.hawsa_qibla_info);
    }
}