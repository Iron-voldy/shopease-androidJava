package com.example.shopease.models;

import java.io.Serializable;

public class OrderProductModel implements Serializable {
    private String productId;
    private String title;
    private double price;
    private int quantity;
    private String imageUrl;
    private String sellerId;
    private String sellerName;
    private Double deliveryFee;
    private String deliveryStatus;

    // Empty constructor for Firebase
    public OrderProductModel() {
    }

    public OrderProductModel(String productId, String title, double price, int quantity,
                             String imageUrl, String sellerId, String sellerName,
                             Double deliveryFee, String deliveryStatus) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.deliveryFee = deliveryFee;
        this.deliveryStatus = deliveryStatus;
    }

    // Getters and setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public Double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    // Helper method to calculate subtotal
    public double getSubtotal() {
        return price * quantity;
    }

    // Helper method to calculate total with delivery
    public double getTotal() {
        return getSubtotal() + (deliveryFee != null ? deliveryFee : 0);
    }
}