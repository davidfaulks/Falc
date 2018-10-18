package faulks.david.falc;

import android.support.v7.app.AppCompatActivity;
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
