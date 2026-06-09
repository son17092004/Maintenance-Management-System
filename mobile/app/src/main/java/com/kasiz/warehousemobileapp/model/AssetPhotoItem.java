package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AssetPhotoItem {
    @SerializedName("photoId")
    public int photoId;

    @SerializedName("filePath")
    public String filePath;

    @SerializedName("caption")
    public String caption;

    @SerializedName("uploadedByName")
    public String uploadedByName;

    @SerializedName("createdAt")
    public String createdAt;
}