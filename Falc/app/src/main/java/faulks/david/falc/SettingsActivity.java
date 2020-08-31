package faulks.david.falc;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        settings = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.SA_SettingsFragment);
    }

    private SettingsFragment settings;




}
