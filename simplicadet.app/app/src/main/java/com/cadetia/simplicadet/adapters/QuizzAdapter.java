package com.cadetia.simplicadet.adapters;

import android.app.Activity;
import android.content.Context;
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
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.Quizz;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

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
        TextView createdName;
        private ImageView imageQuizz;
        private ImageView createdProfile;
        private LinearLayout layoutQuizz;

        public QuizzViewHolder(@NonNull View itemView) {
            super(itemView);
            quizzTitleTextView = itemView.findViewById(R.id.quizzTitle);
            imageQuizz = itemView.findViewById(R.id.imageQuizz);
            layoutQuizz = itemView.findViewById(R.id.layoutQuizz);
            createdName = itemView.findViewById(R.id.created_name);
            createdProfile = itemView.findViewById(R.id.created_profile);
        }

        public void bind(Quizz quizz) {
            Context context = itemView.getContext();
            if (context instanceof Activity && !((Activity) context).isDestroyed()) {
                RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL);
                Glide.with(context).load(quizz.getImageResourceUrl()).apply(options).into(imageQuizz);
            } else {
                Log.e("QuizzAdapter", "Invalid context: " + context);
            }

            quizzTitleTextView.setText(quizz.getTitle());
            layoutQuizz.setAlpha(quizz.hasQuestions() ? 1.0f : 0.2f);

            if (quizz.getCreatedBy() != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("USERS").document(quizz.getCreatedBy())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String userName = documentSnapshot.getString("NAME");
                                String profileImage = documentSnapshot.getString("PHOTO");
                                createdName.setText(userName != null ? userName : "Unknown User");
                                if (profileImage != null && !profileImage.isEmpty()) {
                                    Glide.with(context).load(profileImage).into(createdProfile);
                                } else {
                                    Glide.with(context).load(R.raw.guest).into(createdProfile);
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.e("QuizzAdapter", "Failed to fetch user data", e));
            }
        }
    }
}
