package com.example.shopease.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shopease.R;
import com.example.shopease.models.CartItem;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private final CartItemActionListener listener;

    public CartAdapter(CartItemActionListener listener) {
        this.cartItems = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
        notifyDataSetChanged();
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public double calculateSubtotal() {
        double subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getPrice() * item.getQuantity();
        }
        return subtotal;
    }

    public double calculateDeliveryFee() {
        double totalDeliveryFee = 0;
        for (CartItem item : cartItems) {
            totalDeliveryFee += item.getDeliveryFee();
        }
        return totalDeliveryFee;
    }

    public interface CartItemActionListener {
        void onRemoveItem(CartItem item);
        void onQuantityChanged(CartItem item, int newQuantity);
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle;
        private final TextView productPrice;
        private final TextView deliveryFee;
        private final TextView quantityTextView;
        private final MaterialButton decreaseButton;
        private final MaterialButton increaseButton;
        private final ImageButton removeButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productTitle = itemView.findViewById(R.id.productTitle);
            productPrice = itemView.findViewById(R.id.productPrice);
            deliveryFee = itemView.findViewById(R.id.deliveryFee);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            decreaseButton = itemView.findViewById(R.id.decreaseQuantityButton);
            increaseButton = itemView.findViewById(R.id.increaseQuantityButton);
            removeButton = itemView.findViewById(R.id.removeItemButton);
        }

        public void bind(CartItem item) {
            // Set product details
            productTitle.setText(item.getTitle());
            productPrice.setText(itemView.getContext().getString(R.string.price_format, item.getPrice()));
            deliveryFee.setText(itemView.getContext().getString(R.string.delivery_fee_format,
                    itemView.getContext().getString(R.string.currency_symbol) + item.getDeliveryFee()));
            quantityTextView.setText(String.valueOf(item.getQuantity()));

            // Load product image
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .into(productImage);
            }

            // Set click listeners
            decreaseButton.setOnClickListener(v -> {
                int currentQuantity = item.getQuantity();
                if (currentQuantity > 1) {
                    listener.onQuantityChanged(item, currentQuantity - 1);
                }
            });

            increaseButton.setOnClickListener(v -> {
                int currentQuantity = item.getQuantity();
                listener.onQuantityChanged(item, currentQuantity + 1);
            });

            removeButton.setOnClickListener(v -> listener.onRemoveItem(item));
        }
    }
}