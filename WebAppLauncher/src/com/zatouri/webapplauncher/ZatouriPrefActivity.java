package com.zatouri.webapplauncher;

import android.os.Bundle;

public class ZatouriPrefActivity extends android.preference.PreferenceActivity {
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {        
        super.onCreate(savedInstanceState);        
        addPreferencesFromResource(R.xml.preference);        
    }
}
