package com.example.shopease.adapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.R;
import com.example.shopease.models.Order;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orderList;
    private final DecimalFormat currencyFormat = new DecimalFormat("0.00");

    public OrderAdapter() {
        this.orderList = new ArrayList<>();
    }

    public void setOrderList(List<Order> orderList) {
        this.orderList = orderList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.orderIdTextView.setText("Order ID: " + order.getOrderId());
        holder.totalAmountTextView.setText("Total: $" + currencyFormat.format(order.getAmount()));
        holder.statusTextView.setText("Status: " + order.getStatus());

        // Build products string
        StringBuilder productsString = new StringBuilder("Products: ");
        Map<String, Order.Product> products = order.getProducts();
        if (products != null) {
            for (Map.Entry<String, Order.Product> entry : products.entrySet()) {
                Order.Product product = entry.getValue();
                productsString.append(product.getTitle())
                        .append(" (Qty: ")
                        .append(product.getQuantity())
                        .append(", Delivery: ")
                        .append(product.getDeliveryStatus())
                        .append("), ");
            }
            // Remove trailing comma and space
            if (productsString.length() > "Products: ".length()) {
                productsString.setLength(productsString.length() - 2);
            }
        }
        holder.productsTextView.setText(productsString.toString());
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdTextView, totalAmountTextView, statusTextView, productsTextView;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdTextView = itemView.findViewById(R.id.orderIdTextView);
            totalAmountTextView = itemView.findViewById(R.id.totalAmountTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            productsTextView = itemView.findViewById(R.id.productsTextView);
        }
    }
}