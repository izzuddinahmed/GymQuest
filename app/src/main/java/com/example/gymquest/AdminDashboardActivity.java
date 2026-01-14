package com.example.gymquest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Bind Buttons
        Button btnGyms = findViewById(R.id.btnManageGyms);
        Button btnUsers = findViewById(R.id.btnManageUsers);
        Button btnMap = findViewById(R.id.btnGoToMap);       // --- NEW
        Button btnProfile = findViewById(R.id.btnAdminProfile); // --- NEW
        Button btnLogout = findViewById(R.id.btnLogoutAdmin);

        // 1. Manage Gyms
        btnGyms.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, GymListActivity.class);
            startActivity(intent);
        });

        // 2. Manage Users
        btnUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, UserListActivity.class);
            startActivity(intent);
        });

        // 3. Go to Map (Main Activity) --- NEW LOGIC
        btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // 4. Go to Profile --- NEW LOGIC
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // 5. Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            // Clear Admin Key (Important security step!)
            getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                    .edit().clear().apply();

            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}