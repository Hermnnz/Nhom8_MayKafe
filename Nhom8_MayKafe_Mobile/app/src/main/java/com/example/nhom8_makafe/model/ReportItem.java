package com.example.nhom8_makafe.model;

public class ReportItem {
    private final String name;
    private final int count;
    private final int revenue;
    private final String assetLabel;
    private final String accentColorHex;
    private final String imageUrl;

    public ReportItem(String name, int count, int revenue, String assetLabel, String accentColorHex, String imageUrl) {
        this.name = name;
        this.count = count;
        this.revenue = revenue;
        this.assetLabel = assetLabel;
        this.accentColorHex = accentColorHex;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public int getRevenue() {
        return revenue;
    }

    public String getAssetLabel() {
        return assetLabel;
    }

    public String getAccentColorHex() {
        return accentColorHex;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
