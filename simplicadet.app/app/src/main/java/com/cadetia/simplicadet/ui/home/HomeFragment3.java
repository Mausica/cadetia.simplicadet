package com.cadetia.simplicadet.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.cadetia.simplicadet.R;

public class HomeFragment3 extends Fragment {

    private FrameLayout mindMapContainer;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f, lastScaleFactor = 1.0f, dX, dY;
    private boolean isDragging = false;

    public HomeFragment3() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home3, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mindMapContainer = view.findViewById(R.id.mindMapContainer);
        FrameLayout movableLayout = new FrameLayout(requireContext());
        movableLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout parentLayout = new LinearLayout(requireContext());
        parentLayout.setOrientation(LinearLayout.HORIZONTAL);
        parentLayout.setWeightSum(2);

        for (int j = 0; j < 6; j++) {
            LinearLayout gridContainer = new LinearLayout(requireContext());
            gridContainer.setOrientation(LinearLayout.VERTICAL);
            gridContainer.setWeightSum(1);
            GridLayout gridLayout = new GridLayout(requireContext());
            gridLayout.setColumnCount(3);
            gridLayout.setRowCount(9);

            for (int i = 1; i <= 27; i++) {
                Button button = new Button(requireContext());
                button.setText(String.valueOf(i + (j * 27)));
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 150;
                params.height = 150;
                button.setLayoutParams(params);
                button.setOnClickListener(v -> button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)));
                gridLayout.addView(button);
            }

            LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            gridParams.setMargins(30, 30, 30, 30);
            gridLayout.setLayoutParams(gridParams);
            gridContainer.addView(gridLayout);
            parentLayout.addView(gridContainer);
        }

        movableLayout.addView(parentLayout);
        mindMapContainer.addView(movableLayout);

        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float scaleDelta = detector.getScaleFactor();
                if (Math.abs(scaleDelta - lastScaleFactor) > 0.01f) {
                    scaleFactor *= scaleDelta;

                    // Ajustăm zoom-ul pe baza numărului de griduri și a dimensiunii ecranului
                    scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3f));

                    movableLayout.post(() -> {
                        movableLayout.setPivotX(detector.getFocusX());
                        movableLayout.setPivotY(detector.getFocusY());
                        movableLayout.setScaleX(scaleFactor);
                        movableLayout.setScaleY(scaleFactor);
                    });

                    lastScaleFactor = scaleDelta;
                }
                return true;
            }
        });

        movableLayout.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            if (event.getPointerCount() == 1) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDragging = true;
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isDragging) {
                            v.setX(event.getRawX() + dX);
                            v.setY(event.getRawY() + dY);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        break;
                }
            }
            v.performClick();
            return true;
        });
    }
}
