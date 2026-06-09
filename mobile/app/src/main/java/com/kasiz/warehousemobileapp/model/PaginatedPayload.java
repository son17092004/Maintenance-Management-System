package com.kasiz.warehousemobileapp.model;

import java.util.List;

public class PaginatedPayload<T> {
    public List<T> items;
    public int total;
    public int page;
    public int limit;
}