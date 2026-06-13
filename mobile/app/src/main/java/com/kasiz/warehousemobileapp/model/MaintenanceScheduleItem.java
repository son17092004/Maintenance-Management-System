package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class MaintenanceScheduleItem {

    @SerializedName("scheduleId")
    public int scheduleId;

    @SerializedName("assetId")
    public int assetId;

    @SerializedName("assetName")
    public String assetName;

    @SerializedName("assetTypeId")
    public Integer assetTypeId;

    @SerializedName("locationId")
    public Integer locationId;

    @SerializedName("locationName")
    public String locationName;

    @SerializedName("assetTypeName")
    public String assetTypeName;

    @SerializedName("scheduleName")
    public String scheduleName;

    @SerializedName("maintenanceType")
    public String maintenanceType;

    @SerializedName("description")
    public String description;

    @SerializedName("frequencyValue")
    public Integer frequencyValue;

    @SerializedName("frequencyUnit")
    public String frequencyUnit;

    @SerializedName("startDate")
    public String startDate;

    @SerializedName("nextDueDate")
    public String nextDueDate;

    @SerializedName("lastExecutedDate")
    public String lastExecutedDate;

    @SerializedName("endDate")
    public String endDate;

    @SerializedName("estimatedTime")
    public Double estimatedTime;

    @SerializedName("priority")
    public String priority;

    @SerializedName("status")
    public String status;

    @SerializedName("checklistTemplateId")
    public Integer checklistTemplateId;

    @SerializedName("checklistTemplateName")
    public String checklistTemplateName;
}
