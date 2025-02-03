package com.learn.simplify.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.learn.simplify.R;
import com.learn.simplify.ads.InterstitialAdd;
import com.learn.simplify.databinding.ActivityHomeBinding;

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
        window.setStatusBarColor(getResources().getColor(R.color.transparent));

        com.learn.simplify.databinding.ActivityHomeBinding binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setBackgroundResource(R.drawable.gradient_bottom_nav);
        navView.setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));

        int[][] states = new int[][] {new int[] { android.R.attr.state_selected }, new int[] {}};
        int[] colors = new int[] {Color.WHITE, Color.GRAY};
        ColorStateList colorStateList = new ColorStateList(states, colors);
        navView.setItemIconTintList(colorStateList);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_search,R.id.navigation_community)
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
        // Handle navigation view item clicks here.
        int itemId = menuItem.getItemId();
        if (itemId == R.id.drawer_logout) {
            // Sign out the user and navigate to MainActivity
            firebaseAuth.signOut();
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.drawer_new) {
            // Handle "What's new" click
            Toast.makeText(this, "What's new clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.drawer_help) {
            // Handle "Help" click
            Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
        } else if (itemId == R.id.drawer_settings) {
            // Handle "Settings" click
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
        }

        // Close the drawer after handling the click
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
