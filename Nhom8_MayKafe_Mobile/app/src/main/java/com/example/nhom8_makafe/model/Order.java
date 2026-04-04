package com.example.nhom8_makafe.model;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private final String id;
    private final String date;
    private final String time;
    private final List<OrderItem> items;
    private final int subtotal;
    private final int discountPercent;
    private final int discountAmount;
    private final int finalTotal;
    private final OrderStatus status;
    private final String tableNumber;
    private final PaymentMethod paymentMethod;
    private final String createdBy;

    public Order(String id, String date, String time, List<OrderItem> items, int subtotal, int discountPercent,
                 int discountAmount, int finalTotal, OrderStatus status, String tableNumber,
                 PaymentMethod paymentMethod, String createdBy) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.items = new ArrayList<>(items);
        this.subtotal = subtotal;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
        this.finalTotal = finalTotal;
        this.status = status;
        this.tableNumber = tableNumber;
        this.paymentMethod = paymentMethod;
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public List<OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    public int getSubtotal() {
        return subtotal;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public int getFinalTotal() {
        return finalTotal;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
