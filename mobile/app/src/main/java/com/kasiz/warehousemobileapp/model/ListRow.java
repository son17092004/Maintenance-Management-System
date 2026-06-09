package com.kasiz.warehousemobileapp.model;

public class ListRow {
    public final long id;
    public final String kind;
    public final String title;
    public final String subtitle;
    public final String badge;
    public final String detail;

    public ListRow(long id, String kind, String title, String subtitle, String badge, String detail) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.detail = detail;
    }
}