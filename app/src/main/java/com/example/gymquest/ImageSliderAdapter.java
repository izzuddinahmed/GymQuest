package com.example.gymquest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private Context context;
    private List<Map<String, Object>> imageList;
    private boolean isAdmin;
    private OnPhotoDeleteListener deleteListener;

    public interface OnPhotoDeleteListener {
        void onDelete(String photoId, int position);
    }

    public ImageSliderAdapter(Context context, List<Map<String, Object>> imageList, boolean isAdmin, OnPhotoDeleteListener deleteListener) {
        this.context = context;
        this.imageList = imageList;
        this.isAdmin = isAdmin;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Map<String, Object> item = imageList.get(position);

        // 1. Decode and Show Image
        String type = (String) item.get("type"); // "MAIN" or "USER"

        if (item.get("bitmap") != null) {
            // It's the main image passed as Bitmap
            holder.imageView.setImageBitmap((Bitmap) item.get("bitmap"));
        } else if (item.get("imageStr") != null) {
            // It's a user photo (Base64 string)
            String base64 = (String) item.get("imageStr");
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.imageView.setImageBitmap(decodedByte);
        }

        // 2. Handle Delete Button Visibility
        // Show ONLY if: User is Admin AND it is a "USER" photo (not the main logo)
        if (isAdmin && "USER".equals(type)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Photo")
                        .setMessage("Are you sure you want to delete this photo?")
                        .setPositiveButton("DELETE", (dialog, which) -> {
                            String photoId = (String) item.get("id");
                            deleteListener.onDelete(photoId, position);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnDelete;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgSlider);
            btnDelete = itemView.findViewById(R.id.btnDeletePhoto);
        }
    }
}