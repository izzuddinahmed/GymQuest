package com.example.gymquest;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
// Toast is no longer needed for the Users button, but keeping it is fine if you use it elsewhere
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        Button btnGyms = findViewById(R.id.btnManageGyms);
        Button btnUsers = findViewById(R.id.btnManageUsers);
        Button btnLogout = findViewById(R.id.btnLogoutAdmin);

        // 1. Manage Gyms Logic
        btnGyms.setOnClickListener(v -> {
            // Sends Admin to the Gym List where they can now Long-Press to delete
            Intent intent = new Intent(AdminDashboardActivity.this, GymListActivity.class);
            startActivity(intent);
        });

        // 2. Manage Users Logic (UPDATED)
        btnUsers.setOnClickListener(v -> {
            // Open the new User List page
            Intent intent = new Intent(AdminDashboardActivity.this, UserListActivity.class);
            startActivity(intent);
        });

        // 3. Logout Logic
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            // Clear the "back stack" so they can't press back to return to the admin dashboard
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}