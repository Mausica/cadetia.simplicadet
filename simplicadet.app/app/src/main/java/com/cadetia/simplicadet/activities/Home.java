package com.cadetia.simplicadet.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.database.ExcelUpload;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.focus));

        // Detectează tema curentă
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        com.cadetia.simplicadet.databinding.ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        //navView.setBackgroundResource(R.drawable.gradient_bottom_light);
        navView.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_selected }, // Selecționată
                new int[] {} // Neselectată
        };

        int[] colors;
        if (isDarkTheme) {
            colors = new int[] {Color.WHITE, Color.GRAY}; // Alb pentru tema dark
        } else {
            colors = new int[] {Color.BLACK, Color.GRAY}; // Negru pentru tema light
        }

        ColorStateList colorStateList = new ColorStateList(states, colors);
        navView.setItemIconTintList(colorStateList);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);
        NavigationUI.setupWithNavController(binding.navView, navController);

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                ExcelUpload excelUpload = new ExcelUpload();
                excelUpload.uploadQuestions(this, fileUri);
            }
        }
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
            Toast.makeText(this, "What's new clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.drawer_help) {
            // Navigate to Community fragment // TO DO - MAPPING RANDOMLY TO HOME/SEARCH
            navController.navigate(R.id.navigation_community);
        } else if (itemId == R.id.drawer_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.drawer_upload) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
            startActivityForResult(intent, 1);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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

    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "");
        String userPhoto = sharedPreferences.getString("userPhoto", "");

        drawerNameTextView.setText(userName);

        if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")){
            Glide.with(this).load(R.raw.guest).into(drawerloadingButton);
        } else {
            Glide.with(this).load(userPhoto).into(drawerloadingButton);
        }
    }
}
