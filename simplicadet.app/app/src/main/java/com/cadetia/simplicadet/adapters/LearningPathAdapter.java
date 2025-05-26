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
            // Completed state
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_completed);
            holder.nodeIcon.setImageResource(R.drawable.ic_check);
            holder.nodeIcon.setColorFilter(Color.WHITE);
        } else if (pathModel.isUnlocked()) {
            // Unlocked state
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_unlocked);
            holder.nodeIcon.setImageResource(R.drawable.ic_play);
            holder.nodeIcon.setColorFilter(Color.WHITE);
        } else {
            // Locked state
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

        // Determine connector color based on current node completion status
        int connectorColor = learningPathList.get(position).isCompleted() ?
                ContextCompat.getColor(context, R.color.primary) :
                ContextCompat.getColor(context, R.color.primary_transparent);

        // Position nodes in zigzag pattern
        FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
        setNodePosition(nodeParams, position);

        // Create connection path after layout
        holder.connectionContainer.post(() -> createConnectorPath(holder, position, connectorColor));

        holder.nodeContainer.setLayoutParams(nodeParams);
        holder.connectionPath.setVisibility(View.VISIBLE);
    }

    private void setNodePosition(FrameLayout.LayoutParams nodeParams, int position) {
        // Clear existing margins
        nodeParams.leftMargin = 0;
        nodeParams.rightMargin = 0;
        nodeParams.topMargin = 0;
        nodeParams.bottomMargin = 0;

        // Zigzag pattern with 4 positions for better map-like appearance
        int pattern = position % 4;

        switch (pattern) {
            case 0:
                // Left position
                nodeParams.gravity = android.view.Gravity.START | android.view.Gravity.TOP;
                nodeParams.leftMargin = dpToPx(40);
                break;
            case 1:
                // Right position
                nodeParams.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
                nodeParams.rightMargin = dpToPx(40);
                break;
            case 2:
                // Center-right position
                nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
                nodeParams.leftMargin = dpToPx(60);
                break;
            case 3:
                // Center-left position
                nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
                nodeParams.rightMargin = dpToPx(60);
                break;
        }
    }

    private void createConnectorPath(LearningPathViewHolder holder, int position, int color) {
        int containerWidth = holder.connectionContainer.getWidth();
        int containerHeight = holder.connectionContainer.getHeight();

        if (containerWidth == 0 || containerHeight == 0) {
            return; // Container not measured yet
        }

        Bitmap bitmap = Bitmap.createBitmap(containerWidth, containerHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Get the actual positions of current and next nodes
        float[] currentPos = getActualNodePosition(position, containerWidth);
        float[] nextPos = getActualNodePosition(position + 1, containerWidth);

        // Connection container layout:
        // - Total item height: 180dp
        // - Node at top: 0-100dp
        // - Connection container: 40dp from top, so 40-180dp (140dp tall)
        // - Connection path ImageView: 120dp tall, centered in container

        // Start point: bottom of current node
        // Current node bottom is at 100dp from item top
        // Connection container starts at 40dp, so node bottom is 60dp into container
        float startX = currentPos[0];
        float startY = dpToPx(60);

        // End point: top of next node (which will be in the next RecyclerView item)
        // The path should go towards the bottom of this connection area
        // where it will connect to the top of the next node
        float endX = nextPos[0];
        float endY = containerHeight - dpToPx(20); // Near bottom of connection container

        // Create simple curved path
        Path path = createTreasureMapPath(startX, startY, endX, endY);

        // Draw the connection
        drawTreasureMapConnection(canvas, path, color, position);

        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
        holder.connectionPath.setImageDrawable(drawable);
    }

    private Path createTreasureMapPath(float startX, float startY, float endX, float endY) {
        Path path = new Path();
        path.moveTo(startX, startY);

        // Calculate control points for a smooth curved path
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // Create a gentle S-curve like treasure map paths
        // Control points are offset to create a natural curve
        float controlOffset = Math.min(distance * 0.4f, dpToPx(60));

        // First control point - curved away from start direction
        float control1X = startX + deltaX * 0.3f;
        float control1Y = startY + deltaY * 0.2f;

        // Add some perpendicular offset for the curve
        float perpX = -deltaY / distance * controlOffset * 0.5f;
        float perpY = deltaX / distance * controlOffset * 0.5f;
        control1X += perpX;
        control1Y += perpY;

        // Second control point - curved toward end direction
        float control2X = startX + deltaX * 0.7f;
        float control2Y = startY + deltaY * 0.8f;
        control2X -= perpX;
        control2Y -= perpY;

        // Create smooth cubic bezier curve
        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY);

        return path;
    }

    private void drawTreasureMapConnection(Canvas canvas, Path path, int color, int position) {
        boolean isCompleted = learningPathList.get(position).isCompleted();

        // Create dotted/dashed path like in treasure maps
        Paint pathPaint = new Paint();
        pathPaint.setColor(color);
        pathPaint.setStrokeWidth(dpToPx(4));
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setAntiAlias(true);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        // Create dashed effect like treasure map dotted lines
        if (isCompleted) {
            // Solid line for completed paths
            pathPaint.setPathEffect(null);
        } else {
            // Dotted line for incomplete paths
            DashPathEffect dashEffect = new DashPathEffect(new float[]{dpToPx(8), dpToPx(6)}, 0);
            pathPaint.setPathEffect(dashEffect);
        }

        // Draw subtle shadow for depth
        Paint shadowPaint = new Paint(pathPaint);
        shadowPaint.setColor(Color.argb(30, 0, 0, 0));
        shadowPaint.setStrokeWidth(dpToPx(6));

        canvas.save();
        canvas.translate(dpToPx(1), dpToPx(1));
        canvas.drawPath(path, shadowPaint);
        canvas.restore();

        // Draw main path
        canvas.drawPath(path, pathPaint);

        // Add glow effect for completed paths
        if (isCompleted) {
            Paint glowPaint = new Paint();
            glowPaint.setColor(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
            glowPaint.setStrokeWidth(dpToPx(8));
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setAntiAlias(true);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);
            glowPaint.setStrokeJoin(Paint.Join.ROUND);

            canvas.drawPath(path, glowPaint);
            canvas.drawPath(path, pathPaint); // Redraw main path on top
        }
    }

    private float[] getActualNodePosition(int position, int containerWidth) {
        float[] pos = new float[2];
        int pattern = position % 4;

        float centerX = containerWidth / 2f;
        float nodeRadius = dpToPx(50); // Half of node width (100dp / 2)

        switch (pattern) {
            case 0:
                // Left position - center of the node
                pos[0] = dpToPx(40) + nodeRadius;
                break;
            case 1:
                // Right position - center of the node
                pos[0] = containerWidth - dpToPx(40) - nodeRadius;
                break;
            case 2:
                // Center-right position - center of the node
                pos[0] = centerX + dpToPx(60);
                break;
            case 3:
                // Center-left position - center of the node
                pos[0] = centerX - dpToPx(60);
                break;
            default:
                pos[0] = centerX;
        }

        pos[1] = 0; // Y position handled in path creation
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