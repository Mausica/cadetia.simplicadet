package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.JournalEntry;

import java.util.List;

public class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.ViewHolder> {
    private List<JournalEntry> journalList;
    private Context context;

    public JournalAdapter(List<JournalEntry> journalList, Context context) {
        this.journalList = journalList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_journal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JournalEntry entry = journalList.get(position);
        holder.title.setText(entry.getTitle());
        holder.subtitle.setText(entry.getSubtitle());
        holder.date.setText(entry.getDate());
        Glide.with(context)
                .load(entry.getImageUrl())
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, date;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.journalTitle);
            subtitle = itemView.findViewById(R.id.journalSubtitle);
            date = itemView.findViewById(R.id.journalDateTime);
            image = itemView.findViewById(R.id.imageJournal);
        }
    }
}