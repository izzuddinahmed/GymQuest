package com.example.gymquest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GymDetailActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewsAdapter adapter;
    private List<Map<String, Object>> reviewList;

    private RatingBar rbAvg;
    private TextView tvAvgInfo;
    private TextView tvDescription; // --- NEW

    private Button btnContact;
    private Button btnEditPhone;
    private Button btnEditDesc; // --- NEW BUTTON

    private String gymPhoneNumber = "";
    private String gymDescription = ""; // --- NEW VARIABLE
    private String currentGymId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_detail);

        TextView tvName = findViewById(R.id.tvDetailName);
        ImageView imgGym = findViewById(R.id.imgDetail);
        Button btnClose = findViewById(R.id.btnClose);
        Button btnReview = findViewById(R.id.btnWriteReview);
        Button btnNavigate = findViewById(R.id.btnNavigate);

        btnContact = findViewById(R.id.btnContactUs);
        btnEditPhone = findViewById(R.id.btnEditPhone);
        btnEditDesc = findViewById(R.id.btnEditDesc); // --- BIND NEW BUTTON

        tvDescription = findViewById(R.id.tvDescription); // --- BIND NEW TEXTVIEW

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

        // --- CHECK ADMIN STATUS ---
        SharedPreferences prefs = getSharedPreferences("GymAppPrefs", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("isAdmin", false);

        if (isAdmin) {
            btnEditPhone.setVisibility(View.VISIBLE);
            btnEditDesc.setVisibility(View.VISIBLE); // --- SHOW BUTTON FOR ADMIN
        } else {
            btnEditPhone.setVisibility(View.GONE);
            btnEditDesc.setVisibility(View.GONE);
        }

        loadReviews(name);

        // --- BUTTON LISTENERS ---
        btnEditPhone.setOnClickListener(v -> showEditPhoneDialog());
        btnEditDesc.setOnClickListener(v -> showEditDescriptionDialog()); // --- NEW LISTENER

        btnContact.setOnClickListener(v -> {
            if (gymPhoneNumber != null && !gymPhoneNumber.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + gymPhoneNumber));
                startActivity(intent);
            } else {
                Toast.makeText(GymDetailActivity.this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnNavigate.setOnClickListener(v -> {
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(this, "Invalid Coordinates", Toast.LENGTH_SHORT).show();
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

    // --- NEW: EDIT DESCRIPTION DIALOG ---
    private void showEditDescriptionDialog() {
        if (currentGymId == null || currentGymId.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Description");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setLines(4);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        input.setText(gymDescription); // Pre-fill current text
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDesc = input.getText().toString().trim();
            updateDescriptionInDatabase(newDesc);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateDescriptionInDatabase(String newDesc) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", newDesc);

        db.collection("Gyms").document(currentGymId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GymDetailActivity.this, "Description updated!", Toast.LENGTH_SHORT).show();
                    gymDescription = newDesc;
                    tvDescription.setText(newDesc.isEmpty() ? "No description available." : newDesc);
                })
                .addOnFailureListener(e -> Toast.makeText(GymDetailActivity.this, "Error updating description", Toast.LENGTH_SHORT).show());
    }

    // --- EXISTING: EDIT PHONE DIALOG ---
    private void showEditPhoneDialog() {
        if (currentGymId == null || currentGymId.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Phone Number");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setText(gymPhoneNumber);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPhone = input.getText().toString().trim();
            updatePhoneInDatabase(newPhone);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updatePhoneInDatabase(String newPhone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", newPhone);

        db.collection("Gyms").document(currentGymId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GymDetailActivity.this, "Phone number updated!", Toast.LENGTH_SHORT).show();
                    gymPhoneNumber = newPhone;
                    if (!newPhone.isEmpty()) btnContact.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> Toast.makeText(GymDetailActivity.this, "Error updating phone", Toast.LENGTH_SHORT).show());
    }

    private void loadReviews(String gymName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Gyms")
                .whereEqualTo("name", gymName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot gymDoc = queryDocumentSnapshots.getDocuments().get(0);
                        currentGymId = gymDoc.getId();

                        // Load Phone
                        if (gymDoc.contains("phone")) {
                            gymPhoneNumber = gymDoc.getString("phone");
                            btnContact.setVisibility(View.VISIBLE);
                        } else {
                            gymPhoneNumber = "";
                            btnContact.setVisibility(View.GONE);
                        }

                        // --- NEW: Load Description ---
                        if (gymDoc.contains("description")) {
                            gymDescription = gymDoc.getString("description");
                            tvDescription.setText(gymDescription.isEmpty() ? "No description available." : gymDescription);
                        } else {
                            gymDescription = "";
                            tvDescription.setText("No description available.");
                        }
                        // -----------------------------

                        // Load Reviews
                        db.collection("Gyms").document(currentGymId).collection("Reviews")
                                .get()
                                .addOnSuccessListener(reviewsSnapshot -> {
                                    reviewList.clear();
                                    double totalScore = 0;
                                    int count = 0;

                                    for (QueryDocumentSnapshot doc : reviewsSnapshot) {
                                        Map<String, Object> reviewData = doc.getData();
                                        reviewData.put("reviewId", doc.getId());
                                        reviewData.put("gymId", currentGymId);
                                        reviewList.add(reviewData);

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