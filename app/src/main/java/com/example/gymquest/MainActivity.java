package com.example.gymquest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- GATEKEEPER CHECK: Ensure user is logged in ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_maps);

        // 1. Setup "Add Gym" Button
        Button btnAdd = findViewById(R.id.btnAddGymPage);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddGymActivity.class);
            startActivity(intent);
        });

        // 2. Setup "Profile" Button (Formerly Logout)
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // --- NEW CODE START ---
        // 3. Setup "View List" Button
        Button btnList = findViewById(R.id.btnViewList);
        btnList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GymListActivity.class);
            startActivity(intent);
        });
        // --- NEW CODE END ---

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            loadGymsFromFirebase();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // --- NEW: Add Padding to move map controls down ---
        // Usage: setPadding(left, top, right, bottom)
        // We add 200 pixels to the TOP to clear your Profile button
        mMap.setPadding(0, 200, 0, 0);
        // -------------------------------------------------------

        enableUserLocation();
        loadGymsFromFirebase();

        // Default view: Shah Alam
        LatLng shahAlam = new LatLng(3.0733, 101.5185);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shahAlam, 10f));

        // --- UPDATED CLICK LISTENER START ---
        mMap.setOnMarkerClickListener(marker -> {
            String gymName = marker.getTitle();
            String gymImage = (String) marker.getTag();

            // 1. Get the coordinates of the clicked marker
            double lat = marker.getPosition().latitude;
            double lng = marker.getPosition().longitude;

            Intent intent = new Intent(MainActivity.this, GymDetailActivity.class);
            intent.putExtra("GYM_NAME", gymName);
            intent.putExtra("GYM_IMAGE", gymImage);

            // 2. Pass them to the next screen
            intent.putExtra("GYM_LAT", lat);
            intent.putExtra("GYM_LNG", lng);

            startActivity(intent);

            return false;
        });
        // --- UPDATED CLICK LISTENER END ---
    }

    private void loadGymsFromFirebase() {
        if (mMap != null) {
            mMap.clear();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Gyms")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String name = document.getString("name");
                                Double lat = document.getDouble("lat");
                                Double lng = document.getDouble("lng");
                                String imageStr = document.getString("imageStr");

                                if (name != null && lat != null && lng != null) {
                                    LatLng gymPos = new LatLng(lat, lng);

                                    Marker m = mMap.addMarker(new MarkerOptions()
                                            .position(gymPos)
                                            .title(name)
                                            .icon(bitmapDescriptorFromVector(this, R.drawable.ic_gym_marker)));

                                    if (m != null) {
                                        m.setTag(imageStr);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to load gyms", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        if (vectorDrawable == null) return null;

        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}