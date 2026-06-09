package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AssetQrResponse {
    @SerializedName("assetId")
    public int assetId;

    @SerializedName("assetName")
    public String assetName;

    @SerializedName("qrPayload")
    public String qrPayload;

    @SerializedName("dataUrl")
    public String dataUrl;
}