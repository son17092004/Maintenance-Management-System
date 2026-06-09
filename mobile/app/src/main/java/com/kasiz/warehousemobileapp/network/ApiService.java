package com.kasiz.warehousemobileapp.network;

import com.google.gson.JsonObject;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.AssetFormRequest;
import com.kasiz.warehousemobileapp.model.AssetItem;
import com.kasiz.warehousemobileapp.model.AssetPhotoItem;
import com.kasiz.warehousemobileapp.model.AssetQrResponse;
import com.kasiz.warehousemobileapp.model.AssetStatusRequest;
import com.kasiz.warehousemobileapp.model.AssetTypeItem;
import com.kasiz.warehousemobileapp.model.ChecklistDetailItem;
import com.kasiz.warehousemobileapp.model.ChecklistResultItem;
import com.kasiz.warehousemobileapp.model.HealthInfo;
import com.kasiz.warehousemobileapp.model.LoginRequest;
import com.kasiz.warehousemobileapp.model.LocationItem;
import com.kasiz.warehousemobileapp.model.PaginatedPayload;
import com.kasiz.warehousemobileapp.model.NotificationsPayload;
import com.kasiz.warehousemobileapp.model.WorkOrderItem;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/login")
    Call<ApiEnvelope<JsonObject>> login(@Body LoginRequest request);

    @GET("health")
    Call<ApiEnvelope<HealthInfo>> health();

    @GET("auth/me")
    Call<ApiEnvelope<com.kasiz.warehousemobileapp.model.MeProfile>> me();

    @POST("auth/logout")
    Call<ApiEnvelope<JsonObject>> logout();

    @GET("notifications")
    Call<ApiEnvelope<NotificationsPayload>> notifications(
            @Query("limit") int limit,
            @Query("offset") int offset
    );

    @PATCH("notifications/{id}/read")
    Call<ApiEnvelope<JsonObject>> markNotificationRead(@retrofit2.http.Path("id") int id);

    @PATCH("notifications/read-all")
    Call<ApiEnvelope<JsonObject>> markAllNotificationsRead();

    @GET("assets")
    Call<ApiEnvelope<PaginatedPayload<AssetItem>>> assets(
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("search") String search,
            @Query("status") String status,
            @Query("assetTypeId") String assetTypeId,
            @Query("locationId") String locationId,
            @Query("productionLine") String productionLine
    );

    @GET("assets/{id}")
    Call<ApiEnvelope<AssetItem>> assetById(@retrofit2.http.Path("id") int id);

    @POST("assets")
    Call<ApiEnvelope<AssetItem>> createAsset(@Body AssetFormRequest request);

    @PUT("assets/{id}")
    Call<ApiEnvelope<AssetItem>> updateAsset(@Path("id") int id, @Body AssetFormRequest request);

    @DELETE("assets/{id}")
    Call<ApiEnvelope<JsonObject>> deleteAsset(@Path("id") int id);

    @GET("assets/{id}/photos")
    Call<ApiEnvelope<List<AssetPhotoItem>>> assetPhotos(@retrofit2.http.Path("id") int id);

    @Multipart
    @POST("assets/{id}/photos")
    Call<ApiEnvelope<List<AssetPhotoItem>>> uploadAssetPhotos(@Path("id") int id, @Part List<MultipartBody.Part> photos);

    @DELETE("assets/{id}/photos/{photoId}")
    Call<ApiEnvelope<List<AssetPhotoItem>>> deleteAssetPhoto(@Path("id") int id, @Path("photoId") int photoId);

    @GET("assets/{id}/qr")
    Call<ApiEnvelope<AssetQrResponse>> assetQrBase64(@Path("id") int id, @Query("format") String format);

    @PATCH("assets/{id}/status")
    Call<ApiEnvelope<AssetItem>> updateAssetStatus(@Path("id") int id, @Body AssetStatusRequest request);

    @GET("asset-types/leaves")
    Call<ApiEnvelope<List<AssetTypeItem>>> assetTypeLeaves();

    @GET("locations")
    Call<ApiEnvelope<List<LocationItem>>> locations();

    @GET("production-lines")
    Call<ApiEnvelope<List<com.kasiz.warehousemobileapp.model.ProductionLineItem>>> productionLines();

    @GET("checklists/results")
    Call<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> checklistResults(
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("q") String q,
            @Query("overallStatus") String overallStatus,
            @Query("reviewStatus") String reviewStatus
    );

    @GET("checklists/results/{id}")
    Call<ApiEnvelope<ChecklistDetailItem>> checklistResultById(@retrofit2.http.Path("id") int id);

    // --- Work Orders ---
    @GET("work-orders")
    Call<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> workOrders(
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("status") String status,
            @Query("priority") String priority
    );

    @GET("work-orders/{id}")
    Call<ApiEnvelope<WorkOrderItem>> workOrderById(@Path("id") int id);

    @GET("approvals/history/{resourceType}/{resourceId}")
    Call<ApiEnvelope<com.kasiz.warehousemobileapp.model.ApprovalHistoryPayload>> approvalHistory(
            @Path("resourceType") String resourceType,
            @Path("resourceId") int resourceId
    );

    @PATCH("work-orders/{id}/status")
    Call<ApiEnvelope<WorkOrderItem>> changeWorkOrderStatus(@Path("id") int id, @Body JsonObject body);

    @PATCH("work-orders/{id}/closure-notes")
    Call<ApiEnvelope<WorkOrderItem>> saveWorkOrderClosureNotesDraft(@Path("id") int id, @Body JsonObject body);

    @Multipart
    @POST("work-orders/{id}/photos")
    Call<ApiEnvelope<List<WorkOrderItem.Photo>>> uploadWorkOrderPhotos(@Path("id") int id, @Part List<MultipartBody.Part> photos);

    @DELETE("work-orders/{id}/photos/{photoId}")
    Call<ApiEnvelope<List<WorkOrderItem.Photo>>> deleteWorkOrderPhoto(@Path("id") int id, @Path("photoId") int photoId);

    @POST("work-orders/{id}/counter-reset-baseline")
    Call<ApiEnvelope<JsonObject>> resetWorkOrderRuntimeBaseline(@Path("id") int id);

    // --- Asset Readings / Counter ---
    @POST("assets/{id}/readings")
    Call<ApiEnvelope<JsonObject>> recordAssetReading(@Path("id") int id, @Body JsonObject body);

    // --- Checklist QR & Submission ---
    @GET("checklists/qr/{assetId}")
    Call<ApiEnvelope<JsonObject>> getChecklistQrInfo(@Path("assetId") String assetId, @Query("woId") Integer woId);

    @GET("assets/{id}/maintenance-history")
    Call<ApiEnvelope<List<JsonObject>>> assetMaintenanceHistory(
            @Path("id") int id,
            @Query("limit") int limit
    );

    @Multipart
    @POST("checklists/results")
    Call<ApiEnvelope<JsonObject>> submitChecklist(
            @Part List<MultipartBody.Part> parts
    );
}