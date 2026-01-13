package com.example.gymquest;

import android.app.AlertDialog; // --- NEW IMPORT
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // --- NEW IMPORT

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore; // --- NEW IMPORT

import java.util.List;
import java.util.Map;

public class GymListAdapter extends RecyclerView.Adapter<GymListAdapter.GymViewHolder> {

    private List<Map<String, Object>> gymList;
    private Context context;
    private SharedPreferences prefs;

    // Constructor
    public GymListAdapter(Context context, List<Map<String, Object>> gymList) {
        this.context = context;
        this.gymList = gymList;
        // Setup local storage for Favorites
        prefs = context.getSharedPreferences("GymFavorites", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public GymViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gym_list, parent, false);
        return new GymViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GymViewHolder holder, int position) {
        Map<String, Object> gym = gymList.get(position);
        String name = (String) gym.get("name");

        // Retrieve data safely
        Double lat = (Double) gym.get("lat");
        Double lng = (Double) gym.get("lng");
        String imageStr = (String) gym.get("imageStr");

        holder.tvName.setText(name);

        // --- FAVORITE LOGIC ---
        boolean isFav = prefs.getBoolean(name, false); // Check if saved
        updateHeartIcon(holder.btnFav, isFav);

        // Handle Heart Click
        holder.btnFav.setOnClickListener(v -> {
            boolean currentState = prefs.getBoolean(name, false);
            boolean newState = !currentState;

            // Save new state
            prefs.edit().putBoolean(name, newState).apply();
            updateHeartIcon(holder.btnFav, newState);
        });

        // Handle Row Click (Go to Details)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GymDetailActivity.class);
            intent.putExtra("GYM_NAME", name);
            intent.putExtra("GYM_IMAGE", imageStr);
            if(lat != null) intent.putExtra("GYM_LAT", lat);
            if(lng != null) intent.putExtra("GYM_LNG", lng);
            context.startActivity(intent);
        });

        // --- NEW CODE: ADMIN LONG PRESS TO DELETE ---
        // 1. Check if the current user is an Admin (using the key we saved in LoginActivity)
        boolean isAdmin = context.getSharedPreferences("GymAppPrefs", Context.MODE_PRIVATE)
                .getBoolean("isAdmin", false);

        if (isAdmin) {
            holder.itemView.setOnLongClickListener(v -> {
                // Show Confirmation Dialog
                new AlertDialog.Builder(context)
                        .setTitle("Delete Gym")
                        .setMessage("Are you sure you want to delete " + name + "? This cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {

                            // Important: We need the Document ID to delete it.
                            // If your gym map doesn't have an "id" field yet, this might crash.
                            // Ensure your loadGyms() function saves document.getId()!
                            Object gymIdObj = gym.get("id"); // Using generic "id" key, make sure you save it in GymListActivity

                            if (gymIdObj != null) {
                                FirebaseFirestore.getInstance()
                                        .collection("Gyms").document(gymIdObj.toString())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(context, "Gym Deleted", Toast.LENGTH_SHORT).show();
                                            gymList.remove(position); // Remove from local list
                                            notifyItemRemoved(position); // Animate removal
                                            notifyItemRangeChanged(position, gymList.size());
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(context, "Error deleting gym", Toast.LENGTH_SHORT).show()
                                        );
                            } else {
                                // Fallback: try to find by name if ID is missing (Not recommended but safer)
                                deleteGymByName(name, position);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true; // Return true to indicate the long click was handled
            });
        }
    }

    // Helper: If we don't have the ID handy, find the gym by name and delete it
    private void deleteGymByName(String gymName, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Gyms").whereEqualTo("name", gymName).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String docId = snapshots.getDocuments().get(0).getId();
                        db.collection("Gyms").document(docId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Gym Deleted", Toast.LENGTH_SHORT).show();
                                    gymList.remove(position);
                                    notifyItemRemoved(position);
                                });
                    } else {
                        Toast.makeText(context, "Could not find gym to delete", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update Icon using your custom PNGs
    private void updateHeartIcon(ImageView icon, boolean isFav) {
        if (isFav) {
            icon.setImageResource(R.drawable.star);      // Filled Yellow Star
        } else {
            icon.setImageResource(R.drawable.favorite);  // Hollow Orange Star
        }
    }

    @Override
    public int getItemCount() {
        return gymList.size();
    }

    // ViewHolder Class
    public static class GymViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSub;
        ImageView btnFav;

        public GymViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGymName);
            tvSub = itemView.findViewById(R.id.tvGymSub);
            btnFav = itemView.findViewById(R.id.btnFavorite);
        }
    }
}