package com.example.gymquest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // --- NEW IMPORT for Safe Formatting ---
import java.util.Map;

public class GymDetailActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewsAdapter adapter;
    private List<Map<String, Object>> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_detail);

        TextView tvName = findViewById(R.id.tvDetailName);
        ImageView imgGym = findViewById(R.id.imgDetail);
        Button btnClose = findViewById(R.id.btnClose);
        Button btnReview = findViewById(R.id.btnWriteReview);

        // --- Bind the Navigate Button ---
        Button btnNavigate = findViewById(R.id.btnNavigate);

        // Setup RecyclerView
        rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewsAdapter(reviewList);
        rvReviews.setAdapter(adapter);

        // Get data passed from MainActivity
        String name = getIntent().getStringExtra("GYM_NAME");
        String imageStr = getIntent().getStringExtra("GYM_IMAGE");

        // --- Get Coordinates ---
        double lat = getIntent().getDoubleExtra("GYM_LAT", 0.0);
        double lng = getIntent().getDoubleExtra("GYM_LNG", 0.0);

        tvName.setText(name);

        if (imageStr != null && !imageStr.isEmpty()) {
            byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imgGym.setImageBitmap(decodedByte);
        }

        // LOAD REVIEWS
        loadReviews(name);

        // --- UPDATED: Handle Navigation Click with Safety Fix ---
        btnNavigate.setOnClickListener(v -> {
            // Check for invalid coordinates
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(this, "Invalid Coordinates for this Gym", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use Locale.US to force a dot (.) separator, preventing errors on devices using commas
            String uriString = String.format(Locale.US, "google.navigation:q=%f,%f", lat, lng);
            Uri gmmIntentUri = Uri.parse(uriString);

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps"); // Force Google Maps app

            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                // If Google Maps is not installed, show a message
                Toast.makeText(GymDetailActivity.this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
            }
        });

        btnReview.setOnClickListener(v -> {
            Intent intent = new Intent(GymDetailActivity.this, AddReviewActivity.class);
            intent.putExtra("GYM_NAME", name);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> finish());
    }

    // Refresh reviews when we come back from "Add Review" page
    @Override
    protected void onResume() {
        super.onResume();
        String name = getIntent().getStringExtra("GYM_NAME");
        if(name != null) {
            loadReviews(name);
        }
    }

    private void loadReviews(String gymName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Find the Gym ID by Name
        db.collection("Gyms")
                .whereEqualTo("name", gymName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String gymId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // 2. Fetch the Reviews Sub-collection
                        db.collection("Gyms").document(gymId).collection("Reviews")
                                .get()
                                .addOnSuccessListener(reviewsSnapshot -> {
                                    reviewList.clear();
                                    for (QueryDocumentSnapshot doc : reviewsSnapshot) {
                                        reviewList.add(doc.getData());
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }
}