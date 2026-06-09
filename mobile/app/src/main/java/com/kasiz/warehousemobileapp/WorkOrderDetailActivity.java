package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.WorkOrderItem;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;
import com.kasiz.warehousemobileapp.ui.WorkOrderPhotoAdapter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkOrderDetailActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_PHOTOS = 101;

    private int woId;
    private WorkOrderItem currentItem;

    private TextView textWoCode, textTitle, textStatus, textAsset;
    private TextView textDescription, textPriority, textSource, textPlannedDate, textEstimatedHours;
    private TextView textNoPhotos;
    private TextView textLocation, textAssetType, textActualDate, textActualHours, textRequiresShutdown, textPowerState;
    private android.widget.LinearLayout layoutAssignees;
    private View cardExecution, layoutActions, layoutRunningActions;
    private View cardSubmittedChecklists, cardRecentChecklists, cardApprovalHistory;
    private android.widget.LinearLayout layoutSubmittedChecklists, layoutRecentChecklists, layoutApprovalHistory;
    private Button buttonStart, buttonPause, buttonSaveDraft, buttonComplete, buttonResume, buttonChecklistExec, buttonResetBaseline, buttonUploadPhoto;
    private TextInputEditText editFieldNotes, editPartsNotes, editActualHours, editShutdownReason;
    private TextInputLayout layoutShutdownReason;
    private CheckBox checkShutdown;
    private ProgressBar progressBar;
    private RecyclerView recyclerPhotos;
    private WorkOrderPhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_order_detail);

        woId = getIntent().getIntExtra("extra_wo_id", 0);
        if (woId == 0) {
            Toast.makeText(this, "Phiếu việc không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadDetail();
    }

    private void initViews() {
        textWoCode = findViewById(R.id.textWoCode);
        textTitle = findViewById(R.id.textTitle);
        textStatus = findViewById(R.id.textStatus);
        textAsset = findViewById(R.id.textAsset);
        textDescription = findViewById(R.id.textDescription);
        textPriority = findViewById(R.id.textPriority);
        textSource = findViewById(R.id.textSource);
        textPlannedDate = findViewById(R.id.textPlannedDate);
        textEstimatedHours = findViewById(R.id.textEstimatedHours);
        textNoPhotos = findViewById(R.id.textNoPhotos);

        textLocation = findViewById(R.id.textLocation);
        textAssetType = findViewById(R.id.textAssetType);
        textActualDate = findViewById(R.id.textActualDate);
        textActualHours = findViewById(R.id.textActualHours);
        textRequiresShutdown = findViewById(R.id.textRequiresShutdown);
        textPowerState = findViewById(R.id.textPowerState);
        layoutAssignees = findViewById(R.id.layoutAssignees);

        cardSubmittedChecklists = findViewById(R.id.cardSubmittedChecklists);
        cardRecentChecklists = findViewById(R.id.cardRecentChecklists);
        cardApprovalHistory = findViewById(R.id.cardApprovalHistory);
        layoutSubmittedChecklists = findViewById(R.id.layoutSubmittedChecklists);
        layoutRecentChecklists = findViewById(R.id.layoutRecentChecklists);
        layoutApprovalHistory = findViewById(R.id.layoutApprovalHistory);

        cardExecution = findViewById(R.id.cardExecution);
        layoutActions = findViewById(R.id.layoutActions);
        layoutRunningActions = findViewById(R.id.layoutRunningActions);

        buttonStart = findViewById(R.id.buttonStart);
        buttonPause = findViewById(R.id.buttonPause);
        buttonSaveDraft = findViewById(R.id.buttonSaveDraft);
        buttonComplete = findViewById(R.id.buttonComplete);
        buttonResume = findViewById(R.id.buttonResume);
        buttonChecklistExec = findViewById(R.id.buttonChecklistExec);
        buttonResetBaseline = findViewById(R.id.buttonResetBaseline);
        buttonUploadPhoto = findViewById(R.id.buttonUploadPhoto);

        editFieldNotes = findViewById(R.id.editFieldNotes);
        editPartsNotes = findViewById(R.id.editPartsNotes);
        editActualHours = findViewById(R.id.editActualHours);
        editShutdownReason = findViewById(R.id.editShutdownReason);
        layoutShutdownReason = findViewById(R.id.layoutShutdownReason);
        checkShutdown = findViewById(R.id.checkShutdown);
        progressBar = findViewById(R.id.progressBar);
        recyclerPhotos = findViewById(R.id.recyclerPhotos);

        cardSubmittedChecklists = findViewById(R.id.cardSubmittedChecklists);
        cardRecentChecklists = findViewById(R.id.cardRecentChecklists);
        layoutSubmittedChecklists = findViewById(R.id.layoutSubmittedChecklists);
        layoutRecentChecklists = findViewById(R.id.layoutRecentChecklists);

        recyclerPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new WorkOrderPhotoAdapter(this, new ArrayList<>(), photo -> deletePhoto(photo), SessionManager.getInstance(this).getBaseUrl().replace("/api/", ""));
        recyclerPhotos.setAdapter(adapter);

        checkShutdown.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutShutdownReason.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        buttonStart.setOnClickListener(v -> updateStatus("IN_PROGRESS"));
        buttonPause.setOnClickListener(v -> updateStatus("PAUSED"));
        buttonResume.setOnClickListener(v -> updateStatus("IN_PROGRESS"));

        buttonSaveDraft.setOnClickListener(v -> saveDraft());
        buttonComplete.setOnClickListener(v -> completeWork());
        buttonResetBaseline.setOnClickListener(v -> resetBaseline());

        buttonChecklistExec.setOnClickListener(v -> {
            if (currentItem != null) {
                Intent intent = new Intent(this, ChecklistExecutionActivity.class);
                intent.putExtra("extra_asset_id", String.valueOf(currentItem.assetId));
                intent.putExtra("extra_wo_id", currentItem.woId);
                startActivity(intent);
            }
        });

        buttonUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh minh chứng"), REQUEST_PICK_PHOTOS);
        });
    }

    private void loadDetail() {
        setLoading(true);
        ApiClient.getService(this).workOrderById(woId).enqueue(new Callback<ApiEnvelope<WorkOrderItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<WorkOrderItem>> call, Response<ApiEnvelope<WorkOrderItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    currentItem = response.body().data;
                    renderDetail(currentItem);
                    loadApprovalHistory();
                } else {
                    Toast.makeText(WorkOrderDetailActivity.this, "Không tải được chi tiết phiếu việc", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<WorkOrderItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(WorkOrderDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderDetail(WorkOrderItem item) {
        textWoCode.setText("WO-" + String.format("%04d", item.woId));
        textTitle.setText(safe(item.description));
        
        textStatus.setText(getStatusLabel(item.status));
        textStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getStatusColor(item.status)));

        textPriority.setText(getPriorityLabel(item.priority));
        textPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getPriorityColor(item.priority)));

        textAsset.setText(safe(item.assetName) + " (ID: " + item.assetId + ")");
        textLocation.setText(safe(item.locationName).isEmpty() ? "--" : safe(item.locationName));
        textAssetType.setText(safe(item.assetTypeName).isEmpty() ? "--" : safe(item.assetTypeName));
        textDescription.setText(safe(item.description).isEmpty() ? "Không có mô tả" : safe(item.description));
        textSource.setText(safe(item.woSource));
        textPlannedDate.setText(safe(item.plannedDate));
        textActualDate.setText(safe(item.actualDate).isEmpty() ? "--" : safe(item.actualDate));
        textEstimatedHours.setText(item.estimatedHours != null ? item.estimatedHours + " giờ" : "--");
        textActualHours.setText(item.actualHours != null ? item.actualHours + " giờ" : "--");

        if (item.requiresShutdown == 1) {
            textRequiresShutdown.setText("Có dừng máy");
            textRequiresShutdown.setTextColor(0xFFEF4444); // Red
        } else {
            textRequiresShutdown.setText("Không dừng máy");
            textRequiresShutdown.setTextColor(0xFF10B981); // Green
        }
        
        textPowerState.setText(safe(item.powerState).isEmpty() ? "--" : safe(item.powerState));

        // Render Assignees dynamically
        layoutAssignees.removeAllViews();
        if (item.assignments != null && !item.assignments.isEmpty()) {
            for (WorkOrderItem.Assignment a : item.assignments) {
                View assView = getLayoutInflater().inflate(R.layout.item_assignee, layoutAssignees, false);
                TextView textInitials = assView.findViewById(R.id.textInitials);
                TextView textAssigneeName = assView.findViewById(R.id.textAssigneeName);
                TextView textLeaderBadge = assView.findViewById(R.id.textLeaderBadge);
                TextView textAssigneeRole = assView.findViewById(R.id.textAssigneeRole);

                String name = safe(a.fullName);
                textAssigneeName.setText(name);
                
                // Set initials
                if (!name.isEmpty()) {
                    String[] parts = name.split("\\s+");
                    String initials = "";
                    if (parts.length > 0) {
                        String lastWord = parts[parts.length - 1];
                        if (!lastWord.isEmpty()) {
                            initials = lastWord.substring(0, 1).toUpperCase();
                        }
                    }
                    textInitials.setText(initials.isEmpty() ? "A" : initials);
                } else {
                    textInitials.setText("-");
                }

                // Show leader badge
                if (a.isGroupLeader == 1) {
                    textLeaderBadge.setVisibility(View.VISIBLE);
                } else {
                    textLeaderBadge.setVisibility(View.GONE);
                }

                // Subtitle: Position • Specialty • CraftLevel
                StringBuilder sub = new StringBuilder();
                if (a.positionName != null && !a.positionName.trim().isEmpty()) {
                    sub.append(a.positionName.trim());
                }
                if (a.specialty != null && !a.specialty.trim().isEmpty()) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append(a.specialty.trim());
                }
                if (a.craftLevel != null && !a.craftLevel.trim().isEmpty()) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append(a.craftLevel.trim());
                }
                textAssigneeRole.setText(sub.toString());

                layoutAssignees.addView(assView);
            }
        } else {
            View emptyView = getLayoutInflater().inflate(R.layout.item_assignee, layoutAssignees, false);
            emptyView.findViewById(R.id.textInitials).setVisibility(View.GONE);
            emptyView.findViewById(R.id.textLeaderBadge).setVisibility(View.GONE);
            TextView nameTv = emptyView.findViewById(R.id.textAssigneeName);
            nameTv.setText("Chưa có nhân viên phụ trách");
            nameTv.setTextColor(0xFF9CA3AF);
            emptyView.findViewById(R.id.textAssigneeRole).setVisibility(View.GONE);
            layoutAssignees.addView(emptyView);
        }

        // Render Photos
        List<WorkOrderItem.Photo> photos = item.photos == null ? new ArrayList<>() : item.photos;
        textNoPhotos.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.update(photos);

        // Control Execution UI
        String status = item.status;
        boolean isRunningOrPaused = "IN_PROGRESS".equals(status) || "PAUSED".equals(status);

        cardExecution.setVisibility(isRunningOrPaused ? View.VISIBLE : View.GONE);
        buttonUploadPhoto.setVisibility(isRunningOrPaused ? View.VISIBLE : View.GONE);

        buttonStart.setVisibility("WAITING".equals(status) ? View.VISIBLE : View.GONE);
        layoutRunningActions.setVisibility("IN_PROGRESS".equals(status) ? View.VISIBLE : View.GONE);
        buttonResume.setVisibility("PAUSED".equals(status) ? View.VISIBLE : View.GONE);

        boolean isCorrective = "CORRECTIVE".equalsIgnoreCase(item.woSource);
        buttonResetBaseline.setVisibility((isRunningOrPaused && isCorrective) ? View.VISIBLE : View.GONE);

        // Show Checklist button if it has schedule / template, or generally in progress
        buttonChecklistExec.setVisibility(isRunningOrPaused ? View.VISIBLE : View.GONE);

        if (isRunningOrPaused) {
            editFieldNotes.setText(safe(item.closureFieldNotes));
            editPartsNotes.setText(safe(item.closurePartsNotes));
            editActualHours.setText(item.actualHours != null ? String.valueOf(item.actualHours) : "");
            checkShutdown.setChecked(item.requiresShutdown == 1);
            editShutdownReason.setText(safe(item.shutdownReason));
            layoutShutdownReason.setVisibility(item.requiresShutdown == 1 ? View.VISIBLE : View.GONE);
        }

        // Render Submitted Checklists
        layoutSubmittedChecklists.removeAllViews();
        if (item.woLinkedChecklists != null && !item.woLinkedChecklists.isEmpty()) {
            cardSubmittedChecklists.setVisibility(View.VISIBLE);
            for (com.kasiz.warehousemobileapp.model.ChecklistResultItem cl : item.woLinkedChecklists) {
                View clView = getLayoutInflater().inflate(R.layout.item_linked_checklist, layoutSubmittedChecklists, false);
                TextView textStatusBadges = clView.findViewById(R.id.textStatusBadges);
                TextView textMeta = clView.findViewById(R.id.textMeta);
                TextView textTemplate = clView.findViewById(R.id.textTemplate);

                textStatusBadges.setText("[" + safe(cl.overallStatus) + "] [" + getReviewStatusLabel(cl.reviewStatus) + "]");
                textMeta.setText("#" + cl.checklistId + " • " + safe(cl.checkerName) + " • " + safe(cl.checkTime));
                if (cl.templateName != null && !cl.templateName.trim().isEmpty()) {
                    textTemplate.setVisibility(View.VISIBLE);
                    textTemplate.setText("Mẫu: " + cl.templateName);
                } else {
                    textTemplate.setVisibility(View.GONE);
                }

                clView.setOnClickListener(v -> {
                    Intent intent = new Intent(WorkOrderDetailActivity.this, ChecklistDetailActivity.class);
                    intent.putExtra(ChecklistDetailActivity.EXTRA_CHECKLIST_ID, cl.checklistId);
                    startActivity(intent);
                });

                layoutSubmittedChecklists.addView(clView);
            }
        } else {
            cardSubmittedChecklists.setVisibility(View.GONE);
        }

        // Render Recent Checklists
        layoutRecentChecklists.removeAllViews();
        if (item.recentChecklists != null && !item.recentChecklists.isEmpty()) {
            cardRecentChecklists.setVisibility(View.VISIBLE);
            for (com.kasiz.warehousemobileapp.model.ChecklistResultItem c : item.recentChecklists) {
                View clView = getLayoutInflater().inflate(R.layout.item_recent_checklist, layoutRecentChecklists, false);
                TextView textStatusBadge = clView.findViewById(R.id.textStatusBadge);
                TextView textMeta = clView.findViewById(R.id.textMeta);
                TextView textReading = clView.findViewById(R.id.textReading);
                TextView textNotes = clView.findViewById(R.id.textNotes);

                textStatusBadge.setText("[" + safe(c.overallStatus) + "]");
                textMeta.setText("#" + c.checklistId + " • " + safe(c.checkTime) + (c.checkerName != null ? " • " + c.checkerName : ""));
                
                if (c.readingValue != null) {
                    textReading.setVisibility(View.VISIBLE);
                    textReading.setText("Đồng hồ: " + c.readingValue + " h");
                } else {
                    textReading.setVisibility(View.GONE);
                }

                if (c.notes != null && !c.notes.trim().isEmpty()) {
                    textNotes.setText(c.notes);
                    textNotes.setTextColor(0xFF1F2937); // Dark gray
                } else {
                    textNotes.setText("Không có ghi chú hiện trường.");
                    textNotes.setTextColor(0xFF9CA3AF); // Light gray
                }

                clView.setOnClickListener(v -> {
                    Intent intent = new Intent(WorkOrderDetailActivity.this, ChecklistDetailActivity.class);
                    intent.putExtra(ChecklistDetailActivity.EXTRA_CHECKLIST_ID, c.checklistId);
                    startActivity(intent);
                });

                layoutRecentChecklists.addView(clView);
            }
        } else {
            cardRecentChecklists.setVisibility(View.GONE);
        }
    }

    private void updateStatus(String status) {
        setLoading(true);
        JsonObject body = new JsonObject();
        body.addProperty("status", status);

        ApiClient.getService(this).changeWorkOrderStatus(woId, body).enqueue(new Callback<ApiEnvelope<WorkOrderItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<WorkOrderItem>> call, Response<ApiEnvelope<WorkOrderItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(WorkOrderDetailActivity.this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    Toast.makeText(WorkOrderDetailActivity.this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<WorkOrderItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(WorkOrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDraft() {
        setLoading(true);
        JsonObject body = new JsonObject();
        body.addProperty("closureFieldNotes", getEditTextValue(editFieldNotes));
        body.addProperty("closurePartsNotes", getEditTextValue(editPartsNotes));

        ApiClient.getService(this).saveWorkOrderClosureNotesDraft(woId, body).enqueue(new Callback<ApiEnvelope<WorkOrderItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<WorkOrderItem>> call, Response<ApiEnvelope<WorkOrderItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(WorkOrderDetailActivity.this, "Đã lưu nháp ghi chú/vật tư", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    Toast.makeText(WorkOrderDetailActivity.this, "Lưu nháp thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<WorkOrderItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(WorkOrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeWork() {
        String fieldNotes = getEditTextValue(editFieldNotes);
        if (TextUtils.isEmpty(fieldNotes)) {
            Toast.makeText(this, "Vui lòng nhập ghi chú hiện trường / việc đã làm", Toast.LENGTH_LONG).show();
            return;
        }

        String actualHoursStr = getEditTextValue(editActualHours);
        Double actualHours = null;
        if (!TextUtils.isEmpty(actualHoursStr)) {
            try {
                actualHours = Double.parseDouble(actualHoursStr);
                if (actualHours < 0) {
                    Toast.makeText(this, "Số giờ chạy thực tế không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Số giờ chạy thực tế không phải là số hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        boolean requiresShutdown = checkShutdown.isChecked();
        String shutdownReason = getEditTextValue(editShutdownReason);

        setLoading(true);
        JsonObject body = new JsonObject();
        body.addProperty("status", "AWAITING_CLOSURE");
        body.addProperty("closureFieldNotes", fieldNotes);
        body.addProperty("closurePartsNotes", getEditTextValue(editPartsNotes));
        if (actualHours != null) {
            body.addProperty("actualHours", actualHours);
        }
        body.addProperty("requiresShutdown", requiresShutdown);
        if (requiresShutdown) {
            body.addProperty("shutdownReason", shutdownReason);
        }

        ApiClient.getService(this).changeWorkOrderStatus(woId, body).enqueue(new Callback<ApiEnvelope<WorkOrderItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<WorkOrderItem>> call, Response<ApiEnvelope<WorkOrderItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(WorkOrderDetailActivity.this, "Đã nộp kết quả công việc. Chờ nghiệm thu.", Toast.LENGTH_LONG).show();
                    loadDetail();
                } else {
                    Toast.makeText(WorkOrderDetailActivity.this, "Thao tác thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<WorkOrderItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(WorkOrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetBaseline() {
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật mốc giờ chạy")
                .setMessage("Cập nhật mốc “sau bảo trì” theo tổng giờ chạy hiện tại của máy? Lịch bảo trì theo giờ sẽ tính lại từ mốc này.")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    setLoading(true);
                    ApiClient.getService(WorkOrderDetailActivity.this).resetWorkOrderRuntimeBaseline(woId).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
                        @Override
                        public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(WorkOrderDetailActivity.this, "Đã cập nhật mốc giờ chạy cho dự báo", Toast.LENGTH_SHORT).show();
                                loadDetail();
                            } else {
                                Toast.makeText(WorkOrderDetailActivity.this, "Không cập nhật được mốc giờ chạy", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                            setLoading(false);
                            Toast.makeText(WorkOrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deletePhoto(WorkOrderItem.Photo photo) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa ảnh minh chứng")
                .setMessage("Bạn có chắc chắn muốn xóa ảnh này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    setLoading(true);
                    ApiClient.getService(WorkOrderDetailActivity.this).deleteWorkOrderPhoto(woId, photo.photoId).enqueue(new Callback<ApiEnvelope<List<WorkOrderItem.Photo>>>() {
                        @Override
                        public void onResponse(Call<ApiEnvelope<List<WorkOrderItem.Photo>>> call, Response<ApiEnvelope<List<WorkOrderItem.Photo>>> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(WorkOrderDetailActivity.this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show();
                                loadDetail();
                            } else {
                                Toast.makeText(WorkOrderDetailActivity.this, "Xóa ảnh thất bại", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiEnvelope<List<WorkOrderItem.Photo>>> call, Throwable t) {
                            setLoading(false);
                            Toast.makeText(WorkOrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void uploadPhotos(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri uri : uris) {
            MultipartBody.Part part = createPhotoPart(uri);
            if (part != null) {
                parts.add(part);
            }
        }
        if (parts.isEmpty()) return;

        setLoading(true);
        ApiClient.getService(this).uploadWorkOrderPhotos(woId, parts).enqueue(new Callback<ApiEnvelope<List<WorkOrderItem.Photo>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<WorkOrderItem.Photo>>> call, Response<ApiEnvelope<List<WorkOrderItem.Photo>>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(WorkOrderDetailActivity.this, "Đã tải ảnh lên", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    Toast.makeText(WorkOrderDetailActivity.this, "Không tải được ảnh lên", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<WorkOrderItem.Photo>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(WorkOrderDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MultipartBody.Part createPhotoPart(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            byte[] bytes = readAllBytes(inputStream);
            String name = getFileName(uri);
            String mime = getContentResolver().getType(uri);
            if (mime == null || mime.trim().isEmpty()) mime = "image/jpeg";
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mime));
            return MultipartBody.Part.createFormData("photos", name == null ? "photo.jpg" : name, requestBody);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private String getFileName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return "photo.jpg";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PHOTOS && resultCode == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            uploadPhotos(uris);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }

    private String getEditTextValue(TextInputEditText edt) {
        return edt.getText() == null ? "" : edt.getText().toString().trim();
    }

    private String getStatusLabel(String status) {
        if ("WAITING".equals(status)) return "Chờ thực hiện";
        if ("RUNNING".equals(status) || "IN_PROGRESS".equals(status)) return "Đang thực hiện";
        if ("PAUSED".equals(status)) return "Đang tạm dừng";
        if ("AWAITING_CLOSURE".equals(status)) return "Chờ nghiệm thu";
        if ("COMPLETED".equals(status)) return "Đã hoàn thành";
        if ("CANCELLED".equals(status)) return "Đã hủy";
        if ("PENDING_APPROVAL".equals(status)) return "Chờ phê duyệt";
        return status;
    }

    private int getStatusColor(String status) {
        if ("WAITING".equals(status)) return 0xFF64748B;
        if ("RUNNING".equals(status) || "IN_PROGRESS".equals(status)) return 0xFF3B82F6;
        if ("PAUSED".equals(status)) return 0xFFF59E0B;
        if ("AWAITING_CLOSURE".equals(status)) return 0xFF8B5CF6;
        if ("COMPLETED".equals(status)) return 0xFF10B981;
        if ("CANCELLED".equals(status)) return 0xFFEF4444;
        if ("PENDING_APPROVAL".equals(status)) return 0xFFF59E0B;
        return 0xFF64748B;
    }

    private String getPriorityLabel(String priority) {
        if ("LOW".equals(priority)) return "Thấp";
        if ("MEDIUM".equals(priority)) return "Trung bình";
        if ("HIGH".equals(priority)) return "Cao";
        if ("URGENT".equals(priority)) return "Khẩn cấp";
        if ("EMERGENCY".equals(priority)) return "Khẩn cấp";
        return safe(priority);
    }

    private int getPriorityColor(String priority) {
        if ("LOW".equals(priority)) return 0xFF10B981;
        if ("MEDIUM".equals(priority)) return 0xFF3B82F6;
        if ("HIGH".equals(priority)) return 0xFFF59E0B;
        if ("URGENT".equals(priority) || "EMERGENCY".equals(priority)) return 0xFFEF4444;
        return 0xFF3B82F6;
    }

    private String getReviewStatusLabel(String status) {
        if ("PENDING".equals(status)) return "Chờ duyệt";
        if ("APPROVED".equals(status)) return "Đã duyệt";
        if ("REJECTED".equals(status)) return "Từ chối";
        return safe(status);
    }

    private void loadApprovalHistory() {
        ApiClient.getService(this).approvalHistory("WORK_ORDER", woId)
                .enqueue(new Callback<ApiEnvelope<com.kasiz.warehousemobileapp.model.ApprovalHistoryPayload>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<com.kasiz.warehousemobileapp.model.ApprovalHistoryPayload>> call,
                                   Response<ApiEnvelope<com.kasiz.warehousemobileapp.model.ApprovalHistoryPayload>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    renderApprovalHistory(response.body().data.logs);
                } else {
                    cardApprovalHistory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<com.kasiz.warehousemobileapp.model.ApprovalHistoryPayload>> call, Throwable t) {
                cardApprovalHistory.setVisibility(View.GONE);
            }
        });
    }

    private void renderApprovalHistory(List<com.kasiz.warehousemobileapp.model.ApprovalLogItem> logs) {
        layoutApprovalHistory.removeAllViews();
        if (logs != null && !logs.isEmpty()) {
            cardApprovalHistory.setVisibility(View.VISIBLE);
            for (com.kasiz.warehousemobileapp.model.ApprovalLogItem log : logs) {
                View logView = getLayoutInflater().inflate(R.layout.item_approval_log, layoutApprovalHistory, false);
                TextView textStepName = logView.findViewById(R.id.textStepName);
                TextView textActionDate = logView.findViewById(R.id.textActionDate);
                TextView textApproverInfo = logView.findViewById(R.id.textApproverInfo);
                TextView textApprovalStatusBadge = logView.findViewById(R.id.textApprovalStatusBadge);
                TextView textApprovalComment = logView.findViewById(R.id.textApprovalComment);

                textStepName.setText("Bước " + log.currentLevel + " - " + safe(log.stepPositionName));
                
                if (log.actionDate != null && !log.actionDate.isEmpty()) {
                    try {
                        String date = log.actionDate;
                        if (date.contains("T")) {
                            date = date.replace("T", " ");
                        }
                        if (date.contains(".")) {
                            date = date.substring(0, date.indexOf("."));
                        }
                        textActionDate.setText(date);
                    } catch (Exception e) {
                        textActionDate.setText(safe(log.actionDate));
                    }
                } else {
                    textActionDate.setText("--");
                }

                textApproverInfo.setText("Người duyệt: " + safe(log.approverName));

                String status = log.status;
                textApprovalStatusBadge.setText(getApprovalStatusLabel(status));
                textApprovalStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getApprovalStatusColor(status)));

                if (log.comment != null && !log.comment.trim().isEmpty()) {
                    textApprovalComment.setVisibility(View.VISIBLE);
                    textApprovalComment.setText("Ý kiến: " + log.comment.trim());
                } else {
                    textApprovalComment.setVisibility(View.GONE);
                }

                layoutApprovalHistory.addView(logView);
            }
        } else {
            cardApprovalHistory.setVisibility(View.GONE);
        }
    }

    private String getApprovalStatusLabel(String status) {
        if ("PENDING".equals(status)) return "CHỜ DUYỆT";
        if ("APPROVED".equals(status)) return "ĐÃ DUYỆT";
        if ("REJECTED".equals(status)) return "TỪ CHỐI";
        if ("REQUEST_CHANGES".equals(status)) return "YÊU CẦU SỬA";
        return safe(status);
    }

    private int getApprovalStatusColor(String status) {
        if ("PENDING".equals(status)) return 0xFF64748B; // Slate
        if ("APPROVED".equals(status)) return 0xFF10B981; // Green
        if ("REJECTED".equals(status)) return 0xFFEF4444; // Red
        if ("REQUEST_CHANGES".equals(status)) return 0xFFF59E0B; // Amber
        return 0xFF64748B;
    }
}
