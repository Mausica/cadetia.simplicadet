package com.cadetia.simplicadet.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowCompat;

import com.cadetia.simplicadet.dao.LanguagePreferences;
import com.cadetia.simplicadet.dao.LocaleHelper;
import com.cadetia.simplicadet.dao.ThemePreferences;
import com.cadetia.simplicadet.entities.InstitutionSelectionDialog;
import com.cadetia.simplicadet.utils.NetworkUtils;
import com.cadetia.simplicadet.utils.VersionChecker;
import com.cadetia.simplicadet.entities.DialogConfirm;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.entities.LoadingView;
import com.cadetia.simplicadet.listeners.MyCompleteListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private Button main_signup_button, main_login_button, main_facebook_button, main_google_button;
    private FirebaseAuth firebaseAuth;
    private CallbackManager callbackManager;
    private LoadingView loadingViewMain;
    private static final int RC_SIGN_IN = 9001;
    private static final String DEFAULT_LANGUAGE = "en_GB";

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
        LanguagePreferences languagePreferences = new LanguagePreferences(this);
        if (languagePreferences.isFirstLaunch()) {
            String deviceLanguage = Locale.getDefault().getLanguage();
            String deviceCountry = Locale.getDefault().getCountry();
            String deviceLocale = deviceLanguage + "_" + deviceCountry;

            if (isLanguageSupported(deviceLocale)) {
                languagePreferences.setLanguage(deviceLocale);
            } else {
                languagePreferences.setLanguage(DEFAULT_LANGUAGE);
            }
            languagePreferences.setFirstLaunchComplete();
        }

        ThemePreferences themePreferences = new ThemePreferences(this);
        AppCompatDelegate.setDefaultNightMode(themePreferences.getThemeMode());

        super.onCreate(savedInstanceState);

        Log.d(TAG, "Active locale: " + LocaleHelper.getLanguage(this));
        Log.d(TAG, "Test string value: " + getString(R.string.current_language_display));

        setContentView(R.layout.activity_main);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> false);

        FirebaseApp.initializeApp(this);

        loadingViewMain = findViewById(R.id.loadingViewMain);
        firebaseAuth = FirebaseAuth.getInstance();

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        RelativeLayout relativeLayout = findViewById(R.id.container);
        AnimationDrawable animationDrawable = (AnimationDrawable) relativeLayout.getBackground();
        animationDrawable.setEnterFadeDuration(2000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.nothing));
        window.setNavigationBarColor(getResources().getColor(R.color.nothing));

        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        } else {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        main_signup_button = findViewById(R.id.main_signup_button);
        main_login_button = findViewById(R.id.main_login_button);
        main_facebook_button = findViewById(R.id.main_facebook_button);
        main_google_button = findViewById(R.id.main_google_button);

        main_signup_button.setOnClickListener(v -> {
            if (checkNetworkAndShowDialog()) return;
            animateButtonOnClick(main_signup_button, Signup.class);
        });

        main_login_button.setOnClickListener(v -> {
            if (checkNetworkAndShowDialog()) return;
            animateButtonOnClick(main_login_button, Login.class);
        });

        main_facebook_button.setOnClickListener(v -> {
            if (checkNetworkAndShowDialog()) return;
            animateButtonOnClickNull(main_facebook_button);
            handleFacebookLogin();
        });

        main_google_button.setOnClickListener(v -> {
            if (checkNetworkAndShowDialog()) return;
            animateButtonOnClickNull(main_google_button);
            buttonGoogleSignIn(v);
        });
    }

    private boolean checkNetworkAndShowDialog() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            DialogConfirm.show(
                    this,
                    "No Internet Connection",
                    "Please check your internet connection and try again.",
                    null,
                    true
            );
            return true;
        }
        return false;
    }

    private boolean isLanguageSupported(String language) {
        String[] supportedLanguages = {"en_GB", "es_ES", "fr_FR", "ro_RO"};
        for (String lang : supportedLanguages) {
            if (lang.equals(language)) return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        DbQuery.g_firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            checkIfFirstLogin(currentUser);
        } else {
            checkVersionForNonLoggedUser();
        }
    }

    private void checkIfFirstLogin(FirebaseUser user) {
        String email = user.getEmail();
        String username = user.getDisplayName();
        String photourl = String.valueOf(user.getPhotoUrl());
        saveUserData(username, email, photourl);
        openHomeActivity();
    }


    private void checkVersionForNonLoggedUser() {
        VersionChecker.checkVersionOnMainActivity(this, new VersionChecker.VersionCheckCallback() {
            @Override
            public void onVersionSupported() {
            }

            @Override
            public void onVersionUnsupported() {
                VersionChecker.showUnsupportedVersionDialogForMain(MainActivity.this, null);
            }

            @Override
            public void onMaintenanceMode() {
                VersionChecker.showMaintenanceDialog(MainActivity.this, null);
            }

            @Override
            public void onOfflineMode() {
            }
        });
    }

    public void buttonGoogleSignIn(View view) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    private void handleFacebookLogin() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            String userId = object.getString("id");
                            String userName = object.getString("name");
                            String userEmail = object.getString("email");
                            String profilePicUrl = object.getJSONObject("picture").getJSONObject("data").getString("url");

                            AuthCredential credential = FacebookAuthProvider.getCredential(loginResult.getAccessToken().getToken());
                            startLoadingAnimation();
                            firebaseAuth.signInWithCredential(credential)
                                    .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                                assert user != null;
                                                user.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(profilePicUrl)).build());
                                                checkIfFirstLogin(user);
                                            } else {
                                                stopLoadingAnimation();
                                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                                Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,picture.type(large)");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> googleTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = googleTask.getResult(ApiException.class);
                if (account != null) {
                    String displayName = account.getDisplayName();
                    String email = account.getEmail();
                    String photoUrl = Objects.requireNonNull(account.getPhotoUrl()).toString();
                    String idToken = account.getIdToken();

                    if (idToken != null) {
                        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                        startLoadingAnimation();
                        firebaseAuth.signInWithCredential(firebaseCredential).addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();
                                assert user != null;
                                user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(displayName).build());
                                checkIfFirstLogin(user);
                            } else {
                                stopLoadingAnimation();
                                Log.e(TAG, "signInWithCredential:failure", task.getException());
                            }
                        });
                    }
                }
            } catch (ApiException e) {
                if (e.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                    Log.e(TAG, "Google sign-in canceled");
                } else {
                    Log.e(TAG, "Google sign-in failed", e);
                }
            }
        }
    }

    private void animateButtonOnClick(Button button, Class<?> targetActivityClass) {
        button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .withEndAction(() -> {
                            Intent intent = new Intent(button.getContext(), targetActivityClass);
                            button.getContext().startActivity(intent);
                        })
                        .start())
                .start();
    }

    private void animateButtonOnClickNull(Button button) {
        button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(50)
                        .start())
                .start();
    }

    private void openHomeActivity() {
        Intent intent = new Intent(this, Home.class);
        startActivity(intent);
        stopLoadingAnimation();
        finish();
    }


    private void saveUserData(String userName, String userEmail, String userPhoto) {
        SharedPreferences.Editor editor = getSharedPreferences("UserData", MODE_PRIVATE).edit();
        editor.putString("userName", userName);
        editor.putString("userEmail", userEmail);
        editor.putString("userPhoto", userPhoto);
        editor.apply();
    }

    private void startLoadingAnimation() {
        loadingViewMain.startLoadingAnimation(R.raw.anim_loading_blue, true);
    }

    private void stopLoadingAnimation() {
        loadingViewMain.stopLoadingAnimation();
    }
}