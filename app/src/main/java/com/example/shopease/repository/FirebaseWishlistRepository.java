package com.example.shopease.repository;

import androidx.annotation.NonNull;


import com.example.shopease.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseWishlistRepository {

    private final DatabaseReference wishlistRef;
    private final String userId;

    public FirebaseWishlistRepository(String userId) {
        this.userId = userId;
        wishlistRef = FirebaseDatabase.getInstance()
                .getReference("Wishlists");
    }

    public void addToWishlist(Product product, WishlistOperationCallback callback) {
        if (product == null || product.getId() == null) {
            callback.onComplete(false, "Invalid product or product ID");
            return;
        }

        wishlistRef.child(userId).child(product.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onComplete(true, "Product already in wishlist");
                } else {
                    wishlistRef.child(userId).child(product.getId()).setValue(product)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    callback.onComplete(true, "Added to wishlist successfully");
                                } else {
                                    callback.onComplete(false, "Failed to add to wishlist");
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(false, "Database error: " + error.getMessage());
            }
        });
    }

    public void removeFromWishlist(String productId, WishlistOperationCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onComplete(false, "Invalid product ID");
            return;
        }

        wishlistRef.child(userId).child(productId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, "Removed from wishlist successfully");
                    } else {
                        callback.onComplete(false, "Failed to remove from wishlist");
                    }
                });
    }

    public void clearWishlist(WishlistOperationCallback callback) {
        wishlistRef.child(userId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, "Wishlist cleared successfully");
                    } else {
                        callback.onComplete(false, "Failed to clear wishlist");
                    }
                });
    }

    public void getWishlistItemCount(WishlistCountCallback callback) {
        wishlistRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                callback.onCount((int) count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onCount(0);
            }
        });
    }

    public void checkIfProductInWishlist(String productId, WishlistCheckCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onResult(false);
            return;
        }

        wishlistRef.child(userId).child(productId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onResult(snapshot.exists());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onResult(false);
            }
        });
    }

    public interface WishlistOperationCallback {
        void onComplete(boolean success, String message);
    }

    public interface WishlistCountCallback {
        void onCount(int count);
    }

    public interface WishlistCheckCallback {
        void onResult(boolean isInWishlist);
    }
}