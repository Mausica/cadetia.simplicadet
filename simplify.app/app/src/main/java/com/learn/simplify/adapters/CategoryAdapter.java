package com.learn.simplify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.learn.simplify.R;
import com.learn.simplify.model.CategoryModel;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<CategoryModel> catList;
    private final LayoutInflater inflater;
    private final Context context;
    private final OnQuizClickListener onQuizClickListener;

    public CategoryAdapter(List<CategoryModel> catList, Context context, OnQuizClickListener onQuizClickListener) {
        this.catList = catList;
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.onQuizClickListener = onQuizClickListener;
    }

    public interface OnQuizClickListener {
        void onQuizClick(String categoryId, String testId);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.cat_item_layout, parent, false);
        return new CategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryModel category = catList.get(position);
        holder.catNameTextView.setText(category.getName());

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        holder.quizzRecyclerView.setLayoutManager(layoutManager);

        QuizzAdapter quizzAdapter = new QuizzAdapter(category.getQuizzList(), category.getDocID(), onQuizClickListener);
        holder.quizzRecyclerView.setAdapter(quizzAdapter);
    }

    @Override
    public int getItemCount() {
        return catList.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView catNameTextView;
        RecyclerView quizzRecyclerView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            catNameTextView = itemView.findViewById(R.id.catName);
            quizzRecyclerView = itemView.findViewById(R.id.quizzRecyclerView);
        }
    }
}
