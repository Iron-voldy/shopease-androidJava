package com.example.shopease.models;

import java.util.Date;

public class SellerOrderItem {
    private String orderId;
    private String productId;
    private String title;
    private String imageUrl;
    private double price;
    private int quantity;
    private String sellerId;
    private String sellerName;
    private String buyerId;
    private String buyerName;
    private String deliveryStatus;
    private double deliveryFee;
    private Date orderDate;
    private String paymentMethod;

    // No-args constructor required for Firebase
    public SellerOrderItem() {
    }

    public SellerOrderItem(String orderId, String productId, String title, String imageUrl,
                           double price, int quantity, String sellerId, String sellerName,
                           String buyerId, String buyerName, String deliveryStatus,
                           double deliveryFee, Date orderDate, String paymentMethod) {
        this.orderId = orderId;
        this.productId = productId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.deliveryStatus = deliveryStatus;
        this.deliveryFee = deliveryFee;
        this.orderDate = orderDate;
        this.paymentMethod = paymentMethod;
    }



    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // Helper method to calculate total price (product price * quantity + delivery fee)
    public double getTotalPrice() {
        return (price * quantity) + deliveryFee;
    }
}