package com.learn.simplify.ui.community;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.learn.simplify.R;
import com.learn.simplify.database.DbQuery;
import com.learn.simplify.model.UserModel;
import com.learn.simplify.databinding.FragmentCommunityBinding;

import java.util.List;
import java.util.Objects;

public class CommunityFragment extends Fragment {

    private FragmentCommunityBinding binding;
    private View leaderboardFirstLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCommunityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUIComponents();
    }

    private void loadUIComponents() {
        setupButtons();
        loadLeaderboardFirstLayout();
    }

    private void setupButtons() {
        View.OnClickListener buttonClickListener = getOnClickListener();
        binding.leaderboardWeekly.setOnClickListener(buttonClickListener);
        binding.leaderboardAll.setOnClickListener(buttonClickListener);
    }

    private void loadLeaderboardFirstLayout() {
        new Handler(Looper.getMainLooper()).post(() -> {
            LinearLayout leaderView = binding.leaderView;

            LayoutInflater inflater = LayoutInflater.from(requireContext());
            leaderboardFirstLayout = inflater.inflate(R.layout.layout_leaderboard_first, leaderView, false);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            leaderboardFirstLayout.setLayoutParams(layoutParams);
            leaderView.addView(leaderboardFirstLayout);

            loadLeaderboardItems();
        });
    }

    private void loadLeaderboardItems() {
        new Handler(Looper.getMainLooper()).post(() -> {
            LinearLayout leaderboardContainer = binding.leaderboardContainer;
            leaderboardContainer.removeAllViews();

            // Fetch sorted user scores from Firestore
            DbQuery.getUsersSortedByScore(new DbQuery.UserScoreListener() {
                @Override
                public void onUserScoresReceived(List<UserModel> userList) {
                    if (userList.size() > 0) {
                        loadTopThreeUsers(userList.subList(0, Math.min(3, userList.size())));
                        if (userList.size() > 3) {
                            loadLeaderboardView(leaderboardContainer, userList.subList(3, userList.size()));
                        }
                    }
                }

                @Override
                public void onFailure() {
                    // Handle the failure case
                }
            });
        });
    }

    private void loadTopThreeUsers(List<UserModel> topThreeUsers) {
        if (topThreeUsers.size() > 0) {
            loadUserIntoView(leaderboardFirstLayout.findViewById(R.id.imageView1),
                    leaderboardFirstLayout.findViewById(R.id.nameTextView1),
                    leaderboardFirstLayout.findViewById(R.id.currencyTextView1),
                    topThreeUsers.get(0));
        }
        if (topThreeUsers.size() > 1) {
            loadUserIntoView(leaderboardFirstLayout.findViewById(R.id.imageView2),
                    leaderboardFirstLayout.findViewById(R.id.nameTextView2),
                    leaderboardFirstLayout.findViewById(R.id.currencyTextView2),
                    topThreeUsers.get(1));
        }
        if (topThreeUsers.size() > 2) {
            loadUserIntoView(leaderboardFirstLayout.findViewById(R.id.imageView3),
                    leaderboardFirstLayout.findViewById(R.id.nameTextView3),
                    leaderboardFirstLayout.findViewById(R.id.currencyTextView3),
                    topThreeUsers.get(2));
        }
    }

    private void loadUserIntoView(ShapeableImageView imageView, TextView nameTextView, TextView scoreTextView, UserModel user) {
        if (user.getPHOTO().isEmpty() || user.getPHOTO().equals("no_photo") || user.getPHOTO().equals("null")) {
            Glide.with(this).load(R.raw.guest).into(imageView);
        } else {
            Glide.with(this).load(user.getPHOTO()).into(imageView);
        }

        nameTextView.setText(user.getNAME());
        scoreTextView.setText(String.valueOf(user.getTOTAL_SCORE()));
    }

    private void loadLeaderboardView(LinearLayout leaderboardContainer, List<UserModel> userList) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        for (int i = 0; i < userList.size(); i++) {
            UserModel user = userList.get(i);

            View leaderboardItem = layoutInflater.inflate(R.layout.item_container_leaderboard, leaderboardContainer, false);

            TextView textViewNumber = leaderboardItem.findViewById(R.id.number_leaderboard);
            textViewNumber.setText(String.valueOf(i + 4)); // Adjusted index to account for top 3

            ShapeableImageView imageView = leaderboardItem.findViewById(R.id.image_leaderboard);
            if (user.getPHOTO().isEmpty() || user.getPHOTO().equals("no_photo") || user.getPHOTO().equals("null")) {
                Glide.with(this).load(R.raw.guest).into(imageView);
            } else {
                Glide.with(this).load(user.getPHOTO()).into(imageView);
            }

            TextView nameTextView = leaderboardItem.findViewById(R.id.name_leaderboard);
            nameTextView.setText(user.getNAME());

            TextView scoreTextView = leaderboardItem.findViewById(R.id.score_leaderboard);
            scoreTextView.setText(String.valueOf(user.getTOTAL_SCORE()));

            leaderboardContainer.addView(leaderboardItem);
        }
    }

    @NonNull
    private View.OnClickListener getOnClickListener() {
        final int weeklyButtonId = R.id.leaderboard_weekly;

        return v -> {
            AppCompatButton clickedButton = (AppCompatButton) v;
            AppCompatButton otherButton = clickedButton.getId() == weeklyButtonId ? binding.leaderboardAll : binding.leaderboardWeekly;
            Drawable greenDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.button_green);

            if (Objects.equals(clickedButton.getBackground().getConstantState(), Objects.requireNonNull(greenDrawable).getConstantState())) {
                clickedButton.setBackgroundResource(R.drawable.background_nothing);
                otherButton.setTextColor(Color.BLACK);
                clickedButton.setTextColor(Color.WHITE);
            } else {
                clickedButton.setBackgroundResource(R.drawable.button_green);
                otherButton.setBackgroundResource(R.drawable.background_nothing);
                otherButton.setTextColor(Color.WHITE);
                clickedButton.setTextColor(Color.BLACK);
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}