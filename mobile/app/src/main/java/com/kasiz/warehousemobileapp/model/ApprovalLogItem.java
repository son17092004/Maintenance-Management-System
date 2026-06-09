package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ApprovalLogItem {
    @SerializedName("logId")
    public int logId;

    @SerializedName("resourceId")
    public int resourceId;

    @SerializedName("resourceType")
    public String resourceType;

    @SerializedName("currentLevel")
    public int currentLevel;

    @SerializedName("status")
    public String status;

    @SerializedName("comment")
    public String comment;

    @SerializedName("actionDate")
    public String actionDate;

    @SerializedName("approverName")
    public String approverName;

    @SerializedName("stepPositionName")
    public String stepPositionName;
}
