package com.example.gymquest;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText nameField, emailField, passwordField;
    private Button signupBtn;
    private TextView loginLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind Views
        nameField = findViewById(R.id.nameSignup);
        emailField = findViewById(R.id.emailSignup);
        passwordField = findViewById(R.id.passwordSignup);
        signupBtn = findViewById(R.id.btnSignup);
        loginLink = findViewById(R.id.tvGoToLogin);

        signupBtn.setOnClickListener(v -> registerUser());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { nameField.setError("Name Required"); return; }
        if (TextUtils.isEmpty(email)) { emailField.setError("Email Required"); return; }
        if (TextUtils.isEmpty(password)) { passwordField.setError("Password Required"); return; }

        // --- STEP 1: Create Account in Authentication ---
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        // --- STEP 2: AUTO-UPDATE DATABASE ---
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("isAdmin", false); // Default everyone to 'User'

                        // Use the User ID (UID) as the document name
                        db.collection("Users").document(firebaseUser.getUid())
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {

                                    // --- NEW FIX: FORCE LOCAL MEMORY TO "NOT ADMIN" ---
                                    getSharedPreferences("GymAppPrefs", MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("isAdmin", false)
                                            .apply();
                                    // ---------------------------------------------------

                                    Toast.makeText(SignupActivity.this, "Account Created & Saved!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignupActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });

                    } else {
                        Toast.makeText(SignupActivity.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}