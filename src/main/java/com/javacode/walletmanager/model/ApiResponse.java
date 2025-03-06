// This is a personal academic project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: https://pvs-studio.com
package com.javacode.walletmanager.model;

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    public ApiResponse() {
        this.success = true;
        this.data = null;
        this.message = "successfully";
    }

    public ApiResponse(T data) {
        this.success = true;
        this.message = null;
        this.data = data;
    }

    public ApiResponse(String message) {
        this.success = false;
        this.message = message;
        this.data = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
