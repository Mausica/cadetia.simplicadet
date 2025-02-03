package com.learn.simplify.listeners;

import com.learn.simplify.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);

    void onNoteSaved();
}
