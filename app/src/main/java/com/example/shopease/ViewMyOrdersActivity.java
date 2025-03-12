package com.example.shopease;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.OrderAdapter;
import com.example.shopease.models.DatabaseHelper;
import com.example.shopease.models.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewMyOrdersActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private ProgressBar loadingProgressBar;
    private OrderAdapter orderAdapter;
    private DatabaseReference ordersRef;
    private String USER_ID;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_my_orders);

        // Initialize DatabaseHelper and fetch USER_ID
        databaseHelper = new DatabaseHelper(this);
        USER_ID = databaseHelper.getUserId();
        if (USER_ID == null) {
            Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
            USER_ID = "User123"; // Fallback
        }

        // Initialize Firebase reference
        ordersRef = FirebaseDatabase.getInstance()
                .getReference("Orders");

        // Initialize UI components
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        // Setup RecyclerView
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter();
        ordersRecyclerView.setAdapter(orderAdapter);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_orders);
        }

        // Fetch and display orders
        fetchOrders();
    }

    private void fetchOrders() {
        showLoading(true);
        ordersRef.orderByChild("userId").equalTo(USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Order> orderList = new ArrayList<>();
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null) {
                        orderList.add(order);
                        Log.d("ViewMyOrders", "Order loaded: " + order.getOrderId());
                    }
                }
                orderAdapter.setOrderList(orderList);
                showLoading(false);
                if (orderList.isEmpty()) {
                    Toast.makeText(ViewMyOrdersActivity.this, "No orders found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e("ViewMyOrders", "Failed to fetch orders: " + error.getMessage());
                Toast.makeText(ViewMyOrdersActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        ordersRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}