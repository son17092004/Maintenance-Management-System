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
import com.kasiz.warehousemobileapp.model.ChecklistDetailItem;
import com.kasiz.warehousemobileapp.model.ListRow;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.ui.ListRowAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChecklistDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CHECKLIST_ID = "extra_checklist_id";

    private TextView textName;
    private TextView textMeta;
    private TextView textNotes;
    private ProgressBar progressBar;
    private ListRowAdapter adapter;
    private int checklistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checklist_detail);

        checklistId = getIntent().getIntExtra(EXTRA_CHECKLIST_ID, 0);
        textName = findViewById(R.id.textName);
        textMeta = findViewById(R.id.textMeta);
        textNotes = findViewById(R.id.textNotes);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        RecyclerView recyclerView = findViewById(R.id.recyclerDetails);

        adapter = new ListRowAdapter(new ArrayList<>(), null);
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
                    textMeta.setText(join(item.templateName, item.checkerName, item.overallStatus, item.reviewStatus, item.checkTime));
                    textNotes.setText(safe(item.notes));
                    List<ListRow> detailRows = new ArrayList<>();
                    if (item.details != null) {
                        for (int i = 0; i < item.details.size(); i++) {
                            var d = item.details.get(i);
                            detailRows.add(new ListRow(i + 1, "checklist", safe(d.questionText), safe(d.inputType), d.isOK ? "OK" : "NG", safe(d.answerValue)));
                        }
                    }
                    adapter.update(detailRows);
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

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    private String join(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(" • ");
            sb.append(value.trim());
        }
        return sb.length() == 0 ? "--" : sb.toString();
    }
}