package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ChecklistResultItem {
    @SerializedName("checklistId")
    public int checklistId;

    @SerializedName("assetId")
    public int assetId;

    @SerializedName("assetName")
    public String assetName;

    @SerializedName("overallStatus")
    public String overallStatus;

    @SerializedName("reviewStatus")
    public String reviewStatus;

    @SerializedName("checkTime")
    public String checkTime;

    @SerializedName("checkerName")
    public String checkerName;

    @SerializedName("templateName")
    public String templateName;

    @SerializedName("readingValue")
    public Double readingValue;

    @SerializedName("notes")
    public String notes;
}