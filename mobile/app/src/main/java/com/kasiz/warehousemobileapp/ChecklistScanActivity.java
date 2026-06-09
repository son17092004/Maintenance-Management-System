package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChecklistScanActivity extends AppCompatActivity {

    private TextInputEditText editAssetId;
    private Button buttonSearch;
    private ProgressBar progressBar;

    private View layoutAssetInfoContainer;
    private View layoutActionBottom;
    private Button buttonStartChecklist;

    private TextView textAssetName;
    private TextView textAssetMeta;

    private Button tabInfo;
    private Button tabSop;
    private Button tabHistory;
    private Button tabLog;

    private View contentInfo;
    private View contentSop;
    private View contentHistory;
    private View contentLog;

    private TextView textInfoSerialNumber;
    private TextView textInfoStatus;
    private TextView textInfoRuntime;
    private TextView textInfoDescription;

    private LinearLayout layoutSopList;
    private LinearLayout layoutHistoryList;
    private LinearLayout layoutLogList;

    private TextView textSopEmpty;
    private TextView textHistoryEmpty;
    private TextView textLogEmpty;

    private String currentAssetId;
    private Integer activeWoId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_scan);

        editAssetId = findViewById(R.id.editAssetId);
        buttonSearch = findViewById(R.id.buttonSearch);
        progressBar = findViewById(R.id.progressBar);

        layoutAssetInfoContainer = findViewById(R.id.layoutAssetInfoContainer);
        layoutActionBottom = findViewById(R.id.layoutActionBottom);
        buttonStartChecklist = findViewById(R.id.buttonStartChecklist);

        textAssetName = findViewById(R.id.textAssetName);
        textAssetMeta = findViewById(R.id.textAssetMeta);

        tabInfo = findViewById(R.id.tabInfo);
        tabSop = findViewById(R.id.tabSop);
        tabHistory = findViewById(R.id.tabHistory);
        tabLog = findViewById(R.id.tabLog);

        contentInfo = findViewById(R.id.contentInfo);
        contentSop = findViewById(R.id.contentSop);
        contentHistory = findViewById(R.id.contentHistory);
        contentLog = findViewById(R.id.contentLog);

        textInfoSerialNumber = findViewById(R.id.textInfoSerialNumber);
        textInfoStatus = findViewById(R.id.textInfoStatus);
        textInfoRuntime = findViewById(R.id.textInfoRuntime);
        textInfoDescription = findViewById(R.id.textInfoDescription);

        layoutSopList = findViewById(R.id.layoutSopList);
        layoutHistoryList = findViewById(R.id.layoutHistoryList);
        layoutLogList = findViewById(R.id.layoutLogList);

        textSopEmpty = findViewById(R.id.textSopEmpty);
        textHistoryEmpty = findViewById(R.id.textHistoryEmpty);
        textLogEmpty = findViewById(R.id.textLogEmpty);

        buttonSearch.setOnClickListener(v -> searchAsset());
        buttonStartChecklist.setOnClickListener(v -> startChecklist());

        setupTabs();
    }

    private void searchAsset() {
        String idStr = editAssetId.getText().toString().trim();
        if (TextUtils.isEmpty(idStr)) {
            Toast.makeText(this, "Vui lòng nhập ID tài sản", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutAssetInfoContainer.setVisibility(View.GONE);
        layoutActionBottom.setVisibility(View.GONE);

        ApiClient.getService(this).getChecklistQrInfo(idStr, null).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    JsonObject data = response.body().data;
                    currentAssetId = idStr;
                    bindData(data);
                } else {
                    Toast.makeText(ChecklistScanActivity.this, "Không tìm thấy tài sản hoặc chưa cấu hình checklist", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                Toast.makeText(ChecklistScanActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void bindData(JsonObject data) {
        if (!data.has("asset") || data.get("asset").isJsonNull()) {
            Toast.makeText(this, "Lỗi cấu trúc dữ liệu tài sản", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject asset = data.getAsJsonObject("asset");
        int assetId = asset.get("assetId").getAsInt();
        String name = asset.has("assetName") ? asset.get("assetName").getAsString() : "";
        String type = asset.has("assetTypeName") ? asset.get("assetTypeName").getAsString() : "";
        String location = asset.has("locationName") ? asset.get("locationName").getAsString() : "";
        String serial = asset.has("serialNumber") ? asset.get("serialNumber").getAsString() : "";
        String status = asset.has("status") ? asset.get("status").getAsString() : "";
        String desc = asset.has("description") ? asset.get("description").getAsString() : "";

        textAssetName.setText(name);
        textAssetMeta.setText(type + " • Vị trí: " + location);

        textInfoSerialNumber.setText("Số Serial: " + (TextUtils.isEmpty(serial) ? "--" : serial));
        textInfoStatus.setText("Trạng thái: " + (TextUtils.isEmpty(status) ? "--" : status));
        textInfoDescription.setText("Mô tả: " + (TextUtils.isEmpty(desc) ? "--" : desc));

        if (data.has("runtimeCounter") && !data.get("runtimeCounter").isJsonNull()) {
            JsonObject counter = data.getAsJsonObject("runtimeCounter");
            double lastVal = counter.has("lastReadingValue") ? counter.get("lastReadingValue").getAsDouble() : 0.0;
            textInfoRuntime.setText("Chỉ số chạy máy hiện tại: " + lastVal + " giờ");
        } else {
            textInfoRuntime.setText("Chỉ số chạy máy hiện tại: --");
        }

        // Active WO
        activeWoId = null;
        if (data.has("woChecklist") && !data.get("woChecklist").isJsonNull()) {
            JsonObject woCheck = data.getAsJsonObject("woChecklist");
            if (woCheck.has("woId") && !woCheck.get("woId").isJsonNull()) {
                activeWoId = woCheck.get("woId").getAsInt();
            }
        }

        // Render SOP Documents
        if (data.has("documents") && data.get("documents").isJsonArray()) {
            renderDocuments(data.getAsJsonArray("documents"));
        } else {
            renderDocuments(new JsonArray());
        }

        // Render logs
        if (data.has("recentResults") && data.get("recentResults").isJsonArray()) {
            renderLogs(data.getAsJsonArray("recentResults"));
        } else {
            renderLogs(new JsonArray());
        }

        // Load History
        loadMaintenanceHistory(assetId);

        layoutAssetInfoContainer.setVisibility(View.VISIBLE);
        layoutActionBottom.setVisibility(View.VISIBLE);
        selectTab(tabInfo, contentInfo);
    }

    private void renderDocuments(JsonArray docs) {
        layoutSopList.removeAllViews();
        if (docs == null || docs.size() == 0) {
            textSopEmpty.setVisibility(View.VISIBLE);
            return;
        }
        textSopEmpty.setVisibility(View.GONE);
        for (int i = 0; i < docs.size(); i++) {
            JsonObject doc = docs.get(i).getAsJsonObject();
            View item = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, layoutSopList, false);
            TextView text1 = item.findViewById(android.R.id.text1);
            TextView text2 = item.findViewById(android.R.id.text2);

            String title = doc.has("description") && !doc.get("description").isJsonNull() ? doc.get("description").getAsString() : "";
            if (title.isEmpty() && doc.has("fileName") && !doc.get("fileName").isJsonNull()) {
                title = doc.get("fileName").getAsString();
            }
            text1.setText(title);
            text1.setTextColor(Color.parseColor("#0F766E"));
            text1.setTypeface(null, Typeface.BOLD);

            String sub = "Loại: " + (doc.has("fileType") && !doc.get("fileType").isJsonNull() ? doc.get("fileType").getAsString() : "Tài liệu");
            if (doc.has("currentVersion") && !doc.get("currentVersion").isJsonNull()) {
                sub += " • Phiên bản: " + doc.get("currentVersion").getAsString();
            }
            text2.setText(sub);
            text2.setTextColor(Color.parseColor("#64748B"));

            String finalDocUrl = null;
            if (doc.has("filePath") && !doc.get("filePath").isJsonNull()) {
                String path = doc.get("filePath").getAsString();
                String apiOrigin = SessionManager.getInstance(this).getBaseUrl().replaceAll("/api/?$", "");
                if (path.startsWith("http")) {
                    finalDocUrl = path;
                } else {
                    finalDocUrl = apiOrigin + "/" + path.replace("\\", "/");
                }
            }

            String urlToOpen = finalDocUrl;
            item.setOnClickListener(v -> {
                if (urlToOpen != null) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(urlToOpen));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Không thể mở tài liệu", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            layoutSopList.addView(item);
        }
    }

    private void loadMaintenanceHistory(int assetId) {
        layoutHistoryList.removeAllViews();
        textHistoryEmpty.setVisibility(View.GONE);

        ApiClient.getService(this).assetMaintenanceHistory(assetId, 50).enqueue(new Callback<ApiEnvelope<List<JsonObject>>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<List<JsonObject>>> call, Response<ApiEnvelope<List<JsonObject>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    renderHistory(response.body().data);
                } else {
                    textHistoryEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<List<JsonObject>>> call, Throwable t) {
                textHistoryEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderHistory(List<JsonObject> historyList) {
        layoutHistoryList.removeAllViews();
        if (historyList == null || historyList.isEmpty()) {
            textHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }
        textHistoryEmpty.setVisibility(View.GONE);
        for (JsonObject h : historyList) {
            View item = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, layoutHistoryList, false);
            TextView text1 = item.findViewById(android.R.id.text1);
            TextView text2 = item.findViewById(android.R.id.text2);

            String completedDate = h.has("completedDate") && !h.get("completedDate").isJsonNull() ? h.get("completedDate").getAsString() : "";
            if (completedDate.contains("T")) {
                completedDate = completedDate.split("T")[0];
            }
            String source = h.has("woSource") && !h.get("woSource").isJsonNull() ? h.get("woSource").getAsString() : "Bảo trì";
            text1.setText(source + " (" + completedDate + ")");
            text1.setTextColor(Color.parseColor("#0F172A"));
            text1.setTypeface(null, Typeface.BOLD);

            String desc = h.has("description") && !h.get("description").isJsonNull() ? h.get("description").getAsString() : "";
            text2.setText(desc);
            text2.setTextColor(Color.parseColor("#475569"));

            layoutHistoryList.addView(item);
        }
    }

    private void renderLogs(JsonArray logs) {
        layoutLogList.removeAllViews();
        if (logs == null || logs.size() == 0) {
            textLogEmpty.setVisibility(View.VISIBLE);
            return;
        }
        textLogEmpty.setVisibility(View.GONE);
        for (int i = 0; i < logs.size(); i++) {
            JsonObject log = logs.get(i).getAsJsonObject();
            View item = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, layoutLogList, false);
            TextView text1 = item.findViewById(android.R.id.text1);
            TextView text2 = item.findViewById(android.R.id.text2);

            String date = log.has("checkTime") && !log.get("checkTime").isJsonNull() ? log.get("checkTime").getAsString() : "";
            if (date.contains("T")) {
                date = date.replace("T", " ");
                if (date.contains(".")) {
                    date = date.substring(0, date.indexOf('.'));
                }
            }
            String checker = log.has("checkerName") && !log.get("checkerName").isJsonNull() ? log.get("checkerName").getAsString() : "--";
            String status = log.has("overallStatus") && !log.get("overallStatus").isJsonNull() ? log.get("overallStatus").getAsString() : "";

            text1.setText(status + " • " + checker + " (" + date + ")");
            text1.setTextColor(Color.parseColor("#0F172A"));
            text1.setTypeface(null, Typeface.BOLD);

            String note = log.has("notes") && !log.get("notes").isJsonNull() ? log.get("notes").getAsString() : "";
            text2.setText(note.isEmpty() ? "Không ghi chú" : note);
            text2.setTextColor(Color.parseColor("#475569"));

            layoutLogList.addView(item);
        }
    }

    private void setupTabs() {
        tabInfo.setOnClickListener(v -> selectTab(tabInfo, contentInfo));
        tabSop.setOnClickListener(v -> selectTab(tabSop, contentSop));
        tabHistory.setOnClickListener(v -> selectTab(tabHistory, contentHistory));
        tabLog.setOnClickListener(v -> selectTab(tabLog, contentLog));
    }

    private void selectTab(Button selectedButton, View selectedContent) {
        // Reset buttons colors and text types
        Button[] buttons = {tabInfo, tabSop, tabHistory, tabLog};
        for (Button btn : buttons) {
            btn.setTextColor(Color.parseColor("#64748B"));
            btn.setTypeface(null, Typeface.NORMAL);
        }

        // Highlight selected
        selectedButton.setTextColor(Color.parseColor("#0F766E"));
        selectedButton.setTypeface(null, Typeface.BOLD);

        // Reset contents visibility
        View[] contents = {contentInfo, contentSop, contentHistory, contentLog};
        for (View c : contents) {
            c.setVisibility(View.GONE);
        }

        // Show selected
        selectedContent.setVisibility(View.VISIBLE);
    }

    private void startChecklist() {
        if (TextUtils.isEmpty(currentAssetId)) return;
        Intent intent = new Intent(this, ChecklistExecutionActivity.class);
        intent.putExtra("extra_asset_id", currentAssetId);
        if (activeWoId != null) {
            intent.putExtra("extra_wo_id", activeWoId);
        }
        startActivity(intent);
    }
}
