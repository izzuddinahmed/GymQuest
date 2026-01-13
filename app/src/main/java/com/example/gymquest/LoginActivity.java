package com.example.gymquest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Imports for Map are no longer strictly needed if we aren't creating data,
// but keeping them doesn't hurt.
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginBtn, goToSignupBtn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.emailLogin);
        passwordField = findViewById(R.id.passwordLogin);
        loginBtn = findViewById(R.id.btnGoToLogin);
        goToSignupBtn = findViewById(R.id.btnGoToSignup);

        // 1. Login Logic
        loginBtn.setOnClickListener(v -> loginUser());

        // 2. Link to Sign Up Screen
        goToSignupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            return;
        }

        // Firebase logic to sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // Look for the user's ID in the "Users" collection
                        db.collection("Users").document(user.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {

                                    // --- NEW LOGIC: BLOCK DELETED USERS ---
                                    // If the user exists in Auth but NOT in Database (Admin deleted them),
                                    // we delete the Auth account so they must register again.
                                    if (!documentSnapshot.exists()) {
                                        user.delete()
                                                .addOnCompleteListener(delTask -> {
                                                    Toast.makeText(LoginActivity.this, "Account deleted by Admin. Please register again.", Toast.LENGTH_LONG).show();

                                                    // Clear any saved admin data just in case
                                                    getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                                                            .edit().clear().apply();
                                                });
                                        return; // STOP HERE. Do not proceed to login.
                                    }
                                    // --------------------------------------

                                    boolean isAdmin = false;

                                    // Check if the "isAdmin" field exists and is true
                                    if (documentSnapshot.contains("isAdmin")) {
                                        Boolean adminFlag = documentSnapshot.getBoolean("isAdmin");
                                        if (adminFlag != null && adminFlag) {
                                            isAdmin = true;
                                        }
                                    }

                                    if (isAdmin) {
                                        // USER IS ADMIN -> SAVE "TRUE" KEY
                                        getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                                                .edit().putBoolean("isAdmin", true).apply();

                                        Toast.makeText(LoginActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                        startActivity(intent);
                                    } else {
                                        // NORMAL USER -> SAVE "FALSE" KEY
                                        getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                                                .edit().putBoolean("isAdmin", false).apply();

                                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                    }
                                    finish(); // Close login page
                                })
                                .addOnFailureListener(e -> {
                                    // If database check fails (e.g., internet issue), default to normal user
                                    getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                                            .edit().putBoolean("isAdmin", false).apply();

                                    Toast.makeText(LoginActivity.this, "Login Successful (Offline Mode)", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                });

                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                        Toast.makeText(LoginActivity.this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}