package com.example.shopease.models;

public class ProductPerformance {
    private String productId;
    private String productTitle;
    private int totalQuantitySold;
    private double totalRevenue;

    public ProductPerformance(String productId, String productTitle) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.totalQuantitySold = 0;
        this.totalRevenue = 0.0;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public int getTotalQuantitySold() {
        return totalQuantitySold;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void addSale(int quantity, double amount) {
        this.totalQuantitySold += quantity;
        this.totalRevenue += amount;
    }

    // For display in the pie chart and sorting
    public String getShortTitle() {
        if (productTitle == null || productTitle.isEmpty()) {
            return "Unknown";
        }

        if (productTitle.length() <= 15) {
            return productTitle;
        }

        return productTitle.substring(0, 12) + "...";
    }
}