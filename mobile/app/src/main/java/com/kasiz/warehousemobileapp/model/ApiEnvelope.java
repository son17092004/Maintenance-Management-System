package com.kasiz.warehousemobileapp.model;

public class ApiEnvelope<T> {
    public boolean success;
    public T data;
    public String message;
}