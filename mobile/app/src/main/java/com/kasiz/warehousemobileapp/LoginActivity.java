package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.kasiz.warehousemobileapp.model.ApiEnvelope;
import com.kasiz.warehousemobileapp.model.MeProfile;
import com.kasiz.warehousemobileapp.model.LoginRequest;
import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText inputIdentifier;
    private EditText inputPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputIdentifier = findViewById(R.id.inputIdentifier);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);
        View buttonSettings = findViewById(R.id.buttonSettings);

        buttonLogin.setOnClickListener(v -> login());
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, SettingsActivity.class));
            });
        }
    }

    private void login() {
        String identifier = inputIdentifier.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(identifier) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        ApiClient.getService(this).login(new LoginRequest(identifier, password)).enqueue(new Callback<ApiEnvelope<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<JsonObject>> call, Response<ApiEnvelope<JsonObject>> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null || !response.body().success) {
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                SessionManager.getInstance(LoginActivity.this).saveLogin(identifier);
                loadProfileThenOpenDashboard();
            }

            @Override
            public void onFailure(Call<ApiEnvelope<JsonObject>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Không kết nối được server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileThenOpenDashboard() {
        setLoading(true);
        ApiClient.getService(this).me().enqueue(new Callback<ApiEnvelope<MeProfile>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<MeProfile>> call, Response<ApiEnvelope<MeProfile>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    try {
                        MeProfile profile = response.body().data;
                        SessionManager.getInstance(LoginActivity.this).saveProfile(profile);

                        if (!SessionManager.getInstance(LoginActivity.this).isFieldWorker()) {
                            SessionManager.getInstance(LoginActivity.this).clear();
                            Toast.makeText(LoginActivity.this, "Tài khoản không có quyền truy cập. Chỉ KTV hiện trường mới được sử dụng ứng dụng di động.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        startDashboard();
                    } catch (Exception e) {
                        SessionManager.getInstance(LoginActivity.this).clear();
                        Toast.makeText(LoginActivity.this, "Lỗi xử lý: " + e.getMessage() + " (Class: " + e.getClass().getSimpleName() + ")", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    SessionManager.getInstance(LoginActivity.this).clear();
                    Toast.makeText(LoginActivity.this, "Không lấy được thông tin tài khoản", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiEnvelope<MeProfile>> call, Throwable t) {
                setLoading(false);
                SessionManager.getInstance(LoginActivity.this).clear();
                Toast.makeText(LoginActivity.this, "Lỗi kết nối khi tải cấu hình", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDashboard() {
        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!loading);
    }
}