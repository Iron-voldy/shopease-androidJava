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
import com.example.shopease.models.Product;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private List<Product> wishlistItems;
    private final WishlistItemActionListener listener;

    public WishlistAdapter(WishlistItemActionListener listener) {
        this.wishlistItems = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        Product item = wishlistItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return wishlistItems.size();
    }

    public void setWishlistItems(List<Product> wishlistItems) {
        this.wishlistItems = wishlistItems;
        notifyDataSetChanged();
    }

    public List<Product> getWishlistItems() {
        return new ArrayList<>(wishlistItems);
    }

    public interface WishlistItemActionListener {
        void onRemoveItem(Product item);
        void onAddToCart(Product item);
    }

    class WishlistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productTitle;
        private final TextView productPrice;
        private final TextView deliveryFee;
        private final MaterialButton addToCartButton;
        private final ImageButton removeButton;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productTitle = itemView.findViewById(R.id.productTitle);
            productPrice = itemView.findViewById(R.id.productPrice);
            deliveryFee = itemView.findViewById(R.id.deliveryFee);
            addToCartButton = itemView.findViewById(R.id.addToCartButton);
            removeButton = itemView.findViewById(R.id.removeItemButton);
        }

        public void bind(Product item) {
            productTitle.setText(item.getTitle());
            productPrice.setText(itemView.getContext().getString(R.string.price_format, item.getPrice()));
            deliveryFee.setText(itemView.getContext().getString(R.string.delivery_fee_format,
                    itemView.getContext().getString(R.string.currency_symbol) + item.getDeliveryFee()));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .into(productImage);
            }

            addToCartButton.setOnClickListener(v -> listener.onAddToCart(item));
            removeButton.setOnClickListener(v -> listener.onRemoveItem(item));
        }
    }
}