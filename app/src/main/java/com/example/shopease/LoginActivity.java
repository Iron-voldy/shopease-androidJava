package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.shopease.models.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private boolean isLoggingIn = false; // Prevents multiple login requests
    private ProgressBar progressBar; // Loading indicator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();


        // UI Elements
        EditText emailField = findViewById(R.id.emailField);
        EditText passwordField = findViewById(R.id.passwordField);
        Button loginButton = findViewById(R.id.loginButton);
        TextView signUpText = findViewById(R.id.signUpText);
        progressBar = findViewById(R.id.progressBar); // Add a ProgressBar in your layout

        // Handle Login button click
        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                loginUser(email, password);
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle SignUp navigation
        signUpText.setOnClickListener(v -> startActivity(new Intent(this, RegistrationActivity.class)));
    }

    private void loginUser(String email, String password) {
        if (isLoggingIn) return; // Prevent multiple logins
        isLoggingIn = true;
        progressBar.setVisibility(View.VISIBLE); // Show loading indicator

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    isLoggingIn = false;
                    progressBar.setVisibility(View.GONE); // Hide loading indicator

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            navigateBasedOnUserRole(user.getUid());
                        } else {
                            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateBasedOnUserRole(String userId) {
        DatabaseReference usersRef = database.getReference("Users").child(userId); // Only get data for this user

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DatabaseHelper dbHelper = new DatabaseHelper(LoginActivity.this);

                    // Delete all existing data first
                    dbHelper.deleteAllUsers();

                    // Get and save only the current user's data
                    String id = snapshot.getKey();
                    String email = snapshot.child("email").getValue(String.class);
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);
                    boolean isBlocked = snapshot.child("isBlocked").getValue(Boolean.class) != null &&
                            snapshot.child("isBlocked").getValue(Boolean.class);

                    // Store only this user's data in SQLite
                    dbHelper.insertOrUpdateUser(id, email, firstName, lastName, role, isBlocked);

                    // Navigate based on role
                    if (role != null) {
                        Intent intent;
                        switch (role) {
                            case "user":
                                intent = new Intent(LoginActivity.this, HomeActivity.class);
                                break;
                            case "seller":
                                intent = new Intent(LoginActivity.this, SellerDashboardActivity.class);
                                break;
                            case "admin":
                                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                                break;
                            default:
                                Toast.makeText(LoginActivity.this, "Unknown role", Toast.LENGTH_SHORT).show();
                                return;
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "User role not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "User data missing", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Failed to retrieve user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}