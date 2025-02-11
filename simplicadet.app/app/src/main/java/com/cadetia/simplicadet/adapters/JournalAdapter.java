package com.cadetia.simplicadet.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.JournalEntry;

import java.util.List;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {
    private final List<JournalEntry> journalList;
    private final OnJournalClickListener onJournalClickListener;
    private final LruCache<String, Bitmap> memCache;

    public interface OnJournalClickListener {
        void onJournalClick(String journalLink);
    }

    public JournalAdapter(List<JournalEntry> journalList, OnJournalClickListener onJournalClickListener, LruCache<String, Bitmap> memCache) {
        this.journalList = journalList;
        this.onJournalClickListener = onJournalClickListener;
        this.memCache = memCache;
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal, parent, false);
        return new JournalViewHolder(itemView, memCache);  // Pass memCache to the ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalEntry entry = journalList.get(position);
        holder.title.setText(entry.getTitle());
        holder.subtitle.setText(entry.getSubtitle());
        holder.date.setText(entry.getDate());

        holder.itemView.setOnClickListener(v -> {
            if (onJournalClickListener != null) {
                onJournalClickListener.onJournalClick(entry.getLink());
            }
        });

        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    static class JournalViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, date;
        ImageView image;
        LinearLayout layoutJournal;
        LruCache<String, Bitmap> memCache;  // Reference to memCache

        public JournalViewHolder(@NonNull View itemView, LruCache<String, Bitmap> memCache) {
            super(itemView);
            this.memCache = memCache;  // Set memCache
            title = itemView.findViewById(R.id.journalTitle);
            subtitle = itemView.findViewById(R.id.journalSubtitle);
            date = itemView.findViewById(R.id.journalDateTime);
            image = itemView.findViewById(R.id.imageJournal);
            layoutJournal = itemView.findViewById(R.id.layoutJournal);
        }

        public void bind(JournalEntry entry) {
            Context context = itemView.getContext();
            String imageUrl = entry.getImageUrl();

            Bitmap cachedImage = memCache.get(imageUrl); // Check if the bitmap is already cached
            if (cachedImage != null) {
                // If the image is cached, use it
                image.setImageBitmap(cachedImage);
            } else {
                // Otherwise, load the image using Glide
                RequestOptions options = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL);

                Glide.with(context)
                        .asBitmap() // Ensure Glide returns a Bitmap
                        .load(imageUrl)
                        .apply(options)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                // Once the image is loaded, add it to the cache
                                memCache.put(imageUrl, resource);
                                // Set the image to the ImageView
                                image.setImageBitmap(resource);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                // Handle the cleanup when the view is no longer in use
                            }
                        });
            }
        }
    }
}
