package com.learn.simplify.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.learn.simplify.R;
import com.learn.simplify.database.NotesDatabase;
import com.learn.simplify.entities.Note;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNote extends BottomSheetDialogFragment {

    public interface NoteSavedListener {
        void onNoteSaved(boolean isNoteDeleted);
    }

    private NoteSavedListener noteSavedListener;

    public void setNoteSavedListener(NoteSavedListener listener) {
        this.noteSavedListener = listener;
    }
    BottomSheetDialog dialog;
    BottomSheetBehavior<View> bottomSheetBehavior;
    View rootView;

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private RoundedImageView imageNote;
    private ImageView imageRemove;
    private ImageView buttonDel;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;
    private TextView textDateTime;
    private String selectedNoteColor;
    private String selectedImagePath;
    private View viewSubtitleIndicator;
    private View viewTitleIndicator;
    private Note alreadyAvaibleNote;
    private CoordinatorLayout layout;
    private RelativeLayout del_note;
    private AlertDialog dialogDeleteNote;
    private Button  buttonAdd;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        return dialog;
    }

    public interface NoteDeletedListener {
        void onNoteDeleted();
    }

    private NoteDeletedListener noteDeletedListener;

    public void setNoteDeletedListener(NoteDeletedListener listener) {
        this.noteDeletedListener = listener;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_create_note, container, false);
        return rootView;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Reset the state of the dialog here
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); // Retain the instance across configuration changes
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);


        //IDS
        viewSubtitleIndicator = dialog.findViewById(R.id.viewSubtitleIndicator);
        viewTitleIndicator = dialog.findViewById(R.id.viewTitleIndicator);
        inputNoteTitle = dialog.findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = dialog.findViewById(R.id.inputNoteSubtitle);
        inputNoteText = dialog.findViewById(R.id.inputNote);
        textDateTime = dialog.findViewById(R.id.textDateTime);
        imageNote = dialog.findViewById(R.id.imageNote);
        imageRemove = dialog.findViewById(R.id.imageRemoveImage);
        buttonDel = dialog.findViewById(R.id.icon_delete);
        del_note = dialog.findViewById(R.id.del_note);
        selectedNoteColor = "#2C2C2C";
        selectedImagePath = "";

        buttonAdd = dialog.findViewById(R.id.buttonAdd);
        assert buttonAdd != null;
        buttonAdd.setText("Add");
        buttonAdd.setOnClickListener(v -> saveNote());


        Bundle args = getArguments();
        if (args != null) {
            boolean isViewOrUpdate = args.getBoolean("isViewOrUpdate");
            if (isViewOrUpdate) {
                alreadyAvaibleNote = (Note) args.getSerializable("note");
                buttonAdd.setText("Edit");
                setViewUpdateNote();
            }}

        if (textDateTime != null) {
            textDateTime.setText(
                    new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale.getDefault())
                            .format(new Date())
            );}

        //Bottom Size
        layout = dialog.findViewById(R.id.bottomNote);
        assert layout != null;
        layout.setMinimumHeight((int) ((Resources.getSystem().getDisplayMetrics().heightPixels)*0.3));
        bottomSheetBehavior.setDraggable(false);
        bottomSheetBehavior.setMaxHeight((int) ((Resources.getSystem().getDisplayMetrics().heightPixels)*0.7));

        //INIT
        initMiscellaneous();
        setIndicatorColor();

        //FUNC
        ImageView imageBack = dialog.findViewById(R.id.imageBack);
        assert imageBack != null;
        imageBack.setOnClickListener(v -> dismiss());

        imageRemove.setOnClickListener(v ->{
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            imageRemove.setVisibility(View.GONE);
            selectedImagePath="";
        });


    }


    private void setViewUpdateNote(){
        if (alreadyAvaibleNote != null) {
            inputNoteSubtitle.setText(alreadyAvaibleNote.getSubtitle());
            inputNoteTitle.setText(alreadyAvaibleNote.getTitle());
            inputNoteText.setText(alreadyAvaibleNote.getNoteText());
            textDateTime.setText(alreadyAvaibleNote.getDateTime());

            if(alreadyAvaibleNote.getImagePath() != null && !alreadyAvaibleNote.getImagePath().trim().isEmpty()){
                imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvaibleNote.getImagePath()));
                imageNote.setVisibility(View.VISIBLE);
                imageRemove.setVisibility(View.VISIBLE);
                selectedImagePath = alreadyAvaibleNote.getImagePath();
            }

            selectedNoteColor = alreadyAvaibleNote.getColor();
            setIndicatorColor();}
    }

    private void saveNote() {
        if (inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireActivity(), "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        } else if (inputNoteSubtitle.getText().toString().trim().isEmpty() && inputNoteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireActivity(), "Note can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();

        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if(alreadyAvaibleNote != null){
            note.setId(alreadyAvaibleNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {

            boolean isNoteDeleted = false;

            @Override
            protected Void doInBackground(Void... voids) {
                Context context = requireContext();
                if(alreadyAvaibleNote != null){
                    // If alreadyAvailableNote is not null, it means the note is being updated
                    NotesDatabase.getDatabase(context).noteDao().updateNote(note);
                } else {
                    // If alreadyAvailableNote is null, it means a new note is being added
                    NotesDatabase.getDatabase(context).noteDao().insertNote(note);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                dialog.dismiss();
                if (noteSavedListener != null) {
                    noteSavedListener.onNoteSaved(isNoteDeleted);
                }
                if (noteDeletedListener != null && isNoteDeleted) {
                    noteDeletedListener.onNoteDeleted();
                }
                Home activity = (Home) getActivity();
                if (activity != null) {
                    activity.showAdd();
                }
            }
        }

        new SaveNoteTask().execute();
    }

    public void initMiscellaneous() {
        final ImageView imageColor1 = dialog.findViewById(R.id.image1);
        final ImageView imageColor2 = dialog.findViewById(R.id.image2);
        final ImageView imageColor3 = dialog.findViewById(R.id.image3);
        final ImageView imageColor4 = dialog.findViewById(R.id.image4);

        dialog.findViewById(R.id.color1).setOnClickListener(v -> {
            selectedNoteColor = "#2C2C2C";
            assert imageColor1 != null;
            imageColor1.setImageResource(R.drawable.home_ic_check);
            assert imageColor2 != null;
            imageColor2.setImageResource(0);
            assert imageColor3 != null;
            imageColor3.setImageResource(0);
            assert imageColor4 != null;
            imageColor4.setImageResource(0);
            setIndicatorColor();
        });
        dialog.findViewById(R.id.color2).setOnClickListener(v -> {
            selectedNoteColor = "#4B917D";
            assert imageColor1 != null;
            imageColor1.setImageResource(0);
            assert imageColor2 != null;
            imageColor2.setImageResource(R.drawable.home_ic_check);
            assert imageColor3 != null;
            imageColor3.setImageResource(0);
            assert imageColor4 != null;
            imageColor4.setImageResource(0);
            setIndicatorColor();
        });
        dialog.findViewById(R.id.color3).setOnClickListener(v -> {
            selectedNoteColor = "#1DB954";
            assert imageColor1 != null;
            imageColor1.setImageResource(0);
            assert imageColor2 != null;
            imageColor2.setImageResource(0);
            assert imageColor3 != null;
            imageColor3.setImageResource(R.drawable.home_ic_check);
            assert imageColor4 != null;
            imageColor4.setImageResource(0);
            setIndicatorColor();
        });
        dialog.findViewById(R.id.color4).setOnClickListener(v -> {
            selectedNoteColor = "#CDF564";
            assert imageColor1 != null;
            imageColor1.setImageResource(0);
            assert imageColor2 != null;
            imageColor2.setImageResource(0);
            assert imageColor3 != null;
            imageColor3.setImageResource(0);
            assert imageColor4 != null;
            imageColor4.setImageResource(R.drawable.home_ic_check);
            setIndicatorColor();
        });

        if(alreadyAvaibleNote != null && alreadyAvaibleNote.getColor() != null && !alreadyAvaibleNote.getColor().trim().isEmpty()){
            switch (alreadyAvaibleNote.getColor()){
                case "#4B917D":
                    dialog.findViewById(R.id.color2).performClick();
                    break;
                case "#1DB954":
                    dialog.findViewById(R.id.color3).performClick();
                    break;
                case "#CDF564":
                    dialog.findViewById(R.id.color4).performClick();
                    break;
            }
        }

        dialog.findViewById(R.id.icon_image).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                            requireActivity(), // Pass the activity reference
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION
                    );
                }

            }else {
                selectImage();
            }

        });

        if(alreadyAvaibleNote != null){
            del_note.setAlpha(1f);
            del_note.setClickable(true);
            del_note.setOnClickListener(v -> {
                showDeleteNoteDialog();
               /*Home activity = (Home) getActivity();
               if (activity != null) {
                   activity.showAdd();
               }

                */
            });
        } else {
            del_note.setAlpha(0.2f);
            del_note.setClickable(false);
        }


    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            View view = LayoutInflater.from(requireActivity()).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) requireActivity().findViewById(R.id.layoutDeleteNoteContainer)
            );

            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.ok_btn_del).setOnClickListener(v -> {
                @SuppressLint("StaticFieldLeak")
                class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        NotesDatabase.getDatabase(requireActivity().getApplicationContext())
                                .noteDao()
                                .deleteNote(alreadyAvaibleNote);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        super.onPostExecute(unused);
                        dialogDeleteNote.dismiss();
                        if (noteSavedListener != null) {
                            noteSavedListener.onNoteSaved(true);
                        }
                        dismiss();
                    }
                }

                new DeleteNoteTask().execute();
            });

            view.findViewById(R.id.cancelDel).setOnClickListener(v -> dialogDeleteNote.dismiss());
        }

        dialogDeleteNote.show();
    }

    private void setIndicatorColor(){
        if (viewSubtitleIndicator != null && selectedNoteColor != null) {
            int color = Color.parseColor(selectedNoteColor);
            ViewCompat.setBackgroundTintList(viewSubtitleIndicator, ColorStateList.valueOf(color));
            ViewCompat.setBackgroundTintList(viewTitleIndicator, ColorStateList.valueOf(color));
        }
    }

    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == REQUEST_CODE_SELECT_IMAGE && data != null){
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null){
                    try {
                        InputStream inputStream = requireActivity().getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        imageRemove.setVisibility(View.VISIBLE);
                        selectedImagePath = getPathFromUri(selectedImageUri);
                    } catch (Exception exception){
                        Toast.makeText(requireActivity(), exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri){
        String filePath;
        Cursor cursor = requireActivity().getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null){
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }
}
