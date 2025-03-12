package com.example.shopease.models;

import java.util.Map;

public class Order {
    private String orderId;
    private String userId;
    private double amount;
    private String status;
    private String paymentMethod;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private long timestamp;
    private Map<String, Product> products;

    // Default constructor required for Firebase
    public Order() {}

    public Order(String orderId, String userId, double amount, String status, String paymentMethod,
                 String address, String city, String state, String zipCode, String phone, long timestamp,
                 Map<String, Product> products) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.phone = phone;
        this.timestamp = timestamp;
        this.products = products;
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public String getPhone() { return phone; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Product> getProducts() { return products; }

    // Nested Product class
    public static class Product {
        private String title;
        private double price;
        private int quantity;
        private double deliveryFee;
        private String imageUrl;
        private String deliveryStatus;

        private String sellerId;
        private String sellerName;

        public Product() {}

        public Product(String title, double price, int quantity, double deliveryFee, String imageUrl, String deliveryStatus,String sellerId,String sellerName) {
            this.title = title;
            this.price = price;
            this.quantity = quantity;
            this.deliveryFee = deliveryFee;
            this.imageUrl = imageUrl;
            this.deliveryStatus = deliveryStatus;
            this.sellerId = sellerId;
            this.sellerName = sellerName;
        }

        public String getTitle() { return title; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public double getDeliveryFee() { return deliveryFee; }
        public String getImageUrl() { return imageUrl; }
        public String getDeliveryStatus() { return deliveryStatus; }

        public String getSellerId() {
            return sellerId;
        }

        public String getSellerName() {
            return sellerName;
        }
    }
}