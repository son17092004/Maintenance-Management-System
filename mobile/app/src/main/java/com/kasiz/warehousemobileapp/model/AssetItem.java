package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AssetItem {
    @SerializedName("assetId")
    public int assetId;

    @SerializedName("assetTypeId")
    public Integer assetTypeId;

    @SerializedName("assetName")
    public String assetName;

    @SerializedName("assetTypeName")
    public String assetTypeName;

    @SerializedName("locationName")
    public String locationName;

    @SerializedName("locationId")
    public Integer locationId;

    @SerializedName("status")
    public String status;

    @SerializedName("commissionDate")
    public String commissionDate;

    @SerializedName("serialNumber")
    public String serialNumber;

    @SerializedName("manufacturer")
    public String manufacturer;

    @SerializedName("model")
    public String model;

    @SerializedName("yearOfManufacture")
    public Integer yearOfManufacture;

    @SerializedName("technicalSpecs")
    public String technicalSpecs;

    @SerializedName("purchaseDate")
    public String purchaseDate;

    @SerializedName("warrantyDate")
    public String warrantyDate;

    @SerializedName("decommissionDate")
    public String decommissionDate;

    @SerializedName("photo")
    public String photo;

    @SerializedName("qrCodePath")
    public String qrCodePath;

    @SerializedName("description")
    public String description;

    @SerializedName("productionLineId")
    public Integer productionLineId;

    @SerializedName("productionLineName")
    public String productionLineName;

    @SerializedName("photos")
    public List<AssetPhotoItem> photos;
}