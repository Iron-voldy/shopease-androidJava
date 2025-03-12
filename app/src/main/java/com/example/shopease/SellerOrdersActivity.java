package com.example.shopease;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.SellerOrdersAdapter;
import com.example.shopease.models.SellerOrderItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity implements SellerOrdersAdapter.OnStatusUpdateListener {
    private static final String TAG = "SellerOrdersActivity";

    // UI components
    private RecyclerView ordersRecyclerView;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyOrdersView;
    private TabLayout orderStatusTabs;

    // Adapter and data
    private SellerOrdersAdapter ordersAdapter;
    private final List<SellerOrderItem> orderItems = new ArrayList<>();
    private final List<SellerOrderItem> filteredOrderItems = new ArrayList<>();

    // Firebase
    private FirebaseAuth auth;
    private DatabaseReference ordersRef;
    private DatabaseReference usersRef;
    private String sellerId;

    // Filter state
    private String currentStatusFilter = "All"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        // Initialize UI components
        setupToolbar();
        initViews();
        setupStatusTabs();

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sellerId = auth.getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance()
                .getReference("Orders");
        usersRef = FirebaseDatabase.getInstance()
                .getReference("Users");

        // Setup RecyclerView and adapter
        setupRecyclerView();

        // Load seller orders
        fetchSellerOrders();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Your Orders");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        ordersRecyclerView = findViewById(R.id.sellerOrdersRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        emptyOrdersView = findViewById(R.id.emptyOrdersView);
        orderStatusTabs = findViewById(R.id.orderStatusTabs);
    }

    private void setupStatusTabs() {
        // Add tab items
        orderStatusTabs.addTab(orderStatusTabs.newTab().setText("All"));
        orderStatusTabs.addTab(orderStatusTabs.newTab().setText("Processing"));
        orderStatusTabs.addTab(orderStatusTabs.newTab().setText("Packed"));
        orderStatusTabs.addTab(orderStatusTabs.newTab().setText("Shipped"));
        orderStatusTabs.addTab(orderStatusTabs.newTab().setText("Delivered"));

        // Set tab selection listener
        orderStatusTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentStatusFilter = tab.getText().toString();
                filterOrdersByStatus();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void setupRecyclerView() {
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new SellerOrdersAdapter(this, filteredOrderItems, this);
        ordersRecyclerView.setAdapter(ordersAdapter);
    }

    private void fetchSellerOrders() {
        showLoading(true);

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Orders data received: " + snapshot.getChildrenCount() + " orders");

                orderItems.clear();

                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String orderId = orderSnapshot.getKey();
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    Date orderDate = timestamp != null ? new Date(timestamp) : new Date();

                    // Get buyer information
                    String buyerId = orderSnapshot.child("userId").getValue(String.class);

                    // Get payment method from order
                    String paymentMethod = orderSnapshot.child("paymentMethod").getValue(String.class);
                    if (paymentMethod == null) {
                        paymentMethod = "Not specified";
                    }


                    // Process products in this order
                    DataSnapshot productsSnapshot = orderSnapshot.child("products");
                    if (productsSnapshot.exists()) {
                        processOrderProducts(orderId, buyerId, orderDate, paymentMethod, productsSnapshot);
                    }
                }

                // Filter and update UI
                filterOrdersByStatus();
                showLoading(false);
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching orders: " + error.getMessage());
                Toast.makeText(SellerOrdersActivity.this, "Failed to fetch orders", Toast.LENGTH_SHORT).show();
                showLoading(false);
                updateEmptyState();
            }
        });
    }

    private void processOrderProducts(String orderId, String buyerId, Date orderDate, String paymentMethod, DataSnapshot productsSnapshot) {
        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
            String productId = productSnapshot.getKey();

            // Check if this is the current seller's product
            String productSellerId = productSnapshot.child("sellerId").getValue(String.class);
            if (productSellerId != null && productSellerId.equals(sellerId)) {
                // Extract product details
                String title = productSnapshot.child("title").getValue(String.class);
                String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);
                Double price = productSnapshot.child("price").getValue(Double.class);
                Integer quantity = productSnapshot.child("quantity").getValue(Integer.class);
                Double deliveryFee = productSnapshot.child("deliveryFee").getValue(Double.class);
                String sellerName = productSnapshot.child("sellerName").getValue(String.class);
                String deliveryStatus = productSnapshot.child("deliveryStatus").getValue(String.class);

                // Set default values if null
                if (price == null) price = 0.0;
                if (quantity == null) quantity = 0;
                if (deliveryFee == null) deliveryFee = 0.0;
                if (deliveryStatus == null) deliveryStatus = "Processing";

                // Get buyer name
                Double finalPrice = price;
                Integer finalQuantity = quantity;
                String finalDeliveryStatus = deliveryStatus;
                Double finalDeliveryFee = deliveryFee;
                fetchBuyerName(buyerId, buyerName -> {
                    // Create order item
                    SellerOrderItem orderItem = new SellerOrderItem(
                            orderId,
                            productId,
                            title,
                            imageUrl,
                            finalPrice,
                            finalQuantity,
                            sellerId,
                            sellerName,
                            buyerId,
                            buyerName,
                            finalDeliveryStatus,
                            finalDeliveryFee,
                            orderDate,
                            paymentMethod
                    );

                    // Add to list
                    orderItems.add(orderItem);

                    // Refresh the filtered list
                    filterOrdersByStatus();
                });
            }
        }
    }

    private void fetchBuyerName(String buyerId, OnBuyerNameFetchedListener listener) {
        if (buyerId == null || buyerId.isEmpty()) {
            listener.onBuyerNameFetched("Unknown Customer");
            return;
        }

        usersRef.child(buyerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);

                String fullName;
                if (firstName != null && lastName != null) {
                    fullName = firstName + " " + lastName;
                } else if (firstName != null) {
                    fullName = firstName;
                } else if (lastName != null) {
                    fullName = lastName;
                } else {
                    fullName = "Customer " + buyerId.substring(0, Math.min(buyerId.length(), 5));
                }

                listener.onBuyerNameFetched(fullName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching buyer info: " + error.getMessage());
                listener.onBuyerNameFetched("Unknown Customer");
            }
        });
    }

    private interface OnBuyerNameFetchedListener {
        void onBuyerNameFetched(String buyerName);
    }

    private void filterOrdersByStatus() {
        filteredOrderItems.clear();

        if (currentStatusFilter.equals("All")) {
            filteredOrderItems.addAll(orderItems);
        } else {
            for (SellerOrderItem item : orderItems) {
                if (currentStatusFilter.equals(item.getDeliveryStatus())) {
                    filteredOrderItems.add(item);
                }
            }
        }

        // Update UI
        ordersAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredOrderItems.isEmpty()) {
            emptyOrdersView.setVisibility(View.VISIBLE);
            ordersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyOrdersView.setVisibility(View.GONE);
            ordersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            ordersRecyclerView.setVisibility(View.GONE);
            emptyOrdersView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStatusUpdate(String orderId, String productId, String newStatus) {
        if (orderId == null || productId == null || newStatus == null) {
            Toast.makeText(this, "Invalid update request", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Update the status in Firebase
        DatabaseReference productRef = ordersRef.child(orderId)
                .child("products")
                .child(productId)
                .child("deliveryStatus");

        productRef.setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Status updated successfully");
                    Toast.makeText(SellerOrdersActivity.this,
                            "Delivery status updated to " + newStatus,
                            Toast.LENGTH_SHORT).show();

                    // Also update the overall order status if needed
                    updateOrderOverallStatus(orderId);

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update status: " + e.getMessage());
                    Toast.makeText(SellerOrdersActivity.this,
                            "Failed to update status",
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    // Helper method to update the overall order status based on product statuses
    private void updateOrderOverallStatus(String orderId) {
        ordersRef.child(orderId).child("products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String lowestStatus = "Delivered"; // Start with highest status

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    String status = productSnapshot.child("deliveryStatus").getValue(String.class);
                    if (status == null) status = "Processing";

                    // Determine lower status (Processing < Packed < Shipped < Delivered)
                    if (status.equals("Processing")) {
                        lowestStatus = "Processing";
                        break; // No need to check further
                    } else if (status.equals("Packed") && !lowestStatus.equals("Processing")) {
                        lowestStatus = "Packed";
                    } else if (status.equals("Shipped") && !lowestStatus.equals("Processing")
                            && !lowestStatus.equals("Packed")) {
                        lowestStatus = "Shipped";
                    }
                }

                // Update overall order status
                ordersRef.child(orderId).child("status").setValue(lowestStatus);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error updating overall order status: " + error.getMessage());
            }
        });
    }
}