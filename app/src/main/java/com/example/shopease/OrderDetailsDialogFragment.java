package com.example.shopease;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.models.OrderModel;
import com.example.shopease.models.OrderProductModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailsDialogFragment extends DialogFragment {

    private static final String ARG_ORDER = "order";

    private OrderModel order;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    public static OrderDetailsDialogFragment newInstance(OrderModel order) {
        OrderDetailsDialogFragment fragment = new OrderDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ORDER, order);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            order = (OrderModel) getArguments().getSerializable(ARG_ORDER);
        }

        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_order_details, null);

        // Set up the dialog
        setupDialog(view);

        builder.setView(view)
                .setTitle("Order Details")
                .setPositiveButton("Close", null);

        return builder.create();
    }

    private void setupDialog(View view) {
        if (order == null) return;

        // Setup TextViews
        TextView orderIdTextView = view.findViewById(R.id.orderIdTextView);
        TextView dateTextView = view.findViewById(R.id.dateTextView);
        TextView statusTextView = view.findViewById(R.id.statusTextView);
        TextView customerNameTextView = view.findViewById(R.id.customerNameTextView);
        TextView addressTextView = view.findViewById(R.id.addressTextView);
        TextView phoneTextView = view.findViewById(R.id.phoneTextView);
        TextView paymentMethodTextView = view.findViewById(R.id.paymentMethodTextView);
        TextView totalAmountTextView = view.findViewById(R.id.totalAmountTextView);

        // Set values
        orderIdTextView.setText(order.getOrderId());
        dateTextView.setText(dateFormat.format(new Date(order.getTimestamp())));
        statusTextView.setText(order.getStatus());

        // Set status color
        int statusColor;
        switch (order.getStatus()) {
            case "Pending":
                statusColor = requireContext().getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "Processing":
                statusColor = requireContext().getResources().getColor(android.R.color.holo_blue_dark);
                break;
            case "Delivered":
                statusColor = requireContext().getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "Cancelled":
                statusColor = requireContext().getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                statusColor = requireContext().getResources().getColor(android.R.color.darker_gray);
                break;
        }
        statusTextView.setTextColor(statusColor);

        // Customer name placeholder (would need to fetch from Users node)
        customerNameTextView.setText(order.getUserId());

        addressTextView.setText(order.getFullAddress());
        phoneTextView.setText(order.getPhone());
        paymentMethodTextView.setText(order.getPaymentMethod());
        totalAmountTextView.setText(currencyFormat.format(order.getAmount()));

        // Setup RecyclerView for order items
        RecyclerView productsRecyclerView = view.findViewById(R.id.productsRecyclerView);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Convert products map to list
        List<OrderProductModel> productsList = new ArrayList<>();
        for (Map.Entry<String, OrderProductModel> entry : order.getProducts().entrySet()) {
            productsList.add(entry.getValue());
        }

        OrderProductAdapter adapter = new OrderProductAdapter(productsList);
        productsRecyclerView.setAdapter(adapter);
    }

    /**
     * Adapter for order products
     */
    private class OrderProductAdapter extends RecyclerView.Adapter<OrderProductAdapter.ProductViewHolder> {

        private final List<OrderProductModel> productsList;

        public OrderProductAdapter(List<OrderProductModel> productsList) {
            this.productsList = productsList;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_product, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            OrderProductModel product = productsList.get(position);

            holder.titleTextView.setText(product.getTitle());
            holder.priceTextView.setText(currencyFormat.format(product.getPrice()));
            holder.quantityTextView.setText("x" + product.getQuantity());
            holder.subtotalTextView.setText(currencyFormat.format(product.getSubtotal()));

            if (product.getDeliveryStatus() != null) {
                holder.deliveryStatusTextView.setVisibility(View.VISIBLE);
                holder.deliveryStatusTextView.setText("Status: " + product.getDeliveryStatus());
            } else {
                holder.deliveryStatusTextView.setVisibility(View.GONE);
            }

            if (product.getDeliveryFee() != null && product.getDeliveryFee() > 0) {
                holder.deliveryFeeTextView.setVisibility(View.VISIBLE);
                holder.deliveryFeeTextView.setText("Delivery Fee: " + currencyFormat.format(product.getDeliveryFee()));
            } else {
                holder.deliveryFeeTextView.setVisibility(View.GONE);
            }

            if (product.getSellerName() != null && !product.getSellerName().isEmpty()) {
                holder.sellerTextView.setVisibility(View.VISIBLE);
                holder.sellerTextView.setText("Seller: " + product.getSellerName());
            } else {
                holder.sellerTextView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return productsList.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView priceTextView;
            TextView quantityTextView;
            TextView subtotalTextView;
            TextView deliveryStatusTextView;
            TextView deliveryFeeTextView;
            TextView sellerTextView;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                priceTextView = itemView.findViewById(R.id.priceTextView);
                quantityTextView = itemView.findViewById(R.id.quantityTextView);
                subtotalTextView = itemView.findViewById(R.id.subtotalTextView);
                deliveryStatusTextView = itemView.findViewById(R.id.deliveryStatusTextView);
                deliveryFeeTextView = itemView.findViewById(R.id.deliveryFeeTextView);
                sellerTextView = itemView.findViewById(R.id.sellerTextView);
            }
        }
    }
}