package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChecklistDetailItem {
    @SerializedName("checklistId")
    public int checklistId;

    @SerializedName("assetId")
    public int assetId;

    @SerializedName("assetName")
    public String assetName;

    @SerializedName("templateName")
    public String templateName;

    @SerializedName("checkerName")
    public String checkerName;

    @SerializedName("overallStatus")
    public String overallStatus;

    @SerializedName("reviewStatus")
    public String reviewStatus;

    @SerializedName("checkTime")
    public String checkTime;

    @SerializedName("notes")
    public String notes;

    @SerializedName("details")
    public List<ChecklistDetailRow> details;
}