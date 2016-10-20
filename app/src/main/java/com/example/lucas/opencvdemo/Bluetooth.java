package com.example.lucas.opencvdemo;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Toast;

import java.util.Map;

@SuppressWarnings("deprecation")
public class Bluetooth extends TabActivity {
    /** Called when the activity is first created. */

	enum ServerOrCilent{
		NONE,
		SERVICE,
		CILENT
	};
    private Context mContext;
    static AnimationTabHost mTabHost;
    static String BlueToothAddress = "null";
    static ServerOrCilent serviceOrCilent = ServerOrCilent.NONE;
    static boolean isOpen = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        
        mContext = this;        
    	setContentView(com.example.lucas.opencvdemo.R.layout.main);
        //实例化
    	mTabHost = (AnimationTabHost) getTabHost();         
        mTabHost.addTab(mTabHost.newTabSpec("Tab1")
        		.setIndicator("设备列表",getResources().getDrawable(android.R.drawable.ic_menu_add))
        		.setContent(new Intent(mContext, deviceActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("Tab2").
        		setIndicator("对话列表",getResources().getDrawable(android.R.drawable.ic_menu_add))
        		.setContent(new Intent(mContext, chatActivity.class)));
        mTabHost.setOnTabChangedListener(new OnTabChangeListener(){
        	public void onTabChanged(String tabId) {
        		// TODO Auto-generated method stub    
        		if(tabId.equals("Tab1"))
        		{        			
        		}
        	}         
        });
        mTabHost.setCurrentTab(0);
    }
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		Toast.makeText(mContext, "address:", Toast.LENGTH_SHORT).show();

	}
	public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
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
	public class SiriListItem {
		String message;
		boolean isSiri;

		public SiriListItem(String msg, boolean siri) {
			message = msg;
			isSiri = siri;
		}
	}   
}