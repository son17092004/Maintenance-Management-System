package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {
    @SerializedName("notiId")
    public int notiId;

    @SerializedName("message")
    public String message;

    @SerializedName("isRead")
    public Integer isRead;

    @SerializedName("createdAt")
    public String createdAt;

    public boolean isRead() {
        return isRead != null && isRead == 1;
    }
}