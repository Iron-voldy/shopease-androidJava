package com.example.shopease.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shopease.R;
import com.example.shopease.models.SellerProduct;

import java.util.List;

public class SellerProductsAdapter extends RecyclerView.Adapter<SellerProductsAdapter.ProductViewHolder> {
    private List<SellerProduct> productList;
    private OnProductEditListener editListener;
    private OnProductDeleteListener deleteListener;

    public interface OnProductEditListener {
        void onEdit(SellerProduct product);
    }

    public interface OnProductDeleteListener {
        void onDelete(String productId, String category);
    }

    public SellerProductsAdapter(List<SellerProduct> productList, OnProductEditListener editListener, OnProductDeleteListener deleteListener) {
        this.productList = productList;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        SellerProduct product = productList.get(position);
        holder.productTitle.setText(product.getTitle());
        holder.productDescription.setText(product.getDescription());
        holder.productPrice.setText("Price: $" + product.getPrice());

        // Use Glide to load image from URL into ImageView
        Glide.with(holder.productImage.getContext())
                .load(product.getimageUrl()) // URL from Firebase
                .into(holder.productImage);

        holder.editProductBtn.setOnClickListener(v -> editListener.onEdit(product));
        holder.deleteProductBtn.setOnClickListener(v -> deleteListener.onDelete(product.getProductId(), product.getCategory()));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productTitle, productDescription, productPrice;
        ImageView productImage;
        Button editProductBtn, deleteProductBtn;

        ProductViewHolder(View itemView) {
            super(itemView);
            productTitle = itemView.findViewById(R.id.sellerProductTitle);
            productDescription = itemView.findViewById(R.id.sellerProductDescription);
            productPrice = itemView.findViewById(R.id.sellerProductPrice);
            productImage = itemView.findViewById(R.id.sellerProductImage);
            editProductBtn = itemView.findViewById(R.id.sellerEditProductBtn);
            deleteProductBtn = itemView.findViewById(R.id.sellerDeleteProductBtn);
        }
    }
}
