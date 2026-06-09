package com.kasiz.warehousemobileapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.kasiz.warehousemobileapp.storage.SessionManager;
import com.kasiz.warehousemobileapp.network.ApiClient;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApiClient.reset();

        SessionManager session = SessionManager.getInstance(this);
        Class<?> target;
        if (session.isLoggedIn()) {
            if (session.isFieldWorker()) {
                target = DashboardActivity.class;
            } else {
                session.clear();
                target = LoginActivity.class;
            }
        } else {
            target = LoginActivity.class;
        }

        startActivity(new Intent(this, target));
        finish();
    }
}