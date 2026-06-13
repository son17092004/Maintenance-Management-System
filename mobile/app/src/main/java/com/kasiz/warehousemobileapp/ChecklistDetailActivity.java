package com.kasiz.warehousemobileapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.ChecklistDetailItem;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;
import com.kasiz.warehousemobileapp.ui.ChecklistDetailAdapter;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChecklistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CHECKLIST_ID = "extra_checklist_id";

    private TextView textName;
    private TextView textMeta;
    private TextView textNotes;
    private TextView textSupervisorNotes;
    private TextView textReadingValue;
    private TextView badgeOverallStatus;
    private TextView badgeReviewStatus;
    
    private View cardFieldNotes;
    private View cardSupervisorNotes;
    private View cardEvidencePhoto;
    private ImageView imageEvidencePhoto;
    
    private ProgressBar progressBar;
    private ChecklistDetailAdapter adapter;
    private int checklistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_detail);

        checklistId = getIntent().getIntExtra(EXTRA_CHECKLIST_ID, 0);
        
        textName = findViewById(R.id.textName);
        textMeta = findViewById(R.id.textMeta);
        textNotes = findViewById(R.id.textNotes);
        textSupervisorNotes = findViewById(R.id.textSupervisorNotes);
        textReadingValue = findViewById(R.id.textReadingValue);
        badgeOverallStatus = findViewById(R.id.badgeOverallStatus);
        badgeReviewStatus = findViewById(R.id.badgeReviewStatus);
        
        cardFieldNotes = findViewById(R.id.cardFieldNotes);
        cardSupervisorNotes = findViewById(R.id.cardSupervisorNotes);
        cardEvidencePhoto = findViewById(R.id.cardEvidencePhoto);
        imageEvidencePhoto = findViewById(R.id.imageEvidencePhoto);
        
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        RecyclerView recyclerView = findViewById(R.id.recyclerDetails);

        adapter = new ChecklistDetailAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonRefresh.setOnClickListener(v -> loadDetail());
        loadDetail();
    }

    private void loadDetail() {
        setLoading(true);
        ApiClient.getService(this).checklistResultById(checklistId).enqueue(new Callback<ApiEnvelope<ChecklistDetailItem>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<ChecklistDetailItem>> call, Response<ApiEnvelope<ChecklistDetailItem>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    ChecklistDetailItem item = response.body().data;
                    
                    textName.setText(safe(item.assetName));
                    String reviewerInfo = null;
                    if (item.reviewerName != null && !item.reviewerName.trim().isEmpty()) {
                        reviewerInfo = "Người duyệt: " + item.reviewerName.trim() + (item.reviewedAt != null ? " (" + formatCheckTime(item.reviewedAt) + ")" : "");
                    }
                    textMeta.setText(join(
                            item.templateName,
                            item.locationName != null ? "Vị trí: " + item.locationName : null,
                            item.checkerName != null ? "Người kiểm tra: " + item.checkerName : null,
                            "Thời gian kiểm tra: " + formatCheckTime(item.checkTime),
                            reviewerInfo
                    ));
                    
                    setStatusBadge(badgeOverallStatus, item.overallStatus, true);
                    setStatusBadge(badgeReviewStatus, item.reviewStatus, false);
                    
                    if (item.readingValue != null) {
                        textReadingValue.setVisibility(View.VISIBLE);
                        textReadingValue.setText("Chỉ số chạy máy: " + item.readingValue + " giờ");
                    } else {
                        textReadingValue.setVisibility(View.GONE);
                    }
                    
                    if (item.notes != null && !item.notes.trim().isEmpty()) {
                        cardFieldNotes.setVisibility(View.VISIBLE);
                        textNotes.setText(item.notes.trim());
                    } else {
                        cardFieldNotes.setVisibility(View.GONE);
                    }
                    
                    if (item.supervisorNotes != null && !item.supervisorNotes.trim().isEmpty()) {
                        cardSupervisorNotes.setVisibility(View.VISIBLE);
                        textSupervisorNotes.setText(item.supervisorNotes.trim());
                    } else {
                        cardSupervisorNotes.setVisibility(View.GONE);
                    }
                    
                    if (item.evidencePhoto != null && !item.evidencePhoto.trim().isEmpty()) {
                        cardEvidencePhoto.setVisibility(View.VISIBLE);
                        loadImage(item.evidencePhoto, imageEvidencePhoto);
                    } else {
                        cardEvidencePhoto.setVisibility(View.GONE);
                    }
                    
                    adapter.update(item.details);
                } else {
                    Toast.makeText(ChecklistDetailActivity.this, "Không tải được chi tiết checklist", Toast.LENGTH_SHORT).show();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<ChecklistDetailItem>> call, Throwable t) {
                Toast.makeText(ChecklistDetailActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void setStatusBadge(TextView view, String status, boolean isOverall) {
        if (status == null || status.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setText(status.trim().toUpperCase());
        
        int bg = android.graphics.Color.parseColor("#94A3B8");
        if (isOverall) {
            if ("OK".equalsIgnoreCase(status)) bg = android.graphics.Color.parseColor("#10B981");
            else if ("WARNING".equalsIgnoreCase(status)) bg = android.graphics.Color.parseColor("#F59E0B");
            else if ("NG".equalsIgnoreCase(status)) bg = android.graphics.Color.parseColor("#EF4444");
        } else {
            if ("PENDING".equalsIgnoreCase(status)) {
                view.setText("CHỜ DUYỆT");
                bg = android.graphics.Color.parseColor("#6B7280");
            } else if ("APPROVED".equalsIgnoreCase(status)) {
                view.setText("ĐÃ DUYỆT");
                bg = android.graphics.Color.parseColor("#10B981");
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                view.setText("BỊ TỪ CHỐI");
                bg = android.graphics.Color.parseColor("#F43F5E");
            }
        }
        
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(12f);
        drawable.setColor(bg);
        view.setBackground(drawable);
    }

    private void loadImage(String relativePath, ImageView imageView) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            imageView.setVisibility(View.GONE);
            return;
        }
        imageView.setVisibility(View.VISIBLE);
        
        final String fullUrl;
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            fullUrl = relativePath;
        } else {
            String baseUrl = SessionManager.getInstance(this).getBaseUrl();
            String apiOrigin = baseUrl.replaceAll("/api/?$", "");
            String s = relativePath.replace("\\", "/");
            String path;
            int u = s.toLowerCase().indexOf("/uploads/");
            if (u >= 0) {
                path = s.substring(u + 1);
            } else if (s.startsWith("uploads/")) {
                path = s;
            } else {
                String[] parts = s.split("/");
                String filename = parts[parts.length - 1];
                path = "uploads/photos/" + filename;
            }
            fullUrl = apiOrigin + "/" + path;
        }

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(fullUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                android.graphics.Bitmap myBitmap = android.graphics.BitmapFactory.decodeStream(input);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    imageView.setImageBitmap(myBitmap);
                });
            } catch (Exception e) {
                e.printStackTrace();
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    imageView.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    private String formatCheckTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        try {
            // "2026-05-07T05:08:39.000Z" -> "07/05/2026 12:08" or similar
            String clean = dateStr.replace("T", " ");
            if (clean.contains(".")) {
                clean = clean.substring(0, clean.indexOf('.'));
            }
            return clean;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(" | ");
            sb.append(value.trim());
        }
        return sb.length() == 0 ? "--" : sb.toString();
    }
}