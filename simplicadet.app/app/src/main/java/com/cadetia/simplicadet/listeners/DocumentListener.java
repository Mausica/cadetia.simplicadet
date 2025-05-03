package com.cadetia.simplicadet.listeners;

import com.cadetia.simplicadet.entities.Document;

public interface DocumentListener {
    void onDocumentClicked(Document document, int position);
}