package com.cadetia.simplicadet.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.FlashcardsActivity;
import com.cadetia.simplicadet.activities.Home;
import com.cadetia.simplicadet.activities.QuestionsActivity;
import com.cadetia.simplicadet.activities.ShowRedeem;
import com.cadetia.simplicadet.adapters.CategoryAdapter;
import com.cadetia.simplicadet.adapters.JournalAdapter;
import com.cadetia.simplicadet.adapters.LearningPathAdapter;
import com.cadetia.simplicadet.adapters.MainTaskAdapter;
import com.cadetia.simplicadet.database.DatabaseClient;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.Task;
import com.cadetia.simplicadet.utils.NetworkUtils;

import com.cadetia.simplicadet.adapters.LearningPathAdapter;
import com.cadetia.simplicadet.model.LearningPathModel;
import com.cadetia.simplicadet.utils.LearningPathHelper;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment1 extends Fragment implements LearningPathAdapter.OnLearningPathClickListener, JournalAdapter.OnJournalClickListener {
    private static final String TAG = "HomeFragment1";
    private RecyclerView categoryRecyclerView;
    private RecyclerView mainTasksRecycler;
    private MainTaskAdapter mainTaskAdapter;
    private List<Task> tasks = new ArrayList<>();
    private Handler handler = new Handler();
    private boolean isLoadingDismissed = false;
    private String userEmail;
    private RecyclerView journalRecyclerView;
    private JournalAdapter journalAdapter;
    private List<JournalEntry> journalList = new ArrayList<>();
    private LruCache<String, Bitmap> memCache;
    private LearningPathAdapter learningPathAdapter;
    private List<LearningPathModel> learningPathList = new ArrayList<>();
    private static final int DEFAULT_PATH_NODES = 13;

    // Learning Path Header Views
    private View learningPathHeader;
    private TextView pathTitle;
    private TextView progressText;
    private CircularProgressIndicator circularProgress;

    private View loadingLayout;
    private View contentView;

    public HomeFragment1() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home1, container, false);

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout1);

        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        mainTasksRecycler = view.findViewById(R.id.tasksMainRecyclerView);
        journalRecyclerView = view.findViewById(R.id.journalRecyclerView);

        // Initialize learning path header
        initializeLearningPathHeader(view);

        showLoading(true);

        return view;
    }

    private void initializeLearningPathHeader(View parentView) {
        // Find the header view in your fragment layout
        learningPathHeader = parentView.findViewById(R.id.learningPathHeader);

        if (learningPathHeader != null) {
            pathTitle = learningPathHeader.findViewById(R.id.pathTitle);
            progressText = learningPathHeader.findViewById(R.id.progressText);
            circularProgress = learningPathHeader.findViewById(R.id.circularProgress);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        // Initialize the cache
        memCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        showLoading(true);
        new Handler().postDelayed(() -> {
            if (isAdded()) {
                loadTaskFirstLayout();
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    loadJournals();
                    loadLearningPath();
                } else {
                    journalsLoaded = true;
                    categoriesLoaded = true;
                    checkAllDataLoaded();
                }
            } else {
                Log.w(TAG, "Fragment not attached in postDelayed");
            }
        }, 1000);
    }

    private void loadLearningPath() {
        String categoryTitle = "Grafuri";
        int nodeCount = DEFAULT_PATH_NODES;

        learningPathList = LearningPathHelper.generateLearningPath(requireContext(), nodeCount, categoryTitle);
        setUpLearningPathRecyclerView();
        updateLearningPathHeader();

        categoriesLoaded = true;
        checkAllDataLoaded();
    }

    private void updateLearningPathHeader() {
        if (learningPathHeader == null || learningPathList.isEmpty()) {
            return;
        }

        // Calculate progress
        int completedNodes = 0;
        for (LearningPathModel node : learningPathList) {
            if (node.isCompleted()) {
                completedNodes++;
            }
        }

        int totalNodes = learningPathList.size();
        int progressPercentage = totalNodes > 0 ? (completedNodes * 100) / totalNodes : 0;

        // Update header views
        if (pathTitle != null) {
            pathTitle.setText("Grafuri");
        }

        if (progressText != null) {
            progressText.setText(progressPercentage + "%");
        }

        if (circularProgress != null) {
            circularProgress.setProgress(progressPercentage);
        }

        // Show/hide header based on whether we have learning path data
        learningPathHeader.setVisibility(totalNodes > 0 ? View.VISIBLE : View.GONE);
    }

    private void setUpLearningPathRecyclerView() {
        if (isAdded()) {
            learningPathAdapter = new LearningPathAdapter(learningPathList, requireContext(), this);

            // Use LinearLayoutManager for vertical scrolling
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
            categoryRecyclerView.setLayoutManager(layoutManager);
            categoryRecyclerView.setAdapter(learningPathAdapter);

            // Optional: Add some padding to the RecyclerView for better visual appearance
            int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
            categoryRecyclerView.setPadding(padding, 0, padding, padding); // Remove top padding since header is now separate
            categoryRecyclerView.setClipToPadding(false);

        } else {
            Log.e(TAG, "Fragment is not attached, cannot set up learning path recycler view");
        }
    }

    private void showLoading(boolean show) {
        if (loadingLayout != null && contentView != null) {
            if (show) {
                isLoadingDismissed = false;
                loadingLayout.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
            } else if (!isLoadingDismissed) {
                isLoadingDismissed = true;

                Context context = getContext();
                if (context != null) {
                    loadingLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
                    new Handler().postDelayed(() -> {
                        if (loadingLayout != null && isAdded()) {
                            loadingLayout.setVisibility(View.GONE);
                            contentView.setVisibility(View.VISIBLE);
                            contentView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
                        }
                    }, 250);
                } else {
                    Log.w(TAG, "Context is null, skipping animations");
                }
            }
        }
    }

    private void loadTaskFirstLayout() {
        if (isAdded()) {
            setUpAdapter();
            getSavedTasks();
        } else {
            Log.w(TAG, "Fragment not attached, skipping task layout load");
        }
    }

    private void loadJournals() {
        DbQuery.loadJournals(new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSucces() {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not attached, skipping journal load.");
                    return;
                }

                if (journalAdapter == null) {
                    journalAdapter = new JournalAdapter(journalList, HomeFragment1.this, memCache);
                }

                journalRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                journalRecyclerView.setAdapter(journalAdapter);

                journalList.clear();
                journalList.addAll(DbQuery.g_homeJournalList);
                journalAdapter.notifyDataSetChanged();

                journalsLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load journals");
                journalsLoaded = true;
                checkAllDataLoaded();
            }
        });
    }

    private void getSavedTasks() {
        @SuppressLint("StaticFieldLeak")
        class GetSavedTasks extends AsyncTask<Void, Void, List<Task>> {
            @Override
            protected List<Task> doInBackground(Void... voids) {
                return DatabaseClient
                        .getInstance(requireContext())
                        .getAppDatabase()
                        .dataBaseAction()
                        .getAllTasksList();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Task> fetchedTasks) {
                super.onPostExecute(fetchedTasks);
                tasks.clear();
                tasks.addAll(fetchedTasks);
                if (mainTaskAdapter == null) {
                    mainTaskAdapter = new MainTaskAdapter(tasks);
                    mainTasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
                    mainTasksRecycler.setAdapter(mainTaskAdapter);
                } else {
                    mainTaskAdapter.notifyDataSetChanged();
                }
                tasksLoaded = true;
                checkAllDataLoaded();
            }
        }

        GetSavedTasks savedTasks = new GetSavedTasks();
        savedTasks.execute();
    }

    private void setUpAdapter() {
        mainTaskAdapter = new MainTaskAdapter(tasks);
        mainTasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mainTasksRecycler.setAdapter(mainTaskAdapter);
    }

    @Override
    public void onJournalClick(String journalLink) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if (journalLink != null && !journalLink.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(journalLink));
            startActivity(intent);
        } else {
            Log.e(TAG, "Invalid journal link: " + journalLink);
        }
    }

    private boolean tasksLoaded = false;
    private boolean journalsLoaded = false;
    private boolean categoriesLoaded = false;

    private void checkAllDataLoaded() {
        // If all data is loaded, hide
        if (tasksLoaded && journalsLoaded && categoriesLoaded) {
            showLoading(false);
        }
    }

    @Override
    public void onPathNodeClick(int position, LearningPathModel pathModel) {
        if (!pathModel.isUnlocked()) {
            Toast.makeText(requireContext(), "Complete previous lessons to unlock this one", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pathModel.isCompleted()) {
            Toast.makeText(requireContext(), "Lesson already completed!", Toast.LENGTH_SHORT).show();
            return;
        }

        simulateLessonCompletion(position, pathModel);

        Intent intent = new Intent(getActivity(), FlashcardsActivity.class);
        startActivity(intent);

        // Or you can start an actual activity like this:
        // Intent intent = new Intent(requireContext(), QuestionsActivity.class);
        // intent.putExtra("lessonId", pathModel.getId());
        // intent.putExtra("lessonTitle", pathModel.getTitle());
        // startActivityForResult(intent, 1);
    }

    private void simulateLessonCompletion(int position, LearningPathModel pathModel) {
        // Mark the node as completed
        LearningPathHelper.completeNode(requireContext(), learningPathList, position);

        // Update the adapter
        learningPathAdapter.updateNodeCompletion(position);

        // Update the header with new progress
        updateLearningPathHeader();

        // Show completion message
        String message = "Completed: " + pathModel.getTitle();
        if (position + 1 < learningPathList.size()) {
            message += "\nNext lesson unlocked!";
        } else {
            message += "\nCongratulations! All lessons completed!";
        }

        //Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

        // Check if path is completed
        if (LearningPathHelper.isPathCompleted(learningPathList)) {
            showPathCompletionDialog();
        }
    }

    private void showPathCompletionDialog() {
        // You can show a custom dialog or use your existing ShowRedeem dialog
        int totalScore = 100; // Calculate based on your scoring system
        int correctAnswers = learningPathList.size();
        int totalQuestions = learningPathList.size();
        float totalTime = 60.0f; // Or calculate actual time spent

        showRedeemDialog(totalScore, correctAnswers, totalQuestions, totalTime);
    }

    public void setLearningPathNodeCount(int count) {
        if (count > 0 && count <= 20) { // Reasonable limits
            String categoryTitle = "Grafuri"; // Or get from current category
            learningPathList = LearningPathHelper.generateLearningPath(requireContext(), count, categoryTitle);
            if (learningPathAdapter != null) {
                learningPathAdapter.notifyDataSetChanged();
            }
            // Update header when node count changes
            updateLearningPathHeader();
        }
    }

    public void resetLearningPath() {
        LearningPathHelper.resetLearningPath(requireContext(), learningPathList);
        if (learningPathAdapter != null) {
            learningPathAdapter.notifyDataSetChanged();
        }
        // Update header when path is reset
        updateLearningPathHeader();
        Toast.makeText(requireContext(), "Learning path reset!", Toast.LENGTH_SHORT).show();
    }

    public int getLearningPathProgress() {
        return LearningPathHelper.getProgressPercentage(learningPathList);
    }

    private void showRedeemDialog(int totalScore, int correctAnswers, int totalQuestions, float totalTime) {
        if (totalScore > 0) {
            ShowRedeem bottomSheetFragment = new ShowRedeem();
            Bundle bundle = new Bundle();

            bundle.putInt("totalScore", totalScore);
            bundle.putInt("totalQuestions", totalQuestions);
            bundle.putFloat("totalTime", totalTime);
            bundle.putInt("correctAnswers", correctAnswers);

            bottomSheetFragment.setArguments(bundle);
            bottomSheetFragment.show(requireActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        retrieveUserData();
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            int totalScore = data.getIntExtra("totalScore", 0);
            int correctAnswers = data.getIntExtra("correctAnswers", 0);
            int totalQuestions = data.getIntExtra("totalQuestions", 0);
            long totalTime = data.getLongExtra("totalTime", 0L);
            float totalResponseTime = (float) totalTime / 1000;
            handler.postDelayed(() -> showRedeemDialog(totalScore, correctAnswers, totalQuestions, totalResponseTime), 1000);

            // Update the total score in the database
            DbQuery.updateTotalScore(userEmail, totalScore, new MyCompleteListener() {
                @Override
                public void onSucces() {
                    Log.d(TAG, "Total score updated successfully in Firestore.");
                }

                @Override
                public void onFailure() {
                    Log.e(TAG, "Failed to update total score in Firestore.");
                }
            });
        }
    }

    private void retrieveUserData() {
        // Get the SharedPreferences object
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);

        // Retrieve the values using the keys
        userEmail = sharedPreferences.getString("userEmail", "");
    }
}