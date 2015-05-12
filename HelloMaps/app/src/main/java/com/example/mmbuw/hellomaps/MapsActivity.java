package com.example.mmbuw.hellomaps;

import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import static com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private EditText txtMarker;
    SharedPreferences sharedPreferences;
    int locationCount = 0;
    private ArrayList<Marker> markers = new ArrayList<Marker>(); // to keep list of markers on the map
    private ArrayList<Circle> circles=new ArrayList<Circle>(); // to keep list of circles on the map


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        txtMarker=(EditText)findViewById(R.id.txtMarkerName);
        mMap.setOnMapLongClickListener(this); // to add markers after long click
        setOnCameraChangeListener(); // to add circles after camera change
    }

    private void setOnCameraChangeListener() {

        mMap.setOnCameraChangeListener(new OnCameraChangeListener() {

            public void onCameraChange(CameraPosition position) {

                double radius=0.0;
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds; // getting visible part of the map bounds

                //remove all circles, in order to draw new circles due to camera change
                for(Circle circle:circles){
                    circle.remove();
                }

                //check if marker is not in bounds, draw a circle around that
                for (Marker marker : markers) {

                    if (!bounds.contains(marker.getPosition())) {
                        radius=getRadius(bounds,marker.getPosition()); // this will return the radius for the circle we want to draw
                        circles.add(mMap.addCircle(new CircleOptions().center(marker.getPosition()).radius(radius))); // add a circle to map, marker used as center
                    }
                }



            }
        });
    }

    private double getRadius(LatLngBounds bounds,LatLng position) {
        double radius=0.0;
        //Store 8 points on bounds
        ArrayList<LatLng> points=new ArrayList<LatLng>();
        //find 8 points on bounds
        //Bounds= currennt view bounds,  position= marker position
        points=getPoints(bounds,position);
        radius=getMinRadius(points,position);
        return radius;
    }

    //this function returns minimum radius for the circle, concerning distance to 8 points on bounds
    private double getMinRadius(ArrayList<LatLng> points, LatLng position) {
        double radius=0.0;
        float[] tmpRad=new float[8];
        for(LatLng point:points){
            Location.distanceBetween(point.latitude,point.longitude,position.latitude,position.longitude,tmpRad);
            if(radius==0 || radius>tmpRad[0]){
                radius=tmpRad[0];
            }
        }
        //multiplied because the radius must be a little more than the distance, in order to be visible in current bounds
        return radius*1.03;
    }

    //with this function we get 8 points on the bounds to find the minimum radius for the circle to draw
    private ArrayList<LatLng> getPoints(LatLngBounds bounds, LatLng position) {
        ArrayList<LatLng> points=new ArrayList<LatLng>();
        //northeast
        LatLng northeast=bounds.northeast;
        points.add(northeast);
        //southeast
        LatLng southeast=new LatLng(bounds.northeast.latitude,bounds.southwest.longitude);
        points.add(southeast);
        //southwest
        LatLng southwest=bounds.southwest;
        points.add(southwest);
        //northwest
        LatLng northwest=new LatLng(bounds.southwest.latitude,bounds.northeast.longitude);
        points.add(northwest);
        //north
        LatLng north=new LatLng((northeast.latitude+northwest.latitude)/2,northeast.longitude);
        points.add(north);
        //east
        LatLng east=new LatLng(northeast.latitude,(northeast.longitude+southeast.longitude)/2);
        points.add(east);
        //south
        LatLng south=new LatLng((southeast.latitude+southwest.latitude)/2,southeast.longitude);
        points.add(south);
        //west
        LatLng west=new LatLng(southwest.latitude,(northwest.longitude+southwest.longitude)/2);
        points.add(west);

        return points;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        //specify current location
        getCurrentLocation();

        //getting saved locations which stored in shared preferences
        readSavedLocations();

    }

    private void readSavedLocations() {

        // http://wptrafficanalyzer.in/blog/adding-multiple-marker-locations-in-google-maps-android-api-v2-and-save-it-in-shared-preferences/
        // Opening the sharedPreferences object
        sharedPreferences = getSharedPreferences("location", 0);

        // Getting number of locations already stored
        locationCount = sharedPreferences.getInt("locationCount", 0);

        // Getting stored zoom level if exists else return 0
        String zoom = sharedPreferences.getString("zoom", "0");

        // If locations are already saved
        if (locationCount != 0) {

            String lat = "";
            String lon = "";
            String title = "";

            // Iterating through all the locations stored
            for (int i = 0; i < locationCount; i++) {

                // Getting the latitude of the i-th location
                lat = sharedPreferences.getString("lat" + i, "0");

                // Getting the longitude of the i-th location
                lon = sharedPreferences.getString("lng" + i, "0");

                // Getting the longitude of the i-th location
                title = sharedPreferences.getString("title" + i, "");
                LatLng point = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                // Drawing marker on the map
                Marker marker=mMap.addMarker(new MarkerOptions().position(point).title(title));
                markers.add(marker);
            }
        }
    }
    @Override
    public void onMapLongClick(LatLng point) {
        String title=txtMarker.getText().toString();
        if(!title.equals("")){
            locationCount++;
            Marker marker=mMap.addMarker(new MarkerOptions().position(point).title(title));
            markers.add(marker);
            saveLocation((double) point.latitude, (double) point.longitude, title, locationCount);
            txtMarker.setText("");
        }
        else{
            Toast.makeText(this, "Enter marker name first!",Toast.LENGTH_LONG).show();
        }

    }



    private void saveLocation(double lat, double lon, String title, int locCount) {
        /** Opening the editor object to write data to sharedPreferences */
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Storing the latitude for the i-th location
        editor.putString("lat"+ Integer.toString((locCount-1)), Double.toString(lat));

        // Storing the longitude for the i-th location
        editor.putString("lon"+ Integer.toString((locCount-1)), Double.toString(lon));

        // Storing the title for the i-th location
        editor.putString("title"+ Integer.toString((locCount-1)), title);

        // Storing the count of locations or marker count
        editor.putInt("locationCount", locCount);

        /** Storing the zoom level to the shared preferences */
        editor.putString("zoom", Float.toString(mMap.getCameraPosition().zoom));

        /** Saving the values stored in the shared preferences */
        editor.commit();

        Toast.makeText(getBaseContext(), "Marker is saved!", Toast.LENGTH_SHORT).show();
    }

    private void getCurrentLocation() {
        LocationManager locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
        Criteria crt=new Criteria();
        String provider=locationManager.getBestProvider(crt,true);
        Location myLoc=locationManager.getLastKnownLocation(provider);
        double lat=0,lon=0;
        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Marker marker;
        if(isGPSEnabled && isNetworkEnabled){
            lat = myLoc.getLatitude();
            lon = myLoc.getLongitude();
            marker=mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("You are here!"));
            markers.add(marker);
        }else if (myLoc!=null){
            Location   getLastLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            lon = getLastLocation.getLongitude();
            lat = getLastLocation.getLatitude();
            marker=mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("You are here!"));
            markers.add(marker);
        }
        else {
            Toast.makeText(this, "Unable to find your location.GPS or Network is not available.",Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }
}


