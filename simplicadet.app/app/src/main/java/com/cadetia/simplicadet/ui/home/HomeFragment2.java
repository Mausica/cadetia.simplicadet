package com.cadetia.simplicadet.ui.home;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.cadetia.simplicadet.activities.Home;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.CreateNote;
import com.cadetia.simplicadet.adapters.NotesAdapter;
import com.cadetia.simplicadet.database.NotesDatabase;
import com.cadetia.simplicadet.entities.Note;
import com.cadetia.simplicadet.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment2 extends Fragment implements NotesListener, CreateNote.NoteSavedListener, CreateNote.NoteDeletedListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    private boolean isLoadingDismissed = false;
    private View loadingLayout;
    private View contentView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    public int noteClickedPosition = -1;
    private FloatingActionButton fabMain;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Cache for loaded notes to avoid unnecessary database calls
    private List<Note> cachedNotes;
    private long lastLoadTime = 0;
    private static final long CACHE_DURATION = 5000; // 5 seconds cache

    private final ActivityResultLauncher<Intent> createNoteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, false);
                }
            }
    );

    public HomeFragment2() {
        // Required empty public constructor
    }

    @Override
    public void onNoteDeleted() {
        // Invalidate cache and reload notes
        cachedNotes = null;
        loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, true);
    }

    public static HomeFragment2 newInstance() {
        return new HomeFragment2();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home2, container, false);

        //fabMain = view.findViewById(R.id.fabMain);
        // fabMain.setOnClickListener(v -> showCreateNote(null, false));

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout2);

        showLoading(true);

        RecyclerView notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // Enable recycler view optimizations
        notesRecyclerView.setHasFixedSize(true);
        notesRecyclerView.setItemViewCacheSize(20);
        notesRecyclerView.setDrawingCacheEnabled(true);
        notesRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        long currentTime = System.currentTimeMillis();
        if (cachedNotes != null && (currentTime - lastLoadTime) < CACHE_DURATION) {
            // Use cached data
            noteList.clear();
            noteList.addAll(cachedNotes);
            notesAdapter.notifyDataSetChanged();
            showLoading(false);
        } else {
            // Load fresh data
            showLoading(true);
            loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, false);
        }
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
                    loadingLayout.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        showCreateNote(note, false);
    }

    @Override
    public void onNoteSaved() {
        // Invalidate cache when note is saved
        cachedNotes = null;
    }

    public void loadNotesInBackground(final int requestCode, final boolean isNoteDeleted) {
        if (executorService.isShutdown()) {
            Log.w(TAG, "Executor is shutdown, skipping loadNotesInBackground");
            return;
        }

        executorService.execute(() -> {
            try {
                List<Note> notes = NotesDatabase.getDatabase(requireContext()).noteDao().getAllNotes();

                cachedNotes = new ArrayList<>(notes);
                lastLoadTime = System.currentTimeMillis();

                handler.post(() -> {
                    if (!isAdded()) return;

                    switch (requestCode) {
                        case REQUEST_CODE_SHOW_NOTES:
                        case REQUEST_CODE_ADD_NOTE:
                            noteList.clear();
                            noteList.addAll(notes);
                            notesAdapter.notifyDataSetChanged();
                            break;
                        case REQUEST_CODE_UPDATE_NOTE:
                            if (isNoteDeleted) {
                                if (noteClickedPosition >= 0 && noteClickedPosition < noteList.size()) {
                                    noteList.remove(noteClickedPosition);
                                    notesAdapter.notifyItemRemoved(noteClickedPosition);
                                }
                            } else {
                                if (noteClickedPosition >= 0 && noteClickedPosition < notes.size()) {
                                    noteList.set(noteClickedPosition, notes.get(noteClickedPosition));
                                    notesAdapter.notifyItemChanged(noteClickedPosition);
                                }
                            }
                            break;
                    }
                    showLoading(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading notes", e);
                handler.post(() -> {
                    if (isAdded()) {
                        showLoading(false);
                    }
                });
            }
        });
    }

    public void showCreateNote(Note note, boolean isNoteDeleted) {
        Bundle args = new Bundle();
        if (note == null) {
            args.putBoolean("isViewOrUpdate", false);
        } else {
            args.putBoolean("isViewOrUpdate", true);
            args.putSerializable("note", note);
        }
        args.putBoolean("isNoteDeleted", isNoteDeleted);

        CreateNote bottomSheetFragment = new CreateNote();
        bottomSheetFragment.setArguments(args);
        bottomSheetFragment.setNoteSavedListener(this);
        bottomSheetFragment.show(requireActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    @Override
    public void onNoteSaved(boolean isNoteDeleted) {
        // Invalidate cache and reload
        cachedNotes = null;
        loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, isNoteDeleted);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}