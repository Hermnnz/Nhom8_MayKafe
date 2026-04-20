package com.example.nhom8_makafe.data.api;

import java.util.List;

class ApiEnvelope<T> {
    boolean success;
    String message;
    Object errors;
    T data;
}

class LoginRequest {
    String username;
    String password;

    LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

class LoginPayloadDto {
    String token;
    UserDto user;
}

class LoginBrandingDto {
    String loginAvatarUrl;
}

class UserDto {
    String username;
    String displayName;
    String role;
    String avatarInitials;
    String avatarColorHex;
}

class CategoryDto {
    long id;
    String name;
}

class ProductDto {
    long id;
    String name;
    int price;
    String category;
    Long categoryId;
    boolean available;
    String assetLabel;
    String accentColorHex;
    String image;
    String imageUrl;
    String imagePath;
}

class ProductWriteRequest {
    String name;
    int price;
    String category;
    boolean available;
    String assetLabel;
    String accentColorHex;
    String imageUrl;
}

class ProductImageUploadDto {
    String image;
    String imageUrl;
    String imagePath;
}

class ToggleAvailabilityRequest {
    boolean available;

    ToggleAvailabilityRequest(boolean available) {
        this.available = available;
    }
}

class OrderItemDto {
    String name;
    int quantity;
    int unitPrice;
    String note;
    String assetLabel;
    String accentColorHex;
}

class OrderDto {
    int pk;
    String id;
    String tableNumber;
    String date;
    String time;
    List<OrderItemDto> items;
    int total;
    String status;
    String paymentMethod;
    String paymentMethodLabel;
    String note;
    int subtotal;
    int discountAmount;
    int discountPercent;
    int cashReceived;
    int changeAmount;
}

class InvoiceSummaryDto {
    int paidCount;
    int cancelledCount;
    int revenue;
}

class CheckoutItemRequest {
    long productId;
    int quantity;
    String note;

    CheckoutItemRequest(long productId, int quantity, String note) {
        this.productId = productId;
        this.quantity = quantity;
        this.note = note;
    }
}

class CheckoutRequest {
    String tableNumber;
    int discountPercent;
    String paymentMethod;
    Integer cashReceived;
    List<CheckoutItemRequest> items;
}

class PaymentInitRequest {
    String tableNumber;
    int discountPercent;
    List<CheckoutItemRequest> items;
}

class PendingOrderRequest {
    String tableNumber;
    int discountPercent;
    List<CheckoutItemRequest> items;
}

class CashPaymentRequest {
    String tableNumber;
    int discountPercent;
    int cashReceived;
    List<CheckoutItemRequest> items;
}

class CashPaymentConfirmRequest {
    int cashReceived;

    CashPaymentConfirmRequest(int cashReceived) {
        this.cashReceived = cashReceived;
    }
}

class PaymentDto {
    int paymentId;
    int orderId;
    String orderCode;
    int amount;
    String bankName;
    String accountNumber;
    String accountName;
    String transferContent;
    String qrContent;
    String status;
    String expiresAt;
    String paidAt;
}

class DashboardDto {
    String period;
    DashboardSummaryDto summary;
    ChartDto chart;
    List<ReportItemDto> topItems;
    List<CategoryShareDto> categoryShares;
}

class DashboardSummaryDto {
    String revenueChange;
    String ordersChange;
    String averageChange;
    String customersChange;
    int customers;
    boolean revenueUp;
    boolean ordersUp;
    boolean averageUp;
    boolean customersUp;
}

class ChartDto {
    String title;
    String metaLabel;
    List<ChartPointDto> points;
}

class ChartPointDto {
    String label;
    int revenue;
    int orders;
}

class ReportItemDto {
    String name;
    int count;
    int revenue;
    String assetLabel;
    String accentColorHex;
    String imageUrl;
}

class CategoryShareDto {
    String name;
    int percent;
    String colorHex;
}
