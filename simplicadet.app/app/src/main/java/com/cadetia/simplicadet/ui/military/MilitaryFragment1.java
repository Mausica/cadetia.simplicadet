package com.cadetia.simplicadet.ui.military;

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
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.QuestionsActivity;
import com.cadetia.simplicadet.activities.ShowRedeem;
import com.cadetia.simplicadet.adapters.CategoryAdapter;
import com.cadetia.simplicadet.adapters.DestinationAdapter;
import com.cadetia.simplicadet.adapters.JournalAdapter;
import com.cadetia.simplicadet.adapters.MainTaskAdapter;
import com.cadetia.simplicadet.adapters.RankAdapter;
import com.cadetia.simplicadet.database.DatabaseClient;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.DestinationItem;
import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.RankModel;
import com.cadetia.simplicadet.model.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MilitaryFragment1 extends Fragment implements CategoryAdapter.OnQuizClickListener, JournalAdapter.OnJournalClickListener, DestinationAdapter.OnDestinationClickListener {

    private static final String TAG = "MilitaryFragment1";
    private RecyclerView categoryRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Task> tasks = new ArrayList<>();
    private RecyclerView rankRecyclerView;
    private RankAdapter rankAdapter;
    private List<RankModel> rankList = new ArrayList<>();
    private Handler handler = new Handler();
    private boolean isLoadingDismissed = false;
    private String userEmail;
    private RecyclerView journalRecyclerView;
    private JournalAdapter journalAdapter;
    private List<JournalEntry> journalList = new ArrayList<>();
    private LruCache<String, Bitmap> memCache;

    // New variables for destination feature
    private RecyclerView destinationRecyclerView;
    private DestinationAdapter destinationAdapter;
    private List<DestinationItem> destinationItems = new ArrayList<>();
    private FirebaseFirestore db;

    private View loadingLayout;
    private View contentView;

    public MilitaryFragment1() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_military1, container, false);

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout1);

        rankRecyclerView = view.findViewById(R.id.rankRecyclerView);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        journalRecyclerView = view.findViewById(R.id.journalRecyclerView);

        // Initialize destination RecyclerView - make sure to add this to your layout XML
        destinationRecyclerView = view.findViewById(R.id.destinationRecyclerView);

        showLoading(true);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

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
            if (isAdded()) {
                loadJournals();
                loadCategories();
                loadRanks();
                loadDestinations();
            } else {
                Log.w(TAG, "Fragment not attached in postDelayed");
            }
        }, 1000);
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

    private void loadDestinations() {
        db.document("MILITARY/RO/CNMTV/PICTURES")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        destinationItems.clear();
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            for (String key : document.getData().keySet()) {
                                String imageUrl = document.getString(key);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    DestinationItem item = new DestinationItem(key, imageUrl);
                                    destinationItems.add(item);
                                }
                            }

                            // Shuffle the items if there are more than 3
                            if (destinationItems.size() > 3) {
                                Collections.shuffle(destinationItems);
                            }

                            setupDestinationRecyclerView();
                        }

                        destinationsLoaded = true;
                        checkAllDataLoaded();
                    } else {
                        Log.e(TAG, "Error getting destinations: ", task.getException());
                        destinationsLoaded = true;
                        checkAllDataLoaded();
                    }
                });
    }


    // Set up the destination RecyclerView with PagerSnapHelper for swipe animation
    private void setupDestinationRecyclerView() {
        if (!isAdded()) return;

        Context context = requireContext();

        // Create and set up adapter
        destinationAdapter = new DestinationAdapter(destinationItems, context, memCache, this);

        // Use LinearLayoutManager with horizontal orientation
        if (destinationRecyclerView.getLayoutManager() == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            destinationRecyclerView.setLayoutManager(layoutManager);
        }

        // Safely attach SnapHelper (avoid IllegalStateException)
        if (destinationRecyclerView.getOnFlingListener() != null) {
            destinationRecyclerView.setOnFlingListener(null);
        }

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(destinationRecyclerView);

        // Set adapter
        destinationRecyclerView.setAdapter(destinationAdapter);

        // Add scroll listener to handle pagination effects
        destinationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Animation already happening in adapter
                }
            }
        });
    }


    @Override
    public void onDestinationClick(DestinationItem destination) {
        // Handle destination click if needed
        Log.d(TAG, "Destination clicked: " + destination.getId());
    }

    private void loadRanks() {
        DbQuery.loadRanks(new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSucces() {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not attached, skipping rank load.");
                    return;
                }

                if (rankAdapter == null) {
                    rankAdapter = new RankAdapter(rankList, requireContext(), memCache);
                }

                LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
                rankRecyclerView.setLayoutManager(layoutManager);
                rankRecyclerView.setAdapter(rankAdapter);

                rankList.clear();
                rankList.addAll(DbQuery.g_rankList);
                rankAdapter.notifyDataSetChanged();

                ranksLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load ranks");
                ranksLoaded = true;
                checkAllDataLoaded();
            }
        });
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
                    journalAdapter = new JournalAdapter(journalList, MilitaryFragment1.this, memCache);
                }

                journalRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                journalRecyclerView.setAdapter(journalAdapter);

                journalList.clear();
                journalList.addAll(DbQuery.g_journalList);
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
                categoriesLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load categories");
                categoriesLoaded = true;
                checkAllDataLoaded();
            }
        });
    }

    // Helper method to check if all data is loaded
    private boolean journalsLoaded = false;
    private boolean categoriesLoaded = false;
    private boolean ranksLoaded = false;
    private boolean destinationsLoaded = false;

    private void checkAllDataLoaded() {
        // If all data is loaded, hide loading
        if (journalsLoaded && categoriesLoaded && ranksLoaded && destinationsLoaded) {
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