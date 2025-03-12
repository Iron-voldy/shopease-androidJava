package com.example.shopease.models;

public class SellerProduct {
    private String productId;
    private String sellerId;
    private String title;
    private String description;
    private double price;
    private String imageUrl;
    private String category;

    public SellerProduct() {
        // Default constructor required for Firebase
    }

    public SellerProduct(String productId, String sellerId, String title, String description, double price, String imageUrl, String category) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    // Getter Methods
    public String getProductId() { return productId; }
    public String getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getimageUrl() { return imageUrl; }
    public String getCategory() { return category; }

    // Setter Methods (Fixes your issue)
    public void setProductId(String productId) { this.productId = productId; }
    public void setCategory(String category) { this.category = category; }
}
