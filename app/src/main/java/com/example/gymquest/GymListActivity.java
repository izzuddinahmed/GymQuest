package com.example.gymquest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull; // --- NEW IMPORT
import androidx.appcompat.app.AlertDialog; // --- NEW IMPORT
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper; // --- NEW IMPORT
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GymListActivity extends AppCompatActivity {

    private RecyclerView rvGyms;
    private GymListAdapter adapter;
    private List<Map<String, Object>> fullGymList;
    private List<Map<String, Object>> displayList;
    private boolean isFilterActive = false;
    private Spinner spinnerSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_list);

        rvGyms = findViewById(R.id.rvGymList);
        rvGyms.setLayoutManager(new LinearLayoutManager(this));

        fullGymList = new ArrayList<>();
        displayList = new ArrayList<>();
        adapter = new GymListAdapter(this, displayList);
        rvGyms.setAdapter(adapter);

        Button btnFavOnly = findViewById(R.id.btnShowFavorites);
        spinnerSort = findViewById(R.id.spinnerSort);

        // --- 1. SETUP SPINNER (Dropdown) ---
        String[] sortOptions = {"Sort: Name (A-Z)", "Sort: Best Rated", "Sort: Worst Rated"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(spinnerAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySorting(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- 2. SETUP SWIPE TO DELETE (NEW FEATURE) ---
        setupSwipeToDelete();
        // ----------------------------------------------

        loadGyms();

        // --- 3. FAVORITES TOGGLE LOGIC ---
        btnFavOnly.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("GymFavorites", Context.MODE_PRIVATE);

            if (!isFilterActive) {
                isFilterActive = true;
                btnFavOnly.setText("Show All Gyms");

                displayList.clear();
                for (Map<String, Object> gym : fullGymList) {
                    String name = (String) gym.get("name");
                    if (prefs.getBoolean(name, false)) {
                        displayList.add(gym);
                    }
                }
                if (displayList.isEmpty()) {
                    Toast.makeText(GymListActivity.this, "No favorites yet!", Toast.LENGTH_SHORT).show();
                }
            } else {
                isFilterActive = false;
                btnFavOnly.setText("Favorites Only");
                displayList.clear();
                displayList.addAll(fullGymList);
            }

            applySorting(spinnerSort.getSelectedItemPosition());
            adapter.notifyDataSetChanged();
        });
    }

    private void loadGyms() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Gyms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullGymList.clear();
                    displayList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> gymData = doc.getData();

                        // --- CRITICAL FIX: SAVE THE ID ---
                        gymData.put("id", doc.getId());
                        // --------------------------------

                        // Default rating
                        gymData.put("averageRating", 0.0);

                        // --- FETCH REVIEWS FOR THIS GYM ---
                        String gymId = doc.getId();
                        db.collection("Gyms").document(gymId).collection("Reviews")
                                .get()
                                .addOnSuccessListener(reviews -> {
                                    double total = 0;
                                    int count = 0;
                                    for (QueryDocumentSnapshot review : reviews) {
                                        if (review.contains("rating")) {
                                            Double r = review.getDouble("rating");
                                            if (r != null) {
                                                total += r;
                                                count++;
                                            }
                                        }
                                    }
                                    double avg = (count > 0) ? (total / count) : 0.0;
                                    gymData.put("averageRating", avg);

                                    applySorting(spinnerSort.getSelectedItemPosition());
                                });

                        fullGymList.add(gymData);
                    }

                    displayList.addAll(fullGymList);
                    adapter.notifyDataSetChanged();
                });
    }

    // --- NEW METHOD: HANDLES SWIPE TO DELETE ---
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't want drag-and-drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();

                // 1. Check if user is Admin
                SharedPreferences prefs = getSharedPreferences("GymAppPrefs", MODE_PRIVATE);
                boolean isAdmin = prefs.getBoolean("isAdmin", false);

                if (!isAdmin) {
                    // Not an admin? Cancel swipe and warn them
                    adapter.notifyItemChanged(position);
                    Toast.makeText(GymListActivity.this, "Only Admins can delete gyms!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 2. Get Gym Info
                Map<String, Object> gym = displayList.get(position);
                String gymId = (String) gym.get("id");
                String gymName = (String) gym.get("name");

                // 3. Show Confirmation Dialog
                new AlertDialog.Builder(GymListActivity.this)
                        .setTitle("Delete Gym?")
                        .setMessage("Are you sure you want to delete " + gymName + "? This cannot be undone.")
                        .setPositiveButton("DELETE", (dialog, which) -> {
                            deleteGymFromFirebase(gymId);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            adapter.notifyItemChanged(position); // Put item back
                        })
                        .setCancelable(false)
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(rvGyms);
    }

    private void deleteGymFromFirebase(String gymId) {
        FirebaseFirestore.getInstance().collection("Gyms").document(gymId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GymListActivity.this, "Gym Deleted Successfully", Toast.LENGTH_SHORT).show();
                    loadGyms(); // Refresh the list
                })
                .addOnFailureListener(e -> Toast.makeText(GymListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // --- 3. SORTING LOGIC ---
    private void applySorting(int position) {
        switch (position) {
            case 0: // Name (A-Z)
                Collections.sort(displayList, (o1, o2) -> {
                    String n1 = (String) o1.get("name");
                    String n2 = (String) o2.get("name");
                    return n1.compareToIgnoreCase(n2);
                });
                break;

            case 1: // Best Rated (High -> Low)
                Collections.sort(displayList, (o1, o2) -> {
                    Double r1 = (Double) o1.get("averageRating");
                    Double r2 = (Double) o2.get("averageRating");
                    return Double.compare(r2 != null ? r2 : 0.0, r1 != null ? r1 : 0.0);
                });
                break;

            case 2: // Worst Rated (Low -> High)
                Collections.sort(displayList, (o1, o2) -> {
                    Double r1 = (Double) o1.get("averageRating");
                    Double r2 = (Double) o2.get("averageRating");
                    return Double.compare(r1 != null ? r1 : 0.0, r2 != null ? r2 : 0.0);
                });
                break;
        }
        adapter.notifyDataSetChanged();
    }
}