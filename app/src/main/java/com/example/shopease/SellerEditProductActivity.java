package com.example.shopease;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SellerEditProductActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "SellerEditProduct";

    // UI elements
    private ImageView editProductImage;
    private TextInputEditText editProductTitle, editProductDescription, editProductPrice, editProductQuantity, editDeliveryFee;
    private AutoCompleteTextView editProductCategory;
    private MaterialButton saveProductChanges, uploadImageButton;
    private ProgressBar loadingProgressBar;

    // Firebase and Cloudinary
    private DatabaseReference productsRef;
    private Cloudinary cloudinary;

    // Product data
    private String productId, category;
    private String originalImageUrl;
    private Uri newImageUri;
    private boolean imageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_edit_product);

        setupToolbar();
        initializeViews();
        setupCategoryDropdown();
        initializeCloudinary();

        // Retrieve product details from intent
        productId = getIntent().getStringExtra("productId");
        category = getIntent().getStringExtra("category");

        if (productId == null || category == null) {
            Toast.makeText(this, "Product information missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase reference
        productsRef = FirebaseDatabase.getInstance()
                .getReference("Products").child(category).child(productId);

        // Load product data
        loadProductData();

        // Set up button click listeners
        uploadImageButton.setOnClickListener(v -> openImageChooser());
        saveProductChanges.setOnClickListener(v -> updateProductDetails());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Product");
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

    private void initializeViews() {
        editProductImage = findViewById(R.id.editProductImage);
        editProductTitle = findViewById(R.id.editProductTitle);
        editProductCategory = findViewById(R.id.editProductCategory);
        editProductDescription = findViewById(R.id.editProductDescription);
        editProductPrice = findViewById(R.id.editProductPrice);
        editProductQuantity = findViewById(R.id.editProductQuantity);
        editDeliveryFee = findViewById(R.id.editDeliveryFee);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        saveProductChanges = findViewById(R.id.saveProductChanges);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Eco-Friendly", "Smart Devices", "Subscription Boxes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        editProductCategory.setAdapter(adapter);
        editProductCategory.setThreshold(1);
        editProductCategory.setOnClickListener(v -> editProductCategory.showDropDown());
    }

    private void initializeCloudinary() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "du35enier",
                "api_key", "724985657511325",
                "api_secret", "yvROS8927ehK-xyYNHmRfRMZIwY"
        ));
    }

    private void loadProductData() {
        loadingProgressBar.setVisibility(View.VISIBLE);

        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> productData = (Map<String, Object>) dataSnapshot.getValue();

                    if (productData != null) {
                        // Set product details to views
                        editProductTitle.setText(String.valueOf(productData.get("title")));
                        editProductDescription.setText(String.valueOf(productData.get("description")));
                        editProductCategory.setText(String.valueOf(productData.get("category")));

                        // Handle numeric values
                        if (productData.get("price") != null) {
                            editProductPrice.setText(String.valueOf(productData.get("price")));
                        }

                        if (productData.get("quantity") != null) {
                            editProductQuantity.setText(String.valueOf(productData.get("quantity")));
                        }

                        if (productData.get("deliveryFee") != null) {
                            editDeliveryFee.setText(String.valueOf(productData.get("deliveryFee")));
                        }

                        // Load image
                        originalImageUrl = String.valueOf(productData.get("imageUrl"));
                        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                            Glide.with(SellerEditProductActivity.this)
                                    .load(originalImageUrl)
                                    .into(editProductImage);
                        }
                    }
                } else {
                    Toast.makeText(SellerEditProductActivity.this, "Product not found", Toast.LENGTH_SHORT).show();
                    finish();
                }

                loadingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading product data: " + databaseError.getMessage());
                Toast.makeText(SellerEditProductActivity.this, "Failed to load product data", Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Product Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            newImageUri = data.getData();
            editProductImage.setImageURI(newImageUri);
            imageChanged = true;
        }
    }

    private void updateProductDetails() {
        // Validate inputs
        if (editProductTitle.getText().toString().trim().isEmpty() ||
                editProductCategory.getText().toString().trim().isEmpty() ||
                editProductDescription.getText().toString().trim().isEmpty() ||
                editProductPrice.getText().toString().trim().isEmpty() ||
                editProductQuantity.getText().toString().trim().isEmpty() ||
                editDeliveryFee.getText().toString().trim().isEmpty()) {

            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);

        // Get values from fields
        String newTitle = editProductTitle.getText().toString().trim();
        String newCategory = editProductCategory.getText().toString().trim();
        String newDescription = editProductDescription.getText().toString().trim();
        double newPrice = Double.parseDouble(editProductPrice.getText().toString().trim());
        int newQuantity = Integer.parseInt(editProductQuantity.getText().toString().trim());
        double newDeliveryFee = Double.parseDouble(editDeliveryFee.getText().toString().trim());

        // Prepare product data map
        Map<String, Object> updatedProductData = new HashMap<>();
        updatedProductData.put("title", newTitle);
        updatedProductData.put("category", newCategory);
        updatedProductData.put("description", newDescription);
        updatedProductData.put("price", newPrice);
        updatedProductData.put("quantity", newQuantity);
        updatedProductData.put("deliveryFee", newDeliveryFee);

        // If category changed, need to move the product to new category node
        final boolean categoryChanged = !newCategory.equals(category);
        final DatabaseReference newCategoryRef = categoryChanged ?
                FirebaseDatabase.getInstance()
                        .getReference("Products").child(newCategory).child(productId)
                : null;

        // If image changed, upload new image first
        if (imageChanged && newImageUri != null) {
            new Thread(() -> {
                try {
                    // Upload new image to Cloudinary
                    Map uploadResult = cloudinary.uploader().upload(
                            new File(getRealPathFromURI(newImageUri)),
                            ObjectUtils.emptyMap()
                    );
                    String newImageUrl = uploadResult.get("secure_url").toString();
                    updatedProductData.put("imageUrl", newImageUrl);

                    // Now update the product data
                    runOnUiThread(() -> finishProductUpdate(updatedProductData, categoryChanged, newCategoryRef));

                } catch (IOException e) {
                    Log.e(TAG, "Image upload failed", e);
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(SellerEditProductActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        } else {
            // No image change, just update product data
            finishProductUpdate(updatedProductData, categoryChanged, newCategoryRef);
        }
    }

    private void finishProductUpdate(Map<String, Object> updatedProductData, boolean categoryChanged, DatabaseReference newCategoryRef) {
        if (categoryChanged) {
            // If category changed, first copy to new location
            newCategoryRef.setValue(updatedProductData)
                    .addOnSuccessListener(aVoid -> {
                        // Then delete from old location
                        productsRef.removeValue()
                                .addOnSuccessListener(aVoid1 -> {
                                    loadingProgressBar.setVisibility(View.GONE);
                                    Toast.makeText(SellerEditProductActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    loadingProgressBar.setVisibility(View.GONE);
                                    Log.e(TAG, "Failed to delete product from old category", e);
                                    Toast.makeText(SellerEditProductActivity.this, "Product updated but could not remove from old category", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Failed to update product in new category", e);
                        Toast.makeText(SellerEditProductActivity.this, "Failed to update product", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No category change, just update in place
            productsRef.updateChildren(updatedProductData)
                    .addOnSuccessListener(aVoid -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(SellerEditProductActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Failed to update product", e);
                        Toast.makeText(SellerEditProductActivity.this, "Failed to update product", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) return uri.getPath();
        cursor.moveToFirst();
        int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        String filePath = (index != -1) ? cursor.getString(index) : null;
        cursor.close();
        Log.d(TAG, "Real file path: " + filePath);
        return filePath;
    }
}