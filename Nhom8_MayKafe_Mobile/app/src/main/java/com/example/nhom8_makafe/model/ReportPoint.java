package com.example.nhom8_makafe.model;

public class ReportPoint {
    private final String label;
    private final int revenue;
    private final int orders;

    public ReportPoint(String label, int revenue, int orders) {
        this.label = label;
        this.revenue = revenue;
        this.orders = orders;
    }

    public String getLabel() {
        return label;
    }

    public int getRevenue() {
        return revenue;
    }

    public int getOrders() {
        return orders;
    }
}
