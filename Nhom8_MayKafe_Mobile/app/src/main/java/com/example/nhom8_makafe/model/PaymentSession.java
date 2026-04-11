package com.example.nhom8_makafe.model;

public class PaymentSession {
    private final int orderId;
    private final String orderCode;
    private final int paymentId;
    private final int amount;
    private final String bankName;
    private final String accountNumber;
    private final String accountName;
    private final String transferContent;
    private final String qrContent;
    private final String status;
    private final String expiresAt;

    public PaymentSession(int orderId, String orderCode, int paymentId, int amount, String bankName,
                          String accountNumber, String accountName, String transferContent,
                          String qrContent, String status, String expiresAt) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.paymentId = paymentId;
        this.amount = amount;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.transferContent = transferContent;
        this.qrContent = qrContent;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getAmount() {
        return amount;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getTransferContent() {
        return transferContent;
    }

    public String getQrContent() {
        return qrContent;
    }

    public String getStatus() {
        return status;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
