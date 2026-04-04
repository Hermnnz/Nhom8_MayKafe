package com.example.nhom8_makafe.data.api;

public interface ApiCallback<T> {
    void onSuccess(T data);

    void onError(String message);
}
