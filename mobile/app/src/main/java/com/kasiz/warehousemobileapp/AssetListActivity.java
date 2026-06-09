package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.AssetItem;
import com.kasiz.warehousemobileapp.model.AssetTypeItem;
import com.kasiz.warehousemobileapp.model.ListRow;
import com.kasiz.warehousemobileapp.model.LocationItem;
import com.kasiz.warehousemobileapp.model.PaginatedPayload;
import com.kasiz.warehousemobileapp.model.ProductionLineItem;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;
import com.kasiz.warehousemobileapp.ui.ListRowAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssetListActivity extends AppCompatActivity {

    private static final String[] STATUS_VALUES = new String[] {"", "AVAILABLE", "MONITORING", "CAUTION", "MAINTENANCE", "BROKEN", "DECOMMISSIONED"};
    private static final String[] STATUS_LABELS = new String[] {"Tất cả trạng thái", "Hoạt động bình thường", "Đang giám sát", "Cần chú ý", "Đang bảo trì", "Hỏng hóc", "Ngưng hoạt động"};
    private static final int REQUEST_FORM = 1001;

    private EditText editSearch;
    private Spinner spinnerStatus;
    private Spinner spinnerType;
    private Spinner spinnerLocation;
    private Spinner spinnerProductionLine;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private ListRowAdapter adapter;
    private Button buttonCreate;

    private final List<AssetTypeItem> types = new ArrayList<>();
    private final List<LocationItem> locations = new ArrayList<>();
    private final List<ProductionLineItem> productionLines = new ArrayList<>();
    private String searchText = "";
    private String selectedStatus = "";
    private String selectedTypeId = "";
    private String selectedLocationId = "";
    private String selectedProductionLineId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_list);

        editSearch = findViewById(R.id.editSearch);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerLocation = findViewById(R.id.spinnerLocation);
        spinnerProductionLine = findViewById(R.id.spinnerProductionLine);
        progressBar = findViewById(R.id.progressBar);
        textEmpty = findViewById(R.id.textEmpty);
        buttonCreate = findViewById(R.id.buttonCreate);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        Button buttonReset = findViewById(R.id.buttonReset);
        Button buttonFilter = findViewById(R.id.buttonFilter);
        RecyclerView recyclerView = findViewById(R.id.recyclerList);

        adapter = new ListRowAdapter(new ArrayList<>(), row -> openDetail(row));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonCreate.setVisibility(SessionManager.getInstance(this).canCreateAssets() ? View.VISIBLE : View.GONE);
        buttonCreate.setOnClickListener(v -> openCreateForm());
        buttonRefresh.setOnClickListener(v -> reloadOptionsAndData());
        buttonFilter.setOnClickListener(v -> applyFiltersAndLoad());
        buttonReset.setOnClickListener(v -> resetFilters());

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                searchText = s == null ? "" : s.toString().trim();
            }
        });

        spinnerStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, STATUS_LABELS));
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = STATUS_VALUES[position];
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedStatus = ""; }
        });

        loadMasterData();
    }

    private void reloadOptionsAndData() {
        loadMasterData();
    }

    private void loadMasterData() {
        setLoading(true);
        ApiClient.getService(this).assetTypeLeaves().enqueue(new Callback<ApiEnvelope<List<AssetTypeItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<AssetTypeItem>>> call, Response<ApiEnvelope<List<AssetTypeItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    types.clear();
                    types.addAll(response.body().data);
                    List<String> labels = new ArrayList<>();
                    labels.add("Tất cả loại");
                    for (AssetTypeItem type : types) {
                        labels.add(labelForType(type));
                    }
                    spinnerType.setAdapter(new ArrayAdapter<>(AssetListActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                    spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedTypeId = position <= 0 ? "" : String.valueOf(types.get(position - 1).assetTypeId);
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) { selectedTypeId = ""; }
                    });
                } else {
                    String errorMsg = "Lỗi Loại TS: " + response.code();
                    try { if (response.errorBody() != null) errorMsg += " (" + response.errorBody().string() + ")"; } catch (Exception ignored) {}
                    Toast.makeText(AssetListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
                loadLocations();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<AssetTypeItem>>> call, Throwable t) {
                Toast.makeText(AssetListActivity.this, "Lỗi kết nối Loại TS: " + t.getMessage(), Toast.LENGTH_LONG).show();
                loadLocations();
            }
        });
    }

    private void loadLocations() {
        ApiClient.getService(this).locations().enqueue(new Callback<ApiEnvelope<List<LocationItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<LocationItem>>> call, Response<ApiEnvelope<List<LocationItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    locations.clear();
                    locations.addAll(response.body().data);
                    List<String> labels = new ArrayList<>();
                    labels.add("Tất cả vị trí");
                    for (LocationItem location : locations) {
                        labels.add(labelForLocation(location));
                    }
                    spinnerLocation.setAdapter(new ArrayAdapter<>(AssetListActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                    spinnerLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedLocationId = position <= 0 ? "" : String.valueOf(locations.get(position - 1).locationId);
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) { selectedLocationId = ""; }
                    });
                } else {
                    String errorMsg = "Lỗi Vị trí: " + response.code();
                    try { if (response.errorBody() != null) errorMsg += " (" + response.errorBody().string() + ")"; } catch (Exception ignored) {}
                    Toast.makeText(AssetListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
                loadProductionLines();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<LocationItem>>> call, Throwable t) {
                Toast.makeText(AssetListActivity.this, "Lỗi kết nối Vị trí: " + t.getMessage(), Toast.LENGTH_LONG).show();
                loadProductionLines();
            }
        });
    }

    private void loadProductionLines() {
        ApiClient.getService(this).productionLines().enqueue(new Callback<ApiEnvelope<List<ProductionLineItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<ProductionLineItem>>> call, Response<ApiEnvelope<List<ProductionLineItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    productionLines.clear();
                    productionLines.addAll(response.body().data);
                    List<String> labels = new ArrayList<>();
                    labels.add("Tất cả dây chuyền");
                    for (ProductionLineItem line : productionLines) {
                        labels.add(line.lineName);
                    }
                    spinnerProductionLine.setAdapter(new ArrayAdapter<>(AssetListActivity.this, android.R.layout.simple_spinner_dropdown_item, labels));
                    spinnerProductionLine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedProductionLineId = position <= 0 ? "" : String.valueOf(productionLines.get(position - 1).lineId);
                        }
                        @Override public void onNothingSelected(AdapterView<?> parent) { selectedProductionLineId = ""; }
                    });
                } else {
                    String errorMsg = "Lỗi Dây chuyền: " + response.code();
                    try { if (response.errorBody() != null) errorMsg += " (" + response.errorBody().string() + ")"; } catch (Exception ignored) {}
                    Toast.makeText(AssetListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
                applyFiltersAndLoad();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<ProductionLineItem>>> call, Throwable t) {
                Toast.makeText(AssetListActivity.this, "Lỗi kết nối Dây chuyền: " + t.getMessage(), Toast.LENGTH_LONG).show();
                applyFiltersAndLoad();
            }
        });
    }

    private void applyFiltersAndLoad() {
        loadAssets();
    }

    private void resetFilters() {
        editSearch.setText("");
        spinnerStatus.setSelection(0);
        spinnerType.setSelection(0);
        spinnerLocation.setSelection(0);
        spinnerProductionLine.setSelection(0);
        searchText = "";
        selectedStatus = "";
        selectedTypeId = "";
        selectedLocationId = "";
        selectedProductionLineId = "";
        loadAssets();
    }

    private void loadAssets() {
        setLoading(true);
        ApiClient.getService(this).assets(50, 0,
                searchText.isEmpty() ? null : searchText,
                selectedStatus.isEmpty() ? null : selectedStatus,
                selectedTypeId.isEmpty() ? null : selectedTypeId,
                selectedLocationId.isEmpty() ? null : selectedLocationId,
                selectedProductionLineId.isEmpty() ? null : selectedProductionLineId)
                .enqueue(new Callback<ApiEnvelope<PaginatedPayload<AssetItem>>>() {
                    @Override
                    public void onResponse(Call<ApiEnvelope<PaginatedPayload<AssetItem>>> call, Response<ApiEnvelope<PaginatedPayload<AssetItem>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                            List<AssetItem> items = response.body().data.items == null ? new ArrayList<>() : response.body().data.items;
                            List<ListRow> rows = new ArrayList<>();
                            for (AssetItem item : items) {
                                rows.add(new ListRow(
                                        item.assetId,
                                        ModuleListActivity.MODULE_ASSETS,
                                        safe(item.assetName),
                                        joinNonEmpty(item.assetTypeName, item.locationName),
                                        safe(item.status),
                                        joinNonEmpty(joinNonEmpty(item.serialNumber, item.model), item.productionLineName)
                                ));
                            }
                            adapter.update(rows);
                            textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            String errorMsg = "Lỗi tải tài sản: " + response.code();
                            try { if (response.errorBody() != null) errorMsg += " (" + response.errorBody().string() + ")"; } catch (Exception ignored) {}
                            Toast.makeText(AssetListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            showEmpty();
                        }
                        setLoading(false);
                    }

                    @Override
                    public void onFailure(Call<ApiEnvelope<PaginatedPayload<AssetItem>>> call, Throwable t) {
                        Toast.makeText(AssetListActivity.this, "Lỗi kết nối tải tài sản: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        showEmpty();
                        setLoading(false);
                    }
                });
    }

    private void showEmpty() {
        adapter.update(new ArrayList<>());
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void openDetail(ListRow row) {
        Intent intent = new Intent(this, AssetDetailActivity.class);
        intent.putExtra(AssetDetailActivity.EXTRA_ASSET_ID, (int) row.id);
        startActivity(intent);
    }

    private void openCreateForm() {
        Intent intent = new Intent(this, AssetFormActivity.class);
        startActivityForResult(intent, REQUEST_FORM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FORM && resultCode == RESULT_OK) {
            loadAssets();
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value.trim();
    }

    private String joinNonEmpty(String first, String second) {
        String a = safe(first);
        String b = safe(second);
        if ("--".equals(a) && "--".equals(b)) return "--";
        if ("--".equals(a)) return b;
        if ("--".equals(b)) return a;
        return a + " • " + b;
    }

    private String labelForType(AssetTypeItem type) {
        String parent = safe(type.parentTypeName);
        String current = safe(type.typeName);
        if ("--".equals(parent)) {
            return current;
        }
        return parent + " • " + current;
    }

    private String labelForLocation(LocationItem location) {
        String parent = safe(location.parentLocationName);
        String current = safe(location.locationName);
        if ("--".equals(parent)) {
            return current;
        }
        return parent + " • " + current;
    }
}