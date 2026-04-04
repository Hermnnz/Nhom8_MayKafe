package com.example.nhom8_makafe.model;

public class OrderItem {
    private final String name;
    private final int quantity;
    private final int unitPrice;
    private final String note;
    private final String assetLabel;
    private final String accentColorHex;

    public OrderItem(String name, int quantity, int unitPrice, String note, String assetLabel, String accentColorHex) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.note = note;
        this.assetLabel = assetLabel;
        this.accentColorHex = accentColorHex;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public String getNote() {
        return note;
    }

    public String getAssetLabel() {
        return assetLabel;
    }

    public String getAccentColorHex() {
        return accentColorHex;
    }

    public int getLineTotal() {
        return unitPrice * quantity;
    }
}
