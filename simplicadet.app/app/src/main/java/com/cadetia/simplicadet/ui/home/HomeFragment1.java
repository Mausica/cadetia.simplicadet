package com.cadetia.simplicadet.ui.home;

import static android.content.Context.MODE_PRIVATE;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.Flashcards;
import com.cadetia.simplicadet.activities.PathSelector;
import com.cadetia.simplicadet.activities.Questions;
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
import com.cadetia.simplicadet.utils.ImageStorageUtils;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeFragment1 extends Fragment implements LearningPathAdapter.OnLearningPathClickListener, JournalAdapter.OnJournalClickListener {
    private static final String TAG = "HomeFragment1";
    private static final int LEARNING_PATH_REQUEST = 101;
    private static final int PATH_SELECTOR_REQUEST = 102;
    private static final int IMAGE_PICKER_REQUEST = 103;
    private static final long CACHE_DURATION = 30000;

    private RecyclerView categoryRecyclerView, mainTasksRecycler, journalRecyclerView;
    private MainTaskAdapter mainTaskAdapter;
    private JournalAdapter journalAdapter;
    private LearningPathAdapter learningPathAdapter;
    private List<Task> tasks = new ArrayList<>();
    private List<JournalEntry> journalList = new ArrayList<>();
    private List<LearningPathModel> learningPathList = new ArrayList<>();
    private View learningPathHeader, loadingLayout, contentView;
    private TextView pathTitle, progressText;
    private CircularProgressIndicator circularProgress;
    private ImageView scheduleImage;
    private Handler handler = new Handler();
    private ExecutorService executorService;
    private boolean isLoadingDismissed = false, tasksLoaded = false, journalsLoaded = false, categoriesLoaded = false;
    private int lastClickedPosition = -1;
    private String userEmail;
    private LruCache<String, Bitmap> memCache;
    private DbQuery.LearningPath currentLearningPath;
    private SharedPreferences learningPathPrefs;
    private List<Task> cachedTasks;
    private List<JournalEntry> cachedJournals;
    private DbQuery.LearningPath cachedLearningPath;
    private long tasksLastLoad = 0, journalsLastLoad = 0, pathLastLoad = 0;

    private ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newCachedThreadPool();
        }
        return executorService;
    }

    private boolean isFragmentSafe() {
        return isAdded() && !isDetached() && getContext() != null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home1, container, false);
        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout1);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        mainTasksRecycler = view.findViewById(R.id.tasksMainRecyclerView);
        journalRecyclerView = view.findViewById(R.id.journalRecyclerView);
        learningPathHeader = view.findViewById(R.id.learningPathHeader);
        scheduleImage = view.findViewById(R.id.schedule_image);

        setupCache();
        setupViews();
        setupImageView();
        resetLoadingState();
        showLoading(true);
        return view;
    }

    private void setupImageView() {
        if (scheduleImage != null) {
            loadStoredImage();
            scheduleImage.setOnClickListener(v -> openImagePicker());
        }
    }

    private void loadStoredImage() {
        if (ImageStorageUtils.hasStoredImage(requireContext())) {
            Bitmap storedImage = ImageStorageUtils.loadImageFromInternalStorage(requireContext());
            if (storedImage != null) {
                scheduleImage.setImageBitmap(storedImage);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICKER_REQUEST);
    }

    private void resetLoadingState() {
        isLoadingDismissed = false;
        tasksLoaded = false;
        journalsLoaded = false;
        categoriesLoaded = false;
    }

    private void setupViews() {
        if (learningPathHeader != null) {
            pathTitle = learningPathHeader.findViewById(R.id.pathTitle);
            progressText = learningPathHeader.findViewById(R.id.progressText);
            circularProgress = learningPathHeader.findViewById(R.id.circularProgress);
            learningPathHeader.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), PathSelector.class);
                startActivity(intent);
            });
        }
        learningPathPrefs = requireActivity().getSharedPreferences("LearningPathProgress", MODE_PRIVATE);
        mainTasksRecycler.setHasFixedSize(true);
        mainTasksRecycler.setItemViewCacheSize(15);
        mainTasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mainTaskAdapter = new MainTaskAdapter(tasks);
        mainTasksRecycler.setAdapter(mainTaskAdapter);
        journalRecyclerView.setHasFixedSize(true);
        journalRecyclerView.setItemViewCacheSize(10);
        journalRecyclerView.setDrawingCacheEnabled(true);
        journalRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        journalRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        journalAdapter = new JournalAdapter(journalList, this, memCache);
        journalRecyclerView.setAdapter(journalAdapter);
        categoryRecyclerView.setHasFixedSize(true);
        categoryRecyclerView.setItemViewCacheSize(8);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        learningPathAdapter = new LearningPathAdapter(learningPathList, requireContext(), this);
        categoryRecyclerView.setAdapter(learningPathAdapter);
    }

    private void setupCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 3;
        memCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    @Override
    public void onDestroyView() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (journalAdapter != null) journalAdapter.cleanup();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) executorService.shutdownNow();
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        loadingLayout = null;
        contentView = null;
        categoryRecyclerView = null;
        mainTasksRecycler = null;
        journalRecyclerView = null;
        learningPathHeader = null;
        pathTitle = null;
        progressText = null;
        circularProgress = null;
        scheduleImage = null;
        mainTaskAdapter = null;
        journalAdapter = null;
        learningPathAdapter = null;
        super.onDestroyView();
    }

    private void showLoading(boolean show) {
        if (loadingLayout != null && contentView != null) {
            if (show) {
                isLoadingDismissed = false;
                loadingLayout.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
            } else if (!isLoadingDismissed) {
                isLoadingDismissed = true;
                Context ctx = getContext();
                if (ctx != null && isAdded()) {
                    loadingLayout.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_out));
                    handler.postDelayed(() -> {
                        if (loadingLayout != null && contentView != null && isAdded()) {
                            loadingLayout.setVisibility(View.GONE);
                            contentView.setVisibility(View.VISIBLE);
                            contentView.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.fade_in));
                        }
                    }, 250);
                } else {
                    loadingLayout.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void loadTasks() {
        if (!isFragmentSafe()) return;
        getExecutorService().execute(() -> {
            try {
                if (!isFragmentSafe()) return;
                List<Task> taskList = DatabaseClient.getInstance(requireContext()).getAppDatabase().dataBaseAction().getAllTasksList();
                if (handler != null) {
                    handler.post(() -> {
                        if (!isFragmentSafe()) return;
                        tasks.clear();
                        if (taskList != null) {
                            tasks.addAll(taskList);
                            cachedTasks = new ArrayList<>(taskList);
                            tasksLastLoad = System.currentTimeMillis();
                        }
                        if (mainTaskAdapter != null) mainTaskAdapter.notifyDataSetChanged();
                        tasksLoaded = true;
                        checkAllDataLoaded();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading tasks", e);
                if (handler != null) {
                    handler.post(() -> {
                        if (isFragmentSafe()) {
                            tasksLoaded = true;
                            checkAllDataLoaded();
                        }
                    });
                }
            }
        });
    }

    private void loadJournals() {
        DbQuery.loadJournals("NEWS", new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSucces() {
                if (!isFragmentSafe()) return;
                journalList.clear();
                if (DbQuery.g_militaryJournalList != null && !DbQuery.g_militaryJournalList.isEmpty()) {
                    journalList.addAll(DbQuery.g_militaryJournalList);
                    cachedJournals = new ArrayList<>(DbQuery.g_militaryJournalList);
                    journalsLastLoad = System.currentTimeMillis();
                    preloadJournalImages();
                }
                if (journalAdapter != null) journalAdapter.notifyDataSetChanged();
                journalsLoaded = true;
                checkAllDataLoaded();
            }
            @Override
            public void onFailure() {
                if (isFragmentSafe()) {
                    journalsLoaded = true;
                    checkAllDataLoaded();
                }
            }
        });
    }

    private void preloadJournalImages() {
        if (journalList != null && !journalList.isEmpty() && isFragmentSafe()) {
            getExecutorService().execute(() -> {
                for (int i = 0; i < Math.min(3, journalList.size()); i++) {
                    if (!isFragmentSafe()) break;
                    final String imageUrl = journalList.get(i).getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            Glide.with(requireContext()).asBitmap().load(imageUrl).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).override(200, 150)).preload();
                        } catch (Exception e) {
                            Log.e(TAG, "Error preloading image: " + imageUrl, e);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void loadSelectedLearningPath() {
        SharedPreferences selectedPathPrefs = requireActivity().getSharedPreferences("SelectedLearningPath", MODE_PRIVATE);
        String selectedPathId = selectedPathPrefs.getString("selectedPathId", null);
        if (selectedPathId != null) {
            DbQuery.selectLearningPath(selectedPathId, new MyCompleteListener() {
                @Override
                public void onSucces() {
                    if (!isFragmentSafe()) return;
                    currentLearningPath = DbQuery.g_learningPath;
                    if (currentLearningPath != null) {
                        cachedLearningPath = currentLearningPath;
                        pathLastLoad = System.currentTimeMillis();
                        populateLearningPathList();
                        if (learningPathAdapter != null) learningPathAdapter.notifyDataSetChanged();
                        updateLearningPathHeader();
                    }
                    categoriesLoaded = true;
                    checkAllDataLoaded();
                }
                @Override
                public void onFailure() {
                    if (isFragmentSafe()) loadLearningPathFromDb();
                }
            });
        } else {
            loadLearningPathFromDb();
        }
    }

    private void loadLearningPathFromDb() {
        DbQuery.loadLearningPath(new MyCompleteListener() {
            @Override
            public void onSucces() {
                if (!isFragmentSafe()) return;
                currentLearningPath = DbQuery.g_learningPath;
                if (currentLearningPath != null) {
                    cachedLearningPath = currentLearningPath;
                    pathLastLoad = System.currentTimeMillis();
                    populateLearningPathList();
                    if (learningPathAdapter != null) learningPathAdapter.notifyDataSetChanged();
                    updateLearningPathHeader();
                }
                categoriesLoaded = true;
                checkAllDataLoaded();
            }
            @Override
            public void onFailure() {
                if (isFragmentSafe()) {
                    categoriesLoaded = true;
                    checkAllDataLoaded();
                }
            }
        });
    }

    private void populateLearningPathList() {
        learningPathList.clear();
        boolean previousCompleted = true;
        if (currentLearningPath != null && currentLearningPath.nodes != null) {
            for (int i = 0; i < currentLearningPath.nodes.size(); i++) {
                DbQuery.LearningPathNode node = currentLearningPath.nodes.get(i);
                String nodeKey = currentLearningPath.id + "_" + node.id;
                boolean isCompleted = learningPathPrefs.getBoolean(nodeKey, false);
                LearningPathModel model = new LearningPathModel(node.id, node.title, node.type == 0 ? R.drawable.ic_play : R.drawable.home_ic_new, isCompleted, (i == 0 || previousCompleted));
                model.setType(node.type);
                learningPathList.add(model);
                previousCompleted = model.isCompleted() && model.isUnlocked();
            }
        }
    }

    private void updateLearningPathHeader() {
        if (learningPathHeader == null || learningPathList.isEmpty() || currentLearningPath == null) {
            if (learningPathHeader != null) learningPathHeader.setVisibility(View.GONE);
            return;
        }
        int completed = 0;
        for (LearningPathModel node : learningPathList) {
            if (node.isCompleted()) completed++;
        }
        int progress = learningPathList.size() > 0 ? (completed * 100) / learningPathList.size() : 0;
        if (pathTitle != null) pathTitle.setText(currentLearningPath.title);
        if (progressText != null) progressText.setText(progress + "%");
        if (circularProgress != null) circularProgress.setProgress(progress);
        learningPathHeader.setVisibility(View.VISIBLE);
    }

    @Override
    public void onJournalClick(String journalLink) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No Internet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (journalLink != null && !journalLink.isEmpty()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(journalLink)));
            }
            catch (Exception e) {
                Log.e(TAG, "Error opening journal link", e);
                Toast.makeText(requireContext(), "Could not open link", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAllDataLoaded() {
        if (tasksLoaded && journalsLoaded && categoriesLoaded) showLoading(false);
    }

    @Override
    public void onPathNodeClick(int position, LearningPathModel pathModel) {
        if (!pathModel.isUnlocked()) {
            Toast.makeText(requireContext(), "Unlock previous first", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent;
        if (pathModel.getType() == 0) {
            intent = new Intent(getActivity(), Questions.class);
            String categoryId = (currentLearningPath != null) ? currentLearningPath.title : "LearningPath";
            intent.putExtra("categoryId", categoryId);
            intent.putExtra("testId", pathModel.getId());
        } else {
            intent = new Intent(getActivity(), Flashcards.class);
            intent.putExtra("flashcardId", pathModel.getId());
        }
        intent.putExtra("position", position);
        this.lastClickedPosition = position;
        startActivityForResult(intent, LEARNING_PATH_REQUEST);
    }

    private void markNodeAsCompleted(int position) {
        if (position >= 0 && position < learningPathList.size() && currentLearningPath != null) {
            LearningPathModel nodeModel = learningPathList.get(position);
            if (!nodeModel.isCompleted()) {
                String nodeKey = currentLearningPath.id + "_" + nodeModel.getId();
                learningPathPrefs.edit().putBoolean(nodeKey, true).apply();
                nodeModel.setCompleted(true);
                if (position + 1 < learningPathList.size()) learningPathList.get(position + 1).setUnlocked(true);
                if (learningPathAdapter != null) learningPathAdapter.notifyDataSetChanged();
                updateLearningPathHeader();
                if (isPathCompleted()) showPathCompletionDialog();
            }
        }
    }

    private boolean isPathCompleted() {
        for (LearningPathModel node : learningPathList) {
            if (!node.isCompleted()) return false;
        }
        return !learningPathList.isEmpty();
    }

    private void showPathCompletionDialog() {
        showRedeemDialog(100, learningPathList.size(), learningPathList.size(), 60.0f);
    }

    private void showRedeemDialog(int score, int correct, int total, float time) {
        if (isFragmentSafe() && score > 0) {
            ShowRedeem sheet = new ShowRedeem();
            Bundle b = new Bundle();
            b.putInt("totalScore", score);
            b.putInt("totalQuestions", total);
            b.putFloat("totalTime", time);
            b.putInt("correctAnswers", correct);
            sheet.setArguments(b);
            sheet.show(requireActivity().getSupportFragmentManager(), sheet.getTag());
        }
    }

    private boolean hasLearningPathChanged() {
        SharedPreferences selectedPathPrefs = requireActivity().getSharedPreferences("SelectedLearningPath", MODE_PRIVATE);
        String selectedPathId = selectedPathPrefs.getString("selectedPathId", null);

        if (cachedLearningPath == null && selectedPathId == null) return false;
        if (cachedLearningPath == null || selectedPathId == null) return true;

        return !selectedPathId.equals(cachedLearningPath.id);
    }

    @Override
    public void onResume() {
        super.onResume();
        long currentTime = System.currentTimeMillis();
        retrieveUserData();
        resetLoadingState();
        showLoading(true);
        loadTasks();
        loadStoredImage();

        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            if (cachedJournals != null && (currentTime - journalsLastLoad) < CACHE_DURATION) {
                journalList.clear();
                journalList.addAll(cachedJournals);
                if (journalAdapter != null) journalAdapter.notifyDataSetChanged();
                journalsLoaded = true;
                checkAllDataLoaded();
            } else {
                loadJournals();
            }

            boolean pathChanged = hasLearningPathChanged();
            if (!pathChanged && cachedLearningPath != null && (currentTime - pathLastLoad) < CACHE_DURATION) {
                currentLearningPath = cachedLearningPath;
                populateLearningPathList();
                if (learningPathAdapter != null) learningPathAdapter.notifyDataSetChanged();
                updateLearningPathHeader();
                categoriesLoaded = true;
                checkAllDataLoaded();
            } else {
                if (pathChanged) {
                    cachedLearningPath = null;
                }
                loadSelectedLearningPath();
            }
        } else {
            journalsLoaded = true;
            categoriesLoaded = true;
            checkAllDataLoaded();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        retrieveUserData();

        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                String imagePath = ImageStorageUtils.saveImageToInternalStorage(
                        requireContext(),
                        selectedImageUri,
                        "schedule_image_" + System.currentTimeMillis()
                );
                if (imagePath != null) {
                    loadStoredImage();
                    Toast.makeText(requireContext(), "Image saved successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        if (requestCode == PATH_SELECTOR_REQUEST && resultCode == Activity.RESULT_OK) {
            cachedLearningPath = null;
            pathLastLoad = 0;

            resetLoadingState();
            showLoading(true);
            loadTasks();

            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                if (cachedJournals != null && (System.currentTimeMillis() - journalsLastLoad) < CACHE_DURATION) {
                    journalList.clear();
                    journalList.addAll(cachedJournals);
                    if (journalAdapter != null) journalAdapter.notifyDataSetChanged();
                    journalsLoaded = true;
                    checkAllDataLoaded();
                } else {
                    loadJournals();
                }

                loadSelectedLearningPath();
            } else {
                journalsLoaded = true;
                categoriesLoaded = true;
                checkAllDataLoaded();
            }
            return;
        }

        if (requestCode == LEARNING_PATH_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            int position = data.getIntExtra("position", -1);
            if (position == -1) position = lastClickedPosition;
            lastClickedPosition = -1;
            if (position != -1 && position < learningPathList.size()) {
                final LearningPathModel completedNode = learningPathList.get(position);
                final int finalPosition = position;
                if (completedNode.getType() == 0) {
                    int score = data.getIntExtra("totalScore", 0);
                    int correct = data.getIntExtra("correctAnswers", 0);
                    int total = data.getIntExtra("totalQuestions", 0);
                    long time = data.getLongExtra("totalTime", 0L);
                    DbQuery.updateTotalScore(userEmail, score, new MyCompleteListener() {
                        @Override
                        public void onSucces() {
                        }

                        @Override
                        public void onFailure() {
                        }
                    });
                    if (score > 0) {
                        handler.postDelayed(() -> {
                            showRedeemDialog(score, correct, total, (float) time / 1000);
                            markNodeAsCompleted(finalPosition);
                        }, 1000);
                    } else {
                        markNodeAsCompleted(finalPosition);
                    }
                } else {
                    markNodeAsCompleted(finalPosition);
                }
            }
        }
    }

    private void retrieveUserData() {
        if (getActivity() != null) {
            SharedPreferences sp = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);
            userEmail = sp.getString("userEmail", "");
        }
    }
}