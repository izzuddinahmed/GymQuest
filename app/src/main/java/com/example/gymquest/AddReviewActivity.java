package com.example.gymquest;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddReviewActivity extends AppCompatActivity {

    private TextView tvGymName;
    private RatingBar ratingBar;
    private EditText etComment;
    private Button btnSubmit;
    private String gymName; // Passed from the previous screen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_review);

        // 1. Get the Gym Name from the Intent
        gymName = getIntent().getStringExtra("GYM_NAME");

        // 2. Bind Views
        tvGymName = findViewById(R.id.tvReviewGymName);
        ratingBar = findViewById(R.id.rbGymRating);
        etComment = findViewById(R.id.etReviewComment);
        btnSubmit = findViewById(R.id.btnSubmitReview);

        if (gymName != null) {
            tvGymName.setText("Rate " + gymName);
        }

        // 3. Submit Logic
        btnSubmit.setOnClickListener(v -> submitReview());
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        if (rating == 0) {
            Toast.makeText(this, "Please select a star rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare Review Data
        Map<String, Object> review = new HashMap<>();
        review.put("author", userEmail);
        review.put("rating", rating);
        review.put("comment", comment);
        review.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 4. FIND the gym by name, THEN add the review
        db.collection("Gyms")
                .whereEqualTo("name", gymName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the ID of the gym document
                        String gymId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Add review to a sub-collection named "Reviews"
                        db.collection("Gyms").document(gymId).collection("Reviews")
                                .add(review)
                                .addOnSuccessListener(doc -> {
                                    Toast.makeText(AddReviewActivity.this, "Review Submitted!", Toast.LENGTH_SHORT).show();
                                    finish(); // Close screen
                                });
                    } else {
                        Toast.makeText(AddReviewActivity.this, "Error: Gym not found in DB", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error finding gym", Toast.LENGTH_SHORT).show());
    }
}