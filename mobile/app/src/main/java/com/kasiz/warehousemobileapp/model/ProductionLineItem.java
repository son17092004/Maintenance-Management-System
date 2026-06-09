package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ProductionLineItem {
    @SerializedName("lineId")
    public int lineId;

    @SerializedName("lineName")
    public String lineName;

    @SerializedName("description")
    public String description;

    @SerializedName("isActive")
    public Integer isActive;
}
