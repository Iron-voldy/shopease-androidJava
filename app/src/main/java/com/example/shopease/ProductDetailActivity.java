package com.example.shopease;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.shopease.models.DatabaseHelper;
import com.example.shopease.models.Product;
import com.example.shopease.repository.FirebaseCartRepository;
import com.example.shopease.repository.FirebaseWishlistRepository;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProductDetailActivity extends AppCompatActivity {


    private DatabaseReference databaseReference;
    private FirebaseWishlistRepository wishlistRepository;
    private FirebaseCartRepository cartRepository;
    private String productId, category, userId;

    private DatabaseHelper databaseHelper;

    // UI Components
    private TextView productTitle, productPrice, productDescription, sellerName;
    private TextView productCategory, productQuantity, deliveryFee, bottomProductPrice;
    private TextView availableQuantityDisplay;
    private ImageView productImage;
    private MaterialButton wishlistButton, addToCartButton;
    private FloatingActionButton backButton;
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private View loadingOverlay;

    private boolean isInWishlist = false;
    private int quantity = 1;
    private int availableQuantity = 0;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Fetch userId from SQLite
        userId = databaseHelper.getUserId();
        if (userId == null) {
            Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
            userId = "User123"; // Fallback
        }

        // Remove default title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize UI components
        initializeViews();

        // Set up click listeners
        setupListeners();

        // Retrieve Product ID and Category from Intent
        productId = getIntent().getStringExtra("PRODUCT_ID");
        category = getIntent().getStringExtra("CATEGORY");

        if (productId == null || category == null) {
            Toast.makeText(this, "Invalid Product Data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase references
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("Products").child(category).child(productId);
        wishlistRepository = new FirebaseWishlistRepository(userId);
        cartRepository = new FirebaseCartRepository(userId);

        fetchProductDetails();
        checkWishlistStatus();

        // Load reviews fragment by default
        loadReviewsFragment();
    }

    private void initializeViews() {
        // Text views
        productTitle = findViewById(R.id.productTitle);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        sellerName = findViewById(R.id.sellerName);
        productCategory = findViewById(R.id.productCategory);
        productQuantity = findViewById(R.id.productQuantity);
        deliveryFee = findViewById(R.id.deliveryFee);
        bottomProductPrice = findViewById(R.id.bottomProductPrice);
        availableQuantityDisplay = findViewById(R.id.availableQuantityDisplay);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Toolbar and collapsing toolbar
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Image and buttons
        productImage = findViewById(R.id.productImage);
        wishlistButton = findViewById(R.id.wishlistButton);
        addToCartButton = findViewById(R.id.addToCartButton);

    }

    private void setupListeners() {

        toolbar.setNavigationOnClickListener(v -> finish());
        wishlistButton.setOnClickListener(v -> toggleWishlist());
        addToCartButton.setOnClickListener(v -> addToCart());

        MaterialButton decreaseButton = findViewById(R.id.decreaseQuantityButton);
        MaterialButton increaseButton = findViewById(R.id.increaseQuantityButton);

        decreaseButton.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityDisplay();
            }
        });

        increaseButton.setOnClickListener(v -> {
            if (quantity < availableQuantity) {
                quantity++;
                updateQuantityDisplay();
            } else {
                Toast.makeText(this, getString(R.string.max_quantity_reached), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQuantityDisplay() {
        productQuantity.setText(String.valueOf(quantity));
    }

    private void fetchProductDetails() {
        showLoading(true);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                if (snapshot.exists()) {
                    currentProduct = snapshot.getValue(Product.class);
                    if (currentProduct != null) {
                        currentProduct.setId(productId);
                        availableQuantity = currentProduct.getQuantity();
                        displayProductDetails(currentProduct);
                    }
                } else {
                    Log.d("ProductDetailActivity", "Product not found in Firebase");
                    Toast.makeText(ProductDetailActivity.this, "Product not found!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e("ProductDetailActivity", "Error fetching product: " + error.getMessage());
                Toast.makeText(ProductDetailActivity.this, "Failed to load product", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProductDetails(Product product) {
        productTitle.setText(product.getTitle());
        productPrice.setText("$" + product.getPrice());
        productDescription.setText(product.getDescription());
        sellerName.setText(product.getSellerName());
        productCategory.setText(product.getCategory());
        productQuantity.setText(String.valueOf(quantity));
        deliveryFee.setText(getString(R.string.delivery_fee_format, "$" + product.getDeliveryFee()));
        bottomProductPrice.setText("$" + product.getPrice());
        availableQuantityDisplay.setText(getString(R.string.available_quantity, product.getQuantity()));

        updateAvailabilityUI(product.getQuantity());

        collapsingToolbar.setTitle(product.getTitle());
        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.white));

        loadProductImage(product);
    }

    private void updateAvailabilityUI(int availableQty) {
        if (availableQty <= 0) {
            addToCartButton.setEnabled(false);
            addToCartButton.setText(R.string.out_of_stock);
            availableQuantityDisplay.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else if (availableQty <= 5) {
            availableQuantityDisplay.setTextColor(ContextCompat.getColor(this, R.color.warning_yellow));
        } else {
            availableQuantityDisplay.setTextColor(ContextCompat.getColor(this, R.color.success_green));
        }
    }

    private void loadProductImage(Product product) {
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(product.getImageUrl())
                    .into(productImage);
        } else {
            Log.d("ProductDetailActivity", "No image URL available for this product");
        }
    }

    private void checkWishlistStatus() {
        if (productId != null && !productId.isEmpty()) {
            wishlistRepository.checkIfProductInWishlist(productId, isInWishlist -> {
                this.isInWishlist = isInWishlist;
                updateWishlistIcon();
            });
        }
    }

    private void toggleWishlist() {
        if (currentProduct == null) {
            Toast.makeText(this, "Product data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        wishlistButton.setEnabled(false);

        if (isInWishlist) {
            wishlistRepository.removeFromWishlist(productId, new FirebaseWishlistRepository.WishlistOperationCallback() {
                @Override
                public void onComplete(boolean success, String message) {
                    showLoading(false);
                    wishlistButton.setEnabled(true);
                    if (success) {
                        isInWishlist = false;
                        updateWishlistIcon();
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.item_removed_from_wishlist),
                                Snackbar.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            currentProduct.setId(productId);
            Product wishlistProduct = new Product(
                    productId,
                    currentProduct.getSellerId(),
                    currentProduct.getTitle(),
                    currentProduct.getPrice(),
                    currentProduct.getImageUrl(),
                    currentProduct.getDescription(),
                    currentProduct.getCategory(),
                    currentProduct.getSellerName(),
                    currentProduct.getQuantity(), // Use full available quantity for wishlist
                    currentProduct.getDeliveryFee()

            );

            wishlistRepository.addToWishlist(wishlistProduct, new FirebaseWishlistRepository.WishlistOperationCallback() {
                @Override
                public void onComplete(boolean success, String message) {
                    showLoading(false);
                    wishlistButton.setEnabled(true);
                    if (success) {
                        isInWishlist = true;
                        updateWishlistIcon();
                        Snackbar.make(findViewById(android.R.id.content),
                                        getString(R.string.added_to_wishlist),
                                        Snackbar.LENGTH_LONG)
                                .setAction(R.string.view_wishlist, v -> {
                                    Intent intent = new Intent(ProductDetailActivity.this, WishlistActivity.class);
                                    startActivity(intent);
                                })
                                .show();
                    } else {
                        Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void addToCart() {
        if (currentProduct == null) {
            Toast.makeText(this, "Product data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (quantity <= 0 || quantity > availableQuantity) {
            Toast.makeText(this, "Invalid quantity selected", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        addToCartButton.setEnabled(false);

        cartRepository.addToCart(currentProduct, quantity, new FirebaseCartRepository.CartOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showLoading(false);
                addToCartButton.setEnabled(true);

                if (success) {
                    Snackbar.make(findViewById(android.R.id.content),
                                    getString(R.string.added_to_cart_with_quantity, quantity, currentProduct.getTitle()),
                                    Snackbar.LENGTH_LONG)
                            .setAction(R.string.view_cart, v -> {
                                Intent intent = new Intent(ProductDetailActivity.this, CartActivity.class);
                                startActivity(intent);
                            })
                            .show();
                } else {
                    Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateWishlistIcon() {
        if (isInWishlist) {
            wishlistButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red)));
        } else {
            wishlistButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray)));
        }
    }

    private void showLoading(boolean isLoading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void loadReviewsFragment() {
        Log.d("ProductDetailActivity", "Loading reviews fragment for product: " + productId);
        ReviewsFragment reviewsFragment = ReviewsFragment.newInstance(productId);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.productDetailsViewPager, reviewsFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }
}