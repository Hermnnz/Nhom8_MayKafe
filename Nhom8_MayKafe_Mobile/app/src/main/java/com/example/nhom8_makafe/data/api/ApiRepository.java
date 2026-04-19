package com.example.nhom8_makafe.data.api;

import androidx.annotation.Nullable;

import com.example.nhom8_makafe.data.SessionManager;
import com.example.nhom8_makafe.model.CartItem;
import com.example.nhom8_makafe.model.CategoryShare;
import com.example.nhom8_makafe.model.Invoice;
import com.example.nhom8_makafe.model.InvoiceSummaryData;
import com.example.nhom8_makafe.model.OrderItem;
import com.example.nhom8_makafe.model.OrderStatus;
import com.example.nhom8_makafe.model.PaymentSession;
import com.example.nhom8_makafe.model.PaymentMethod;
import com.example.nhom8_makafe.model.Product;
import com.example.nhom8_makafe.model.ReportChartSection;
import com.example.nhom8_makafe.model.ReportDashboardData;
import com.example.nhom8_makafe.model.ReportItem;
import com.example.nhom8_makafe.model.ReportPeriod;
import com.example.nhom8_makafe.model.ReportPoint;
import com.example.nhom8_makafe.model.ReportSummary;
import com.example.nhom8_makafe.model.Role;
import com.example.nhom8_makafe.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiRepository {
    private static final int PAGE_SIZE = 100;
    private static ApiRepository instance;

    private final ApiService apiService = ApiClient.getService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    public static synchronized ApiRepository getInstance() {
        if (instance == null) {
            instance = new ApiRepository();
        }
        return instance;
    }

    public void login(String username, String password, ApiCallback<User> callback) {
        enqueue(apiService.login(new LoginRequest(username, password)), new ApiCallback<LoginPayloadDto>() {
            @Override
            public void onSuccess(LoginPayloadDto data) {
                if (data == null || data.user == null || data.token == null || data.token.trim().isEmpty()) {
                    callback.onError("Không nhận được dữ liệu đăng nhập hợp lệ.");
                    return;
                }
                User user = mapUser(data.user);
                sessionManager.login(data.token, user);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onError(normalizeLoginErrorMessage(message));
            }
        });
    }

    public void refreshMe(ApiCallback<User> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.me(authorization), new ApiCallback<UserDto>() {
            @Override
            public void onSuccess(UserDto data) {
                User user = mapUser(data);
                sessionManager.login(sessionManager.getAuthToken(), user);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void logout(ApiCallback<Void> callback) {
        String authorization = sessionManager.getAuthHeader();
        if (authorization == null) {
            callback.onSuccess(null);
            return;
        }
        enqueue(apiService.logout(authorization), new ApiCallback<Object>() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchCategories(ApiCallback<List<String>> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.getCategories(authorization), new ApiCallback<List<CategoryDto>>() {
            @Override
            public void onSuccess(List<CategoryDto> data) {
                List<String> categories = new ArrayList<>();
                if (data != null) {
                    for (CategoryDto dto : data) {
                        if (dto != null && dto.name != null && !dto.name.trim().isEmpty()) {
                            categories.add(dto.name.trim());
                        }
                    }
                }
                callback.onSuccess(categories);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchProducts(String search, @Nullable String category, @Nullable Boolean available,
                              ApiCallback<List<Product>> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.getProducts(
                authorization,
                emptyToNull(search),
                emptyToNull(category),
                available,
                PAGE_SIZE
        ), new ApiCallback<List<ProductDto>>() {
            @Override
            public void onSuccess(List<ProductDto> data) {
                List<Product> products = new ArrayList<>();
                if (data != null) {
                    for (ProductDto dto : data) {
                        products.add(mapProduct(dto));
                    }
                }
                callback.onSuccess(products);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void createProduct(Product product, ApiCallback<Product> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.createProduct(authorization, toProductWriteRequest(product)), mapProductCallback(callback));
    }

    public void updateProduct(Product product, ApiCallback<Product> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.updateProduct(authorization, product.getId(), toProductWriteRequest(product)), mapProductCallback(callback));
    }

    public void deleteProduct(long productId, ApiCallback<Void> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.deleteProduct(authorization, productId), new ApiCallback<Object>() {
            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void toggleAvailability(Product product, ApiCallback<Product> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        boolean targetAvailability = !product.isAvailable();
        enqueue(apiService.toggleAvailability(
                authorization,
                product.getId(),
                new ToggleAvailabilityRequest(targetAvailability)
        ), mapProductCallback(callback));
    }

    public void fetchInvoices(String search, String date, @Nullable OrderStatus status, ApiCallback<List<Invoice>> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.getOrders(
                authorization,
                emptyToNull(search),
                emptyToNull(date),
                status == null ? null : status.name(),
                PAGE_SIZE
        ), new ApiCallback<List<OrderDto>>() {
            @Override
            public void onSuccess(List<OrderDto> data) {
                List<Invoice> invoices = new ArrayList<>();
                if (data != null) {
                    for (OrderDto dto : data) {
                        invoices.add(mapInvoice(dto));
                    }
                }
                callback.onSuccess(invoices);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchInvoiceByCode(String code, ApiCallback<Invoice> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.getOrderByCode(authorization, code), new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto data) {
                callback.onSuccess(mapInvoice(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchInvoiceSummary(String date, ApiCallback<InvoiceSummaryData> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.getOrderSummary(authorization, emptyToNull(date)), new ApiCallback<InvoiceSummaryDto>() {
            @Override
            public void onSuccess(InvoiceSummaryDto data) {
                if (data == null) {
                    callback.onSuccess(new InvoiceSummaryData(0, 0, 0));
                    return;
                }
                callback.onSuccess(new InvoiceSummaryData(data.paidCount, data.cancelledCount, data.revenue));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void checkout(String tableNumber, int discountPercent, PaymentMethod paymentMethod, int cashReceived,
                         List<CartItem> cartItems, ApiCallback<Invoice> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        CheckoutRequest request = new CheckoutRequest();
        request.tableNumber = tableNumber;
        request.discountPercent = discountPercent;
        request.paymentMethod = paymentMethod == PaymentMethod.QR ? "QR" : "CASH";
        request.cashReceived = paymentMethod == PaymentMethod.CASH ? cashReceived : null;
        request.items = buildCheckoutItems(cartItems);

        enqueue(apiService.checkout(authorization, request), new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto data) {
                callback.onSuccess(mapInvoice(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void initializeQrPayment(String tableNumber, int discountPercent,
                                    List<CartItem> cartItems, ApiCallback<PaymentSession> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        PaymentInitRequest request = new PaymentInitRequest();
        request.tableNumber = tableNumber;
        request.discountPercent = discountPercent;
        request.items = buildCheckoutItems(cartItems);
        enqueue(apiService.createQrPayment(authorization, request), new ApiCallback<PaymentDto>() {
            @Override
            public void onSuccess(PaymentDto data) {
                callback.onSuccess(mapPaymentSession(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void refreshQrPayment(int orderId, ApiCallback<PaymentSession> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.refreshQrPayment(authorization, orderId), new ApiCallback<PaymentDto>() {
            @Override
            public void onSuccess(PaymentDto data) {
                callback.onSuccess(mapPaymentSession(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void confirmCashPayment(String tableNumber, int discountPercent, int cashReceived,
                                   List<CartItem> cartItems, ApiCallback<Invoice> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        CashPaymentRequest request = new CashPaymentRequest();
        request.tableNumber = tableNumber;
        request.discountPercent = discountPercent;
        request.cashReceived = cashReceived;
        request.items = buildCheckoutItems(cartItems);
        enqueue(apiService.confirmCashPayment(authorization, request), new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto data) {
                callback.onSuccess(mapInvoice(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void confirmCashPaymentForOrder(int orderId, int cashReceived, ApiCallback<Invoice> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.confirmCashPaymentForOrder(
                authorization,
                orderId,
                new CashPaymentConfirmRequest(cashReceived)
        ), new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto data) {
                callback.onSuccess(mapInvoice(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void confirmBankTransfer(int paymentId, ApiCallback<Invoice> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.confirmBankTransfer(authorization, paymentId), new ApiCallback<OrderDto>() {
            @Override
            public void onSuccess(OrderDto data) {
                callback.onSuccess(mapInvoice(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchPaymentStatus(int paymentId, ApiCallback<PaymentSession> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        enqueue(apiService.getPaymentStatus(authorization, paymentId), new ApiCallback<PaymentDto>() {
            @Override
            public void onSuccess(PaymentDto data) {
                callback.onSuccess(mapPaymentSession(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchReportDashboard(ReportPeriod period, ApiCallback<ReportDashboardData> callback) {
        String authorization = requireAuthorization(callback);
        if (authorization == null) {
            return;
        }
        String periodValue = period == null ? "week" : period.name().toLowerCase(Locale.ROOT);
        enqueue(apiService.getDashboard(authorization, periodValue), new ApiCallback<DashboardDto>() {
            @Override
            public void onSuccess(DashboardDto data) {
                callback.onSuccess(mapDashboard(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private ProductWriteRequest toProductWriteRequest(Product product) {
        ProductWriteRequest request = new ProductWriteRequest();
        request.name = product.getName();
        request.price = product.getPrice();
        request.category = product.getCategory();
        request.available = product.isAvailable();
        request.assetLabel = product.getAssetLabel();
        request.accentColorHex = product.getAccentColorHex();
        request.imageUrl = emptyToNull(product.getImageUrl());
        return request;
    }

    private List<CheckoutItemRequest> buildCheckoutItems(List<CartItem> cartItems) {
        List<CheckoutItemRequest> requests = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            requests.add(new CheckoutItemRequest(
                    cartItem.getProductId(),
                    cartItem.getQuantity(),
                    emptyToNull(cartItem.getNote())
            ));
        }
        return requests;
    }

    private ApiCallback<ProductDto> mapProductCallback(ApiCallback<Product> callback) {
        return new ApiCallback<ProductDto>() {
            @Override
            public void onSuccess(ProductDto data) {
                callback.onSuccess(mapProduct(data));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        };
    }

    private Product mapProduct(ProductDto dto) {
        return new Product(
                dto == null ? 0 : dto.id,
                dto == null || dto.name == null ? "" : dto.name,
                dto == null ? 0 : dto.price,
                dto == null || dto.category == null ? "" : dto.category,
                dto != null && dto.available,
                dto == null || dto.assetLabel == null || dto.assetLabel.trim().isEmpty() ? "MK" : dto.assetLabel,
                dto == null || dto.accentColorHex == null || dto.accentColorHex.trim().isEmpty() ? "#6B3F2A" : dto.accentColorHex,
                resolveImage(dto)
        );
    }

    private Invoice mapInvoice(OrderDto dto) {
        List<OrderItem> items = new ArrayList<>();
        if (dto != null && dto.items != null) {
            for (OrderItemDto itemDto : dto.items) {
                items.add(new OrderItem(
                        itemDto.name == null ? "" : itemDto.name,
                        itemDto.quantity,
                        itemDto.unitPrice,
                        itemDto.note == null ? "" : itemDto.note,
                        itemDto.assetLabel == null ? "MK" : itemDto.assetLabel,
                        itemDto.accentColorHex == null ? "#6B3F2A" : itemDto.accentColorHex
                ));
            }
        }
        return new Invoice(
                dto == null || dto.id == null ? "" : dto.id,
                dto == null || dto.tableNumber == null ? "" : dto.tableNumber,
                dto == null || dto.date == null ? "" : dto.date,
                dto == null || dto.time == null ? "" : dto.time,
                items,
                dto == null ? 0 : dto.total,
                mapOrderStatus(dto == null ? null : dto.status),
                dto == null ? null : dto.paymentMethodLabel,
                dto == null ? "" : dto.note
        );
    }

    private PaymentSession mapPaymentSession(PaymentDto dto) {
        return new PaymentSession(
                dto == null ? 0 : dto.orderId,
                dto == null || dto.orderCode == null ? "" : dto.orderCode,
                dto == null ? 0 : dto.paymentId,
                dto == null ? 0 : dto.amount,
                dto == null || dto.bankName == null ? "" : dto.bankName,
                dto == null || dto.accountNumber == null ? "" : dto.accountNumber,
                dto == null || dto.accountName == null ? "" : dto.accountName,
                dto == null || dto.transferContent == null ? "" : dto.transferContent,
                dto == null || dto.qrContent == null ? "" : dto.qrContent,
                dto == null || dto.status == null ? "" : dto.status,
                dto == null || dto.expiresAt == null ? "" : dto.expiresAt
        );
    }

    private ReportDashboardData mapDashboard(DashboardDto dto) {
        DashboardSummaryDto summaryDto = dto == null ? null : dto.summary;
        ReportSummary summary = new ReportSummary(
                summaryDto == null || summaryDto.revenueChange == null ? "+0.0%" : summaryDto.revenueChange,
                summaryDto == null || summaryDto.ordersChange == null ? "+0.0%" : summaryDto.ordersChange,
                summaryDto == null || summaryDto.averageChange == null ? "+0.0%" : summaryDto.averageChange,
                summaryDto == null || summaryDto.customersChange == null ? "+0.0%" : summaryDto.customersChange,
                summaryDto == null ? 0 : summaryDto.customers,
                summaryDto == null || summaryDto.revenueUp,
                summaryDto == null || summaryDto.ordersUp,
                summaryDto == null || summaryDto.averageUp,
                summaryDto == null || summaryDto.customersUp
        );

        List<ReportPoint> points = new ArrayList<>();
        if (dto != null && dto.chart != null && dto.chart.points != null) {
            for (ChartPointDto pointDto : dto.chart.points) {
                points.add(new ReportPoint(
                        pointDto.label == null ? "" : pointDto.label,
                        pointDto.revenue,
                        pointDto.orders
                ));
            }
        }

        List<ReportItem> topItems = new ArrayList<>();
        if (dto != null && dto.topItems != null) {
            for (ReportItemDto itemDto : dto.topItems) {
                topItems.add(new ReportItem(
                        itemDto.name == null ? "" : itemDto.name,
                        itemDto.count,
                        itemDto.revenue,
                        itemDto.assetLabel == null ? "MK" : itemDto.assetLabel,
                        itemDto.accentColorHex == null ? "#6B3F2A" : itemDto.accentColorHex,
                        itemDto.imageUrl == null ? "" : itemDto.imageUrl
                ));
            }
        }

        List<CategoryShare> categoryShares = new ArrayList<>();
        if (dto != null && dto.categoryShares != null) {
            for (CategoryShareDto shareDto : dto.categoryShares) {
                categoryShares.add(new CategoryShare(
                        shareDto.name == null ? "" : shareDto.name,
                        shareDto.percent,
                        shareDto.colorHex == null ? "#6B3F2A" : shareDto.colorHex
                ));
            }
        }

        ChartDto chartDto = dto == null ? null : dto.chart;
        ReportChartSection chartSection = new ReportChartSection(
                chartDto == null || chartDto.title == null ? "" : chartDto.title,
                chartDto == null || chartDto.metaLabel == null ? "" : chartDto.metaLabel,
                points
        );
        return new ReportDashboardData(chartSection, summary, topItems, categoryShares);
    }

    private User mapUser(UserDto dto) {
        Role role;
        try {
            role = Role.valueOf(dto == null || dto.role == null ? Role.STAFF.name() : dto.role.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            role = Role.STAFF;
        }
        return new User(
                dto == null || dto.username == null ? "" : dto.username,
                "",
                dto == null || dto.displayName == null ? "" : dto.displayName,
                role,
                dto == null || dto.avatarInitials == null ? "MK" : dto.avatarInitials,
                dto == null || dto.avatarColorHex == null ? "#6B3F2A" : dto.avatarColorHex
        );
    }

    private OrderStatus mapOrderStatus(String value) {
        if (value == null) {
            return OrderStatus.PAID;
        }
        try {
            return OrderStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return OrderStatus.PAID;
        }
    }

    private String resolveImage(ProductDto dto) {
        if (dto == null) {
            return "";
        }
        if (dto.imageUrl != null && !dto.imageUrl.trim().isEmpty()) {
            return dto.imageUrl;
        }
        if (dto.image != null && !dto.image.trim().isEmpty()) {
            return dto.image;
        }
        return "";
    }

    private String requireAuthorization(ApiCallback<?> callback) {
        String authorization = sessionManager.getAuthHeader();
        if (authorization == null || authorization.trim().isEmpty()) {
            callback.onError("Phiên đăng nhập không hợp lệ.");
            return null;
        }
        return authorization;
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private <T> void enqueue(Call<ApiEnvelope<T>> call, ApiCallback<T> callback) {
        call.enqueue(new Callback<ApiEnvelope<T>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<T>> call, Response<ApiEnvelope<T>> response) {
                if (!response.isSuccessful()) {
                    callback.onError(buildHttpErrorMessage(response));
                    return;
                }
                ApiEnvelope<T> body = response.body();
                if (body == null) {
                    callback.onError("Máy chủ không trả dữ liệu.");
                    return;
                }
                if (!body.success) {
                    String detailMessage = extractErrorMessage(body.errors);
                    callback.onError(detailMessage == null
                            ? (body.message == null ? "Yêu cầu thất bại." : body.message)
                            : detailMessage);
                    return;
                }
                callback.onSuccess(body.data);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<T>> call, Throwable throwable) {
                callback.onError(buildFailureMessage(throwable));
            }
        });
    }

    private String buildHttpErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    try {
                        JSONObject jsonObject = new JSONObject(raw);
                        String detailMessage = extractErrorMessage(jsonObject.opt("errors"));
                        if (detailMessage != null && !detailMessage.trim().isEmpty()) {
                            return detailMessage;
                        }
                        String message = jsonObject.optString("message", null);
                        if (message != null && !message.trim().isEmpty()) {
                            return message;
                        }
                    } catch (Exception ignored) {
                    }
                    return raw;
                }
            }
        } catch (IOException ignored) {
        }
        return "Kết nối máy chủ thất bại (" + response.code() + ").";
    }

    private String extractErrorMessage(Object errors) {
        if (errors == null || errors == JSONObject.NULL) {
            return null;
        }
        if (errors instanceof String) {
            String value = ((String) errors).trim();
            return value.isEmpty() ? null : value;
        }
        if (errors instanceof JSONArray) {
            JSONArray array = (JSONArray) errors;
            for (int index = 0; index < array.length(); index++) {
                String message = extractErrorMessage(array.opt(index));
                if (message != null) {
                    return message;
                }
            }
            return null;
        }
        if (errors instanceof JSONObject) {
            JSONObject object = (JSONObject) errors;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String message = extractErrorMessage(object.opt(keys.next()));
                if (message != null) {
                    return message;
                }
            }
            return null;
        }
        if (errors instanceof List<?>) {
            for (Object item : (List<?>) errors) {
                String message = extractErrorMessage(item);
                if (message != null) {
                    return message;
                }
            }
            return null;
        }
        if (errors instanceof Map<?, ?>) {
            for (Object value : ((Map<?, ?>) errors).values()) {
                String message = extractErrorMessage(value);
                if (message != null) {
                    return message;
                }
            }
            return null;
        }
        String fallback = String.valueOf(errors).trim();
        return fallback.isEmpty() ? null : fallback;
    }

    private String normalizeLoginErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("ten dang nhap hoac mat khau khong dung")
                || normalized.contains("sai thong tin dang nhap")
                || normalized.contains("validation error")
                || normalized.contains("invalid credentials")
                || normalized.contains("unauthorized")) {
            return "Sai thông tin đăng nhập";
        }
        return message;
    }

    private String buildFailureMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return "Không thể kết nối đến máy chủ.";
        }
        return message;
    }
}
