package com.cadetia.simplicadet.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.otaliastudios.zoom.ZoomLayout;

public class HomeFragment3 extends Fragment {

    private FrameLayout mindMapContainer;
    private ZoomLayout zoomLayout;

    public HomeFragment3() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mindMapContainer = view.findViewById(R.id.mindMapContainer);
        zoomLayout = view.findViewById(R.id.zoomLayout);

        LinearLayout parentLayout = new LinearLayout(requireContext());
        parentLayout.setOrientation(LinearLayout.HORIZONTAL);

        for (int j = 0; j < 6; j++) {
            LinearLayout gridContainer = new LinearLayout(requireContext());
            gridContainer.setOrientation(LinearLayout.VERTICAL);
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

                // Make the button turn red when clicked
                button.setOnClickListener(v -> button.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                ));

                gridLayout.addView(button);
            }

            LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            gridParams.setMargins(50, 50, 50, 50);
            gridLayout.setLayoutParams(gridParams);
            gridContainer.addView(gridLayout);
            parentLayout.addView(gridContainer);
        }

        mindMapContainer.addView(parentLayout);
    }
}
