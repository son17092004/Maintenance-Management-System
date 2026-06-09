package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("notiId")
    public int notiId;

    @SerializedName("message")
    public String message;

    @SerializedName("isRead")
    public boolean isRead;

    @SerializedName("createdAt")
    public String createdAt;
}