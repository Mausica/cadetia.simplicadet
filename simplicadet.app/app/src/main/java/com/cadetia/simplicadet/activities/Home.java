package com.cadetia.simplicadet.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.database.TextUpload;
import com.cadetia.simplicadet.ui.home.HomeFragment;
import com.cadetia.simplicadet.ui.home.HomeFragment1;
import com.cadetia.simplicadet.ui.home.HomeFragment2;
import com.cadetia.simplicadet.ui.home.HomeFragment3;
import com.cadetia.simplicadet.ui.home.HomeFragment4;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.ads.InterstitialAdd;
import com.cadetia.simplicadet.databinding.ActivityHomeBinding;

public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private View darkOverlay;
    private InterstitialAdd interstitialAdd;
    static final float END_SCALE = 0.7f;
    private MenuItem menuItem;
    private FirebaseAuth firebaseAuth;
    private TextView drawerNameTextView;
    ShapeableImageView drawerloadingButton;
    private DrawerLayout drawerLayout;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        // Detect current theme
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        com.cadetia.simplicadet.databinding.ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_selected },
                new int[] {}
        };

        int[] colors;
        if (isDarkTheme) {
            colors = new int[] {Color.WHITE, Color.GRAY};
        } else {
            colors = new int[] {Color.BLACK, Color.GRAY};
        }

        ColorStateList colorStateList = new ColorStateList(states, colors);
        navView.setItemIconTintList(colorStateList);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);

        // Custom navigation with fade animations
        navView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Prevent re-navigation if already on the destination
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == itemId) {
                return false;
            }

            // Build navigation options with animations
            NavOptions navOptions = new NavOptions.Builder()
                    //.setEnterAnim(R.anim.fade_in_d)
                    //.setExitAnim(R.anim.fade_out_d)
                    //.setPopEnterAnim(R.anim.fade_in_d)
                    //.setPopExitAnim(R.anim.fade_out_d)
                    .setLaunchSingleTop(true)
                    .build();

            try {
                navController.navigate(itemId, null, navOptions);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return false;
            }
        });

        // Sync BottomNavigationView with current destination
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            if (destId == R.id.navigation_home || destId == R.id.navigation_search || destId == R.id.navigation_liked) {
                navView.setSelectedItemId(destId);
            }
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);

        drawerNameTextView = headerView.findViewById(R.id.drawer_name);
        drawerloadingButton = headerView.findViewById(R.id.loading_button);

        // Retrieve and set user data
        retrieveUserData();
        firebaseAuth = FirebaseAuth.getInstance();

        final Handler handler = new Handler();
        handler.postDelayed(() -> navigationDrawer(window), 100);
    }

    private void navigationDrawer(Window view) {
        NavigationView navigationView = this.findViewById(R.id.navigation_view);
        drawerLayout = this.findViewById(R.id.drawer_layout);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.bringToFront();

            SharedPreferences sharedPreferences = this.getSharedPreferences("UserData", MODE_PRIVATE);
            String userPhoto = sharedPreferences.getString("userPhoto", "");

            ShapeableImageView profileButton = view.findViewById(R.id.mainProfileButton);

            if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")) {
                Glide.with(this).load(R.raw.guest).into(profileButton);
            } else {
                Glide.with(this).load(userPhoto).into(profileButton);
            }

        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        //BottomNavigationView bottomNavigationView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);

        if (itemId == R.id.drawer_logout) {
            firebaseAuth.signOut();
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.drawer_new) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://presamil.ro/ultimul_nr/"));
            startActivity(browserIntent);
        } else if (itemId == R.id.drawer_help) {
            // navController.navigate(R.id.navigation_liked);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.google.com/"));
            startActivity(browserIntent);

        } else if (itemId == R.id.drawer_settings) {
            //Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.drawer_upload) {
            if (userEmail.equals("marius.gabryel2017@gmail.com")){
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                String[] mimeTypes = {"text/plain", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(intent, 1);
            } else
                Toast.makeText(this, "Nu ai permisiunea necesară!", Toast.LENGTH_LONG).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    String mimeType = getContentResolver().getType(fileUri);

                    // Verifică tipul fișierului
                    if (mimeType != null && mimeType.contains("text/plain")) {
                        // Procesează fișier text
                        TextUpload textUpload = new TextUpload();
                        textUpload.uploadQuestionsFromText(this, fileUri);
                    } else if (mimeType != null && (mimeType.contains("excel") || mimeType.contains("sheet"))) {
                        // Procesează fișier Excel
                        // Dacă vrei să păstrezi și suportul pentru Excel, adaugă aici codul pentru ExcelUpload
                        Toast.makeText(this, "Fișierele Excel nu mai sunt acceptate. Folosiți formatul text.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Format de fișier neacceptat. Folosiți fișiere text (.txt).", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            interstitialAdd = new InterstitialAdd();
            interstitialAdd.loadInterstitialAd(this);
        }
    }

    public void openNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void showAdd(){
        if (interstitialAdd != null) {
            interstitialAdd.showInterstitialAd(Home.this);
        }
    }

    public void updateFabIcon(Fragment fragment) {
        FloatingActionButton fabMain = findViewById(R.id.fabMain);

        // Get the actual visible fragment from NavHost
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_home);
        if (navHostFragment instanceof NavHostFragment) {
            Fragment primaryFragment = ((NavHostFragment) navHostFragment).getChildFragmentManager().getPrimaryNavigationFragment();

            if (primaryFragment instanceof HomeFragment) {
                // Check child fragment for HomeFragment
                Fragment childFragment = primaryFragment.getChildFragmentManager().findFragmentById(R.id.smallerFragmentContainer);

                if (childFragment instanceof HomeFragment1) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    hideFab(); // Use your updated method
                } else if (childFragment instanceof HomeFragment2) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    showFab(); // Use the new method to restore layout
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        HomeFragment homeFragment = (HomeFragment) primaryNavigationFragment;
                        homeFragment.actionController();
                    });
                } else if (childFragment instanceof HomeFragment3) {
                    fabMain.setImageResource(R.drawable.home_ic_rotate);
                    showFab(); // Use the new method to restore layout
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        HomeFragment homeFragment = (HomeFragment) primaryNavigationFragment;
                        homeFragment.actionController();
                    });
                } else if (childFragment instanceof HomeFragment4) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    showFab(); // Use the new method to restore layout
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        HomeFragment homeFragment = (HomeFragment) primaryNavigationFragment;
                        homeFragment.actionController();
                    });
                }
            }
        } else {
            fabMain.setImageResource(R.drawable.home_ic_plus);
            hideFab(); // Use your updated method
        }
    }

    public void hideFab() {
        FloatingActionButton fabMain = findViewById(R.id.fabMain);
        fabMain.setImageResource(R.drawable.home_ic_plus);
        fabMain.setVisibility(View.GONE);

        // Get reference to the nav_card
        androidx.cardview.widget.CardView navCard = findViewById(R.id.nav_card);

        // Update constraints to center the nav_card when FAB is hidden
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) navCard.getLayoutParams();

        // Clear the end constraint to fab_container
        params.endToStart = -1;
        // Set end constraint to parent
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;

        navCard.setLayoutParams(params);
    }

    public void showFab() {
        FloatingActionButton fabMain = findViewById(R.id.fabMain);
        fabMain.setVisibility(View.VISIBLE);

        // Get reference to the nav_card
        androidx.cardview.widget.CardView navCard = findViewById(R.id.nav_card);

        // Restore original constraints
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) navCard.getLayoutParams();

        // Set end constraint back to fab_container
        params.endToStart = R.id.fab_container;
        // Clear end constraint to parent
        params.endToEnd = -1;

        navCard.setLayoutParams(params);
    }

    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "");
        String userPhoto = sharedPreferences.getString("userPhoto", "");
        userEmail = sharedPreferences.getString("userEmail", "");

        drawerNameTextView.setText(userName);

        if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")){
            Glide.with(this).load(R.raw.guest).into(drawerloadingButton);
        } else {
            Glide.with(this).load(userPhoto).into(drawerloadingButton);
        }
    }
}