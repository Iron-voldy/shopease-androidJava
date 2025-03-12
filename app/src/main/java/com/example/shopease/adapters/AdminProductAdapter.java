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
import com.bumptech.glide.request.RequestOptions;
import com.example.shopease.R;
import com.example.shopease.models.ProductModel;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder> {

    private final List<ProductModel> productList;
    private final ProductActionListener listener;
    private final NumberFormat currencyFormat;

    public interface ProductActionListener {
        void onEditProduct(ProductModel product);
        void onDeleteProduct(ProductModel product);
        void onViewProductDetails(ProductModel product);
    }

    public AdminProductAdapter(List<ProductModel> productList, ProductActionListener listener) {
        this.productList = productList;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.currencyFormat.setCurrency(Currency.getInstance("USD"));
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductModel product = productList.get(position);

        // Set product details
        holder.titleTextView.setText(product.getTitle());
        holder.priceTextView.setText(currencyFormat.format(product.getPrice()));
        holder.quantityTextView.setText("Stock: " + product.getQuantity());
        holder.categoryTextView.setText(product.getCategory());

        // Load product image
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .apply(RequestOptions.centerCropTransform())
                    .placeholder(R.drawable.ic_products)
                    .error(R.drawable.ic_products)
                    .into(holder.productImageView);
        } else {
            holder.productImageView.setImageResource(R.drawable.ic_products);
        }

        // Setup click listeners
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditProduct(product);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteProduct(product);
            }
        });

        // Setup item click listener for viewing details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewProductDetails(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView titleTextView;
        TextView priceTextView;
        TextView quantityTextView;
        TextView categoryTextView;
        ImageButton editButton;
        ImageButton deleteButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            quantityTextView = itemView.findViewById(R.id.quantityTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}