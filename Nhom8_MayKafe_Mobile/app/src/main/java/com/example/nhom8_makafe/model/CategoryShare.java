package com.example.nhom8_makafe.model;

public class CategoryShare {
    private final String name;
    private final int percent;
    private final String colorHex;

    public CategoryShare(String name, int percent, String colorHex) {
        this.name = name;
        this.percent = percent;
        this.colorHex = colorHex;
    }

    public String getName() {
        return name;
    }

    public int getPercent() {
        return percent;
    }

    public String getColorHex() {
        return colorHex;
    }
}
