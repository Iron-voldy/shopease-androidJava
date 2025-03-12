package com.example.shopease;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.shopease.models.CartItem;
import com.example.shopease.models.DatabaseHelper;

import com.example.shopease.repository.FirebaseCartRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutActivity extends AppCompatActivity {

    // UI Components
    private String USER_ID; // Replace with actual user ID

    private String firstName,lastName,address,city,state,zipCode,phone,email;
    private TextView subtotalTextView;
    private TextView deliveryFeeTextView;
    private TextView taxTextView;
    private TextView totalTextView;
    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private TextInputLayout addressLayout;
    private TextInputEditText addressInput;
    private TextInputLayout cityLayout;
    private TextInputEditText cityInput;
    private TextInputLayout stateLayout;
    private TextInputEditText stateInput;
    private TextInputLayout zipCodeLayout;
    private TextInputEditText zipCodeInput;
    private TextInputLayout phoneLayout;
    private TextInputEditText phoneInput;
    private RadioGroup paymentMethodGroup;
    private MaterialButton placeOrderButton;
    private View loadingOverlay;
    private static final int PAYHERE_REQUEST = 11001;
    // Order details
    private double subtotal;
    private double deliveryFee;
    private double tax;
    private double total;

    private DatabaseHelper databaseHelper;
    // Firebase
    private FirebaseCartRepository cartRepository;
    private DatabaseReference usersReference;

    // Formatting
    private final DecimalFormat currencyFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        //Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(this);
        // Fetch userId from SQLite
        USER_ID = databaseHelper.getUserId();
        if (USER_ID == null) {
            Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
            USER_ID = "User123"; // Fallback to default if no user is found
        }

        // Initialize Firebase references
        usersReference = FirebaseDatabase.getInstance()
                .getReference("Users");
        // Initialize Firebase repository
        cartRepository = new FirebaseCartRepository(USER_ID);

        // Initialize UI components
        initializeViews();

        // Set up toolbar
        setupToolbar();

        // Get order details from intent
        getOrderDetailsFromIntent();

        // Display order summary
        displayOrderSummary();

        // Set up listeners
        setupListeners();

        // Fetch and populate user details from Firebase
        fetchUserDetails();
    }

    private void initializeViews() {
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryFeeTextView = findViewById(R.id.deliveryFeeTextView);
        taxTextView = findViewById(R.id.taxTextView);
        totalTextView = findViewById(R.id.totalTextView);

        nameLayout = findViewById(R.id.nameLayout);
        nameInput = findViewById(R.id.nameInput);
        addressLayout = findViewById(R.id.addressLayout);
        addressInput = findViewById(R.id.addressInput);
        cityLayout = findViewById(R.id.cityLayout);
        cityInput = findViewById(R.id.cityInput);
        stateLayout = findViewById(R.id.stateLayout);
        stateInput = findViewById(R.id.stateInput);
        zipCodeLayout = findViewById(R.id.zipCodeLayout);
        zipCodeInput = findViewById(R.id.zipCodeInput);
        phoneLayout = findViewById(R.id.phoneLayout);
        phoneInput = findViewById(R.id.phoneInput);

        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        placeOrderButton = findViewById(R.id.placeOrderButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.checkout);
        }
    }

    private void getOrderDetailsFromIntent() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            subtotal = extras.getDouble("SUBTOTAL", 0);
            deliveryFee = extras.getDouble("DELIVERY_FEE", 0);
            tax = extras.getDouble("TAX", 0);
            total = extras.getDouble("TOTAL", 0);
        }
    }

    private void displayOrderSummary() {
        subtotalTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(subtotal)));
        deliveryFeeTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(deliveryFee)));
        taxTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(tax)));
        totalTextView.setText(getString(R.string.currency_format,
                getString(R.string.currency_symbol), currencyFormat.format(total)));
    }

    private void setupListeners() {
        placeOrderButton.setOnClickListener(v -> validateAndPlaceOrder());
    }

    private void fetchUserDetails() {
        showLoading(true);
        usersReference.child(USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                showLoading(false);
                if (snapshot.exists()) {
                    firstName = snapshot.child("firstName").getValue(String.class);
                    lastName = snapshot.child("lastName").getValue(String.class);
                    address = snapshot.child("address").getValue(String.class);
                    city = snapshot.child("city").getValue(String.class);
                    state = snapshot.child("state").getValue(String.class);
                    zipCode = snapshot.child("zipCode").getValue(String.class);
                    phone = snapshot.child("phone").getValue(String.class);
                    email = snapshot.child("email").getValue(String.class);
                    Boolean isBlocked = snapshot.child("isBlocked").getValue(Boolean.class);



                    // Populate UI with fetched data
                    if (firstName != null && lastName != null) {
                        nameInput.setText(firstName + " " + lastName);
                    }
                    if (address != null) {
                        addressInput.setText(address);
                    }
                    if (city != null) {
                        cityInput.setText(city);
                    }
                    if (state != null) {
                        stateInput.setText(state);
                    }
                    if (zipCode != null) {
                        zipCodeInput.setText(zipCode);
                    }
                    if (phone != null) {
                        phoneInput.setText(phone);
                    }


                    if (isBlocked != null && isBlocked) {
                        placeOrderButton.setEnabled(false);
                        Toast.makeText(CheckoutActivity.this, "Your account is blocked. Cannot place order.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(CheckoutActivity.this, "User not found in Firebase", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showLoading(false);
                Toast.makeText(CheckoutActivity.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndPlaceOrder() {
        // Reset errors
        nameLayout.setError(null);
        addressLayout.setError(null);
        cityLayout.setError(null);
        stateLayout.setError(null);
        zipCodeLayout.setError(null);
        phoneLayout.setError(null);

        // Get input values
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String city = cityInput.getText().toString().trim();
        String state = stateInput.getText().toString().trim();
        String zipCode = zipCodeInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        // Validate inputs
        boolean isValid = true;

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (address.isEmpty()) {
            addressLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (city.isEmpty()) {
            cityLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (state.isEmpty()) {
            stateLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        }

        if (zipCode.isEmpty()) {
            zipCodeLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!zipCode.matches("\\d{5}(-\\d{4})?")) {
            zipCodeLayout.setError(getString(R.string.error_invalid_zip));
            isValid = false;
        }

        if (phone.isEmpty()) {
            phoneLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!phone.matches("\\d{10}")) {
            phoneLayout.setError(getString(R.string.error_invalid_phone));
            isValid = false;
        }

        // Check if payment method is selected
        int selectedPaymentId = paymentMethodGroup.getCheckedRadioButtonId();
        if (selectedPaymentId == -1) {
            Toast.makeText(this, R.string.error_select_payment, Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // If all inputs are valid, place order
        if (isValid) {
            // Get selected payment method
            RadioButton selectedPayment = findViewById(selectedPaymentId);
            String paymentMethod = selectedPayment.getText().toString();


            placeOrder(name, address, city, state, zipCode, phone, paymentMethod);
        }
    }

//    private void placeOrder(String name, String address, String city, String state,
//                            String zipCode, String phone, String paymentMethod) {
//        showLoading(true);
//
//        // In a real app, you would create an order in the database here
//        // For this example, we'll just clear the cart and show a success message
//
//        cartRepository.clearCart(new FirebaseCartRepository.CartOperationCallback() {
//            @Override
//            public void onComplete(boolean success, String message) {
//                showLoading(false);
//
//                if (success) {
//                    showOrderConfirmationDialog();
//                } else {
//                    Toast.makeText(CheckoutActivity.this,
//                            getString(R.string.error_processing_order, message),
//                            Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }

    private void placeOrder(String name, String address, String city, String state,
                            String zipCode, String phone, String paymentMethod) {
        showLoading(true);

        switch (paymentMethod.toLowerCase()) {
            case "cash on delivery":
                savePaymentDetails("cash on delivery", name, address, city, state, zipCode, phone);
                break;

            case "credit card":
                processCreditCardPayment(name, address, city, state, zipCode, phone);
                break;

            default:
                showLoading(false);
                Toast.makeText(CheckoutActivity.this, "Unsupported payment method: " + paymentMethod, Toast.LENGTH_SHORT).show();
                break;
        }
    }
    private void processCreditCardPayment(String name, String address, String city, String state, String zipCode, String phone) {

        InitRequest req = new InitRequest();
        req.setMerchantId("1221688");       // Merchant ID
        req.setCurrency("USD");             // Currency code LKR/USD/GBP/EUR/AUD
        req.setAmount(total);             // Final Amount to be charged
        req.setOrderId(UUID.randomUUID().toString());        // Unique Reference ID
        req.setItemsDescription("Product Payment");  // Item description title

        req.getCustomer().setFirstName(firstName);
        req.getCustomer().setLastName(lastName);
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(phone);
        req.getCustomer().getAddress().setAddress(address);
        req.getCustomer().getAddress().setCity(city);
        req.getCustomer().getAddress().setCountry("Sri Lanka");


        Intent intent = new Intent(this, PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
        startActivityForResult(intent, PAYHERE_REQUEST); //unique request ID e.g. "11001"

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PAYHERE_REQUEST && data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

            String msg;
            if (response == null) {
                msg = "Result: No response";
            } else {
                if (resultCode == Activity.RESULT_OK) {
                    if (response.isSuccess()) {
                        msg = "Payment Successful: " + response.getData().toString();
                        // Pass user details and payment method to savePaymentDetails
                        String name = nameInput.getText().toString().trim();
                        String address = addressInput.getText().toString().trim();
                        String city = cityInput.getText().toString().trim();
                        String state = stateInput.getText().toString().trim();
                        String zipCode = zipCodeInput.getText().toString().trim();
                        String phone = phoneInput.getText().toString().trim();
                        savePaymentDetails("credit card", name, address, city, state, zipCode, phone);
                    } else {
                        msg = "Payment Failed: " + response.toString();
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    msg = "User canceled the payment request";
                } else {
                    msg = "Unexpected result code: " + resultCode;
                }
            }

            Log.i(TAG, msg);
            // Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }
    private void savePaymentDetails(String paymentMethod, String name, String address, String city, String state, String zipCode, String phone) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                .getReference("Orders");
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("Carts").child(USER_ID);
        DatabaseReference productsRef = FirebaseDatabase.getInstance()
                .getReference("Products");

        String orderId = ordersRef.push().getKey();

        // Show loading indicator
        showLoading(true);

        // Fetch cart items to include in the order
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CartItem> cartItems = new ArrayList<>();
                Map<String, Object> productsMap = new HashMap<>();

                // Populate cart items and products map
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item != null) {
                        item.setId(itemSnapshot.getKey());
                        cartItems.add(item);
                        Log.d("CheckoutActivity", "Cart item loaded: " + item.getId() + ", Quantity: " + item.getQuantity());

                        // Add product details to the products map, including deliveryStatus
                        Map<String, Object> productDetails = new HashMap<>();
                        productDetails.put("title", item.getTitle());
                        productDetails.put("price", item.getPrice());
                        productDetails.put("quantity", item.getQuantity());
                        productDetails.put("deliveryFee", item.getDeliveryFee());
                        productDetails.put("imageUrl", item.getImageUrl());
                        productDetails.put("sellerId", item.getSellerId());
                        productDetails.put("sellerName", item.getSellerName());
                        productDetails.put("deliveryStatus", "Pending"); // Initial delivery status
                        productsMap.put(item.getId(), productDetails);
                    }
                }

                if (cartItems.isEmpty()) {
                    showLoading(false);
                    Log.w("CheckoutActivity", "Cart is empty for user: " + USER_ID);
                    showOrderConfirmationDialog();
                    return;
                }

                // Prepare order data with products
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderId", orderId);
                orderData.put("userId", USER_ID);
                orderData.put("paymentMethod", paymentMethod); // Use the passed payment method
                orderData.put("amount", total);
                orderData.put("status", "Pending");
                orderData.put("address", address);
                orderData.put("city", city);
                orderData.put("state", state);
                orderData.put("zipCode", zipCode);
                orderData.put("phone", phone);
                orderData.put("timestamp", System.currentTimeMillis());
                orderData.put("products", productsMap); // Add purchased products with deliveryStatus

                // Save order to Firebase
                if (orderId != null) {
                    ordersRef.child(orderId).setValue(orderData)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("CheckoutActivity", "Order saved successfully with products: " + orderId);
                                    // Update product quantities and clear cart
                                    updateProductQuantities(productsRef, cartItems);
                                } else {
                                    showLoading(false);
                                    Log.e("CheckoutActivity", "Failed to save order: " + task.getException().getMessage());
                                    Toast.makeText(CheckoutActivity.this, "Error saving order", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e("CheckoutActivity", "Failed to fetch cart items: " + error.getMessage());
                Toast.makeText(CheckoutActivity.this, "Error fetching cart", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateProductQuantities(DatabaseReference productsRef, List<CartItem> cartItems) {
        Log.d("CheckoutActivity", "Updating quantities for " + cartItems.size() + " cart items");

        for (CartItem cartItem : cartItems) {
            String productId = cartItem.getId();
            int purchasedQuantity = cartItem.getQuantity();
            Log.d("CheckoutActivity", "Processing cart item - Product ID: " + productId + ", Quantity: " + purchasedQuantity);

            // Search for the product across all categories
            productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean productFound = false;
                    for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        DataSnapshot productSnapshot = categorySnapshot.child(productId);
                        if (productSnapshot.exists()) {
                            productFound = true;
                            String category = categorySnapshot.getKey();
                            Log.d("CheckoutActivity", "Found product " + productId + " in category " + category);

                            // Use a transaction to update the quantity atomically
                            DatabaseReference productQuantityRef = productsRef.child(category).child(productId).child("quantity");
                            productQuantityRef.runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    Integer currentQuantity = currentData.getValue(Integer.class);
                                    if (currentQuantity == null) {
                                        Log.w("CheckoutActivity", "Quantity field missing or null for product: " + productId);
                                        return Transaction.success(currentData);
                                    }

                                    int newQuantity = currentQuantity - purchasedQuantity;
                                    if (newQuantity < 0) {
                                        newQuantity = 0;
                                        Log.w("CheckoutActivity", "Product " + productId + " quantity would go below 0, setting to 0");
                                    }
                                    currentData.setValue(newQuantity);
                                    Log.d("CheckoutActivity", "Transaction updating " + productId + " from " + currentQuantity + " to " + newQuantity);
                                    return Transaction.success(currentData);
                                }

                                @Override
                                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                                    if (error != null) {
                                        Log.e("CheckoutActivity", "Transaction failed for " + productId + ": " + error.getMessage());
                                    } else if (committed) {
                                        Log.d("CheckoutActivity", "Transaction committed for " + productId + ", new quantity: " + snapshot.getValue());
                                    } else {
                                        Log.w("CheckoutActivity", "Transaction not committed for " + productId);
                                    }
                                }
                            });
                            break; // Exit loop once product is found
                        }
                    }
                    if (!productFound) {
                        Log.w("CheckoutActivity", "Product not found in Products node: " + productId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("CheckoutActivity", "Failed to fetch products: " + error.getMessage());
                }
            });
        }

        // Clear cart after initiating all updates (not waiting for completion)
        clearCartAfterOrder();
        showLoading(false);
    }

    private void clearCartAfterOrder() {
        cartRepository.clearCart(new FirebaseCartRepository.CartOperationCallback() {
            @Override
            public void onComplete(boolean success, String message) {
                if (success) {
                    Log.d("CheckoutActivity", "Cart cleared successfully after order");
                    showOrderConfirmationDialog(); // Show confirmation after all operations
                } else {
                    Log.e("CheckoutActivity", "Failed to clear cart: " + message);
                    Toast.makeText(CheckoutActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showOrderConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.order_placed)
                .setMessage(R.string.order_confirmation_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    Intent intent = new Intent(CheckoutActivity.this, ViewMyOrdersActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
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