package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private CardView manageUsersCard, manageProductsCard, manageOrdersCard, salesReportCard;
    private TextView usersCount, productsCount, ordersCount;
    private FloatingActionButton refreshFab;
    private DatabaseReference databaseRef;
    private FirebaseAuth firebaseAuth;
    private int totalProductCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize Firebase Realtime Database
        databaseRef = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI components
        initViews();
        setupClickListeners();
        loadDashboardData();
    }

    private void initViews() {
        // Cards
        manageUsersCard = findViewById(R.id.manageUsersCard);
        manageProductsCard = findViewById(R.id.manageProductsCard);
        manageOrdersCard = findViewById(R.id.manageOrdersCard);
        salesReportCard = findViewById(R.id.salesReportCard);

        // Count TextViews
        usersCount = findViewById(R.id.usersCount);
        productsCount = findViewById(R.id.productsCount);
        ordersCount = findViewById(R.id.ordersCount);

        // FAB
        refreshFab = findViewById(R.id.refreshFab);
    }

    private void setupClickListeners() {
        // Navigation to management screens
        manageUsersCard.setOnClickListener(v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageUsersActivity.class)));

        manageProductsCard.setOnClickListener(v ->
                startActivity(new Intent(AdminDashboardActivity.this, ManageProductsActivity.class)));

        manageOrdersCard.setOnClickListener(v ->
                startActivity(new Intent(AdminDashboardActivity.this, OrdersManagementActivity.class)));

        salesReportCard.setOnClickListener(v -> {
            // Create an intent to SalesReportActivity
            Intent intent = new Intent(AdminDashboardActivity.this, SalesReportActivity.class);
            startActivity(intent);
        });

        // Refresh button
        refreshFab.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing dashboard data...", Toast.LENGTH_SHORT).show();
            loadDashboardData();
        });
    }

    private void loadDashboardData() {
        // Load Users Count
        databaseRef.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    usersCount.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                } else {
                    usersCount.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load users data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Load Products Count - Since products are organized by categories, we need to count all products
        databaseRef.child("Products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    totalProductCount = 0;
                    // Products are organized by categories, so we need to count all products across categories
                    for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                        totalProductCount += categorySnapshot.getChildrenCount();
                    }
                    productsCount.setText(String.valueOf(totalProductCount));
                } else {
                    productsCount.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load products data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Load Orders Count
        databaseRef.child("Orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ordersCount.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                } else {
                    ordersCount.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Failed to load orders data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            // Handle logout using Firebase Auth
            firebaseAuth.signOut();

            // Navigate to login screen
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            // Navigate to settings
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the dashboard
        loadDashboardData();
    }
}