package com.kasiz.warehousemobileapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.AssetItem;
import com.kasiz.warehousemobileapp.model.ChecklistResultItem;
import com.kasiz.warehousemobileapp.model.ListRow;
import com.kasiz.warehousemobileapp.model.PaginatedPayload;
import com.kasiz.warehousemobileapp.model.WorkOrderItem;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.ui.ListRowAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModuleListActivity extends AppCompatActivity {

    public static final String EXTRA_MODULE = "extra_module";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_SUBTITLE = "extra_subtitle";

    public static final String MODULE_ASSETS = "assets";
    public static final String MODULE_CHECKLISTS = "checklists";
    public static final String MODULE_WORK_ORDERS = "work_orders";

    private TextView textTitle;
    private TextView textSubtitle;
    private TextView textEmpty;
    private ProgressBar progressBar;
    private ListRowAdapter adapter;
    private String module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_list);

        module = getIntent().getStringExtra(EXTRA_MODULE);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String subtitle = getIntent().getStringExtra(EXTRA_SUBTITLE);

        textTitle = findViewById(R.id.textTitle);
        textSubtitle = findViewById(R.id.textSubtitle);
        textEmpty = findViewById(R.id.textEmpty);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        RecyclerView recyclerView = findViewById(R.id.recyclerList);

        textTitle.setText(TextUtils.isEmpty(title) ? "Danh sách" : title);
        textSubtitle.setText(TextUtils.isEmpty(subtitle) ? "" : subtitle);

        adapter = new ListRowAdapter(new ArrayList<>(), this::openDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonRefresh.setOnClickListener(v -> loadList());
        loadList();
    }

    private void loadList() {
        setLoading(true);
        if (MODULE_ASSETS.equals(module)) {
            loadAssets();
            return;
        }
        if (MODULE_CHECKLISTS.equals(module)) {
            loadChecklists();
            return;
        }
        if (MODULE_WORK_ORDERS.equals(module)) {
            loadWorkOrders();
            return;
        }
        setLoading(false);
        Toast.makeText(this, "Module không hợp lệ", Toast.LENGTH_SHORT).show();
    }

    private void loadWorkOrders() {
        ApiClient.getService(this).workOrders(20, 0).enqueue(new Callback<ApiEnvelope<PaginatedPayload<WorkOrderItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> call, Response<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    List<WorkOrderItem> items = response.body().data.items == null ? new ArrayList<>() : response.body().data.items;
                    List<ListRow> rows = new ArrayList<>();
                    for (WorkOrderItem item : items) {
                        rows.add(new ListRow(
                                item.woId,
                                MODULE_WORK_ORDERS,
                                "WO-" + String.format("%04d", item.woId) + ": " + safe(item.title),
                                joinNonEmpty(item.assetName, "Độ ưu tiên: " + safe(item.priority)),
                                safe(item.status),
                                joinNonEmpty("Nguồn: " + safe(item.woSource), "Ngày lên lịch: " + safe(item.plannedDate))
                        ));
                    }
                    adapter.update(rows);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    showEmpty();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> call, Throwable t) {
                showEmpty();
                setLoading(false);
            }
        });
    }

    private void loadAssets() {
        ApiClient.getService(this).assets(20, 0, null, null, null, null, null).enqueue(new Callback<ApiEnvelope<PaginatedPayload<AssetItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<AssetItem>>> call, Response<ApiEnvelope<PaginatedPayload<AssetItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    List<AssetItem> items = response.body().data.items == null ? new ArrayList<>() : response.body().data.items;
                    List<ListRow> rows = new ArrayList<>();
                    for (AssetItem item : items) {
                        rows.add(new ListRow(
                                item.assetId,
                                MODULE_ASSETS,
                                safe(item.assetName),
                                joinNonEmpty(item.assetTypeName, item.locationName),
                                safe(item.status),
                                joinNonEmpty("ID: " + item.assetId, item.serialNumber)
                        ));
                    }
                    adapter.update(rows);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    showEmpty();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<AssetItem>>> call, Throwable t) {
                showEmpty();
                setLoading(false);
            }
        });
    }

    private void loadChecklists() {
        ApiClient.getService(this).checklistResults(20, 0).enqueue(new Callback<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> call, Response<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    List<ChecklistResultItem> items = response.body().data.items == null ? new ArrayList<>() : response.body().data.items;
                    List<ListRow> rows = new ArrayList<>();
                    for (ChecklistResultItem item : items) {
                        rows.add(new ListRow(
                                item.checklistId,
                                MODULE_CHECKLISTS,
                                safe(item.assetName),
                                joinNonEmpty(item.templateName, item.checkerName),
                                joinNonEmpty(item.overallStatus, item.reviewStatus),
                                safe(item.checkTime)
                        ));
                    }
                    adapter.update(rows);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    showEmpty();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> call, Throwable t) {
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

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    private String joinNonEmpty(String first, String second) {
        String a = safe(first);
        String b = safe(second);
        if ("--".equals(a) && "--".equals(b)) return "--";
        if ("--".equals(a)) return b;
        if ("--".equals(b)) return a;
        return a + " • " + b;
    }

    private void openDetail(ListRow row) {
        if (MODULE_ASSETS.equals(row.kind)) {
            android.content.Intent intent = new android.content.Intent(this, AssetDetailActivity.class);
            intent.putExtra(AssetDetailActivity.EXTRA_ASSET_ID, (int) row.id);
            startActivity(intent);
            return;
        }
        if (MODULE_CHECKLISTS.equals(row.kind)) {
            android.content.Intent intent = new android.content.Intent(this, ChecklistDetailActivity.class);
            intent.putExtra(ChecklistDetailActivity.EXTRA_CHECKLIST_ID, (int) row.id);
            startActivity(intent);
            return;
        }
        if (MODULE_WORK_ORDERS.equals(row.kind)) {
            android.content.Intent intent = new android.content.Intent(this, WorkOrderDetailActivity.class);
            intent.putExtra("extra_wo_id", (int) row.id);
            startActivity(intent);
        }
    }
}