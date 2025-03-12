package com.example.shopease.models;

import java.io.Serializable;
import java.util.Map;

public class OrderModel implements Serializable {
    private String orderId;
    private String userId;
    private String status;
    private double amount;
    private long timestamp;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private String paymentMethod;
    private Map<String, OrderProductModel> products;

    // Empty constructor for Firebase
    public OrderModel() {
    }

    public OrderModel(String orderId, String userId, String status, double amount, long timestamp,
                      String address, String city, String state, String zipCode, String phone,
                      String paymentMethod, Map<String, OrderProductModel> products) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.amount = amount;
        this.timestamp = timestamp;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.phone = phone;
        this.paymentMethod = paymentMethod;
        this.products = products;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Map<String, OrderProductModel> getProducts() {
        return products;
    }

    public void setProducts(Map<String, OrderProductModel> products) {
        this.products = products;
    }

    // Helper method to get full address
    public String getFullAddress() {
        StringBuilder addressBuilder = new StringBuilder();

        if (address != null && !address.isEmpty()) {
            addressBuilder.append(address);
        }

        if (city != null && !city.isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(city);
        }

        if (state != null && !state.isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(state);
        }

        if (zipCode != null && !zipCode.isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(" ");
            }
            addressBuilder.append(zipCode);
        }

        return addressBuilder.toString();
    }
}