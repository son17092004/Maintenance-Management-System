package com.kasiz.warehousemobileapp;

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
import com.kasiz.warehousemobileapp.model.ListRow;
import com.kasiz.warehousemobileapp.model.NotificationItem;
import com.kasiz.warehousemobileapp.model.NotificationsPayload;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.ui.ListRowAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenterActivity extends AppCompatActivity {

    private TextView textUnread;
    private ProgressBar progressBar;
    private ListRowAdapter adapter;
    private final List<NotificationItem> rawItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        textUnread = findViewById(R.id.textUnread);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        Button buttonMarkAllRead = findViewById(R.id.buttonMarkAllRead);
        RecyclerView recyclerView = findViewById(R.id.recyclerNotifications);

        adapter = new ListRowAdapter(new ArrayList<>(), row -> {
            int index = (int) row.id - 1;
            if (index >= 0 && index < rawItems.size()) {
                markRead(rawItems.get(index).notiId);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonRefresh.setOnClickListener(v -> loadNotifications());
        buttonMarkAllRead.setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    private void loadNotifications() {
        setLoading(true);
        ApiClient.getService(this).notifications(50, 0).enqueue(new Callback<ApiEnvelope<NotificationsPayload>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<NotificationsPayload>> call, Response<ApiEnvelope<NotificationsPayload>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    NotificationsPayload payload = response.body().data;
                    rawItems.clear();
                    if (payload.items != null) rawItems.addAll(payload.items);
                    textUnread.setText("Chưa đọc: " + payload.unreadCount + " / Tổng: " + payload.total);

                    List<ListRow> rows = new ArrayList<>();
                    for (int i = 0; i < rawItems.size(); i++) {
                        NotificationItem item = rawItems.get(i);
                        rows.add(new ListRow(
                                i + 1,
                                "notification",
                                safe(item.message),
                                item.isRead ? "Đã đọc" : "Chưa đọc",
                                item.isRead ? "OK" : "NEW",
                                safe(item.createdAt)
                        ));
                    }
                    adapter.update(rows);
                } else {
                    Toast.makeText(NotificationCenterActivity.this, "Không tải được thông báo", Toast.LENGTH_SHORT).show();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<NotificationsPayload>> call, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void markRead(int id) {
        ApiClient.getService(this).markNotificationRead(id).enqueue(new Callback<ApiEnvelope<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<com.google.gson.JsonObject>> call, Response<ApiEnvelope<com.google.gson.JsonObject>> response) {
                loadNotifications();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<com.google.gson.JsonObject>> call, Throwable t) {
                loadNotifications();
            }
        });
    }

    private void markAllRead() {
        ApiClient.getService(this).markAllNotificationsRead().enqueue(new Callback<ApiEnvelope<com.google.gson.JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<com.google.gson.JsonObject>> call, Response<ApiEnvelope<com.google.gson.JsonObject>> response) {
                loadNotifications();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<com.google.gson.JsonObject>> call, Throwable t) {
                loadNotifications();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }
}