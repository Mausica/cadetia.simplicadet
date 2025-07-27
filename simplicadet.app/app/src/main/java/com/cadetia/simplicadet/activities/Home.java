package com.cadetia.simplicadet.activities;

import static com.cadetia.simplicadet.database.DbQuery.getCellValueAsString;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.dao.LocaleHelper;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.database.TextUpload;
import com.cadetia.simplicadet.entities.DialogConfirm;
import com.cadetia.simplicadet.entities.InstitutionSelectionDialog;
import com.cadetia.simplicadet.entities.UploadTypeDialog;
import com.cadetia.simplicadet.listeners.MyCompleteListener;
import com.cadetia.simplicadet.ui.home.HomeFragment;
import com.cadetia.simplicadet.ui.home.HomeFragment1;
import com.cadetia.simplicadet.ui.home.HomeFragment2;
import com.cadetia.simplicadet.ui.home.HomeFragment3;
import com.cadetia.simplicadet.ui.home.HomeFragment4;
import com.cadetia.simplicadet.ui.military.MilitaryFragment;
import com.cadetia.simplicadet.ui.military.MilitaryFragment1;
import com.cadetia.simplicadet.ui.military.MilitaryFragment2;
import com.cadetia.simplicadet.ui.military.MilitaryFragment3;
import com.cadetia.simplicadet.ui.military.MilitaryFragment4;
import com.cadetia.simplicadet.utils.NetworkUtils;
import com.cadetia.simplicadet.utils.VersionChecker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.ads.InterstitialAdd;
import com.cadetia.simplicadet.databinding.ActivityHomeBinding;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Home extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private String currentLanguage;
    private InterstitialAdd interstitialAdd;
    private FirebaseAuth firebaseAuth;
    private TextView drawerNameTextView;
    ShapeableImageView drawerloadingButton;
    private DrawerLayout drawerLayout;
    private boolean isNetworkAvailable = true;

    private TextView drawerInstitutionTextView;
    private String userEmail;

    private boolean isAdmin = false;
    private String userInstitution = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            int uiMode = overrideConfiguration.uiMode;
            overrideConfiguration.setTo(getBaseContext().getResources().getConfiguration());
            overrideConfiguration.uiMode = uiMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentLanguage = LocaleHelper.getLanguage(this);

        Window window = getWindow();

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

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);

        navView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (!isNetworkAvailable && itemId != R.id.navigation_home) {
                showNoInternetDialog();
                return false;
            }
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == itemId) {
                return false;
            }
            NavOptions navOptions = new NavOptions.Builder()
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

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            if (destId == R.id.navigation_home || destId == R.id.navigation_search || destId == R.id.navigation_military) {
                navView.setSelectedItemId(destId);
                new Handler().postDelayed(() -> navigationDrawer(window), 100);
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
        drawerNameTextView = headerView.findViewById(R.id.drawer_name);
        drawerInstitutionTextView = headerView.findViewById(R.id.drawer_institution);
        drawerloadingButton = headerView.findViewById(R.id.loading_button);

        retrieveUserData();
        refreshUserInstitution();
        checkUserPermissions();
        checkIfNeedsInstitutionSelection();

        firebaseAuth = FirebaseAuth.getInstance();

        final Handler handler = new Handler();
        handler.postDelayed(() -> navigationDrawer(window), 100);

        VersionChecker.checkVersionOnHomeActivity(this, new VersionChecker.VersionCheckCallback() {
            @Override
            public void onVersionSupported() {
            }
            @Override
            public void onVersionUnsupported() {
                VersionChecker.showUnsupportedVersionDialogForHome(Home.this, () -> {
                });
            }
            @Override
            public void onMaintenanceMode() {
                VersionChecker.showMaintenanceDialog(Home.this, null);
            }
            @Override
            public void onOfflineMode() {
            }
        });
    }

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean previousStatus = isNetworkAvailable;
            isNetworkAvailable = NetworkUtils.isNetworkAvailable(Home.this);

            if (!previousStatus && isNetworkAvailable) {
                handleConnectionRestored();
            } else if (previousStatus && !isNetworkAvailable) {
                handleConnectionLost();
            }
        }
    };

    private void checkIfNeedsInstitutionSelection() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String email = sharedPreferences.getString("userEmail", "");

        DbQuery.g_firestore.collection("USERS").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showInstitutionSelectionDialog();
                    }
                });
    }

    private void showInstitutionSelectionDialog() {
        InstitutionSelectionDialog.show(this, new InstitutionSelectionDialog.InstitutionSelectionCallback() {
            @Override
            public void onInstitutionSelected(String institution, String accessCode) {
                SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                String email = sharedPreferences.getString("userEmail", "");
                String name = sharedPreferences.getString("userName", "");
                String photo = sharedPreferences.getString("userPhoto", "");

                if ("INDIVIDUAL".equals(institution)) {
                    createIndividualUser(email, name, photo);
                } else {
                    if (accessCode == null || accessCode.trim().isEmpty()) {
                        Toast.makeText(Home.this, "Please enter an access code", Toast.LENGTH_LONG).show();
                        return;
                    }

                    DbQuery.validateAndLockAccessCode(accessCode.trim(), email, new DbQuery.AccessCodeValidationCallback() {
                        @Override
                        public void onAccessCodeValid(DbQuery.AccessCodeData data) {
                            Map<String, Object> userData = new ArrayMap<>();
                            userData.put("EMAIL_ID", email);
                            userData.put("NAME", data.name);
                            userData.put("PHOTO", photo);
                            userData.put("TOTAL_SCORE", 0);
                            userData.put("ADMIN", false);
                            userData.put("PREMIUM", false);
                            userData.put("DATE", System.currentTimeMillis());
                            userData.put("INSTITUTION", data.institution);
                            userData.put("HEIGHT", data.height);
                            userData.put("PLUTON", data.pluton);
                            userData.put("RANK", data.rank);

                            DbQuery.g_firestore.collection("USERS").document(email).set(userData)
                                    .addOnSuccessListener(unused -> {
                                        if (data.year != null && !data.year.isEmpty()) {
                                            String institutionPath = "MILITARY/RO/" + data.institution + "/STUDENTS/" + data.year;
                                            DbQuery.g_firestore.collection(institutionPath).document(email).set(userData);
                                        }

                                        DbQuery.g_firestore.collection("ACCESS_CODES").document(accessCode.trim()).delete();

                                        SharedPreferences.Editor editor = getSharedPreferences("UserData", MODE_PRIVATE).edit();
                                        editor.putString("userInstitution", data.institution);
                                        editor.apply();

                                        checkUserPermissions();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(Home.this, "Error creating user account", Toast.LENGTH_LONG).show());
                        }

                        @Override
                        public void onAccessCodeInvalid() {
                            Toast.makeText(Home.this, "Invalid or already used access code", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onCancel() {
                firebaseAuth.signOut();
                Intent intent = new Intent(Home.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void createIndividualUser(String email, String name, String photo) {
        Map<String, Object> userData = new ArrayMap<>();
        userData.put("EMAIL_ID", email);
        userData.put("NAME", name);
        userData.put("PHOTO", photo);
        userData.put("TOTAL_SCORE", 0);
        userData.put("ADMIN", false);
        userData.put("PREMIUM", false);
        userData.put("DATE", System.currentTimeMillis());
        userData.put("INSTITUTION", "INDIVIDUAL");

        DbQuery.g_firestore.collection("USERS").document(email).set(userData)
                .addOnSuccessListener(unused -> {
                    SharedPreferences.Editor editor = getSharedPreferences("UserData", MODE_PRIVATE).edit();
                    editor.putString("userInstitution", "INDIVIDUAL");
                    editor.apply();

                    checkUserPermissions();
                })
                .addOnFailureListener(e -> Toast.makeText(Home.this, "Error creating individual user account", Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String savedLanguage = LocaleHelper.getLanguage(this);
        if (!savedLanguage.equals(currentLanguage)) {
            currentLanguage = savedLanguage;
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(networkChangeReceiver);
    }

    private void checkUserPermissions() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String email = sharedPreferences.getString("userEmail", "");

        DbQuery.checkUserPermissions(email, new DbQuery.PermissionCallback() {
            @Override
            public void onPermissionsReceived(boolean admin, boolean premium, String institution) {
                isAdmin = admin;
                userInstitution = institution;
                updateNavigationMenu();
            }

            @Override
            public void onFailure() {
                isAdmin = false;
                userInstitution = "INDIVIDUAL";
            }
        });
    }

    private void updateNavigationMenu() {
        NavigationView navigationView = findViewById(R.id.navigation_view);
        Menu menu = navigationView.getMenu();
        MenuItem uploadItem = menu.findItem(R.id.drawer_upload);

        if (uploadItem != null) {
            uploadItem.setVisible(isAdmin);
        }
    }
    private void navigationDrawer(Window view) {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_home);
        if (navHostFragment instanceof NavHostFragment) {
            Fragment primaryFragment = ((NavHostFragment) navHostFragment).getChildFragmentManager().getPrimaryNavigationFragment();
            boolean isMilitary = (primaryFragment instanceof MilitaryFragment);

            NavigationView navigationView = findViewById(R.id.navigation_view);
            if (navigationView != null) {
                View headerView = navigationView.getHeaderView(0);
                navigationView.setNavigationItemSelectedListener(this);
                navigationView.bringToFront();

                SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                String userPhoto = sharedPreferences.getString("userPhoto", "");

                ShapeableImageView profileButton = headerView.findViewById(R.id.loading_button);
                ProgressBar progressBar = headerView.findViewById(R.id.drawer_progress_bar);

                if (profileButton != null) {
                    if (!isNetworkAvailable || userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")) {
                        if (isMilitary) {
                            Glide.with(this).load(R.raw.guest_military).into(profileButton);
                        } else {
                            Glide.with(this).load(R.raw.guest_civil).into(profileButton);
                        }
                    } else {
                        Glide.with(this).load(userPhoto).into(profileButton);
                    }
                }

                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(false);

                    if (isMilitary) {
                        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.home_loading_military));
                    } else {
                        progressBar.setProgressDrawable(ContextCompat.getDrawable(this, R.drawable.home_loading_civil));
                    }

                    progressBar.setMax(100);
                    progressBar.setProgress(0);
                    progressBar.setProgress(58);

                    Log.d("ProgressBar", "Progress updated to 58 for " + (isMilitary ? "military" : "civil"));
                }
            }
        }
    }


    private Fragment getCurrentFragment() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_home);
        if (navHostFragment instanceof NavHostFragment) {
            return ((NavHostFragment) navHostFragment).getChildFragmentManager().getPrimaryNavigationFragment();
        }
        return null;
    }

    private void showNoInternetDialog() {
        DialogConfirm.show(
                this,
                getString(R.string.no_internet_title),
                getString(R.string.no_internet_text),
                () -> {
                    Fragment currentFragment = getCurrentFragment();
                    if (currentFragment instanceof HomeFragment) {
                        NavController navController = Navigation.findNavController(Home.this, R.id.nav_host_fragment_activity_home);
                        navController.navigate(R.id.navigation_home, null, new NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setPopUpTo(R.id.navigation_home, true)
                                .build());
                    }
                },
                false
        );
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (!isNetworkAvailable && itemId != R.id.drawer_logout && itemId != R.id.drawer_settings) {
            showNoInternetDialog();
            drawerLayout.closeDrawer(GravityCompat.START);
            return false;
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);

        if (itemId == R.id.drawer_logout) {
            firebaseAuth.signOut();
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (itemId == R.id.drawer_new) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Mausica/cadetia.simplicadet/releases"));
            startActivity(browserIntent);
        } else if (itemId == R.id.drawer_help) {
            Intent intent = new Intent(this, Community.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
        } else if (itemId == R.id.drawer_settings) {
            Intent intent = new Intent(Home.this, Settings.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in_d, R.anim.fade_out_d);
            finish();
        } else if (itemId == R.id.drawer_upload) {
            if (isAdmin) {
                showUploadTypeDialog();
            } else {
                Toast.makeText(this, "Nu ai permisiunea necesară!", Toast.LENGTH_LONG).show();
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showUploadTypeDialog() {
        UploadTypeDialog.show(this, new UploadTypeDialog.UploadTypeCallback() {
            @Override
            public void onQuizUpload() {
                selectQuizFile();
            }

            @Override
            public void onStudentUpload() {
                selectStudentFile();
            }

            @Override
            public void onCancel() {
            }
        });
    }

    private void selectQuizFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        startActivityForResult(intent, 1);
    }

    private void selectStudentFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        startActivityForResult(intent, 2);
    }

    private void forceNavigationToHome() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setSelectedItemId(R.id.navigation_home);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_home);
        navController.popBackStack(R.id.navigation_home, false);
    }

    private void handleConnectionRestored() {
        VersionChecker.checkVersion(this, new VersionChecker.VersionCheckCallback() {
            @Override
            public void onVersionSupported() {
                runOnUiThread(() -> {
                    retrieveUserData();
                    Toast.makeText(Home.this, "Connection restored", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onMaintenanceMode() {
                VersionChecker.showMaintenanceDialog(Home.this, null);
            }

            @Override
            public void onVersionUnsupported() {
                runOnUiThread(() -> {
                    VersionChecker.showUnsupportedVersionDialog(Home.this, () -> {
                        firebaseAuth.signOut();
                        Intent intent = new Intent(Home.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                });
            }

            @Override
            public void onOfflineMode() {
                runOnUiThread(() -> {
                    retrieveUserData();
                    Toast.makeText(Home.this, "Connection restored", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleConnectionLost() {
        runOnUiThread(() -> {
            if (!(getCurrentFragment() instanceof HomeFragment)) {
                forceNavigationToHome();
            }
            retrieveUserData();
            showNoInternetDialog();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                if (requestCode == 1) {
                    TextUpload textUpload = new TextUpload();
                    textUpload.uploadQuestionsFromText(this, fileUri);
                } else if (requestCode == 2) {
                    showInstitutionYearDialog(fileUri);
                } else if (requestCode == 3) {
                    uploadAccessCodesFromExcel(fileUri);
                }
            }
        }
    }

    private void uploadAccessCodesFromExcel(Uri fileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            List<DbQuery.AccessCodeData> accessCodes = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String accessCode = getCellValueAsString(row.getCell(0));
                    String name = getCellValueAsString(row.getCell(1));
                    String institution = getCellValueAsString(row.getCell(2));
                    String year = getCellValueAsString(row.getCell(3));
                    String photo = getCellValueAsString(row.getCell(4));
                    String height = getCellValueAsString(row.getCell(5));
                    String pluton = getCellValueAsString(row.getCell(6));
                    String rank = getCellValueAsString(row.getCell(7));

                    DbQuery.AccessCodeData accessCodeData = new DbQuery.AccessCodeData(
                            accessCode, name, year, institution, photo,
                            Integer.parseInt(height.isEmpty() ? "170" : height),
                            Integer.parseInt(pluton.isEmpty() ? "1" : pluton),
                            Integer.parseInt(rank.isEmpty() ? "0" : rank)
                    );

                    accessCodes.add(accessCodeData);
                }
            }

            DbQuery.uploadAccessCodes(accessCodes, new MyCompleteListener() {
                @Override
                public void onSucces() {
                    Toast.makeText(Home.this, "Access codes uploaded successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure() {
                    Toast.makeText(Home.this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            });

            workbook.close();
            inputStream.close();

        } catch (Exception e) {
            Log.e("Home", "Error uploading access codes", e);
            Toast.makeText(this, "Error uploading access codes", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInstitutionYearDialog(Uri fileUri) {
        Dialog dialog = new Dialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_institution_year, null);
        dialog.setContentView(view);

        EditText editInstitution = view.findViewById(R.id.edit_institution);
        EditText editYear = view.findViewById(R.id.edit_year);
        Button btnUpload = view.findViewById(R.id.btn_upload);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnUpload.setOnClickListener(v -> {
            String institution = editInstitution.getText().toString().trim();
            String year = editYear.getText().toString().trim();

            if (!institution.isEmpty() && !year.isEmpty()) {
                DbQuery.uploadStudentsWithAccessCodes(this, fileUri, institution, year, new MyCompleteListener() {
                    @Override
                    public void onSucces() {
                        Toast.makeText(Home.this, "Students and access codes uploaded successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(Home.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_home);
        if (navHostFragment instanceof NavHostFragment) {
            Fragment primaryFragment = ((NavHostFragment) navHostFragment).getChildFragmentManager().getPrimaryNavigationFragment();

            if (primaryFragment instanceof HomeFragment) {
                fabMain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.primary)));
                Fragment childFragment = primaryFragment.getChildFragmentManager().findFragmentById(R.id.smallerFragmentContainer);

                if (childFragment instanceof HomeFragment1) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    hideFab();
                } else if (childFragment instanceof HomeFragment2) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    showFab();
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        if (primaryNavigationFragment instanceof HomeFragment) {
                            ((HomeFragment) primaryNavigationFragment).actionController();
                        }
                    });
                } else if (childFragment instanceof HomeFragment3) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    hideFab();
                } else if (childFragment instanceof HomeFragment4) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    showFab();
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        if (primaryNavigationFragment instanceof HomeFragment) {
                            ((HomeFragment) primaryNavigationFragment).actionController();
                        }
                    });
                }
            } else if (primaryFragment instanceof MilitaryFragment) {
                fabMain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.military)));
                Fragment childFragment = primaryFragment.getChildFragmentManager().findFragmentById(R.id.smallerFragmentContainer);

                if (childFragment instanceof MilitaryFragment1) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    hideFab();
                } else if (childFragment instanceof MilitaryFragment2) {
                    fabMain.setImageResource(R.drawable.home_ic_plus);
                    showFab();
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        if (primaryNavigationFragment instanceof MilitaryFragment) {
                            ((MilitaryFragment) primaryNavigationFragment).actionController();
                        }
                    });
                } else if (childFragment instanceof MilitaryFragment3) {
                    fabMain.setImageResource(R.drawable.home_ic_rotate);
                    showFab();
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        if (primaryNavigationFragment instanceof MilitaryFragment) {
                            ((MilitaryFragment) primaryNavigationFragment).actionController();
                        }
                    });
                } else if (childFragment instanceof MilitaryFragment4) {
                    fabMain.setImageResource(R.drawable.home_ic_time);
                    showFab();
                    fabMain.setOnClickListener(v -> {
                        Fragment primaryNavigationFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                        if (primaryNavigationFragment instanceof MilitaryFragment) {
                            ((MilitaryFragment) primaryNavigationFragment).actionController();
                        }
                    });
                }
            }
        } else {
            fabMain.setImageResource(R.drawable.home_ic_plus);
            hideFab();
        }
    }

    public void hideFab() {
        FloatingActionButton fabMain = findViewById(R.id.fabMain);
        if (fabMain == null) return;

        fabMain.setImageResource(R.drawable.home_ic_plus);
        fabMain.setVisibility(View.GONE);

        FrameLayout navCard = findViewById(R.id.nav_card);
        if (navCard == null) return;

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) navCard.getLayoutParams();

        params.endToStart = -1;
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;

        navCard.setLayoutParams(params);
    }

    public void showFab() {
        FloatingActionButton fabMain = findViewById(R.id.fabMain);
        fabMain.setVisibility(View.VISIBLE);

        FrameLayout navCard = findViewById(R.id.nav_card);

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) navCard.getLayoutParams();

        params.endToStart = R.id.fab_container;
        params.endToEnd = -1;

        navCard.setLayoutParams(params);
    }

    private void refreshUserInstitution() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String email = sharedPreferences.getString("userEmail", "");

        if (!email.isEmpty()) {
            DbQuery.g_firestore.collection("USERS").document(email).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String newInstitution = documentSnapshot.getString("INSTITUTION");
                            if (newInstitution != null) {
                                SharedPreferences.Editor editor = getSharedPreferences("UserData", MODE_PRIVATE).edit();
                                editor.putString("userInstitution", newInstitution);
                                editor.apply();

                                if (drawerInstitutionTextView != null) {
                                    drawerInstitutionTextView.setText(newInstitution);
                                }
                            }
                        }
                    });
        }
    }
    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String userName = sharedPreferences.getString("userName", "");
        String userPhoto = sharedPreferences.getString("userPhoto", "");
        String userInstitution = sharedPreferences.getString("userInstitution", "Student");
        userEmail = sharedPreferences.getString("userEmail", "");

        drawerNameTextView.setText(userName);
        if (drawerInstitutionTextView != null) {
            drawerInstitutionTextView.setText(userInstitution);
        }

        if (!isNetworkAvailable) {
            Glide.with(this).load(R.raw.guest_civil).into(drawerloadingButton);
        } else {
            if (userPhoto.isEmpty() || userPhoto.equals("no_photo") || userPhoto.equals("null")) {
                Glide.with(this).load(R.raw.guest_civil).into(drawerloadingButton);
            } else {
                Glide.with(this).load(userPhoto).into(drawerloadingButton);
            }
        }
    }
}