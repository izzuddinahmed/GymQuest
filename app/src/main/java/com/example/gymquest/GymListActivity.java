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

import androidx.appcompat.app.AppCompatActivity;
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
        // Create options list
        String[] sortOptions = {"Sort: Name (A-Z)", "Sort: Best Rated", "Sort: Worst Rated"};

        // Connect options to the spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(spinnerAdapter);

        // Handle Dropdown Selection
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySorting(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadGyms();

        // --- 2. FAVORITES TOGGLE LOGIC ---
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

            // Re-apply whatever sort is currently selected
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
                        // We save the document ID so the Adapter knows which gym to delete
                        gymData.put("id", doc.getId());
                        // --------------------------------

                        // Default rating is 0.0 until we fetch reviews
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
                                    // Calculate and Store Average
                                    double avg = (count > 0) ? (total / count) : 0.0;
                                    gymData.put("averageRating", avg);

                                    // Refresh list to show new sort order if needed
                                    applySorting(spinnerSort.getSelectedItemPosition());
                                });

                        fullGymList.add(gymData);
                    }

                    displayList.addAll(fullGymList);
                    adapter.notifyDataSetChanged();
                });
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
                    // Use compare(r2, r1) for Descending order
                    return Double.compare(r2 != null ? r2 : 0.0, r1 != null ? r1 : 0.0);
                });
                break;

            case 2: // Worst Rated (Low -> High)
                Collections.sort(displayList, (o1, o2) -> {
                    Double r1 = (Double) o1.get("averageRating");
                    Double r2 = (Double) o2.get("averageRating");
                    // Use compare(r1, r2) for Ascending order
                    return Double.compare(r1 != null ? r1 : 0.0, r2 != null ? r2 : 0.0);
                });
                break;
        }
        adapter.notifyDataSetChanged();
    }
}