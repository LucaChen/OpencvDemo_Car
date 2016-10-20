package com.example.lucas.opencvdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.Map;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends Activity {

    private static final String TAG ="MainActivity";

    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG,"Opencv not loaded");

        }else {
            Log.d(TAG,"Opencv loaded");

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,new SettingsFragment()).commit();
        setContentView(R.layout.activity_main);
    }
    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            // Load the preferences from an XML resource
        }

        @Override
        public void onResume() {
            super.onResume();

            // Set up initial values for all list preferences
            Map<String, ?> sharedPreferencesMap = getPreferenceScreen().getSharedPreferences().getAll();
            Preference pref;
            ListPreference listPref;
            for (Map.Entry<String, ?> entry : sharedPreferencesMap.entrySet()) {
                pref = findPreference(entry.getKey());
                if (pref instanceof ListPreference) {
                    listPref = (ListPreference) pref;
                    pref.setSummary(listPref.getEntry());
                }
            }

            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            if (pref instanceof ListPreference) {
                ListPreference listPref = (ListPreference) pref;
                pref.setSummary(listPref.getEntry());
            }

            getActivity().setResult(-1);
        }
        public class SiriListItem {
            String message;
            boolean isSiri;

            public SiriListItem(String msg, boolean siri) {
                message = msg;
                isSiri = siri;
            }
        }
    }
    @Override
    protected void onDestroy() {
        /* unbind from the service */

        super.onDestroy();
    }
}
