package com.example.shopease;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.shopease.models.ProductPerformance;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

public class SellerEarningsActivity extends AppCompatActivity {
    private static final String TAG = "SellerEarningsActivity";

    // UI elements
    private TextView totalEarningsText, totalProductsSoldText, totalOrdersText, avgOrderValueText;
    private LineChart salesLineChart;
    private PieChart productPieChart;
    private ChipGroup dateFilterChipGroup;
    private Chip chipWeek, chipMonth, chipYear, chipAll;
    private ProgressBar loadingProgressBar;

    // Firebase references
    private DatabaseReference ordersRef;
    private String sellerId;

    // Data
    private List<SalesData> allSalesData = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.US);
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.US);

    // Filter parameters
    private int dateFilterDays = 30; // Default to last 30 days

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_earnings);

        // Set up toolbar
        setupToolbar();

        // Initialize UI elements
        initializeViews();

        // Set up date filter chips
        setupDateFilter();

        // Initialize Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sellerId = auth.getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance()
                .getReference("Orders");

        // Fetch sales data
        fetchSalesData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Earnings and Sales Report");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        totalEarningsText = findViewById(R.id.totalEarnings);
        totalProductsSoldText = findViewById(R.id.totalProductsSold);
        totalOrdersText = findViewById(R.id.totalOrders);
        avgOrderValueText = findViewById(R.id.avgOrderValue);
        salesLineChart = findViewById(R.id.salesLineChart);
        productPieChart = findViewById(R.id.productPieChart);
        dateFilterChipGroup = findViewById(R.id.dateFilterChipGroup);
        chipWeek = findViewById(R.id.chipWeek);
        chipMonth = findViewById(R.id.chipMonth);
        chipYear = findViewById(R.id.chipYear);
        chipAll = findViewById(R.id.chipAll);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        // Initialize charts
        setupLineChart();
        setupPieChart();
    }

    private void setupDateFilter() {
        dateFilterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipWeek) {
                dateFilterDays = 7;
            } else if (checkedId == R.id.chipMonth) {
                dateFilterDays = 30;
            } else if (checkedId == R.id.chipYear) {
                dateFilterDays = 365;
            } else if (checkedId == R.id.chipAll) {
                dateFilterDays = Integer.MAX_VALUE;
            }

            // Apply filter to the data
            if (!allSalesData.isEmpty()) {
                updateUIWithFilteredData();
            }
        });
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
        salesLineChart.setNoDataText("No sales data available");
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
        productPieChart.setCenterTextSize(16f);
        productPieChart.setRotationEnabled(true);

        // Don't set empty data at initialization
        productPieChart.setNoDataText("No product data available");
        productPieChart.invalidate();
    }

    private void fetchSalesData() {
        showLoading(true);

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Orders data received: " + snapshot.getChildrenCount() + " orders");

                allSalesData.clear();

                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    String orderId = orderSnapshot.getKey();
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    Date orderDate = timestamp != null ? new Date(timestamp) : new Date();

                    // Process products in this order
                    DataSnapshot productsSnapshot = orderSnapshot.child("products");
                    if (productsSnapshot.exists()) {
                        processOrderProducts(orderId, orderDate, productsSnapshot);
                    }
                }

                // Update UI with filtered data
                updateUIWithFilteredData();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching sales data: " + error.getMessage());
                Toast.makeText(SellerEarningsActivity.this, "Failed to fetch sales data", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void processOrderProducts(String orderId, Date orderDate, DataSnapshot productsSnapshot) {
        for (DataSnapshot productSnapshot : productsSnapshot.getChildren()) {
            String productId = productSnapshot.getKey();

            // Check if this is the current seller's product
            String productSellerId = productSnapshot.child("sellerId").getValue(String.class);
            if (productSellerId != null && productSellerId.equals(sellerId)) {
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

                // Only count completed orders (assuming "Delivered" status means completed)
                // If you want to count all orders regardless of status, remove this condition
                if (deliveryStatus == null || !"Delivered".equals(deliveryStatus)) {
                    continue; // Skip this product as it's not delivered yet
                }

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
    }

    private void updateUIWithFilteredData() {
        // Filter data based on selected date range
        List<SalesData> filteredData = filterSalesByDate(allSalesData, dateFilterDays);

        // Calculate summary metrics
        updateSummaryMetrics(filteredData);

        // Update charts
        updateSalesChart(filteredData);
        updateProductPerformanceChart(filteredData);
    }

    private List<SalesData> filterSalesByDate(List<SalesData> salesData, int days) {
        if (days == Integer.MAX_VALUE) {
            return new ArrayList<>(salesData); // Return all data
        }

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
        double avgOrderValue = totalOrders > 0 ? totalEarnings / totalOrders : 0.0;

        // Update UI
        totalEarningsText.setText(currencyFormat.format(totalEarnings));
        totalProductsSoldText.setText(String.valueOf(totalQuantity));
        totalOrdersText.setText(String.valueOf(totalOrders));
        avgOrderValueText.setText(currencyFormat.format(avgOrderValue));
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

            // Format date labels based on date range
            String dateLabel;
            if (dateFilterDays <= 30) {
                dateLabel = dateFormat.format(entry.getKey());
            } else {
                dateLabel = monthFormat.format(entry.getKey());
            }
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
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return currencyFormat.format(value);
            }
        });

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

        // If we have more than 30 data points and not showing all time data,
        // group by month for better visualization
        if (result.size() > 30 && dateFilterDays < Integer.MAX_VALUE) {
            return groupSalesByMonth(result);
        }

        return result;
    }

    private Map<Date, Double> groupSalesByMonth(Map<Date, Double> dailySales) {
        Map<Date, Double> monthlySales = new TreeMap<>(Comparator.naturalOrder());

        for (Map.Entry<Date, Double> entry : dailySales.entrySet()) {
            Date monthKey = truncateToMonth(entry.getKey());
            double amount = entry.getValue();

            if (monthlySales.containsKey(monthKey)) {
                monthlySales.put(monthKey, monthlySales.get(monthKey) + amount);
            } else {
                monthlySales.put(monthKey, amount);
            }
        }

        return monthlySales;
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

    private Date truncateToMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
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

        // Take top 5 products only for clarity
        List<ProductPerformance> topProducts = sortedProducts.size() > 5
                ? sortedProducts.subList(0, 5)
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

        // Update center text to show total
        productPieChart.setCenterText("Top Products");

        // Refresh chart
        productPieChart.invalidate();
    }

    private void showLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}