package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.DestinationItem;

import java.util.Collections;
import java.util.List;

public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder> {

    private static final String TAG = "DestinationAdapter";
    private List<DestinationItem> destinationItems;
    private Context context;
    private LruCache<String, Bitmap> memCache;
    private OnDestinationClickListener listener;

    public interface OnDestinationClickListener {
        void onDestinationClick(DestinationItem destination);
    }

    public DestinationAdapter(List<DestinationItem> destinationItems, Context context, LruCache<String, Bitmap> memCache, OnDestinationClickListener listener) {
        this.destinationItems = destinationItems;
        this.context = context;
        this.memCache = memCache;
        this.listener = listener;
        // Shuffle the items if there are more than 3
        if (destinationItems.size() > 3) {
            Collections.shuffle(destinationItems);
        }
    }

    @NonNull
    @Override
    public DestinationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_destination, parent, false);
        return new DestinationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DestinationViewHolder holder, int position) {
        DestinationItem item = destinationItems.get(position);

        // Load image with Glide
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop();

        Glide.with(context)
                .load(item.getImageUrl())
                .apply(requestOptions)
                .into(holder.imageView);

        // Apply animation when binding
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_right));

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDestinationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return destinationItems.size();
    }

    public void refreshData(List<DestinationItem> newItems) {
        destinationItems.clear();
        destinationItems.addAll(newItems);
        // Shuffle the items if there are more than 3
        if (destinationItems.size() > 3) {
            Collections.shuffle(destinationItems);
        }
        notifyDataSetChanged();
    }

    public static class DestinationViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public DestinationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.destination_image);
        }
    }
}
