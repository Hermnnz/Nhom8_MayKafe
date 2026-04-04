package com.example.nhom8_makafe.model;

public class Product {
    private final long id;
    private String name;
    private int price;
    private String category;
    private boolean available;
    private String assetLabel;
    private String accentColorHex;
    private String imageUrl;

    public Product(long id, String name, int price, String category, boolean available, String assetLabel, String accentColorHex, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.available = available;
        this.assetLabel = assetLabel;
        this.accentColorHex = accentColorHex;
        this.imageUrl = imageUrl;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return available;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setAssetLabel(String assetLabel) {
        this.assetLabel = assetLabel;
    }

    public void setAccentColorHex(String accentColorHex) {
        this.accentColorHex = accentColorHex;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
