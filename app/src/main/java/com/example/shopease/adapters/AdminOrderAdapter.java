package com.example.shopease.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.R;
import com.example.shopease.models.OrderModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder> {

    private final List<OrderModel> orderList;
    private final OrderActionListener listener;
    private final NumberFormat currencyFormat;
    private final SimpleDateFormat dateFormat;

    public interface OrderActionListener {
        void onViewOrderDetails(OrderModel order);
        void onUpdateOrderStatus(OrderModel order, String newStatus);
    }

    public AdminOrderAdapter(List<OrderModel> orderList, OrderActionListener listener) {
        this.orderList = orderList;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.currencyFormat.setCurrency(Currency.getInstance("USD"));
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderModel order = orderList.get(position);

        // Set order details
        holder.orderIdTextView.setText(order.getOrderId());
        holder.dateTextView.setText(dateFormat.format(new Date(order.getTimestamp())));
        holder.amountTextView.setText(currencyFormat.format(order.getAmount()));

        // Set status with appropriate color
        holder.statusTextView.setText(order.getStatus());
        int statusColor;
        switch (order.getStatus()) {
            case "Pending":
                statusColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "Processing":
                statusColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_blue_dark);
                break;
            case "Delivered":
                statusColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "Cancelled":
                statusColor = holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                statusColor = holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray);
                break;
        }
        holder.statusTextView.setTextColor(statusColor);

        // Count products in order
        int productCount = order.getProducts().size();
        holder.productCountTextView.setText(productCount + " " + (productCount == 1 ? "item" : "items"));

        // Configure action buttons based on current status
        configureActionButtons(holder, order);

        // Setup item click listener for viewing details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewOrderDetails(order);
            }
        });
    }

    private void configureActionButtons(OrderViewHolder holder, OrderModel order) {
        holder.statusUpdateButton.setVisibility(View.VISIBLE);

        switch (order.getStatus()) {
            case "Pending":
                holder.statusUpdateButton.setText("Process Order");
                holder.statusUpdateButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUpdateOrderStatus(order, "Processing");
                    }
                });
                break;

            case "Processing":
                holder.statusUpdateButton.setText("Mark Delivered");
                holder.statusUpdateButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUpdateOrderStatus(order, "Delivered");
                    }
                });
                break;

            case "Delivered":
                // No more actions needed
                holder.statusUpdateButton.setVisibility(View.GONE);
                break;

            case "Cancelled":
                // No more actions needed
                holder.statusUpdateButton.setVisibility(View.GONE);
                break;

            default:
                holder.statusUpdateButton.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdTextView;
        TextView dateTextView;
        TextView statusTextView;
        TextView amountTextView;
        TextView productCountTextView;
        Button statusUpdateButton;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdTextView = itemView.findViewById(R.id.orderIdTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            productCountTextView = itemView.findViewById(R.id.productCountTextView);
            statusUpdateButton = itemView.findViewById(R.id.statusUpdateButton);
        }
    }
}