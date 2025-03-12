package com.example.shopease.repository;

import androidx.annotation.NonNull;

import com.example.shopease.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseRepository {

    private final DatabaseReference databaseReference;

    public FirebaseRepository() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference("Products");
    }

    public void fetchProducts(final FirebaseCallback callback) {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> products = new ArrayList<>();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String category = categorySnapshot.getKey();
                    if (category == null) {
                        category = "";
                    }
                    for (DataSnapshot productSnapshot : categorySnapshot.getChildren()) {
                        Product product = productSnapshot.getValue(Product.class);
                        if (product != null) {
                            product.setId(productSnapshot.getKey() != null ? productSnapshot.getKey() : "");
                            product.setCategory(category);
                            products.add(product);
                        }
                    }
                }
                callback.onCallback(products);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors
            }
        });
    }

    public interface FirebaseCallback {
        void onCallback(List<Product> productList);
    }
}
