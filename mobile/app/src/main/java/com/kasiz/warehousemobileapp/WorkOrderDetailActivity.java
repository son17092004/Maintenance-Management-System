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
    private TextView textAssignees, textNoPhotos;
    private View cardExecution, layoutActions, layoutRunningActions;
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
        textAssignees = findViewById(R.id.textAssignees);
        textNoPhotos = findViewById(R.id.textNoPhotos);

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

        recyclerPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new WorkOrderPhotoAdapter(this, new ArrayList<>(), photo -> deletePhoto(photo), SessionManager.getInstance(this).getBaseUrl().replace("/api/", ""));
        recyclerPhotos.setAdapter(adapter);

        checkShutdown.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutShutdownReason.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        buttonStart.setOnClickListener(v -> updateStatus("RUNNING"));
        buttonPause.setOnClickListener(v -> updateStatus("PAUSED"));
        buttonResume.setOnClickListener(v -> updateStatus("RUNNING"));

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
        textTitle.setText(safe(item.title));
        textStatus.setText(getStatusLabel(item.status));
        textAsset.setText("Thiết bị: " + safe(item.assetName) + " (ID: " + item.assetId + ")");
        textDescription.setText("Mô tả: " + safe(item.description));
        textPriority.setText("Mức độ ưu tiên: " + safe(item.priority));
        textSource.setText("Nguồn phát sinh: " + safe(item.woSource));
        textPlannedDate.setText("Ngày lên lịch: " + safe(item.plannedDate));
        textEstimatedHours.setText("Thời gian ước tính: " + (item.estimatedHours != null ? item.estimatedHours + " giờ" : "--"));

        // Render Assignees
        StringBuilder assigneesText = new StringBuilder();
        if (item.assignments != null) {
            for (WorkOrderItem.Assignment a : item.assignments) {
                if (assigneesText.length() > 0) assigneesText.append("\n");
                assigneesText.append(safe(a.fullName)).append(" (").append(safe(a.positionName)).append(")");
                if (a.isGroupLeader == 1) {
                    assigneesText.append(" [Trưởng nhóm]");
                }
            }
        }
        textAssignees.setText(assigneesText.length() == 0 ? "Chưa có người thực hiện" : assigneesText.toString());

        // Render Photos
        List<WorkOrderItem.Photo> photos = item.photos == null ? new ArrayList<>() : item.photos;
        textNoPhotos.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.update(photos);

        // Control Execution UI
        String status = item.status;
        boolean canWork = "WAITING".equals(status) || "RUNNING".equals(status) || "PAUSED".equals(status);
        boolean isRunningOrPaused = "RUNNING".equals(status) || "PAUSED".equals(status);

        cardExecution.setVisibility(isRunningOrPaused ? View.VISIBLE : View.GONE);
        buttonUploadPhoto.setVisibility(isRunningOrPaused ? View.VISIBLE : View.GONE);

        buttonStart.setVisibility("WAITING".equals(status) ? View.VISIBLE : View.GONE);
        layoutRunningActions.setVisibility("RUNNING".equals(status) ? View.VISIBLE : View.GONE);
        buttonResume.setVisibility("PAUSED".equals(status) ? View.VISIBLE : View.GONE);

        boolean isCorrective = "CORRECTIVE".equalsIgnoreCase(item.woSource);
        buttonResetBaseline.setVisibility((isRunningOrPaused && isCorrective) ? View.VISIBLE : View.GONE);

        // Show Checklist button if it has schedule / template, or generally in progress
        buttonChecklistExec.setVisibility(isRunningOrPaused ? View.VISIBLE : View.GONE);

        if (isRunningOrPaused) {
            editFieldNotes.setText(safe(item.closureFieldNotes));
            editPartsNotes.setText(safe(item.closurePartsNotes));
            editActualHours.setText(item.actualHours != null ? String.valueOf(item.actualHours) : "");
            checkShutdown.setChecked(item.requiresShutdown);
            editShutdownReason.setText(safe(item.shutdownReason));
            layoutShutdownReason.setVisibility(item.requiresShutdown ? View.VISIBLE : View.GONE);
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
        if ("RUNNING".equals(status)) return "Đang thực hiện";
        if ("PAUSED".equals(status)) return "Đang tạm dừng";
        if ("AWAITING_CLOSURE".equals(status)) return "Chờ nghiệm thu";
        if ("COMPLETED".equals(status)) return "Đã hoàn thành";
        if ("CANCELLED".equals(status)) return "Đã hủy";
        return status;
    }
}
