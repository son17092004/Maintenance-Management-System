package com.kasiz.warehousemobileapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kasiz.warehousemobileapp.R;
import com.kasiz.warehousemobileapp.model.ChecklistDetailRow;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import java.util.List;

public class ChecklistDetailAdapter extends RecyclerView.Adapter<ChecklistDetailAdapter.VH> {

    private final List<ChecklistDetailRow> items;

    public ChecklistDetailAdapter(List<ChecklistDetailRow> items) {
        this.items = items;
    }

    public void update(List<ChecklistDetailRow> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checklist_detail_row, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChecklistDetailRow row = items.get(position);
        Context context = holder.itemView.getContext();

        holder.textQuestionText.setText((position + 1) + ". " + row.questionText);
        holder.textTypeBadge.setText(translateInputType(row.inputType));

        if ("Photo".equalsIgnoreCase(row.inputType)) {
            holder.textAnswerValue.setVisibility(View.GONE);
            holder.imageQuestionPhoto.setVisibility(View.VISIBLE);
            loadImage(context, row.answerValue, holder.imageQuestionPhoto);
        } else {
            holder.imageQuestionPhoto.setVisibility(View.GONE);
            holder.textAnswerValue.setVisibility(View.VISIBLE);
            holder.textAnswerValue.setText(row.answerValue);
        }

        if (row.isOK()) {
            holder.textResultBadge.setText("ĐẠT (OK)");
            holder.textResultBadge.setTextColor(Color.parseColor("#0F766E"));
            holder.textResultBadge.setBackgroundColor(Color.parseColor("#E6F4F1"));
        } else {
            holder.textResultBadge.setText("KHÔNG ĐẠT (NG)");
            holder.textResultBadge.setTextColor(Color.parseColor("#B91C1C"));
            holder.textResultBadge.setBackgroundColor(Color.parseColor("#FEE2E2"));
        }
    }

    private String translateInputType(String type) {
        if ("PassFail".equalsIgnoreCase(type)) return "ĐẠT / KHÔNG ĐẠT";
        if ("Numeric".equalsIgnoreCase(type)) return "TRỊ SỐ";
        if ("Text".equalsIgnoreCase(type)) return "NHẬP CHỮ";
        if ("Photo".equalsIgnoreCase(type)) return "HÌNH ẢNH";
        if ("Range".equalsIgnoreCase(type)) return "KHOẢNG GIỚI HẠN";
        return type == null ? "" : type.toUpperCase();
    }

    private void loadImage(Context context, String relativePath, ImageView imageView) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            imageView.setVisibility(View.GONE);
            return;
        }
        imageView.setVisibility(View.VISIBLE);
        final String fullUrl;
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            fullUrl = relativePath;
        } else {
            String baseUrl = SessionManager.getInstance(context).getBaseUrl();
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

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView textQuestionText;
        final TextView textTypeBadge;
        final TextView textAnswerValue;
        final ImageView imageQuestionPhoto;
        final TextView textResultBadge;

        VH(@NonNull View itemView) {
            super(itemView);
            textQuestionText = itemView.findViewById(R.id.textQuestionText);
            textTypeBadge = itemView.findViewById(R.id.textTypeBadge);
            textAnswerValue = itemView.findViewById(R.id.textAnswerValue);
            imageQuestionPhoto = itemView.findViewById(R.id.imageQuestionPhoto);
            textResultBadge = itemView.findViewById(R.id.textResultBadge);
        }
    }
}
