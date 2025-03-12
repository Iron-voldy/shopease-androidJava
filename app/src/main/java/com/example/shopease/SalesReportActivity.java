package com.example.shopease;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.TopProductsAdapter;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SalesReportActivity extends AppCompatActivity {

    private BarChart salesBarChart;
    private PieChart categoryPieChart;
    private TextView totalRevenueTextView;
    private RecyclerView topProductsRecyclerView;
    private DatabaseReference databaseRef;
    private double totalRevenue = 0;
    private final Map<String, Double> categoryRevenue = new HashMap<>();
    private final Map<String, Double> monthlySales = new TreeMap<>();
    private final List<ProductSalesInfo> productSalesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_report);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sales Report");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        initViews();

        // Initialize Firebase Database Reference
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Load sales data
        loadSalesData();
    }

    private void initViews() {
        salesBarChart = findViewById(R.id.salesBarChart);
        categoryPieChart = findViewById(R.id.categoryPieChart);
        totalRevenueTextView = findViewById(R.id.totalRevenueTextView);

        // Setup RecyclerView
        topProductsRecyclerView = findViewById(R.id.topProductsRecyclerView);
        topProductsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Adapter will be set after data is loaded
    }

    private void loadSalesData() {
        databaseRef.child("Orders").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                totalRevenue = 0;
                categoryRevenue.clear();
                monthlySales.clear();
                productSalesList.clear();

                // Track product sales
                Map<String, ProductSalesInfo> productSalesMap = new HashMap<>();

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Double amount = orderSnapshot.child("amount").getValue(Double.class);
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);

                    if (amount != null && timestamp != null) {
                        totalRevenue += amount;

                        // Process monthly sales
                        String month = getMonthFromTimestamp(timestamp);
                        monthlySales.put(month, monthlySales.getOrDefault(month, 0.0) + amount);

                        // Process products in the order
                        DataSnapshot productsSnapshot = orderSnapshot.child("products");
                        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
                            String title = productSnapshot.child("title").getValue(String.class);
                            String category = productSnapshot.child("category") != null ?
                                    productSnapshot.child("category").getValue(String.class) : "Unknown";
                            Double price = productSnapshot.child("price").getValue(Double.class);
                            Integer quantity = productSnapshot.child("quantity").getValue(Integer.class);
                            String imageUrl = productSnapshot.child("imageUrl").getValue(String.class);


                            if (title != null && price != null && quantity != null) {
                                // Update category revenue
                                if (category != null) {
                                    categoryRevenue.put(category,
                                            categoryRevenue.getOrDefault(category, 0.0) + (price * quantity));
                                }

                                // Update product sales info
                                String productId = productSnapshot.getKey();
                                if (productId != null) {
                                    ProductSalesInfo info = productSalesMap.get(productId);
                                    if (info == null) {
                                        info = new ProductSalesInfo(productId, title, imageUrl, price, 0, 0.0);
                                        productSalesMap.put(productId, info);
                                    }

                                    info.setQuantitySold(info.getQuantitySold() + quantity);
                                    info.setTotalRevenue(info.getTotalRevenue() + (price * quantity));
                                }
                            }
                        }
                    }
                }

                // Convert product sales map to list and sort by revenue
                productSalesList.addAll(productSalesMap.values());
                productSalesList.sort((p1, p2) -> Double.compare(p2.getTotalRevenue(), p1.getTotalRevenue()));

                // Update UI with collected data
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private String getMonthFromTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void updateUI() {
        // Format and display total revenue
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setCurrency(Currency.getInstance("USD"));
        totalRevenueTextView.setText(currencyFormat.format(totalRevenue));

        // Update bar chart with monthly sales
        setupBarChart();

        // Update pie chart with category revenue
        setupPieChart();

        // Update RecyclerView with top selling products
        // Limit to top 5 products
        List<ProductSalesInfo> topProducts = productSalesList.size() > 5 ?
                productSalesList.subList(0, 5) : productSalesList;
        TopProductsAdapter adapter = new TopProductsAdapter(topProducts);
        topProductsRecyclerView.setAdapter(adapter);
    }

    private void setupBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Double> entry : monthlySales.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue().floatValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Sales");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);

        salesBarChart.setData(barData);
        salesBarChart.getDescription().setEnabled(false);
        salesBarChart.setDrawGridBackground(false);

        XAxis xAxis = salesBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45);

        salesBarChart.animateY(1000);
        salesBarChart.invalidate();
    }

    private void setupPieChart() {
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Double> entry : categoryRevenue.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);

        categoryPieChart.setData(pieData);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleRadius(35f);
        categoryPieChart.setTransparentCircleRadius(40f);
        categoryPieChart.setDrawEntryLabels(false);
        categoryPieChart.setEntryLabelTextSize(12f);
        categoryPieChart.setCenterText("Sales by Category");
        categoryPieChart.setCenterTextSize(14f);
        categoryPieChart.getLegend().setWordWrapEnabled(true);

        categoryPieChart.animateY(1000);
        categoryPieChart.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Model class for product sales information
    public static class ProductSalesInfo {
        private String productId;
        private String productName;
        private String imageUrl;
        private double price;
        private int quantitySold;
        private double totalRevenue;

        public ProductSalesInfo(String productId, String productName, String imageUrl,
                                double price, int quantitySold, double totalRevenue) {
            this.productId = productId;
            this.productName = productName;
            this.imageUrl = imageUrl;
            this.price = price;
            this.quantitySold = quantitySold;
            this.totalRevenue = totalRevenue;
        }

        // Getters and setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getQuantitySold() { return quantitySold; }
        public void setQuantitySold(int quantitySold) { this.quantitySold = quantitySold; }

        public double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    }
}