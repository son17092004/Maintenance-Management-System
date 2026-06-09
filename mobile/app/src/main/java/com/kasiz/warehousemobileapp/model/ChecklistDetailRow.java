package com.kasiz.warehousemobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ChecklistDetailRow {
    @SerializedName("detailId")
    public int detailId;

    @SerializedName("questionText")
    public String questionText;

    @SerializedName("inputType")
    public String inputType;

    @SerializedName("answerValue")
    public String answerValue;

    @SerializedName("isOK")
    public boolean isOK;
}