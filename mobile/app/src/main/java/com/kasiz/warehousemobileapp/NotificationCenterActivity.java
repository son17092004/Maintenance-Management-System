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
import com.kasiz.warehousemobileapp.model.NotificationItem;
import com.kasiz.warehousemobileapp.model.NotificationsPayload;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.ui.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenterActivity extends AppCompatActivity {

    private TextView textUnread;
    private TextView textEmpty;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private final List<NotificationItem> rawItems = new ArrayList<>();

    // Pagination
    private int currentPage = 1;
    private static final int PAGE_SIZE = 15;
    private View layoutPagination;
    private Button buttonPrevPage;
    private Button buttonNextPage;
    private TextView textPageIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        textUnread  = findViewById(R.id.textUnread);
        textEmpty   = findViewById(R.id.textEmpty);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh    = findViewById(R.id.buttonRefresh);
        Button buttonMarkAllRead = findViewById(R.id.buttonMarkAllRead);
        recyclerView = findViewById(R.id.recyclerNotifications);

        // Pagination Views
        layoutPagination = findViewById(R.id.layoutPagination);
        buttonPrevPage = findViewById(R.id.buttonPrevPage);
        buttonNextPage = findViewById(R.id.buttonNextPage);
        textPageIndicator = findViewById(R.id.textPageIndicator);

        buttonPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadNotifications();
            }
        });

        buttonNextPage.setOnClickListener(v -> {
            currentPage++;
            loadNotifications();
        });

        adapter = new NotificationAdapter(new ArrayList<>());
        adapter.setOnClickListener(item -> {
            if (!item.isRead()) {
                markRead(item.notiId);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonRefresh.setOnClickListener(v -> {
            currentPage = 1;
            loadNotifications();
        });
        buttonMarkAllRead.setOnClickListener(v -> {
            currentPage = 1;
            markAllRead();
        });

        loadNotifications();
    }

    private void loadNotifications() {
        setLoading(true);
        int offset = (currentPage - 1) * PAGE_SIZE;
        ApiClient.getService(this).notifications(PAGE_SIZE, offset).enqueue(new Callback<ApiEnvelope<NotificationsPayload>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<NotificationsPayload>> call, Response<ApiEnvelope<NotificationsPayload>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    NotificationsPayload payload = response.body().data;
                    rawItems.clear();
                    if (payload.items != null) rawItems.addAll(payload.items);

                    long unreadCount = payload.unreadCount;
                    textUnread.setText("Chưa đọc: " + unreadCount + " / Tổng: " + payload.total);

                    adapter.update(new ArrayList<>(rawItems));
                    recyclerView.scrollToPosition(0);
                    textEmpty.setVisibility(rawItems.isEmpty() ? View.VISIBLE : View.GONE);
                    updatePaginationControls((int) payload.total);
                } else {
                    Toast.makeText(NotificationCenterActivity.this, "Không tải được thông báo", Toast.LENGTH_SHORT).show();
                    updatePaginationControls(0);
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<NotificationsPayload>> call, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
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
}