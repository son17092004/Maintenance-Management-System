package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.MeProfile;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView textName;
    private TextView textMeta;
    private TextView textSummary;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textName = findViewById(R.id.textName);
        textMeta = findViewById(R.id.textMeta);
        textSummary = findViewById(R.id.textSummary);
        progressBar = findViewById(R.id.progressBar);
        Button buttonRefresh = findViewById(R.id.buttonRefresh);
        Button buttonSettings = findViewById(R.id.buttonSettings);
        Button buttonLogout = findViewById(R.id.buttonLogout);

        buttonRefresh.setOnClickListener(v -> loadMe());
        buttonSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        buttonLogout.setOnClickListener(v -> logout());

        loadMe();
    }

    private void loadMe() {
        setLoading(true);
        ApiClient.getService(this).me().enqueue(new Callback<ApiEnvelope<MeProfile>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<MeProfile>> call, Response<ApiEnvelope<MeProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    MeProfile profile = response.body().data;
                    textName.setText(safe(profile.fullName));
                    textMeta.setText(join(profile.username, profile.email, profile.positionName, profile.departmentName));
                    if (profile.fieldWorkSummary != null) {
                        textSummary.setText(join(profile.fieldWorkSummary.availability, profile.fieldWorkSummary.headline, profile.fieldWorkSummary.detail));
                    } else {
                        textSummary.setText(join(profile.craftLevel, profile.specialty));
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Không tải được hồ sơ", Toast.LENGTH_SHORT).show();
                }
                setLoading(false);
            }

            @Override
            public void onFailure(Call<ApiEnvelope<MeProfile>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void logout() {
        ApiClient.getService(this).logout().enqueue(new Callback<ApiEnvelope<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                finishLogout();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        SessionManager.getInstance(this).clear();
        ApiClient.clearCookies();
        startActivity(new Intent(this, MainActivity.class));
        finish();
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