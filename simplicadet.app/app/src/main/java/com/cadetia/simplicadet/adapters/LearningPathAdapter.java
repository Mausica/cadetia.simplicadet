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

        Path path = createTreasureMapPath(startX, startY, endX, endY);
        drawTreasureMapConnection(canvas, path, color, position);

        holder.connectionPath.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
    }

    private Path createTreasureMapPath(float startX, float startY, float endX, float endY) {
        Path path = new Path();
        path.moveTo(startX, startY);

        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        float controlOffset = Math.min(distance * 0.5f, dpToPx(70));

        float control1X = startX + deltaX * 0.3f;
        float control1Y = startY + deltaY * 0.2f;
        float perpX = -deltaY / distance * controlOffset * 0.6f;
        float perpY = deltaX / distance * controlOffset * 0.6f;
        control1X += perpX;
        control1Y += perpY;

        float control2X = startX + deltaX * 0.7f;
        float control2Y = startY + deltaY * 0.8f;
        control2X -= perpX;
        control2Y -= perpY;

        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY);
        return path;
    }

    private void drawTreasureMapConnection(Canvas canvas, Path path, int color, int position) {
        boolean isCompleted = learningPathList.get(position).isCompleted();

        Paint pathPaint = new Paint();
        pathPaint.setColor(color);
        pathPaint.setStrokeWidth(dpToPx(4));
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setAntiAlias(true);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        if (!isCompleted) {
            pathPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(10), dpToPx(8)}, 0));
        }

        Paint shadowPaint = new Paint(pathPaint);
        shadowPaint.setColor(Color.argb(35, 0, 0, 0));
        shadowPaint.setStrokeWidth(dpToPx(6));

        canvas.save();
        canvas.translate(dpToPx(2), dpToPx(2));
        canvas.drawPath(path, shadowPaint);
        canvas.restore();

        canvas.drawPath(path, pathPaint);

        if (isCompleted) {
            Paint glowPaint = new Paint();
            glowPaint.setColor(Color.argb(45, Color.red(color), Color.green(color), Color.blue(color)));
            glowPaint.setStrokeWidth(dpToPx(9));
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setAntiAlias(true);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);
            glowPaint.setStrokeJoin(Paint.Join.ROUND);

            canvas.drawPath(path, glowPaint);
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