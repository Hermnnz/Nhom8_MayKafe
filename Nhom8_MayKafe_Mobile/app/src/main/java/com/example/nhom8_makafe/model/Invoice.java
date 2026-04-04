package com.example.nhom8_makafe.model;

import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private final String id;
    private final String tableNumber;
    private final String date;
    private final String time;
    private final List<OrderItem> items;
    private final int total;
    private final OrderStatus status;
    private final String paymentMethodLabel;
    private final String note;

    public Invoice(String id, String tableNumber, String date, String time, List<OrderItem> items, int total,
                   OrderStatus status, String paymentMethodLabel, String note) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.date = date;
        this.time = time;
        this.items = new ArrayList<>(items);
        this.total = total;
        this.status = status;
        this.paymentMethodLabel = paymentMethodLabel;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getTableNumber() {
        return tableNumber;
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

    public int getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getPaymentMethodLabel() {
        return paymentMethodLabel;
    }

    public String getNote() {
        return note;
    }
}
