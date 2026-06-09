package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApprovalHistoryPayload {
    @SerializedName("logs")
    public List<ApprovalLogItem> logs;
}
