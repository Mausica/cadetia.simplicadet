package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.DashPathEffect;
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
import java.util.Random;

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
        configureNodeAppearance(holder, pathModel, position);
        configureNodePositionAndConnectors(holder, position);

        holder.nodeContainer.setOnClickListener(v -> {
            if (pathModel.isUnlocked() && listener != null) {
                listener.onPathNodeClick(position, pathModel);
            }
        });
    }

    private void configureNodeAppearance(LearningPathViewHolder holder, LearningPathModel pathModel, int position) {
        if (pathModel.isCompleted()) {
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_completed);
            holder.nodeIcon.setImageResource(R.drawable.home_ic_check);
            holder.nodeIcon.setColorFilter(Color.WHITE);
        } else if (pathModel.isUnlocked()) {
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_unlocked);
            holder.nodeIcon.setImageResource(R.drawable.ic_play);
            holder.nodeIcon.setColorFilter(Color.WHITE);
        } else {
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
        if (position == learningPathList.size() - 1) {
            holder.connectionPath.setVisibility(View.GONE);
            FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
            setNodePosition(nodeParams, position);
            holder.nodeContainer.setLayoutParams(nodeParams);
            return;
        }

        int connectorColor = learningPathList.get(position).isCompleted() ?
                ContextCompat.getColor(context, R.color.primary) :
                ContextCompat.getColor(context, R.color.primary_transparent);

        FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
        setNodePosition(nodeParams, position);
        holder.connectionContainer.post(() -> createConnectorPath(holder, position, connectorColor));
        holder.nodeContainer.setLayoutParams(nodeParams);
        holder.connectionPath.setVisibility(View.VISIBLE);
    }

    private void setNodePosition(FrameLayout.LayoutParams nodeParams, int position) {
        nodeParams.leftMargin = nodeParams.rightMargin = nodeParams.topMargin = nodeParams.bottomMargin = 0;

        switch (position % 4) {
            case 0:
                nodeParams.gravity = android.view.Gravity.START | android.view.Gravity.TOP;
                nodeParams.leftMargin = dpToPx(40);
                break;
            case 1:
                nodeParams.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
                nodeParams.rightMargin = dpToPx(40);
                break;
            case 2:
                nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
                nodeParams.leftMargin = dpToPx(60);
                break;
            case 3:
                nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
                nodeParams.rightMargin = dpToPx(60);
                break;
        }
    }

    private void createConnectorPath(LearningPathViewHolder holder, int position, int color) {
        int w = holder.connectionContainer.getWidth();
        int h = holder.connectionContainer.getHeight();
        if (w == 0 || h == 0) return;

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float[] currentPos = getActualNodePosition(position, w);
        float[] nextPos = getActualNodePosition(position + 1, w);

        float startX = currentPos[0];
        float startY = dpToPx(35);
        float endX = nextPos[0];
        float endY = h + dpToPx(5);

        // Create the main pirate map path with loops
        Path path = createPirateMapPath(startX, startY, endX, endY, position);
        drawPirateMapConnection(canvas, path, color, position);

        holder.connectionPath.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
    }

    private Path createPirateMapPath(float startX, float startY, float endX, float endY, int position) {
        Path path = new Path();
        path.moveTo(startX, startY);

        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // Create multiple segments with loops for a more mystical pirate map feel
        Random random = new Random(position); // Deterministic randomness based on position

        // First segment - initial curve
        float midY = startY + deltaY * 0.3f;
        float control1X = startX + deltaX * 0.2f + random.nextFloat() * dpToPx(30) - dpToPx(15);
        float control1Y = midY + random.nextFloat() * dpToPx(40) - dpToPx(20);

        path.quadTo(control1X, control1Y, startX + deltaX * 0.4f, midY);

        // Add mystical loops/curves in the middle section
        addMysticalLoops(path, startX + deltaX * 0.4f, midY,
                startX + deltaX * 0.6f, startY + deltaY * 0.7f, position);

        // Final segment to destination with dramatic curve
        float finalMidX = startX + deltaX * 0.8f;
        float finalMidY = startY + deltaY * 0.8f;
        float finalControlX = finalMidX + random.nextFloat() * dpToPx(40) - dpToPx(20);
        float finalControlY = finalMidY + random.nextFloat() * dpToPx(30) - dpToPx(15);

        path.quadTo(finalControlX, finalControlY, endX, endY);

        return path;
    }

    private void addMysticalLoops(Path path, float startX, float startY, float endX, float endY, int position) {
        Random random = new Random(position + 42); // Different seed for variety

        // Determine if we should add loops (every 2-3 nodes for variety)
        boolean addLoop = (position % 3 == 0) || (position % 5 == 2);

        if (addLoop) {
            // Create a decorative loop/swirl
            float loopCenterX = (startX + endX) / 2f;
            float loopCenterY = (startY + endY) / 2f;
            float loopRadius = dpToPx(15 + random.nextInt(20));

            // Direction of the loop (alternate for variety)
            int direction = (position % 2 == 0) ? -1 : 1;

            // Create spiral/loop path
            float offsetX = direction * loopRadius;
            float offsetY = random.nextFloat() * dpToPx(20) - dpToPx(10);

            // First part of loop
            path.quadTo(loopCenterX + offsetX, loopCenterY + offsetY - loopRadius,
                    loopCenterX + offsetX * 0.5f, loopCenterY + offsetY);

            // Complete the decorative swirl
            path.quadTo(loopCenterX - offsetX * 0.3f, loopCenterY + offsetY + loopRadius * 0.6f,
                    endX, endY);
        } else {
            // Simple curved connection without loops
            float midX = (startX + endX) / 2f + random.nextFloat() * dpToPx(40) - dpToPx(20);
            float midY = (startY + endY) / 2f + random.nextFloat() * dpToPx(30) - dpToPx(15);
            path.quadTo(midX, midY, endX, endY);
        }
    }

    private void drawPirateMapConnection(Canvas canvas, Path path, int color, int position) {
        boolean isCompleted = learningPathList.get(position).isCompleted();

        // Enhanced shadow for mystical effect
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.argb(60, 0, 0, 0));
        shadowPaint.setStrokeWidth(dpToPx(8));
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setAntiAlias(true);
        shadowPaint.setStrokeCap(Paint.Cap.ROUND);
        shadowPaint.setStrokeJoin(Paint.Join.ROUND);

        // Draw shadow first (behind everything)
        canvas.save();
        canvas.translate(dpToPx(3), dpToPx(3));
        canvas.drawPath(path, shadowPaint);
        canvas.restore();

        // Main path paint
        Paint pathPaint = new Paint();
        pathPaint.setColor(color);
        pathPaint.setStrokeWidth(dpToPx(5));
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setAntiAlias(true);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        if (!isCompleted) {
            // Pirate map style dashed line
            pathPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(12), dpToPx(6), dpToPx(4), dpToPx(6)}, 0));
        }

        // Draw main path
        canvas.drawPath(path, pathPaint);

        if (isCompleted) {
            // Magical glow effect for completed paths
            Paint glowPaint1 = new Paint();
            glowPaint1.setColor(Color.argb(80, Color.red(color), Color.green(color), Color.blue(color)));
            glowPaint1.setStrokeWidth(dpToPx(12));
            glowPaint1.setStyle(Paint.Style.STROKE);
            glowPaint1.setAntiAlias(true);
            glowPaint1.setStrokeCap(Paint.Cap.ROUND);
            glowPaint1.setStrokeJoin(Paint.Join.ROUND);

            Paint glowPaint2 = new Paint();
            glowPaint2.setColor(Color.argb(40, 255, 215, 0)); // Golden mystical glow
            glowPaint2.setStrokeWidth(dpToPx(16));
            glowPaint2.setStyle(Paint.Style.STROKE);
            glowPaint2.setAntiAlias(true);
            glowPaint2.setStrokeCap(Paint.Cap.ROUND);
            glowPaint2.setStrokeJoin(Paint.Join.ROUND);

            // Draw multiple glow layers for mystical effect
            canvas.drawPath(path, glowPaint2);
            canvas.drawPath(path, glowPaint1);
            canvas.drawPath(path, pathPaint);
        }
    }

    private float[] getActualNodePosition(int position, int containerWidth) {
        float[] pos = new float[2];
        float centerX = containerWidth / 2f;
        float nodeRadius = dpToPx(50);

        switch (position % 4) {
            case 0: pos[0] = dpToPx(40) + nodeRadius; break;
            case 1: pos[0] = containerWidth - dpToPx(40) - nodeRadius; break;
            case 2: pos[0] = centerX + dpToPx(60); break;
            case 3: pos[0] = centerX - dpToPx(60); break;
            default: pos[0] = centerX;
        }
        return pos;
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
            learningPathList.get(position).setCompleted(true);

            if (position + 1 < learningPathList.size()) {
                learningPathList.get(position + 1).setUnlocked(true);
                notifyItemChanged(position + 1);
            }

            notifyItemChanged(position);
            if (position > 0) notifyItemChanged(position - 1);
        }
    }

    static class LearningPathViewHolder extends RecyclerView.ViewHolder {
        FrameLayout nodeContainer, connectionContainer;
        ImageView nodeIcon, connectionPath;

        public LearningPathViewHolder(@NonNull View itemView) {
            super(itemView);
            nodeContainer = itemView.findViewById(R.id.nodeContainer);
            nodeIcon = itemView.findViewById(R.id.nodeIcon);
            connectionContainer = itemView.findViewById(R.id.connectionContainer);
            connectionPath = itemView.findViewById(R.id.connectionPath);
        }
    }
}