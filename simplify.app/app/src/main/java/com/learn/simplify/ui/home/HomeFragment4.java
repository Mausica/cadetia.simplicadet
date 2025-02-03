package com.learn.simplify.ui.home;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.learn.simplify.R;
import com.learn.simplify.activities.CreateTaskBottom;
import com.learn.simplify.adapters.TaskAdapter;
import com.learn.simplify.ads.InterstitialAdd;
import com.learn.simplify.database.DatabaseClient;
import com.learn.simplify.model.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment4 extends Fragment implements TaskAdapter.DeleteTaskListener, TaskAdapter.TaskEditListener, CreateTaskBottom.TaskSavedListener {
    RecyclerView tasksRecycler;
    FloatingActionButton addTask;
    ImageView noDataImage;
    TaskAdapter taskAdapter;
    List<Task> tasks = new ArrayList<>();

    public HomeFragment4() {
        // Required empty public constructor
    }

    public static HomeFragment4 newInstance() {
        return new HomeFragment4();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home4, container, false);

        tasksRecycler = view.findViewById(R.id.tasksRecyclerView);
        addTask = view.findViewById(R.id.fabTask);
        addTask.setOnClickListener(v -> showCreateTask());

        setUpAdapter();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getSavedTasks();

        return view;
    }

    private void showEditTask(Task task) {
        CreateTaskBottom createTaskBottom = new CreateTaskBottom();

        // Pass the task details to the bottom sheet fragment
        createTaskBottom.setEditMode(true);
        createTaskBottom.setTaskToEdit(task);

        // Pass the taskId to the bottom sheet fragment for editing
        createTaskBottom.setTaskId(task.getTaskId(), true);

        createTaskBottom.setTaskSavedListener(this);
        createTaskBottom.show(getChildFragmentManager(), createTaskBottom.getTag());
    }

    private void showCreateTask() {
        CreateTaskBottom createTaskBottom = new CreateTaskBottom();
        createTaskBottom.setTaskSavedListener(this);
        createTaskBottom.show(getChildFragmentManager(), createTaskBottom.getTag());
    }

    public void setUpAdapter() {
        taskAdapter = new TaskAdapter(tasks, this, this);
        tasksRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        tasksRecycler.setAdapter(taskAdapter);
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
                taskAdapter.notifyDataSetChanged();
            }
        }

        GetSavedTasks savedTasks = new GetSavedTasks();
        savedTasks.execute();
    }

    @Override
    public void onTaskSaved() {
        // This method is called when a task is saved in CreateTaskBottom
        // You can refresh the task list here
        getSavedTasks();
    }

    @Override
    public void onTaskEdit(Task task) {
        // Open the edit dialog when a task is clicked
        showEditTask(task);
    }

    @Override
    public void onTaskDeleted(int taskId) {
        // Perform database deletion using Room or other methods
        // based on your implementation in HomeFragment4
        DatabaseClient.getInstance(requireContext())
                .getAppDatabase()
                .dataBaseAction()
                .deleteTaskFromId(taskId);
    }


}
