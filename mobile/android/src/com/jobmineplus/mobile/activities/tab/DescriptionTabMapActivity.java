package com.jobmineplus.mobile.activities.tab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.jobmineplus.mobile.JbmnplsApplication;
import com.jobmineplus.mobile.R;
import com.jobmineplus.mobile.services.JbmnplsHttpService;
import com.jobmineplus.mobile.widgets.JbmnplsAsyncTaskBase;
import com.jobmineplus.mobile.widgets.Job;

public class DescriptionTabMapActivity extends MapActivity{

    protected MapView map;
    protected MapController mpCntrlr; 
    protected Geocoder geoCoder;
    protected String query;
    protected JbmnplsHttpService service;
    
    private final int DEFAULT_MAX_RESULTS = 5;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.description_map_tab);
        map = (MapView) findViewById(R.id.map_view);
        map.setBuiltInZoomControls(true);
        mpCntrlr = map.getController();
        geoCoder = new Geocoder(this, Locale.getDefault());
        service = JbmnplsHttpService.getInstance();
        
        if (getIntent() != null && getIntent().getExtras() != null) {
            int jobId = getIntent().getExtras().getInt("jobId");
            Job job = ((JbmnplsApplication) getApplication()).getJob(jobId);
            //query = job.getEmployer() + ", " + job.getLocation();
            query = "Bank of America Merrill Lynch, Toronto";
//            setLocationWithString(query);
            
            new SetLocationTask(this).execute();
        }
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }
    
    protected void setLocationWithString(String query) {
        setLocationWithString(query, 0);
    }
    
    protected void setLocationWithString(String query, int maxResults) {
        List<Address> addresses = null;
        try {
            log(maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS);
            addresses = geoCoder.getFromLocationName(query, 
                    maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS);
        } catch (IOException e) {
            System.out.println("Are you running this on 2.2 emulator?");
            e.printStackTrace();
        }
        log(addresses);
        if (addresses != null && !addresses.isEmpty()) {
            GeoPoint p = new GeoPoint(
                    convertToGeoCoordinate(addresses.get(0).getLatitude()),
                    convertToGeoCoordinate(addresses.get(0).getLongitude()));
            mpCntrlr.animateTo(p);
            mpCntrlr.setZoom(16);
            map.invalidate();
        }
    }
    
    protected int convertToGeoCoordinate (double num) {
        return (int) (num * 1E6);
    }
    
    private void log(Object text) {
        System.out.println(text);
    }
    
    final class SetLocationTask extends JbmnplsAsyncTaskBase<Void, Void,  GeoPoint[]> {

        public SetLocationTask(Activity activity) {
            super(activity);
        }
        
        @Override
        protected void onPostExecute( GeoPoint[] result) {
            super.onPostExecute(result);
            log("Finished");
            if (result != null && result.length > 0) {
                GeoPoint point = result[0];
                mpCntrlr.animateTo(point);
                mpCntrlr.setZoom(16);
                map.invalidate();
            }
        }

        @Override
        protected GeoPoint[] doInBackground(Void... arg0) {
            GeoPoint[] addresses = getGeoPointFromGeocoder();
            
            return addresses;
        }
        
        protected GeoPoint[] getGeoPointFromGeocoder() {
            List<Address> addresses = null;
            GeoPoint[] geoList = new GeoPoint[DEFAULT_MAX_RESULTS];
            try {
                addresses = geoCoder.getFromLocationName(query, DEFAULT_MAX_RESULTS);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            if (addresses.isEmpty()) {
                log("Didnt get it");
                return getGeoPointFromGoogleApi();
            }
            for (int i = 0; i < DEFAULT_MAX_RESULTS; i++) {
                geoList[i] = new GeoPoint(
                        (int) (addresses.get(i).getLatitude() * 1E6),
                        (int) (addresses.get(i).getLongitude() * 1E6));
            }
            log(geoList);
            return geoList;
        }
        
        protected GeoPoint[] getGeoPointFromGoogleApi() {
            String url;
            JSONArray json = null;
            try {
                url = new URI("https", "maps.googleapis.com", "/maps/api/geocode/json", 
                        "sensor=false&address=" + query, null).toASCIIString(); 
                log(url);
                service.getJSON(url);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
            
            
            
            
            log(json);
            log(json==null);
            
            
            return null;
        }
        
        //TODO implement from cache
        
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
