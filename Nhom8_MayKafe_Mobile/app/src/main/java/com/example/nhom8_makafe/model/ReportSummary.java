package com.example.nhom8_makafe.model;

public class ReportSummary {
    private final String revenueChange;
    private final String ordersChange;
    private final String averageChange;
    private final String customersChange;
    private final int customers;
    private final boolean revenueUp;
    private final boolean ordersUp;
    private final boolean averageUp;
    private final boolean customersUp;

    public ReportSummary(String revenueChange, String ordersChange, String averageChange, String customersChange,
                         int customers, boolean revenueUp, boolean ordersUp, boolean averageUp, boolean customersUp) {
        this.revenueChange = revenueChange;
        this.ordersChange = ordersChange;
        this.averageChange = averageChange;
        this.customersChange = customersChange;
        this.customers = customers;
        this.revenueUp = revenueUp;
        this.ordersUp = ordersUp;
        this.averageUp = averageUp;
        this.customersUp = customersUp;
    }

    public String getRevenueChange() {
        return revenueChange;
    }

    public String getOrdersChange() {
        return ordersChange;
    }

    public String getAverageChange() {
        return averageChange;
    }

    public String getCustomersChange() {
        return customersChange;
    }

    public int getCustomers() {
        return customers;
    }

    public boolean isRevenueUp() {
        return revenueUp;
    }

    public boolean isOrdersUp() {
        return ordersUp;
    }

    public boolean isAverageUp() {
        return averageUp;
    }

    public boolean isCustomersUp() {
        return customersUp;
    }
}
