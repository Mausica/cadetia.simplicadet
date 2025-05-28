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
import com.cadetia.simplicadet.activities.QuestionsActivity;
import com.cadetia.simplicadet.activities.ShowRedeem;
import com.cadetia.simplicadet.adapters.JournalAdapter;
import com.cadetia.simplicadet.adapters.LearningPathAdapter;
import com.cadetia.simplicadet.adapters.MainTaskAdapter;
import com.cadetia.simplicadet.database.DatabaseClient;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.LearningPathModel;
import com.cadetia.simplicadet.model.Task;
import com.cadetia.simplicadet.utils.NetworkUtils;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment1 extends Fragment implements LearningPathAdapter.OnLearningPathClickListener, JournalAdapter.OnJournalClickListener {
    private static final String TAG = "HomeFragment1"; // <-- For logging
    private RecyclerView categoryRecyclerView, mainTasksRecycler, journalRecyclerView;
    private MainTaskAdapter mainTaskAdapter;
    private List<Task> tasks = new ArrayList<>();
    private Handler handler = new Handler();
    private boolean isLoadingDismissed = false;
    private String userEmail;
    private JournalAdapter journalAdapter;
    private List<JournalEntry> journalList = new ArrayList<>();
    private LruCache<String, Bitmap> memCache;
    private LearningPathAdapter learningPathAdapter;
    private List<LearningPathModel> learningPathList = new ArrayList<>();
    private View learningPathHeader, loadingLayout, contentView;
    private TextView pathTitle, progressText;
    private CircularProgressIndicator circularProgress;
    private DbQuery.LearningPath currentLearningPath;
    private SharedPreferences learningPathPrefs;
    private static final int LEARNING_PATH_REQUEST = 101;
    private boolean tasksLoaded = false, journalsLoaded = false, categoriesLoaded = false;
    private int lastClickedPosition = -1; // <-- Add this for fallback

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home1, container, false);
        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout1);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        mainTasksRecycler = view.findViewById(R.id.tasksMainRecyclerView);
        journalRecyclerView = view.findViewById(R.id.journalRecyclerView);
        learningPathHeader = view.findViewById(R.id.learningPathHeader);
        if (learningPathHeader != null) {
            pathTitle = learningPathHeader.findViewById(R.id.pathTitle);
            progressText = learningPathHeader.findViewById(R.id.progressText);
            circularProgress = learningPathHeader.findViewById(R.id.circularProgress);
        }
        learningPathPrefs = requireActivity().getSharedPreferences("LearningPathProgress", MODE_PRIVATE);
        showLoading(true);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
        memCache = new LruCache<String, Bitmap>(cacheSize) { @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount() / 1024; } };
    }

    @Override
    public void onResume() {
        super.onResume();
        showLoading(true);
        new Handler().postDelayed(() -> {
            if (isAdded()) {
                retrieveUserData(); // Retrieve user data early
                loadTaskFirstLayout();
                if (NetworkUtils.isNetworkAvailable(requireContext())) { loadJournals(); loadLearningPathFromDb(); }
                else { journalsLoaded = true; categoriesLoaded = true; checkAllDataLoaded(); }
            }
        }, 1000);
    }

    private void loadLearningPathFromDb() {
        DbQuery.loadLearningPath(new MyCompleteListener() {
            @Override public void onSucces() {
                if (!isAdded()) return;
                currentLearningPath = DbQuery.g_learningPath;
                if (currentLearningPath != null) { populateLearningPathList(); setUpLearningPathRecyclerView(); updateLearningPathHeader(); }
                categoriesLoaded = true; checkAllDataLoaded();
            }
            @Override public void onFailure() { if (isAdded()) { categoriesLoaded = true; checkAllDataLoaded(); } }
        });
    }

    private void populateLearningPathList() {
        learningPathList.clear(); boolean previousCompleted = true;
        if (currentLearningPath != null && currentLearningPath.nodes != null) {
            for (int i = 0; i < currentLearningPath.nodes.size(); i++) {
                DbQuery.LearningPathNode node = currentLearningPath.nodes.get(i);
                String nodeKey = currentLearningPath.id + "_" + node.id;
                boolean isCompleted = learningPathPrefs.getBoolean(nodeKey, false);
                LearningPathModel model = new LearningPathModel(node.id, node.title, node.type == 0 ? R.drawable.ic_play : R.drawable.home_ic_new, isCompleted, (i == 0 || previousCompleted));
                model.setType(node.type); learningPathList.add(model);
                previousCompleted = model.isCompleted() && model.isUnlocked();
            }
        } else {
            Log.e(TAG, "Current Learning Path or its nodes are null!");
        }
    }


    private void updateLearningPathHeader() {
        if (learningPathHeader == null || learningPathList.isEmpty() || currentLearningPath == null) {
            if (learningPathHeader != null) learningPathHeader.setVisibility(View.GONE); return;
        }
        int completed = 0; for (LearningPathModel node : learningPathList) if (node.isCompleted()) completed++;
        int progress = learningPathList.size() > 0 ? (completed * 100) / learningPathList.size() : 0;
        if (pathTitle != null) pathTitle.setText(currentLearningPath.title);
        if (progressText != null) progressText.setText(progress + "%");
        if (circularProgress != null) circularProgress.setProgress(progress);
        learningPathHeader.setVisibility(View.VISIBLE);
    }

    private void setUpLearningPathRecyclerView() {
        if (isAdded()) {
            learningPathAdapter = new LearningPathAdapter(learningPathList, requireContext(), this);
            categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
            categoryRecyclerView.setAdapter(learningPathAdapter);
        }
    }

    private void showLoading(boolean show) {
        if (loadingLayout != null && contentView != null) {
            if (show) { isLoadingDismissed = false; loadingLayout.setVisibility(View.VISIBLE); contentView.setVisibility(View.GONE); }
            else if (!isLoadingDismissed) {
                isLoadingDismissed = true; Context ctx = getContext();
                if (ctx != null) {
                    loadingLayout.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_out));
                    new Handler().postDelayed(() -> {
                        if (loadingLayout != null && isAdded()) { loadingLayout.setVisibility(View.GONE); contentView.setVisibility(View.VISIBLE); contentView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_in)); }
                    }, 250);
                }
            }
        }
    }

    private void loadTaskFirstLayout() { if (isAdded()) { setUpAdapter(); getSavedTasks(); } }

    private void loadJournals() {
        DbQuery.loadJournals(new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override public void onSucces() {
                if (!isAdded()) return;
                if (journalAdapter == null) { journalAdapter = new JournalAdapter(journalList, HomeFragment1.this, memCache); journalRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext())); journalRecyclerView.setAdapter(journalAdapter); }
                journalList.clear(); journalList.addAll(DbQuery.g_homeJournalList); journalAdapter.notifyDataSetChanged();
                journalsLoaded = true; checkAllDataLoaded();
            }
            @Override public void onFailure() { journalsLoaded = true; checkAllDataLoaded(); }
        });
    }

    private void getSavedTasks() {
        @SuppressLint("StaticFieldLeak")
        class GetSavedTasks extends AsyncTask<Void, Void, List<Task>> {
            @Override protected List<Task> doInBackground(Void... v) { return DatabaseClient.getInstance(requireContext()).getAppDatabase().dataBaseAction().getAllTasksList(); }
            @SuppressLint("NotifyDataSetChanged")
            @Override protected void onPostExecute(List<Task> ft) {
                super.onPostExecute(ft); tasks.clear(); tasks.addAll(ft);
                if (mainTaskAdapter == null) setUpAdapter(); else mainTaskAdapter.notifyDataSetChanged();
                tasksLoaded = true; checkAllDataLoaded();
            }
        }
        new GetSavedTasks().execute();
    }

    private void setUpAdapter() {
        mainTaskAdapter = new MainTaskAdapter(tasks);
        mainTasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mainTasksRecycler.setAdapter(mainTaskAdapter);
    }

    @Override
    public void onJournalClick(String journalLink) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) { Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show(); return; }
        if (journalLink != null && !journalLink.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(journalLink)));
    }

    private void checkAllDataLoaded() { if (tasksLoaded && journalsLoaded && categoriesLoaded) showLoading(false); }

    @Override
    public void onPathNodeClick(int position, LearningPathModel pathModel) {
        if (!pathModel.isUnlocked()) { Toast.makeText(requireContext(), "Unlock previous first", Toast.LENGTH_SHORT).show(); return; }
        Intent intent;
        if (pathModel.getType() == 0) {
            intent = new Intent(getActivity(), QuestionsActivity.class);
            String categoryId = (currentLearningPath != null) ? currentLearningPath.title : "LearningPath";
            intent.putExtra("categoryId", categoryId);
            intent.putExtra("testId", pathModel.getId());
        } else {
            intent = new Intent(getActivity(), FlashcardsActivity.class);
            intent.putExtra("flashcardId", pathModel.getId());
        }
        intent.putExtra("position", position);
        this.lastClickedPosition = position; // <-- HACK: Store the position
        startActivityForResult(intent, LEARNING_PATH_REQUEST);
    }

    private void markNodeAsCompleted(int position) {
        if (position >= 0 && position < learningPathList.size() && currentLearningPath != null) {
            LearningPathModel nodeModel = learningPathList.get(position);
            // Ensure node isn't already completed before proceeding
            if (!nodeModel.isCompleted()) {
                Log.d(TAG, "Marking node at position " + position + " as completed.");
                String nodeKey = currentLearningPath.id + "_" + nodeModel.getId();
                learningPathPrefs.edit().putBoolean(nodeKey, true).apply();
                nodeModel.setCompleted(true);
                // Unlock the next node if it exists
                if (position + 1 < learningPathList.size()) {
                    learningPathList.get(position + 1).setUnlocked(true);
                }
                // Notify adapter and update header
                if (learningPathAdapter != null) {
                    learningPathAdapter.notifyDataSetChanged();
                }
                updateLearningPathHeader();
                //Toast.makeText(requireContext(), "Completed: " + nodeModel.getTitle(), Toast.LENGTH_SHORT).show();
                // Check if the whole path is now completed
                if (isPathCompleted()) {
                    showPathCompletionDialog();
                }
            } else {
                Log.d(TAG, "Node at position " + position + " is already completed.");
            }
        } else {
            Log.e(TAG, "markNodeAsCompleted: Invalid position or path data. Position: " + position);
        }
    }


    private boolean isPathCompleted() { for (LearningPathModel node : learningPathList) if (!node.isCompleted()) return false; return !learningPathList.isEmpty(); }
    private void showPathCompletionDialog() { showRedeemDialog(100, learningPathList.size(), learningPathList.size(), 60.0f); }

    private void showRedeemDialog(int score, int correct, int total, float time) {
        // Ensure fragment is added before showing dialog
        if (isAdded() && score > 0) {
            Log.d(TAG, "Showing Redeem Dialog. Score: " + score);
            ShowRedeem sheet = new ShowRedeem(); Bundle b = new Bundle();
            b.putInt("totalScore", score); b.putInt("totalQuestions", total); b.putFloat("totalTime", time); b.putInt("correctAnswers", correct);
            sheet.setArguments(b);
            sheet.show(requireActivity().getSupportFragmentManager(), sheet.getTag());
        } else {
            Log.w(TAG, "Not showing Redeem Dialog. IsAdded=" + isAdded() + ", Score=" + score);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        retrieveUserData(); // Ensure userEmail is fresh

        if (requestCode == LEARNING_PATH_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "onActivityResult: Received result for LEARNING_PATH_REQUEST");

            int position = data.getIntExtra("position", -1);
            Log.d(TAG, "onActivityResult: Position from Intent data = " + position);

            // Use the fallback if position wasn't returned
            if (position == -1) {
                position = lastClickedPosition;
                Log.w(TAG, "onActivityResult: Position not in Intent, using fallback = " + position);
            }

            lastClickedPosition = -1; // Reset fallback immediately

            if (position != -1 && position < learningPathList.size()) {
                Log.d(TAG, "onActivityResult: Processing position " + position);
                final LearningPathModel completedNode = learningPathList.get(position);
                final int finalPosition = position; // Use final copy for lambdas

                // Check if it was a Quiz (Type 0)
                if (completedNode.getType() == 0) {
                    int score = data.getIntExtra("totalScore", 0);
                    int correct = data.getIntExtra("correctAnswers", 0);
                    int total = data.getIntExtra("totalQuestions", 0);
                    long time = data.getLongExtra("totalTime", 0L);
                    Log.d(TAG, "onActivityResult: Quiz finished. Score = " + score);

                    // Update score in DB
                    DbQuery.updateTotalScore(userEmail, score, new MyCompleteListener() {
                        @Override public void onSucces() { Log.d(TAG, "Score updated successfully."); }
                        @Override public void onFailure() { Log.e(TAG, "Score update failed."); }
                    });

                    // Show redeem dialog only if score > 0
                    if (score > 0) {
                        handler.postDelayed(() -> {
                            showRedeemDialog(score, correct, total, (float) time / 1000);
                            // Mark completed AFTER showing dialog
                            markNodeAsCompleted(finalPosition);
                        }, 1000); // 1-second delay
                    } else {
                        // If score is 0, don't show dialog, but still mark completed
                        Log.d(TAG, "onActivityResult: Score is 0, marking completed without dialog.");
                        markNodeAsCompleted(finalPosition);
                    }
                } else { // It's a Flashcard (or other type)
                    Log.d(TAG, "onActivityResult: Flashcard finished. Marking node " + finalPosition + ".");
                    markNodeAsCompleted(finalPosition);
                }
            } else {
                Log.e(TAG, "onActivityResult: Invalid position (" + position + ") or data.");
            }
        } else {
            Log.w(TAG, "onActivityResult: Mismatch or no data. RC=" + requestCode + ", ResC=" + resultCode + ", Data=" + (data != null));
        }
    }


    private void retrieveUserData() {
        if (getActivity() != null) {
            SharedPreferences sp = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);
            userEmail = sp.getString("userEmail", "");
            Log.d(TAG, "User email retrieved: " + (userEmail.isEmpty() ? "EMPTY" : "OK"));
        } else {
            Log.e(TAG, "getActivity() is null in retrieveUserData");
        }
    }
}