package com.Mediatron.Lab_Management.data_dto;

public class APIResponse<T> {
    private boolean success;
    private String message ;
    private T data;
    private Integer status;

    public APIResponse(boolean success, String message, T data) {
        this(success, message, data, null);
    }

    public APIResponse(boolean success, String message, T data, Integer status) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.status = status;
    }

    // Static helpers for convenience
    public static <T> APIResponse<T> ok(String message, T data) {
        return new APIResponse<>(true, message, data);
    }

    public static <T> APIResponse<T> error(String message) {
        return new APIResponse<>(false, message, null);
    }

    public static <T> APIResponse<T> error(String message, int status) {
        return new APIResponse<>(false, message, null, status);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Integer getStatus() { return status; }

}
