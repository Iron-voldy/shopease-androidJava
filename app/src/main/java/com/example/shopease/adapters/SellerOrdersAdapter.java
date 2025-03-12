package com.example.shopease.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shopease.R;
import com.example.shopease.models.SellerOrderItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerOrdersAdapter extends RecyclerView.Adapter<SellerOrdersAdapter.OrderViewHolder> {
    private static final String TAG = "SellerOrdersAdapter";

    private final List<SellerOrderItem> orderItems;
    private final OnStatusUpdateListener statusUpdateListener;
    private final Context context;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    public interface OnStatusUpdateListener {
        void onStatusUpdate(String orderId, String productId, String newStatus);
    }

    public SellerOrdersAdapter(Context context, List<SellerOrderItem> orderItems, OnStatusUpdateListener listener) {
        this.context = context;
        this.orderItems = orderItems != null ? orderItems : new ArrayList<>();
        this.statusUpdateListener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seller_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        SellerOrderItem item = orderItems.get(position);

        // Set order header information
        holder.orderIdText.setText("Order #" + item.getOrderId());
        if (item.getOrderDate() != null) {
            holder.orderDateText.setText(dateFormat.format(item.getOrderDate()));
        } else {
            holder.orderDateText.setText("Processing");
        }

        // Set product details
        holder.productTitle.setText(item.getTitle());
        holder.orderQuantity.setText("Qty: " + item.getQuantity());
        holder.orderPrice.setText(currencyFormat.format(item.getTotalPrice()));

        // Set customer info
        holder.customerInfo.setText("Customer: " + item.getBuyerName());

        // Set payment method
        String paymentMethod = item.getPaymentMethod();
        if (paymentMethod != null && !paymentMethod.isEmpty()) {
            holder.paymentMethodText.setText("Payment: " + paymentMethod);
            holder.paymentMethodText.setVisibility(View.VISIBLE);
        } else {
            holder.paymentMethodText.setVisibility(View.GONE);
        }

        // Load product image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .into(holder.productImage);
        }

        // Setup delivery status dropdown
        setupStatusDropdown(holder, item);

        // Set button click listener
        holder.updateStatusBtn.setOnClickListener(v -> {
            String newStatus = holder.statusDropdown.getText().toString();
            if (!newStatus.equals(item.getDeliveryStatus())) {
                statusUpdateListener.onStatusUpdate(
                        item.getOrderId(),
                        item.getProductId(),
                        newStatus
                );
            }
        });
    }

    private void setupStatusDropdown(OrderViewHolder holder, SellerOrderItem item) {
        String[] statusOptions = {
                "Processing",
                "Packed",
                "Shipped",
                "Delivered"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_dropdown_item_1line,
                statusOptions
        );

        holder.statusDropdown.setAdapter(adapter);

        // Set current status
        if (item.getDeliveryStatus() != null) {
            holder.statusDropdown.setText(item.getDeliveryStatus(), false);
        } else {
            holder.statusDropdown.setText(statusOptions[0], false);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public void updateData(List<SellerOrderItem> newItems) {
        this.orderItems.clear();
        if (newItems != null) {
            this.orderItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addItem(SellerOrderItem item) {
        if (!orderItems.contains(item)) {
            orderItems.add(item);
            notifyItemInserted(orderItems.size() - 1);
        }
    }

    public void removeItem(SellerOrderItem item) {
        int position = orderItems.indexOf(item);
        if (position > -1) {
            orderItems.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText;
        TextView orderDateText;
        ImageView productImage;
        TextView productTitle;
        TextView orderQuantity;
        TextView orderPrice;
        TextView customerInfo;
        TextView paymentMethodText;
        TextInputLayout statusContainer;
        AutoCompleteTextView statusDropdown;
        MaterialButton updateStatusBtn;

        OrderViewHolder(View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.orderIdText);
            orderDateText = itemView.findViewById(R.id.orderDateText);
            productImage = itemView.findViewById(R.id.productImage);
            productTitle = itemView.findViewById(R.id.productTitle);
            orderQuantity = itemView.findViewById(R.id.orderQuantity);
            orderPrice = itemView.findViewById(R.id.orderPrice);
            customerInfo = itemView.findViewById(R.id.customerInfo);
            paymentMethodText = itemView.findViewById(R.id.paymentMethodText);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            statusDropdown = itemView.findViewById(R.id.statusDropdown);
            updateStatusBtn = itemView.findViewById(R.id.updateStatusBtn);
        }
    }
}