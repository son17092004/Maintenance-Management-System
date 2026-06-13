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

    private TextView textAvatar;
    private TextView textName;
    private TextView textRole;
    private TextView textUsername;
    private TextView textEmail;
    private TextView textPhone;
    private TextView textDepartment;
    private TextView textPosition;
    private TextView textCraftLevel;
    private View cardWorkStatus;
    private TextView textAvailability;
    private TextView textWorkHeadline;
    private TextView textWorkDetail;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textAvatar      = findViewById(R.id.textAvatar);
        textName        = findViewById(R.id.textName);
        textRole        = findViewById(R.id.textRole);
        textUsername    = findViewById(R.id.textUsername);
        textEmail       = findViewById(R.id.textEmail);
        textPhone       = findViewById(R.id.textPhone);
        textDepartment  = findViewById(R.id.textDepartment);
        textPosition    = findViewById(R.id.textPosition);
        textCraftLevel  = findViewById(R.id.textCraftLevel);
        cardWorkStatus  = findViewById(R.id.cardWorkStatus);
        textAvailability  = findViewById(R.id.textAvailability);
        textWorkHeadline  = findViewById(R.id.textWorkHeadline);
        textWorkDetail    = findViewById(R.id.textWorkDetail);
        progressBar     = findViewById(R.id.progressBar);

        Button buttonRefresh  = findViewById(R.id.buttonRefresh);
        Button buttonSettings = findViewById(R.id.buttonSettings);
        Button buttonLogout   = findViewById(R.id.buttonLogout);

        buttonRefresh.setOnClickListener(v -> loadMe());
        buttonSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        buttonLogout.setOnClickListener(v -> logout());

        // Try show cached profile immediately while loading
        MeProfile cached = SessionManager.getInstance(this).getCachedProfile();
        if (cached != null) bindProfile(cached);

        loadMe();
    }

    private void loadMe() {
        setLoading(true);
        ApiClient.getService(this).me().enqueue(new Callback<ApiEnvelope<MeProfile>>() {
            @Override
            public void onResponse(Call<ApiEnvelope<MeProfile>> call, Response<ApiEnvelope<MeProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success && response.body().data != null) {
                    MeProfile profile = response.body().data;
                    SessionManager.getInstance(ProfileActivity.this).saveProfile(profile);
                    bindProfile(profile);
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

    private void bindProfile(MeProfile profile) {
        // Avatar: first letter of full name
        String name = safe(profile.fullName);
        textAvatar.setText(name.equals("--") ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
        textName.setText(name);

        // Role badge text
        String roleText = safe(profile.positionName);
        if (!roleText.equals("--") && profile.craftLevel != null && !profile.craftLevel.isEmpty()) {
            roleText += " · Bậc " + profile.craftLevel;
        }
        textRole.setText(roleText);

        textUsername.setText(safe(profile.username));
        textEmail.setText(safe(profile.email));
        textPhone.setText(safe(profile.phone));
        textDepartment.setText(safe(profile.departmentName));
        textPosition.setText(safe(profile.positionName));

        // Craft level + specialty
        String craft = safe(profile.craftLevel);
        String specialty = safe(profile.specialty);
        if ("--".equals(craft) && "--".equals(specialty)) {
            textCraftLevel.setText("--");
        } else if ("--".equals(craft)) {
            textCraftLevel.setText(specialty);
        } else if ("--".equals(specialty)) {
            textCraftLevel.setText("Bậc " + craft);
        } else {
            textCraftLevel.setText("Bậc " + craft + " · " + specialty);
        }

        // Work status section
        if (profile.fieldWorkSummary != null) {
            cardWorkStatus.setVisibility(View.VISIBLE);
            textAvailability.setText(safe(profile.fieldWorkSummary.availability));
            textWorkHeadline.setText(safe(profile.fieldWorkSummary.headline));
            textWorkDetail.setText(safe(profile.fieldWorkSummary.detail));
        } else {
            cardWorkStatus.setVisibility(View.GONE);
        }
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
}