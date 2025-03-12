package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registration);

        // Initialize FirebaseAuth and FirebaseDatabase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // UI Elements
        EditText firstNameField = findViewById(R.id.firstNameField);
        EditText lastNameField = findViewById(R.id.lastNameField);
        EditText emailField = findViewById(R.id.emailField);
        EditText passwordField = findViewById(R.id.passwordField);
        Button registerButton = findViewById(R.id.registerButton);
        TextView signInText = findViewById(R.id.signInText);

        registerButton.setOnClickListener(v -> {
            String firstName = firstNameField.getText().toString().trim();
            String lastName = lastNameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Validate inputs
            if (!validateName(firstName)) {
                firstNameField.setError("Invalid first name (only letters, 2-50 characters)");
                return;
            }

            if (!validateName(lastName)) {
                lastNameField.setError("Invalid last name (only letters, 2-50 characters)");
                return;
            }

            if (!validateEmail(email)) {
                emailField.setError("Invalid email format");
                return;
            }

            if (!validatePassword(password)) {
                passwordField.setError("Password must be 8+ characters with uppercase, lowercase, number, and special character");
                return;
            }

            // If all validations pass, register the user
            registerUser(firstName, lastName, email, password);
        });

        // Handle "I already have an account" click event
        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close RegistrationActivity
        });
    }
    private void registerUser(String firstName, String lastName, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                        if (userId == null) return;

                        Map<String, Object> user = new HashMap<>();
                        user.put("firstName", firstName);
                        user.put("lastName", lastName);
                        user.put("email", email);
                        user.put("role", "user");
                        user.put("isBlocked", false);

                        database.getReference("Users").child(userId).setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                });
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Validation functions
    private boolean validateName(String name) {
        return name.matches("^[A-Za-z]{2,50}$"); // Only letters, 2-50 characters
    }

    private boolean validateEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean validatePassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[@#$%^&+=!].*");
    }
}