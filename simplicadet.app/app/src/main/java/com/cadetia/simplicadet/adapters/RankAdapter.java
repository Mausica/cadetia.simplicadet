package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.RankModel;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.List;

public class RankAdapter extends RecyclerView.Adapter<RankAdapter.ViewHolder> {

    private List<RankModel> rankList;
    private Context context;
    private LruCache<String, Bitmap> memCache;

    public RankAdapter(List<RankModel> rankList, Context context, LruCache<String, Bitmap> memCache) {
        this.rankList = rankList;
        this.context = context;
        this.memCache = memCache;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rank, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RankModel rank = rankList.get(position);
        holder.rankTitle.setText(rank.getName());

        // Load image from URL using cached bitmap or load it
        Bitmap cachedBitmap = memCache.get(rank.getImageUrl());
        if (cachedBitmap != null) {
            holder.imageRank.setImageBitmap(cachedBitmap);
        } else {
            // Use Glide or similar library to load image
            Glide.with(context)
                    .load(rank.getImageUrl())
                    .placeholder(R.drawable.background_nothing_rounded)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                            memCache.put(rank.getImageUrl(), bitmap);
                            holder.imageRank.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.imageRank.setImageDrawable(placeholder);
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return rankList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView rankTitle;
        private RoundedImageView imageRank;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTitle = itemView.findViewById(R.id.rankTitle);
            imageRank = itemView.findViewById(R.id.imageRank);
        }
    }
}
