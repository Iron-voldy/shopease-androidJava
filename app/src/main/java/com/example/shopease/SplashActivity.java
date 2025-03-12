package com.example.shopease;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.shopease.models.DatabaseHelper;
import com.google.firebase.FirebaseApp;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkAppState();
        }, 3000);
    }

    private void checkAppState() {
        SharedPreferences sharedPreferences = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE);
        boolean isOnboardingCompleted = sharedPreferences.getBoolean("isOnboardingCompleted", false);

        if (!isOnboardingCompleted) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            String role = dbHelper.getUserRole();  // Fetch user role

            if (role != null) {
                navigateBasedOnRole(role);
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        }
        finish();
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;
        switch (role) {
            case "user":
                intent = new Intent(this, HomeActivity.class);
                break;
            case "seller":
                intent = new Intent(this, SellerDashboardActivity.class);
                break;
            case "admin":
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            default:
                Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show();
                return;
        }
        startActivity(intent);
    }

}
