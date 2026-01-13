package com.example.gymquest;

import android.app.AlertDialog; // --- NEW IMPORT
import android.content.Context; // --- NEW IMPORT
import android.content.SharedPreferences; // --- NEW IMPORT
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // --- NEW IMPORT
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast; // --- NEW IMPORT

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore; // --- NEW IMPORT

import java.util.List;
import java.util.Map;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private List<Map<String, Object>> reviewList;

    public ReviewsAdapter(List<Map<String, Object>> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Map<String, Object> review = reviewList.get(position);
        Context context = holder.itemView.getContext(); // Get context from the view

        String author = (String) review.get("author");
        String comment = (String) review.get("comment");
        Double rating = (Double) review.get("rating");

        holder.tvAuthor.setText(author);
        holder.tvComment.setText(comment);
        if (rating != null) {
            holder.rbRating.setRating(rating.floatValue());
        }

        // --- NEW CODE: ADMIN DELETE BUTTON LOGIC ---

        // 1. Check if user is Admin
        SharedPreferences prefs = context.getSharedPreferences("GymAppPrefs", Context.MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("isAdmin", false);

        if (isAdmin) {
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnDelete.setOnClickListener(v -> {
                // Confirm before deleting
                new AlertDialog.Builder(context)
                        .setTitle("Delete Review")
                        .setMessage("Are you sure you want to delete this review?")
                        .setPositiveButton("Delete", (dialog, which) -> {

                            // We need IDs to delete. Safely check if they exist.
                            String gymId = (String) review.get("gymId");
                            String reviewId = (String) review.get("reviewId");

                            if (gymId != null && reviewId != null) {
                                FirebaseFirestore.getInstance()
                                        .collection("Gyms").document(gymId)
                                        .collection("Reviews").document(reviewId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(context, "Review Deleted", Toast.LENGTH_SHORT).show();
                                            reviewList.remove(position); // Remove from local list
                                            notifyItemRemoved(position);
                                            notifyItemRangeChanged(position, reviewList.size());
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show()
                                        );
                            } else {
                                Toast.makeText(context, "Error: Missing ID. Cannot delete.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            // Normal user: Hide the button
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvComment;
        RatingBar rbRating;
        ImageView btnDelete; // --- NEW VARIABLE

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvReviewAuthor);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewRating);
            // Bind the delete button from the XML
            btnDelete = itemView.findViewById(R.id.btnDeleteReview);
        }
    }
}