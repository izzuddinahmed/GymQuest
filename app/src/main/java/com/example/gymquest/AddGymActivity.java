package com.example.gymquest;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddGymActivity extends AppCompatActivity {

    private EditText etGymName;
    private EditText etGymPhone;
    private EditText etGymDesc; // --- NEW VARIABLE FOR DESCRIPTION
    private TextView tvLocationStatus;
    private Button btnTakePhoto, btnSaveGym, btnOpenMap;
    private ImageView imgPreview;

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_MAP_PICK = 102;
    private static final int PERMISSION_REQUEST_CAMERA = 103;

    private Bitmap capturedImage;
    private double currentLat = 0.0;
    private double currentLng = 0.0;
    private boolean locationSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_gym);

        // Permission Check (Notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        createNotificationChannel();

        // Bind Views
        etGymName = findViewById(R.id.etGymName);
        etGymPhone = findViewById(R.id.etGymPhone);
        etGymDesc = findViewById(R.id.etGymDesc); // --- NEW BINDING
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imgPreview = findViewById(R.id.imgPreview);
        btnSaveGym = findViewById(R.id.btnSaveGym);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        // 1. Open Map
        btnOpenMap.setOnClickListener(v -> {
            Intent intent = new Intent(AddGymActivity.this, PickLocationActivity.class);
            startActivityForResult(intent, REQUEST_MAP_PICK);
        });

        // 2. Open Camera
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());

        // 3. Save
        btnSaveGym.setOnClickListener(v -> saveGymToDatabase());
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    capturedImage = (Bitmap) extras.get("data");
                    imgPreview.setImageBitmap(capturedImage);
                }
            } else if (requestCode == REQUEST_MAP_PICK) {
                currentLat = data.getDoubleExtra("LAT", 0);
                currentLng = data.getDoubleExtra("LNG", 0);
                locationSelected = true;

                String locationText = String.format("📍 Selected: %.4f, %.4f", currentLat, currentLng);
                tvLocationStatus.setText(locationText);
                tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private void saveGymToDatabase() {
        String name = etGymName.getText().toString().trim();
        String phone = etGymPhone.getText().toString().trim();
        String desc = etGymDesc.getText().toString().trim(); // --- NEW: GET DESCRIPTION

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a Gym Name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!locationSelected) {
            Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        // Note: Phone and Description can be optional, so we don't force check them.

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> gym = new HashMap<>();
        gym.put("name", name);
        gym.put("phone", phone);
        gym.put("description", desc); // --- NEW: SAVE DESCRIPTION TO DB
        gym.put("lat", currentLat);
        gym.put("lng", currentLng);

        if (capturedImage != null) {
            String imageString = bitmapToString(capturedImage);
            gym.put("imageStr", imageString);
        } else {
            gym.put("imageStr", "");
        }

        db.collection("Gyms")
                .add(gym)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddGymActivity.this, "Gym Added!", Toast.LENGTH_SHORT).show();
                    showSuccessNotification();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AddGymActivity.this, "Error adding gym", Toast.LENGTH_SHORT).show());
    }

    private String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "GymQuestChannel";
            String description = "Channel for GymQuest notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("GYM_CHANNEL_ID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showSuccessNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "GYM_CHANNEL_ID")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Success!")
                .setContentText("Your new gym has been added to the map.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }
}