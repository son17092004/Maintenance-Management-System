package com.kasiz.warehousemobileapp.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.kasiz.warehousemobileapp.R;
import com.kasiz.warehousemobileapp.model.ListRow;

import java.util.List;

public class ListRowAdapter extends RecyclerView.Adapter<ListRowAdapter.VH> {

    public interface OnRowClickListener {
        void onRowClick(ListRow row);
    }

    private final List<ListRow> rows;
    private final OnRowClickListener listener;

    public ListRowAdapter(List<ListRow> rows, OnRowClickListener listener) {
        this.rows = rows;
        this.listener = listener;
    }

    public void update(List<ListRow> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_row, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ListRow row = rows.get(position);
        holder.textTitle.setText(row.title);
        holder.textSubtitle.setText(row.subtitle);
        holder.textBadge.setText(row.badge);
        holder.textDetail.setText(row.detail);

        // Dynamic badge color based on status text
        int[] colors = getBadgeColors(row.badge);
        holder.textBadge.setBackgroundColor(colors[0]);
        holder.textBadge.setTextColor(colors[1]);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRowClick(row);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    /** Returns [backgroundColor, textColor] based on status string. */
    public static int[] getBadgeColors(String badge) {
        if (badge == null) return new int[]{Color.parseColor("#E6F4F1"), Color.parseColor("#0F766E")};
        String s = badge.toUpperCase();

        // 1. Red (Critical / Issues / Failures)
        if (s.equals("QUÁ HẠN") || s.contains("OVERDUE") || s.contains("NG") || s.contains("CANCELLED") || s.contains("HỦY") || s.contains("REJECTED") || s.equals("TỪ CHỐI")) {
            return new int[]{Color.parseColor("#FEE2E2"), Color.parseColor("#991B1B")}; // Red
        }
        // 2. Yellow/Orange (Warning / Pending action / Draft)
        if (s.equals("CHỜ DUYỆT") || s.contains("PENDING_APPROVAL") || s.contains("WARNING") || s.contains("PAUSED") || s.contains("WAITING") || s.contains("ĐỊNH KỲ")) {
            return new int[]{Color.parseColor("#FEF9C3"), Color.parseColor("#854D0E")}; // Yellow/Orange
        }
        // 3. Blue (In Progress / Pending execution)
        if (s.equals("CHỜ TH") || s.equals("PENDING") || s.contains("IN_PROGRESS") || s.contains("ĐANG")) {
            return new int[]{Color.parseColor("#DBEAFE"), Color.parseColor("#1D4ED8")}; // Blue
        }
        // 4. Green (Success / Approved / Completed)
        if (s.contains("COMPLETED") || s.contains("OK") || s.contains("APPROVED") || s.contains("ĐÃ DUYỆT") || s.contains("HOÀN THÀNH")) {
            return new int[]{Color.parseColor("#DCFCE7"), Color.parseColor("#166534")}; // Green
        }
        // 5. Gray (Draft)
        if (s.equals("BẢN NHÁP") || s.contains("DRAFT")) {
            return new int[]{Color.parseColor("#F1F5F9"), Color.parseColor("#64748B")}; // Gray
        }
        if (s.contains("NEW") || s.contains("CHƯA ĐỌC")) {
            return new int[]{Color.parseColor("#F3E8FF"), Color.parseColor("#6B21A8")};
        }
        if (s.contains("ĐÃ ĐỌC")) {
            return new int[]{Color.parseColor("#F1F5F9"), Color.parseColor("#64748B")};
        }
        // Default teal
        return new int[]{Color.parseColor("#E6F4F1"), Color.parseColor("#0F766E")};
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final TextView textTitle;
        final TextView textSubtitle;
        final TextView textBadge;
        final TextView textDetail;

        VH(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardRow);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textBadge = itemView.findViewById(R.id.textBadge);
            textDetail = itemView.findViewById(R.id.textDetail);
        }
    }
}