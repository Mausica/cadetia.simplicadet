package com.cadetia.simplicadet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    class DocumentViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout documentLayout;
        private TextView textTitle, textSubtitle, textDate;
        private ImageView documentImage;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            documentLayout = itemView.findViewById(R.id.layoutNote);
            textTitle = itemView.findViewById(R.id.textTitle);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textDate = itemView.findViewById(R.id.textDateTime);
            documentImage = itemView.findViewById(R.id.imageNote);
        }

        void setDocument(FirestoreDocument document) {
            textTitle.setText(document.getTitle());
            textSubtitle.setText(document.getSubtitle());
            if (document.getCategory() != null && !document.getCategory().isEmpty()) {
                textDate.setVisibility(View.VISIBLE);
                textDate.setText(document.getCategory());
            } else {
                textDate.setVisibility(View.GONE);
            }
            if (document.getImageUrl() != null && !document.getImageUrl().isEmpty()) {
                documentImage.setVisibility(View.VISIBLE);
                Glide.with(documentImage.getContext())
                        .load(document.getImageUrl())
                        .into(documentImage);
            } else {
                documentImage.setVisibility(View.GONE);
            }
            documentLayout.setOnClickListener(v -> {
                if (documentListener != null) {
                    documentListener.onDocumentClicked(document, getAdapterPosition());
                }
            });
        }
    }
}