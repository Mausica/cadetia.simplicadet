package com.cadetia.simplicadet.ui.home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.cadetia.simplicadet.R;
import com.otaliastudios.zoom.ZoomLayout;

public class HomeFragment3 extends Fragment {

    private static final int PLATOON_COUNT = 6;
    private static final int SOLDIERS_PER_PLATOON = 27;
    private static final int TOTAL_SOLDIERS = PLATOON_COUNT * SOLDIERS_PER_PLATOON;

    private static final String[] SOLDIERS = {
            "Radulescu Marius", "Grama Bianca", "Popescu Ion", "Stanescu Maria",
            "Ionescu Andrei", "Constantinescu Elena", "Popa Florin", "Diaconescu Raluca",
            "Marinescu Cristian", "Negulescu Ana"
    };

    private static final int STATE_PRESENT = 0;
    private static final int STATE_HOME = 1;
    private static final int STATE_ABSENT = 2;

    private ZoomLayout zoomLayout;
    private TextView tvTotalCount;
    private final int[] presentCount = new int[PLATOON_COUNT];
    private final int[] homeCount = new int[PLATOON_COUNT];
    private final int[] absentCount = new int[PLATOON_COUNT];

    public HomeFragment3() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        zoomLayout = view.findViewById(R.id.zoomLayout);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvTotalCount.setText("Total: " + TOTAL_SOLDIERS);

        LinearLayout parentLayout = new LinearLayout(requireContext());
        parentLayout.setOrientation(LinearLayout.HORIZONTAL);

        ViewGroup mindMapContainer = view.findViewById(R.id.mindMapContainer);
        mindMapContainer.addView(parentLayout);

        int soldierIndex = 0;

        for (int i = 0; i < PLATOON_COUNT; i++) {
            presentCount[i] = SOLDIERS_PER_PLATOON;
            homeCount[i] = 0;
            absentCount[i] = 0;

            LinearLayout platoonContainer = new LinearLayout(requireContext());
            platoonContainer.setOrientation(LinearLayout.VERTICAL);
            platoonContainer.setPadding(20, 20, 20, 20);

            TextView platoonStats = new TextView(requireContext());
            updatePlatoonStats(i, platoonStats);

            GridLayout gridLayout = new GridLayout(requireContext());
            gridLayout.setColumnCount(3);
            gridLayout.setRowCount(9);

            for (int j = 0; j < SOLDIERS_PER_PLATOON; j++) {
                View soldierView = getLayoutInflater().inflate(R.layout.item_formation, gridLayout, false);
                soldierView.getLayoutParams().width = 100;
                soldierView.getLayoutParams().height = 100;

                TextView tvInitials = soldierView.findViewById(R.id.tv_soldier_initials);
                String fullName = SOLDIERS[soldierIndex % SOLDIERS.length];
                String[] nameParts = fullName.split(" ");
                String initials = nameParts[0].substring(0, 1) + nameParts[1].substring(0, 1);
                tvInitials.setText(initials);
                soldierIndex++;

                soldierView.setTag(R.id.soldier_state_key, STATE_PRESENT);
                soldierView.setTag(R.id.platoon_index_key, i);
                soldierView.setOnClickListener(v -> cycleState(v, platoonStats));
                gridLayout.addView(soldierView);
            }

            platoonContainer.addView(platoonStats);
            platoonContainer.addView(gridLayout);
            parentLayout.addView(platoonContainer);
        }
    }

    private void cycleState(View v, TextView platoonStats) {
        int platoonIndex = (int) v.getTag(R.id.platoon_index_key);
        int currentState = (int) v.getTag(R.id.soldier_state_key);
        int newState = (currentState + 1) % 3;
        v.setTag(R.id.soldier_state_key, newState);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, requireContext().getResources().getDisplayMetrics()));

        int backgroundColor;

        switch (newState) {
            case STATE_PRESENT:
                backgroundColor = getThemeColor(R.attr.backgroundLight);
                presentCount[platoonIndex]++;
                absentCount[platoonIndex]--;
                break;

            case STATE_HOME:
                backgroundColor = getThemeColor(R.attr.backgroundDark);
                presentCount[platoonIndex]--;
                homeCount[platoonIndex]++;
                break;

            case STATE_ABSENT:
                backgroundColor = Color.parseColor("#D9FA5A50");
                homeCount[platoonIndex]--;
                absentCount[platoonIndex]++;
                break;

            default:
                return;
        }

        drawable.setColor(backgroundColor);
        v.setBackground(drawable);
        updatePlatoonStats(platoonIndex, platoonStats);
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }


    private void updatePlatoonStats(int platoonIndex, TextView platoonStats) {
        platoonStats.setText("P: " + presentCount[platoonIndex] + " H: " + homeCount[platoonIndex] + " A: " + absentCount[platoonIndex]);
    }
}