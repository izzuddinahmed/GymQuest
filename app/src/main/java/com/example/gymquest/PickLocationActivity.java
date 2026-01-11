package com.example.gymquest;

import androidx.fragment.app.FragmentActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class PickLocationActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLocation; // To store where the user tapped
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_location);

        btnConfirm = findViewById(R.id.btnConfirmLocation);

        // Send data back when "Confirm" is clicked
        btnConfirm.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LAT", selectedLocation.latitude);
                resultIntent.putExtra("LNG", selectedLocation.longitude);
                setResult(RESULT_OK, resultIntent);
                finish(); // Close this screen
            } else {
                Toast.makeText(this, "Please tap a location on the map first!", Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Default view: Shah Alam
        LatLng startPoint = new LatLng(3.0733, 101.5185);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPoint, 10f));

        Toast.makeText(this, "Tap anywhere to select a location", Toast.LENGTH_LONG).show();

        // Listen for map taps
        mMap.setOnMapClickListener(latLng -> {
            // 1. Clear old marker
            mMap.clear();
            // 2. Save the new position
            selectedLocation = latLng;
            // 3. Show a marker there
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        });
    }
}