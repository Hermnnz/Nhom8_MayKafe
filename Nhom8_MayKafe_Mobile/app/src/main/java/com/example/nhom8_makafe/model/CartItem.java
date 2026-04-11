package com.example.nhom8_makafe.model;

public class CartItem {
    private final long productId;
    private final String name;
    private final int price;
    private final String category;
    private final String assetLabel;
    private final String accentColorHex;
    private final String imageUrl;
    private int quantity;
    private String note;

    public CartItem(long productId, String name, int price, String category, String assetLabel, String accentColorHex, String imageUrl, int quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.category = category;
        this.assetLabel = assetLabel;
        this.accentColorHex = accentColorHex;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.note = "";
    }

    public long getProductId() {
        return productId;
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

    public String getAssetLabel() {
        return assetLabel;
    }

    public String getAccentColorHex() {
        return accentColorHex;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
