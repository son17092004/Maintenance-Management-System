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
import com.kasiz.warehousemobileapp.R;
import com.kasiz.warehousemobileapp.model.WorkOrderItem;

import java.util.List;

public class WorkOrderPhotoAdapter extends RecyclerView.Adapter<WorkOrderPhotoAdapter.VH> {

    public interface Listener {
        void onDelete(WorkOrderItem.Photo photo);
    }

    private final Context context;
    private final List<WorkOrderItem.Photo> items;
    private final Listener listener;
    private final String origin;

    public WorkOrderPhotoAdapter(Context context, List<WorkOrderItem.Photo> items, Listener listener, String origin) {
        this.context = context;
        this.items = items;
        this.listener = listener;
        this.origin = origin;
    }

    public void update(List<WorkOrderItem.Photo> newItems) {
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
        WorkOrderItem.Photo photo = items.get(position);
        holder.textCaption.setText("Ảnh #" + photo.photoId);
        holder.textMeta.setText(photo.uploadedAt == null ? "--" : photo.uploadedAt);
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

    static class VH extends RecyclerView.ViewHolder {
        final ImageView imagePhoto;
        final TextView textCaption;
        final TextView textMeta;
        final ImageButton buttonDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
            textCaption = itemView.findViewById(R.id.textCaption);
            textMeta = itemView.findViewById(R.id.textMeta);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
