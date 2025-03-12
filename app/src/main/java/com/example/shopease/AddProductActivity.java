package com.example.shopease;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.shopease.models.DatabaseHelper;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddProductActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView mainProductImage;
    private TextInputEditText productTitle, productDescription, productPrice, productQuantity, deliveryFee;
    private AutoCompleteTextView productCategory;
    private MaterialButton addProductButton, uploadImageButton;
    private Uri imageUri;
    private ProgressBar progressBar;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private String sellerId, sellerName;
    private Cloudinary cloudinary;

    private String USER_ID;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        setupToolbar();

        // Initialize DatabaseHelper and fetch USER_ID
        databaseHelper = new DatabaseHelper(this);


        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Products");

        // Initialize Cloudinary
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "du35enier",
                "api_key", "724985657511325",
                "api_secret", "yvROS8927ehK-xyYNHmRfRMZIwY"
        ));

        // Initialize UI elements
        initializeViews();
        setupCategoryDropdown();
        fetchSellerInfo();

        // Select Image
        uploadImageButton.setOnClickListener(v -> openImageChooser());

        // Upload Product to Firebase
        addProductButton.setOnClickListener(v -> uploadProductToFirebase());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Product");
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
        mainProductImage = findViewById(R.id.mainProductImage);
        productTitle = findViewById(R.id.productTitle);
        productCategory = findViewById(R.id.productCategory);
        productPrice = findViewById(R.id.productPrice);
        productDescription = findViewById(R.id.productDescription);
        productQuantity = findViewById(R.id.productQuantity);
        deliveryFee = findViewById(R.id.deliveryFee);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        addProductButton = findViewById(R.id.addProductButton);
        progressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Eco-Friendly", "Smart Devices", "Subscription Boxes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        productCategory.setAdapter(adapter);
        productCategory.setThreshold(1);
        productCategory.setOnClickListener(v -> productCategory.showDropDown());
    }

    private void fetchSellerInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {

            USER_ID = databaseHelper.getUserId();
            if (USER_ID == null) {
                Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
                USER_ID = "User123"; // Fallback
            }
            String fullName = databaseHelper.getFullName(USER_ID);
            if (fullName != null) {
                sellerName = fullName;
            }
            sellerId = user.getUid();

        } else {
            sellerId = "UnknownSeller";
            sellerName = "Unknown Seller";
        }
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
            imageUri = data.getData();
            mainProductImage.setImageURI(imageUri);
        }
    }

    private void uploadProductToFirebase() {
        String id = UUID.randomUUID().toString();
        String title = productTitle.getText().toString().trim();
        String category = productCategory.getText().toString().trim();
        String description = productDescription.getText().toString().trim();
        double price = Double.parseDouble(productPrice.getText().toString());
        int quantity = Integer.parseInt(productQuantity.getText().toString());
        double deliveryFeeAmount = Double.parseDouble(deliveryFee.getText().toString());

        if (title.isEmpty() || category.isEmpty() || description.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill all fields and select an image!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);

        new Thread(() -> {
            try {
                Map uploadResult = cloudinary.uploader().upload(new File(getRealPathFromURI(imageUri)), ObjectUtils.emptyMap());
                String imageUrl = uploadResult.get("secure_url").toString();
                Log.d("AddProductActivity", "Upload successful: " + imageUrl);

                Map<String, Object> productData = new HashMap<>();
                productData.put("title", title);
                productData.put("category", category);
                productData.put("description", description);
                productData.put("price", price);
                productData.put("quantity", quantity);
                productData.put("deliveryFee", deliveryFeeAmount);
                productData.put("imageUrl", imageUrl);
                productData.put("sellerId", sellerId);
                productData.put("sellerName", sellerName);

                runOnUiThread(() -> databaseReference.child(category).child(id).setValue(productData)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("AddProductActivity", "Product added successfully");
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                            Toast.makeText(this, "Product Added!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("AddProductActivity", "Failed to add product", e);
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                        }));
            } catch (IOException e) {
                Log.e("AddProductActivity", "Image upload failed", e);
                progressBar.setVisibility(ProgressBar.INVISIBLE);
            }
        }).start();
    }

    private String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) return uri.getPath();
        cursor.moveToFirst();
        int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        String filePath = (index != -1) ? cursor.getString(index) : null;
        cursor.close();
        Log.d("AddProductActivity", "Real file path: " + filePath);
        return filePath;
    }
}
