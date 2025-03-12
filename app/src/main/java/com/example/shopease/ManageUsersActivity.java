package com.example.shopease;

import android.content.DialogInterface;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.shopease.adapters.UserAdapter;
import com.example.shopease.models.UserModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity implements UserAdapter.UserActionListener {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout tabLayout;

    private DatabaseReference usersRef;
    private List<UserModel> userList;
    private List<UserModel> filteredUserList;
    private String currentFilter = "all"; // Default filter: all, admin, user, seller

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Users");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize UI components
        initViews();
        setupListeners();

        // Initialize user lists
        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();

        // Setup RecyclerView and adapter
        setupRecyclerView();

        // Load users data
        loadUsers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.usersRecyclerView);
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
                filterUsers(s.toString());
            }
        });

        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadUsers);

        // Tab selection for filtering
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "admin";
                        break;
                    case 2:
                        currentFilter = "user";
                        break;
                    case 3:
                        currentFilter = "seller";
                        break;
                }
                filterUsers(searchEditText.getText().toString());
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
        userAdapter = new UserAdapter(filteredUserList, this);
        recyclerView.setAdapter(userAdapter);
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String firstName = userSnapshot.child("firstName").getValue(String.class);
                    String lastName = userSnapshot.child("lastName").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);
                    String phone = userSnapshot.child("phone").getValue(String.class);
                    String address = userSnapshot.child("address").getValue(String.class);
                    String city = userSnapshot.child("city").getValue(String.class);
                    String state = userSnapshot.child("state").getValue(String.class);
                    String zipCode = userSnapshot.child("zipCode").getValue(String.class);
                    String role = userSnapshot.child("role").getValue(String.class);
                    String profilePictureUrl = userSnapshot.child("profilePictureUrl").getValue(String.class);
                    Boolean isBlocked = userSnapshot.child("isBlocked").getValue(Boolean.class);

                    UserModel user = new UserModel(
                            userId, firstName, lastName, email, phone, address, city,
                            state, zipCode, role, profilePictureUrl, isBlocked != null && isBlocked
                    );

                    userList.add(user);
                }

                // Apply current filter and search
                filterUsers(searchEditText.getText().toString());

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                updateEmptyViewVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManageUsersActivity.this,
                        "Failed to load users: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                updateEmptyViewVisibility();
            }
        });
    }

    private void filterUsers(String searchText) {
        filteredUserList.clear();

        for (UserModel user : userList) {
            // Apply role filter
            if (!currentFilter.equals("all") && !user.getRole().equals(currentFilter)) {
                continue;
            }

            // Apply search filter
            String fullName = user.getFirstName() + " " + user.getLastName();
            if (searchText.isEmpty() ||
                    fullName.toLowerCase().contains(searchText.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(searchText.toLowerCase()) ||
                    user.getPhone().contains(searchText)) {
                filteredUserList.add(user);
            }
        }

        userAdapter.notifyDataSetChanged();
        updateEmptyViewVisibility();
    }

    private void updateEmptyViewVisibility() {
        if (filteredUserList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            if (searchEditText.getText().toString().isEmpty() && currentFilter.equals("all")) {
                emptyView.setText("No users found");
            } else {
                emptyView.setText("No matching users found");
            }
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBlockUser(UserModel user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = user.isBlocked() ?
                "Are you sure you want to unblock " + user.getFirstName() + "?" :
                "Are you sure you want to block " + user.getFirstName() + "?";

        builder.setTitle(user.isBlocked() ? "Unblock User" : "Block User")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Toggle blocked status in Firebase
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isBlocked", !user.isBlocked());

                    usersRef.child(user.getUserId()).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                user.setBlocked(!user.isBlocked());
                                userAdapter.notifyDataSetChanged();
                                Toast.makeText(ManageUsersActivity.this,
                                        user.isBlocked() ? "User blocked successfully" : "User unblocked successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ManageUsersActivity.this,
                                            "Failed to update user status: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onChangeRole(UserModel user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String[] roles = {"user", "seller", "admin"};

        int currentRoleIndex = 0;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(user.getRole())) {
                currentRoleIndex = i;
                break;
            }
        }

        builder.setTitle("Change User Role")
                .setSingleChoiceItems(new String[]{"User", "Seller", "Admin"}, currentRoleIndex, null)
                .setPositiveButton("Change", (dialog, which) -> {
                    int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedPosition != -1 && !roles[selectedPosition].equals(user.getRole())) {
                        // Update role in Firebase
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("role", roles[selectedPosition]);

                        usersRef.child(user.getUserId()).updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    user.setRole(roles[selectedPosition]);
                                    userAdapter.notifyDataSetChanged();
                                    Toast.makeText(ManageUsersActivity.this,
                                            "User role updated to " + roles[selectedPosition],
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(ManageUsersActivity.this,
                                                "Failed to update user role: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewUserDetails(UserModel user) {
        // Create and show a dialog with user details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);

        // Set user details in the dialog
        TextView nameTextView = dialogView.findViewById(R.id.nameTextView);
        TextView emailTextView = dialogView.findViewById(R.id.emailTextView);
        TextView phoneTextView = dialogView.findViewById(R.id.phoneTextView);
        TextView addressTextView = dialogView.findViewById(R.id.addressTextView);
        TextView roleTextView = dialogView.findViewById(R.id.roleTextView);
        TextView statusTextView = dialogView.findViewById(R.id.statusTextView);

        nameTextView.setText(user.getFirstName() + " " + user.getLastName());
        emailTextView.setText(user.getEmail());
        phoneTextView.setText(user.getPhone());

        String fullAddress = user.getAddress();
        if (user.getCity() != null && !user.getCity().isEmpty()) {
            fullAddress += ", " + user.getCity();
        }
        if (user.getState() != null && !user.getState().isEmpty()) {
            fullAddress += ", " + user.getState();
        }
        if (user.getZipCode() != null && !user.getZipCode().isEmpty()) {
            fullAddress += " " + user.getZipCode();
        }
        addressTextView.setText(fullAddress);

        roleTextView.setText(user.getRole());
        statusTextView.setText(user.isBlocked() ? "Blocked" : "Active");

        builder.setTitle("User Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
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