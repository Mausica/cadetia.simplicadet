package com.cadetia.simplicadet.ui.home;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment2 extends Fragment implements NotesListener, CreateNote.NoteSavedListener, CreateNote.NoteDeletedListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;

    private ShimmerFrameLayout shimmerFrameLayout;
    private View contentView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    public int noteClickedPosition = -1;
    private FloatingActionButton fabMain;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
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
        // Reload note list when a note is deleted
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

        // Initialize shimmer layout
        shimmerFrameLayout = view.findViewById(R.id.shimmer_layout_2);
        contentView = view.findViewById(R.id.contentLayout2);

        // Show shimmer initially
        showShimmer(true);

        RecyclerView notesRecyclerView = view.findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        // Load notes with delay to show shimmer effect
        new Handler().postDelayed(() -> {
            loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, false);
        }, 1000); // 1 second delay to show shimmer

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Show shimmer when the fragment is resumed
        showShimmer(true);

        // Load data with a slight delay to show the shimmer effect
        new Handler().postDelayed(() -> {
            loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, false);
        }, 1000); // 1 second delay to show shimmer
    }

    private void showShimmer(boolean show) {
        if (show) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.startShimmer();
            contentView.setVisibility(View.GONE);
        } else {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
            contentView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        showCreateNote(note, false);
    }

    @Override
    public void onNoteSaved() {

    }

    public void loadNotesInBackground(final int requestCode, final boolean isNoteDeleted) {
        executorService.execute(() -> {
            List<Note> notes = NotesDatabase.getDatabase(requireContext()).noteDao().getAllNotes();
            handler.post(() -> {
                switch (requestCode) {
                    case REQUEST_CODE_SHOW_NOTES:
                    case REQUEST_CODE_ADD_NOTE:
                        // Clear existing notes and add all notes
                        noteList.clear();
                        noteList.addAll(notes);
                        notesAdapter.notifyDataSetChanged();
                        break;
                    case REQUEST_CODE_UPDATE_NOTE:
                        if (isNoteDeleted) {
                            // Note is deleted, notify adapter about the removal
                            notesAdapter.notifyItemRemoved(noteClickedPosition);
                        } else {
                            // Note is updated, update the corresponding note in the list
                            if (noteClickedPosition >= 0 && noteClickedPosition < notes.size()) {
                                noteList.set(noteClickedPosition, notes.get(noteClickedPosition));
                                notesAdapter.notifyItemChanged(noteClickedPosition);
                            }
                        }
                        break;
                }

                // Hide shimmer after data is loaded
                showShimmer(false);

                // Fallback: hide shimmer after a timeout even if loading fails
                new Handler().postDelayed(() -> {
                    if (isAdded()) {
                        showShimmer(false);
                    }
                }, 3000); // 3 seconds max waiting time
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
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
        loadNotesInBackground(REQUEST_CODE_SHOW_NOTES, isNoteDeleted);
    }
}