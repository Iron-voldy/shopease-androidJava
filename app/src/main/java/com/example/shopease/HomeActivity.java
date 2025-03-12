package com.example.shopease;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.shopease.adapters.BannerAdapter;
import com.example.shopease.adapters.ProductAdapter;
import com.example.shopease.repository.FirebaseRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ProductAdapter productAdapter;
    private final FirebaseRepository firebaseRepository = new FirebaseRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        // ✅ Setup Banner Slider
        List<Integer> banners = new ArrayList<>();
        banners.add(R.drawable.banner2);
        banners.add(R.drawable.banner1);
        ViewPager2 bannerViewPager = findViewById(R.id.bannerViewPager);
        bannerViewPager.setAdapter(new BannerAdapter(this, banners));

        // ✅ Setup Featured Products RecyclerView
        RecyclerView featuredList = findViewById(R.id.featuredList);
        productAdapter = new ProductAdapter(this, new ArrayList<>());
        featuredList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        featuredList.setAdapter(productAdapter);
        fetchProducts();

        // ✅ Bottom Navigation Handling
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return handleNavigation(item);
            }
        });

    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        Class<?> activity = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_home) {
            return true; // Already in Home
        } else if (itemId == R.id.nav_cart) {
            activity = CartActivity.class;
        } else if (itemId == R.id.nav_wishlist) {
            activity = WishlistActivity.class;
        }else if (itemId == R.id.nav_profile) {
            activity = UserProfileActivity.class;
        }

        if (activity != null) {
            startActivity(new Intent(this, activity));
        }
        return false;
    }

    // ✅ Fetch products from Firebase
    private void fetchProducts() {
        firebaseRepository.fetchProducts(products -> {
            if (products != null) {
                runOnUiThread(() -> productAdapter.updateProducts(products));
            }
        });
    }
}