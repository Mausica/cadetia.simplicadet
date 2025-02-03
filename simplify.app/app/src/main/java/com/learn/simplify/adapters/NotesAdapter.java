package com.learn.simplify.adapters;

import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.learn.simplify.R;
import com.learn.simplify.entities.Note;
import com.learn.simplify.listeners.NotesListener;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.List;

public class NotesAdapter  extends  RecyclerView.Adapter<NotesAdapter.NoteViewHolder>{

    private final List<Note> notes;
    private final NotesListener notesListener;



    public NotesAdapter(List<Note> notes, NotesListener notesListener) {

        this.notes = notes;
        this.notesListener = notesListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_note,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.setNote(notes.get(position));
        holder.layoutNote.setOnClickListener(v -> notesListener.onNoteClicked(notes.get(position),position));

    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder{

        TextView textTitle, textSubtitle, textDateTime;
        LinearLayout layoutNote;
        RoundedImageView imageNote;
        NoteViewHolder(@NonNull View itemview){
            super(itemview);
            textTitle = itemview.findViewById(R.id.textTitle);
            textSubtitle = itemview.findViewById(R.id.textSubtitle);
            textDateTime = itemview.findViewById(R.id.textDateTime);
            layoutNote = itemview.findViewById(R.id.layoutNote);
            imageNote = itemview.findViewById(R.id.imageNote);

        }

        void setNote(Note note){
            textTitle.setText(note.getTitle());
            if(note.getTitle().trim().isEmpty()) {
                textSubtitle.setVisibility(View.GONE);
            } else {
                textSubtitle.setText(note.getSubtitle());
            }
            textDateTime.setText(note.getDateTime());
            int color;
            int textColor;

            // Color Note
            if(note.getColor()!= null) {
                color = Color.parseColor(note.getColor());
            } else {
                color = Color.parseColor("#2C2C2C");
            }

            // Calculate brightness of the note color
            double brightness = ColorUtils.calculateLuminance(color);

            // Background Color
            if (color == Color.parseColor("#2C2C2C")){
                textColor = Color.parseColor("#7d7d7d");
                textDateTime.setTextColor(textColor);
                textColor = Color.WHITE;
                textTitle.setTextColor(textColor);
                textSubtitle.setTextColor(textColor);
            } else {
                if (brightness > 0.4) {
                    // Background is light, set text color to dark
                    textColor = Color.BLACK;
                } else {
                    // Background is dark, set text color to light
                    textColor = Color.WHITE;
                }
                // Set text color for title, subtitle, and date time
                textTitle.setTextColor(textColor);
                textSubtitle.setTextColor(textColor);
                textDateTime.setTextColor(textColor);
            }

            ViewCompat.setBackgroundTintList(layoutNote, ColorStateList.valueOf(color));

            if(note.getImagePath() != null){
                imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                imageNote.setVisibility(View.VISIBLE);
            } else {
                imageNote.setVisibility(View.GONE);
            }

        }

    }
}
