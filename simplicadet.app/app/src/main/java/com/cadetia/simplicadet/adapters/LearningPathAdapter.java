package com.cadetia.simplicadet.adapters;

import android.content.Context;
import android.content.res.TypedArray;
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
            holder.nodeIcon.setColorFilter(getThemeColor(R.attr.textNormal));
            holder.nodeIcon.setAlpha(0.8f);
        } else {
            holder.nodeContainer.setBackgroundResource(R.drawable.node_background_locked);
            holder.nodeIcon.setImageResource(R.drawable.ic_lock);
            holder.nodeIcon.setColorFilter(getThemeColor(R.attr.textDark));
            holder.nodeContainer.setAlpha(0.6f);
        }
        if (pathModel.isUnlocked() || pathModel.isCompleted()) holder.nodeContainer.setAlpha(1.0f);
    }

    private void configureNodePositionAndConnectors(LearningPathViewHolder holder, int position) {
        if (position == learningPathList.size() - 1) {
            holder.connectionPath.setVisibility(View.GONE);
            FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
            setNodePosition(nodeParams, position);
            holder.nodeContainer.setLayoutParams(nodeParams);
            return;
        }
        int connectorColor = learningPathList.get(position).isCompleted() ? ContextCompat.getColor(context, R.color.primary) : getThemeColor(R.attr.textDark);
        FrameLayout.LayoutParams nodeParams = (FrameLayout.LayoutParams) holder.nodeContainer.getLayoutParams();
        setNodePosition(nodeParams, position);

        holder.connectionContainer.post(() -> createConnectorPath(holder, position, connectorColor));
        holder.nodeContainer.setLayoutParams(nodeParams);
        holder.connectionPath.setVisibility(View.VISIBLE);
    }

    private void setNodePosition(FrameLayout.LayoutParams nodeParams, int position) {
        nodeParams.leftMargin = nodeParams.rightMargin = nodeParams.topMargin = nodeParams.bottomMargin = 0;
        switch (position % 4) {
            case 0: nodeParams.gravity = android.view.Gravity.START | android.view.Gravity.TOP; nodeParams.leftMargin = dpToPx(40); break;
            case 1: nodeParams.gravity = android.view.Gravity.END | android.view.Gravity.TOP; nodeParams.rightMargin = dpToPx(40); break;
            case 2: nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP; nodeParams.leftMargin = dpToPx(60); break;
            case 3: nodeParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP; nodeParams.rightMargin = dpToPx(60); break;
        }
    }

    private void createConnectorPath(LearningPathViewHolder holder, int position, int color) {
        int w = holder.connectionContainer.getWidth();
        int h = holder.connectionContainer.getHeight();
        if (w == 0 || h == 0) return;

        Bitmap bitmap = Bitmap.createBitmap(w, h + dpToPx(10), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float[] currentPos = getActualNodePosition(position, w);
        float[] nextPos = getActualNodePosition(position + 1, w);

        float startY = dpToPx(40);
        float endY = h + dpToPx(5);
        float startX = currentPos[0];
        float endX = nextPos[0];

        Path path = createSmoothCurvePath(startX, startY, endX, endY, position, w);
        drawPirateMapConnection(canvas, path, color, position);
        holder.connectionPath.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
    }

    private Path createSmoothCurvePath(float startX, float startY, float endX, float endY, int position, int containerWidth) {
        Path path = new Path();
        path.moveTo(startX, startY);

        float deltaX = endX - startX;
        float deltaY = endY - startY;

        boolean curveRight = (position % 4 == 0) || (position % 4 == 3);

        float baseOffset = dpToPx(40);
        float yControlOffset = deltaY * 0.45f;

        float controlX1, controlY1, controlX2, controlY2;

        if (curveRight) {
            controlX1 = startX + baseOffset;
            controlX2 = endX - baseOffset;
        } else {
            controlX1 = startX - baseOffset;
            controlX2 = endX + baseOffset;
        }

        controlY1 = startY + yControlOffset;
        controlY2 = endY - yControlOffset;

        float margin = dpToPx(10);
        controlX1 = Math.max(margin, Math.min(containerWidth - margin, controlX1));
        controlX2 = Math.max(margin, Math.min(containerWidth - margin, controlX2));

        path.cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY);

        return path;
    }


    private void drawPirateMapConnection(Canvas canvas, Path path, int color, int position) {
        Paint pathPaint = new Paint();
        pathPaint.setColor(color);
        pathPaint.setStrokeWidth(dpToPx(5));
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setAntiAlias(true);

        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);

        if (!learningPathList.get(position).isCompleted()) {
            pathPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(12), dpToPx(6)}, 0));
        }
        canvas.drawPath(path, pathPaint);
    }


    private float[] getActualNodePosition(int position, int containerWidth) {
        float[] pos = new float[2];
        float centerX = containerWidth / 2f;
        float nodeRadius = dpToPx(50);
        switch (position % 4) {
            case 0: pos[0] = dpToPx(40) + nodeRadius; break;
            case 1: pos[0] = containerWidth - dpToPx(40) - nodeRadius; break;
            case 2: pos[0] = centerX + dpToPx(15); break;
            case 3: pos[0] = centerX - dpToPx(15); break;
            default: pos[0] = centerX;
        }
        pos[1] = 0;
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

    private int getThemeColor(int attr) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr});
        int color = typedArray.getColor(0, Color.BLACK);
        typedArray.recycle();
        return color;
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