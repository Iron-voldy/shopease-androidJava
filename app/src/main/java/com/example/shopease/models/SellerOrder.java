package com.example.shopease.models;

public class SellerOrder {
    private String orderId;
    private String sellerId;
    private String buyerId;
    private String productTitle;
    private int quantity;
    private double totalPrice;
    private String status; // Pending, Shipped, Delivered

    public SellerOrder() {
        // Default constructor required for Firebase
    }

    public SellerOrder(String orderId, String sellerId, String buyerId, String productTitle, int quantity, double totalPrice, String status) {
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }
}
