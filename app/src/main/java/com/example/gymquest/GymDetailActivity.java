package com.example.gymquest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GymDetailActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewsAdapter adapter;
    private List<Map<String, Object>> reviewList;

    private RatingBar rbAvg;
    private TextView tvAvgInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_detail);

        TextView tvName = findViewById(R.id.tvDetailName);
        ImageView imgGym = findViewById(R.id.imgDetail);
        Button btnClose = findViewById(R.id.btnClose);
        Button btnReview = findViewById(R.id.btnWriteReview);
        Button btnNavigate = findViewById(R.id.btnNavigate);

        rbAvg = findViewById(R.id.rbAverageRating);
        tvAvgInfo = findViewById(R.id.tvAverageInfo);

        rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewsAdapter(reviewList);
        rvReviews.setAdapter(adapter);

        String name = getIntent().getStringExtra("GYM_NAME");
        String imageStr = getIntent().getStringExtra("GYM_IMAGE");
        double lat = getIntent().getDoubleExtra("GYM_LAT", 0.0);
        double lng = getIntent().getDoubleExtra("GYM_LNG", 0.0);

        tvName.setText(name);

        if (imageStr != null && !imageStr.isEmpty()) {
            byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imgGym.setImageBitmap(decodedByte);
        }

        loadReviews(name);

        btnNavigate.setOnClickListener(v -> {
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(this, "Invalid Coordinates for this Gym", Toast.LENGTH_SHORT).show();
                return;
            }
            String uriString = String.format(Locale.US, "google.navigation:q=%f,%f", lat, lng);
            Uri gmmIntentUri = Uri.parse(uriString);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
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

                                    double totalScore = 0;
                                    int count = 0;

                                    for (QueryDocumentSnapshot doc : reviewsSnapshot) {
                                        // --- CRITICAL FIX: SAVE IDs FOR DELETION ---
                                        Map<String, Object> reviewData = doc.getData();
                                        reviewData.put("reviewId", doc.getId()); // Save the Review ID
                                        reviewData.put("gymId", gymId);          // Save the Gym ID

                                        reviewList.add(reviewData);
                                        // -------------------------------------------

                                        if (doc.contains("rating")) {
                                            Double rating = doc.getDouble("rating");
                                            if (rating != null) {
                                                totalScore += rating;
                                                count++;
                                            }
                                        }
                                    }

                                    if (count > 0) {
                                        double average = totalScore / count;
                                        rbAvg.setRating((float) average);
                                        tvAvgInfo.setText(String.format(Locale.US, "%.1f (%d reviews)", average, count));
                                    } else {
                                        rbAvg.setRating(0);
                                        tvAvgInfo.setText("No reviews yet");
                                    }

                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }
}