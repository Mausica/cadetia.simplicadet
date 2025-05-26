package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

        // Configure node appearance
        configureNodeAppearance(holder, pathModel, position);

        // Configure positioning and connectors
        configureNodePositionAndConnectors(holder, position);

        // Set click listener on the entire node container
        holder.nodeContainer.setOnClickListener(v -> {
            if (pathModel.isUnlocked() && listener != null) {
                listener.onPathNodeClick(position, pathModel);
            }
        });
    }

    private void configureNodeAppearance(LearningPathViewHolder holder, LearningPathModel pathModel, int position) {
        if (pathModel.isCompleted()) {
            // Completed state - larger size
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_completed);
            holder.nodeIcon.setImageResource(R.drawable.ic_check);
            holder.nodeIcon.setColorFilter(Color.WHITE);
        } else if (pathModel.isUnlocked()) {
            // Unlocked state - larger size
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_unlocked);
            holder.nodeIcon.setImageResource(R.drawable.ic_play);
            holder.nodeIcon.setColorFilter(Color.WHITE);
        } else {
            // Locked state - larger size
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_locked);
            holder.nodeIcon.setImageResource(R.drawable.ic_lock);
            holder.nodeIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary));
            holder.nodeContainer.setAlpha(0.6f);
        }

        if (pathModel.isUnlocked() || pathModel.isCompleted()) {
            holder.nodeContainer.setAlpha(1.0f);
        }
    }

    private void configureNodePositionAndConnectors(LearningPathViewHolder holder, int position) {
        // If this is the last item, don't show connectors
        if (position == learningPathList.size() - 1) {
            holder.connectionPath.setVisibility(View.GONE);
            // Position the last node
            FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
            setNodePosition(nodeParams, position);
            holder.nodeContainer.setLayoutParams(nodeParams);
            return;
        }

        // Determine connector color
        int connectorColor = learningPathList.get(position).isCompleted() ?
                ContextCompat.getColor(context, R.color.path_completed) :
                ContextCompat.getColor(context, R.color.path_locked);

        // Position nodes in zigzag pattern
        FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
        setNodePosition(nodeParams, position);

        // Create connection path with 90-degree angles
        createConnectorPath(holder, position, connectorColor);

        holder.nodeContainer.setLayoutParams(nodeParams);
        holder.connectionPath.setVisibility(View.VISIBLE);
    }

    private void setNodePosition(FrameLayout.LayoutParams nodeParams, int position) {
        // Clear existing margins
        nodeParams.leftMargin = 0;
        nodeParams.rightMargin = 0;
        nodeParams.topMargin = 0;
        nodeParams.bottomMargin = 0;

        int pattern = position % 4;

        switch (pattern) {
            case 0:
                // Center position
                nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
                break;
            case 1:
                // Right position
                nodeParams.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
                nodeParams.rightMargin = dpToPx(20);
                break;
            case 2:
                // Center position
                nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
                break;
            case 3:
                // Left position
                nodeParams.gravity = android.view.Gravity.START | android.view.Gravity.TOP;
                nodeParams.leftMargin = dpToPx(20);
                break;
        }
    }

    private void createConnectorPath(LearningPathViewHolder holder, int position, int color) {
        int containerWidth = holder.connectionContainer.getWidth();
        if (containerWidth == 0) {
            containerWidth = dpToPx(300);
        }
        int containerHeight = dpToPx(120); // Increased height for better spacing

        Bitmap bitmap = Bitmap.createBitmap(containerWidth, containerHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(dpToPx(6)); // Thicker line
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        Path path = new Path();

        // Define positions based on container dimensions
        float centerX = containerWidth / 2f;
        float leftX = dpToPx(20) + dpToPx(40); // Account for margin + half node width
        float rightX = containerWidth - dpToPx(20) - dpToPx(40); // Account for margin + half node width

        // Start and end points for connections
        float startY = dpToPx(30); // Start below current node
        float endY = containerHeight - dpToPx(30); // End above next node
        float midY = (startY + endY) / 2;

        int currentPattern = position % 4;
        int nextPattern = (position + 1) % 4;

        // Determine start and end positions
        float startX = getXPositionForPattern(currentPattern, centerX, leftX, rightX);
        float endX = getXPositionForPattern(nextPattern, centerX, leftX, rightX);

        // Create path with 90-degree angles
        path.moveTo(startX, startY);

        if (startX == endX) {
            // Straight vertical line
            path.lineTo(endX, endY);
        } else {
            // Create 90-degree path
            path.lineTo(startX, midY);
            path.lineTo(endX, midY);
            path.lineTo(endX, endY);
        }

        canvas.drawPath(path, paint);

        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        holder.connectionPath.setImageDrawable(drawable);
    }

    private float getXPositionForPattern(int pattern, float centerX, float leftX, float rightX) {
        switch (pattern) {
            case 0:
            case 2:
                return centerX;
            case 1:
                return rightX;
            case 3:
                return leftX;
            default:
                return centerX;
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

            // Update previous item to refresh connector color
            if (position > 0) {
                notifyItemChanged(position - 1);
            }
        }
    }

    static class LearningPathViewHolder extends RecyclerView.ViewHolder {
        FrameLayout nodeContainer;
        ImageView nodeIcon;
        FrameLayout connectionContainer;
        ImageView connectionPath;

        public LearningPathViewHolder(@NonNull View itemView) {
            super(itemView);
            nodeContainer = itemView.findViewById(R.id.nodeContainer);
            nodeIcon = itemView.findViewById(R.id.nodeIcon);
            connectionContainer = itemView.findViewById(R.id.connectionContainer);
            connectionPath = itemView.findViewById(R.id.connectionPath);
        }
    }
}