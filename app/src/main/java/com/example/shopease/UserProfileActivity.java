package com.example.shopease;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.shopease.models.DatabaseHelper;
import com.example.shopease.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private String USER_ID;
    private DatabaseHelper databaseHelper;
    private DatabaseReference userRef;
    private Cloudinary cloudinary;

    // UI Components
    private TextInputEditText firstNameField, lastNameField;
    private TextInputEditText addressField, cityInput, stateInput, zipCodeInput, phoneInput;
    private ImageView profilePicture;
    private MaterialButton changePictureButton, saveButton, editButton , checkOrdersButton;
    private TextInputLayout addressContainer;

    // Constants for image picking and cropping
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int UCROP_REQUEST_CODE = 2;
    private Uri imageUri;

    // Activity Result Launcher for MapActivity
    private ActivityResultLauncher<Intent> mapActivityLauncher;

    // Selected location coordinates
    private Double latitude;
    private Double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Initialize DatabaseHelper and fetch USER_ID
        databaseHelper = new DatabaseHelper(this);
        USER_ID = databaseHelper.getUserId();
        if (USER_ID == null) {
            Toast.makeText(this, "No user found in database", Toast.LENGTH_SHORT).show();
            USER_ID = "User123"; // Fallback
        }

        // Initialize Firebase reference
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(USER_ID);

        // Initialize Cloudinary
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "du35enier", // Replace with your Cloudinary cloud name
                "api_key", "724985657511325", // Replace with your API key
                "api_secret", "yvROS8927ehK-xyYNHmRfRMZIwY" // Replace with your API secret
        ));

        // Initialize UI components
        initializeViews();

        // Setup toolbar
        setupToolbar();

        // Load user profile data
        loadUserProfile();

        // Setup listeners
        setupListeners();

        // Initialize ActivityResultLauncher for MapActivity
        mapActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        latitude = data.getDoubleExtra("latitude", 0.0);
                        longitude = data.getDoubleExtra("longitude", 0.0);
                        String locationName = data.getStringExtra("locationName");
                        updateFieldsFromLocation(latitude, longitude, locationName);
                    }
                }
        );
    }

    private void initializeViews() {
        firstNameField = findViewById(R.id.firstNameField);
        lastNameField = findViewById(R.id.lastNameField);
        addressField = findViewById(R.id.addressField);
        cityInput = findViewById(R.id.cityInput);
        stateInput = findViewById(R.id.stateInput);
        zipCodeInput = findViewById(R.id.zipCodeInput);
        phoneInput = findViewById(R.id.phoneInput);
        profilePicture = findViewById(R.id.profilePicture);
        changePictureButton = findViewById(R.id.changePictureText);
        saveButton = findViewById(R.id.saveButton);
        editButton = findViewById(R.id.editButton);
        addressContainer = findViewById(R.id.addressContainer);
        checkOrdersButton = findViewById(R.id.checkOrdersButton);

        // Initially disable editing (read-only mode)
        setFieldsEditable(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }
    }

    private void setupListeners() {
        editButton.setOnClickListener(v -> setFieldsEditable(true));
        changePictureButton.setOnClickListener(v -> chooseProfilePicture());
        saveButton.setOnClickListener(v -> saveProfileChanges());
        addressContainer.setEndIconOnClickListener(v -> launchMapActivity());
        checkOrdersButton.setOnClickListener(view -> {
            Intent intent = new Intent(UserProfileActivity.this, ViewMyOrdersActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        showLoading(true);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        displayUserProfile(user);
                        // Load geopoint if available
                        Map<String, Double> geopoint = (Map<String, Double>) snapshot.child("geopoint").getValue();
                        if (geopoint != null) {
                            latitude = geopoint.get("latitude");
                            longitude = geopoint.get("longitude");
                        }
                    } else {
                        Toast.makeText(UserProfileActivity.this, "Failed to parse user data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("UserProfileActivity", "No user data found in Firebase for " + USER_ID);
                    Toast.makeText(UserProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e("UserProfileActivity", "Error fetching user data: " + error.getMessage());
                Toast.makeText(UserProfileActivity.this, "Error loading profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserProfile(User user) {
        firstNameField.setText(user.getFirstName() != null ? user.getFirstName() : "");
        lastNameField.setText(user.getLastName() != null ? user.getLastName() : "");
        addressField.setText(user.getAddress() != null ? user.getAddress() : "");
        cityInput.setText(user.getCity() != null ? user.getCity() : "");
        stateInput.setText(user.getState() != null ? user.getState() : "");
        zipCodeInput.setText(user.getZipCode() != null ? user.getZipCode() : "");
        phoneInput.setText(user.getPhone() != null ? user.getPhone() : "");

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfilePictureUrl())
                    .placeholder(R.drawable.baseline_account_circle_24)
                    .error(R.drawable.baseline_account_circle_24)
                    .into(profilePicture);
        } else {
            profilePicture.setImageResource(R.drawable.baseline_account_circle_24);
        }
    }

    private void setFieldsEditable(boolean editable) {
        firstNameField.setEnabled(editable);
        lastNameField.setEnabled(editable);
        addressField.setEnabled(editable);
        cityInput.setEnabled(editable);
        stateInput.setEnabled(editable);
        zipCodeInput.setEnabled(editable);
        phoneInput.setEnabled(editable);
        changePictureButton.setEnabled(editable);
        addressContainer.setEndIconActivated(editable); // Enable/disable location icon
        editButton.setVisibility(editable ? View.GONE : View.VISIBLE);
        saveButton.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void launchMapActivity() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        Intent intent = new Intent(this, MapActivity.class);
        mapActivityLauncher.launch(intent);
    }

    private void updateFieldsFromLocation(double latitude, double longitude, String locationName) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                addressField.setText(locationName != null ? locationName : address.getAddressLine(0));
                cityInput.setText(address.getLocality() != null ? address.getLocality() : "");
                stateInput.setText(address.getAdminArea() != null ? address.getAdminArea() : "");
                zipCodeInput.setText(address.getPostalCode() != null ? address.getPostalCode() : "");
            } else {
                addressField.setText(locationName);
                Toast.makeText(this, "Could not fetch detailed address", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            addressField.setText(locationName);
            Log.e("UserProfileActivity", "Geocoding failed: ", e);
            Toast.makeText(this, "Error fetching address details", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileChanges() {
        showLoading(true);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User currentUser = snapshot.getValue(User.class);
                    if (currentUser == null) {
                        showLoading(false);
                        Toast.makeText(UserProfileActivity.this, "Failed to load current user data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get current values
                    String currentFirstName = currentUser.getFirstName();
                    String currentLastName = currentUser.getLastName();
                    String currentAddress = currentUser.getAddress();
                    String currentCity = currentUser.getCity();
                    String currentState = currentUser.getState();
                    String currentZipCode = currentUser.getZipCode();
                    String currentPhone = currentUser.getPhone();

                    // Get updated values from UI
                    String updatedFirstName = firstNameField.getText().toString().trim();
                    String updatedLastName = lastNameField.getText().toString().trim();
                    String updatedAddress = addressField.getText().toString().trim();
                    String updatedCity = cityInput.getText().toString().trim();
                    String updatedState = stateInput.getText().toString().trim();
                    String updatedZipCode = zipCodeInput.getText().toString().trim();
                    String updatedPhone = phoneInput.getText().toString().trim();

                    // Map to hold only changed fields
                    Map<String, Object> updatedFields = new HashMap<>();

                    String phoneRegex = "^[0-9]{10}$"; // Simple 10-digit check
                    if (!updatedPhone.matches(phoneRegex) && !updatedPhone.isEmpty()) {
                        phoneInput.setError("Invalid phone number (10 digits required)");
                        showLoading(false);
                        return;
                    }

                    // Only add fields that have changed and are non-empty
                    if (!updatedFirstName.equals(currentFirstName) && !updatedFirstName.isEmpty()) {
                        updatedFields.put("firstName", updatedFirstName);
                    }
                    if (!updatedLastName.equals(currentLastName) && !updatedLastName.isEmpty()) {
                        updatedFields.put("lastName", updatedLastName);
                    }
                    if (!updatedAddress.equals(currentAddress) && !updatedAddress.isEmpty()) {
                        updatedFields.put("address", updatedAddress);
                    }
                    if (!updatedCity.equals(currentCity) && !updatedCity.isEmpty()) {
                        updatedFields.put("city", updatedCity);
                    }
                    if (!updatedState.equals(currentState) && !updatedState.isEmpty()) {
                        updatedFields.put("state", updatedState);
                    }
                    if (!updatedZipCode.equals(currentZipCode) && !updatedZipCode.isEmpty()) {
                        updatedFields.put("zipCode", updatedZipCode);
                    }
                    if (!updatedPhone.equals(currentPhone) && !updatedPhone.isEmpty()) {
                        updatedFields.put("phone", updatedPhone);
                    }

                    // Add geopoint if location was selected
                    if (latitude != null && longitude != null) {
                        Map<String, Double> geopoint = new HashMap<>();
                        geopoint.put("latitude", latitude);
                        geopoint.put("longitude", longitude);
                        updatedFields.put("geopoint", geopoint);
                    }

                    if (updatedFields.isEmpty()) {
                        showLoading(false);
                        Toast.makeText(UserProfileActivity.this, "No changes to save", Toast.LENGTH_SHORT).show();
                        setFieldsEditable(false);
                        return;
                    }

                    // Update only the changed fields in Firebase
                    userRef.updateChildren(updatedFields)
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                Toast.makeText(UserProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                setFieldsEditable(false);
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(UserProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    showLoading(false);
                    Toast.makeText(UserProfileActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Log.e("UserProfileActivity", "Error fetching user data: " + error.getMessage());
                Toast.makeText(UserProfileActivity.this, "Error loading profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Image picking and cropping methods
    private void chooseProfilePicture() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_profile.jpg"));

            UCrop.of(imageUri, destinationUri)
                    .withAspectRatio(1, 1) // Square crop
                    .withMaxResultSize(512, 512) // Max size
                    .start(this, UCROP_REQUEST_CODE);
        } else if (requestCode == UCROP_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri croppedImageUri = UCrop.getOutput(data);
            if (croppedImageUri != null) {
                Glide.with(this)
                        .load(croppedImageUri)
                        .circleCrop()
                        .into(profilePicture);
                uploadProfilePictureToCloudinary(croppedImageUri);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            Log.e("UserProfileActivity", "Crop Error: ", cropError);
            Toast.makeText(this, "Image cropping failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfilePictureToCloudinary(Uri imageUri) {
        String filePath = getRealPathFromURI(imageUri);
        if (filePath == null) {
            Log.e("Upload Error", "File path is null");
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        showLoading(true);

        new Thread(() -> {
            try {
                Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
                String imageUrl = uploadResult.get("secure_url").toString();

                runOnUiThread(() -> {
                    updateUserProfilePictureInFirebase(imageUrl);
                    showLoading(false);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Log.e("UserProfileActivity", "Upload failed: ", e);
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updateUserProfilePictureInFirebase(String imageUrl) {
        userRef.child("profilePictureUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getRealPathFromURI(Uri uri) {
        String filePath = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                if (index != -1) {
                    filePath = cursor.getString(index);
                }
                cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
            filePath = uri.getPath();
        }
        return filePath;
    }

    private void showLoading(boolean isLoading) {
        findViewById(R.id.loadingProgressBar).setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchMapActivity();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}