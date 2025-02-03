package com.learn.simplify.adapters;// MainTaskAdapter.java

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.learn.simplify.R;
import com.learn.simplify.database.DatabaseClient;
import com.learn.simplify.model.Task;

import java.util.List;

public class MainTaskAdapter extends RecyclerView.Adapter<MainTaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;

    public MainTaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_main, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return Math.min(taskList.size(), 3);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView main_title;
        CheckBox checkBoxGlow;
        View glowedView;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            main_title = itemView.findViewById(R.id.main_title);
            checkBoxGlow = itemView.findViewById(R.id.main_checked_task);
            glowedView = itemView.findViewById(R.id.main_view_glowed);
        }

        private class UpdateTaskAsyncTask extends AsyncTask<Task, Void, Void> {
            @Override
            protected Void doInBackground(Task... tasks) {
                Task taskToUpdate = tasks[0];
                DatabaseClient.getInstance(itemView.getContext())
                        .getAppDatabase()
                        .dataBaseAction()
                        .updateTask(taskToUpdate);
                return null;
            }
        }

        @SuppressLint("ResourceAsColor")
        void bind(Task task) {
            if (task != null) {
                main_title.setText(task.getTaskTitle());

                // Set checkbox state
                checkBoxGlow.setChecked(task.isComplete());

                // Update view color based on initial checkbox state
                if (task.isComplete()) {
                    glowedView.setBackgroundResource(R.drawable.glow_effect_green);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        glowedView.setOutlineSpotShadowColor(R.color.green);
                        main_title.setPaintFlags(main_title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    }
                } else {
                    glowedView.setBackgroundResource(R.drawable.glow_effect_red);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        glowedView.setOutlineSpotShadowColor(R.color.red);
                        main_title.setPaintFlags(0);
                    }
                }

                checkBoxGlow.setOnCheckedChangeListener(null); // Remove previous listener to avoid infinite loop

                checkBoxGlow.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // Update database with checkbox state
                    task.setCompleted(isChecked);
                    new UpdateTaskAsyncTask().execute(task);

                    // Update view color based on checkbox state
                    if (isChecked) {
                        glowedView.setBackgroundResource(R.drawable.glow_effect_green);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            glowedView.setOutlineSpotShadowColor(R.color.green);
                            main_title.setPaintFlags(main_title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        }
                    } else {
                        glowedView.setBackgroundResource(R.drawable.glow_effect_red);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            glowedView.setOutlineSpotShadowColor(R.color.red);
                            main_title.setPaintFlags(0);
                        }
                    }
                });

            }
        }
    }
}
