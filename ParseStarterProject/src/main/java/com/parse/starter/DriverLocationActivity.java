package com.parse.starter;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class DriverLocationActivity extends FragmentActivity implements OnMapReadyCallback {

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driver_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        intent = getIntent();

        //Get Locations
        LatLng driverLocation = new LatLng(intent.getDoubleExtra("driverLatitude", 0),
                intent.getDoubleExtra("driverLongitude", 0));
        LatLng requestLocation = new LatLng(intent.getDoubleExtra("requestLatitude", 0),
                intent.getDoubleExtra("requestLongitude", 0));

        //add driver & requests location to an array list of Markers
        ArrayList<Marker> markers = new ArrayList<>();
        markers.add(googleMap.addMarker(new MarkerOptions().position(driverLocation).title("Your Location")));
        markers.add(googleMap.addMarker(new MarkerOptions().position(requestLocation).title("Request Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))));

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
        googleMap.animateCamera(cameraUpdate);
    }

    public void acceptRequest(View view) {

        //update the request
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", intent.getStringExtra("username"));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null && objects.size() > 0) {
                    for (ParseObject object : objects) {
                        object.put("driverUserName", ParseUser.getCurrentUser().getUsername());
                        object.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    //get directions
                                    Intent directionsIntent = new Intent(android.content.Intent.ACTION_VIEW,
                                            Uri.parse("http://maps.google.com/maps?saddr="
                                                    + intent.getDoubleExtra("driverLatitude",0)
                                                    + ","
                                                    + intent.getDoubleExtra("driverLongitude",0)
                                                    + "&daddr="
                                                    + intent.getDoubleExtra("requestLatitude",0)
                                                    + ","
                                                    + intent.getDoubleExtra("requestLongitude",0)));
                                    startActivity(directionsIntent);
                                }
                            }
                        });
                    }
                }
            }
        });

    }
}