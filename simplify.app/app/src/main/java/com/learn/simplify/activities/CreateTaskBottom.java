package com.learn.simplify.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.learn.simplify.R;
import com.learn.simplify.ads.InterstitialAdd;
import com.learn.simplify.database.DatabaseClient;
import com.learn.simplify.model.Task;

import java.util.Calendar;

public class CreateTaskBottom extends BottomSheetDialogFragment {

    EditText addTaskTitle;
    EditText addTaskDescription;
    EditText taskDate;
    EditText taskTime;
    Button addTask;
    ImageView backImage;
    ImageView iconDelete;
    RelativeLayout backgroundDelete;
    int taskId;
    boolean isEdit;
    private InterstitialAdd interstitialAdd;
    Task task;
    int mYear, mMonth, mDay;
    int mHour, mMinute;
    TimePickerDialog timePickerDialog;
    DatePickerDialog datePickerDialog;
    private boolean isEditMode = false;
    private Task taskToEdit;

    private TaskSavedListener taskSavedListener;

    public void setTaskSavedListener(TaskSavedListener taskSavedListener) {
        this.taskSavedListener = taskSavedListener;
    }

    public void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
    }

    public void setTaskToEdit(Task taskToEdit) {
        this.taskToEdit = taskToEdit;
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_create_task, container, false);

        // Initialize EditText and ImageView objects
        addTaskTitle = contentView.findViewById(R.id.promptText);
        addTaskDescription = contentView.findViewById(R.id.inputTaskDescription);
        taskDate = contentView.findViewById(R.id.taskDate);
        taskTime = contentView.findViewById(R.id.taskTime);
        iconDelete = contentView.findViewById(R.id.icon_delete);
        backgroundDelete = contentView.findViewById(R.id.background_delete);
        addTask = contentView.findViewById(R.id.questionAsk);
        backImage = contentView.findViewById(R.id.questionClose);

        interstitialAdd = new InterstitialAdd(); // Initialize the interstitial ad
        interstitialAdd.loadInterstitialAd(requireActivity()); // Load the ad

        backImage.setOnClickListener(v -> dismiss());

        addTask.setOnClickListener(view -> {
            if (validateFields()) {
                createOrUpdateTask();
            }
        });

        iconDelete.setOnClickListener(v -> {
            if (task != null) {
                deleteTask(task.getTaskId());
            } else {
                // Handle the case where task is null
                Toast.makeText(getContext(), "Task is null", Toast.LENGTH_SHORT).show();
            }
        });

        // Set initial button text and delete icon alpha
        if (isEditMode && taskToEdit != null) {
            populateFields(taskToEdit);
            backgroundDelete.setAlpha(1f); // Set alpha to 1 when in edit mode
            iconDelete.setClickable(true);
            addTask.setText("Edit");
        } else {
            backgroundDelete.setAlpha(0.2f); // Set alpha to 0.2 when not in edit mode
            iconDelete.setClickable(false);
            addTask.setText("Add");
        }

        // Set text change listener for title field
        addTaskTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Change button text to "Edit" if fields are not empty and in edit mode
                if (!addTaskTitle.getText().toString().isEmpty() &&
                        !addTaskDescription.getText().toString().isEmpty() &&
                        isEditMode) {
                    addTask.setText("Edit");
                }
            }
        });

        // Set text change listener for description field
        addTaskDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Change button text to "Edit" if fields are not empty and in edit mode
                if (!addTaskTitle.getText().toString().isEmpty() &&
                        !addTaskDescription.getText().toString().isEmpty() &&
                        isEditMode) {
                    addTask.setText("Edit");
                }
            }
        });

        // Intercept touch events for date and time fields
        taskDate.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                showDatePicker();
            }
            return true;
        });

        taskTime.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                showTimePicker();
            }
            return true;
        });

        return contentView;
    }

    public void setTaskId(int taskId, boolean isEdit) {
        this.taskId = taskId;
        this.isEdit = isEdit;
    }

    public interface TaskSavedListener {
        void onTaskSaved();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.fragment_create_task, null);

        // Initialize views
        addTask = contentView.findViewById(R.id.questionAsk);
        taskTime = contentView.findViewById(R.id.taskTime);
        taskDate = contentView.findViewById(R.id.taskDate);
        addTaskDescription = contentView.findViewById(R.id.inputTaskDescription);
        addTaskTitle = contentView.findViewById(R.id.promptText);
        iconDelete = contentView.findViewById(R.id.icon_delete);

        dialog.setContentView(contentView);

        if (isEdit) {
            showTaskFromId();
        }

        taskDate.setOnClickListener(v -> showDatePicker());
        taskTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        datePickerDialog = new DatePickerDialog(getActivity(), R.style.DatePickerDark, (view, year, monthOfYear, dayOfMonth) -> taskDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year), mYear, mMonth, mDay);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
    }

    private void deleteTask(int taskId) {
        class DeleteTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .dataBaseAction().deleteTaskFromId(taskId);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // Notify the listener that a task has been deleted
                if (taskSavedListener != null) {
                    taskSavedListener.onTaskSaved();
                }
                dismiss(); // Dismiss the bottom sheet after deletion
            }
        }

        DeleteTask deleteTask = new DeleteTask();
        deleteTask.execute();
    }


    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        timePickerDialog = new TimePickerDialog(getActivity(), R.style.TimePickerDark, (view, hourOfDay, minute) -> {
            taskTime.setText(hourOfDay + ":" + minute);
        }, mHour, mMinute, false);
        timePickerDialog.show();
    }

    public boolean validateFields() {
        if (addTaskTitle.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please enter a valid title", Toast.LENGTH_SHORT).show();
            return false;
        } else if (addTaskDescription.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please enter a valid description", Toast.LENGTH_SHORT).show();
            return false;
        } else if (taskDate.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please enter date", Toast.LENGTH_SHORT).show();
            return false;
        } else if (taskTime.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Please enter time", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void populateFields(Task task) {
        // Populate UI fields with task details
        addTaskTitle.setText(task.getTaskTitle());
        addTaskDescription.setText(task.getTaskDescription());
        taskDate.setText(task.getDate());
        taskTime.setText(task.getLastAlarm());
    }

    private void createOrUpdateTask() {
        class SaveTaskInBackend extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                // If in edit mode, update the existing task
                if (isEditMode && taskToEdit != null) {
                    taskToEdit.setTaskTitle(addTaskTitle.getText().toString());
                    taskToEdit.setTaskDescription(addTaskDescription.getText().toString());
                    taskToEdit.setDate(taskDate.getText().toString());
                    taskToEdit.setLastAlarm(taskTime.getText().toString());

                    // Update the task in the database
                    DatabaseClient.getInstance(getActivity()).getAppDatabase()
                            .dataBaseAction()
                            .updateTask(taskToEdit);

                } else {
                    // Otherwise, create a new task
                    Task newTask = new Task();
                    newTask.setTaskTitle(addTaskTitle.getText().toString());
                    newTask.setTaskDescription(addTaskDescription.getText().toString());
                    newTask.setDate(taskDate.getText().toString());
                    newTask.setLastAlarm(taskTime.getText().toString());

                    // Insert the new task into the database
                    DatabaseClient.getInstance(getActivity()).getAppDatabase()
                            .dataBaseAction()
                            .insertDataIntoTaskList(newTask);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                // Notify the listener that a task has been saved
                if (taskSavedListener != null) {
                    taskSavedListener.onTaskSaved();
                }

                dismiss();

                if (interstitialAdd != null) {
                    interstitialAdd.showInterstitialAd(requireActivity());
                }
            }
        }

        SaveTaskInBackend st = new SaveTaskInBackend();
        st.execute();
    }

    private void showTaskFromId() {
        class ShowTaskFromId extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                task = DatabaseClient.getInstance(getActivity()).getAppDatabase()
                        .dataBaseAction().selectDataFromAnId(taskId);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                setDataInUI();
            }
        }
        ShowTaskFromId st = new ShowTaskFromId();
        st.execute();
    }

    private void setDataInUI() {
        addTaskTitle.setText(task.getTaskTitle());
        addTaskDescription.setText(task.getTaskDescription());
        taskDate.setText(task.getDate());
        taskTime.setText(task.getLastAlarm());
    }
}
