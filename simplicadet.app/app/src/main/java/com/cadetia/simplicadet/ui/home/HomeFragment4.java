package com.cadetia.simplicadet.ui.home;

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

import com.cadetia.simplicadet.activities.Home;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.CreateTaskBottom;
import com.cadetia.simplicadet.adapters.TaskAdapter;
import com.cadetia.simplicadet.database.DatabaseClient;
import com.cadetia.simplicadet.model.Task;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment4 extends Fragment implements TaskAdapter.DeleteTaskListener, TaskAdapter.TaskEditListener, CreateTaskBottom.TaskSavedListener {
    RecyclerView tasksRecycler;
    FloatingActionButton addTask;
    ImageView noDataImage;
    TaskAdapter taskAdapter;
    List<Task> tasks = new ArrayList<>();
    private Task placeholderTask;

    public HomeFragment4() {
    }

    public static HomeFragment4 newInstance() {
        return new HomeFragment4();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home4, container, false);
        tasksRecycler = view.findViewById(R.id.tasksRecyclerView);
        setUpAdapter();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSavedTasks();
        return view;
    }

    private void showEditTask(Task task) {
        if (task == placeholderTask) return;
        CreateTaskBottom createTaskBottom = new CreateTaskBottom();
        createTaskBottom.setEditMode(true);
        createTaskBottom.setTaskToEdit(task);
        createTaskBottom.setTaskId(task.getTaskId(), true);
        createTaskBottom.setTaskSavedListener(this);
        createTaskBottom.show(getChildFragmentManager(), createTaskBottom.getTag());
    }

    public void showCreateTask() {
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
                if (fetchedTasks.isEmpty()) {
                    placeholderTask = new Task();
                    placeholderTask.setTaskTitle("Start by clicking on ➕");
                    placeholderTask.setDate("28-7-2025");
                    placeholderTask.setLastAlarm("12:00");
                    placeholderTask.setTaskId(-1);
                    tasks.add(placeholderTask);
                } else {
                    placeholderTask = null;
                    tasks.addAll(fetchedTasks);
                }
                taskAdapter.notifyDataSetChanged();
            }
        }
        GetSavedTasks savedTasks = new GetSavedTasks();
        savedTasks.execute();
    }

    @Override
    public void onTaskSaved() {
        getSavedTasks();
    }

    @Override
    public void onTaskEdit(Task task) {
        showEditTask(task);
    }

    @Override
    public void onTaskDeleted(int taskId) {
        DatabaseClient.getInstance(requireContext())
                .getAppDatabase()
                .dataBaseAction()
                .deleteTaskFromId(taskId);
    }
}