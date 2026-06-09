package com.kasiz.warehousemobileapp.ui;

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
        holder.cardView.setAlpha("--".equals(row.badge) ? 0.82f : 1f);
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