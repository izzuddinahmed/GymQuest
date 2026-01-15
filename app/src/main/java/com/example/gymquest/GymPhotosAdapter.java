package com.example.gymquest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class GymPhotosAdapter extends RecyclerView.Adapter<GymPhotosAdapter.PhotoViewHolder> {

    private List<Map<String, Object>> photoList;
    private Context context;
    private boolean isAdmin;
    private OnPhotoDeleteListener deleteListener;

    // Interface to communicate back to Activity
    public interface OnPhotoDeleteListener {
        void onDelete(String photoId);
    }

    public GymPhotosAdapter(Context context, List<Map<String, Object>> photoList, boolean isAdmin, OnPhotoDeleteListener deleteListener) {
        this.context = context;
        this.photoList = photoList;
        this.isAdmin = isAdmin;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gym_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Map<String, Object> photoData = photoList.get(position);
        String imageStr = (String) photoData.get("imageStr");

        if (imageStr != null && !imageStr.isEmpty()) {
            byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.imageView.setImageBitmap(decodedByte);
        }

        // --- ADMIN DELETE LOGIC ---
        if (isAdmin) {
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Photo?")
                        .setMessage("Are you sure you want to delete this photo?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            String photoId = (String) photoData.get("id");
                            deleteListener.onDelete(photoId);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgGymPhoto);
        }
    }
}