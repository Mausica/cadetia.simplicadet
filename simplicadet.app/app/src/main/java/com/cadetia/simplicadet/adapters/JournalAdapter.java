package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
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
        return new JournalViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        JournalEntry entry = journalList.get(position);
        holder.title.setText(entry.getTitle());
        holder.subtitle.setText(entry.getSubtitle());
        holder.date.setText(entry.getDate());

        // Handle click event for the journal entry
        holder.itemView.setOnClickListener(v -> onJournalClickListener.onJournalClick(entry.getLink()));

        // Load the image for the journal entry
        loadImage(entry.getImageUrl(), holder.imageView);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    private void loadImage(String imageUrl, ImageView imageView) {
        Bitmap cachedImage = memCache.get(imageUrl); // Try to get the image from cache
        if (cachedImage != null) {
            imageView.setImageBitmap(cachedImage);  // If image is cached, use it
        } else {
            Glide.with(imageView.getContext())
                    .asBitmap()
                    .load(imageUrl)
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))  // Cache image to disk
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            imageView.setImageBitmap(resource);  // Set the image in the ImageView
                            memCache.put(imageUrl, resource);  // Store the image in cache
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                        }
                    });
        }
    }

    static class JournalViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, date;
        ImageView imageView;

        public JournalViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.journal_title);
            subtitle = itemView.findViewById(R.id.journal_subtitle);
            date = itemView.findViewById(R.id.journal_date);
            imageView = itemView.findViewById(R.id.journal_image);
        }
    }
}
