package app.organicmaps.hawsa;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import app.organicmaps.R;

public class HawsaTidesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hawsa_tides);

        TextView tvTitle = findViewById(R.id.tv_tides_title);
        if (tvTitle != null) tvTitle.setText(R.string.hawsa_tides_title);

        TextView tvInfo = findViewById(R.id.tv_tides_info);
        if (tvInfo != null) tvInfo.setText(R.string.hawsa_tides_info);
    }
}