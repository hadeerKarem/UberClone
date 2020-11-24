package com.parse.starter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    String TAG = "RiderActivityLogTag";
    Button buttonCallUber;
    Boolean requestActive = false;
    Handler handler = new Handler();
    TextView textViewInfo;
    Boolean driverActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);

        buttonCallUber = findViewById(R.id.buttonCallUber);
        textViewInfo = findViewById(R.id.textViewInfo);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    requestActive = true;
                    buttonCallUber.setText("Cancel Uber");

                    checkForUpdates();
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateMap(lastKnownLocation);
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateMap(location);
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                updateMap(lastKnownLocation);
            }
        }
    }

    public void updateMap(Location location) {
        if (!driverActive) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }

    public void callUber(View view) {
        Log.i(TAG, "Call Uber");

        if (requestActive) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null && objects.size() > 0) {
                        for (ParseObject object : objects) {
                            object.deleteInBackground();
                        }

                        requestActive = false;
                        buttonCallUber.setText("Call An Uber");
                    }
                }
            });

        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, locationListener);
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    request.put("location", parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                buttonCallUber.setText("Cancel Uber");
                                requestActive = true;

                                checkForUpdates();
                            }
                        }
                    });
                } else {
                    Toast.makeText(this, "Couldn't find Location!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void checkForUpdates() {
        final ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUserName");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    driverActive = true;
                    ParseQuery<ParseUser> userParseQuery = ParseUser.getQuery();
                    userParseQuery.whereEqualTo("username", objects.get(0).getString("driverUserName"));
                    userParseQuery.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if (e == null && objects.size() > 0) {
                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");

                                if (ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                        == PackageManager.PERMISSION_GRANTED) {
                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    if (lastKnownLocation != null) {
                                        ParseGeoPoint userLocation = new ParseGeoPoint
                                                (lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                                        double distanceInKilometers = driverLocation.distanceInKilometersTo(userLocation);
                                        double distanceOneDP = (double) Math.round(distanceInKilometers * 10) / 10;

                                        if (distanceInKilometers <= 0.2) {
                                            Log.i(TAG, "driver is very near");
                                            textViewInfo.setText("Your driver is here!");

                                            ParseQuery<ParseObject> requestParseQuery = ParseQuery.getQuery("Request");
                                            requestParseQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                            requestParseQuery.findInBackground(new FindCallback<ParseObject>() {
                                                @Override
                                                public void done(List<ParseObject> objects, ParseException e) {
                                                    if (e == null && objects.size() > 0) {
                                                        for (ParseObject object : objects) {
                                                            object.deleteInBackground();
                                                        }
                                                    }
                                                }
                                            });

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    textViewInfo.setText("");
                                                    textViewInfo.setBackgroundColor(getResources().getColor(R.color.transparent));
                                                    buttonCallUber.setVisibility(View.VISIBLE);
                                                    buttonCallUber.setText("Call an Uber");
                                                    requestActive = false;
                                                    driverActive = false;
                                                }
                                            }, 5000);

                                        } else {
                                            textViewInfo.setText("Your driver is " + distanceOneDP + " Km away");

                                            //Get Locations
                                            LatLng yourUberLocation = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                            LatLng yourLocation = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                            //add driver & requests location to an array list of Markers
                                            ArrayList<Marker> markers = new ArrayList<>();
                                            mMap.clear();
                                            markers.add(mMap.addMarker(new MarkerOptions().position(yourUberLocation).title("Driver Location")
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))));
                                            markers.add(mMap.addMarker(new MarkerOptions().position(yourLocation).title("Your Location")));

                                            //calculate the bounds of all the markers
                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();

                                            //obtain the movement description object by using the factory
                                            int padding = 64; // offset from edges of the map in pixels
                                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                            //move the camera
                                            mMap.animateCamera(cameraUpdate);

                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdates();
                                                }
                                            }, 2000);
                                        }
                                    }
                                }
                            }
                        }
                    });

                    buttonCallUber.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void logout(View view) {
        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}