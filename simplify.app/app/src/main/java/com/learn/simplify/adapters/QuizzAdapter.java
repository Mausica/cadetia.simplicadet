package com.learn.simplify.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.learn.simplify.R;
import com.learn.simplify.activities.QuestionsActivity;
import com.learn.simplify.model.Quizz;

import java.util.List;

public class QuizzAdapter extends RecyclerView.Adapter<QuizzAdapter.QuizzViewHolder> {

    private final List<Quizz> quizzList;
    private final CategoryAdapter.OnQuizClickListener onQuizClickListener;
    private final String categoryId;

    public QuizzAdapter(List<Quizz> quizzList, String categoryId, CategoryAdapter.OnQuizClickListener onQuizClickListener) {
        this.quizzList = quizzList;
        this.categoryId = categoryId;
        this.onQuizClickListener = onQuizClickListener;
    }

    @NonNull
    @Override
    public QuizzViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quizz, parent, false);
        return new QuizzViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizzViewHolder holder, int position) {
        Quizz quizz = quizzList.get(position);
        holder.quizzTitleTextView.setText(quizz.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (onQuizClickListener != null) {
                // Notify the listener about the quiz click
                onQuizClickListener.onQuizClick(categoryId, quizz.getTestId());
            }
        });
        holder.bind(quizz);
    }

    @Override
    public int getItemCount() {
        return quizzList.size();
    }

    static class QuizzViewHolder extends RecyclerView.ViewHolder {
        TextView quizzTitleTextView;
        private ImageView imageQuizz;
        private LinearLayout layoutQuizz; // Move layoutQuizz here

        public QuizzViewHolder(@NonNull View itemView) {
            super(itemView);
            quizzTitleTextView = itemView.findViewById(R.id.quizzTitle);
            imageQuizz = itemView.findViewById(R.id.imageQuizz);
            layoutQuizz = itemView.findViewById(R.id.layoutQuizz); // Initialize layoutQuizz here
        }

        public void bind(Quizz quizz) {
            // Load image using Glide if the context is still valid
            Context context = itemView.getContext();
            if (context instanceof Activity && !((Activity) context).isDestroyed()) {
                RequestOptions options = new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL); // Cache image to disk

                Glide.with(context)
                        .load(quizz.getImageResourceUrl())
                        .apply(options)
                        .into(imageQuizz);
            } else {
                // Log a message or handle the case where the context is not valid
                Log.e("QuestionAdapter", "Invalid context: " + context);
            }

            quizzTitleTextView.setText(quizz.getTitle());

            if (quizz.hasQuestions()) {
                layoutQuizz.setAlpha(1.0f); // Full alpha if quiz has questions
            } else {
                layoutQuizz.setAlpha(0.2f); // Lower alpha if quiz doesn't have questions
            }
        }
    }
}
