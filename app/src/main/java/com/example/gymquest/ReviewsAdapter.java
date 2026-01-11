package com.example.gymquest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

        String author = (String) review.get("author");
        String comment = (String) review.get("comment");
        // Firebase stores numbers as Double, we need float for RatingBar
        Double rating = (Double) review.get("rating");

        holder.tvAuthor.setText(author);
        holder.tvComment.setText(comment);
        if (rating != null) {
            holder.rbRating.setRating(rating.floatValue());
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvComment;
        RatingBar rbRating;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvReviewAuthor);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            rbRating = itemView.findViewById(R.id.rbReviewRating);
        }
    }
}