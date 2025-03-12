package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.WishlistAdapter;
import com.example.shopease.models.DatabaseHelper;
import com.example.shopease.models.Product;
import com.example.shopease.repository.FirebaseCartRepository;
import com.example.shopease.repository.FirebaseWishlistRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WishlistActivity extends AppCompatActivity implements WishlistAdapter.WishlistItemActionListener {

    private String USER_ID;
    private DatabaseHelper databaseHelper;

    // UI Components
    private RecyclerView wishlistItemsRecyclerView;
    private TextView emptyWishlistMessage;
    private MaterialButton addAllToCartButton;
    private FloatingActionButton clearWishlistButton;
    private View loadingOverlay;

    // Firebase
    private FirebaseWishlistRepository wishlistRepository;
    private FirebaseCartRepository cartRepository;
    private DatabaseReference wishlistRef;

    // Adapter
    private WishlistAdapter wishlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);
        USER_ID = databaseHelper.getUserId();
        if (USER_ID == null) {
            Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
            USER_ID = "User123"; // Fallback
        }

        // Initialize Firebase references
        wishlistRepository = new FirebaseWishlistRepository(USER_ID);
        cartRepository = new FirebaseCartRepository(USER_ID);
        wishlistRef = FirebaseDatabase.getInstance()
                .getReference("Wishlists").child(USER_ID);

        // Initialize UI components
        initializeViews();

        // Set up toolbar
        setupToolbar();

        // Set up RecyclerView
        setupRecyclerView();

        // Load wishlist items
        loadWishlistItems();

        // Set up click listeners
        setupListeners();
    }

    private void initializeViews() {
        wishlistItemsRecyclerView = findViewById(R.id.wishlistItemsRecyclerView);
        emptyWishlistMessage = findViewById(R.id.emptyWishlistMessage);
        addAllToCartButton = findViewById(R.id.addAllToCartButton);
        clearWishlistButton = findViewById(R.id.clearWishlistButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.wishlist);
        }
    }

    private void setupRecyclerView() {
        wishlistAdapter = new WishlistAdapter(this);
        wishlistItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        wishlistItemsRecyclerView.setAdapter(wishlistAdapter);
    }

    private void setupListeners() {
        addAllToCartButton.setOnClickListener(v -> addAllToCart());
        clearWishlistButton.setOnClickListener(v -> showClearWishlistConfirmationDialog());
    }

    private void loadWishlistItems() {
        showLoading(true);

        wishlistRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> items = new ArrayList<>();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Product item = itemSnapshot.getValue(Product.class);
                    if (item != null) {
                        item.setId(itemSnapshot.getKey());
                        items.add(item);
                    }
                }

                wishlistAdapter.setWishlistItems(items);
                updateWishlistUI(items.isEmpty());
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WishlistActivity.this,
                        getString(R.string.error_loading_wishlist, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void updateWishlistUI(boolean isEmpty) {
        if (isEmpty) {
            emptyWishlistMessage.setVisibility(View.VISIBLE);
            wishlistItemsRecyclerView.setVisibility(View.GONE);
            addAllToCartButton.setEnabled(false);
            clearWishlistButton.setVisibility(View.GONE);
        } else {
            emptyWishlistMessage.setVisibility(View.GONE);
            wishlistItemsRecyclerView.setVisibility(View.VISIBLE);
            addAllToCartButton.setEnabled(true);
            clearWishlistButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRemoveItem(Product item) {
        showLoading(true);

        wishlistRepository.removeFromWishlist(item.getId(), new FirebaseWishlistRepository.WishlistOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showLoading(false);
                if (success) {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.item_removed_from_wishlist),
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(WishlistActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onAddToCart(Product item) {
        showLoading(true);

        cartRepository.addToCart(item, 1, new FirebaseCartRepository.CartOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showLoading(false);
                if (success) {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.item_added_to_cart),
                            Snackbar.LENGTH_SHORT).show();
                    // Optionally remove from wishlist after adding to cart
                    wishlistRepository.removeFromWishlist(item.getId(), new FirebaseWishlistRepository.WishlistOperationCallback() {
                        @Override
                        public void onComplete(boolean success, String message) {
                            if (!success) {
                                Log.w("WishlistActivity", "Failed to remove from wishlist after adding to cart: " + message);
                            }
                        }
                    });
                } else {
                    Toast.makeText(WishlistActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addAllToCart() {
        List<Product> items = wishlistAdapter.getWishlistItems();
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.wishlist_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        int[] completed = {0};
        for (Product item : items) {
            cartRepository.addToCart(item, 1, new FirebaseCartRepository.CartOperationCallback() {
                @Override
                public void onComplete(boolean success, String message) {
                    completed[0]++;
                    if (success) {
                        wishlistRepository.removeFromWishlist(item.getId(), new FirebaseWishlistRepository.WishlistOperationCallback() {
                            @Override
                            public void onComplete(boolean success, String message) {
                                if (!success) {
                                    Log.w("WishlistActivity", "Failed to remove " + item.getId() + " from wishlist: " + message);
                                }
                            }
                        });
                    } else {
                        Log.w("WishlistActivity", "Failed to add " + item.getId() + " to cart: " + message);
                    }

                    if (completed[0] == items.size()) {
                        showLoading(false);
                        Toast.makeText(WishlistActivity.this, R.string.all_items_added_to_cart, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(WishlistActivity.this, CartActivity.class);
                        startActivity(intent);
                    }
                }
            });
        }
    }

    private void showClearWishlistConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_wishlist_title)
                .setMessage(R.string.clear_wishlist_confirmation)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearWishlist())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearWishlist() {
        showLoading(true);

        wishlistRepository.clearWishlist(new FirebaseWishlistRepository.WishlistOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showLoading(false);
                if (success) {
                    Toast.makeText(WishlistActivity.this, R.string.wishlist_cleared, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(WishlistActivity.this, R.string.error_clearing_wishlist, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
}