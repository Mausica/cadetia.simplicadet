package com.cadetia.simplicadet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.entities.FirestoreDocument;
import com.cadetia.simplicadet.listeners.DocumentListener;

import java.util.List;

public class FirestoreDocumentsAdapter extends RecyclerView.Adapter<FirestoreDocumentsAdapter.DocumentViewHolder> {

    private List<FirestoreDocument> documents;
    private DocumentListener documentListener;

    public FirestoreDocumentsAdapter(List<FirestoreDocument> documents, DocumentListener documentListener) {
        this.documents = documents;
        this.documentListener = documentListener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DocumentViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_note,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.setDocument(documents.get(position));
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    public class DocumentViewHolder extends RecyclerView.ViewHolder {

        TextView textTitle;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
        }

        void setDocument(FirestoreDocument document) {
            String displayTitle = document.getTitle();
            if (document.getSubtitle() != null && !document.getSubtitle().isEmpty()) {
                displayTitle += "\n" + document.getSubtitle();
            }
            textTitle.setText(displayTitle);
            itemView.setOnClickListener(v -> documentListener.onDocumentClicked(document, getAdapterPosition()));
        }
    }
}