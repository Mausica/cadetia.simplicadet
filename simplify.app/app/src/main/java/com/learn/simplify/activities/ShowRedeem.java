package com.learn.simplify.activities;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.learn.simplify.R;
import com.learn.simplify.ads.InterstitialAdd; // Import your ad class

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.xml.KonfettiView;


public class ShowRedeem extends BottomSheetDialogFragment {

    private BottomSheetDialog dialog;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private KonfettiView konfettiView = null;
    private View rootView;
    private Handler handler = new Handler();
    private LottieAnimationView animationView;
    private InterstitialAdd interstitialAdd; // Declare the interstitial ad
    private int totalScore;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> preloadLottieAnimation());
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_redeem, container, false);

        konfettiView = rootView.findViewById(R.id.konfettiRedeem);
        animationView = rootView.findViewById(R.id.redeemAnimationView);
        ImageView closeRedeem = rootView.findViewById(R.id.redeemClose);
        closeRedeem.setOnClickListener(v -> dismiss());

        interstitialAdd = new InterstitialAdd(); // Initialize the interstitial ad
        interstitialAdd.loadInterstitialAd(requireActivity()); // Load the ad

        return rootView;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        CoordinatorLayout layout = dialog.findViewById(R.id.bottomRedeem);
        assert layout != null;
        layout.setMinimumHeight((int) ((Resources.getSystem().getDisplayMetrics().heightPixels) * 0.1));
        bottomSheetBehavior.setDraggable(false);

        Bundle bundle = getArguments();
        assert bundle != null;
        totalScore = bundle.getInt("totalScore", 0);


        String redeemTitleText = "Congratulations! You've got " + totalScore + " organicoins!";
        TextView redeemTitle = rootView.findViewById(R.id.redeemTitle);
        redeemTitle.setText(redeemTitleText);

    }

    private void preloadLottieAnimation() {
        int animationRes = R.raw.uni_redeem;
        animationView.setAnimation(animationRes);
        animationView.setRepeatCount(7);
        animationView.setRepeatMode(LottieDrawable.RESTART);

        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                handler.postDelayed(() -> explode(), 300);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                dismiss();
            }

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        animationView.addLottieOnCompositionLoadedListener(composition -> {
            animationView.playAnimation();
        });
    }

    private void explode() {
        EmitterConfig emitterConfig = new Emitter(120L, TimeUnit.MILLISECONDS).max(30);
        konfettiView.start(
                new PartyFactory(emitterConfig)
                        .spread(360)
                        .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                        .setSpeedBetween(0f, 30f)
                        .position(new Position.Relative(0.5, 0.4))
                        .build());
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (interstitialAdd != null) {
            interstitialAdd.showInterstitialAd(requireActivity()); // Show the ad when the dialog is dismissed
        }
    }

}
