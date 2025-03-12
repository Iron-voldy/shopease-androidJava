package com.example.shopease;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.R;
import com.example.shopease.adapters.ReviewAdapter;
import com.example.shopease.models.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewsFragment extends Fragment {

    private RecyclerView reviewsRecyclerView;
    private TextView averageRatingText;
    private TextView totalReviewsText;
    private RatingBar averageRatingBar;
    private String productId;

    public ReviewsFragment() {
        // Required empty public constructor
    }

    public static ReviewsFragment newInstance(String productId) {
        ReviewsFragment fragment = new ReviewsFragment();
        Bundle args = new Bundle();
        args.putString("PRODUCT_ID", productId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("PRODUCT_ID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reviews, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        reviewsRecyclerView = view.findViewById(R.id.reviewsRecyclerView);
        averageRatingText = view.findViewById(R.id.averageRatingText);
        totalReviewsText = view.findViewById(R.id.totalReviewsText);
        averageRatingBar = view.findViewById(R.id.averageRatingBar);

        // Set up RecyclerView
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load dummy reviews for now - replace with Firebase in production
        loadSampleReviews();
    }

    private void loadSampleReviews() {
        List<Review> reviews = new ArrayList<>();

        // Sample review 1
        reviews.add(new Review(
                "user1",
                "Jessica Thompson",
                "March 3, 2025",
                "Absolutely love this product! The quality is top-notch and it arrived earlier than expected. The color is exactly as shown in the pictures. I've been using it for a week now and it works perfectly.",
                4.5f,
                true
        ));

        // Sample review 2
        reviews.add(new Review(
                "user2",
                "Michael Chen",
                "February 27, 2025",
                "Good value for money. The material is better than I expected for this price range. Shipping was fast and packaging was secure. Would recommend to others looking for something similar.",
                4.0f,
                true
        ));

        // Sample review 3
        reviews.add(new Review(
                "user3",
                "Sarah Johnson",
                "February 15, 2025",
                "It's okay, but not as durable as I was hoping. The design is nice and modern, but I've noticed some wear after just a few uses. Customer service was helpful when I asked about maintenance tips though.",
                3.0f,
                true
        ));

        // Sample review 4
        reviews.add(new Review(
                "user4",
                "Robert Wilson",
                "February 3, 2025",
                "Perfect fit for what I needed! Easy to set up and the instructions were clear. This is my second purchase from this seller and both times I've been impressed with the quality.",
                5.0f,
                true
        ));

        // Sample review 5
        reviews.add(new Review(
                "user5",
                "Emily Davis",
                "January 20, 2025",
                "Disappointing. The product doesn't match the description accurately. It's smaller than I expected and the finish isn't what was shown in the photos. I'm considering returning it.",
                2.0f,
                true
        ));

        // Calculate and display average rating
        float totalRating = 0;
        for (Review review : reviews) {
            totalRating += review.getRating();
        }

        float averageRating = totalRating / reviews.size();
        averageRatingText.setText(String.format("%.1f", averageRating));
        averageRatingBar.setRating(averageRating);
        totalReviewsText.setText(getString(R.string.based_on_reviews, reviews.size()));

        // Set adapter
        ReviewAdapter adapter = new ReviewAdapter(getContext(), reviews);
        reviewsRecyclerView.setAdapter(adapter);
    }
}