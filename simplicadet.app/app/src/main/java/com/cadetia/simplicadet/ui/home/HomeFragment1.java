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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cadetia.simplicadet.R;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private String userEmail;
    private RecyclerView journalRecyclerView;
    private JournalAdapter journalAdapter;
    private List<JournalEntry> journalList = new ArrayList<>();
    private LruCache<String, Bitmap> memCache;

    public HomeFragment1() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home1, container, false);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        mainTasksRecycler = view.findViewById(R.id.tasksMainRecyclerView);
        journalRecyclerView = view.findViewById(R.id.journalRecyclerView);
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
        loadJournals();
        loadTaskFirstLayout();
        loadCategories();
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
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load journals");
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
                    mainTasksRecycler.setAdapter(mainTaskAdapter); // Ensure adapter is set here after data is ready
                } else {
                    mainTaskAdapter.notifyDataSetChanged();
                }
            }
        }

        GetSavedTasks savedTasks = new GetSavedTasks();
        savedTasks.execute();
    }

    private void setUpAdapter() {
        mainTaskAdapter = new MainTaskAdapter(tasks);
        mainTasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mainTasksRecycler.setAdapter(mainTaskAdapter); // Set the adapter for the RecyclerView
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
        DbQuery.loadCategories(new MyCompleteListener() {
            @Override
            public void onSucces() {
                List<CategoryModel> categoryList = DbQuery.g_catList;
                if (categoryList != null && !categoryList.isEmpty()) {
                    setUpCategoryRecyclerView(categoryList);
                } else {
                    Log.e(TAG, "Category list is empty");
                }
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load categories");
            }
        });
    }

    private void setUpCategoryRecyclerView(List<CategoryModel> categoryList) {
        if (isAdded()) { // Check if the fragment is attached
            categoryAdapter = new CategoryAdapter(categoryList, requireContext(), this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            categoryRecyclerView.setLayoutManager(layoutManager);
            categoryRecyclerView.setAdapter(categoryAdapter);
        } else {
            Log.e(TAG, "Fragment is not attached, cannot set up category recycler view");
        }
    }

    @Override
    public void onQuizClick(String categoryId, String testId) {
        Log.e(TAG, "Category ID: " + categoryId + ", Test ID: " + testId);

        // Get the context from categoryRecyclerView
        Context context = categoryRecyclerView.getContext();
        Intent intent = new Intent(context, QuestionsActivity.class);

        // Add the categoryId and testId to the intent
        intent.putExtra("categoryId", categoryId);
        intent.putExtra("testId", testId);

        startActivityForResult(intent, 1);
    }

    private void showRedeemDialog(int totalScore, int correctAnswers, float averageTime) {
        if (totalScore > 0) {
            ShowRedeem bottomSheetFragment = new ShowRedeem();
            Bundle bundle = new Bundle();
            bundle.putInt("totalScore", totalScore);
            bundle.putInt("correctAnswers", correctAnswers);
            bundle.putFloat("averageTime", averageTime);
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
            float averageTime = data.getFloatExtra("averageTime", 0);
            handler.postDelayed(() -> showRedeemDialog(totalScore, correctAnswers, averageTime), 1000);


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