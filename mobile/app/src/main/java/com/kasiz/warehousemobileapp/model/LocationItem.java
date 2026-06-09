package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class LocationItem {
    @SerializedName("locationId")
    public int locationId;

    @SerializedName("locationName")
    public String locationName;

    @SerializedName("parentLocationName")
    public String parentLocationName;
}