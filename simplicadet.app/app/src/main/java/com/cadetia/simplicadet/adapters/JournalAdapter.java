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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {
    private final List<JournalEntry> journalList;
    private final OnJournalClickListener onJournalClickListener;
    private final LruCache<String, Bitmap> memCache;
    private final ExecutorService imageLoadExecutor;
    private final RequestOptions glideOptions;

    public interface OnJournalClickListener {
        void onJournalClick(String journalLink);
    }

    public JournalAdapter(List<JournalEntry> journalList, OnJournalClickListener onJournalClickListener, LruCache<String, Bitmap> memCache) {
        this.journalList = journalList;
        this.onJournalClickListener = onJournalClickListener;
        this.memCache = memCache;
        this.imageLoadExecutor = Executors.newFixedThreadPool(3);

        this.glideOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .override(200, 150)
                .centerCrop();
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

        holder.itemView.setOnClickListener(v -> onJournalClickListener.onJournalClick(entry.getLink()));

        loadImageOptimized(entry.getImageUrl(), holder.imageView, holder);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    private void loadImageOptimized(String imageUrl, ImageView imageView, JournalViewHolder holder) {

        Bitmap cachedImage = memCache.get(imageUrl);
        if (cachedImage != null) {
            imageView.setImageBitmap(cachedImage);
            return;
        }

        Glide.with(imageView.getContext())
                .asBitmap()
                .load(imageUrl)
                .apply(glideOptions)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            imageView.setImageBitmap(resource);
                            memCache.put(imageUrl, resource);
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {

                        }
                    }
                });
    }

    public void cleanup() {
        if (imageLoadExecutor != null && !imageLoadExecutor.isShutdown()) {
            imageLoadExecutor.shutdown();
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