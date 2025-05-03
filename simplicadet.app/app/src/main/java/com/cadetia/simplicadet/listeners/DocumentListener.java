package com.cadetia.simplicadet.listeners;

import com.cadetia.simplicadet.entities.FirestoreDocument;

public interface DocumentListener {
    void onDocumentClicked(FirestoreDocument document, int position);
}