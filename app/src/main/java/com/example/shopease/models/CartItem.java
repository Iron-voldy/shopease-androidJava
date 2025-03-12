package com.example.shopease.models;

import com.google.firebase.database.Exclude;

public class CartItem {
    @Exclude
    private String id;
    private String title;
    private double price;
    private String imageUrl;
    private int quantity;
    private double deliveryFee;
    private long addedTimestamp;

    private String sellerId;
    private String sellerName;

    // Empty constructor for Firebase
    public CartItem() {
        this.addedTimestamp = System.currentTimeMillis();
    }

    public CartItem(String id, String title, double price, String imageUrl, int quantity, double deliveryFee,String sellerId,String sellerName) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.deliveryFee = deliveryFee;
        this.addedTimestamp = System.currentTimeMillis();
        this.sellerId = sellerId;
        this.sellerName = sellerName;
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public long getAddedTimestamp() {
        return addedTimestamp;
    }

    public void setAddedTimestamp(long addedTimestamp) {
        this.addedTimestamp = addedTimestamp;
    }

    @Exclude
    public double getTotalPrice() {
        return price * quantity;
    }
}