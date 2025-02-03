package com.learn.simplify.ads;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.learn.simplify.R;

/** @noinspection CommentedOutCode*/


public class InterstitialAdd {

    private static final String TAG = "InterstitialAdd";
    private InterstitialAd mInterstitialAd;

    public void loadInterstitialAd(Activity activity) {

        MobileAds.initialize(activity, initializationStatus -> {});

        AdRequest adRequest = new AdRequest.Builder().build();

        String adUnitId = activity.getResources().getString(R.string.id_InterstitialAdd);

        InterstitialAd.load(activity, adUnitId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.e(TAG, "Ad loaded.");
                        //Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        Log.e(TAG, "Ad cannot be loaded");

                    }
                });
    }

    public void showInterstitialAd(Activity activity) {
        if (mInterstitialAd != null) {
            Log.e(TAG, "Ad starting.");
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    //Log.d(TAG, "Ad was clicked.");
                    //Toast.makeText(activity, "Ad was clicked.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    //Toast.makeText(activity, "Ad dismissed fullscreen content.", Toast.LENGTH_SHORT).show();
                    //mInterstitialAd = null;

                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Ad failed to show fullscreen content.");
                    //Toast.makeText(activity, "Ad failed to show fullscreen content.", Toast.LENGTH_SHORT).show();
                    mInterstitialAd = null;
                }

                @Override
                public void onAdImpression() {
                    //Log.d(TAG, "Ad recorded an impression.");
                    //Toast.makeText(activity, "Ad recorded an impression.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    //Log.d(TAG, "Ad showed fullscreen content.");
                    //Toast.makeText(activity, "Ad showed fullscreen content.", Toast.LENGTH_SHORT).show();
                }
            });

            mInterstitialAd.show(activity);
        } else {
            Log.e(TAG, "Interstitial ad is not ready.");
            //Toast.makeText(activity, "Interstitial ad is not ready.", Toast.LENGTH_SHORT).show();
        }
    }
}
