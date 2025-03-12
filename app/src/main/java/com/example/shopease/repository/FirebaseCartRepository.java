package com.example.shopease.repository;

import androidx.annotation.NonNull;

import com.example.shopease.models.CartItem;
import com.example.shopease.models.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseCartRepository {

    private final DatabaseReference cartRef;
    private final String userId;

    public FirebaseCartRepository(String userId) {
        this.userId = userId;
        cartRef = FirebaseDatabase.getInstance()
                .getReference("Carts");
    }

    public void addToCart(Product product, int quantity, CartOperationCallback callback) {
        if (product == null || product.getId() == null || quantity <= 0) {
            callback.onComplete(false, "Invalid product or quantity");
            return;
        }

        // Check if item already exists in cart
        cartRef.child(userId).child(product.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Item exists, update quantity
                    CartItem existingItem = snapshot.getValue(CartItem.class);
                    if (existingItem != null) {
                        int newQuantity = existingItem.getQuantity() + quantity;

                        // Check if new quantity exceeds available stock
                        if (newQuantity > product.getQuantity()) {
                            callback.onComplete(false, "Cannot add more items than available in stock");
                            return;
                        }

                        // Update quantity
                        existingItem.setQuantity(newQuantity);
                        cartRef.child(userId).child(product.getId()).setValue(existingItem)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        callback.onComplete(true, "Cart updated successfully");
                                    } else {
                                        callback.onComplete(false, "Failed to update cart");
                                    }
                                });
                    }
                } else {
                    // Create new cart item
                    CartItem newItem = new CartItem(
                            product.getId(),
                            product.getTitle(),
                            product.getPrice(),
                            product.getImageUrl(),
                            quantity,
                            product.getDeliveryFee(),
                            product.getSellerId(),
                            product.getSellerName()
                    );

                    cartRef.child(userId).child(product.getId()).setValue(newItem)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    callback.onComplete(true, "Added to cart successfully");
                                } else {
                                    callback.onComplete(false, "Failed to add to cart");
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

    public void removeFromCart(String productId, CartOperationCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onComplete(false, "Invalid product ID");
            return;
        }

        cartRef.child(userId).child(productId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, "Removed from cart successfully");
                    } else {
                        callback.onComplete(false, "Failed to remove from cart");
                    }
                });
    }

    public void updateCartItemQuantity(String productId, int newQuantity, CartOperationCallback callback) {
        if (productId == null || productId.isEmpty() || newQuantity <= 0) {
            callback.onComplete(false, "Invalid product ID or quantity");
            return;
        }

        cartRef.child(userId).child(productId).child("quantity").setValue(newQuantity)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, "Quantity updated successfully");
                    } else {
                        callback.onComplete(false, "Failed to update quantity");
                    }
                });
    }

    public void clearCart(CartOperationCallback callback) {
        cartRef.child(userId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(true, "Cart cleared successfully");
                    } else {
                        callback.onComplete(false, "Failed to clear cart");
                    }
                });
    }

    public void getCartItemCount(CartCountCallback callback) {
        cartRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
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

    public interface CartOperationCallback {
        void onComplete(boolean success, String message);
    }

    public interface CartCountCallback {
        void onCount(int count);
    }
}