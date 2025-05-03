package com.cadetia.simplicadet.ui.military;

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

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.PdfViewerActivity;
import com.cadetia.simplicadet.adapters.FirestoreDocumentsAdapter;
import com.cadetia.simplicadet.entities.FirestoreDocument;
import com.cadetia.simplicadet.listeners.DocumentListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MilitaryFragment2 extends Fragment implements DocumentListener {

    private static final String TAG = "MilitaryFragment2";
    private boolean isLoadingDismissed = false;
    private View loadingLayout;
    private View contentView;
    private List<FirestoreDocument> documentList;
    private FirestoreDocumentsAdapter documentsAdapter;
    private FirebaseFirestore db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public MilitaryFragment2() {
        // Required empty public constructor
    }

    public static MilitaryFragment2 newInstance() {
        return new MilitaryFragment2();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_military2, container, false);

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout2);

        showLoading(true);

        db = FirebaseFirestore.getInstance();

        RecyclerView documentsRecyclerView = view.findViewById(R.id.notesRecyclerView);
        documentsRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        documentList = new ArrayList<>();
        documentsAdapter = new FirestoreDocumentsAdapter(documentList, this);
        documentsRecyclerView.setAdapter(documentsAdapter);

        new Handler().postDelayed(this::loadDocumentsFromFirestore, 1000);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showLoading(true);
        new Handler().postDelayed(this::loadDocumentsFromFirestore, 1000);
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
                    Log.w(TAG, "Context is null, skipping animations");
                }
            }
        }
    }

    private void loadDocumentsFromFirestore() {
        executorService.execute(() -> {
            // Accesăm direct documentul DOCUMENTS
            db.collection("MILITARY")
                    .document("RO")
                    .collection("CNMTV")
                    .document("DOCUMENTS")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            List<FirestoreDocument> documents = new ArrayList<>();
                            for (int fieldIndex = 0; fieldIndex <= 2; fieldIndex++) {
                                ArrayList<String> docArray = (ArrayList<String>) task.getResult().get(String.valueOf(fieldIndex));

                                if (docArray != null && docArray.size() >= 4) {
                                    String title = docArray.get(0);
                                    String subtitle = docArray.get(1);
                                    String imageUrl = docArray.get(2);
                                    String pdfUrl = docArray.get(3);
                                    String date = "";
                                    if (docArray.size() >= 5) {
                                        date = docArray.get(4);
                                    }
                                    String docId = String.valueOf(fieldIndex);
                                    FirestoreDocument firestoreDocument = new FirestoreDocument(
                                            docId,
                                            title,
                                            subtitle,
                                            imageUrl,
                                            pdfUrl,
                                            date,
                                            fieldIndex
                                    );

                                    Log.d(TAG, "Document încărcat: Titlu=" + title +
                                            ", Subtitlu=" + subtitle +
                                            ", Imagine=" + imageUrl +
                                            ", PDF=" + pdfUrl +
                                            ", Data=" + date);

                                    documents.add(firestoreDocument);
                                } else {
                                    Log.e(TAG, "Array-ul de la indexul " + fieldIndex + " este null sau incomplet");
                                }
                            }

                            handler.post(() -> {
                                documentList.clear();
                                documentList.addAll(documents);
                                documentsAdapter.notifyDataSetChanged();
                                Log.d(TAG, "Documente încărcate: " + documents.size());

                                showLoading(false);
                            });
                        } else {
                            Log.e(TAG, "Eroare la obținerea documentului DOCUMENTS: ",
                                    task.getException() != null ? task.getException() : new Exception("Document inexistent"));
                            handler.post(() -> showLoading(false));
                        }
                    });
        });
    }

    @Override
    public void onDocumentClicked(FirestoreDocument document, int position) {
        if (document.getPdfUrl() != null && !document.getPdfUrl().isEmpty()) {
            Intent intent = new Intent(getActivity(), PdfViewerActivity.class);
            intent.putExtra("pdfUrl", document.getPdfUrl());
            intent.putExtra("pdfTitle", document.getTitle());
            intent.putExtra("pdfSubtitle", document.getSubtitle());
            startActivity(intent);
        } else {
            Log.e(TAG, "URL-ul PDF este null sau gol pentru documentul: " + document.getTitle());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}