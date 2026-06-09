package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AssetTypeItem {
    @SerializedName("assetTypeId")
    public int assetTypeId;

    @SerializedName("typeName")
    public String typeName;

    @SerializedName("parentTypeName")
    public String parentTypeName;
}