package com.learn.simplify.adapters;// TaskAdapter.java

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.learn.simplify.R;
import com.learn.simplify.database.DatabaseClient;
import com.learn.simplify.model.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd MMM yyyy", Locale.US);
    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd-M-yyyy", Locale.US);
    private final TaskEditListener editListener;

    public TaskAdapter(List<Task> taskList, TaskEditListener editListener, DeleteTaskListener deleteTaskListener) {
        this.taskList = taskList;
        this.editListener = editListener;
    }

    public interface DeleteTaskListener {
        void onTaskDeleted(int taskId);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView day;
        TextView date;
        TextView month;
        TextView title;
        TextView description;
        TextView time;
        CheckBox checkBoxGlow;
        View glowedView;
        ImageButton imageButton;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.day);
            date = itemView.findViewById(R.id.date);
            month = itemView.findViewById(R.id.month);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            time = itemView.findViewById(R.id.time);
            checkBoxGlow = itemView.findViewById(R.id.checked_task);
            glowedView = itemView.findViewById(R.id.view_glowed);
            imageButton = itemView.findViewById(R.id.delete_task);

            imageButton.setOnClickListener(v -> deleteTask(getAdapterPosition()));
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
                title.setText(task.getTaskTitle());
                description.setText(task.getTaskDescription());
                time.setText(task.getLastAlarm());

                // Set checkbox state
                checkBoxGlow.setOnCheckedChangeListener(null);
                checkBoxGlow.setChecked(task.isComplete());

                // Update view color based on initial checkbox state
                if (task.isComplete()) {
                    glowedView.setBackgroundResource(R.drawable.glow_effect_green);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        glowedView.setOutlineSpotShadowColor(R.color.green);
                        title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    }
                } else {
                    glowedView.setBackgroundResource(R.drawable.glow_effect_red);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        glowedView.setOutlineSpotShadowColor(R.color.red);
                        title.setPaintFlags(0);
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
                            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        }
                    } else {
                        glowedView.setBackgroundResource(R.drawable.glow_effect_red);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            glowedView.setOutlineSpotShadowColor(R.color.red);
                            title.setPaintFlags(0);
                        }
                    }
                });

                try {
                    Date date = inputDateFormat.parse(task.getDate());
                    String outputDateString = dateFormat.format(date);
                    String[] items1 = outputDateString.split(" ");
                    String day = items1[0];
                    String dd = items1[1];
                    String month = items1[2];

                    this.day.setText(day);
                    this.date.setText(dd);
                    this.month.setText(month);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                itemView.setOnClickListener(v -> {
                    // Open the edit dialog when a task is clicked
                    if (editListener != null) {
                        editListener.onTaskEdit(task);
                    }
                });
            }
        }

        public void deleteTask(int position) {
            if (position >= 0 && position < taskList.size()) {
                Task taskToDelete = taskList.get(position);
                taskList.remove(position);
                notifyItemRemoved(position);
                new DeleteTaskAsyncTask().execute(taskToDelete);
            }
        }

        private class DeleteTaskAsyncTask extends AsyncTask<Task, Void, Void> {
            @Override
            protected Void doInBackground(Task... tasks) {
                Task taskToDelete = tasks[0];
                DatabaseClient.getInstance(itemView.getContext())
                        .getAppDatabase()
                        .dataBaseAction()
                        .deleteTaskFromId(taskToDelete.getTaskId());
                return null;
            }
        }
    }

    public interface TaskEditListener {
        void onTaskEdit(Task task);
    }
}
