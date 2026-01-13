package com.example.gymquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail, tvName;
    private Button btnLogout, btnEditProfile, btnChangePassword;
    private Button btnAdmin;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Bind Views
        tvName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnAdmin = findViewById(R.id.btnAdminDashboard);

        // Get Current User
        user = FirebaseAuth.getInstance().getCurrentUser();

        // 1. Load Data on Start
        if (user != null) {
            tvEmail.setText(user.getEmail());
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                tvName.setText(user.getDisplayName());
            } else {
                tvName.setText("No Name Set");
            }
        }

        // --- ADMIN DASHBOARD LOGIC ---
        SharedPreferences prefs = getSharedPreferences("GymAppPrefs", Context.MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("isAdmin", false);

        if (isAdmin) {
            btnAdmin.setVisibility(View.VISIBLE);
            btnAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
            });
        } else {
            btnAdmin.setVisibility(View.GONE);
        }
        // ----------------------------------

        // 2. Button: Logout (UPDATED WITH FIX)
        btnLogout.setOnClickListener(v -> {
            // 1. Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. --- NEW FIX: WIPE THE ADMIN MEMORY ---
            // This ensures the next user doesn't inherit admin rights by accident
            getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                    .edit()
                    .clear() // This deletes all saved data (like isAdmin)
                    .apply();
            // -----------------------------------------

            // 3. Go to Login
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // 3. Button: Edit Profile (Opens Name Dialog)
        btnEditProfile.setOnClickListener(v -> showEditNameDialog());

        // 4. Button: Change Password (Opens Password Dialog)
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    // --- HELPER 1: Show Popup to Type Name ---
    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter your new name");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateFirebaseProfile(newName);
            } else {
                Toast.makeText(ProfileActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateFirebaseProfile(String newName) {
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tvName.setText(newName);
                            Toast.makeText(ProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Error updating profile.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // --- HELPER 2: Show Popup to Change Password ---
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etOldPass = new EditText(this);
        etOldPass.setHint("Current Password");
        etOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etOldPass);

        final EditText etNewPass = new EditText(this);
        etNewPass.setHint("New Password");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(ProfileActivity.this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            performPasswordUpdate(oldPass, newPass);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performPasswordUpdate(String oldPass, String newPass) {
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Password Changed Successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "Update Failed: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(ProfileActivity.this, "Incorrect Current Password", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}