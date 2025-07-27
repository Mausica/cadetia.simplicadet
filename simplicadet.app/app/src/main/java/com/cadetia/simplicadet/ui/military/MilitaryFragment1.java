package com.cadetia.simplicadet.ui.military;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.activities.Questions;
import com.cadetia.simplicadet.activities.ShowRedeem;
import com.cadetia.simplicadet.adapters.CategoryAdapter;
import com.cadetia.simplicadet.adapters.DestinationAdapter;
import com.cadetia.simplicadet.adapters.JournalAdapter;
import com.cadetia.simplicadet.adapters.RankAdapter;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.model.CategoryModel;
import com.cadetia.simplicadet.model.DestinationItem;
import com.cadetia.simplicadet.model.JournalEntry;
import com.cadetia.simplicadet.model.RankModel;
import com.cadetia.simplicadet.utils.NetworkUtils;
import com.cadetia.simplicadet.utils.SvgImageLoader;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MilitaryFragment1 extends Fragment implements CategoryAdapter.OnQuizClickListener, JournalAdapter.OnJournalClickListener, DestinationAdapter.OnDestinationClickListener {

    private static final String TAG = "MilitaryFragment1";
    private static final long CACHE_DURATION = 30000;

    private RecyclerView categoryRecyclerView;
    private RecyclerView rankRecyclerView;
    private RecyclerView journalRecyclerView;
    private RecyclerView destinationRecyclerView;
    private View loadingLayout;
    private View contentView;
    private ImageView schoolLogo;
    private ImageView schoolMotto;
    private TextView rankTitle;
    private TextView destinationTitle;

    private RankAdapter rankAdapter;
    private JournalAdapter journalAdapter;
    private CategoryAdapter categoryAdapter;
    private DestinationAdapter destinationAdapter;

    private final List<RankModel> rankList = new ArrayList<>();
    private final List<JournalEntry> journalList = new ArrayList<>();
    private final List<DestinationItem> destinationItems = new ArrayList<>();
    private List<CategoryModel> categoryList = new ArrayList<>();

    private List<RankModel> cachedRanks;
    private List<JournalEntry> cachedJournals;
    private List<DestinationItem> cachedDestinations;
    private List<CategoryModel> cachedCategories;
    private DocumentSnapshot cachedAboutData;
    private long ranksLastLoad = 0;
    private long journalsLastLoad = 0;
    private long destinationsLastLoad = 0;
    private long categoriesLastLoad = 0;
    private long aboutLastLoad = 0;
    private final Handler handler = new Handler();
    private ExecutorService executorService;
    private boolean isLoadingDismissed = false;
    private boolean journalsLoaded = false;
    private boolean categoriesLoaded = false;
    private boolean ranksLoaded = false;
    private boolean destinationsLoaded = false;
    private boolean aboutLoaded = false;

    private String userEmail;
    private String userInstitution;
    private String cachedInstitution; // Track the institution for which data is cached
    private LruCache<String, Bitmap> memCache;
    private FirebaseFirestore db;

    public MilitaryFragment1() {
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_military1, container, false);

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout1);
        rankRecyclerView = view.findViewById(R.id.rankRecyclerView);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        journalRecyclerView = view.findViewById(R.id.journalRecyclerView);
        destinationRecyclerView = view.findViewById(R.id.destinationRecyclerView);
        schoolLogo = view.findViewById(R.id.school_logo);
        schoolMotto = view.findViewById(R.id.school_motto);
        rankTitle = view.findViewById(R.id.rankTitle);
        destinationTitle = view.findViewById(R.id.destinationTitle);

        setupCache();
        setupRecyclerViews();
        resetLoadingState();
        showLoading(true);
        retrieveUserData();

        return view;
    }

    private void resetLoadingState() {
        isLoadingDismissed = false;
        journalsLoaded = false;
        categoriesLoaded = false;
        ranksLoaded = false;
        destinationsLoaded = false;
        aboutLoaded = false;
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

    private void setupRecyclerViews() {
        journalRecyclerView.setHasFixedSize(true);
        journalRecyclerView.setItemViewCacheSize(10);
        journalRecyclerView.setDrawingCacheEnabled(true);
        journalRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        journalRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        journalAdapter = new JournalAdapter(journalList, this, memCache);
        journalRecyclerView.setAdapter(journalAdapter);

        categoryRecyclerView.setHasFixedSize(true);
        categoryRecyclerView.setItemViewCacheSize(8);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        rankRecyclerView.setHasFixedSize(true);
        rankRecyclerView.setItemViewCacheSize(15);

        destinationRecyclerView.setHasFixedSize(true);
        destinationRecyclerView.setItemViewCacheSize(10);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Clear all cached data when institution changes
     */
    private void clearCacheIfInstitutionChanged() {
        if (cachedInstitution == null || !cachedInstitution.equals(userInstitution)) {
            Log.d(TAG, "Institution changed from '" + cachedInstitution + "' to '" + userInstitution + "', clearing cache");

            // Clear all cached data
            cachedRanks = null;
            cachedJournals = null;
            cachedDestinations = null;
            cachedCategories = null;
            cachedAboutData = null;

            // Reset cache timestamps
            ranksLastLoad = 0;
            journalsLastLoad = 0;
            destinationsLastLoad = 0;
            categoriesLastLoad = 0;
            aboutLastLoad = 0;

            // Clear memory cache
            if (memCache != null) {
                memCache.evictAll();
            }

            // Update cached institution
            cachedInstitution = userInstitution;

            Log.d(TAG, "Cache cleared for institution change");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        long currentTime = System.currentTimeMillis();

        resetLoadingState();
        showLoading(true);

        if (userInstitution == null) retrieveUserData();

        // Clear cache if institution has changed
        clearCacheIfInstitutionChanged();

        loadAboutDataWithCache(currentTime);
        loadImagesWithCache();

        if (NetworkUtils.isNetworkAvailable(requireContext())) {

            if (cachedJournals != null && (currentTime - journalsLastLoad) < CACHE_DURATION) {
                journalList.clear();
                journalList.addAll(cachedJournals);
                if (journalAdapter != null) journalAdapter.notifyDataSetChanged();
                journalsLoaded = true;
                checkAllDataLoaded();
            } else {
                loadJournals();
            }

            if (cachedCategories != null && (currentTime - categoriesLastLoad) < CACHE_DURATION) {
                categoryList.clear();
                categoryList.addAll(cachedCategories);
                setUpCategoryRecyclerView(categoryList);
                categoriesLoaded = true;
                checkAllDataLoaded();
            } else {
                loadCategories();
            }

            if (cachedRanks != null && (currentTime - ranksLastLoad) < CACHE_DURATION) {
                rankList.clear();
                rankList.addAll(cachedRanks);
                setupRanksRecyclerView();
                ranksLoaded = true;
                checkAllDataLoaded();
            } else {
                loadRanks();
            }

            if (cachedDestinations != null && (currentTime - destinationsLastLoad) < CACHE_DURATION) {
                destinationItems.clear();
                destinationItems.addAll(cachedDestinations);
                setupDestinationRecyclerView();
                destinationsLoaded = true;
                checkAllDataLoaded();
            } else {
                loadDestinations();
            }
        } else {
            if (cachedJournals != null) {
                journalList.clear();
                journalList.addAll(cachedJournals);
                if (journalAdapter != null) journalAdapter.notifyDataSetChanged();
            }
            if (cachedCategories != null) {
                categoryList.clear();
                categoryList.addAll(cachedCategories);
                setUpCategoryRecyclerView(categoryList);
            }
            if (cachedRanks != null) {
                rankList.clear();
                rankList.addAll(cachedRanks);
                setupRanksRecyclerView();
            }
            if (cachedDestinations != null) {
                destinationItems.clear();
                destinationItems.addAll(cachedDestinations);
                setupDestinationRecyclerView();
            }

            journalsLoaded = true;
            categoriesLoaded = true;
            ranksLoaded = true;
            destinationsLoaded = true;
            checkAllDataLoaded();
        }
    }

    @Override
    public void onDestroyView() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (journalAdapter != null) {
            journalAdapter.cleanup();
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
        categoryRecyclerView = null;
        rankRecyclerView = null;
        journalRecyclerView = null;
        destinationRecyclerView = null;
        schoolLogo = null;
        schoolMotto = null;
        rankTitle = null;
        destinationTitle = null;

        rankAdapter = null;
        journalAdapter = null;
        categoryAdapter = null;
        destinationAdapter = null;

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
                    loadingLayout.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void loadAboutDataWithCache(long currentTime) {
        if (cachedAboutData != null && (currentTime - aboutLastLoad) < CACHE_DURATION) {
            updateAboutUI(cachedAboutData);
            aboutLoaded = true;
            checkAllDataLoaded();
        } else {
            loadAboutData();
        }
    }

    private void loadAboutData() {
        if (userInstitution == null) return;
        db.document("MILITARY/RO/" + userInstitution + "/ABOUT")
                .get()
                .addOnCompleteListener(task -> {
                    if (!isFragmentSafe()) return;

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            cachedAboutData = document;
                            aboutLastLoad = System.currentTimeMillis();
                            updateAboutUI(document);
                        }
                    } else {
                        Log.e(TAG, "Error getting about data: ", task.getException());
                    }
                    aboutLoaded = true;
                    checkAllDataLoaded();
                });
    }

    private void updateAboutUI(DocumentSnapshot document) {
        if (document != null && document.exists()) {
            String rankTitleText = document.getString("rankTitle");
            String destinationTitleText = document.getString("destinationTitle");

            if (rankTitleText != null && rankTitle != null) {
                rankTitle.setText(rankTitleText);
            }
            if (destinationTitleText != null && destinationTitle != null) {
                destinationTitle.setText(destinationTitleText);
            }
        }
    }

    private void loadImagesWithCache() {
        if (!isFragmentSafe() || userInstitution == null) return;
        Context ctx = requireContext();
        if (schoolLogo != null) {
            Glide.with(ctx)
                    .load("https://firebasestorage.googleapis.com/v0/b/simplicadet.firebasestorage.app/o/MILITARY%2FRO%2F" + userInstitution + "%2Flogo.png?alt=media")
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).override(500, 500).dontTransform())
                    .into(schoolLogo);
        }
        if (schoolMotto != null) {
            coil.ImageLoader loader = SvgImageLoader.get(ctx);
            coil.request.ImageRequest req = new coil.request.ImageRequest.Builder(ctx)
                    .data("https://firebasestorage.googleapis.com/v0/b/simplicadet.firebasestorage.app/o/MILITARY%2FRO%2F" + userInstitution + "%2Fmotto.svg?alt=media")
                    .target(schoolMotto)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build();
            loader.enqueue(req);
        }
    }

    private void loadDestinations() {
        if (userInstitution == null) return;
        db.document("MILITARY/RO/" + userInstitution + "/PICTURES")
                .get()
                .addOnCompleteListener(task -> {
                    if (!isFragmentSafe()) return;

                    if (task.isSuccessful()) {
                        destinationItems.clear();
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            for (String key : Objects.requireNonNull(document.getData()).keySet()) {
                                String imageUrl = document.getString(key);
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    DestinationItem item = new DestinationItem(key, imageUrl);
                                    destinationItems.add(item);
                                }
                            }

                            if (destinationItems.size() > 3) {
                                Collections.shuffle(destinationItems);
                            }

                            cachedDestinations = new ArrayList<>(destinationItems);
                            destinationsLastLoad = System.currentTimeMillis();

                            setupDestinationRecyclerView();
                            preloadDestinationImages();
                        }

                    } else {
                        Log.e(TAG, "Error getting destinations: ", task.getException());
                    }
                    destinationsLoaded = true;
                    checkAllDataLoaded();
                });
    }

    private void preloadDestinationImages() {
        if (destinationItems != null && !destinationItems.isEmpty() && isFragmentSafe()) {
            getExecutorService().execute(() -> {
                for (int i = 0; i < Math.min(3, destinationItems.size()); i++) {
                    if (!isFragmentSafe()) break;

                    final String imageUrl = destinationItems.get(i).getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            Glide.with(requireContext())
                                    .asBitmap()
                                    .load(imageUrl)
                                    .apply(new RequestOptions()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .override(300, 200))
                                    .preload();
                        } catch (Exception e) {
                            Log.e(TAG, "Error preloading destination image: " + imageUrl, e);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void setupDestinationRecyclerView() {
        if (!isFragmentSafe()) return;

        Context context = requireContext();

        if (destinationAdapter == null) {
            destinationAdapter = new DestinationAdapter(destinationItems, context, memCache, this);
        }

        if (destinationRecyclerView.getLayoutManager() == null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            destinationRecyclerView.setLayoutManager(layoutManager);
        }

        if (destinationRecyclerView.getOnFlingListener() != null) {
            destinationRecyclerView.setOnFlingListener(null);
        }

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(destinationRecyclerView);

        destinationRecyclerView.setAdapter(destinationAdapter);

        destinationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    @Override
    public void onDestinationClick(DestinationItem destination) {
        Log.d(TAG, "Destination clicked: " + destination.getId());
    }

    private void loadRanks() {
        DbQuery.loadRanks(userInstitution, new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSucces() {
                if (!isFragmentSafe()) return;

                rankList.clear();
                rankList.addAll(DbQuery.g_rankList);

                cachedRanks = new ArrayList<>(DbQuery.g_rankList);
                ranksLastLoad = System.currentTimeMillis();

                setupRanksRecyclerView();

                ranksLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                if (isFragmentSafe()) {
                    Log.e(TAG, "Failed to load ranks");
                    ranksLoaded = true;
                    checkAllDataLoaded();
                }
            }
        });
    }

    private void setupRanksRecyclerView() {
        if (!isFragmentSafe()) return;

        if (rankAdapter == null) {
            rankAdapter = new RankAdapter(rankList, requireContext(), memCache);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rankRecyclerView.setLayoutManager(layoutManager);
        rankRecyclerView.setAdapter(rankAdapter);

        if (rankAdapter != null) {
            rankAdapter.notifyDataSetChanged();
        }
    }

    private void loadJournals() {
        DbQuery.loadJournals(userInstitution, new MyCompleteListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSucces() {
                if (!isFragmentSafe()) return;

                journalList.clear();
                if (DbQuery.g_militaryJournalList != null && !DbQuery.g_militaryJournalList.isEmpty()) {
                    journalList.addAll(DbQuery.g_militaryJournalList);

                    cachedJournals = new ArrayList<>(DbQuery.g_militaryJournalList);
                    journalsLastLoad = System.currentTimeMillis();

                    preloadJournalImages();
                }

                if (journalAdapter != null) {
                    journalAdapter.notifyDataSetChanged();
                }

                journalsLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                if (isFragmentSafe()) {
                    Log.e(TAG, "Failed to load journals");
                    journalsLoaded = true;
                    checkAllDataLoaded();
                }
            }
        });
    }

    private void preloadJournalImages() {
        if (journalList != null && !journalList.isEmpty() && isFragmentSafe()) {
            getExecutorService().execute(() -> {
                for (int i = 0; i < Math.min(3, journalList.size()); i++) {
                    if (!isFragmentSafe()) break;

                    final String imageUrl = journalList.get(i).getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            Glide.with(requireContext())
                                    .asBitmap()
                                    .load(imageUrl)
                                    .apply(new RequestOptions()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .override(200, 150))
                                    .preload();
                        } catch (Exception e) {
                            Log.e(TAG, "Error preloading journal image: " + imageUrl, e);
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onJournalClick(String journalLink) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            return;
        }

        if (journalLink != null && !journalLink.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(journalLink));
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening journal link", e);
            }
        } else {
            Log.e(TAG, "Invalid journal link: " + journalLink);
        }
    }

    private void loadCategories() {
        DbQuery.loadMilitaryCategories(requireContext(), userInstitution, new MyCompleteListener() {
            @Override
            public void onSucces() {
                if (!isFragmentSafe()) return;

                List<CategoryModel> loadedCategories = DbQuery.g_militaryCatList;
                if (loadedCategories != null && !loadedCategories.isEmpty()) {
                    categoryList.clear();
                    categoryList.addAll(loadedCategories);
                    cachedCategories = new ArrayList<>(loadedCategories);
                    categoriesLastLoad = System.currentTimeMillis();
                    setUpCategoryRecyclerView(categoryList);
                }
                categoriesLoaded = true;
                checkAllDataLoaded();
            }

            @Override
            public void onFailure() {
                if (isFragmentSafe()) {
                    categoriesLoaded = true;
                    checkAllDataLoaded();
                }
            }
        });
    }

    private void checkAllDataLoaded() {
        if (journalsLoaded && categoriesLoaded && ranksLoaded && destinationsLoaded && aboutLoaded) {
            showLoading(false);
        }
    }

    private void setUpCategoryRecyclerView(List<CategoryModel> categoryList) {
        if (isFragmentSafe()) {
            if (categoryAdapter == null) {
                categoryAdapter = new CategoryAdapter(categoryList, requireContext(), this);
            }

            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
            categoryRecyclerView.setLayoutManager(layoutManager);
            categoryRecyclerView.setAdapter(categoryAdapter);
        } else {
            Log.e(TAG, "Fragment is not attached, cannot set up category recycler view");
        }
    }

    @Override
    public void onQuizClick(String categoryId, String testId) {
        Context context = categoryRecyclerView.getContext();
        Intent intent = new Intent(context, Questions.class);

        intent.putExtra("categoryId", categoryId);
        intent.putExtra("testId", testId);

        startActivityForResult(intent, 1);
    }

    private void showRedeemDialog(int totalScore, int correctAnswers, int totalQuestions, float totalTime) {
        if (isFragmentSafe() && totalScore > 0) {
            ShowRedeem bottomSheetFragment = new ShowRedeem();
            Bundle bundle = new Bundle();

            bundle.putInt("totalScore", totalScore);
            bundle.putInt("totalQuestions", totalQuestions);
            bundle.putFloat("totalTime", totalTime);
            bundle.putInt("correctAnswers", correctAnswers);

            bottomSheetFragment.setArguments(bundle);
            bottomSheetFragment.show(requireActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        retrieveUserData();

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            int totalScore = data.getIntExtra("totalScore", 0);
            int correctAnswers = data.getIntExtra("correctAnswers", 0);
            int totalQuestions = data.getIntExtra("totalQuestions", 0);
            long totalTime = data.getLongExtra("totalTime", 0L);
            float totalResponseTime = (float) totalTime / 1000;

            handler.postDelayed(() -> showRedeemDialog(totalScore, correctAnswers, totalQuestions, totalResponseTime), 1000);

            DbQuery.updateTotalScore(userEmail, totalScore, new MyCompleteListener() {
                @Override
                public void onSucces() {
                    Log.d(TAG, "Total score updated successfully in Firestore.");
                }

                @Override
                public void onFailure() {
                    Log.e(TAG, "Failed to update total score in Firestore.");
                }
            });
        }
    }

    private void retrieveUserData() {
        if (getActivity() != null) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserData", MODE_PRIVATE);
            userEmail = sharedPreferences.getString("userEmail", "");
            userInstitution = sharedPreferences.getString("userInstitution", "");
        }
    }
}