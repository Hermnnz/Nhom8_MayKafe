package com.example.nhom8_makafe.data.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("auth/login/")
    Call<ApiEnvelope<LoginPayloadDto>> login(@Body LoginRequest request);

    @GET("auth/me/")
    Call<ApiEnvelope<UserDto>> me(@Header("Authorization") String authorization);

    @POST("auth/logout/")
    Call<ApiEnvelope<Object>> logout(@Header("Authorization") String authorization);

    @GET("catalog/categories/")
    Call<ApiEnvelope<List<CategoryDto>>> getCategories(@Header("Authorization") String authorization);

    @GET("catalog/products/")
    Call<ApiEnvelope<List<ProductDto>>> getProducts(
            @Header("Authorization") String authorization,
            @Query("search") String search,
            @Query("category") String category,
            @Query("available") Boolean available,
            @Query("page_size") Integer pageSize
    );

    @POST("catalog/products/")
    Call<ApiEnvelope<ProductDto>> createProduct(
            @Header("Authorization") String authorization,
            @Body ProductWriteRequest request
    );

    @PATCH("catalog/products/{id}/")
    Call<ApiEnvelope<ProductDto>> updateProduct(
            @Header("Authorization") String authorization,
            @Path("id") long productId,
            @Body ProductWriteRequest request
    );

    @PATCH("catalog/products/{id}/availability/")
    Call<ApiEnvelope<ProductDto>> toggleAvailability(
            @Header("Authorization") String authorization,
            @Path("id") long productId,
            @Body ToggleAvailabilityRequest request
    );

    @DELETE("catalog/products/{id}/")
    Call<ApiEnvelope<Object>> deleteProduct(
            @Header("Authorization") String authorization,
            @Path("id") long productId
    );

    @GET("orders/")
    Call<ApiEnvelope<List<OrderDto>>> getOrders(
            @Header("Authorization") String authorization,
            @Query("search") String search,
            @Query("date") String date,
            @Query("status") String status,
            @Query("page_size") Integer pageSize
    );

    @GET("orders/by-code/{code}/")
    Call<ApiEnvelope<OrderDto>> getOrderByCode(
            @Header("Authorization") String authorization,
            @Path("code") String code
    );

    @GET("orders/summary/")
    Call<ApiEnvelope<InvoiceSummaryDto>> getOrderSummary(
            @Header("Authorization") String authorization,
            @Query("date") String date
    );

    @POST("orders/checkout/")
    Call<ApiEnvelope<OrderDto>> checkout(
            @Header("Authorization") String authorization,
            @Body CheckoutRequest request
    );

    @GET("reports/dashboard/")
    Call<ApiEnvelope<DashboardDto>> getDashboard(
            @Header("Authorization") String authorization,
            @Query("period") String period
    );
}
