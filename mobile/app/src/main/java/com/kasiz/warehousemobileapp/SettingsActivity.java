package com.kasiz.warehousemobileapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kasiz.warehousemobileapp.network.ApiClient;
import com.kasiz.warehousemobileapp.storage.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText inputBaseUrl = findViewById(R.id.inputBaseUrl);
        Button buttonSave = findViewById(R.id.buttonSave);

        SessionManager session = SessionManager.getInstance(this);
        inputBaseUrl.setText(session.getBaseUrl());

        buttonSave.setOnClickListener(v -> {
            session.saveBaseUrl(inputBaseUrl.getText().toString());
            ApiClient.reset();
            Toast.makeText(this, "Đã lưu base URL", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}