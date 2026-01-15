package com.example.gymquest;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2; // --- NEW IMPORT

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GymDetailActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ViewPager2 viewPagerGallery; // --- CHANGED to ViewPager2
    private ReviewsAdapter reviewsAdapter;
    private ImageSliderAdapter sliderAdapter; // --- NEW ADAPTER

    private List<Map<String, Object>> reviewList;
    private List<Map<String, Object>> fullGalleryList; // Holds Main + User photos

    private RatingBar rbAvg;
    private TextView tvAvgInfo;
    private TextView tvDescription;

    private Button btnContact, btnEditPhone, btnEditDesc, btnAddPhoto;

    private String gymPhoneNumber = "";
    private String gymDescription = "";
    private String currentGymId = "";
    private Bitmap mainGymBitmap = null; // To store the original image
    private boolean isAdmin = false;

    private static final int REQUEST_IMAGE_CAPTURE = 200;
    private static final int PERMISSION_REQUEST_CAMERA = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_detail);

        // Bind Views
        TextView tvName = findViewById(R.id.tvDetailName);
        viewPagerGallery = findViewById(R.id.viewPagerGallery); // --- NEW BIND

        btnContact = findViewById(R.id.btnContactUs);
        btnEditPhone = findViewById(R.id.btnEditPhone);
        btnEditDesc = findViewById(R.id.btnEditDesc);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);

        tvDescription = findViewById(R.id.tvDescription);
        rbAvg = findViewById(R.id.rbAverageRating);
        tvAvgInfo = findViewById(R.id.tvAverageInfo);
        Button btnNavigate = findViewById(R.id.btnNavigate);
        Button btnReview = findViewById(R.id.btnWriteReview);
        Button btnClose = findViewById(R.id.btnClose);

        // Setup Reviews
        rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        reviewsAdapter = new ReviewsAdapter(reviewList);
        rvReviews.setAdapter(reviewsAdapter);

        // Setup Gallery List
        fullGalleryList = new ArrayList<>();

        // Check Admin
        SharedPreferences prefs = getSharedPreferences("GymAppPrefs", MODE_PRIVATE);
        isAdmin = prefs.getBoolean("isAdmin", false);

        // Setup Slider Adapter
        sliderAdapter = new ImageSliderAdapter(this, fullGalleryList, isAdmin, this::deleteUserPhoto);
        viewPagerGallery.setAdapter(sliderAdapter);

        if (isAdmin) {
            btnEditPhone.setVisibility(View.VISIBLE);
            btnEditDesc.setVisibility(View.VISIBLE);
        }

        // Get Intent Data
        String name = getIntent().getStringExtra("GYM_NAME");
        String imageStr = getIntent().getStringExtra("GYM_IMAGE");
        double lat = getIntent().getDoubleExtra("GYM_LAT", 0.0);
        double lng = getIntent().getDoubleExtra("GYM_LNG", 0.0);

        tvName.setText(name);

        // Process Main Image
        Map<String, Object> mainImageMap = new HashMap<>();
        mainImageMap.put("type", "MAIN");
        if (imageStr != null && !imageStr.isEmpty()) {
            byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
            mainGymBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            mainImageMap.put("bitmap", mainGymBitmap);
        } else {
            // Placeholder if no image
            mainImageMap.put("bitmap", BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background));
        }
        fullGalleryList.add(mainImageMap); // Always add main image first
        sliderAdapter.notifyDataSetChanged();

        loadGymData(name);

        // Listeners
        btnAddPhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnEditPhone.setOnClickListener(v -> showEditPhoneDialog());
        btnEditDesc.setOnClickListener(v -> showEditDescriptionDialog());

        // ... (Keep your existing Navigation, Contact, Review, Close listeners here) ...
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

    private void loadGymData(String gymName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Gyms").whereEqualTo("name", gymName).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot gymDoc = queryDocumentSnapshots.getDocuments().get(0);
                        currentGymId = gymDoc.getId();

                        if (gymDoc.contains("phone")) {
                            gymPhoneNumber = gymDoc.getString("phone");
                            btnContact.setVisibility(View.VISIBLE);
                        }
                        if (gymDoc.contains("description")) {
                            gymDescription = gymDoc.getString("description");
                            tvDescription.setText(gymDescription);
                        }

                        // LOAD USER PHOTOS
                        loadUserPhotos(currentGymId);

                        // LOAD REVIEWS (Your existing code)
                        loadReviews(currentGymId);
                    }
                });
    }

    private void loadUserPhotos(String gymId) {
        FirebaseFirestore.getInstance().collection("Gyms").document(gymId).collection("UserPhotos")
                .get()
                .addOnSuccessListener(snapshots -> {
                    // 1. Keep the first item (Main Image)
                    Map<String, Object> mainImage = fullGalleryList.get(0);
                    fullGalleryList.clear();
                    fullGalleryList.add(mainImage);

                    // 2. Add User Photos
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        data.put("type", "USER"); // Mark as user photo
                        fullGalleryList.add(data);
                    }
                    sliderAdapter.notifyDataSetChanged();
                });
    }

    // --- DELETE LOGIC ---
    private void deleteUserPhoto(String photoId, int position) {
        if (currentGymId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("Gyms").document(currentGymId).collection("UserPhotos").document(photoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                    // Remove from list and update UI immediately
                    fullGalleryList.remove(position);
                    sliderAdapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting", Toast.LENGTH_SHORT).show());
    }

    // --- CAMERA & UPLOAD LOGIC ---
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try { startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE); }
        catch (Exception e) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            uploadPhotoToFirebase(capturedImage);
        }
    }

    private void uploadPhotoToFirebase(Bitmap bitmap) {
        if (currentGymId.isEmpty()) return;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String imageStr = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        Map<String, Object> photoData = new HashMap<>();
        photoData.put("imageStr", imageStr);
        photoData.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("Gyms").document(currentGymId).collection("UserPhotos")
                .add(photoData)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Photo added to gallery!", Toast.LENGTH_SHORT).show();
                    loadUserPhotos(currentGymId); // Refresh slider
                    // Auto-scroll to the new photo (last item)
                    viewPagerGallery.setCurrentItem(fullGalleryList.size());
                });
    }

    // --- Helper for Reviews (Kept brief) ---
    private void loadReviews(String gymId) {
        FirebaseFirestore.getInstance().collection("Gyms").document(gymId).collection("Reviews").get()
                .addOnSuccessListener(snapshots -> {
                    reviewList.clear();
                    double total = 0; int count = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        reviewList.add(data);
                        if(doc.contains("rating")) { total += doc.getDouble("rating"); count++; }
                    }
                    if(count>0) {
                        rbAvg.setRating((float)(total/count));
                        tvAvgInfo.setText(String.format(Locale.US, "%.1f (%d reviews)", total/count, count));
                    }
                    reviewsAdapter.notifyDataSetChanged();
                });
    }

    // --- Helper for Dialogs (Kept same as before) ---
    private void showEditDescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Description");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(gymDescription);
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            updateFieldInDatabase("description", input.getText().toString().trim());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Phone");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setText(gymPhoneNumber);
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            updateFieldInDatabase("phone", input.getText().toString().trim());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateFieldInDatabase(String field, String value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);
        FirebaseFirestore.getInstance().collection("Gyms").document(currentGymId).update(updates)
                .addOnSuccessListener(a -> {
                    if(field.equals("description")) tvDescription.setText(value);
                    if(field.equals("phone")) gymPhoneNumber = value;
                });
    }
}