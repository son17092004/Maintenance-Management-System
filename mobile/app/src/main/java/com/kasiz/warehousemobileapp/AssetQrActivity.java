package com.kasiz.warehousemobileapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AssetQrActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_QR_PAYLOAD = "extra_qr_payload";
    public static final String EXTRA_DATA_URL = "extra_data_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_qr);

        TextView textTitle = findViewById(R.id.textTitle);
        TextView textPayload = findViewById(R.id.textPayload);
        ImageView imageQr = findViewById(R.id.imageQr);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String payload = getIntent().getStringExtra(EXTRA_QR_PAYLOAD);
        String dataUrl = getIntent().getStringExtra(EXTRA_DATA_URL);

        textTitle.setText(title == null ? "Mã QR tài sản" : title);
        textPayload.setText(payload == null ? "" : payload);

        if (dataUrl != null && dataUrl.contains(",")) {
            String base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            imageQr.setImageBitmap(bitmap);
        }
    }
}