package com.learn.simplify.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.learn.simplify.R;
import com.learn.simplify.activities.CreateNote;
import com.learn.simplify.activities.QuestionsActivity;
import com.learn.simplify.activities.ShowRedeem;
import com.learn.simplify.adapters.CategoryAdapter;
import com.learn.simplify.adapters.MainTaskAdapter;
import com.learn.simplify.database.DatabaseClient;
import com.learn.simplify.database.DbQuery;
import com.learn.simplify.entities.Note;
import com.learn.simplify.listeners.MyCompleteListener;
import com.learn.simplify.model.CategoryModel;
import com.learn.simplify.model.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment1 extends Fragment implements CategoryAdapter.OnQuizClickListener {

    private static final String TAG = "HomeFragment1";
    private RecyclerView categoryRecyclerView;
    private RecyclerView mainTasksRecycler;
    private MainTaskAdapter mainTaskAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Task> tasks = new ArrayList<>();
    private Handler handler = new Handler();
    private String userEmail;

    public HomeFragment1() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home1, container, false);

        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        mainTasksRecycler = view.findViewById(R.id.tasksMainRecyclerView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTaskFirstLayout();
        loadCategories();
    }

    private void loadTaskFirstLayout() {
        setUpAdapter();
        getSavedTasks();
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
                mainTaskAdapter.notifyDataSetChanged();
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

    private void showRedeemDialog(int totalScore) {
        if (totalScore > 0) {
            ShowRedeem bottomSheetFragment = new ShowRedeem();
            Bundle bundle = new Bundle();
            bundle.putInt("totalScore", totalScore);
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
            handler.postDelayed(() -> showRedeemDialog(totalScore), 1000);

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
        userEmail= sharedPreferences.getString("userEmail", "");

    }

}

