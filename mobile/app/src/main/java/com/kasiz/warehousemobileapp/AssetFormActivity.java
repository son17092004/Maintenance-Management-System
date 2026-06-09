package com.kasiz.warehousemobileapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.AssetFormRequest;
import com.kasiz.warehousemobileapp.model.AssetItem;
import com.kasiz.warehousemobileapp.model.AssetTypeItem;
import com.kasiz.warehousemobileapp.model.LocationItem;
import com.kasiz.warehousemobileapp.model.ProductionLineItem;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssetFormActivity extends AppCompatActivity {

    public static final String EXTRA_ASSET_ID = "extra_asset_id";

    private EditText editName;
    private Spinner spinnerType;
    private Spinner spinnerLocation;
    private Spinner spinnerStatus;
    private Spinner spinnerProductionLine;
    private EditText editManufacturer;
    private EditText editSerial;
    private EditText editModel;
    private EditText editYear;
    private EditText editCommission;
    private EditText editPurchase;
    private EditText editWarranty;
    private EditText editDecommission;
    private EditText editSpecs;
    private EditText editDescription;
    private ProgressBar progressBar;
    private Button buttonSave;

    private final List<AssetTypeItem> types = new ArrayList<>();
    private final List<LocationItem> locations = new ArrayList<>();
    private final List<ProductionLineItem> productionLines = new ArrayList<>();
    private AssetItem editingAsset;
    private int assetId;

    private static final String[] STATUS_VALUES = new String[] {"AVAILABLE", "MONITORING", "CAUTION", "MAINTENANCE", "BROKEN", "DECOMMISSIONED"};
    private static final String[] STATUS_LABELS = new String[] {"Hoạt động bình thường", "Đang giám sát", "Cần chú ý", "Đang bảo trì", "Hỏng hóc", "Ngưng hoạt động"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_form);

        assetId = getIntent().getIntExtra(EXTRA_ASSET_ID, 0);

        editName = findViewById(R.id.editName);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerProductionLine = findViewById(R.id.spinnerProductionLine);
        editManufacturer = findViewById(R.id.editManufacturer);
        editSerial = findViewById(R.id.editSerial);
        editModel = findViewById(R.id.editModel);
        editYear = findViewById(R.id.editYear);
        editCommission = findViewById(R.id.editCommission);
        editPurchase = findViewById(R.id.editPurchase);
        editWarranty = findViewById(R.id.editWarranty);
        editDecommission = findViewById(R.id.editDecommission);
        editSpecs = findViewById(R.id.editSpecs);
        editDescription = findViewById(R.id.editDescription);
        progressBar = findViewById(R.id.progressBar);
        buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);

        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, STATUS_LABELS));
        buttonSave.setOnClickListener(v -> saveAsset());
        buttonCancel.setOnClickListener(v -> finish());

        loadOptions();
        if (assetId > 0) {
            loadAsset();
        }
    }

    private void loadOptions() {
        ApiClient.getService(this).assetTypeLeaves().enqueue(new Callback<ApiEnvelope<List<AssetTypeItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<AssetTypeItem>>> call, Response<ApiEnvelope<List<AssetTypeItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    types.clear();
                    types.addAll(response.body().data);
                    List<String> labels = new ArrayList<>();
                    for (AssetTypeItem type : types) labels.add(labelForType(type));
                    spinnerType.setAdapter(new ArrayAdapter<>(AssetFormActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                    syncEditingSelections();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<AssetTypeItem>>> call, Throwable t) { }
        });

        ApiClient.getService(this).locations().enqueue(new Callback<ApiEnvelope<List<LocationItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<LocationItem>>> call, Response<ApiEnvelope<List<LocationItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    locations.clear();
                    locations.addAll(response.body().data);
                    List<String> labels = new ArrayList<>();
                    for (LocationItem location : locations) labels.add(labelForLocation(location));
                    spinnerLocation.setAdapter(new ArrayAdapter<>(AssetFormActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                    syncEditingSelections();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<LocationItem>>> call, Throwable t) { }
        });

        ApiClient.getService(this).productionLines().enqueue(new Callback<ApiEnvelope<List<ProductionLineItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<ProductionLineItem>>> call, Response<ApiEnvelope<List<ProductionLineItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    productionLines.clear();
                    productionLines.addAll(response.body().data);
                    List<String> labels = new ArrayList<>();
                    labels.add("— Chưa phân loại —");
                    for (ProductionLineItem line : productionLines) labels.add(line.lineName);
                    spinnerProductionLine.setAdapter(new ArrayAdapter<>(AssetFormActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                    syncEditingSelections();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<ProductionLineItem>>> call, Throwable t) { }
        });
    }

    private void loadAsset() {
        setLoading(true);
        ApiClient.getService(this).assetById(assetId).enqueue(new Callback<ApiEnvelope<AssetItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AssetItem>> call, Response<ApiEnvelope<AssetItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    editingAsset = response.body().data;
                    fillForm(editingAsset);
                    syncEditingSelections();
                } else {
                    Toast.makeText(AssetFormActivity.this, "Không tải được dữ liệu tài sản", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AssetItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AssetFormActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void fillForm(AssetItem asset) {
        editName.setText(safe(asset.assetName));
        editManufacturer.setText(safe(asset.manufacturer));
        editSerial.setText(safe(asset.serialNumber));
        editModel.setText(safe(asset.model));
        editYear.setText(asset.yearOfManufacture == null ? "" : String.valueOf(asset.yearOfManufacture));
        editCommission.setText(trimDate(asset.commissionDate));
        editPurchase.setText(trimDate(asset.purchaseDate));
        editWarranty.setText(trimDate(asset.warrantyDate));
        editDecommission.setText(trimDate(asset.decommissionDate));
        editSpecs.setText(safe(asset.technicalSpecs));
        editDescription.setText(safe(asset.description));

        int statusIndex = 0;
        if (asset.status != null) {
            for (int i = 0; i < STATUS_VALUES.length; i++) {
                if (STATUS_VALUES[i].equalsIgnoreCase(asset.status.trim())) {
                    statusIndex = i;
                    break;
                }
            }
        }
        spinnerStatus.setSelection(statusIndex);
    }

    private void syncEditingSelections() {
        if (editingAsset == null) return;
        if (editingAsset.assetTypeId != null && !types.isEmpty()) {
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i).assetTypeId == editingAsset.assetTypeId) {
                    spinnerType.setSelection(i);
                    break;
                }
            }
        }
        if (editingAsset.locationId != null && !locations.isEmpty()) {
            for (int i = 0; i < locations.size(); i++) {
                if (locations.get(i).locationId == editingAsset.locationId) {
                    spinnerLocation.setSelection(i);
                    break;
                }
            }
        }
        if (!productionLines.isEmpty()) {
            if (editingAsset.productionLineId != null) {
                for (int i = 0; i < productionLines.size(); i++) {
                    if (productionLines.get(i).lineId == editingAsset.productionLineId) {
                        spinnerProductionLine.setSelection(i + 1);
                        break;
                    }
                }
            } else {
                spinnerProductionLine.setSelection(0);
            }
        }
    }

    private void saveAsset() {
        if (editName.getText().toString().trim().isEmpty()) {
            editName.setError("Bắt buộc");
            return;
        }
        if (spinnerType.getSelectedItemPosition() < 0 || types.isEmpty()) {
            Toast.makeText(this, "Chưa có loại tài sản", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spinnerLocation.getSelectedItemPosition() < 0 || locations.isEmpty()) {
            Toast.makeText(this, "Chưa có vị trí", Toast.LENGTH_SHORT).show();
            return;
        }

        AssetFormRequest request = new AssetFormRequest();
        request.assetName = text(editName);
        request.assetTypeId = String.valueOf(types.get(spinnerType.getSelectedItemPosition()).assetTypeId);
        request.locationId = String.valueOf(locations.get(spinnerLocation.getSelectedItemPosition()).locationId);
        request.status = STATUS_VALUES[spinnerStatus.getSelectedItemPosition()];
        request.manufacturer = text(editManufacturer);
        request.serialNumber = text(editSerial);
        request.model = text(editModel);
        request.yearOfManufacture = text(editYear);
        request.commissionDate = text(editCommission);
        request.purchaseDate = text(editPurchase);
        request.warrantyDate = text(editWarranty);
        request.decommissionDate = text(editDecommission);
        request.technicalSpecs = text(editSpecs);
        request.description = text(editDescription);
        int selectedLinePos = spinnerProductionLine.getSelectedItemPosition();
        if (selectedLinePos > 0 && !productionLines.isEmpty()) {
            request.productionLine = String.valueOf(productionLines.get(selectedLinePos - 1).lineId);
        } else {
            request.productionLine = null;
        }

        setLoading(true);
        Call<ApiEnvelope<AssetItem>> call = assetId > 0
                ? ApiClient.getService(this).updateAsset(assetId, request)
                : ApiClient.getService(this).createAsset(request);
        call.enqueue(new Callback<ApiEnvelope<AssetItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<AssetItem>> call, Response<ApiEnvelope<AssetItem>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    setResult(RESULT_OK);
                    Toast.makeText(AssetFormActivity.this, assetId > 0 ? "Đã cập nhật tài sản" : "Đã tạo tài sản", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AssetFormActivity.this, "Không lưu được tài sản", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<AssetItem>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AssetFormActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSave.setEnabled(!loading);
    }

    private String text(EditText editText) {
        String value = editText.getText().toString().trim();
        return value.isEmpty() ? null : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trimDate(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String trimmed = value.trim();
        return trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
    }

    private String labelForType(AssetTypeItem type) {
        if (type.parentTypeName == null || type.parentTypeName.trim().isEmpty()) {
            return safe(type.typeName);
        }
        return type.parentTypeName.trim() + " • " + safe(type.typeName);
    }

    private String labelForLocation(LocationItem location) {
        if (location.parentLocationName == null || location.parentLocationName.trim().isEmpty()) {
            return safe(location.locationName);
        }
        return location.parentLocationName.trim() + " • " + safe(location.locationName);
    }
}