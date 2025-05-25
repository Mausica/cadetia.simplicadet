package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.model.LearningPathModel;
import java.util.List;

public class LearningPathAdapter extends RecyclerView.Adapter<LearningPathAdapter.LearningPathViewHolder> {

    private List<LearningPathModel> learningPathList;
    private Context context;
    private OnLearningPathClickListener listener;

    public interface OnLearningPathClickListener {
        void onPathNodeClick(int position, LearningPathModel pathModel);
    }

    public LearningPathAdapter(List<LearningPathModel> learningPathList, Context context, OnLearningPathClickListener listener) {
        this.learningPathList = learningPathList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LearningPathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_learning_path_node, parent, false);
        return new LearningPathViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LearningPathViewHolder holder, int position) {
        LearningPathModel pathModel = learningPathList.get(position);

        // Set title
        holder.titleText.setText(pathModel.getTitle());

        // Configure node appearance based on state
        configureNodeAppearance(holder, pathModel, position);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (pathModel.isUnlocked() && listener != null) {
                listener.onPathNodeClick(position, pathModel);
            }
        });

        // Show/hide connection line
        if (position == learningPathList.size() - 1) {
            holder.connectionLine.setVisibility(View.GONE);
        } else {
            holder.connectionLine.setVisibility(View.VISIBLE);

            // Color the connection line based on completion status
            LearningPathModel nextNode = learningPathList.get(position + 1);
            if (pathModel.isCompleted()) {
                holder.connectionLine.setBackgroundColor(ContextCompat.getColor(context, R.color.path_completed));
            } else {
                holder.connectionLine.setBackgroundColor(ContextCompat.getColor(context, R.color.path_locked));
            }
        }
    }

    private void configureNodeAppearance(LearningPathViewHolder holder, LearningPathModel pathModel, int position) {
        GradientDrawable nodeBackground = new GradientDrawable();
        nodeBackground.setShape(GradientDrawable.RECTANGLE);
        nodeBackground.setCornerRadius(dpToPx(16));

        if (pathModel.isCompleted()) {
            // Completed state - green with checkmark
            nodeBackground.setColor(ContextCompat.getColor(context, R.color.path_completed));
            holder.nodeIcon.setImageResource(R.drawable.ic_check);
            holder.nodeIcon.setColorFilter(Color.WHITE);
            holder.titleText.setTextColor(Color.WHITE);

        } else if (pathModel.isUnlocked()) {
            // Unlocked state - purple with play icon
            nodeBackground.setColor(ContextCompat.getColor(context, R.color.path_unlocked));
            holder.nodeIcon.setImageResource(R.drawable.ic_play);
            holder.nodeIcon.setColorFilter(Color.WHITE);
            holder.titleText.setTextColor(Color.WHITE);

        } else {
            // Locked state - gray with lock icon
            nodeBackground.setColor(ContextCompat.getColor(context, R.color.path_locked));
            holder.nodeIcon.setImageResource(R.drawable.ic_lock);
            holder.nodeIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            holder.titleText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        holder.nodeContainer.setBackground(nodeBackground);

        // Add subtle elevation for unlocked/completed nodes
        if (pathModel.isUnlocked() || pathModel.isCompleted()) {
            holder.nodeContainer.setElevation(dpToPx(4));
        } else {
            holder.nodeContainer.setElevation(0);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return learningPathList.size();
    }

    public void updateNodeCompletion(int position) {
        if (position >= 0 && position < learningPathList.size()) {
            LearningPathModel currentNode = learningPathList.get(position);
            currentNode.setCompleted(true);

            // Unlock next node if it exists
            if (position + 1 < learningPathList.size()) {
                LearningPathModel nextNode = learningPathList.get(position + 1);
                nextNode.setUnlocked(true);
                notifyItemChanged(position + 1);
            }

            notifyItemChanged(position);
        }
    }

    static class LearningPathViewHolder extends RecyclerView.ViewHolder {
        View nodeContainer;
        ImageView nodeIcon;
        TextView titleText;
        View connectionLine;

        public LearningPathViewHolder(@NonNull View itemView) {
            super(itemView);
            nodeContainer = itemView.findViewById(R.id.nodeContainer);
            nodeIcon = itemView.findViewById(R.id.nodeIcon);
            titleText = itemView.findViewById(R.id.titleText);
            connectionLine = itemView.findViewById(R.id.connectionLine);
        }
    }
}