package com.example.nhom8_makafe.model;

public class InvoiceSummaryData {
    private final int paidCount;
    private final int cancelledCount;
    private final int revenue;

    public InvoiceSummaryData(int paidCount, int cancelledCount, int revenue) {
        this.paidCount = paidCount;
        this.cancelledCount = cancelledCount;
        this.revenue = revenue;
    }

    public int getPaidCount() {
        return paidCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public int getRevenue() {
        return revenue;
    }
}
