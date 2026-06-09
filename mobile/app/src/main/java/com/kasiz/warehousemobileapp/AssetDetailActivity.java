package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.AssetItem;
import com.kasiz.warehousemobileapp.model.AssetPhotoItem;
import com.kasiz.warehousemobileapp.model.AssetQrResponse;
import com.kasiz.warehousemobileapp.model.AssetStatusRequest;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;
import com.kasiz.warehousemobileapp.ui.AssetPhotoAdapter;

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

public class AssetDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET_ID = "extra_asset_id";
    private static final int REQUEST_EDIT = 2001;
    private static final int REQUEST_PICK_PHOTOS = 2002;

    private TextView textName;
    private TextView textStatus;
    private TextView textMeta;
    private TextView textDescription;
    private TextView textCount;
    private TextView textEmptyPhotos;
    private ProgressBar progressBar;
    private AssetPhotoAdapter adapter;
    private Button buttonQr;
    private Button buttonChangeStatus;
    private Button buttonEdit;
    private Button buttonDelete;
    private Button buttonAddPhoto;
    private Button buttonRecordReading;
    private int assetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_detail);

        assetId = getIntent().getIntExtra(EXTRA_ASSET_ID, 0);
        textName = findViewById(R.id.textName);
        textStatus = findViewById(R.id.textStatus);
        textMeta = findViewById(R.id.textMeta);
        textDescription = findViewById(R.id.textDescription);
        textCount = findViewById(R.id.textCount);
        textEmptyPhotos = findViewById(R.id.textEmptyPhotos);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        buttonQr = findViewById(R.id.buttonQr);
        buttonChangeStatus = findViewById(R.id.buttonChangeStatus);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonAddPhoto = findViewById(R.id.buttonAddPhoto);
        buttonRecordReading = findViewById(R.id.buttonRecordReading);
        RecyclerView recyclerView = findViewById(R.id.recyclerPhotos);

        adapter = new AssetPhotoAdapter(
                this,
                new ArrayList<>(),
                this::deletePhoto,
                SessionManager.getInstance(this).getBaseUrl().replace("/api/", "/")
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonRefresh.setOnClickListener(v -> loadDetail());
        buttonQr.setOnClickListener(v -> openQr());
        buttonChangeStatus.setOnClickListener(v -> showStatusDialog());
        buttonEdit.setOnClickListener(v -> openEditForm());
        buttonDelete.setOnClickListener(v -> deleteAsset());
        buttonAddPhoto.setOnClickListener(v -> pickPhotos());
        buttonRecordReading.setOnClickListener(v -> showRecordReadingDialog());
        loadDetail();
    }

    private void loadDetail() {
        setLoading(true);
        ApiClient.getService(this).assetById(assetId).enqueue(new Callback<ApiEnvelope<AssetItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AssetItem>> call, Response<ApiEnvelope<AssetItem>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    AssetItem asset = response.body().data;
                    textName.setText(safe(asset.assetName));
                    textStatus.setText(safe(asset.status));
                    textMeta.setText(join(asset.assetTypeName, asset.productionLineName, asset.locationName, asset.serialNumber, asset.model));
                    textDescription.setText(safe(asset.description));

                    boolean canUpdate = SessionManager.getInstance(AssetDetailActivity.this).canUpdateAssets();
                    boolean canDelete = SessionManager.getInstance(AssetDetailActivity.this).canDeleteAssets();
                    boolean canRecordReading = SessionManager.getInstance(AssetDetailActivity.this).hasPermission("RUNTIME_LOG", "CREATE");
                    buttonChangeStatus.setVisibility(canUpdate ? View.VISIBLE : View.GONE);
                    buttonEdit.setVisibility(canUpdate ? View.VISIBLE : View.GONE);
                    buttonAddPhoto.setVisibility(canUpdate ? View.VISIBLE : View.GONE);
                    buttonRecordReading.setVisibility(canRecordReading ? View.VISIBLE : View.GONE);
                    buttonDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);

                    List<AssetPhotoItem> photos = asset.photos == null ? new ArrayList<>() : asset.photos;
                    textCount.setText("Số ảnh: " + photos.size());
                    textEmptyPhotos.setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.update(photos);
                } else {
                    Toast.makeText(AssetDetailActivity.this, "Không tải được chi tiết tài sản", Toast.LENGTH_SHORT).show();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AssetItem>> call, Throwable t) {
                Toast.makeText(AssetDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void showRecordReadingDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Nhập số giờ chạy tích lũy...");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Ghi số giờ chạy")
                .setMessage("Nhập số giờ chạy hiện tại của thiết bị (giờ đồng hồ):")
                .setView(input)
                .setPositiveButton("Ghi nhận", (dialog, which) -> {
                    String str = input.getText().toString().trim();
                    if (str.isEmpty()) return;
                    try {
                        double val = Double.parseDouble(str);
                        if (val < 0) {
                            Toast.makeText(this, "Số giờ chạy không thể âm", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        recordReading(val);
                    } catch (Exception e) {
                        Toast.makeText(this, "Số giờ không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void recordReading(double value) {
        setLoading(true);
        JsonObject body = new JsonObject();
        body.addProperty("readingValue", value);

        ApiClient.getService(this).recordAssetReading(assetId, body).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(AssetDetailActivity.this, "Đã ghi nhận số giờ chạy thiết bị", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    Toast.makeText(AssetDetailActivity.this, "Ghi nhận số giờ chạy thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AssetDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void openQr() {
        setLoading(true);
        ApiClient.getService(this).assetQrBase64(assetId, "base64").enqueue(new Callback<ApiEnvelope<AssetQrResponse>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AssetQrResponse>> call, Response<ApiEnvelope<AssetQrResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    AssetQrResponse qr = response.body().data;
                    Intent intent = new Intent(AssetDetailActivity.this, AssetQrActivity.class);
                    intent.putExtra(AssetQrActivity.EXTRA_TITLE, safe(qr.assetName));
                    intent.putExtra(AssetQrActivity.EXTRA_QR_PAYLOAD, safe(qr.qrPayload));
                    intent.putExtra(AssetQrActivity.EXTRA_DATA_URL, qr.dataUrl);
                    startActivity(intent);
                } else {
                    Toast.makeText(AssetDetailActivity.this, "Không tải được mã QR", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AssetQrResponse>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AssetDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStatusDialog() {
        if (!SessionManager.getInstance(this).canUpdateAssets()) {
            Toast.makeText(this, "Bạn không có quyền cập nhật tài sản", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] statuses = new String[] {"AVAILABLE", "MONITORING", "CAUTION", "MAINTENANCE", "BROKEN", "DECOMMISSIONED"};
        final CharSequence[] labels = new CharSequence[] {"Hoạt động bình thường", "Đang giám sát", "Cần chú ý", "Đang bảo trì", "Hỏng hóc", "Ngưng hoạt động"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đổi trạng thái tài sản")
                .setItems(labels, (dialog, which) -> updateStatus(statuses[which]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openEditForm() {
        if (!SessionManager.getInstance(this).canUpdateAssets()) {
            Toast.makeText(this, "Bạn không có quyền sửa tài sản", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, AssetFormActivity.class);
        intent.putExtra(AssetFormActivity.EXTRA_ASSET_ID, assetId);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    private void deleteAsset() {
        if (!SessionManager.getInstance(this).canDeleteAssets()) {
            Toast.makeText(this, "Bạn không có quyền xoá tài sản", Toast.LENGTH_SHORT).show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa tài sản")
                .setMessage("Tài sản sẽ được chuyển sang ngưng hoạt động. Bạn chắc chắn muốn tiếp tục?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    setLoading(true);
                    ApiClient.getService(AssetDetailActivity.this).deleteAsset(assetId).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
                        @Override
                        public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(AssetDetailActivity.this, "Đã lưu trữ tài sản", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AssetDetailActivity.this, "Không xoá được tài sản", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                            setLoading(false);
                            Toast.makeText(AssetDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void pickPhotos() {
        if (!SessionManager.getInstance(this).canUpdateAssets()) {
            Toast.makeText(this, "Bạn không có quyền thêm ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh tài sản"), REQUEST_PICK_PHOTOS);
    }

    private void uploadPhotos(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        setLoading(true);
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri uri : uris) {
            MultipartBody.Part part = createPhotoPart(uri);
            if (part != null) parts.add(part);
        }
        if (parts.isEmpty()) {
            setLoading(false);
            Toast.makeText(this, "Không đọc được ảnh đã chọn", Toast.LENGTH_SHORT).show();
            return;
        }
        ApiClient.getService(this).uploadAssetPhotos(assetId, parts).enqueue(new Callback<ApiEnvelope<List<AssetPhotoItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<AssetPhotoItem>>> call, Response<ApiEnvelope<List<AssetPhotoItem>>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(AssetDetailActivity.this, "Đã tải ảnh lên", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    Toast.makeText(AssetDetailActivity.this, "Không tải được ảnh lên", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<AssetPhotoItem>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AssetDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
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

    private void deletePhoto(AssetPhotoItem photo) {
        if (photo == null || photo.photoId == 0) return;
        if (!SessionManager.getInstance(this).canUpdateAssets()) {
            Toast.makeText(this, "Bạn không có quyền xóa ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa ảnh")
                .setMessage("Xóa ảnh này khỏi tài sản?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    setLoading(true);
                    ApiClient.getService(AssetDetailActivity.this).deleteAssetPhoto(assetId, photo.photoId).enqueue(new Callback<ApiEnvelope<List<AssetPhotoItem>>>() {
                        @Override
                        public void onResponse(Call<ApiEnvelope<List<AssetPhotoItem>>> call, Response<ApiEnvelope<List<AssetPhotoItem>>> response) {
                            setLoading(false);
                            if (response.isSuccessful() && response.body() != null && response.body().success) {
                                Toast.makeText(AssetDetailActivity.this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show();
                                loadDetail();
                            } else {
                                Toast.makeText(AssetDetailActivity.this, "Không xóa được ảnh", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiEnvelope<List<AssetPhotoItem>>> call, Throwable t) {
                            setLoading(false);
                            Toast.makeText(AssetDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT && resultCode == RESULT_OK) {
            loadDetail();
            return;
        }
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

    private void updateStatus(String status) {
        setLoading(true);
        ApiClient.getService(this).updateAssetStatus(assetId, new AssetStatusRequest(status)).enqueue(new Callback<ApiEnvelope<AssetItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AssetItem>> call, Response<ApiEnvelope<AssetItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(AssetDetailActivity.this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    Toast.makeText(AssetDetailActivity.this, "Không cập nhật được trạng thái", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AssetItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AssetDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(" • ");
            sb.append(value.trim());
        }
        return sb.length() == 0 ? "--" : sb.toString();
    }
}