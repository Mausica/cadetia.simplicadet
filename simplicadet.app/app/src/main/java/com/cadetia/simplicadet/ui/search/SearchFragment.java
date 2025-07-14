package com.cadetia.simplicadet.ui.search;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.Home;
import com.cadetia.simplicadet.activities.Questions;
import com.cadetia.simplicadet.adapters.CategoryAdapter;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.databinding.FragmentSearchBinding;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.Quizz;
import com.cadetia.simplicadet.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements CategoryAdapter.OnQuizClickListener {

    private static final String TAG = "SearchFragment";
    private FragmentSearchBinding binding;
    private ShapeableImageView searchImage;
    private TextInputEditText searchEditText;
    private RecyclerView searchRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<CategoryModel> allCategories = new ArrayList<>();
    private List<CategoryModel> filteredCategories = new ArrayList<>();
    private View loadingLayout;
    private View contentView;
    private boolean isLoadingDismissed = false;
    private boolean categoriesLoaded = false;
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SearchViewModel dashboardViewModel =
                new ViewModelProvider(this).get(SearchViewModel.class);

        binding = FragmentSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize views
        searchImage = root.findViewById(R.id.searchProfileButton);
        searchEditText = root.findViewById(R.id.text_search);
        searchRecyclerView = root.findViewById(R.id.searchRecyclerView);
        loadingLayout = root.findViewById(R.id.search_loading);
        contentView = root.findViewById(R.id.contentLayout2);
        searchEditText.setSingleLine(true);

        // Set up profile button click listener
        searchImage.setOnClickListener(v -> {
            Home homeActivity = (Home) requireActivity();
            homeActivity.openNavigationDrawer();
        });

        // Set up search functionality
        setupSearchFunctionality();

        // Hide FAB
        ((Home) getActivity()).hideFab();

        retrieveUserData();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategories();
    }

    private void setupSearchFunctionality() {
        // Handle text changes with debouncing
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Schedule new search with delay
                searchRunnable = () -> {
                    String query = s.toString().trim();
                    Log.d(TAG, "Text changed, filtering with: '" + query + "'");
                    filterCategories(query);
                };
                searchHandler.postDelayed(searchRunnable, 300); // 300ms delay
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle Enter key press to trigger immediate search
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO) {

                String query = searchEditText.getText().toString().trim();
                Log.d(TAG, "Enter pressed, searching for: '" + query + "'");
                hideKeyboard();

                // Cancel any pending search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                filterCategories(query); // Perform immediate search
                return true;
            }
            return false;
        });
    }

    private void filterCategories(String query) {
        Log.d(TAG, "Filtering categories with query: '" + query + "'");
        Log.d(TAG, "Total categories available: " + allCategories.size());

        filteredCategories.clear();

        if (query.isEmpty()) {
            filteredCategories.addAll(allCategories);
            Log.d(TAG, "Empty query - showing all categories: " + filteredCategories.size());
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (CategoryModel category : allCategories) {
                if (category == null) continue;

                boolean categoryMatches = category.getName() != null &&
                        category.getName().toLowerCase().contains(lowerCaseQuery);
                List<Quizz> matchingQuizzes = new ArrayList<>();

                if (category.getQuizzList() != null) {
                    for (Quizz quiz : category.getQuizzList()) {
                        if (quiz != null && quiz.getTitle() != null &&
                                quiz.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                            matchingQuizzes.add(quiz);
                            Log.d(TAG, "Found matching quiz: " + quiz.getTitle());
                        }
                    }
                }

                boolean hasMatchingQuiz = !matchingQuizzes.isEmpty();

                if (categoryMatches || hasMatchingQuiz) {
                    // If category name matches, include all quizzes; otherwise, only matching ones
                    List<Quizz> quizzesToInclude = categoryMatches ?
                            (category.getQuizzList() != null ? new ArrayList<>(category.getQuizzList()) : new ArrayList<>()) :
                            matchingQuizzes;

                    // Clone the category and set the filtered quizzes
                    CategoryModel filteredCategory = new CategoryModel(
                            category.getDocID(),
                            category.getName(),
                            quizzesToInclude.size(),
                            quizzesToInclude
                    );
                    filteredCategories.add(filteredCategory);
                    Log.d(TAG, "Added category: " + category.getName() + " with " + quizzesToInclude.size() + " quizzes");
                }
            }
        }

        Log.d(TAG, "Filtered categories count: " + filteredCategories.size());

        // Log details of filtered categories
        for (CategoryModel cat : filteredCategories) {
            Log.d(TAG, "Filtered category: " + cat.getName() + " with " +
                    (cat.getQuizzList() != null ? cat.getQuizzList().size() : 0) + " quizzes");
        }

        // Update the adapter
        updateCategoryAdapter();
    }

    private void updateCategoryAdapter() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, cannot update adapter");
            return;
        }

        // Ensure we're on the main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Initialize adapter if not already done
                if (categoryAdapter == null) {
                    Log.d(TAG, "Initializing category adapter");
                    categoryAdapter = new CategoryAdapter(filteredCategories, requireContext(), this);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
                    searchRecyclerView.setLayoutManager(layoutManager);
                    searchRecyclerView.setAdapter(categoryAdapter);
                } else {
                    Log.d(TAG, "Updating existing adapter with " + filteredCategories.size() + " categories");
                    categoryAdapter.updateCategories(filteredCategories);
                }

                // Log RecyclerView state
                Log.d(TAG, "RecyclerView adapter item count: " +
                        (searchRecyclerView.getAdapter() != null ? searchRecyclerView.getAdapter().getItemCount() : "null"));
                Log.d(TAG, "RecyclerView visibility: " +
                        (searchRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));

                // Ensure content is visible
                if (contentView != null && contentView.getVisibility() != View.VISIBLE) {
                    contentView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Made content view visible");
                }

                if (loadingLayout != null && loadingLayout.getVisibility() == View.VISIBLE) {
                    loadingLayout.setVisibility(View.GONE);
                    Log.d(TAG, "Hid loading layout");
                }

                // Force RecyclerView to refresh
                searchRecyclerView.invalidate();
                searchRecyclerView.requestLayout();
            });
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && searchEditText != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        }
    }

    private void loadCategories() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showLoading(false);
            return;
        }

        showLoading(true);

        DbQuery.loadCategories(requireContext(), new MyCompleteListener() {
            @Override
            public void onSucces() {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not attached, skipping category load.");
                    return;
                }

                allCategories.clear();
                filteredCategories.clear();

                if (DbQuery.g_catList != null) {
                    allCategories.addAll(DbQuery.g_catList);
                    filteredCategories.addAll(allCategories);

                    Log.d(TAG, "Loaded " + allCategories.size() + " categories from database");
                    for (CategoryModel cat : allCategories) {
                        Log.d(TAG, "Category: " + cat.getName() + " with " +
                                (cat.getQuizzList() != null ? cat.getQuizzList().size() : 0) + " quizzes");
                    }

                    setupCategoryRecyclerView();
                } else {
                    Log.e(TAG, "Category list is null");
                }

                categoriesLoaded = true;
                showLoading(false);
            }

            @Override
            public void onFailure() {
                Log.e(TAG, "Failed to load categories");
                categoriesLoaded = true;
                showLoading(false);
            }
        });
    }

    private void setupCategoryRecyclerView() {
        if (!isAdded()) {
            Log.e(TAG, "Fragment is not attached, cannot set up category recycler view");
            return;
        }

        Log.d(TAG, "Setting up RecyclerView with " + filteredCategories.size() + " categories");
        categoryAdapter = new CategoryAdapter(filteredCategories, requireContext(), this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        searchRecyclerView.setLayoutManager(layoutManager);
        searchRecyclerView.setAdapter(categoryAdapter);

        // Make sure the RecyclerView is visible
        if (contentView != null) {
            contentView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (loadingLayout != null && contentView != null) {
            if (show) {
                loadingLayout.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
            } else {
                loadingLayout.setVisibility(View.GONE);
                contentView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onQuizClick(String categoryId, String testId) {
        Context context = getContext();
        if (context != null) {
            Intent intent = new Intent(context, Questions.class);
            intent.putExtra("categoryId", categoryId);
            intent.putExtra("testId", testId);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up handler callbacks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        binding = null;
    }

    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);

        String userPhoto = sharedPreferences.getString("userPhoto", "");

        if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")){
            Glide.with(this).load(R.raw.guest_civil).into(searchImage);
        }else {
            Glide.with(this).load(userPhoto).into(searchImage);
        }
    }
}