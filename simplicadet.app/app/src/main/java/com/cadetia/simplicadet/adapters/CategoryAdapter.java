package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.CategoryModel;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private static final String TAG = "CategoryAdapter";
    private final List<CategoryModel> catList;
    private final LayoutInflater inflater;
    private final Context context;
    private final OnQuizClickListener onQuizClickListener;

    public CategoryAdapter(List<CategoryModel> catList, Context context, OnQuizClickListener onQuizClickListener) {
        this.catList = new ArrayList<>(catList); // Create a copy to avoid reference issues
        this.inflater = LayoutInflater.from(context);
        this.context = context;
        this.onQuizClickListener = onQuizClickListener;
        Log.d(TAG, "CategoryAdapter created with " + this.catList.size() + " categories");
    }

    public interface OnQuizClickListener {
        void onQuizClick(String categoryId, String testId);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_row, parent, false);
        return new CategoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (position >= catList.size()) {
            Log.w(TAG, "Position " + position + " out of bounds for list size " + catList.size());
            return;
        }

        CategoryModel category = catList.get(position);
        if (category == null) {
            Log.w(TAG, "Category at position " + position + " is null");
            return;
        }

        Log.d(TAG, "Binding category: " + category.getName() + " at position " + position);
        holder.catNameTextView.setText(category.getName());

        // Set up the quiz RecyclerView
        if (holder.quizzRecyclerView.getLayoutManager() == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            holder.quizzRecyclerView.setLayoutManager(layoutManager);
        }

        // Create new QuizzAdapter with current data
        QuizzAdapter quizzAdapter = new QuizzAdapter(
                category.getQuizzList() != null ? category.getQuizzList() : new ArrayList<>(),
                category.getDocID(),
                onQuizClickListener
        );
        holder.quizzRecyclerView.setAdapter(quizzAdapter);

        Log.d(TAG, "Set adapter for category " + category.getName() + " with " +
                (category.getQuizzList() != null ? category.getQuizzList().size() : 0) + " quizzes");
    }

    @Override
    public int getItemCount() {
        int count = catList.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public void updateCategories(List<CategoryModel> newCategories) {
        Log.d(TAG, "updateCategories called with " + (newCategories != null ? newCategories.size() : 0) + " categories");

        if (newCategories == null) {
            Log.w(TAG, "newCategories is null, clearing list");
            this.catList.clear();
        } else {
            this.catList.clear();
            this.catList.addAll(newCategories);

            // Log the categories being added
            for (int i = 0; i < newCategories.size(); i++) {
                CategoryModel cat = newCategories.get(i);
                Log.d(TAG, "Category " + i + ": " + (cat != null ? cat.getName() : "null") +
                        " with " + (cat != null && cat.getQuizzList() != null ? cat.getQuizzList().size() : 0) + " quizzes");
            }
        }

        Log.d(TAG, "Notifying data set changed, new size: " + catList.size());
        notifyDataSetChanged();
    }

    public List<CategoryModel> getCategories() {
        return new ArrayList<>(catList); // Return a copy to prevent external modification
    }

    public void clearCategories() {
        Log.d(TAG, "clearCategories called");
        this.catList.clear();
        notifyDataSetChanged();
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