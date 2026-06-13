package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WorkOrderItem {
    @SerializedName("woId")
    public int woId;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("status")
    public String status;

    @SerializedName("priority")
    public String priority;

    @SerializedName("woSource")
    public String woSource;

    @SerializedName("assetId")
    public int assetId;

    @SerializedName("assetName")
    public String assetName;

    @SerializedName("assetTypeName")
    public String assetTypeName;

    @SerializedName("locationName")
    public String locationName;

    @SerializedName("actualDate")
    public String actualDate;

    @SerializedName("plannedDate")
    public String plannedDate;

    @SerializedName("estimatedHours")
    public Double estimatedHours;

    @SerializedName("actualHours")
    public Double actualHours;

    @SerializedName("closureFieldNotes")
    public String closureFieldNotes;

    @SerializedName("closurePartsNotes")
    public String closurePartsNotes;

    @SerializedName("requiresShutdown")
    public int requiresShutdown;

    @SerializedName("shutdownReason")
    public String shutdownReason;

    @SerializedName("powerState")
    public String powerState;

    @SerializedName("counterBaselineResetAt")
    public String counterBaselineResetAt;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("assignments")
    public List<Assignment> assignments;

    @SerializedName("photos")
    public List<Photo> photos;

    @SerializedName("woLinkedChecklists")
    public List<ChecklistResultItem> woLinkedChecklists;

    @SerializedName("recentChecklists")
    public List<ChecklistResultItem> recentChecklists;

    @SerializedName("checklistRequirements")
    public List<ChecklistRequirement> checklistRequirements;

    @SerializedName("checklistRequirementsMet")
    public Boolean checklistRequirementsMet;

    public static class ChecklistRequirement {
        @SerializedName("templateId")
        public int templateId;

        @SerializedName("templateName")
        public String templateName;

        @SerializedName("status")
        public String status;

        @SerializedName("dueDate")
        public String dueDate;

        @SerializedName("slotMissing")
        public boolean slotMissing;
    }

    public static class Assignment {
        @SerializedName("employeeId")
        public int employeeId;

        @SerializedName("fullName")
        public String fullName;

        @SerializedName("positionName")
        public String positionName;

        @SerializedName("specialty")
        public String specialty;

        @SerializedName("craftLevel")
        public String craftLevel;

        @SerializedName("isGroupLeader")
        public int isGroupLeader;
    }

    public static class Photo {
        @SerializedName("photoId")
        public int photoId;

        @SerializedName("filePath")
        public String filePath;

        @SerializedName("uploadedAt")
        public String uploadedAt;
    }
}
