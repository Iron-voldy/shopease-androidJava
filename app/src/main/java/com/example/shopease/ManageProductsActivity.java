package com.example.shopease;

import android.content.Intent;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.shopease.adapters.AdminProductAdapter;
import com.example.shopease.models.ProductModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageProductsActivity extends AppCompatActivity implements AdminProductAdapter.ProductActionListener {

    private RecyclerView recyclerView;
    private AdminProductAdapter productAdapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout tabLayout;
    private FloatingActionButton addProductFab;

    private DatabaseReference productsRef;
    private List<ProductModel> productList;
    private List<ProductModel> filteredProductList;
    private String currentCategory = "all"; // Default filter: all or specific category

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);

        // Initialize Firebase
        productsRef = FirebaseDatabase.getInstance().getReference().child("Products");

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Products");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize UI components
        initViews();
        setupListeners();

        // Initialize product lists
        productList = new ArrayList<>();
        filteredProductList = new ArrayList<>();

        // Setup RecyclerView and adapter
        setupRecyclerView();

        // Load product categories for tabs
        loadProductCategories();

        // Load products data
        loadProducts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.productsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchEditText = findViewById(R.id.searchEditText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tabLayout = findViewById(R.id.tabLayout);
        addProductFab = findViewById(R.id.addProductFab);
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
                filterProducts(s.toString());
            }
        });

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadProducts);

        // Tab selection for filtering by category
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String tabText = tab.getText().toString();
                currentCategory = tabText.equals("All") ? "all" : tabText;
                filterProducts(searchEditText.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Add new product
        addProductFab.setOnClickListener(v -> {
            // Navigate to add/edit product activity
            //Intent intent = new Intent(ManageProductsActivity.this, AddEditProductActivity.class);
            // startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        // Use GridLayoutManager with 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        productAdapter = new AdminProductAdapter(filteredProductList, this);
        recyclerView.setAdapter(productAdapter);
    }

    private void loadProductCategories() {
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear existing tabs except the "All" tab
                tabLayout.removeAllTabs();

                // Add "All" tab first
                tabLayout.addTab(tabLayout.newTab().setText("All"));

                // Add tab for each category
                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    String category = categorySnapshot.getKey();
                    if (category != null) {
                        tabLayout.addTab(tabLayout.newTab().setText(category));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageProductsActivity.this,
                        "Failed to load product categories: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                productList.clear();

                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    String category = categorySnapshot.getKey();

                    for (DataSnapshot productSnapshot : categorySnapshot.getChildren()) {
                        String productId = productSnapshot.getKey();

                        String title = productSnapshot.child("title").getValue(String.class);
                        String description = productSnapshot.child("description").getValue(String.class);
                        Double price = productSnapshot.child("price").getValue(Double.class);
                        Integer quantity = productSnapshot.child("quantity").getValue(Integer.class);
                        Double deliveryFee = productSnapshot.child("deliveryFee").getValue(Double.class);
                        String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);
                        String sellerId = productSnapshot.child("sellerId").getValue(String.class);
                        String sellerName = productSnapshot.child("sellerName").getValue(String.class);

                        if (title != null && price != null && quantity != null) {
                            ProductModel product = new ProductModel(
                                    productId, title, description, price, quantity,
                                    category, imageUrl, deliveryFee, sellerId, sellerName
                            );

                            productList.add(product);
                        }
                    }
                }

                // Sort products by title
                Collections.sort(productList, (p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));

                // Apply current filter and search
                filterProducts(searchEditText.getText().toString());

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                updateEmptyViewVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageProductsActivity.this,
                        "Failed to load products: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                updateEmptyViewVisibility();
            }
        });
    }

    private void filterProducts(String searchText) {
        filteredProductList.clear();

        for (ProductModel product : productList) {
            // Apply category filter
            if (!currentCategory.equals("all") && !product.getCategory().equals(currentCategory)) {
                continue;
            }

            // Apply search filter
            if (searchText.isEmpty() ||
                    product.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                    (product.getDescription() != null &&
                            product.getDescription().toLowerCase().contains(searchText.toLowerCase()))) {
                filteredProductList.add(product);
            }
        }

        productAdapter.notifyDataSetChanged();
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        if (filteredProductList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (searchEditText.getText().toString().isEmpty() && currentCategory.equals("all")) {
                emptyView.setText("No products found");
            } else {
                emptyView.setText("No matching products found");
            }
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onEditProduct(ProductModel product) {
        // Navigate to edit product activity
        //Intent intent = new Intent(ManageProductsActivity.this, AddEditProductActivity.class);
        // intent.putExtra("PRODUCT_ID", product.getProductId());
        //intent.putExtra("PRODUCT_CATEGORY", product.getCategory());
        // startActivity(intent);
    }

    @Override
    public void onDeleteProduct(ProductModel product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getTitle() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Firebase
                    DatabaseReference productRef = productsRef
                            .child(product.getCategory())
                            .child(product.getProductId());

                    productRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                productList.remove(product);
                                filterProducts(searchEditText.getText().toString());
                                Toast.makeText(ManageProductsActivity.this,
                                        "Product deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ManageProductsActivity.this,
                                            "Failed to delete product: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewProductDetails(ProductModel product) {
        // Show product details dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_product_details, null);

        // Set product details in dialog
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        TextView descriptionTextView = dialogView.findViewById(R.id.descriptionTextView);
        TextView priceTextView = dialogView.findViewById(R.id.priceTextView);
        TextView quantityTextView = dialogView.findViewById(R.id.quantityTextView);
        TextView categoryTextView = dialogView.findViewById(R.id.categoryTextView);
        TextView sellerTextView = dialogView.findViewById(R.id.sellerTextView);
        TextView deliveryFeeTextView = dialogView.findViewById(R.id.deliveryFeeTextView);

        titleTextView.setText(product.getTitle());

        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            descriptionTextView.setText(product.getDescription());
        } else {
            descriptionTextView.setText("No description available");
        }

        priceTextView.setText("$" + product.getPrice());
        quantityTextView.setText(String.valueOf(product.getQuantity()));
        categoryTextView.setText(product.getCategory());

        if (product.getSellerName() != null && !product.getSellerName().isEmpty()) {
            sellerTextView.setText(product.getSellerName());
        } else {
            sellerTextView.setText("Unknown seller");
        }

        if (product.getDeliveryFee() != null) {
            deliveryFeeTextView.setText("$" + product.getDeliveryFee());
        } else {
            deliveryFeeTextView.setText("Not specified");
        }

        builder.setTitle("Product Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadProducts();
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