package com.kasiz.warehousemobileapp;

import android.content.Intent;
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

        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String welcomeName = session.getFullName();
        textWelcome.setText(welcomeName == null || welcomeName.isEmpty() ? "Xin chào" : "Xin chào, " + welcomeName);

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