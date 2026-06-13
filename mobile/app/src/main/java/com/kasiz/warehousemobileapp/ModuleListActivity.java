package com.kasiz.warehousemobileapp;

import android.content.Intent;
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
    public static final String MODULE_SCHEDULES = "schedules";

    private TextView textTitle;
    private TextView textSubtitle;
    private TextView textEmpty;
    private ProgressBar progressBar;
    private ListRowAdapter adapter;
    private RecyclerView recyclerView;
    private String module;

    private View layoutFilters;
    private com.google.android.material.chip.ChipGroup chipGroupStatus;
    private android.widget.Spinner spinnerPriority;
    private String selectedStatus = null;
    private String selectedPriority = null;

    private View layoutChecklistFilters;
    private com.google.android.material.textfield.TextInputEditText editChecklistSearch;
    private android.widget.Spinner spinnerOverallStatus;
    private android.widget.Spinner spinnerReviewStatus;
    private String selectedChecklistSearch = null;
    private String selectedOverallStatus = null;
    private String selectedReviewStatus = null;

    // Pagination
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;
    private View layoutPagination;
    private Button buttonPrevPage;
    private Button buttonNextPage;
    private TextView textPageIndicator;

    private static final String[] PRIORITIES = {"Mọi ưu tiên", "EMERGENCY", "HIGH", "MEDIUM", "LOW"};
    private static final String[] PRIORITY_VALUES = {null, "EMERGENCY", "HIGH", "MEDIUM", "LOW"};

    private static final String[] OVERALL_STATUS_OPTIONS = {"Tất cả kết quả", "OK", "WARNING (cảnh báo)", "NG (sự cố)"};
    private static final String[] OVERALL_STATUS_VALUES = {null, "OK", "WARNING", "NG"};

    private static final String[] REVIEW_STATUS_OPTIONS = {"Tất cả trạng thái duyệt", "Chờ duyệt", "Đã duyệt", "Bị từ chối"};
    private static final String[] REVIEW_STATUS_VALUES = {null, "PENDING", "APPROVED", "REJECTED"};

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
        Button buttonScanQr = findViewById(R.id.buttonScanQr);
        recyclerView = findViewById(R.id.recyclerList);

        // Pagination Views
        layoutPagination = findViewById(R.id.layoutPagination);
        buttonPrevPage = findViewById(R.id.buttonPrevPage);
        buttonNextPage = findViewById(R.id.buttonNextPage);
        textPageIndicator = findViewById(R.id.textPageIndicator);

        buttonPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadList();
            }
        });

        buttonNextPage.setOnClickListener(v -> {
            currentPage++;
            loadList();
        });

        if (MODULE_CHECKLISTS.equals(module)) {
            buttonScanQr.setVisibility(View.VISIBLE);
            buttonScanQr.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChecklistScanActivity.class);
                startActivity(intent);
            });
        }

        textTitle.setText(TextUtils.isEmpty(title) ? "Danh sách" : title);
        textSubtitle.setText(TextUtils.isEmpty(subtitle) ? "" : subtitle);

        adapter = new ListRowAdapter(new ArrayList<>(), this::openDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonRefresh.setOnClickListener(v -> {
            currentPage = 1;
            loadList();
        });

        layoutFilters = findViewById(R.id.layoutFilters);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        spinnerPriority = findViewById(R.id.spinnerPriority);

        if (MODULE_WORK_ORDERS.equals(module)) {
            layoutFilters.setVisibility(View.VISIBLE);
            
            chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
                currentPage = 1;
                if (checkedId == R.id.chipWaiting) {
                    selectedStatus = "WAITING";
                } else if (checkedId == R.id.chipRunning) {
                    selectedStatus = "IN_PROGRESS";
                } else if (checkedId == R.id.chipAwaitingClosure) {
                    selectedStatus = "AWAITING_CLOSURE";
                } else if (checkedId == R.id.chipCompleted) {
                    selectedStatus = "COMPLETED";
                } else if (checkedId == R.id.chipCancelled) {
                    selectedStatus = "CANCELLED";
                } else {
                    selectedStatus = null;
                }
                loadList();
            });

            android.widget.ArrayAdapter<String> priorityAdapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, PRIORITIES);
            priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPriority.setAdapter(priorityAdapter);
            spinnerPriority.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    currentPage = 1;
                    selectedPriority = PRIORITY_VALUES[position];
                    loadList();
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }

        layoutChecklistFilters = findViewById(R.id.layoutChecklistFilters);
        editChecklistSearch = findViewById(R.id.editChecklistSearch);
        spinnerOverallStatus = findViewById(R.id.spinnerOverallStatus);
        spinnerReviewStatus = findViewById(R.id.spinnerReviewStatus);

        if (MODULE_CHECKLISTS.equals(module)) {
            layoutChecklistFilters.setVisibility(View.VISIBLE);

            editChecklistSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    currentPage = 1;
                    selectedChecklistSearch = s.toString().trim();
                    if (selectedChecklistSearch.isEmpty()) {
                        selectedChecklistSearch = null;
                    }
                    loadList();
                }
            });

            android.widget.ArrayAdapter<String> overallAdapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, OVERALL_STATUS_OPTIONS);
            overallAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerOverallStatus.setAdapter(overallAdapter);
            spinnerOverallStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    currentPage = 1;
                    selectedOverallStatus = OVERALL_STATUS_VALUES[position];
                    loadList();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            android.widget.ArrayAdapter<String> reviewAdapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, REVIEW_STATUS_OPTIONS);
            reviewAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerReviewStatus.setAdapter(reviewAdapter);
            spinnerReviewStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    currentPage = 1;
                    selectedReviewStatus = REVIEW_STATUS_VALUES[position];
                    loadList();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

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
        if (MODULE_SCHEDULES.equals(module)) {
            loadSchedules();
            return;
        }
        setLoading(false);
        Toast.makeText(this, "Module không hợp lệ", Toast.LENGTH_SHORT).show();
    }

    private void loadWorkOrders() {
        int offset = (currentPage - 1) * PAGE_SIZE;
        ApiClient.getService(this).workOrders(PAGE_SIZE, offset, selectedStatus, selectedPriority).enqueue(new Callback<ApiEnvelope<PaginatedPayload<WorkOrderItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> call, Response<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    PaginatedPayload<WorkOrderItem> data = response.body().data;
                    List<WorkOrderItem> items = data.items == null ? new ArrayList<>() : data.items;
                    List<ListRow> rows = new ArrayList<>();
                    for (WorkOrderItem item : items) {
                        String primaryText = (item.title != null && !item.title.isEmpty())
                                ? item.title
                                : safe(item.description);
                        if (primaryText.length() > 55) {
                            primaryText = primaryText.substring(0, 52) + "...";
                        }
                        rows.add(new ListRow(
                                item.woId,
                                MODULE_WORK_ORDERS,
                                "WO-" + String.format("%04d", item.woId) + ": " + primaryText,
                                joinNonEmpty(item.assetName, "Ưu tiên: " + safe(item.priority)),
                                safe(item.status),
                                joinNonEmpty("Nguồn: " + safe(item.woSource), "Ngày lên lịch: " + safe(item.plannedDate))
                        ));
                    }
                    adapter.update(rows);
                    recyclerView.scrollToPosition(0);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    updatePaginationControls(data.total);
                } else {
                    showEmpty();
                    updatePaginationControls(0);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<WorkOrderItem>>> call, Throwable t) {
                showEmpty();
                updatePaginationControls(0);
                setLoading(false);
            }
        });
    }

    private void loadAssets() {
        int offset = (currentPage - 1) * PAGE_SIZE;
        ApiClient.getService(this).assets(PAGE_SIZE, offset, null, null, null, null, null).enqueue(new Callback<ApiEnvelope<PaginatedPayload<AssetItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<AssetItem>>> call, Response<ApiEnvelope<PaginatedPayload<AssetItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    PaginatedPayload<AssetItem> data = response.body().data;
                    List<AssetItem> items = data.items == null ? new ArrayList<>() : data.items;
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
                    recyclerView.scrollToPosition(0);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    updatePaginationControls(data.total);
                } else {
                    showEmpty();
                    updatePaginationControls(0);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<AssetItem>>> call, Throwable t) {
                showEmpty();
                updatePaginationControls(0);
                setLoading(false);
            }
        });
    }

    private void loadChecklists() {
        int offset = (currentPage - 1) * PAGE_SIZE;
        ApiClient.getService(this).checklistResults(PAGE_SIZE, offset, selectedChecklistSearch, selectedOverallStatus, selectedReviewStatus).enqueue(new Callback<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> call, Response<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    PaginatedPayload<ChecklistResultItem> data = response.body().data;
                    List<ChecklistResultItem> items = data.items == null ? new ArrayList<>() : data.items;
                    List<ListRow> rows = new ArrayList<>();
                    for (ChecklistResultItem item : items) {
                        // Formatting checkTime
                        String rawTime = safe(item.checkTime);
                        String formattedTime = rawTime;
                        if (rawTime.contains("T")) {
                            try {
                                String datePart = rawTime.split("T")[0]; // 2026-05-21
                                String timePart = rawTime.split("T")[1].substring(0, 5); // 18:51
                                String[] dateParts = datePart.split("-");
                                formattedTime = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0] + " " + timePart;
                            } catch (Exception e) {
                                // fallback
                            }
                        }

                        // overallStatus & reviewStatus badge
                        String overall = safe(item.overallStatus).toUpperCase();
                        String review = safe(item.reviewStatus).toUpperCase();
                        String badgeText = overall + " | " + review;

                        // detail text: Thời gian & Ghi chú
                        String notesPart = (item.notes != null && !item.notes.trim().isEmpty())
                                ? "\nGhi chú: " + item.notes
                                : "";
                        String detailText = "Thời gian: " + formattedTime + notesPart;

                        rows.add(new ListRow(
                                item.checklistId,
                                MODULE_CHECKLISTS,
                                safe(item.assetName),
                                joinNonEmpty(item.templateName, "Người kiểm tra: " + safe(item.checkerName)),
                                badgeText,
                                detailText
                        ));
                    }
                    adapter.update(rows);
                    recyclerView.scrollToPosition(0);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    updatePaginationControls(data.total);
                } else {
                    showEmpty();
                    updatePaginationControls(0);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<ChecklistResultItem>>> call, Throwable t) {
                showEmpty();
                updatePaginationControls(0);
                setLoading(false);
            }
        });
    }

    private void showEmpty() {
        adapter.update(new ArrayList<>());
        textEmpty.setVisibility(View.VISIBLE);
    }

    private void loadSchedules() {
        int offset = (currentPage - 1) * PAGE_SIZE;
        ApiClient.getService(this).maintenanceSchedules(PAGE_SIZE, offset, null).enqueue(new Callback<ApiEnvelope<PaginatedPayload<com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<PaginatedPayload<com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem>>> call,
                                   Response<ApiEnvelope<PaginatedPayload<com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    PaginatedPayload<com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem> data = response.body().data;
                    List<com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem> items = data.items == null ? new ArrayList<>() : data.items;
                    List<ListRow> rows = new ArrayList<>();
                    for (com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem item : items) {
                        // Translate Frequency Unit
                        String freqUnit = safe(item.frequencyUnit).toUpperCase();
                        if ("DAYS".equals(freqUnit)) freqUnit = "ngày";
                        else if ("WEEKS".equals(freqUnit)) freqUnit = "tuần";
                        else if ("MONTHS".equals(freqUnit)) freqUnit = "tháng";
                        else if ("YEARS".equals(freqUnit)) freqUnit = "năm";
                        else if ("HOURS".equals(freqUnit)) freqUnit = "giờ";
                        else freqUnit = freqUnit.toLowerCase();

                        String freq = "";
                        if (item.frequencyValue != null) {
                            freq = item.frequencyValue + " " + freqUnit;
                        } else {
                            freq = freqUnit;
                        }

                        // Translate Status to match web STATUS_LABEL exactly
                        String rawStatus = safe(item.status).toUpperCase();
                        String statusLabel = rawStatus;
                        if ("DRAFT".equals(rawStatus)) {
                            statusLabel = "Bản nháp";
                        } else if ("PENDING_APPROVAL".equals(rawStatus)) {
                            statusLabel = "Chờ duyệt";
                        } else if ("PENDING".equals(rawStatus)) {
                            statusLabel = "Chờ TH";
                        } else if ("IN_PROGRESS".equals(rawStatus)) {
                            statusLabel = "Đang TH";
                        } else if ("COMPLETED".equals(rawStatus)) {
                            statusLabel = "Hoàn thành";
                        } else if ("OVERDUE".equals(rawStatus)) {
                            statusLabel = "Quá hạn";
                        } else if ("CANCELLED".equals(rawStatus)) {
                            statusLabel = "Hủy";
                        } else if ("REJECTED".equals(rawStatus)) {
                            statusLabel = "Từ chối";
                        }

                        // Kiểu lịch (Periodic vs Predictive)
                        String kind = "HOURS".equals(item.frequencyUnit) ? "Dự báo (giờ)" : "Định kỳ";

                        // Checklist Template Name
                        String checklistName = (item.checklistTemplateName != null && !item.checklistTemplateName.isEmpty())
                                ? item.checklistTemplateName
                                : "Chưa gắn template";

                        // Hạn tiếp theo / Trạng thái hiệu lực
                        String nextDueText = "";
                        if ("DRAFT".equals(rawStatus) || "PENDING_APPROVAL".equals(rawStatus) || "REJECTED".equals(rawStatus)) {
                            nextDueText = "Chưa hiệu lực";
                        } else if ("HOURS".equals(item.frequencyUnit)) {
                            nextDueText = "Theo giờ chạy";
                        } else {
                            nextDueText = safe(item.nextDueDate);
                        }

                        // Ngày thực hiện cuối
                        String lastExec = (item.lastExecutedDate != null && !item.lastExecutedDate.isEmpty())
                                ? item.lastExecutedDate
                                : "Chưa TH";

                        String detailText = "Kiểu: " + kind + " | Chu kỳ: " + freq + "\n"
                                + "Checklist: " + checklistName + "\n"
                                + "Hạn tiếp theo: " + nextDueText + " | Lần cuối: " + lastExec;
                        
                        rows.add(new ListRow(
                                item.scheduleId,
                                MODULE_SCHEDULES,
                                safe(item.scheduleName),
                                joinNonEmpty(item.assetName, item.locationName),
                                statusLabel,
                                detailText
                        ));
                    }
                    adapter.update(rows);
                    recyclerView.scrollToPosition(0);
                    textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
                    updatePaginationControls(data.total);
                } else {
                    showEmpty();
                    updatePaginationControls(0);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<PaginatedPayload<com.kasiz.warehousemobileapp.model.MaintenanceScheduleItem>>> call, Throwable t) {
                showEmpty();
                updatePaginationControls(0);
                setLoading(false);
            }
        });
    }

    private void updatePaginationControls(int totalCount) {
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (totalPages < 1) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        textPageIndicator.setText("Trang " + currentPage + " / " + totalPages);
        buttonPrevPage.setEnabled(currentPage > 1);
        buttonNextPage.setEnabled(currentPage < totalPages);

        if (totalPages > 1) {
            layoutPagination.setVisibility(View.VISIBLE);
        } else {
            layoutPagination.setVisibility(View.GONE);
        }
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
        return a + " | " + b;
    }

    private void openDetail(ListRow row) {
        if (MODULE_ASSETS.equals(row.kind)) {
            Intent intent = new Intent(this, AssetDetailActivity.class);
            intent.putExtra(AssetDetailActivity.EXTRA_ASSET_ID, (int) row.id);
            startActivity(intent);
            return;
        }
        if (MODULE_CHECKLISTS.equals(row.kind)) {
            Intent intent = new Intent(this, ChecklistDetailActivity.class);
            intent.putExtra(ChecklistDetailActivity.EXTRA_CHECKLIST_ID, (int) row.id);
            startActivity(intent);
            return;
        }
        if (MODULE_WORK_ORDERS.equals(row.kind)) {
            Intent intent = new Intent(this, WorkOrderDetailActivity.class);
            intent.putExtra("extra_wo_id", (int) row.id);
            startActivity(intent);
        }
    }
}