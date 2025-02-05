package com.cadetia.simplicadet.listeners;

import com.cadetia.simplicadet.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);

    void onNoteSaved();
}
