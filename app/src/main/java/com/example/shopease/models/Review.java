package com.example.shopease.models;

public class Review {
    private String reviewerId;
    private String reviewerName;
    private String reviewDate;
    private String reviewText;
    private float rating;
    private boolean verifiedPurchase;

    // Empty constructor for Firebase
    public Review() {
    }

    public Review(String reviewerId, String reviewerName, String reviewDate, String reviewText, float rating, boolean verifiedPurchase) {
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewDate = reviewDate;
        this.reviewText = reviewText;
        this.rating = rating;
        this.verifiedPurchase = verifiedPurchase;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(String reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public boolean isVerifiedPurchase() {
        return verifiedPurchase;
    }

    public void setVerifiedPurchase(boolean verifiedPurchase) {
        this.verifiedPurchase = verifiedPurchase;
    }
}