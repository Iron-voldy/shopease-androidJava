package com.example.shopease.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shopease.R;
import com.example.shopease.SalesReportActivity;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class TopProductsAdapter extends RecyclerView.Adapter<TopProductsAdapter.ProductViewHolder> {

    private final List<SalesReportActivity.ProductSalesInfo> productsList;
    private final NumberFormat currencyFormat;

    public TopProductsAdapter(List<SalesReportActivity.ProductSalesInfo> productsList) {
        this.productsList = productsList;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        this.currencyFormat.setCurrency(Currency.getInstance("USD"));
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_product, parent, false);
        return new ProductViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        SalesReportActivity.ProductSalesInfo product = productsList.get(position);

        holder.productNameTextView.setText(product.getProductName());
        holder.quantitySoldTextView.setText("Sold: " + product.getQuantitySold());
        holder.revenueTextView.setText(currencyFormat.format(product.getTotalRevenue()));

        // Load product image using Glide
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_products)
                    .error(R.drawable.ic_products)
                    .centerCrop()
                    .into(holder.productImageView);
        } else {
            holder.productImageView.setImageResource(R.drawable.ic_products);
        }
    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView productNameTextView;
        TextView quantitySoldTextView;
        TextView revenueTextView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImageView);
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
            quantitySoldTextView = itemView.findViewById(R.id.quantitySoldTextView);
            revenueTextView = itemView.findViewById(R.id.revenueTextView);
        }
    }
}