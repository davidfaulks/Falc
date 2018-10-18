package faulks.david.falc;

import android.content.SharedPreferences;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;

/* The settings controller, using the recommended Android methods. */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {



    private SharedPreferences currentPrefs;


    @Override public void onCreatePreferences(Bundle bundle, String s) {
        currentPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        addPreferencesFromResource(R.xml.settings);
        onSharedPreferenceChanged(currentPrefs,getString(R.string.number_format_preference));
    }

    @Override public void onResume() {
        super.onResume();
        SharedPreferences cpref = getPreferenceScreen().getSharedPreferences();
        cpref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override public void onPause() {
        SharedPreferences cpref = getPreferenceScreen().getSharedPreferences();
        cpref.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }



    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // getting the value
        Preference foundPref = findPreference(key);
        if (!(foundPref instanceof ListPreference)) return;
        ListPreference listPref = (ListPreference) foundPref;
        String formatValue = listPref.getValue();
        FormatStore.changeFormatSet(formatValue);


        /*
        int formatIndex = listPref.findIndexOfValue(currentPrefs.getString(key,""));
        if (formatIndex < 0) return; */
    }


}
