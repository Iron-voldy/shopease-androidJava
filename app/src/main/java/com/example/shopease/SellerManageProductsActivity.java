package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.SellerProductsAdapter;
import com.example.shopease.models.SellerProduct;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.view.View;
import android.widget.ProgressBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SellerManageProductsActivity extends AppCompatActivity {

    private RecyclerView productsRecyclerView;
    private SellerProductsAdapter productsAdapter;
    private List<SellerProduct> productList;
    private DatabaseReference productsRef;
    private String sellerId;

    private TextView totalProductsValue;
    private ProgressBar progressBar;
    private FloatingActionButton addProductFab;

    private MaterialButton addNewProductButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_manage_products);

        setupToolbar();

        // Initialize Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        sellerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        productsRef = FirebaseDatabase.getInstance().getReference("Products");

        // Initialize UI elements
        initializeViews();
        fetchSellerInfo();

        // Setup RecyclerView Adapter
        productList = new ArrayList<>();
        productsAdapter = new SellerProductsAdapter(productList, this::editProduct, this::deleteProduct);
        productsRecyclerView.setAdapter(productsAdapter);

        // Handle Add Product FAB click
        addProductFab.setOnClickListener(v -> {
            Intent intent = new Intent(SellerManageProductsActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        addNewProductButton.setOnClickListener(v -> {
            Intent intent = new Intent(SellerManageProductsActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        // Fetch and display products from Firebase
        fetchSellerProducts();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Products");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        productsRecyclerView = findViewById(R.id.sellerProductsRecyclerView);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        totalProductsValue = findViewById(R.id.totalProductsValue);
        progressBar = findViewById(R.id.loadingProgressBar);
        addProductFab = findViewById(R.id.addProductFab);
        addNewProductButton = findViewById(R.id.addNewProductButton);
    }

    private void fetchSellerInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            sellerId = user.getUid();
        } else {
            sellerId = "UnknownSeller";
        }
    }

    private void fetchSellerProducts() {
        // Show ProgressBar while loading products
        progressBar.setVisibility(View.VISIBLE);

        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                int totalProducts = 0;  // Count products

                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot productSnapshot : categorySnapshot.getChildren()) {
                        SellerProduct product = productSnapshot.getValue(SellerProduct.class);
                        if (product != null && sellerId.equals(product.getSellerId())) {
                            product.setProductId(productSnapshot.getKey());
                            product.setCategory(categorySnapshot.getKey());
                            productList.add(product);
                            totalProducts++;
                        }
                    }
                }

                // Set the total products count
                totalProductsValue.setText(String.valueOf(totalProducts));

                // Hide ProgressBar after loading
                progressBar.setVisibility(View.INVISIBLE);

                productsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching products: " + error.getMessage());
                Toast.makeText(SellerManageProductsActivity.this, "Failed to fetch products.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void editProduct(SellerProduct product) {
        Intent intent = new Intent(this, SellerEditProductActivity.class);
        intent.putExtra("productId", product.getProductId());
        intent.putExtra("category", product.getCategory());
        startActivity(intent);
    }

    private void deleteProduct(String productId, String category) {
        productsRef.child(category).child(productId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product deleted successfully.", Toast.LENGTH_SHORT).show();
                    fetchSellerProducts(); // Refresh list after deletion
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete product.", Toast.LENGTH_SHORT).show());
    }
}
