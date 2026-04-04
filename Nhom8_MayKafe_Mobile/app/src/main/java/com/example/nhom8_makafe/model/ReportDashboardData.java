package com.example.nhom8_makafe.model;

import java.util.ArrayList;
import java.util.List;

public class ReportDashboardData {
    private final ReportChartSection chartSection;
    private final ReportSummary summary;
    private final List<ReportItem> topItems;
    private final List<CategoryShare> categoryShares;

    public ReportDashboardData(ReportChartSection chartSection, ReportSummary summary,
                               List<ReportItem> topItems, List<CategoryShare> categoryShares) {
        this.chartSection = chartSection;
        this.summary = summary;
        this.topItems = new ArrayList<>(topItems);
        this.categoryShares = new ArrayList<>(categoryShares);
    }

    public ReportChartSection getChartSection() {
        return chartSection;
    }

    public ReportSummary getSummary() {
        return summary;
    }

    public List<ReportItem> getTopItems() {
        return new ArrayList<>(topItems);
    }

    public List<CategoryShare> getCategoryShares() {
        return new ArrayList<>(categoryShares);
    }
}
