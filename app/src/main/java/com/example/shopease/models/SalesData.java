package com.example.shopease.models;

import java.util.Date;

public class SalesData {
    private String productId;
    private String productTitle;
    private String category;
    private double price;
    private int quantity;
    private double totalAmount;
    private Date orderDate;
    private String orderId;

    public SalesData() {
        // Required empty constructor for Firebase
    }

    public SalesData(String productId, String productTitle, String category, double price,
                     int quantity, double totalAmount, Date orderDate, String orderId) {
        this.productId = productId;
        this.productTitle = productTitle;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}