package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MeProfile {
    @SerializedName("employeeId")
    public int employeeId;
    @SerializedName("fullName")
    public String fullName;
    @SerializedName("username")
    public String username;
    @SerializedName("email")
    public String email;
    @SerializedName("phone")
    public String phone;
    @SerializedName("positionName")
    public String positionName;
    @SerializedName("departmentName")
    public String departmentName;
    @SerializedName("positionId")
    public int positionId;
    @SerializedName("positionLevel")
    public int positionLevel;
    @SerializedName("craftLevel")
    public String craftLevel;
    @SerializedName("specialty")
    public String specialty;
    @SerializedName("photoPath")
    public String photoPath;
    @SerializedName("isActive")
    public Object isActive;
    @SerializedName("emailVerified")
    public Object emailVerified;
    @SerializedName("fieldWorkSummary")
    public FieldWorkSummary fieldWorkSummary;
    @SerializedName("permissions")
    public List<PermissionItem> permissions;

    public static class FieldWorkSummary {
        @SerializedName("availability")
        public String availability;
        @SerializedName("headline")
        public String headline;
        @SerializedName("detail")
        public String detail;
    }

    public static class PermissionItem {
        @SerializedName("resourceType")
        public String resourceType;
        @SerializedName("permissionName")
        public String permissionName;
    }
}