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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.Home;
import com.cadetia.simplicadet.activities.QuestionsActivity;
import com.cadetia.simplicadet.activities.ShowRedeem;
import com.cadetia.simplicadet.adapters.CategoryAdapter;
import com.cadetia.simplicadet.adapters.JournalAdapter;
import com.cadetia.simplicadet.adapters.MainTaskAdapter;
import com.cadetia.simplicadet.database.DatabaseClient;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment1 extends Fragment implements CategoryAdapter.OnQuizClickListener, JournalAdapter.OnJournalClickListener {

    private static final String TAG = "HomeFragment1";
    private RecyclerView categoryRecyclerView;
    private RecyclerView mainTasksRecycler;
    private MainTaskAdapter mainTaskAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Task> tasks = new ArrayList<>();
    private Handler handler = new Handler();
    private boolean isLoadingDismissed = false;
    private String userEmail;
    private RecyclerView journalRecyclerView;
    private JournalAdapter journalAdapter;
    private List<JournalEntry> journalList = new ArrayList<>();
    private LruCache<String, Bitmap> memCache;

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

        showLoading(true);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Calculate the maximum memory available for caching (in kilobytes)
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // Cache size is 1/8th of the max memory

        // Initialize the cache
        memCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024; // Return the size of the bitmap in kilobytes
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        showLoading(true);
        new Handler().postDelayed(() -> {
            loadJournals();
            loadTaskFirstLayout();
            loadCategories();
        }, 1000); // 1 second delay to show
    }

    private void showLoading(boolean show) {
        if (loadingLayout != null && contentView != null) {
            if (show) {
                // Reset the flag when showing loading
                isLoadingDismissed = false;

                // Show loading immediately
                loadingLayout.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
            } else if (!isLoadingDismissed) { // Only hide if not already dismissed
                // Set flag to prevent multiple dismissals
                isLoadingDismissed = true;

                // Hide loading with animation
                loadingLayout.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out));
                new Handler().postDelayed(() -> {
                    if (loadingLayout != null) { // Safety check in case the fragment is destroyed
                        loadingLayout.setVisibility(View.GONE);

                        // Show content with animation
                        contentView.setVisibility(View.VISIBLE);
                        contentView.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in));
                    }
                }, 250); // Match the duration of the fade-out animation
            }
        }
    }


    private void loadTaskFirstLayout() {
        setUpAdapter();
        getSavedTasks();
    }

    private void loadJournals() {
        DbQuery.loadJournals(new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSucces() {
                // Create the adapter if it doesn't exist
                if (journalAdapter == null) {
                    journalAdapter = new JournalAdapter(journalList, HomeFragment1.this, memCache);
                }

                // Always set the adapter and layout manager to ensure the RecyclerView is properly initialized
                journalRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                journalRecyclerView.setAdapter(journalAdapter);

                // Update the data
                journalList.clear();
                journalList.addAll(DbQuery.g_journalList);
                journalAdapter.notifyDataSetChanged();

                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load journals");
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
        if (journalLink != null && !journalLink.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(journalLink));
            startActivity(intent);
        } else {
            Log.e(TAG, "Invalid journal link: " + journalLink);
        }
    }

    private void loadCategories() {
        DbQuery.loadCategories(requireContext(), new MyCompleteListener() {
            @Override
            public void onSucces() {
                List<CategoryModel> categoryList = DbQuery.g_catList;
                if (categoryList != null && !categoryList.isEmpty()) {
                    setUpCategoryRecyclerView(categoryList);
                } else {
                    Log.e(TAG, "Category list is empty");
                }
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load categories");
                checkAllDataLoaded();
            }
        });
    }

    // Helper method to check if all data is loaded
    private boolean tasksLoaded = false;
    private boolean journalsLoaded = false;
    private boolean categoriesLoaded = false;

    private void checkAllDataLoaded() {
        if (!tasksLoaded && mainTaskAdapter != null && !tasks.isEmpty()) {
            tasksLoaded = true;
        }

        if (!journalsLoaded && journalAdapter != null && !journalList.isEmpty()) {
            journalsLoaded = true;
        }

        if (!categoriesLoaded && categoryAdapter != null) {
            categoriesLoaded = true;
        }

        // If all data is loaded, hide
        if (tasksLoaded && journalsLoaded && categoriesLoaded) {
            showLoading(false);
        }
    }

    private void setUpCategoryRecyclerView(List<CategoryModel> categoryList) {
        if (isAdded()) { // Check if the fragment is attached
            categoryAdapter = new CategoryAdapter(categoryList, requireContext(), this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
            categoryRecyclerView.setLayoutManager(layoutManager);
            categoryRecyclerView.setAdapter(categoryAdapter);
        } else {
            Log.e(TAG, "Fragment is not attached, cannot set up category recycler view");
        }
    }

    @Override
    public void onQuizClick(String categoryId, String testId) {
        // Get the context from categoryRecyclerView
        Context context = categoryRecyclerView.getContext();
        Intent intent = new Intent(context, QuestionsActivity.class);

        // Add the categoryId and testId to the intent
        intent.putExtra("categoryId", categoryId);
        intent.putExtra("testId", testId);

        startActivityForResult(intent, 1);
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