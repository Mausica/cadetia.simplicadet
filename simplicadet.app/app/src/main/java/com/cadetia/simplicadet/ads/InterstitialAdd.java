package com.cadetia.simplicadet.ads;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.cadetia.simplicadet.R;
import com.cadetia.simplicadet.database.DbQuery;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class InterstitialAdd {
    private static final String TAG = "InterstitialAdd";
    private InterstitialAd mInterstitialAd;

    public void loadInterstitialAd(Activity activity) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) return;

        DbQuery.checkUserPermissions(currentUser.getEmail(), new DbQuery.PermissionCallback() {
            @Override
            public void onPermissionsReceived(boolean isAdmin, boolean isPremium, String institution) {
                if (!isPremium) loadAdForNonPremiumUser(activity);
            }
            @Override
            public void onFailure() {}
        });
    }

    private void loadAdForNonPremiumUser(Activity activity) {
        MobileAds.initialize(activity, initializationStatus -> {});
        AdRequest adRequest = new AdRequest.Builder().build();
        String adUnitId = activity.getResources().getString(R.string.id_InterstitialAdd);

        InterstitialAd.load(activity, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.i(TAG, "Ad loaded.");
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
                Log.e(TAG, "Ad cannot be loaded");
            }
        });
    }

    public void showInterstitialAd(Activity activity) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) return;

        DbQuery.checkUserPermissions(currentUser.getEmail(), new DbQuery.PermissionCallback() {
            @Override
            public void onPermissionsReceived(boolean isAdmin, boolean isPremium, String institution) {
                if (!isPremium) showAdForNonPremiumUser(activity);
            }
            @Override
            public void onFailure() {}
        });
    }

    private void showAdForNonPremiumUser(Activity activity) {
        if (mInterstitialAd != null) {
            Log.i(TAG, "Ad starting.");
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    mInterstitialAd = null;
                }
                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Ad failed to show fullscreen content.");
                    mInterstitialAd = null;
                }
            });
            mInterstitialAd.show(activity);
        } else {
            Log.e(TAG, "Interstitial ad is not ready.");
        }
    }
}