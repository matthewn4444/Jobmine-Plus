package com.jobmineplus.mobile.activities.tab;

import android.os.Bundle;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.jobmineplus.mobile.R;

public class DescriptionTabMapActivity extends MapActivity{

	protected MapView map;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.description_map_tab);
		map = (MapView) findViewById(R.id.map_view);
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
