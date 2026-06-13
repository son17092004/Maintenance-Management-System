package com.kasiz.warehousemobileapp.ui;

import android.graphics.Color;
import android.graphics.Typeface;
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

    public interface OnClickListener {
        void onClick(NotificationItem item);
    }

    private final List<NotificationItem> items;
    private OnClickListener clickListener;

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
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
        holder.textTime.setText(formatTime(item.createdAt));

        if (!item.isRead()) {
            // Unread: bold, blue dot, blue badge
            holder.viewDot.setVisibility(View.VISIBLE);
            holder.textMessage.setTypeface(null, Typeface.BOLD);
            holder.textMessage.setTextColor(Color.parseColor("#1C2430"));
            holder.cardView.setCardBackgroundColor(Color.parseColor("#F0F9FF"));
            holder.textBadge.setText("Chưa đọc");
            holder.textBadge.setBackgroundColor(Color.parseColor("#DBEAFE"));
            holder.textBadge.setTextColor(Color.parseColor("#1D4ED8"));
        } else {
            // Read: normal style
            holder.viewDot.setVisibility(View.GONE);
            holder.textMessage.setTypeface(null, Typeface.NORMAL);
            holder.textMessage.setTextColor(Color.parseColor("#6B7280"));
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.textBadge.setText("Đã đọc");
            holder.textBadge.setBackgroundColor(Color.parseColor("#F1F5F9"));
            holder.textBadge.setTextColor(Color.parseColor("#64748B"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }

    /** Shorten ISO datetime to readable short format */
    private String formatTime(String iso) {
        if (iso == null || iso.isEmpty()) return "--";
        // e.g. "2024-05-01T14:32:00.000Z" -> "01/05 14:32"
        try {
            String datePart = iso.substring(0, 10); // YYYY-MM-DD
            String timePart = iso.substring(11, 16); // HH:mm
            String[] dp = datePart.split("-");
            return dp[2] + "/" + dp[1] + " " + timePart;
        } catch (Exception e) {
            return iso.length() > 16 ? iso.substring(0, 16) : iso;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final View viewDot;
        final TextView textMessage;
        final TextView textBadge;
        final TextView textTime;

        VH(@NonNull View itemView) {
            super(itemView);
            cardView    = itemView.findViewById(R.id.cardNotif);
            viewDot     = itemView.findViewById(R.id.viewDot);
            textMessage = itemView.findViewById(R.id.textMessage);
            textBadge   = itemView.findViewById(R.id.textBadge);
            textTime    = itemView.findViewById(R.id.textTime);
        }
    }
}