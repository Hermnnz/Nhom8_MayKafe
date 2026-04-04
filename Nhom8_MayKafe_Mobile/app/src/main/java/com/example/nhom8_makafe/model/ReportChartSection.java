package com.example.nhom8_makafe.model;

import java.util.ArrayList;
import java.util.List;

public class ReportChartSection {
    private final String title;
    private final String metaLabel;
    private final List<ReportPoint> points;

    public ReportChartSection(String title, String metaLabel, List<ReportPoint> points) {
        this.title = title;
        this.metaLabel = metaLabel;
        this.points = new ArrayList<>(points);
    }

    public String getTitle() {
        return title;
    }

    public String getMetaLabel() {
        return metaLabel;
    }

    public List<ReportPoint> getPoints() {
        return new ArrayList<>(points);
    }
}
