package com.cadetia.simplicadet.ui.military;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.PdfViewer;
import com.cadetia.simplicadet.adapters.DocumentsAdapter;
import com.cadetia.simplicadet.entities.Document;
import com.cadetia.simplicadet.listeners.DocumentListener;
import com.cadetia.simplicadet.utils.NetworkUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MilitaryFragment2 extends Fragment implements DocumentListener {

    private static final String TAG = "MilitaryFragment2";
    private static final long CACHE_DURATION = 30000;
    private View loadingLayout;
    private View contentView;
    private RecyclerView documentsRecyclerView;
    private List<Document> documentList;
    private DocumentsAdapter documentsAdapter;
    private List<Document> cachedDocuments;
    private long documentsLastLoad = 0;
    private boolean isLoadingDismissed = false;
    private boolean documentsLoaded = false;
    private FirebaseFirestore db;
    private ExecutorService executorService;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LruCache<String, Bitmap> memCache;
    private String userInstitution;

    public MilitaryFragment2() {
    }

    public static MilitaryFragment2 newInstance() {
        return new MilitaryFragment2();
    }

    private ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newCachedThreadPool();
        }
        return executorService;
    }

    private boolean isFragmentSafe() {
        return isAdded() && !isDetached() && getContext() != null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setupCache();
        retrieveUserData();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_military2, container, false);

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout2);
        documentsRecyclerView = view.findViewById(R.id.notesRecyclerView);

        setupRecyclerView();
        resetLoadingState();
        showLoading(true);

        return view;
    }

    private void setupCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        memCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    private void setupRecyclerView() {
        documentsRecyclerView.setHasFixedSize(true);
        documentsRecyclerView.setItemViewCacheSize(10);
        documentsRecyclerView.setDrawingCacheEnabled(true);
        documentsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        documentsRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        documentList = new ArrayList<>();
        documentsAdapter = new DocumentsAdapter(documentList, this);
        documentsRecyclerView.setAdapter(documentsAdapter);
    }

    private void resetLoadingState() {
        isLoadingDismissed = false;
        documentsLoaded = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        long currentTime = System.currentTimeMillis();

        if (userInstitution == null) retrieveUserData();

        resetLoadingState();
        showLoading(true);

        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            if (cachedDocuments != null && (currentTime - documentsLastLoad) < CACHE_DURATION) {
                documentList.clear();
                documentList.addAll(cachedDocuments);
                if (documentsAdapter != null) {
                    documentsAdapter.notifyDataSetChanged();
                }
                documentsLoaded = true;
                checkAllDataLoaded();
            } else {
                loadDocumentsFromFirestore();
            }
        } else {
            if (cachedDocuments != null) {
                documentList.clear();
                documentList.addAll(cachedDocuments);
                if (documentsAdapter != null) {
                    documentsAdapter.notifyDataSetChanged();
                }
            }
            documentsLoaded = true;
            checkAllDataLoaded();
        }
    }

    @Override
    public void onDestroyView() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        loadingLayout = null;
        contentView = null;
        documentsRecyclerView = null;
        documentsAdapter = null;

        super.onDestroyView();
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
                if (context != null && isAdded()) {
                    loadingLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
                    handler.postDelayed(() -> {
                        if (loadingLayout != null && contentView != null && isAdded()) {
                            loadingLayout.setVisibility(View.GONE);
                            contentView.setVisibility(View.VISIBLE);
                            contentView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
                        }
                    }, 250);
                } else {
                    if (loadingLayout != null && contentView != null) {
                        loadingLayout.setVisibility(View.GONE);
                        contentView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    private void loadDocumentsFromFirestore() {
        if (userInstitution == null) return;
        getExecutorService().execute(() -> {
            if (!isFragmentSafe()) return;

            db.collection("MILITARY")
                    .document("RO")
                    .collection(userInstitution)
                    .document("DOCUMENTS")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!isFragmentSafe()) return;

                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            List<Document> documents = new ArrayList<>();

                            for (int fieldIndex = 0; fieldIndex <= 2; fieldIndex++) {
                                ArrayList<String> docArray = (ArrayList<String>) task.getResult()
                                        .get(String.valueOf(fieldIndex));

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

                                    Document document = new Document(
                                            docId,
                                            title,
                                            subtitle,
                                            imageUrl,
                                            pdfUrl,
                                            date,
                                            fieldIndex
                                    );

                                    Log.d(TAG, "Document loaded: Title=" + title +
                                            ", Subtitle=" + subtitle +
                                            ", Image=" + imageUrl +
                                            ", PDF=" + pdfUrl +
                                            ", Date=" + date);

                                    documents.add(document);
                                } else {
                                    Log.e(TAG, "Array at index " + fieldIndex + " is null or incomplete");
                                }
                            }

                            cachedDocuments = new ArrayList<>(documents);
                            documentsLastLoad = System.currentTimeMillis();

                            handler.post(() -> {
                                if (!isFragmentSafe()) return;

                                documentList.clear();
                                documentList.addAll(documents);
                                if (documentsAdapter != null) {
                                    documentsAdapter.notifyDataSetChanged();
                                }
                                Log.d(TAG, "Documents loaded: " + documents.size());

                                documentsLoaded = true;
                                checkAllDataLoaded();
                            });
                        } else {
                            Log.e(TAG, "Error getting DOCUMENTS document: ",
                                    task.getException() != null ? task.getException() :
                                            new Exception("Document does not exist"));
                            handler.post(() -> {
                                documentsLoaded = true;
                                checkAllDataLoaded();
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isFragmentSafe()) {
                            Log.e(TAG, "Failed to load documents from Firestore", e);
                            handler.post(() -> {
                                documentsLoaded = true;
                                checkAllDataLoaded();
                            });
                        }
                    });
        });
    }

    private void checkAllDataLoaded() {
        if (documentsLoaded) {
            showLoading(false);
        }
    }

    @Override
    public void onDocumentClicked(Document document, int position) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            return;
        }

        if (document.getPdfUrl() != null && !document.getPdfUrl().isEmpty()) {
            try {
                Intent intent = new Intent(getActivity(), PdfViewer.class);
                intent.putExtra("pdfUrl", document.getPdfUrl());
                intent.putExtra("pdfTitle", document.getTitle());
                intent.putExtra("pdfSubtitle", document.getSubtitle());
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening PDF viewer", e);
            }
        } else {
            Log.e(TAG, "PDF URL is null or empty for document: " + document.getTitle());
        }
    }

    private void retrieveUserData() {
        if (getActivity() != null) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);
            userInstitution = sharedPreferences.getString("userInstitution", "");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}