package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.CartAdapter;
import com.example.shopease.models.CartItem;
import com.example.shopease.models.DatabaseHelper;
import com.example.shopease.repository.FirebaseCartRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.CartItemActionListener {

    // Constants
    private static final double TAX_RATE = 0.08; // 8% tax rate
    private String USER_ID; // Replace with actual user ID
    private DatabaseHelper databaseHelper;
    // UI Components
    private RecyclerView cartItemsRecyclerView;
    private TextView emptyCartMessage;
    private TextView subtotalTextView;
    private TextView deliveryFeeTextView;
    private TextView taxTextView;
    private TextView totalTextView;
    private MaterialButton proceedToCheckoutButton;
    private FloatingActionButton clearCartButton;
    private View loadingOverlay;
    private CardView orderSummaryCard;

    // Firebase
    private FirebaseCartRepository cartRepository;
    private DatabaseReference cartRef;

    // Adapter
    private CartAdapter cartAdapter;

    // Formatting
    private final DecimalFormat currencyFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);

        // Fetch userId from SQLite
        USER_ID = databaseHelper.getUserId();
        if (USER_ID == null) {
            Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
            USER_ID = "User123"; // Fallback to default if no user is found
        }

        // Initialize Firebase references
        cartRepository = new FirebaseCartRepository(USER_ID);
        cartRef = FirebaseDatabase.getInstance()
                .getReference("Carts").child(USER_ID);

        // Initialize UI components
        initializeViews();

        // Set up toolbar
        setupToolbar();

        // Set up RecyclerView
        setupRecyclerView();

        // Load cart items
        loadCartItems();

        // Set up click listeners
        setupListeners();
    }

    private void initializeViews() {
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        emptyCartMessage = findViewById(R.id.emptyCartMessage);
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryFeeTextView = findViewById(R.id.deliveryFeeTextView);
        taxTextView = findViewById(R.id.taxTextView);
        totalTextView = findViewById(R.id.totalTextView);
        proceedToCheckoutButton = findViewById(R.id.proceedToCheckoutButton);
        clearCartButton = findViewById(R.id.clearCartButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        orderSummaryCard = findViewById(R.id.orderSummaryCard);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.shopping_cart);
        }
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        // Proceed to checkout button
        proceedToCheckoutButton.setOnClickListener(v -> proceedToCheckout());

        // Clear cart button
        clearCartButton.setOnClickListener(v -> showClearCartConfirmationDialog());
    }

    private void loadCartItems() {
        showLoading(true);

        cartRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CartItem> items = new ArrayList<>();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item != null) {
                        item.setId(itemSnapshot.getKey());
                        items.add(item);
                    }
                }

                cartAdapter.setCartItems(items);
                updateCartUI(items.isEmpty());
                updateOrderSummary();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this,
                        getString(R.string.error_loading_cart, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void updateCartUI(boolean isEmpty) {
        if (isEmpty) {
            emptyCartMessage.setVisibility(View.VISIBLE);
            cartItemsRecyclerView.setVisibility(View.GONE);
            orderSummaryCard.setVisibility(View.GONE);
            proceedToCheckoutButton.setEnabled(false);
            clearCartButton.setVisibility(View.GONE);
        } else {
            emptyCartMessage.setVisibility(View.GONE);
            cartItemsRecyclerView.setVisibility(View.VISIBLE);
            orderSummaryCard.setVisibility(View.VISIBLE);
            proceedToCheckoutButton.setEnabled(true);
            clearCartButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateOrderSummary() {
        double subtotal = cartAdapter.calculateSubtotal();
        double deliveryFee = cartAdapter.calculateDeliveryFee();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + deliveryFee + tax;

        subtotalTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(subtotal)));
        deliveryFeeTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(deliveryFee)));
        taxTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(tax)));
        totalTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(total)));
    }

    @Override
    public void onRemoveItem(CartItem item) {
        showLoading(true);

        cartRepository.removeFromCart(item.getId(), new FirebaseCartRepository.CartOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showLoading(false);

                if (success) {
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.item_removed_from_cart),
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CartActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        showLoading(true);

        cartRepository.updateCartItemQuantity(item.getId(), newQuantity,
                new FirebaseCartRepository.CartOperationCallback() {
                    @Override
                    public void onComplete(boolean success, String message) {
                        showLoading(false);

                        if (!success) {
                            Toast.makeText(CartActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void proceedToCheckout() {
        // Get list of cart items
        List<CartItem> items = cartAdapter.getCartItems();

        if (items.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty_checkout, Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate order details for checkout
        double subtotal = cartAdapter.calculateSubtotal();
        double deliveryFee = cartAdapter.calculateDeliveryFee();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + deliveryFee + tax;


        // Start checkout activity with order details
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra("SUBTOTAL", subtotal);
        intent.putExtra("DELIVERY_FEE", deliveryFee);
        intent.putExtra("TAX", tax);
        intent.putExtra("TOTAL", total);
        startActivity(intent);
    }

    private void showClearCartConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_cart_title)
                .setMessage(R.string.clear_cart_confirmation)
                .setPositiveButton(R.string.clear, (dialog, which) -> clearCart())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearCart() {
        showLoading(true);

        cartRepository.clearCart(new FirebaseCartRepository.CartOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                showLoading(false);

                if (success) {
                    Toast.makeText(CartActivity.this, R.string.cart_cleared, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CartActivity.this, R.string.error_clearing_cart, Toast.LENGTH_SHORT).show();
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