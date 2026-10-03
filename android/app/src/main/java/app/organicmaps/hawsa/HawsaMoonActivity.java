package app.organicmaps.hawsa;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import app.organicmaps.R;

public class HawsaMoonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hawsa_moon);

        TextView tvTitle = findViewById(R.id.tv_moon_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_moon_title);

        TextView tvInfo = findViewById(R.id.tv_moon_info);
        if (tvInfo != null) tvInfo.setText(R.string.hawsa_moon_info);
    }
}