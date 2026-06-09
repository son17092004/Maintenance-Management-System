package com.kasiz.warehousemobileapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.kasiz.warehousemobileapp.R;
import com.kasiz.warehousemobileapp.model.NotificationItem;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private final List<NotificationItem> items;

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    public void update(List<NotificationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationItem item = items.get(position);
        holder.textMessage.setText(item.message == null ? "Không có nội dung" : item.message);
        holder.textMeta.setText((item.isRead ? "Đã đọc" : "Chưa đọc") + " • " + safe(item.createdAt));
        holder.cardView.setAlpha(item.isRead ? 0.75f : 1f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView textMessage;
        final TextView textMeta;

        VH(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardNotification);
            textMessage = itemView.findViewById(R.id.textMessage);
            textMeta = itemView.findViewById(R.id.textMeta);
        }
    }
}