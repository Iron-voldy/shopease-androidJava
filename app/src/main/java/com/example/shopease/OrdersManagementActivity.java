package com.example.shopease;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.shopease.adapters.AdminOrderAdapter;
import com.example.shopease.models.OrderModel;
import com.example.shopease.models.OrderProductModel;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdersManagementActivity extends AppCompatActivity implements AdminOrderAdapter.OrderActionListener {

    private RecyclerView recyclerView;
    private AdminOrderAdapter orderAdapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout tabLayout;

    private DatabaseReference ordersRef;
    private List<OrderModel> orderList;
    private List<OrderModel> filteredOrderList;
    private String currentFilter = "all"; // Default filter: all, pending, processing, delivered, cancelled

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_management);

        // Initialize Firebase
        ordersRef = FirebaseDatabase.getInstance().getReference().child("Orders");

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Orders");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize UI components
        initViews();
        setupListeners();

        // Initialize order lists
        orderList = new ArrayList<>();
        filteredOrderList = new ArrayList<>();

        // Setup RecyclerView and adapter
        setupRecyclerView();

        // Load orders
        loadOrders();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.ordersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchEditText = findViewById(R.id.searchEditText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterOrders(s.toString());
            }
        });

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);

        // Tab selection for filtering
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "Pending";
                        break;
                    case 2:
                        currentFilter = "Processing";
                        break;
                    case 3:
                        currentFilter = "Delivered";
                        break;
                    case 4:
                        currentFilter = "Cancelled";
                        break;
                }
                filterOrders(searchEditText.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new AdminOrderAdapter(filteredOrderList, this);
        recyclerView.setAdapter(orderAdapter);
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                orderList.clear();

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderId = orderSnapshot.getKey();

                    // Get basic order details
                    String userId = orderSnapshot.child("userId").getValue(String.class);
                    String status = orderSnapshot.child("status").getValue(String.class);
                    Double amount = orderSnapshot.child("amount").getValue(Double.class);
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    String address = orderSnapshot.child("address").getValue(String.class);
                    String city = orderSnapshot.child("city").getValue(String.class);
                    String state = orderSnapshot.child("state").getValue(String.class);
                    String zipCode = orderSnapshot.child("zipCode").getValue(String.class);
                    String phone = orderSnapshot.child("phone").getValue(String.class);
                    String paymentMethod = orderSnapshot.child("paymentMethod").getValue(String.class);

                    // Get products in the order
                    Map<String, OrderProductModel> products = new HashMap<>();
                    DataSnapshot productsSnapshot = orderSnapshot.child("products");

                    for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                        String productId = productSnapshot.getKey();
                        String title = productSnapshot.child("title").getValue(String.class);
                        Double price = productSnapshot.child("price").getValue(Double.class);
                        Integer quantity = productSnapshot.child("quantity").getValue(Integer.class);
                        String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);
                        String sellerId = productSnapshot.child("sellerId").getValue(String.class);
                        String sellerName = productSnapshot.child("sellerName").getValue(String.class);
                        Double deliveryFee = productSnapshot.child("deliveryFee").getValue(Double.class);
                        String deliveryStatus = productSnapshot.child("deliveryStatus").getValue(String.class);

                        OrderProductModel product = new OrderProductModel(
                                productId, title, price, quantity, imageUrl, sellerId,
                                sellerName, deliveryFee, deliveryStatus
                        );

                        products.put(productId, product);
                    }

                    if (orderId != null && status != null && amount != null && timestamp != null) {
                        OrderModel order = new OrderModel(
                                orderId, userId, status, amount, timestamp, address, city,
                                state, zipCode, phone, paymentMethod, products
                        );

                        orderList.add(order);
                    }
                }

                // Sort orders by timestamp (newest first)
                Collections.sort(orderList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                // Apply current filter and search
                filterOrders(searchEditText.getText().toString());

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                updateEmptyViewVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(OrdersManagementActivity.this,
                        "Failed to load orders: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                updateEmptyViewVisibility();
            }
        });
    }

    private void filterOrders(String searchText) {
        filteredOrderList.clear();

        for (OrderModel order : orderList) {
            // Apply status filter
            if (!currentFilter.equals("all") && !order.getStatus().equals(currentFilter)) {
                continue;
            }

            // Apply search filter
            if (searchText.isEmpty() ||
                    order.getOrderId().toLowerCase().contains(searchText.toLowerCase()) ||
                    order.getUserId().toLowerCase().contains(searchText.toLowerCase()) ||
                    (order.getAddress() != null && order.getAddress().toLowerCase().contains(searchText.toLowerCase()))) {
                filteredOrderList.add(order);
            }
        }

        orderAdapter.notifyDataSetChanged();
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        if (filteredOrderList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (searchEditText.getText().toString().isEmpty() && currentFilter.equals("all")) {
                emptyView.setText("No orders found");
            } else {
                emptyView.setText("No matching orders found");
            }
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewOrderDetails(OrderModel order) {
        // Navigate to order details activity
        OrderDetailsDialogFragment dialogFragment = OrderDetailsDialogFragment.newInstance(order);
        dialogFragment.show(getSupportFragmentManager(), "OrderDetails");
    }

    @Override
    public void onUpdateOrderStatus(OrderModel order, String newStatus) {
        // Update order status in Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        ordersRef.child(order.getOrderId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    order.setStatus(newStatus);
                    orderAdapter.notifyDataSetChanged();
                    Toast.makeText(OrdersManagementActivity.this,
                            "Order status updated to " + newStatus,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(OrdersManagementActivity.this,
                                "Failed to update order status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}