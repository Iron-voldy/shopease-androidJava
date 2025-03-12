package com.example.shopease;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shopease.adapters.OnboardingAdapter;
import com.example.shopease.models.OnboardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button skipButton;
    private Button nextButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        sharedPreferences = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE);
        boolean isOnboardingCompleted = sharedPreferences.getBoolean("isOnboardingCompleted", false);

        if (isOnboardingCompleted) {
            navigateToRegistration();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        List<OnboardingItem> onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(R.drawable.image1, "Shop From your favorite store", "Discover amazing products and great deals."));
        onboardingItems.add(new OnboardingItem(R.drawable.image3, "Flexible payment", "Shop with ease and convenience from your home."));
        onboardingItems.add(new OnboardingItem(R.drawable.image2, "Get IT Delivered ", "Get your orders delivered quickly and reliably."));

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new OnboardingAdapter(onboardingItems));

        skipButton = findViewById(R.id.skipButton);
        nextButton = findViewById(R.id.nextButton);

        skipButton.setOnClickListener(v -> navigateToRegistration());

        nextButton.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingItems.size()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                markOnboardingCompleted();
                navigateToRegistration();
            }
        });
    }

    private void navigateToRegistration() {
        startActivity(new Intent(this, RegistrationActivity.class));
        finish();
    }

    private void markOnboardingCompleted() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isOnboardingCompleted", true);
        editor.apply();
    }
}
