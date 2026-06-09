package com.kasiz.warehousemobileapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.kasiz.warehousemobileapp.R;
import com.kasiz.warehousemobileapp.model.AssetPhotoItem;

import java.util.List;

public class AssetPhotoAdapter extends RecyclerView.Adapter<AssetPhotoAdapter.VH> {

    public interface Listener {
        void onDelete(AssetPhotoItem photo);
    }

    private final Context context;
    private final List<AssetPhotoItem> items;
    private final Listener listener;
    private final String origin;

    public AssetPhotoAdapter(Context context, List<AssetPhotoItem> items, Listener listener, String origin) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.origin = origin;
    }

    public void update(List<AssetPhotoItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset_photo, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AssetPhotoItem photo = items.get(position);
        holder.textCaption.setText(text(photo.caption, "Không có mô tả"));
        holder.textMeta.setText(text(photo.uploadedByName, "--") + " • " + text(photo.createdAt, "--"));
        holder.buttonDelete.setVisibility(listener == null ? View.GONE : View.VISIBLE);
        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(photo);
        });
        Glide.with(context)
                .load(resolveUrl(photo.filePath))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.imagePhoto);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String resolveUrl(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) return null;
        String value = filePath.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        String base = origin == null ? "" : origin;
        if (!base.endsWith("/")) base += "/";
        return base + value.replaceFirst("^/", "");
    }

    private String text(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final ImageView imagePhoto;
        final TextView textCaption;
        final TextView textMeta;
        final ImageButton buttonDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardPhoto);
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
            textCaption = itemView.findViewById(R.id.textCaption);
            textMeta = itemView.findViewById(R.id.textMeta);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}