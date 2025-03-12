package com.example.shopease;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shopease.adapters.QuickActionsAdapter;
import com.example.shopease.models.ProductPerformance;
import com.example.shopease.models.QuickAction;
import com.example.shopease.models.SalesData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class SellerDashboardActivity extends AppCompatActivity {
    private static final String TAG = "SellerDashboardActivity";

    // UI elements
    private FirebaseAuth auth;
    private RecyclerView quickActionsGrid;
    private ExtendedFloatingActionButton fabAddProduct;
    private TextView statsSummary, productsSoldText, ordersCountText;
    private LineChart salesLineChart;
    private PieChart productPieChart;
    private ProgressBar loadingProgressBar;

    // Firebase references
    private DatabaseReference ordersRef;
    private String sellerId;

    // Data
    private List<SalesData> allSalesData = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.US);

    // Default to last 30 days for dashboard
    private final int dashboardDataDays = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        sellerId = auth.getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance()
                .getReference("Orders");

        // Initialize UI Elements
        initializeViews();

        // Toolbar actions
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Floating Action Button for Adding Products
        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(SellerDashboardActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        // Set up Quick Actions Grid
        setupQuickActions();

        // Initialize charts
        setupCharts();

        // Load seller data
        fetchSalesData();
    }

    private void initializeViews() {
        quickActionsGrid = findViewById(R.id.quickActionsGrid);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        statsSummary = findViewById(R.id.statsSummary);
        productsSoldText = findViewById(R.id.productsSoldText);
        ordersCountText = findViewById(R.id.ordersCountText);
        salesLineChart = findViewById(R.id.salesLineChart);
        productPieChart = findViewById(R.id.productPieChart);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupQuickActions() {
        List<QuickAction> quickActions = new ArrayList<>();
        quickActions.add(new QuickAction("Manage Products", R.drawable.baseline_inventory_24));
        quickActions.add(new QuickAction("Order Management", R.drawable.baseline_manage_history_24));
        quickActions.add(new QuickAction("Earnings & Sales", R.drawable.baseline_monitor_heart_24));
        quickActions.add(new QuickAction("Customer Chats", R.drawable.baseline_chat_24));

        QuickActionsAdapter adapter = new QuickActionsAdapter(quickActions, this::onQuickActionClicked);
        quickActionsGrid.setLayoutManager(new GridLayoutManager(this, 2));
        quickActionsGrid.setAdapter(adapter);
    }

    private void onQuickActionClicked(QuickAction action) {
        switch (action.getTitle()) {
            case "Manage Products":
                startActivity(new Intent(this, SellerManageProductsActivity.class));
                break;
            case "Order Management":
                startActivity(new Intent(this, SellerOrdersActivity.class));
                break;
            case "Earnings & Sales":
                startActivity(new Intent(this, SellerEarningsActivity.class));
                break;
            case "Customer Chats":
                // startActivity(new Intent(this, CustomerChatsActivity.class));
                Toast.makeText(this, "Customer Chats feature coming soon", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupCharts() {
        // Setup Line Chart
        setupLineChart();

        // Setup Pie Chart
        setupPieChart();
    }

    private void setupLineChart() {
        salesLineChart.getDescription().setEnabled(false);
        salesLineChart.setDrawGridBackground(false);
        salesLineChart.setDrawBorders(false);
        salesLineChart.setScaleEnabled(true);
        salesLineChart.setPinchZoom(true);
        salesLineChart.getLegend().setEnabled(false);

        XAxis xAxis = salesLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = salesLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        salesLineChart.getAxisRight().setEnabled(false);

        // Don't set empty data at initialization
        salesLineChart.setNoDataText("Loading sales data...");
        salesLineChart.invalidate();
    }

    private void setupPieChart() {
        productPieChart.getDescription().setEnabled(false);
        productPieChart.setUsePercentValues(true);
        productPieChart.setDrawHoleEnabled(true);
        productPieChart.setHoleColor(Color.WHITE);
        productPieChart.setTransparentCircleRadius(61f);
        productPieChart.setHoleRadius(58f);
        productPieChart.setDrawCenterText(true);
        productPieChart.setCenterText("Products");
        productPieChart.setCenterTextSize(14f);
        productPieChart.setRotationEnabled(true);

        // Don't set empty data at initialization
        productPieChart.setNoDataText("Loading product data...");
        productPieChart.invalidate();
    }

    private void fetchSalesData() {
        showLoading(true);

        Log.d(TAG, "Beginning to fetch sales data");
        Log.d(TAG, "Current seller ID: " + sellerId);

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Orders data received: " + snapshot.getChildrenCount() + " orders");

                allSalesData.clear();

                int orderCount = 0;

                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    orderCount++;
                    String orderId = orderSnapshot.getKey();
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    Date orderDate = timestamp != null ? new Date(timestamp) : new Date();

                    // Process products in this order
                    DataSnapshot productsSnapshot = orderSnapshot.child("products");
                    if (productsSnapshot.exists()) {
                        processOrderProducts(orderId, orderDate, productsSnapshot);
                    }
                }

                Log.d(TAG, "Processing complete. Total orders processed: " + orderCount);
                Log.d(TAG, "Final sales data items: " + allSalesData.size());

                // Update UI with filtered data
                updateDashboardWithData();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching sales data: " + error.getMessage());
                Toast.makeText(SellerDashboardActivity.this, "Failed to fetch sales data", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void processOrderProducts(String orderId, Date orderDate, DataSnapshot productsSnapshot) {
        int productCount = 0;
        int matchingProductCount = 0;
        int deliveredProductCount = 0;

        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
            productCount++;
            String productId = productSnapshot.getKey();

            // Check if this is the current seller's product
            String productSellerId = productSnapshot.child("sellerId").getValue(String.class);

            if (productSellerId != null && productSellerId.equals(sellerId)) {
                matchingProductCount++;
                // Extract product details
                String title = productSnapshot.child("title").getValue(String.class);
                String category = productSnapshot.child("category").getValue(String.class);
                Double price = productSnapshot.child("price").getValue(Double.class);
                Integer quantity = productSnapshot.child("quantity").getValue(Integer.class);
                Double deliveryFee = productSnapshot.child("deliveryFee").getValue(Double.class);
                String deliveryStatus = productSnapshot.child("deliveryStatus").getValue(String.class);

                // Set default values if null
                if (title == null) title = "Unknown Product";
                if (category == null) category = "Uncategorized";
                if (price == null) price = 0.0;
                if (quantity == null) quantity = 0;
                if (deliveryFee == null) deliveryFee = 0.0;

                // For dashboard, include all orders regardless of delivery status
                // This gives a more complete picture of current business activity

                deliveredProductCount++;

                // Calculate total amount for this product
                double totalAmount = (price * quantity) + deliveryFee;

                // Create and add sales data
                SalesData salesData = new SalesData(
                        productId,
                        title,
                        category,
                        price,
                        quantity,
                        totalAmount,
                        orderDate,
                        orderId
                );

                allSalesData.add(salesData);
            }
        }

        Log.d(TAG, "Order " + orderId + " summary: " +
                "Products: " + productCount +
                ", Matching seller products: " + matchingProductCount +
                ", Delivered products: " + deliveredProductCount);
    }

    private void updateDashboardWithData() {
        // Filter data for last 30 days
        List<SalesData> recentData = filterSalesByDate(allSalesData, dashboardDataDays);

        // Calculate summary metrics
        updateSummaryMetrics(recentData);

        // Update charts
        updateSalesChart(recentData);
        updateProductPerformanceChart(recentData);
    }

    private List<SalesData> filterSalesByDate(List<SalesData> salesData, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        Date cutoffDate = calendar.getTime();

        List<SalesData> filteredData = new ArrayList<>();
        for (SalesData sale : salesData) {
            if (sale.getOrderDate().after(cutoffDate)) {
                filteredData.add(sale);
            }
        }

        return filteredData;
    }

    private void updateSummaryMetrics(List<SalesData> salesData) {
        double totalEarnings = 0.0;
        int totalQuantity = 0;
        Set<String> uniqueOrderIds = new HashSet<>();

        for (SalesData sale : salesData) {
            totalEarnings += sale.getTotalAmount();
            totalQuantity += sale.getQuantity();
            uniqueOrderIds.add(sale.getOrderId());
        }

        int totalOrders = uniqueOrderIds.size();

        // Update UI
        statsSummary.setText("Total Sales: " + currencyFormat.format(totalEarnings));
        productsSoldText.setText(String.valueOf(totalQuantity));
        ordersCountText.setText(String.valueOf(totalOrders));
    }

    private void updateSalesChart(List<SalesData> salesData) {
        if (salesData.isEmpty()) {
            salesLineChart.clear();
            salesLineChart.setNoDataText("No sales data available");
            salesLineChart.invalidate();
            return;
        }

        // Group sales by date
        Map<Date, Double> dailySales = groupSalesByDate(salesData);

        // Check if we have grouped sales data
        if (dailySales.isEmpty()) {
            salesLineChart.clear();
            salesLineChart.setNoDataText("No sales data available");
            salesLineChart.invalidate();
            return;
        }

        // Convert to entries for the chart
        List<Entry> entries = new ArrayList<>();
        List<String> xAxisLabels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<Date, Double> entry : dailySales.entrySet()) {
            entries.add(new Entry(index, entry.getValue().floatValue()));

            // Format date labels
            String dateLabel = dateFormat.format(entry.getKey());
            xAxisLabels.add(dateLabel);
            index++;
        }

        // Make sure we have entries before creating the dataset
        if (entries.isEmpty()) {
            salesLineChart.clear();
            salesLineChart.setNoDataText("No sales data available");
            salesLineChart.invalidate();
            return;
        }

        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Sales");
        dataSet.setColor(getResources().getColor(R.color.smartshop_dark_blue));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(getResources().getColor(R.color.smartshop_dark_blue));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false); // Hide values for cleaner dashboard view

        // Set data to chart
        LineData lineData = new LineData(dataSet);
        salesLineChart.setData(lineData);

        // Set X axis labels
        salesLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));

        // Refresh chart
        salesLineChart.invalidate();
    }

    private Map<Date, Double> groupSalesByDate(List<SalesData> salesData) {
        Map<Date, Double> result = new TreeMap<>(Comparator.naturalOrder());

        // Group by date
        for (SalesData sale : salesData) {
            Date dateKey = truncateTime(sale.getOrderDate());
            double amount = sale.getTotalAmount();

            if (result.containsKey(dateKey)) {
                result.put(dateKey, result.get(dateKey) + amount);
            } else {
                result.put(dateKey, amount);
            }
        }

        return result;
    }

    private Date truncateTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private void updateProductPerformanceChart(List<SalesData> salesData) {
        if (salesData.isEmpty()) {
            productPieChart.clear();
            productPieChart.setNoDataText("No product data available");
            productPieChart.invalidate();
            return;
        }

        // Group sales by product
        Map<String, ProductPerformance> productMap = new HashMap<>();

        for (SalesData sale : salesData) {
            String productId = sale.getProductId();
            String productTitle = sale.getProductTitle();

            if (!productMap.containsKey(productId)) {
                productMap.put(productId, new ProductPerformance(productId, productTitle));
            }

            ProductPerformance performance = productMap.get(productId);
            performance.addSale(sale.getQuantity(), sale.getTotalAmount());
        }

        // Check if we have products to display
        if (productMap.isEmpty()) {
            productPieChart.clear();
            productPieChart.setNoDataText("No product data available");
            productPieChart.invalidate();
            return;
        }

        // Sort products by revenue
        List<ProductPerformance> sortedProducts = new ArrayList<>(productMap.values());
        Collections.sort(sortedProducts, (p1, p2) ->
                Double.compare(p2.getTotalRevenue(), p1.getTotalRevenue())); // Sort descending

        // Check again after sorting
        if (sortedProducts.isEmpty()) {
            productPieChart.clear();
            productPieChart.setNoDataText("No product data available");
            productPieChart.invalidate();
            return;
        }

        // Take top 3 products only for dashboard clarity
        List<ProductPerformance> topProducts = sortedProducts.size() > 3
                ? sortedProducts.subList(0, 3)
                : sortedProducts;

        // Create pie chart entries
        List<PieEntry> entries = new ArrayList<>();
        for (ProductPerformance product : topProducts) {
            // Skip products with 0 revenue
            if (product.getTotalRevenue() > 0) {
                entries.add(new PieEntry((float) product.getTotalRevenue(), product.getShortTitle()));
            }
        }

        // Make sure we have entries before creating the dataset
        if (entries.isEmpty()) {
            productPieChart.clear();
            productPieChart.setNoDataText("No product data available");
            productPieChart.invalidate();
            return;
        }

        // Create dataset
        PieDataSet dataSet = new PieDataSet(entries, "Products");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return currencyFormat.format(value);
            }
        });

        // Set data to chart
        PieData pieData = new PieData(dataSet);
        productPieChart.setData(pieData);

        // Update center text
        productPieChart.setCenterText("Top Products");

        // Refresh chart
        productPieChart.invalidate();
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when coming back to the dashboard
        fetchSalesData();
    }
}