package com.cadetia.simplicadet.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

import eightbitlab.com.blurview.BlurView;

public class PathSelector extends AppCompatActivity {

    private SharedPreferences prefs;
    private LinearLayout pathView;
    private BlurView blurView;
    private View topBarBlurBackground;
    private ScrollView scrollView;
    private List<DbQuery.LearningPath> paths = new ArrayList<>();
    private boolean loading = true;
    private String error = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_path_selector);

        prefs = getSharedPreferences("LearningPathProgress", Context.MODE_PRIVATE);

        initializeViews();
        setupBlurView();
        setupScrollListener();
        setupBackButton();
        loadPaths();
    }

    private void initializeViews() {
        pathView = findViewById(R.id.pathView);
        blurView = findViewById(R.id.blur_view_path);
        topBarBlurBackground = findViewById(R.id.path_blur_background);
        scrollView = findViewById(R.id.path_scroll_view);
    }

    private void setupBlurView() {
        View decorView = getWindow().getDecorView();
        ViewGroup rootView = decorView.findViewById(android.R.id.content);
        Drawable windowBackground = decorView.getBackground();

        blurView.setupWith(rootView)
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(10f)
                .setBlurAutoUpdate(true);
    }

    private void setupScrollListener() {
        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float threshold = 50f;
            float alpha = Math.min(1f, scrollY / threshold);
            topBarBlurBackground.setAlpha(alpha);
        });
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.community_back);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadPaths() {

        showLoading();

        try {
            DbQuery.loadAllLearningPaths(new MyCompleteListener() {
                @Override
                public void onSucces() {
                    List<DbQuery.LearningPath> realPaths = DbQuery.g_allLearningPaths != null ?
                            DbQuery.g_allLearningPaths : new ArrayList<>();

                    paths.clear();
                    paths.addAll(realPaths);

                    loading = false;
                    error = null;

                    new Handler(Looper.getMainLooper()).post(() -> updateUI());
                }

                @Override
                public void onFailure() {
                    paths.clear();
                    loading = false;
                    error = "Failed to load";

                    new Handler(Looper.getMainLooper()).post(() -> updateUI());
                }
            });
        } catch (Exception e) {
            Log.e("PathSelector", "load error", e);
            paths.clear();
            loading = false;
            error = "Error: " + e.getMessage();

            new Handler(Looper.getMainLooper()).post(() -> updateUI());
        }
    }

    private void showLoading() {
        pathView.removeAllViews();

        // Create loading indicator
        LinearLayout loadingContainer = new LinearLayout(this);
        loadingContainer.setOrientation(LinearLayout.VERTICAL);
        loadingContainer.setGravity(android.view.Gravity.CENTER);
        loadingContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (200 * getResources().getDisplayMetrics().density)
        ));

        CircularProgressIndicator progressIndicator = new CircularProgressIndicator(this);
        progressIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        loadingContainer.addView(progressIndicator);
        pathView.addView(loadingContainer);
    }

    private void updateUI() {
        pathView.removeAllViews();

        if (loading) {
            showLoading();
        } else if (error != null) {
            showError();
        } else if (paths.isEmpty()) {
            showEmptyState();
        } else {
            showPaths();
        }
    }

    private void showError() {
        LinearLayout errorContainer = new LinearLayout(this);
        errorContainer.setOrientation(LinearLayout.VERTICAL);
        errorContainer.setGravity(android.view.Gravity.CENTER);
        errorContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (200 * getResources().getDisplayMetrics().density)
        ));

        TextView errorText = new TextView(this);
        errorText.setText("Error: " + error);
        errorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        errorText.setTextColor(getColor(android.R.color.holo_red_dark));

        // Add some margin
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, 0, 0, (int) (16 * getResources().getDisplayMetrics().density));
        errorText.setLayoutParams(textParams);

        // Retry button
        TextView retryButton = new TextView(this);
        retryButton.setText("Retry");
        retryButton.setBackgroundResource(android.R.drawable.btn_default);
        retryButton.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density)
        );
        retryButton.setOnClickListener(v -> {
            loading = true;
            error = null;
            loadPaths();
        });

        errorContainer.addView(errorText);
        errorContainer.addView(retryButton);
        pathView.addView(errorContainer);
    }

    private void showEmptyState() {
        LinearLayout emptyContainer = new LinearLayout(this);
        emptyContainer.setOrientation(LinearLayout.VERTICAL);
        emptyContainer.setGravity(android.view.Gravity.CENTER);
        emptyContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (200 * getResources().getDisplayMetrics().density)
        ));

        TextView emptyText = new TextView(this);
        emptyText.setText("No learning paths");
        emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        emptyContainer.addView(emptyText);
        pathView.addView(emptyContainer);
    }

    private void showPaths() {
        pathView.removeAllViews();
        pathView.setOrientation(LinearLayout.VERTICAL);

        for (DbQuery.LearningPath path : paths) {
            View pathCard = createLearningPathCard(path);
            pathView.addView(pathCard);

            View spacer = new View(this);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (5 * getResources().getDisplayMetrics().density)
            );
            spacer.setLayoutParams(spacerParams);
            pathView.addView(spacer);
        }
    }

    private View createLearningPathCard(DbQuery.LearningPath path) {
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout cardView = (LinearLayout) inflater.inflate(R.layout.item_learning_path_header, pathView, false);

        // Set click listener
        cardView.setOnClickListener(v -> selectPath(path));

        // Find views
        TextView titleView = cardView.findViewById(R.id.pathTitle);
        TextView lessonsView = cardView.findViewById(R.id.pathLessons);
        CircularProgressIndicator progressView = cardView.findViewById(R.id.circularProgress);
        TextView progressText = cardView.findViewById(R.id.progressText);

        // Calculate progress
        int progress = calculateProgress(path);
        int total = path.nodes != null ? path.nodes.size() : 0;

        // Set data
        titleView.setText(path.title != null ? path.title : "Unknown");
        lessonsView.setVisibility(View.VISIBLE);
        lessonsView.setText(total + " lessons");
        progressView.setProgress(progress);
        progressText.setText(progress + "%");

        return cardView;
    }

    private int calculateProgress(DbQuery.LearningPath path) {
        if (path.nodes == null || path.nodes.isEmpty()) {
            return 0;
        }

        int completedCount = 0;
        for (DbQuery.LearningPathNode node : path.nodes) {
            if (prefs.getBoolean(path.id + "_" + node.id, false)) {
                completedCount++;
            }
        }

        return (completedCount * 100) / path.nodes.size();
    }

    private void selectPath(DbQuery.LearningPath path) {
        SharedPreferences selectedPathPrefs = getSharedPreferences("SelectedLearningPath", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = selectedPathPrefs.edit();
        editor.putString("selectedPathId", path.id);
        editor.putString("selectedPathTitle", path.title);
        editor.apply();

        DbQuery.g_learningPath = path;
        finish();
    }
}