package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.HealthInfo;
import com.kasiz.warehousemobileapp.model.MeProfile;
import com.kasiz.warehousemobileapp.model.NotificationItem;
import com.kasiz.warehousemobileapp.model.NotificationsPayload;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;
import com.kasiz.warehousemobileapp.ui.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private TextView textWelcome;
    private TextView textServerStatus;
    private TextView textUnreadCount;
    private TextView textEmpty;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;

    // Trạng thái của tôi views
    private View cardMyStatus;
    private View layoutMyStatusBg;
    private TextView textStatusBadge;
    private TextView textStatusHeadline;
    private TextView textStatusDetail;
    private View layoutFocusWorkOrder;
    private TextView textFocusWoCode;
    private TextView textFocusWoAssetName;
    private TextView textFocusWoLocation;
    private TextView textFocusWoStatus;
    private TextView textFocusWoPriority;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn() && !session.isFieldWorker()) {
            logout();
            return;
        }

        textWelcome = findViewById(R.id.textWelcome);
        textServerStatus = findViewById(R.id.textServerStatus);
        textUnreadCount = findViewById(R.id.textUnreadCount);
        textEmpty = findViewById(R.id.textEmpty);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        Button buttonAssets = findViewById(R.id.buttonAssets);
        Button buttonWorkOrders = findViewById(R.id.buttonWorkOrders);
        Button buttonChecklists = findViewById(R.id.buttonChecklists);
        Button buttonProfile = findViewById(R.id.buttonProfile);
        Button buttonNotifications = findViewById(R.id.buttonNotifications);
        Button buttonLogout = findViewById(R.id.buttonLogout);
        RecyclerView recyclerView = findViewById(R.id.recyclerNotifications);

        // Find status card views
        cardMyStatus = findViewById(R.id.cardMyStatus);
        layoutMyStatusBg = findViewById(R.id.layoutMyStatusBg);
        textStatusBadge = findViewById(R.id.textStatusBadge);
        textStatusHeadline = findViewById(R.id.textStatusHeadline);
        textStatusDetail = findViewById(R.id.textStatusDetail);
        layoutFocusWorkOrder = findViewById(R.id.layoutFocusWorkOrder);
        textFocusWoCode = findViewById(R.id.textFocusWoCode);
        textFocusWoAssetName = findViewById(R.id.textFocusWoAssetName);
        textFocusWoLocation = findViewById(R.id.textFocusWoLocation);
        textFocusWoStatus = findViewById(R.id.textFocusWoStatus);
        textFocusWoPriority = findViewById(R.id.textFocusWoPriority);

        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String welcomeName = session.getFullName();
        textWelcome.setText(welcomeName == null || welcomeName.isEmpty() ? "Xin chào" : "Xin chào, " + welcomeName);

        // Try load cached profile status first
        MeProfile cached = session.getCachedProfile();
        if (cached != null) {
            bindFieldWorkSummary(cached.fieldWorkSummary);
        }

        buttonRefresh.setOnClickListener(v -> loadData());
        buttonAssets.setOnClickListener(v -> startActivity(new Intent(this, AssetListActivity.class)));
        buttonWorkOrders.setOnClickListener(v -> openModuleList(ModuleListActivity.MODULE_WORK_ORDERS, "Phiếu việc được giao", "Danh sách phiếu việc bảo trì"));
        buttonChecklists.setOnClickListener(v -> openModuleList(ModuleListActivity.MODULE_CHECKLISTS, "Danh sách checklist", "Các phiếu checklist gần đây"));
        buttonProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        buttonNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        buttonLogout.setOnClickListener(v -> logout());

        loadData();
    }

    private void loadData() {
        setLoading(true);
        loadHealth();
        loadNotifications();
        loadProfile();
    }

    private void loadHealth() {
        ApiClient.getService(this).health().enqueue(new Callback<ApiEnvelope<HealthInfo>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<HealthInfo>> call, Response<ApiEnvelope<HealthInfo>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    HealthInfo info = response.body().data;
                    textServerStatus.setText("Server: " + safe(info.status) + " | DB: " + safe(info.database));
                } else {
                    textServerStatus.setText("Server: lỗi kết nối");
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<HealthInfo>> call, Throwable t) {
                textServerStatus.setText("Server: không phản hồi");
                setLoading(false);
            }
        });
    }

    private void loadNotifications() {
        ApiClient.getService(this).notifications(10, 0).enqueue(new Callback<ApiEnvelope<NotificationsPayload>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<NotificationsPayload>> call, Response<ApiEnvelope<NotificationsPayload>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    NotificationsPayload payload = response.body().data;
                    textUnreadCount.setText("Chưa đọc: " + payload.unreadCount + " / Tổng: " + payload.total);
                    List<NotificationItem> items = payload.items == null ? new ArrayList<>() : payload.items;
                    adapter.update(items);
                    textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    textUnreadCount.setText("Chưa đọc: --");
                    adapter.update(new ArrayList<>());
                    textEmpty.setVisibility(View.VISIBLE);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<NotificationsPayload>> call, Throwable t) {
                textUnreadCount.setText("Chưa đọc: --");
                adapter.update(new ArrayList<>());
                textEmpty.setVisibility(View.VISIBLE);
                setLoading(false);
            }
        });
    }

    private void loadProfile() {
        ApiClient.getService(this).me().enqueue(new Callback<ApiEnvelope<MeProfile>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<MeProfile>> call, Response<ApiEnvelope<MeProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    MeProfile profile = response.body().data;
                    SessionManager.getInstance(DashboardActivity.this).saveProfile(profile);

                    String welcomeName = profile.fullName;
                    textWelcome.setText(welcomeName == null || welcomeName.isEmpty() ? "Xin chào" : "Xin chào, " + welcomeName);

                    bindFieldWorkSummary(profile.fieldWorkSummary);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<MeProfile>> call, Throwable t) {
                MeProfile cached = SessionManager.getInstance(DashboardActivity.this).getCachedProfile();
                if (cached != null) {
                    bindFieldWorkSummary(cached.fieldWorkSummary);
                }
                setLoading(false);
            }
        });
    }

    private void bindFieldWorkSummary(MeProfile.FieldWorkSummary fs) {
        if (fs == null) {
            cardMyStatus.setVisibility(View.GONE);
            return;
        }
        cardMyStatus.setVisibility(View.VISIBLE);
        textStatusHeadline.setText(safe(fs.headline));
        textStatusDetail.setText(safe(fs.detail));
        textStatusBadge.setText(safe(fs.availability));

        int bgColor = Color.parseColor("#F8FAFC");
        int strokeColor = Color.parseColor("#E2E8F0");
        int badgeColor = Color.parseColor("#475569");

        String avail = fs.availability == null ? "IDLE" : fs.availability;
        switch (avail) {
            case "ON_LEAVE":
                bgColor = Color.parseColor("#FFFBEB");
                strokeColor = Color.parseColor("#FDE68A");
                badgeColor = Color.parseColor("#D97706");
                break;
            case "BUSY_ON_SITE":
                bgColor = Color.parseColor("#FFF7ED");
                strokeColor = Color.parseColor("#FED7AA");
                badgeColor = Color.parseColor("#EA580C");
                break;
            case "BUSY_PAUSED":
                bgColor = Color.parseColor("#FEFCE8");
                strokeColor = Color.parseColor("#FEF08A");
                badgeColor = Color.parseColor("#CA8A04");
                break;
            case "BUSY_AWAITING_REVIEW":
                bgColor = Color.parseColor("#F5F3FF");
                strokeColor = Color.parseColor("#DDD6FE");
                badgeColor = Color.parseColor("#7C3AED");
                break;
            case "AWAITING_NON_URGENT":
                bgColor = Color.parseColor("#F0F9FF");
                strokeColor = Color.parseColor("#BAE6FD");
                badgeColor = Color.parseColor("#0284C7");
                break;
            case "ASSIGNED_IDLE":
                bgColor = Color.parseColor("#ECFDF5");
                strokeColor = Color.parseColor("#A7F3D0");
                badgeColor = Color.parseColor("#059669");
                break;
            case "IDLE":
            default:
                bgColor = Color.parseColor("#F8FAFC");
                strokeColor = Color.parseColor("#E2E8F0");
                badgeColor = Color.parseColor("#475569");
                break;
        }

        layoutMyStatusBg.setBackgroundColor(bgColor);
        if (cardMyStatus instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView materialCard = (com.google.android.material.card.MaterialCardView) cardMyStatus;
            materialCard.setStrokeColor(ColorStateList.valueOf(strokeColor));
        }
        textStatusBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

        MeProfile.ActiveWorkOrder activeWo = fs.activeWorkOrder;
        if (activeWo != null) {
            layoutFocusWorkOrder.setVisibility(View.VISIBLE);
            textFocusWoCode.setText("WO-" + String.format("%04d", activeWo.woId));
            textFocusWoAssetName.setText(safe(activeWo.assetName));
            textFocusWoLocation.setText(safe(activeWo.locationName));

            textFocusWoStatus.setText(safe(activeWo.status));
            int statusColor = Color.parseColor("#718096");
            if ("IN_PROGRESS".equals(activeWo.status)) {
                statusColor = Color.parseColor("#3182CE");
            } else if ("PAUSED".equals(activeWo.status)) {
                statusColor = Color.parseColor("#DD6B20");
            } else if ("AWAITING_CLOSURE".equals(activeWo.status)) {
                statusColor = Color.parseColor("#805AD5");
            } else if ("WAITING".equals(activeWo.status)) {
                statusColor = Color.parseColor("#4A5568");
            }
            textFocusWoStatus.setBackgroundTintList(ColorStateList.valueOf(statusColor));

            textFocusWoPriority.setText(safe(activeWo.priority));
            int priorityColor = Color.parseColor("#A0AEC0");
            if ("EMERGENCY".equals(activeWo.priority)) {
                priorityColor = Color.parseColor("#E53E3E");
            } else if ("HIGH".equals(activeWo.priority)) {
                priorityColor = Color.parseColor("#DD6B20");
            } else if ("MEDIUM".equals(activeWo.priority)) {
                priorityColor = Color.parseColor("#D69E2E");
            } else if ("LOW".equals(activeWo.priority)) {
                priorityColor = Color.parseColor("#3182CE");
            }
            textFocusWoPriority.setBackgroundTintList(ColorStateList.valueOf(priorityColor));

            layoutFocusWorkOrder.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, WorkOrderDetailActivity.class);
                intent.putExtra("extra_wo_id", activeWo.woId);
                startActivity(intent);
            });
        } else {
            layoutFocusWorkOrder.setVisibility(View.GONE);
        }
    }

    private void logout() {
        SessionManager.getInstance(this).clear();
        ApiClient.clearCookies();
        ApiClient.reset();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void openModuleList(String module, String title, String subtitle) {
        Intent intent = new Intent(this, ModuleListActivity.class);
        intent.putExtra(ModuleListActivity.EXTRA_MODULE, module);
        intent.putExtra(ModuleListActivity.EXTRA_TITLE, title);
        intent.putExtra(ModuleListActivity.EXTRA_SUBTITLE, subtitle);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }
}